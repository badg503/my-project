package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_safety_alert")
public class AiSafetyAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long labId;
    private String labName; // 实验室名称
    private LocalDateTime alertTime;
    private String alertType;
    private String description;
    private String imageUrl;
    private String status;  // PENDING, CONFIRMED, HANDLED
    private Long handlerId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
