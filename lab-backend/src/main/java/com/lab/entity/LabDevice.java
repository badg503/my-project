package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("lab_device")
public class LabDevice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long labId;
    private String name;
    private String model;
    private String status;  // AVAILABLE, IN_USE, REPAIR, SCRAP, UNAVAILABLE
    private String deviceType;  // PRECISE_MANUAL, NON_PRECISE_MANUAL, AUTO_BATCH, MULTI_USER
    private LocalDate purchaseTime;
    private String serialNo;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 非持久化字段
    @TableField(exist = false)
    private String labName;
    
    // Getter 方法（显式添加，避免 Lombok 问题）
    public Long getId() { return id; }
    public Long getLabId() { return labId; }
    public String getName() { return name; }
    public String getModel() { return model; }
    public String getStatus() { return status; }
    public String getDeviceType() { return deviceType; }
    public LocalDate getPurchaseTime() { return purchaseTime; }
    public String getSerialNo() { return serialNo; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public String getLabName() { return labName; }
}
