package com.lab.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.entity.AiModelConfig;
import com.lab.entity.AiSafetyAlert;
import com.lab.entity.Camera;
import com.lab.entity.LabReserve;
import com.lab.mapper.AiSafetyAlertMapper;
import com.lab.service.AiModelConfigService;
import com.lab.service.CameraService;
import com.lab.service.LabReserveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全检测定时任务
 */
@Component
@Slf4j
public class SafetyDetectionScheduler {

    private final CameraService cameraService;
    private final AiModelConfigService aiModelConfigService;
    private final LabReserveService labReserveService;
    private final AiSafetyAlertMapper aiSafetyAlertMapper;

    public SafetyDetectionScheduler(CameraService cameraService, 
                                   AiModelConfigService aiModelConfigService, 
                                   LabReserveService labReserveService,
                                   AiSafetyAlertMapper aiSafetyAlertMapper) {
        this.cameraService = cameraService;
        this.aiModelConfigService = aiModelConfigService;
        this.labReserveService = labReserveService;
        this.aiSafetyAlertMapper = aiSafetyAlertMapper;
    }

    /**
     * 定时执行安全检测
     * 每10秒检查一次是否需要执行检测
     */
    @Scheduled(fixedRate = 10000) // 每10秒执行一次
    public void scheduledSafetyDetection() {
        try {
            // 检查是否在工作时间（有预约的实验室）
            boolean isWorkingTime = isWorkingTime();
            
            // 获取检测间隔配置
            int interval = getDetectionInterval(isWorkingTime);
            
            // 检查是否需要执行检测（基于上次执行时间）
            if (shouldExecuteDetection(interval)) {
                log.info("开始执行安全检测...");
                Map<String, Object> result = cameraService.detectCamerasByReservation();
                log.info("安全检测完成，检测了{}个摄像头", result.get("total"));
                
                // 更新上次执行时间
                updateLastExecutionTime();
                
                // 保存每日安全记录（如果是今天第一次检测）
                saveDailySafetyRecord();
            }
        } catch (Exception e) {
            log.error("定时安全检测失败: {}", e.getMessage());
        }
    }

    /**
     * 定时清理昨天的安全记录
     * 每天凌晨 0 点 1 分执行，确保数据清理准时进行
     */
    @Scheduled(cron = "0 1 0 * * ?") // 每天凌晨 0:01 执行
    public void scheduledCleanupSafetyRecords() {
        try {
            log.info("开始执行定时清理任务...");
            cleanupYesterdaySafetyRecords(LocalDate.now());
            log.info("定时清理任务执行完成");
        } catch (Exception e) {
            log.error("定时清理安全记录失败：{}", e.getMessage());
        }
    }

