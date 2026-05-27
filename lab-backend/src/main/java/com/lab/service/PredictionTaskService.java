package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.entity.AiModelConfig;
import com.lab.entity.LabDevice;
import com.lab.entity.PredictionTask;
import com.lab.mapper.AiModelConfigMapper;
import com.lab.mapper.LabDeviceMapper;
import com.lab.mapper.PredictionTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 预测任务服务
 */
@Service
public class PredictionTaskService {
    
    private static final Logger log = LoggerFactory.getLogger(PredictionTaskService.class);
    
    private final PredictionTaskMapper predictionTaskMapper;
    private final LabDeviceMapper deviceMapper;
    private final AiService aiService;
    private final JdbcTemplate jdbcTemplate;
    private final AiModelConfigMapper aiModelConfigMapper;
    
    public PredictionTaskService(PredictionTaskMapper predictionTaskMapper,
                                LabDeviceMapper deviceMapper,
                                AiService aiService,
                                JdbcTemplate jdbcTemplate,
                                AiModelConfigMapper aiModelConfigMapper) {
        this.predictionTaskMapper = predictionTaskMapper;
        this.deviceMapper = deviceMapper;
        this.aiService = aiService;
        this.jdbcTemplate = jdbcTemplate;
        this.aiModelConfigMapper = aiModelConfigMapper;
    }
    
    /**
     * 传感器数据状态
     */
    static class SensorDataStatus {
        boolean hasAnyData = false;      // 是否有任何传感器数据
        boolean hasRealtimeData = false; // 是否有实时数据（今天的数据）
        int devicesWithData = 0;         // 有数据的设备数量
        int devicesWithRealtime = 0;     // 有实时数据的设备数量
        int realtimeWindowMinutes = 0;   // 实时时间窗口（分钟）
        int totalDevices = 0;            // 总设备数
        
        SensorDataStatus(int totalDevices) {
            this.totalDevices = totalDevices;
        }
    }
    
    /**
     * 检查设备的传感器数据状态
     */
    private SensorDataStatus checkSensorDataStatus(List<LabDevice> devices) {
        SensorDataStatus status = new SensorDataStatus(devices.size());
        
        if (devices.isEmpty()) {
            return status;
        }
        
        // 获取所有设备 ID
        List<Long> deviceIds = devices.stream()
            .map(LabDevice::getId)
            .collect(Collectors.toList());
        
        try {
            // 从 AI 配置表获取实时时间窗口（分钟），默认 10 分钟
            AiModelConfig config = aiModelConfigMapper.selectOne(
                new LambdaQueryWrapper<AiModelConfig>()
                    .eq(AiModelConfig::getModelType, "FAULT_PREDICT")
                    .eq(AiModelConfig::getParamKey, "realtime_window_minutes")
            );
            int realtimeWindowMinutes = 10; // 默认值
            if (config != null && config.getParamValue() != null) {
                try {
                    realtimeWindowMinutes = Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("实时时间窗口配置格式错误，使用默认值 10 分钟");
                }
            }
            
            // 检查是否有传感器数据
            String checkAnyDataSql = "SELECT COUNT(DISTINCT device_id) FROM sensor_reading WHERE device_id IN (" +
                deviceIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
            
            Integer devicesWithData = jdbcTemplate.queryForObject(checkAnyDataSql, Integer.class, 
                deviceIds.toArray());
            
            if (devicesWithData != null && devicesWithData > 0) {
                status.hasAnyData = true;
                status.devicesWithData = devicesWithData;
                
                // 检查是否有实时数据（最近 N 分钟内的数据）
                List<Object> params = new ArrayList<>(deviceIds);
                params.add(realtimeWindowMinutes);
                
                String checkRealtimeSql = "SELECT COUNT(DISTINCT device_id) FROM sensor_reading WHERE device_id IN (" +
                    deviceIds.stream().map(id -> "?").collect(Collectors.joining(",")) + 
                    ") AND created_at >= DATE_SUB(NOW(), INTERVAL ? MINUTE)";
                
                Integer devicesWithRealtime = jdbcTemplate.queryForObject(checkRealtimeSql, Integer.class,
                    params.toArray());
                
                if (devicesWithRealtime != null && devicesWithRealtime > 0) {
                    status.hasRealtimeData = true;
                    status.devicesWithRealtime = devicesWithRealtime;
                    status.realtimeWindowMinutes = realtimeWindowMinutes;
                }
            }
        } catch (Exception e) {
            log.error("❌ 检查传感器数据状态失败：{}", e.getMessage());
        }
        
        return status;
    }
    
