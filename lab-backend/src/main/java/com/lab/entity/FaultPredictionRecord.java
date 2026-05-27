package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 故障预测记录实体
 */
@Data
@TableName("fault_prediction_record")
public class FaultPredictionRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long labId;
    
    private String triggerType;
    
    private Integer totalDevices;
    
    private Integer faultyDevices;
    
    private Integer normalDevices;
    
    private String predictionResult;
    
    private String status;
    
    private String message;
    
    private Long operatorId;
    
    private LocalDateTime createdAt;
}
