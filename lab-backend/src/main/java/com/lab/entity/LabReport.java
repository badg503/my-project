package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("lab_report")
public class LabReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
    private String content;
    private String attachmentUrl;
    private BigDecimal score;
    private BigDecimal aiSimilarity;
    private String remark;
    private String status;  // SUBMITTED, GRADED
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 非持久化字段
    @TableField(exist = false)
    private String taskTitle;
    @TableField(exist = false)
    private String userName;
}
