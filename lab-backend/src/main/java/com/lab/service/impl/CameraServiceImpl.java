package com.lab.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.entity.Camera;
import com.lab.entity.CameraLog;
import com.lab.entity.AiSafetyAlert;
import com.lab.entity.LabInfo;
import com.lab.mapper.CameraMapper;
import com.lab.mapper.LabInfoMapper;
import com.lab.mapper.AiSafetyAlertMapper;
import com.lab.service.CameraService;
import com.lab.service.LabReserveService;
import com.lab.service.EmailService;
import com.lab.service.SysUserService;
import com.lab.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 摄像头服务实现类
 */
@Service
@Slf4j
public class CameraServiceImpl implements CameraService {
    
    @Autowired
    private CameraMapper cameraMapper;
    
    @Autowired
    private AiSafetyAlertMapper aiSafetyAlertMapper;
    
    @Autowired
    private LabInfoMapper labInfoMapper;
    
    @Autowired
    private LabReserveService labReserveService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SysUserService sysUserService;
    
    @Value("${ai.base-url}")
    private String aiServiceUrl;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // ==================== 摄像头管理 ====================
    
    @Override
    public List<Camera> listCameras(Integer enabled) {
        return cameraMapper.selectAll(enabled);
    }
    
    @Override
    public List<Camera> listByLabId(Long labId, Integer enabled) {
        return cameraMapper.selectByLabId(labId, enabled);
    }
    
    @Override
    public Camera getByCode(String cameraCode) {
        return cameraMapper.selectByCode(cameraCode);
    }
    
    @Override
    public void addCamera(Camera camera) {
        // 检查编码是否已存在
        if (cameraMapper.selectByCode(camera.getCameraCode()) != null) {
            throw new RuntimeException("摄像头编码已存在");
        }
        
        // 设置默认值
        if (camera.getEnabled() == null) {
            camera.setEnabled(1);
        }
        if (camera.getStatus() == null) {
            camera.setStatus("ONLINE");
        }
        if (camera.getAlertThreshold() == null) {
            camera.setAlertThreshold(0.5);
        }
        if (camera.getConfidence() == null) {
            camera.setConfidence(0.4);
        }
        if (camera.getDangerClasses() == null) {
            camera.setDangerClasses("[\"person\",\"fire\",\"knife\"]");
        }
        
        cameraMapper.insert(camera);
        log.info("添加摄像头: {}", camera.getCameraCode());
    }
    
    @Override
    public void updateCamera(String cameraCode, Camera camera) {
        // 检查摄像头是否存在
        Camera existing = cameraMapper.selectByCode(cameraCode);
        if (existing == null) {
            throw new RuntimeException("摄像头不存在");
        }
        
        // 设置编码
        camera.setCameraCode(cameraCode);
        cameraMapper.update(camera);
        log.info("更新摄像头: {}", cameraCode);
    }
    
    @Override
    public void deleteCamera(String cameraCode) {
        int count = cameraMapper.deleteByCode(cameraCode);
        if (count == 0) {
            throw new RuntimeException("摄像头不存在");
        }
        log.info("删除摄像头: {}", cameraCode);
    }
    
