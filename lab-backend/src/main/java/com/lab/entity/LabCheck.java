package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_check")
public class LabCheck {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long labId;
    private Long reserveId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String status;  // PRESENT, ABSENT, LATE
    private String checkInStatus;  // ON_TIME, LATE
    private String checkOutStatus;  // NORMAL, EARLY_LEAVE
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 非持久化字段
    @TableField(exist = false)
    private String labName;
    
    @TableField(exist = false)
    private String reserveTime;
}
