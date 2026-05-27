package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_fault_prediction")
public class AiFaultPrediction {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String taskId;
    private Long labId;
    private Long deviceId;
    private String deviceName;
    private BigDecimal faultProbability;
    private BigDecimal threshold;
    private Integer isFaulty;
    private String predictResult;
    private String message;
    private String suggestion;
    private String triggerType;  // MANUAL, SCHEDULED
    private String status;  // NEW, CONFIRMED, IGNORED
    private Integer deleted;
    private LocalDateTime predictTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private Boolean warning;
}