    @Override
    public void updateStatus(String cameraCode, String status) {
        cameraMapper.updateStatus(cameraCode, status);
        log.info("更新摄像头状态: {} -> {}", cameraCode, status);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 根据实验室ID获取实验室名称
     */
    private String getLabName(Long labId) {
        if (labId == null) {
            return "未知实验室";
        }
        try {
            LabInfo labInfo = labInfoMapper.selectById(labId);
            return labInfo != null ? labInfo.getName() : "未知实验室";
        } catch (Exception e) {
            log.error("获取实验室名称失败: {}", e.getMessage());
            return "未知实验室";
        }
    }
    
    // ==================== 检测记录 ====================
    
    @Override
    public void addDetectionLog(CameraLog log) {
        // 这里不再使用camera_log表，而是使用ai_safety_alert表
        this.log.info("添加检测记录: 摄像头ID={}", log.getCameraId());
    }
    
    @Override
    public List<Map<String, Object>> getLogs(String cameraCode, String startTime, String endTime, Integer hasAlert) {
        // 这里不再使用camera_log表，而是使用ai_safety_alert表
        return new ArrayList<>();
    }
    
    @Override
    public void processAlert(Long logId, Long userId, String remark) {
        // 这里不再使用camera_log表，而是使用ai_safety_alert表
        log.info("处理告警: 记录ID={}, 处理人={}", logId, userId);
    }
    
    // ==================== AI集成 ====================
    
    @Override
    public String detectCamera(String cameraCode) {
        log.info("开始检测摄像头：{}", cameraCode);
        Camera camera = getByCode(cameraCode);
        log.info("获取摄像头信息：{}", camera != null ? camera.getCameraName() : "null");
        if (camera == null) {
            throw new RuntimeException("摄像头不存在");
        }
        
        if (camera.getEnabled() == 0) {
            throw new RuntimeException("摄像头已禁用");
        }
        
        try {
            log.info("开始构建摄像头配置");
            // 构建摄像头配置
            Map<String, Object> cameraConfig = new HashMap<>();
            cameraConfig.put("code", camera.getCameraCode());
            cameraConfig.put("name", camera.getCameraName());
            cameraConfig.put("type", camera.getCameraType());
            cameraConfig.put("url", camera.getCameraUrl());
            cameraConfig.put("location", camera.getLocation());
            cameraConfig.put("description", camera.getDescription());
            cameraConfig.put("lab_id", camera.getLabId());  // 添加实验室 ID
            log.info("获取实验室名称，labId={}", camera.getLabId());
            String labName = getLabName(camera.getLabId());
            log.info("实验室名称：{}", labName);
            cameraConfig.put("lab_name", labName);  // 添加实验室名称
            log.info("解析 dangerClasses: {}", camera.getDangerClasses());
            List<String> dangerClasses;
            if (camera.getDangerClasses() == null || camera.getDangerClasses().trim().isEmpty()) {
                // 使用默认值（移除了 person，只保留真正的危险物品）
                dangerClasses = Arrays.asList("fire", "smoke", "knife", "gun", "lighter", "match");
                log.info("dangerClasses 为空，使用默认值：{}", dangerClasses);
            } else {
                dangerClasses = objectMapper.readValue(camera.getDangerClasses(), List.class);
            }
            cameraConfig.put("danger_classes", dangerClasses);
            cameraConfig.put("alert_threshold", camera.getAlertThreshold());
            cameraConfig.put("confidence", camera.getConfidence());
            log.info("摄像头配置构建完成");
            
            // 调用 AI 服务（POST 方式传递配置）
            String url = aiServiceUrl + "/ai/safety-detect";
            log.info("正在调用 AI 服务：{}", url);
            log.info("摄像头配置：{}", cameraConfig);
            
            String jsonData = objectMapper.writeValueAsString(cameraConfig);
            log.info("发送的 JSON 数据：{}", jsonData);
            
            String response = null;
            try {
                response = HttpClientUtil.post(url, jsonData);
                log.info("AI 服务响应：{}", response);
            } catch (Exception e) {
                log.error("HttpClientUtil.post 抛出异常：{}", e.getMessage(), e);
                throw e;
            }
            
            if (response == null || response.trim().isEmpty()) {
                log.error("AI 服务返回空响应");
                throw new RuntimeException("AI 服务返回空响应");
            }
            
            // 解析响应
            Map<String, Object> result = objectMapper.readValue(response, Map.class);
            
            // 获取照片路径信息
            String safePhotoPath = (String) result.get("safePhoto");
            List<String> dangerPhotoPaths = (List<String>) result.get("dangerPhotos");
            
            // 保存检测记录到 ai_safety_alert 表
            List<Map<String, Object>> dangerObjects = (List<Map<String, Object>>) result.get("dangerObjects");
            if (dangerObjects != null && !dangerObjects.isEmpty()) {
                // 有危险物体，创建告警记录
                for (Map<String, Object> dangerObject : dangerObjects) {
                    AiSafetyAlert alert = new AiSafetyAlert();
                    alert.setLabId(camera.getLabId());
                    alert.setLabName(getLabName(camera.getLabId())); // 设置实验室名称
                    alert.setAlertTime(LocalDateTime.now());
                    alert.setAlertType("SAFETY_HAZARD");
                    alert.setDescription("检测到危险物体：" + dangerObject.get("class"));
                    alert.setStatus("PENDING");
                    
                    // 保存第一张危险照片路径
                    if (dangerPhotoPaths != null && !dangerPhotoPaths.isEmpty()) {
                        alert.setImageUrl(dangerPhotoPaths.get(0));
                    }
                    
                    // 保存告警记录到数据库
                    aiSafetyAlertMapper.insert(alert);
                    log.info("创建安全告警：{}, 照片：{}", alert.getDescription(), alert.getImageUrl());
                    
                    // 发送邮件通知管理员
                    sendAlertEmailToAdmins(alert.getLabName(), alert.getAlertType(), 
                                          alert.getDescription(), alert.getAlertTime());
                }
            } else {
                // 无危险物体，保存正常检测记录
                AiSafetyAlert alert = new AiSafetyAlert();
                alert.setLabId(camera.getLabId());
                alert.setLabName(getLabName(camera.getLabId()));
                alert.setAlertTime(LocalDateTime.now());
                alert.setAlertType("REALTIME_CHECK");
                alert.setDescription("实时安全检测：未发现安全隐患");
                alert.setStatus("COMPLETED");
                
                // 保存安全照片路径
                if (safePhotoPath != null && !safePhotoPath.isEmpty()) {
                    alert.setImageUrl(safePhotoPath);
                }
                
                // 保存记录到数据库
                aiSafetyAlertMapper.insert(alert);
                log.info("保存实时检测记录：实验室{} - 未发现安全隐患，照片：{}", camera.getLabId(), alert.getImageUrl());
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("检测摄像头失败：{}", e.getMessage());
            updateStatus(cameraCode, "ERROR");
            throw new RuntimeException("检测失败：" + e.getMessage());
        }
    }
    
    /**
     * 保存每日安全记录
     * 每个实验室开放时段开始和结束时各记录一次
     * @param labId 实验室 ID
     * @param description 描述
     * @param alertType 告警类型
     */
    public void saveDailySafetyRecord(Long labId, String description, String alertType) {
        try {
            // 创建安全记录
            AiSafetyAlert alert = new AiSafetyAlert();
            alert.setLabId(labId);
            alert.setLabName(getLabName(labId));
            alert.setAlertTime(LocalDateTime.now());
            alert.setAlertType(alertType);
            alert.setDescription(description);
            alert.setStatus("COMPLETED");
            
            // 保存记录到数据库
            aiSafetyAlertMapper.insert(alert);
            log.info("保存安全记录：实验室{} - {}", labId, description);
        } catch (Exception e) {
            log.error("保存安全记录失败：{}", e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> detectAllCameras() {
        List<Camera> cameras = listCameras(1); // 只检测启用的摄像头
        Map<String, Object> results = new HashMap<>();
        List<Map<String, Object>> success = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        
        for (Camera camera : cameras) {
            try {
                String result = detectCamera(camera.getCameraCode());
                Map<String, Object> item = new HashMap<>();
                item.put("cameraCode", camera.getCameraCode());
                item.put("cameraName", camera.getCameraName());
                item.put("result", result);
                success.add(item);
            } catch (Exception e) {
                Map<String, Object> item = new HashMap<>();
                item.put("cameraCode", camera.getCameraCode());
                item.put("cameraName", camera.getCameraName());
                item.put("error", e.getMessage());
                failed.add(item);
            }
        }
        
        results.put("success", success);
        results.put("failed", failed);
        results.put("total", cameras.size());
        results.put("successCount", success.size());
        results.put("failedCount", failed.size());
        
        return results;
    }
    
    @Override
    public Map<String, Object> detectCamerasByReservation() {
        List<Camera> cameras = listCameras(1); // 只检测启用的摄像头
        log.info("检测到 {} 个启用的摄像头", cameras.size());
        Map<String, Object> results = new HashMap<>();
        List<Map<String, Object>> success = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        
        for (Camera camera : cameras) {
            log.info("处理摄像头：{} ({})", camera.getCameraName(), camera.getCameraCode());
            try {
                // 检查该摄像头所属的实验室是否有当前时间的预约
                boolean hasReservation = hasActiveReservation(camera.getLabId());
                log.info("摄像头 {} 所属实验室是否有预约：{}", camera.getCameraName(), hasReservation);
                
                if (!hasReservation) {
                    // 没有预约，跳过检测
                    Map<String, Object> item = new HashMap<>();
                    item.put("cameraCode", camera.getCameraCode());
                    item.put("cameraName", camera.getCameraName());
                    item.put("labId", camera.getLabId());
                    item.put("reason", "实验室无当前预约");
                    skipped.add(item);
                    continue;
                }
                
                // 有预约，执行检测
                log.info("开始检测摄像头：{}", camera.getCameraName());
                String result = detectCamera(camera.getCameraCode());
                Map<String, Object> item = new HashMap<>();
                item.put("cameraCode", camera.getCameraCode());
                item.put("cameraName", camera.getCameraName());
                item.put("labId", camera.getLabId());
                item.put("result", result);
                success.add(item);
            } catch (Exception e) {
                Map<String, Object> item = new HashMap<>();
                item.put("cameraCode", camera.getCameraCode());
                item.put("cameraName", camera.getCameraName());
                item.put("labId", camera.getLabId());
                item.put("error", e.getMessage());
                failed.add(item);
            }
        }
        
        results.put("success", success);
        results.put("failed", failed);
        results.put("skipped", skipped);
        results.put("total", cameras.size());
        results.put("successCount", success.size());
        results.put("failedCount", failed.size());
        results.put("skippedCount", skipped.size());
        
        return results;
    }
    
    /**
     * 检查指定实验室是否有当前时间的预约
     */
    private boolean hasActiveReservation(Long labId) {
        try {
            // 获取当前日期和时间
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            // 查询指定实验室当天的预约
            List<com.lab.entity.LabReserve> reserves = labReserveService.lambdaQuery()
                    .eq(com.lab.entity.LabReserve::getLabId, labId)
                    .eq(com.lab.entity.LabReserve::getReserveDate, today)
                    .eq(com.lab.entity.LabReserve::getStatus, "APPROVED") // 只考虑已批准的预约
                    .list();
            
            // 检查当前时间是否在任何预约的时间范围内
            for (com.lab.entity.LabReserve reserve : reserves) {
                String timeSlotStart = reserve.getTimeSlotStart();
                String timeSlotEnd = reserve.getTimeSlotEnd();
                
                if (timeSlotStart != null && timeSlotEnd != null) {
                    try {
                        LocalTime startTime = LocalTime.parse(timeSlotStart, timeFormatter);
                        LocalTime endTime = LocalTime.parse(timeSlotEnd, timeFormatter);
                        
                        // 检查当前时间是否在预约时间范围内
                        if (!now.isBefore(startTime) && !now.isAfter(endTime)) {
                            return true;
                        }
                    } catch (Exception e) {
                        log.error("解析预约时间失败: {}", e.getMessage());
                    }
                }
            }
            
            // 没有找到当前时间的预约
            return false;
        } catch (Exception e) {
            log.error("检查实验室{}预约状态失败：{}", labId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送邮件通知管理员
     * @param labName 实验室名称
     * @param alertType 告警类型
     * @param description 告警描述
     * @param alertTime 告警时间
     */
    private void sendAlertEmailToAdmins(String labName, String alertType, String description, LocalDateTime alertTime) {
        try {
            // 查询所有管理员的邮箱
            List<com.lab.entity.SysUser> admins = sysUserService.list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.lab.entity.SysUser>()
                    .in(com.lab.entity.SysUser::getRole, "SYS_ADMIN", "LAB_ADMIN")
                    .eq(com.lab.entity.SysUser::getStatus, 1));  // 只查询状态正常的用户
            
            if (admins.isEmpty()) {
                log.warn("⚠️ 系统中没有管理员，无法发送安全告警邮件");
                return;
            }
            
            // 给每个管理员发送邮件
            for (com.lab.entity.SysUser admin : admins) {
                if (admin.getEmail() != null && !admin.getEmail().isEmpty()) {
                    log.info("📧 发送安全告警邮件给管理员：{} ({})", admin.getRealName(), admin.getEmail());
                    emailService.sendSafetyAlertEmail(
                        admin.getEmail(),
                        labName,
                        alertType,
                        description,
                        alertTime
                    );
                }
            }
            
            log.info("✅ 安全告警邮件发送完成，共发送 {} 封邮件", admins.size());
        } catch (Exception e) {
            log.error("❌ 发送安全告警邮件失败：{}", e.getMessage());
            // 邮件发送失败不影响正常业务流程
        }
    }
}
