package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 故障预测详情实体
 */
@Data
@TableName("fault_prediction_detail")
public class FaultPredictionDetail {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long recordId;
    
    private String taskId;
    
    private Long labId;
    
    private Long deviceId;
    
    private String deviceName;
    
    private Double faultProb;
    
    private Double threshold;
    
    private Integer isFaulty;
    
    private String message;
    
    private String suggestion;
    
    private String triggerType;
    
    private LocalDateTime createdAt;
    
    @TableField(exist = false)
    private Boolean warning;
}
