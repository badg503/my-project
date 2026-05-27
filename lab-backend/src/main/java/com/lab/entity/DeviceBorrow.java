package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("device_borrow")
public class DeviceBorrow {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long reserveId;
    
    private Long deviceId;
    
    private String deviceName;
    
    private Long labId;
    
    private String labName;
    
    private Long userId;
    
    private String userName;
    
    private String status;
    
    private String borrowRemark;
    
    private String returnRemark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime borrowTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime returnTime;
}
