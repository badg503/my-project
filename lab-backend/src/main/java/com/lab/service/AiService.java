package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.config.AiProperties;
import com.lab.entity.AiKnowledge;
import com.lab.entity.AiModelConfig;
import com.lab.entity.LabDevice;
import com.lab.entity.LabInfo;
import com.lab.entity.LabReserve;
import com.lab.mapper.AiKnowledgeMapper;
import com.lab.mapper.AiModelConfigMapper;
import com.lab.mapper.LabDeviceMapper;
import com.lab.mapper.LabInfoMapper;
import com.lab.mapper.LabReserveMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能层：问答优先走内置知识库；若 ai.enabled=true 则故障预测/安全检测/调度/分析可转发至 Python 服务。
 */
@Slf4j
@Service
public class AiService {

    private final AiKnowledgeMapper knowledgeMapper;
    private final AiModelConfigMapper configMapper;
    private final LabDeviceMapper deviceMapper;
    private final LabInfoMapper labInfoMapper;
    private final LabReserveMapper labReserveMapper;
    private final AiProperties aiProperties;
    private final RestTemplate restTemplate;
    private final SensorService sensorService;
    private final JdbcTemplate jdbcTemplate;

    public AiService(AiKnowledgeMapper knowledgeMapper, 
                     AiModelConfigMapper configMapper,
                     LabDeviceMapper deviceMapper,
                     LabInfoMapper labInfoMapper,
                     LabReserveMapper labReserveMapper,
                     AiProperties aiProperties, 
                     RestTemplateBuilder builder,
                     SensorService sensorService,
                     JdbcTemplate jdbcTemplate) {
        this.knowledgeMapper = knowledgeMapper;
        this.configMapper = configMapper;
        this.deviceMapper = deviceMapper;
        this.labInfoMapper = labInfoMapper;
        this.labReserveMapper = labReserveMapper;
        this.aiProperties = aiProperties;
        this.restTemplate = builder.setConnectTimeout(Duration.ofMillis(aiProperties.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(aiProperties.getTimeoutMs())).build();
        this.sensorService = sensorService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 从数据库获取 AI 模型配置的阈值 */
    private Double getThreshold(String modelType) {
        AiModelConfig config = configMapper.selectOne(
            new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getModelType, modelType)
                .last("LIMIT 1")
        );
        if (config != null && config.getThreshold() != null) {
            return config.getThreshold().doubleValue();
        }
        // 默认阈值
        if ("QA".equals(modelType)) {
            return 0.70;  // BERT 问答默认阈值（与配置文件一致）
        }
        return 0.5;  // 其他默认 0.5
    }
    
    /** 获取实时数据时间窗口配置（分钟） */
    private int getRealtimeWindowMinutes() {
        try {
            AiModelConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<AiModelConfig>()
                    .eq(AiModelConfig::getModelType, "FAULT_PREDICT")
                    .eq(AiModelConfig::getParamKey, "realtime_window_minutes")
                    .last("LIMIT 1")
            );
            if (config != null && config.getParamValue() != null) {
                int minutes = Integer.parseInt(config.getParamValue());
                System.out.println("✅ 从数据库读取实时时间窗口配置：" + minutes + " 分钟");
                return minutes;
            }
        } catch (Exception e) {
            System.out.println("❌ 读取实时时间窗口配置失败：" + e.getMessage());
        }
        // 默认 10 分钟
        return 10;
    }
    
    /** 获取 BERT 问答相似度阈值（数据库为唯一数据源） */
    public Map<String, Object> getBertThreshold() {
        Double threshold = getThreshold("QA");
        Map<String, Object> result = new HashMap<>();
        result.put("threshold", threshold);
        result.put("status", "SUCCESS");
        return result;
    }
    
    /** 设置 BERT 问答相似度阈值 */
    public Map<String, Object> setBertThreshold(Double threshold) {
        if (threshold == null || threshold < 0.0 || threshold > 1.0) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ERROR");
            result.put("error", "阈值必须在 0.0-1.0 之间");
            return result;
        }

        try {
            AiModelConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<AiModelConfig>()
                    .eq(AiModelConfig::getModelType, "QA")
                    .last("LIMIT 1")
            );
            