    /**
     * 检查是否在工作时间
     * 工作时间：是否有实验室有预约
     */
    private boolean isWorkingTime() {
        try {
            // 获取当前日期和时间
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            // 查询当天的所有预约
            List<LabReserve> reserves = labReserveService.lambdaQuery()
                    .eq(LabReserve::getReserveDate, today)
                    .eq(LabReserve::getStatus, "APPROVED") // 只考虑已批准的预约
                    .list();
            
            // 检查当前时间是否在任何预约的时间范围内
            for (LabReserve reserve : reserves) {
                String timeSlotStart = reserve.getTimeSlotStart();
                String timeSlotEnd = reserve.getTimeSlotEnd();
                
                if (timeSlotStart != null && timeSlotEnd != null) {
                    try {
                        LocalTime startTime = LocalTime.parse(timeSlotStart, timeFormatter);
                        LocalTime endTime = LocalTime.parse(timeSlotEnd, timeFormatter);
                        
                        // 检查当前时间是否在预约时间范围内
                        if (!now.isBefore(startTime) && !now.isAfter(endTime)) {
                            log.info("当前时间在预约时段内: {} - {}", timeSlotStart, timeSlotEnd);
                            return true;
                        }
                    } catch (Exception e) {
                        log.error("解析预约时间失败: {}", e.getMessage());
                    }
                }
            }
            
            // 没有找到当前时间的预约
            log.debug("当前时间不在任何预约时段内");
            return false;
        } catch (Exception e) {
            log.error("判断工作时间失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取检测间隔（秒）
     */
    private int getDetectionInterval(boolean isWorkingTime) {
        String key = isWorkingTime ? "detection_interval_working" : "detection_interval_non_working";
        
        // 从数据库获取配置
        AiModelConfig config = aiModelConfigService.lambdaQuery()
                .eq(AiModelConfig::getModelType, "safety")
                .eq(AiModelConfig::getParamKey, key)
                .one();
        
        if (config != null && config.getParamValue() != null) {
            try {
                return Integer.parseInt(config.getParamValue());
            } catch (NumberFormatException e) {
                log.error("解析检测间隔配置失败: {}", e.getMessage());
            }
        }
        
        // 默认值：工作时间20秒，非工作时间5分钟
        return isWorkingTime ? 20 : 300;
    }

    // 上次执行时间
    private LocalDateTime lastExecutionTime = null;

    /**
     * 检查是否需要执行检测
     */
    private boolean shouldExecuteDetection(int interval) {
        try {
            if (lastExecutionTime == null) {
                // 首次执行
                return true;
            }
            
            // 计算上次执行到现在的时间间隔（秒）
            long secondsSinceLastExecution = java.time.Duration.between(lastExecutionTime, LocalDateTime.now()).getSeconds();
            
            // 如果时间间隔大于等于配置的检测间隔，则执行检测
            return secondsSinceLastExecution >= interval;
        } catch (Exception e) {
            log.error("判断是否执行检测失败: {}", e.getMessage());
            return true; // 出错时默认执行检测
        }
    }

    /**
     * 更新上次执行时间
     */
    private void updateLastExecutionTime() {
        lastExecutionTime = LocalDateTime.now();
        log.info("更新安全检测执行时间: {}", lastExecutionTime);
    }

    /**
     * 检查指定实验室是否在工作时间
     * 工作时间：该实验室的预约时段
     */
    private boolean isLabWorkingTime(Long labId) {
        try {
            // 获取当前日期和时间
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            // 查询指定实验室当天的预约
            List<LabReserve> reserves = labReserveService.lambdaQuery()
                    .eq(LabReserve::getLabId, labId)
                    .eq(LabReserve::getReserveDate, today)
                    .eq(LabReserve::getStatus, "APPROVED") // 只考虑已批准的预约
                    .list();
            
            // 检查当前时间是否在任何预约的时间范围内
            for (LabReserve reserve : reserves) {
                String timeSlotStart = reserve.getTimeSlotStart();
                String timeSlotEnd = reserve.getTimeSlotEnd();
                
                if (timeSlotStart != null && timeSlotEnd != null) {
                    try {
                        LocalTime startTime = LocalTime.parse(timeSlotStart, timeFormatter);
                        LocalTime endTime = LocalTime.parse(timeSlotEnd, timeFormatter);
                        
                        // 检查当前时间是否在预约时间范围内
                        if (!now.isBefore(startTime) && !now.isAfter(endTime)) {
                            log.info("实验室{}当前时间在预约时段内: {} - {}", labId, timeSlotStart, timeSlotEnd);
                            return true;
                        }
                    } catch (Exception e) {
                        log.error("解析预约时间失败: {}", e.getMessage());
                    }
                }
            }
            
            // 没有找到当前时间的预约
            log.debug("实验室{}当前时间不在任何预约时段内", labId);
            return false;
        } catch (Exception e) {
            log.error("判断实验室{}工作时间失败：{}", labId, e.getMessage());
            return false;
        }
    }
    
    // 记录每日安全记录状态（按实验室分别跟踪）
    private LocalDate lastSafetyRecordDate = null;
    private Map<Long, Boolean> labStartRecorded = new HashMap<>(); // 每个实验室开始记录是否已保存
    private Map<Long, Boolean> labEndRecorded = new HashMap<>(); // 每个实验室结束记录是否已保存
    
    /**
     * 保存每日安全记录
     * 每个实验室开放时段开始时记录一次，结束时记录一次
     * 非危险记录只保留开放开始和结束时间的记录
     * 危险记录永久保留
     */
    private void saveDailySafetyRecord() {
        try {
            LocalDate today = LocalDate.now();
            
            // 如果是新的一天，重置所有实验室的状态
            if (!today.equals(lastSafetyRecordDate)) {
                lastSafetyRecordDate = today;
                labStartRecorded.clear();
                labEndRecorded.clear();
                log.info("新的一天开始，重置所有实验室安全记录状态：{}", today);
                
                // 清理昨天的非危险记录，只保留开放开始和结束时间的记录
                cleanupYesterdaySafetyRecords(today);
            }
            
            // 获取今天所有已批准的预约
            List<LabReserve> reserves = labReserveService.lambdaQuery()
                    .eq(LabReserve::getReserveDate, today)
                    .eq(LabReserve::getStatus, "APPROVED")
                    .list();
            
            LocalTime now = LocalTime.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            // 遍历每个预约
            for (LabReserve reserve : reserves) {
                Long labId = reserve.getLabId();
                String timeSlotStart = reserve.getTimeSlotStart();
                String timeSlotEnd = reserve.getTimeSlotEnd();
                
                if (timeSlotStart == null || timeSlotEnd == null) {
                    continue;
                }
                
                LocalTime startTime = LocalTime.parse(timeSlotStart, timeFormatter);
                LocalTime endTime = LocalTime.parse(timeSlotEnd, timeFormatter);
                
                // 初始化该实验室的状态
                labStartRecorded.putIfAbsent(labId, false);
                labEndRecorded.putIfAbsent(labId, false);
                
                // 检查是否在开放时段开始时间（允许前后 1 分钟误差）
                boolean isStartTime = !now.isBefore(startTime.minusMinutes(1)) && now.isBefore(startTime.plusMinutes(1));
                
                // 检查是否在开放时段结束时间（允许前后 1 分钟误差）
                boolean isEndTime = !now.isBefore(endTime.minusMinutes(1)) && now.isBefore(endTime.plusMinutes(1));
                
                // 开放时段开始，记录一次
                if (isStartTime && !labStartRecorded.get(labId)) {
                    log.info("实验室{}开放时段开始，执行安全检测：{} - {}", labId, timeSlotStart, timeSlotEnd);
                    detectAndSaveSafetyRecord(labId, "START");
                    labStartRecorded.put(labId, true);
                }
                
                // 开放时段结束，记录一次
                if (isEndTime && !labEndRecorded.get(labId)) {
                    log.info("实验室{}开放时段结束，执行安全检测：{} - {}", labId, timeSlotStart, timeSlotEnd);
                    detectAndSaveSafetyRecord(labId, "END");
                    labEndRecorded.put(labId, true);
                }
            }
        } catch (Exception e) {
            log.error("保存每日安全记录失败：{}", e.getMessage());
        }
    }
    
    /**
     * 清理昨天的安全记录
     * 保留：危险记录、开放开始记录、开放结束记录
     * 删除：其他非危险记录
     */
    private void cleanupYesterdaySafetyRecords(LocalDate today) {
        try {
            LocalDate yesterday = today.minusDays(1);
            LocalDateTime yesterdayStart = yesterday.atStartOfDay();
            LocalDateTime yesterdayEnd = yesterday.atTime(23, 59, 59);
            
            log.info("开始清理昨天的安全记录：{} 到 {}", yesterdayStart, yesterdayEnd);
            
            // 获取昨天的所有安全记录
            List<AiSafetyAlert> yesterdayRecords = aiSafetyAlertMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiSafetyAlert>()
                    .between(AiSafetyAlert::getAlertTime, yesterdayStart, yesterdayEnd)
            );
            
            log.info("昨天共有 {} 条安全记录", yesterdayRecords.size());
            
            // 统计需要保留和删除的记录
            int keepCount = 0;
            int deleteCount = 0;
            
            for (AiSafetyAlert record : yesterdayRecords) {
                boolean shouldKeep = false;
                
                // 1. 危险记录永久保留
                if ("SAFETY_HAZARD".equals(record.getAlertType()) || 
                    record.getDescription().contains("危险") ||
                    record.getDescription().contains("检测到")) {
                    shouldKeep = true;
                    log.info("保留危险记录：ID={}, 描述={}", record.getId(), record.getDescription());
                }
                
                // 2. 开放开始/结束记录保留
                if ("DAILY_SAFETY_CHECK".equals(record.getAlertType()) &&
                    (record.getDescription().contains("开放时段开始") || 
                     record.getDescription().contains("开放时段结束"))) {
                    shouldKeep = true;
                    log.info("保留日常检查记录：ID={}, 描述={}", record.getId(), record.getDescription());
                }
                
                // 3. 摄像头错误记录保留（用于追踪设备问题）
                if ("CAMERA_ERROR".equals(record.getAlertType())) {
                    shouldKeep = true;
                    log.info("保留摄像头错误记录：ID={}, 描述={}", record.getId(), record.getDescription());
                }
                
                if (shouldKeep) {
                    keepCount++;
                } else {
                    // 删除非危险、非关键的记录
                    aiSafetyAlertMapper.deleteById(record.getId());
                    deleteCount++;
                    log.info("删除普通记录：ID={}, 描述={}", record.getId(), record.getDescription());
                }
            }
            
            log.info("清理完成！保留 {} 条，删除 {} 条", keepCount, deleteCount);
            
        } catch (Exception e) {
            log.error("清理安全记录失败：{}", e.getMessage());
        }
    }
    
    /**
     * 执行安全检测并保存记录
     * @param labId 实验室 ID
     * @param recordType 记录类型：START-开放开始，END-开放结束
     */
    private void detectAndSaveSafetyRecord(Long labId, String recordType) {
        try {
            // 获取该实验室的摄像头
            List<Camera> cameras = cameraService.listByLabId(labId, 1);
            
            if (cameras == null || cameras.isEmpty()) {
                log.warn("实验室{}没有启用的摄像头", labId);
                // 记录无摄像头
                saveSafetyAlert(labId, "摄像头请求失败：实验室未配置摄像头", "CAMERA_ERROR");
                return;
            }
            
            // 检测每个摄像头
            boolean allSuccess = true;
            for (Camera camera : cameras) {
                try {
                    String result = cameraService.detectCamera(camera.getCameraCode());
                    log.info("实验室{}摄像头{}检测成功", labId, camera.getCameraCode());
                } catch (Exception e) {
                    log.error("实验室{}摄像头{}检测失败：{}", labId, camera.getCameraCode(), e.getMessage());
                    allSuccess = false;
                }
            }
            
            // 根据检测结果保存记录
            if (allSuccess) {
                if ("START".equals(recordType)) {
                    saveSafetyAlert(labId, "实验室开放时段开始：安全检测正常", "DAILY_SAFETY_CHECK");
                } else if ("END".equals(recordType)) {
                    saveSafetyAlert(labId, "实验室开放时段结束：安全检测正常", "DAILY_SAFETY_CHECK");
                }
            } else {
                saveSafetyAlert(labId, "摄像头请求失败：部分摄像头检测异常", "CAMERA_ERROR");
            }
        } catch (Exception e) {
            log.error("执行安全检测失败：{}", e.getMessage());
            saveSafetyAlert(labId, "摄像头请求失败：" + e.getMessage(), "CAMERA_ERROR");
        }
    }
    
    /**
     * 保存安全告警记录
     */
    private void saveSafetyAlert(Long labId, String description, String alertType) {
        try {
            // 调用 CameraService 保存记录
            cameraService.saveDailySafetyRecord(labId, description, alertType);
        } catch (Exception e) {
            log.error("保存安全告警失败：{}", e.getMessage());
        }
    }
}