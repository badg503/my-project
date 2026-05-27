package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_repair")
public class LabRepair {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private Long reporterId;
    private String faultDesc;
    private String status;  // PENDING, PROCESSING, FIXED, CLOSED
    private String repairRemark;
    private LocalDateTime repairTime;
    private Long handlerId;
    private String aiFaultType;
    private String aiSuggestion;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 非持久化字段
    @TableField(exist = false)
    private String deviceName;
    @TableField(exist = false)
    private Long labId;
    @TableField(exist = false)
    private String labName;
}