            if (config == null) {
                config = new AiModelConfig();
                config.setModelType("BERT_QA");
                config.setParamKey("similarity_threshold");
                config.setParamValue(String.valueOf(threshold));
                config.setThreshold(new BigDecimal(threshold));
                config.setDescription("BERT 智能问答相似度阈值");
                configMapper.insert(config);
            } else {
                config.setParamValue(String.valueOf(threshold));
                config.setThreshold(new BigDecimal(threshold));
                configMapper.updateById(config);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("threshold", threshold);
            result.put("message", "阈值已更新为 " + threshold);
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ERROR");
            result.put("error", "更新失败：" + e.getMessage());
            return result;
        }
    }

    /** 智能问答：优先使用 BERT 模型，BERT 无法理解时降级到内置知识库 */
    public String qa(String question) {
        if (question == null || question.trim().isEmpty()) return "请输入您的问题。";
        
        System.out.println("🔍 AI 服务调用：question=" + question);
        System.out.println("🔍 AI 配置：enabled=" + aiProperties.isEnabled() + ", baseUrl=" + aiProperties.getBaseUrl());
        
        // 1. 优先尝试 BERT 模型
        if (aiProperties.isEnabled()) {
            try {
                String url = aiProperties.getBaseUrl() + aiProperties.getQaEndpoint();
                System.out.println("🌐 请求 Python BERT AI 服务：" + url);
                
                // 使用 POST 方法，发送 JSON body（包含阈值参数）
                Double qaThreshold = getThreshold("QA");
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("question", question.trim());
                requestBody.put("threshold", qaThreshold);

                ResponseEntity<Map> resp = restTemplate.postForEntity(url, requestBody, Map.class);
                System.out.println("✅ Python BERT AI 响应：" + resp.getBody());
                
                if (resp.getBody() != null) {
                    Object answerObj = resp.getBody().get("answer");
                    Object similarityObj = resp.getBody().get("similarity");
                    
                    // 检查 BERT 返回的相似度
                    Double similarity = null;
                    if (similarityObj instanceof Number) {
                        similarity = ((Number) similarityObj).doubleValue();
                    }
                    
                    System.out.println("📊 BERT 相似度：" + similarity);
                    
                    // 如果 BERT 成功匹配（相似度>0），直接返回答案
                    if (answerObj != null && (similarity == null || similarity > 0)) {
                        if (answerObj instanceof Map) {
                            // Python 返回的是 {"answer": {"question": "...", "answer": "..."}}
                            Map answerMap = (Map) answerObj;
                            String answer = (String) answerMap.get("answer");
                            System.out.println("✅ BERT 回答：" + answer);
                            return answer;
                        } else if (answerObj instanceof String) {
                            // Python 返回的是 {"answer": "..."}
                            String answer = (String) answerObj;
                            System.out.println("✅ BERT 回答：" + answer);
                            return answer;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ Python BERT AI 调用失败：" + e.getMessage());
                e.printStackTrace();
                // 降级到知识库
            }
        }
        
        // 2. BERT 无法理解时，降级到数据库知识库
        System.out.println("📚 BERT 无法理解，降级到数据库知识库查询");
        List<AiKnowledge> list = knowledgeMapper.selectList(
                new LambdaQueryWrapper<AiKnowledge>().like(AiKnowledge::getQuestion, question.trim()).last("LIMIT 5"));
        
        if (!list.isEmpty()) {
            String answer = list.get(0).getAnswer();
            System.out.println("✅ 知识库回答：" + answer);
            return answer;
        }
        
        // 3. 知识库也没有匹配时，返回默认提示
        System.out.println("⚠️ 知识库也未找到匹配，返回默认提示");
        return "暂未找到该问题的答案，请尝试换个问法或联系管理员。";
    }

    /** 设备故障预测：从数据库获取设备信息和使用记录，发送到 Python 服务 */
    @Async("aiExecutor")
    public Map<String, Object> faultPredict(Long deviceId) {
        return faultPredictWithVirtualData(deviceId, false);
    }
    
    /**
     * 设备故障预测（支持虚拟数据模式）
     * @param deviceId 设备 ID
     * @param requireRealtimeData 是否要求实时数据（手动预测时为 true）
     * @return 预测结果
     */
    public Map<String, Object> faultPredictWithVirtualData(Long deviceId, boolean requireRealtimeData) {
        // 从数据库获取阈值配置
        Double threshold = getThreshold("FAULT_PREDICT");
        
        // 获取动态时间窗口配置（分钟）
        int realtimeWindowMinutes = getRealtimeWindowMinutes();
        
        // 获取设备信息
        LabDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("deviceId", deviceId);
            result.put("error", "设备不存在");
            result.put("threshold", threshold);
            return result;
        }
        
        // 构建设备信息（Python 服务需要）
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("id", device.getId());
        deviceInfo.put("name", device.getName());
        deviceInfo.put("deviceType", device.getDeviceType());
        deviceInfo.put("status", device.getStatus());
        deviceInfo.put("labId", device.getLabId());
        
        // 1. 检查传感器数据完整性（必须 3 个传感器都有数据）
        System.out.println("📚 检查设备 " + device.getName() + " 传感器数据...");
        System.out.println("⏰ 实时时间窗口配置：" + realtimeWindowMinutes + " 分钟");
        
        // 检查最近 N 分钟内是否有完整的传感器数据（3 个传感器都有）
        String checkCompleteSql = "SELECT COUNT(*) FROM sensor_reading " +
            "WHERE device_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL ? MINUTE) " +
            "AND temp IS NOT NULL AND vibration IS NOT NULL AND current IS NOT NULL " +
            "AND status = 1 AND sensor_count >= 3";
        Integer completeCount = jdbcTemplate.queryForObject(checkCompleteSql, Integer.class, deviceId, realtimeWindowMinutes);
        System.out.println("🔍 完整实时数据查询结果：deviceId=" + deviceId + ", count=" + completeCount);
        
        boolean hasCompleteRealtimeData = (completeCount != null && completeCount > 0);
        
        // 如果是手动预测，必须有完整的实时数据
        if (requireRealtimeData && !hasCompleteRealtimeData) {
            // 检查是否完全没有传感器数据
            String checkAnyDataSql = "SELECT COUNT(*) FROM sensor_reading WHERE device_id = ?";
            Integer anyDataCount = jdbcTemplate.queryForObject(checkAnyDataSql, Integer.class, deviceId);
            System.out.println("🔍 任何数据查询结果: deviceId=" + deviceId + ", count=" + anyDataCount);
            
            if (anyDataCount == null || anyDataCount == 0) {
                System.out.println("⚠️ 设备 " + device.getName() + " 无任何传感器数据");
                Map<String, Object> result = new HashMap<>();
                result.put("deviceId", deviceId);
                result.put("deviceName", device.getName());
                result.put("warning", "无传感器数据");
                result.put("message", "设备暂无传感器数据，无法进行故障预测");
                result.put("suggestion", "请检查传感器配置或等待数据采集");
                result.put("threshold", threshold);
                result.put("faultProb", 0.0);
                result.put("isFaulty", false);
                return result;
            }
            
            // 有数据但不完整或不是实时的
            System.out.println("⚠️ 设备 " + device.getName() + " 无完整的实时传感器数据");
            Map<String, Object> result = new HashMap<>();
            result.put("deviceId", deviceId);
            result.put("deviceName", device.getName());
            result.put("warning", "传感器数据不完整");
            result.put("message", "设备传感器数据不完整（缺少温度/振动/电流数据），无法进行预测");
            result.put("suggestion", "请检查传感器连接或等待数据采集完成");
            result.put("threshold", threshold);
            result.put("faultProb", 0.0);
            result.put("isFaulty", false);
            return result;
        }
        
        // 2. 读取历史传感器数据（用于定时预测或实时预测）
        List<Map<String, Object>> historyData = getHistorySensorData(deviceId, 30);
        
        if (historyData.isEmpty()) {
            System.out.println("⚠️ 设备 " + device.getName() + " 无历史传感器数据");
            Map<String, Object> result = new HashMap<>();
            result.put("deviceId", deviceId);
            result.put("deviceName", device.getName());
            result.put("warning", "无历史数据");
            result.put("message", "设备无历史传感器数据，无法进行故障预测");
            result.put("suggestion", "请等待数据采集完成后再次尝试");
            result.put("threshold", threshold);
            result.put("faultProb", 0.0);
            result.put("isFaulty", false);
            return result;
        } else {
            System.out.println("✅ 设备 " + device.getName() + " 读取到 " + historyData.size() + " 条有效数据");
        }
        
        // 3. 调用 AI 服务预测
        if (aiProperties.isEnabled()) {
            try {
                // 构建 POST 请求体
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("device_id", deviceId);
                requestBody.put("device_info", deviceInfo);
                requestBody.put("history_data", historyData);
                
                // 获取传感器配置
                Map<String, Object> sensorConfig = sensorService.getSensorConfigForPython(deviceId);
                requestBody.put("sensor_mode", sensorConfig.get("mode"));
                requestBody.put("sensor_config", sensorConfig.get("sensor_config"));
                
                String url = aiProperties.getBaseUrl() + aiProperties.getFaultPredictEndpoint();
                ResponseEntity<Map> resp = restTemplate.postForEntity(url, requestBody, Map.class);
                
                if (resp.getBody() != null) {
                    Map<String, Object> result = resp.getBody();
                    result.put("threshold", threshold);
                    return result;
                }
            } catch (Exception e) {
                System.out.println("❌ 故障预测失败：" + e.getMessage());
                e.printStackTrace();
                
                Map<String, Object> result = new HashMap<>();
                result.put("deviceId", deviceId);
                result.put("error", "AI 服务调用失败：" + e.getMessage());
                result.put("fallback", true);
                result.put("threshold", threshold);
                return result;
            }
        }
        
        // 降级方案：返回模拟数据
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("deviceName", device.getName());
        result.put("faultProb", Math.random() * 0.3);
        result.put("threshold", threshold);
        result.put("message", "故障预测服务未启用，返回模拟数据");
        result.put("suggestion", "建议启用 AI 服务获取准确预测");
        return result;
    }
    
    /**
     * 从数据库读取历史传感器数据
     * @param deviceId 设备 ID
     * @param days 天数
     * @return 历史数据列表
     */
    private List<Map<String, Object>> getHistorySensorData(Long deviceId, int days) {
        try {
            String sql = "SELECT temp, vibration, current, reading_time FROM sensor_reading " +
                "WHERE device_id = ? AND reading_time >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "ORDER BY reading_time ASC";
            
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, deviceId, days);
            
            // 转换数据格式
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : list) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("temp", row.get("temp"));
                dataPoint.put("vibration", row.get("vibration"));
                dataPoint.put("current", row.get("current"));
                dataPoint.put("time", row.get("reading_time"));
                result.add(dataPoint);
            }
            
            return result;
        } catch (Exception e) {
            System.out.println("❌ 读取历史传感器数据失败：" + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 批量故障预测（用于实验室设备管理）
     * 
     * @param labId 实验室 ID
     * @param deviceIds 设备 ID 列表
     * @param triggerType 触发类型 (OPEN/CLOSE/MANUAL)
     * @param operatorId 操作人 ID（手动检测时）
     * @return 预测结果
     */
    public Map<String, Object> batchFaultPredict(
            Long labId, 
            List<Long> deviceIds, 
            String triggerType,
            Long operatorId) {
        
        System.out.println("🔍 开始批量故障预测，实验室：" + labId + ", 设备数：" + deviceIds.size() + ", 触发类型：" + triggerType);
        
        List<Map<String, Object>> predictions = new ArrayList<>();
        
        // 逐个预测设备
        for (Long deviceId : deviceIds) {
            try {
                Map<String, Object> result = faultPredict(deviceId);
                if (result.get("error") == null) {
                    predictions.add(result);
                }
            } catch (Exception e) {
                System.out.println("⚠️ 设备 " + deviceId + " 预测失败：" + e.getMessage());
            }
        }
        
        // 统计结果
        int faultyCount = 0;
        for (Map<String, Object> pred : predictions) {
            Double faultProb = Double.valueOf(pred.get("faultProb").toString());
            Double threshold = Double.valueOf(pred.get("threshold").toString());
            if (faultProb >= threshold) {
                faultyCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", predictions.size());
        result.put("faultyCount", faultyCount);
        result.put("normalCount", predictions.size() - faultyCount);
        result.put("predictions", predictions);
        result.put("message", faultyCount > 0 ? 
            "发现 " + faultyCount + " 台疑似故障设备" : 
            "所有设备运行正常");
        
        return result;
    }

    /** 安全检测：若启用外部服务则请求 Python YOLOv8 接口，否则返回占位说明。 */
    public Map<String, Object> safetyDetect(Long labId) {
        if (aiProperties.isEnabled()) {
            try {
                String url = aiProperties.getBaseUrl() + aiProperties.getSafetyDetectEndpoint() + "?lab_id=" + labId;
                ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
                if (resp.getBody() != null) return resp.getBody();
            } catch (Exception e) {
                Map<String, Object> result = new HashMap<>();
                result.put("labId", labId);
                result.put("error", "AI 服务调用失败：" + e.getMessage());
                result.put("fallback", true);
                return result;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("labId", labId);
        result.put("message", "安全检测需对接 YOLOv8 服务，当前为占位。请设置 ai.enabled=true 并启动 Python 服务。");
        return result;
    }

    /** 智能预约调度：若启用外部服务则请求 Python 遗传算法接口，否则返回占位说明。 */
    public Map<String, Object> schedule() {
        if (aiProperties.isEnabled()) {
            try {
                // 从数据库获取实验室数据
                List<LabInfo> labList = labInfoMapper.selectList(null);
                List<Map<String, Object>> labData = new ArrayList<>();
                for (LabInfo lab : labList) {
                    Map<String, Object> labInfo = new HashMap<>();
                    labInfo.put("labId", lab.getId());
                    labInfo.put("name", lab.getName());
                    labInfo.put("capacity", lab.getCapacity());
                    labData.add(labInfo);
                }
                
                // 从数据库获取今天的预约数据
                LocalDate today = LocalDate.now();
                LambdaQueryWrapper<LabReserve> queryWrapper = new LambdaQueryWrapper<LabReserve>()
                    .eq(LabReserve::getReserveDate, today)
                    .in(LabReserve::getStatus, "APPROVED", "PENDING");
                List<LabReserve> reserveList = labReserveMapper.selectList(queryWrapper);
                List<Map<String, Object>> reserveData = new ArrayList<>();
                for (LabReserve reserve : reserveList) {
                    Map<String, Object> reserveInfo = new HashMap<>();
                    reserveInfo.put("labId", reserve.getLabId());
                    reserveInfo.put("userId", reserve.getUserId());
                    reserveInfo.put("reserveDate", reserve.getReserveDate().toString());
                    reserveInfo.put("timeSlotStart", reserve.getTimeSlotStart());
                    reserveInfo.put("timeSlotEnd", reserve.getTimeSlotEnd());
                    reserveData.add(reserveInfo);
                }
                
                // 构建请求参数
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("labs", labData);
                requestData.put("reserves", reserveData);
                
                // 发送 POST 请求给 AI 服务
                String url = aiProperties.getBaseUrl() + aiProperties.getScheduleEndpoint();
                ResponseEntity<Map> resp = restTemplate.postForEntity(url, requestData, Map.class);
                if (resp.getBody() != null) {
                    log.info("✅ AI 预约调度分析成功");
                    return resp.getBody();
                }
            } catch (Exception e) {
                log.error("❌ AI 预约调度失败：{}", e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("error", "AI 服务调用失败：" + e.getMessage());
                result.put("fallback", true);
                return result;
            }
        }
        
        // 占位数据
        Map<String, Object> result = new HashMap<>();
        result.put("message", "智能预约调度需对接遗传算法服务，当前为占位。请设置 ai.enabled=true 并启动 Python 服务。");
        return result;
    }

    /** AI 数据分析：若启用外部服务则请求 Python 数据分析接口，否则返回占位说明。 */
    public Map<String, Object> analysis() {
        if (aiProperties.isEnabled()) {
            try {
                // 从数据库获取历史预约数据（过去 30 天）
                LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
                String sql = "SELECT lr.lab_id, lr.reserve_date, lr.time_slot_start, lr.time_slot_end, " +
                            "li.capacity, li.name as lab_name " +
                            "FROM lab_reserve lr " +
                            "JOIN lab_info li ON lr.lab_id = li.id " +
                            "WHERE lr.reserve_date >= ? AND lr.status = 'APPROVED' " +
                            "ORDER BY lr.reserve_date";
                
                List<Map<String, Object>> reserveData = jdbcTemplate.queryForList(sql, thirtyDaysAgo);
                
                // 从数据库获取故障记录（过去 30 天）
                String faultSql = "SELECT device_id, lab_id, fault_date, fault_type " +
                                 "FROM lab_repair " +
                                 "WHERE fault_date >= ? " +
                                 "ORDER BY fault_date";
                List<Map<String, Object>> faultData = jdbcTemplate.queryForList(faultSql, thirtyDaysAgo);
                
                // 构建请求参数
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("reserves", reserveData);
                requestData.put("faults", faultData);
                
                // 发送 POST 请求给 AI 服务
                String url = aiProperties.getBaseUrl() + aiProperties.getAnalysisEndpoint();
                ResponseEntity<Map> resp = restTemplate.postForEntity(url, requestData, Map.class);
                if (resp.getBody() != null) {
                    log.info("✅ AI 数据分析成功");
                    return resp.getBody();
                }
            } catch (Exception e) {
                log.error("❌ AI 数据分析失败：{}", e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("error", "AI 服务调用失败：" + e.getMessage());
                result.put("fallback", true);
                return result;
            }
        }
        
        // 占位数据
        Map<String, Object> result = new HashMap<>();
        result.put("message", "AI 数据分析需对接统计分析服务，当前为占位。请设置 ai.enabled=true 并启动 Python 服务。");
        return result;
    }

    /**
     * 查询预测结果（从 ai_fault_prediction 表）
     * 每个设备只显示最新的一条预测记录，去重
     */
    public Map<String, Object> getPredictionResults(Long labId, String triggerType, String date, Integer current, Integer size) {
        try {
            // 使用子查询获取每个设备最新的预测记录 ID
            // 逻辑：先按 device_id 分组，取每组中 predict_time 最大的记录
            StringBuilder sql = new StringBuilder(
                "SELECT * FROM ai_fault_prediction WHERE id IN (" +
                "  SELECT MAX(id) FROM (" +
                "    SELECT id, device_id, predict_time FROM ai_fault_prediction WHERE deleted = 0"
            );
            List<Object> params = new ArrayList<>();
            
            // 只有当 labId 不为 null 时才添加条件
            if (labId != null) {
                sql.append(" AND lab_id = ?");
                params.add(labId);
            }
            
            // 按触发类型筛选
            if (triggerType != null && !triggerType.isEmpty()) {
                sql.append(" AND trigger_type = ?");
                params.add(triggerType);
            }
            
            // 按日期筛选（查询指定日期的 00:00:00 到 23:59:59）
            if (date != null && !date.isEmpty()) {
                sql.append(" AND DATE(predict_time) = ?");
                params.add(date);
            }
            
            sql.append("    ORDER BY device_id, predict_time DESC" +
                      "  ) AS latest GROUP BY device_id" +
                      ") ORDER BY predict_time DESC LIMIT ? OFFSET ?");
            params.add(size);
            params.add((current - 1) * size);
            
            // 查询总数（去重后的设备数）
            StringBuilder countSql = new StringBuilder(
                "SELECT COUNT(DISTINCT device_id) FROM ai_fault_prediction WHERE deleted = 0"
            );
            List<Object> countParams = new ArrayList<>();
            
            // 只有当 labId 不为 null 时才添加条件
            if (labId != null) {
                countSql.append(" AND lab_id = ?");
                countParams.add(labId);
            }
            
            if (triggerType != null && !triggerType.isEmpty()) {
                countSql.append(" AND trigger_type = ?");
                countParams.add(triggerType);
            }
            
            if (date != null && !date.isEmpty()) {
                countSql.append(" AND DATE(predict_time) = ?");
                countParams.add(date);
            }
            
            Long total = jdbcTemplate.queryForObject(countSql.toString(), countParams.toArray(), Long.class);
            
            // 查询数据
            List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
            // 转换数据格式，添加 warning 字段
            for (Map<String, Object> record : records) {
                // 如果没有 fault_probability 或为 null，设置为 0 并标记为 warning
                if (record.get("fault_probability") == null) {
                    record.put("warning", true);
                    record.put("fault_probability", 0.0);
                } else {
                    record.put("warning", false);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("total", total != null ? total : 0);
            result.put("current", current);
            result.put("size", size);
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = new HashMap<>();
            result.put("error", "查询失败：" + e.getMessage());
            return result;
        }
    }
    
    /**
     * 故障反馈学习：当设备报废时，获取该设备最近一周的传感器数据并发送到 AI 服务进行学习
     * @param deviceId 设备 ID
     */
    public void learnFromFault(Long deviceId) {
        try {
            log.info("📚 开始收集设备 {} 的故障学习数据", deviceId);
            
            // 获取设备最近 7 天的传感器数据（不管数据是否完整，都学习）
            String sql = "SELECT temp, vibration, current, created_at FROM sensor_reading " +
                "WHERE device_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                "ORDER BY created_at ASC";
            
            List<Map<String, Object>> readings = jdbcTemplate.queryForList(sql, deviceId);
            
            if (readings.isEmpty()) {
                log.warn("⚠️ 设备 {} 无历史传感器数据，无法学习", deviceId);
                return;
            }
            
            log.info("📊 收集到设备 {} 的 {} 条传感器数据", deviceId, readings.size());
            
            // 转换为 AI 服务需要的格式
            // 注意：传感器数据可能缺失某个特征，用 0 填充
            List<List<Double>> historyData = new ArrayList<>();
            for (Map<String, Object> reading : readings) {
                Double temp = reading.get("temp") != null ? ((Number) reading.get("temp")).doubleValue() : 0.0;
                Double vibration = reading.get("vibration") != null ? ((Number) reading.get("vibration")).doubleValue() : 0.0;
                Double current = reading.get("current") != null ? ((Number) reading.get("current")).doubleValue() : 0.0;
                
                List<Double> sensorData = new ArrayList<>();
                sensorData.add(temp);
                sensorData.add(vibration);
                sensorData.add(current);
                historyData.add(sensorData);
            }
            
            // 调用 Python AI 服务进行学习
            if (aiProperties.isEnabled()) {
                String url = aiProperties.getBaseUrl() + "/ai/fault-learn";
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("deviceId", String.valueOf(deviceId));
                requestBody.put("history", historyData);
                requestBody.put("actualFault", 1); // 1 表示实际故障
                
                log.info("🌐 请求 Python AI 服务：{}", url);
                
                restTemplate.postForEntity(url, requestBody, Map.class);
                
                log.info("✅ 设备 {} 故障反馈学习完成", deviceId);
            } else {
                log.warn("⚠️ AI 服务未启用，跳过故障反馈学习");
            }
            
        } catch (Exception e) {
            log.error("❌ 设备 {} 故障反馈学习失败：{}", deviceId, e.getMessage());
            // 学习失败不影响设备报废流程
        }
    }

    /**
     * AI 数据分析（Prophet）
     * 返回实验室统计数据、未来 7 天预测和决策建议
     */
    public Map<String, Object> getAIAnalysis() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🔍 开始 AI 数据分析...");
            
            // 调用 Python AI 服务
            if (aiProperties.isEnabled()) {
                String url = aiProperties.getBaseUrl() + "/ai/analysis";
                log.info("🌐 请求 Python AI 服务：{}", url);
                
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                
                if (response.getBody() != null) {
                    result = response.getBody();
                    log.info("✅ AI 数据分析成功");
                } else {
                    log.warn("⚠️ AI 服务返回空数据");
                    result.put("statistics", new HashMap<>());
                    result.put("forecasts", new HashMap<>());
                    result.put("suggestions", new ArrayList<>());
                }
            } else {
                log.warn("⚠️ AI 服务未启用，返回空数据");
                result.put("statistics", new HashMap<>());
                result.put("forecasts", new HashMap<>());
                result.put("suggestions", new ArrayList<>());
            }
            
        } catch (Exception e) {
            log.error("❌ AI 数据分析失败：{}", e.getMessage());
            result.put("statistics", new HashMap<>());
            result.put("forecasts", new HashMap<>());
            result.put("suggestions", new ArrayList<>());
            result.put("error", "AI 分析失败：" + e.getMessage());
        }
        
        return result;
    }
}
