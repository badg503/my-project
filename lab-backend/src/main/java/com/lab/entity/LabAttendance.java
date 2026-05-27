package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_attendance")
public class LabAttendance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long userId;
    private String status;  // ATTENDANCE, ABSENCE, LATE
    private String checkInTime;
    private String remark;
    private Integer score;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    // 非持久化字段
    @TableField(exist = false)
    private String realName;
    @TableField(exist = false)
    private String gender;
    @TableField(exist = false)
    private String taskTitle;
    @TableField(exist = false)
    private String labName;
    @TableField(exist = false)
    private String className;
}
