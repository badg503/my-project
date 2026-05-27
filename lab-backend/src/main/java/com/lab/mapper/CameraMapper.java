package com.lab.mapper;

import com.lab.entity.Camera;
import com.lab.entity.CameraLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 摄像头数据访问接口
 */
@Mapper
public interface CameraMapper {
    
    // ==================== 摄像头管理 ====================
    
    /**
     * 获取所有摄像头
     */
    List<Camera> selectAll(@Param("enabled") Integer enabled);
    
    /**
     * 根据实验室ID获取摄像头
     */
    List<Camera> selectByLabId(@Param("labId") Long labId, @Param("enabled") Integer enabled);
    
    /**
     * 根据编码获取摄像头
     */
    Camera selectByCode(@Param("cameraCode") String cameraCode);
    
    /**
     * 添加摄像头
     */
    int insert(Camera camera);
    
    /**
     * 更新摄像头
     */
    int update(Camera camera);
    
    /**
     * 删除摄像头
     */
    int deleteByCode(@Param("cameraCode") String cameraCode);
    
    /**
     * 更新摄像头状态
     */
    int updateStatus(@Param("cameraCode") String cameraCode, @Param("status") String status);
    
    // ==================== 检测记录 ====================
    
    /**
     * 添加检测记录
     */
    int insertLog(CameraLog log);
    
    /**
     * 获取检测记录（带关联摄像头信息）
     */
    List<Map<String, Object>> selectLogs(@Param("cameraCode") String cameraCode,
                                        @Param("startTime") String startTime,
                                        @Param("endTime") String endTime,
                                        @Param("hasAlert") Integer hasAlert);
    
    /**
     * 更新检测记录处理状态
     */
    int updateLogProcessed(@Param("id") Long id, @Param("processed") Integer processed,
                         @Param("processUserId") Long processUserId, @Param("processRemark") String processRemark);
}
