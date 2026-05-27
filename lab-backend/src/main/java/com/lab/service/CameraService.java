package com.lab.service;

import com.lab.entity.Camera;
import com.lab.entity.CameraLog;

import java.util.List;
import java.util.Map;

/**
 * 摄像头服务接口
 */
public interface CameraService {
    
    // ==================== 摄像头管理 ====================
    
    /**
     * 获取所有摄像头
     */
    List<Camera> listCameras(Integer enabled);
    
    /**
     * 根据实验室ID获取摄像头
     */
    List<Camera> listByLabId(Long labId, Integer enabled);
    
    /**
     * 根据编码获取摄像头
     */
    Camera getByCode(String cameraCode);
    
    /**
     * 添加摄像头
     */
    void addCamera(Camera camera);
    
    /**
     * 更新摄像头
     */
    void updateCamera(String cameraCode, Camera camera);
    
    /**
     * 删除摄像头
     */
    void deleteCamera(String cameraCode);
    
    /**
     * 更新摄像头状态
     */
    void updateStatus(String cameraCode, String status);
    
    // ==================== 检测记录 ====================
    
    /**
     * 添加检测记录
     */
    void addDetectionLog(CameraLog log);
    
    /**
     * 获取检测记录
     */
    List<Map<String, Object>> getLogs(String cameraCode, String startTime, String endTime, Integer hasAlert);
    
    /**
     * 处理告警
     */
    void processAlert(Long logId, Long userId, String remark);
    
    // ==================== AI集成 ====================
    
    /**
     * 触发摄像头检测
     */
    String detectCamera(String cameraCode);
    
    /**
     * 批量检测所有摄像头
     */
    Map<String, Object> detectAllCameras();
    
    /**
     * 根据预约情况检测摄像头
     * 只检测有预约的实验室的摄像头
     */
    Map<String, Object> detectCamerasByReservation();
    
    /**
     * 保存每日安全记录
     * 每个实验室开放时段开始和结束时各记录一次
     * @param labId 实验室 ID
     * @param description 描述
     * @param alertType 告警类型
     */
    void saveDailySafetyRecord(Long labId, String description, String alertType);
}
