package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_borrow")
public class LabBorrow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private Long userId;
    private LocalDateTime borrowTime;
    private LocalDateTime expectReturnTime;
    private LocalDateTime actualReturnTime;
    private String status;  // PENDING, APPROVED, REJECTED, RETURNED
    private Long auditUserId;
    private String auditRemark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
