package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 传感器读数实体
 */
@Data
@TableName("sensor_reading")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class SensorReading {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long deviceId;
    
    private Double temp;
    
    private Double vibration;
    
    private Double current;
    
    private LocalDateTime readingTime;
    
    private LocalDateTime createdAt;
}
