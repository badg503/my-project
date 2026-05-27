package com.lab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.Result;
import com.lab.entity.PredictionTask;
import com.lab.mapper.PredictionTaskMapper;
import com.lab.service.AiService;
import com.lab.service.PredictionTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能模块接口：问答助手（知识库+BERT 占位）、故障预测/安全检测/预约调度等可在此扩展或对接 Python 服务。
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final PredictionTaskService predictionTaskService;
    private final PredictionTaskMapper predictionTaskMapper;

    public AiController(AiService aiService, PredictionTaskService predictionTaskService, PredictionTaskMapper predictionTaskMapper) {
        this.aiService = aiService;
        this.predictionTaskService = predictionTaskService;
        this.predictionTaskMapper = predictionTaskMapper;
    }

    @PostMapping("/qa")
    public Result<Map<String, String>> qa(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String answer = aiService.qa(question);
        Map<String, String> data = new HashMap<>();
        data.put("answer", answer);
        return Result.ok(data);
    }

    /** 设备故障预测：内置占位；ai.enabled=true 时转发至 Python LSTM 服务 */
    @GetMapping("/fault-predict")
    public Result<Map<String, Object>> faultPredict(@RequestParam Long deviceId) {
        return Result.ok(aiService.faultPredict(deviceId));
    }

    /** 批量故障预测：用于实验室设备管理（可选实验室，不传则预测所有设备） */
    @PostMapping("/batch-fault-predict")
    public Result<Map<String, Object>> batchFaultPredict(@RequestBody(required = false) Map<String, Object> body) {
        log.info("🔍 收到批量故障预测请求，body: {}", body);
        
        try {
            Long labId = null;
            if (body != null && body.containsKey("labId") && body.get("labId") != null) {
                labId = Long.valueOf(body.get("labId").toString());
                log.info("📍 指定实验室 ID: {}", labId);
            } else {
                log.info("📍 未指定实验室，将预测所有设备");
            }
            
            // 启动预测任务
            PredictionTask task = predictionTaskService.manualPredict(labId);
            log.info("✅ 预测任务创建成功，taskId: {}, 设备数：{}", task.getTaskId(), task.getTotalDevices());
            
            // 返回任务信息
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getTaskId());
            result.put("deviceCount", task.getTotalDevices());
            result.put("estimatedTime", task.getEstimatedTime());
            
            String scope = labId != null ? "该实验室" : "所有";
            result.put("message", String.format(
                "已启动预测任务，共%d台设备，预计需要%d秒", 
                task.getTotalDevices(), task.getEstimatedTime()
            ));
            result.put("scope", scope);
            
            log.info("📤 返回结果：{}", result);
            return Result.ok(result);
        } catch (Exception e) {
            log.error("❌ 批量故障预测失败", e);
            return Result.fail(e.getMessage());
        }
    }
    
    /** 查询预测任务进度 */
    @GetMapping("/prediction-progress/{taskId}")
    public Result<Map<String, Object>> getPredictionProgress(@PathVariable String taskId) {
        PredictionTask task = predictionTaskService.getTaskProgress(taskId);
        
        if (task == null) {
            return Result.fail("任务不存在");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("status", task.getStatus());
        result.put("totalDevices", task.getTotalDevices());
        result.put("processedDevices", task.getProcessedDevices());
        result.put("progress", task.getTotalDevices() > 0 
            ? (int)(task.getProcessedDevices() * 100.0 / task.getTotalDevices()) 
            : 0);
        result.put("estimatedTime", task.getEstimatedTime());
        
        // 计算已用时间
        LocalDateTime startTime = task.getStartTime();
        LocalDateTime endTime = task.getEndTime() != null ? task.getEndTime() : LocalDateTime.now();
        long elapsedTime = Duration.between(startTime, endTime).getSeconds();
        result.put("elapsedTime", elapsedTime);
        
        // 计算剩余时间
        if ("RUNNING".equals(task.getStatus())) {
            int remainingTime = Math.max(0, task.getEstimatedTime() - (int)elapsedTime);
            result.put("remainingTime", remainingTime);
        }
        
        return Result.ok(result);
    }

    /** 安全检测：内置占位；ai.enabled=true 时转发至 Python YOLOv8 服务 */
    @GetMapping("/safety-detect")
    public Result<Map<String, Object>> safetyDetect(@RequestParam Long labId) {
        return Result.ok(aiService.safetyDetect(labId));
    }

    /** 获取 BERT 问答相似度阈值 */
    @GetMapping("/qa-threshold")
    public Result<Map<String, Object>> getQaThreshold() {
        return Result.ok(aiService.getBertThreshold());
    }

    /** 更新 BERT 问答相似度阈值 */
    @PostMapping("/qa-threshold")
    public Result<Map<String, Object>> updateQaThreshold(@RequestBody Map<String, Object> params) {
        Double threshold = (Double) params.get("threshold");
        return Result.ok(aiService.setBertThreshold(threshold));
    }

    /** 获取预测结果（分页，支持按触发类型和日期筛选） */
    @GetMapping("/prediction-results")
    public Result<Map<String, Object>> getPredictionResults(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) String triggerType,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        
        try {
            // 调用 service 查询预测结果
            Map<String, Object> result = aiService.getPredictionResults(labId, triggerType, date, current, size);
            return Result.ok(result);
            
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }
    
    /** 获取当天的预测结果（不分实验室，用于设备管理页面） */
    @GetMapping("/today-predictions")
    public Result<Map<String, Object>> getTodayPredictions() {
        try {
            // 获取今天的日期字符串
            String today = java.time.LocalDate.now().toString();
            
            // 查询今天的所有预测任务
            List<PredictionTask> todayTasks = predictionTaskMapper.selectList(
                new LambdaQueryWrapper<PredictionTask>()
                    .eq(PredictionTask::getStatus, "COMPLETED")
                    .ge(PredictionTask::getStartTime, java.time.LocalDateTime.now().toLocalDate().atStartOfDay())
                    .orderByDesc(PredictionTask::getStartTime)
            );
            
            // 返回今天的预测任务列表
            Map<String, Object> result = new HashMap<>();
            result.put("tasks", todayTasks);
            result.put("count", todayTasks.size());
            result.put("date", today);
            
            return Result.ok(result);
            
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    /** AI 数据分析（Prophet）- 返回统计数据、预测和建议 */
    @GetMapping("/analysis")
    public Result<Map<String, Object>> getAIAnalysis() {
        try {
            Map<String, Object> result = aiService.getAIAnalysis();
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("AI 分析失败：" + e.getMessage());
        }
    }

    /** AI 智能预约调度建议 */
    @RequestMapping(value = "/schedule", method = {RequestMethod.GET, RequestMethod.POST})
    public Result<Map<String, Object>> getSchedule() {
        try {
            Map<String, Object> result = aiService.schedule();
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("预约调度分析失败：" + e.getMessage());
        }
    }
}
