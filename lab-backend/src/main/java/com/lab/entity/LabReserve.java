package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("lab_reserve")
public class LabReserve {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long labId;
    private LocalDate reserveDate;
    private String timeSlotStart;
    private String timeSlotEnd;
    private String purpose;
    private String deviceIds;
    private String borrowRemark;
    private String status;
    private Long auditUserId;
    private String auditRemark;
    private LocalDateTime auditTime;
    private Long labAuditUserId;
    private String labAuditRemark;
    private LocalDateTime labAuditTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 非数据库字段，用于前端显示
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String className;
    @TableField(exist = false)
    private String labName;
    
    // 辅助方法，用于计算使用时长
    public LocalDateTime getStartTime() {
        if (reserveDate == null || timeSlotStart == null) {
            return null;
        }
        String[] parts = timeSlotStart.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return LocalDateTime.of(reserveDate, java.time.LocalTime.of(hour, minute));
    }
    
    public LocalDateTime getEndTime() {
        if (reserveDate == null || timeSlotEnd == null) {
            return null;
        }
        String[] parts = timeSlotEnd.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return LocalDateTime.of(reserveDate, java.time.LocalTime.of(hour, minute));
    }
}
