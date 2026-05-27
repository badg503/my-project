package com.lab.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摄像头检测记录实体类
 */
@Data
public class CameraLog {
    private Long id;
    private Long cameraId;        // 摄像头ID
    private String cameraName;     // 摄像头名称（关联字段）
    private String location;       // 摄像头位置（关联字段）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime detectTime; // 检测时间
    
    private String detectObjects;  // 检测到的物体列表（JSON格式）
    private String dangerObjects;  // 危险物体列表（JSON格式）
    private Integer safetyScore;   // 安全评分 0-100
    
    private Integer hasAlert;      // 是否触发告警 0-否 1-是
    private String alertLevel;     // 告警级别: LOW/MEDIUM/HIGH
    private String alertMessage;   // 告警内容
    
    private String snapshotPath;   // 截图文件路径
    
    private Integer processed;     // 是否已处理 0-否 1-是
    private Long processUserId;    // 处理人ID
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime processTime; // 处理时间
    
    private String processRemark;  // 处理备注
}
