package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_info")
public class LabInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String location;
    private String openTimeStart;
    private String openTimeEnd;
    private Integer capacity;
    private Integer status;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
