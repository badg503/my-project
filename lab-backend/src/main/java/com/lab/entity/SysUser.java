package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("username")
    private String username;
    
    @TableField("password")
    private String password;
    
    @TableField("real_name")
    private String realName;
    
    @TableField("role")
    private String role; // SYS_ADMIN/LAB_ADMIN/TEACHER/STUDENT
    
    @TableField("phone")
    private String phone;
    
    @TableField("email")
    private String email;
    
    @TableField("avatar")
    private String avatar;
    
    @TableField("gender")
    private String gender;
    
    @TableField("class_id")
    private Long classId;
    
    @TableField("major")
    private String major;
    
    @TableField("department")
    private String department;
    
    @TableField("status")
    private Integer status;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
