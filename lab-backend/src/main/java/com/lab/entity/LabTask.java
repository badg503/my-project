package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_task")
public class LabTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teacherId;
    private String title;
    private String content;
    private String attachmentUrl;
    private LocalDateTime deadline;
    private Long labId;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 非持久化字段
    @TableField(exist = false)
    private String labName;
    
    @TableField(exist = false)
    private String studentIds;
    
    @TableField(exist = false)
    private String teacherName;
    
    @TableField(exist = false)
    private String teacherDepartment;
}