    /**
     * 每日定时预测所有设备
     */
    public void dailyFaultPrediction() {
        log.info("🕐 开始执行每日故障预测任务...");
        
        try {
            // 1. 清理 30 天前的旧数据
            cleanupOldData();
            
            // 2. 获取所有启用的设备
            List<LabDevice> devices = getEnabledDevices();
            int total = devices.size();
            
            if (total == 0) {
                log.info("⚠️ 没有启用的设备，跳过预测");
                return;
            }
            
            log.info("📊 共需预测 {} 台设备", total);
            
            // 3. 检查传感器数据状态
            SensorDataStatus sensorStatus = checkSensorDataStatus(devices);
            if (!sensorStatus.hasAnyData) {
                log.warn("⚠️ 未接收到传感器数据，将使用历史数据进行预测");
                sendDataFailureNotification();
            } else {
                log.info("✅ 传感器数据已就绪，{} 台设备有数据", sensorStatus.devicesWithData);
                if (sensorStatus.hasRealtimeData) {
                    log.info("✅ 包含实时数据（最近 {} 分钟内），{} 台设备有实时数据", 
                        sensorStatus.realtimeWindowMinutes, sensorStatus.devicesWithRealtime);
                } else {
                    log.info("⚠️ 无实时数据，仅使用历史数据预测");
                }
            }
            
            // 4. 创建预测任务
            PredictionTask task = createPredictionTask(null, devices, "SCHEDULED");
            
            // 5. 批量预测
            batchPredictDevices(task.getTaskId(), devices, "SCHEDULED", sensorStatus);
            
            log.info("✅ 每日故障预测任务完成，任务 ID: {}", task.getTaskId());
            
        } catch (Exception e) {
            log.error("❌ 每日故障预测任务失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 清理 30 天前的传感器数据和 7 天前的预测结果
     */
    private void cleanupOldData() {
        try {
            // 清理 30 天前的传感器数据
            String sensorSql = "DELETE FROM sensor_reading WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)";
            int sensorCount = jdbcTemplate.update(sensorSql);
            log.info("🧹 清理了 {} 条 30 天前的传感器数据", sensorCount);
            
            // 清理 7 天前的预测结果
            String predictionSql = "DELETE FROM fault_prediction_detail WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)";
            int predictionCount = jdbcTemplate.update(predictionSql);
            log.info("🧹 清理了 {} 条 7 天前的预测结果", predictionCount);
        } catch (Exception e) {
            log.error("❌ 清理旧数据失败：{}", e.getMessage());
        }
    }
    
    /**
     * 手动触发预测（立即预测）
     */
    public PredictionTask manualPredict(Long labId) {
        log.info("🔍 开始手动故障预测，实验室 ID: {}", labId != null ? labId : "所有实验室");
        
        // 查询实验室的设备
        List<LabDevice> devices = getLabDevices(labId);
        
        if (devices.isEmpty()) {
            String msg = labId != null ? "该实验室没有设备" : "当前没有设备";
            throw new RuntimeException(msg);
        }
        
        // 检查传感器数据情况（仅记录，不阻止预测）
        SensorDataStatus sensorStatus = checkSensorDataStatus(devices);
        
        if (!sensorStatus.hasAnyData) {
            log.warn("⚠️ 无传感器数据，预测结果将显示无数据警告");
        } else if (!sensorStatus.hasRealtimeData) {
            log.warn("⚠️ 无实时传感器数据，将使用历史数据进行预测");
        } else {
            log.info("✅ 检测到实时数据（最近 {} 分钟内），设备数：{}", 
                sensorStatus.realtimeWindowMinutes, sensorStatus.devicesWithRealtime);
        }
        
        // 创建预测任务
        PredictionTask task = createPredictionTask(labId, devices, "MANUAL");
        
        // 设置传感器数据状态
        task.setHasRealtimeData(sensorStatus.hasRealtimeData);
        predictionTaskMapper.updateById(task);
        
        // 异步执行预测
        CompletableFuture.runAsync(() -> {
            try {
                updateTaskStatus(task.getTaskId(), "RUNNING", LocalDateTime.now());
                batchPredictDevices(task.getTaskId(), devices, "MANUAL", sensorStatus);
                log.info("✅ 手动预测任务完成：{}", task.getTaskId());
            } catch (Exception e) {
                log.error("❌ 手动预测任务失败：{}", task.getTaskId(), e);
                updateTaskStatus(task.getTaskId(), "FAILED", LocalDateTime.now());
            }
        });
        
        return task;
    }
    
    /**
     * 查询任务进度
     */
    public PredictionTask getTaskProgress(String taskId) {
        return predictionTaskMapper.selectByTaskId(taskId);
    }
    
    /**
     * 批量预测设备
     */
    @Transactional
    private void batchPredictDevices(String taskId, List<LabDevice> devices, String triggerType, SensorDataStatus sensorStatus) {
        updateTaskStatus(taskId, "RUNNING", LocalDateTime.now());
        
        // 如果没有实时数据，记录日志
        if (!sensorStatus.hasRealtimeData) {
            log.warn("⚠️ 无实时传感器数据，仅使用历史数据进行预测");
        }
        
        int total = devices.size();
        int concurrentCount = 5; // 并发数
        Semaphore semaphore = new Semaphore(concurrentCount);
        
        List<CompletableFuture<Void>> futures = devices.stream()
            .map(device -> CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();
                    try {
                        // 预测单个设备（AiService 直接从数据库读取，无需 useVirtualData 参数）
                        predictSingleDevice(device, triggerType, !sensorStatus.hasRealtimeData, taskId);
                        
                        // 更新进度
                        predictionTaskMapper.incrementProgress(taskId, LocalDateTime.now());
                        
                        log.debug("✅ 设备 {} 预测完成", device.getName());
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("预测任务被中断", e);
                }
            }))
            .collect(Collectors.toList());
        
        // 等待所有完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS); // 最多等 60 秒
            
            updateTaskStatus(taskId, "COMPLETED", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("❌ 批量预测失败：{}", e.getMessage(), e);
            updateTaskStatus(taskId, "FAILED", LocalDateTime.now());
            throw new RuntimeException("批量预测失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 预测单个设备
     */
    private void predictSingleDevice(LabDevice device, String triggerType, boolean noRealtimeData, String taskId) {
        try {
            // 不再要求实时数据，所有预测都使用历史数据
            boolean requireRealtimeData = false;
            
            // 调用 AI 服务预测
            Map<String, Object> result = aiService.faultPredictWithVirtualData(device.getId(), requireRealtimeData);
            
            // 如果是定时预测且没有实时数据，添加提示
            if ("SCHEDULED".equals(triggerType) && noRealtimeData) {
                String originalMessage = (String) result.get("message");
                if (originalMessage != null && !originalMessage.contains("无实时数据")) {
                    result.put("message", originalMessage + "（基于历史数据预测）");
                }
            }
            
            // 保存预测结果
            savePredictionResult(device.getLabId(), device, result, triggerType, taskId);
            
            log.info("✅ 设备 {} 预测完成：{}", device.getName(), result.get("message"));
            
        } catch (Exception e) {
            log.error("❌ 设备 {} 预测失败：{}", device.getName(), e.getMessage());
        }
    }
    
    /**
     * 创建预测任务
     */
    private PredictionTask createPredictionTask(Long labId, List<LabDevice> devices, String triggerType) {
        String taskId = UUID.randomUUID().toString();
        int estimatedTime = calculateEstimatedTime(devices.size());
        
        PredictionTask task = new PredictionTask();
        task.setTaskId(taskId);
        task.setLabId(labId);
        task.setTotalDevices(devices.size());
        task.setProcessedDevices(0);
        task.setStatus("PENDING");
        task.setEstimatedTime(estimatedTime);
        task.setStartTime(LocalDateTime.now());
        task.setTriggerType(triggerType);
        
        predictionTaskMapper.insert(task);
        
        log.info("📝 创建预测任务：{}, 设备数：{}, 预估时间：{}秒", 
            taskId, devices.size(), estimatedTime);
        
        return task;
    }
    
    /**
     * 计算预估时间
     */
    private int calculateEstimatedTime(int deviceCount) {
        int concurrentCount = 5;
        double singleDeviceTime = 0.5; // 秒
        return (int) Math.ceil((double) deviceCount / concurrentCount * singleDeviceTime);
    }
    
    /**
     * 检查是否有传感器数据
     */
    private boolean checkSensorData() {
        try {
            // 查询最近 24 小时内是否有传感器数据
            String sql = "SELECT COUNT(*) FROM sensor_reading WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查传感器数据失败：{}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送数据接收失败通知
     */
    private void sendDataFailureNotification() {
        try {
            log.warn("📢 发送数据接收失败通知给管理员");
            
            // 这里可以集成邮件、短信或系统通知
            // 暂时记录日志，实际项目中可以调用通知服务
            
            // 示例：保存到系统通知表
            String insertNotificationSql = "INSERT INTO system_notification (title, content, type, level, created_at) VALUES (?, ?, ?, ?, ?)";
            jdbcTemplate.update(insertNotificationSql,
                "⚠️ 传感器数据接收失败",
                "每日故障预测任务未接收到传感器数据，已使用虚拟数据进行预测。请检查传感器配置和网络连接。",
                "WARNING",
                "HIGH",
                LocalDateTime.now()
            );
            
            log.info("✅ 数据接收失败通知已发送");
        } catch (Exception e) {
            log.error("发送通知失败：{}", e.getMessage());
        }
    }
    
    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, String status, LocalDateTime time) {
        predictionTaskMapper.updateTaskStatus(taskId, status, time);
    }
    
    /**
     * 获取启用的设备
     */
    private List<LabDevice> getEnabledDevices() {
        LambdaQueryWrapper<LabDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabDevice::getStatus, "可用"); // 只预测可用设备
        return deviceMapper.selectList(wrapper);
    }
    
    /**
     * 获取实验室设备
     */
    private List<LabDevice> getLabDevices(Long labId) {
        LambdaQueryWrapper<LabDevice> wrapper = new LambdaQueryWrapper<>();
        if (labId != null) {
            // 如果指定了实验室 ID，只查询该实验室的设备
            wrapper.eq(LabDevice::getLabId, labId);
        }
        // 如果 labId 为 null，查询所有设备
        return deviceMapper.selectList(wrapper);
    }
    
    /**
     * 保存预测结果到 ai_fault_prediction 表
     */
    private void savePredictionResult(Long labId, LabDevice device, Map<String, Object> result, String triggerType, String taskId) {
        try {
            // 检查是否有错误信息（如"无传感器数据"）
            String error = (String) result.get("error");
            if (error != null && !error.isEmpty()) {
                // 保存错误信息到数据库
                String insertSql = "INSERT INTO ai_fault_prediction (task_id, lab_id, device_id, device_name, trigger_type, fault_probability, threshold, is_faulty, predict_result, message, suggestion, predict_time, status, deleted, create_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                jdbcTemplate.update(insertSql, 
                    taskId,
                    labId,
                    device.getId(),
                    device.getName(),
                    triggerType,
                    0.0,  // fault_probability
                    0.0,  // threshold
                    0,    // is_faulty
                    "预测失败", // predict_result
                    error, // message - 保存错误信息
                    "请先添加传感器数据", // suggestion
                    LocalDateTime.now(),
                    "NEW",
                    0,
                    LocalDateTime.now()
                );
                
                log.warn("⚠️ 设备 {} 预测失败：{}", device.getName(), error);
                return;
            }
            
            // 正常预测结果
            String insertSql = "INSERT INTO ai_fault_prediction (task_id, lab_id, device_id, device_name, trigger_type, fault_probability, threshold, is_faulty, predict_result, message, suggestion, predict_time, status, deleted, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            // Python AI 服务返回的字段是 faultProbability，需要正确读取
            Double faultProb = result.get("faultProbability") != null 
                ? ((Number) result.get("faultProbability")).doubleValue() 
                : 0.0;
            Double threshold = result.get("threshold") != null 
                ? ((Number) result.get("threshold")).doubleValue() 
                : 0.0;
            
            // 根据故障概率判断是否故障（概率 >= 阈值）
            boolean isFaulty = faultProb >= threshold;
            
            jdbcTemplate.update(insertSql, 
                taskId,
                labId,
                device.getId(),
                device.getName(),
                triggerType,
                faultProb,
                threshold,
                isFaulty ? 1 : 0,
                isFaulty ? "疑似故障" : "正常",
                result.get("message"),
                result.get("suggestion"),
                LocalDateTime.now(),
                "NEW",
                0,
                LocalDateTime.now()
            );
            
            log.debug("✅ 保存设备 {} 预测结果，触发类型：{}", device.getName(), triggerType);
            
        } catch (Exception e) {
            log.error("❌ 保存预测结果失败：{}", e.getMessage(), e);
            throw new RuntimeException("保存预测结果失败", e);
        }
    }
}
