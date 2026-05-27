package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 传感器配置实体
 */
@Data
@TableName("sensor_config")
public class SensorConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long labId;
    
    private Long deviceId;
    
    private String tempSensorIp;
    
    private String vibrationSensorIp;
    
    private String currentSensorIp;
    
    private String gatewayIp;
    
    private Integer apiPort;
    
    private Integer enabled;
    
    private String mode;
    
    private String description;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
