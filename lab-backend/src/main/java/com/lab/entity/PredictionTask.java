package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 故障预测任务实体
 */
@Data
@TableName("prediction_task")
public class PredictionTask {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String taskId;
    
    private Long labId;
    
    private Integer totalDevices;
    
    private Integer processedDevices;
    
    private String status;
    
    private Integer estimatedTime;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private String triggerType;
    
    private Boolean hasRealtimeData;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
