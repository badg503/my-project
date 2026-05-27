package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("lab_record")
public class LabRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teacherId;
    private Long studentId;
    private Long taskId;
    private Long labId;
    private String title;
    private String content;
    private String attachmentUrl;
    private String status; // NOT_SUBMITTED, SUBMITTED, COMPLETED, GRADED
    private BigDecimal score;
    private String remark;
    private LocalDateTime submitTime;
    
    // 非数据库字段，用于前端显示
    @TableField(exist = false)
    private String realName;
    @TableField(exist = false)
    private String gender;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}