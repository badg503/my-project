package com.lab.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摄像头实体类
 */
@Data
public class Camera {
    private Long id;
    private String cameraCode;     // 摄像头编码
    private String cameraName;     // 摄像头名称
    private String cameraType;     // 类型: rtsp/http/usb
    private String cameraUrl;      // 连接URL
    private Long labId;            // 关联实验室ID
    private String location;       // 具体位置
    private String description;    // 用途说明
    private String dangerClasses;  // 危险类别（JSON格式）
    private Double alertThreshold; // 告警阈值
    private Double confidence;     // 检测置信度
    private Integer enabled;       // 是否启用 0-禁用 1-启用
    private String status;         // 状态: ONLINE/OFFLINE/ERROR
    private Integer snapshotEnabled; // 是否保存截图
    private Integer snapshotInterval; // 截图间隔(秒)
    private Integer alertCooldown;    // 告警冷却时间(秒)
    private String extraConfig;      // 额外配置（JSON格式）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime lastCheckTime; // 最后检测时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createTime;   // 创建时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updateTime;   // 更新时间
}
