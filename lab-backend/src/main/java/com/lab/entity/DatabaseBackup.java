package com.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库备份实体
 */
@Data
@TableName("database_backup")
public class DatabaseBackup {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 备份文件路径
     */
    private String backupFile;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 备份时间
     */
    private LocalDateTime backupTime;
    
    /**
     * 备注
     */
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
