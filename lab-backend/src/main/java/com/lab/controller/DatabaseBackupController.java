package com.lab.controller;

import com.lab.common.Result;
import com.lab.entity.DatabaseBackup;
import com.lab.service.DatabaseBackupService;
import com.lab.service.SystemConfigService;
import jakarta.annotation.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库备份控制器
 */
@RestController
@RequestMapping("/database-backup")
@PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
public class DatabaseBackupController {

    @Resource
    private DatabaseBackupService databaseBackupService;

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 获取备份状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取最近备份时间
        LocalDateTime lastBackupTime = databaseBackupService.getLastBackupTime();
        result.put("lastBackupTime", lastBackupTime != null ? lastBackupTime.toString() : null);
        
        // 获取备份保留天数
        String retentionDays = systemConfigService.getConfigValue("backup.retention.days");
        result.put("retentionDays", retentionDays != null ? retentionDays : "1");
        
        // 获取自动备份配置
        String autoBackupEnabled = systemConfigService.getConfigValue("auto.backup.enabled");
        String autoBackupTime = systemConfigService.getConfigValue("auto.backup.time");
        result.put("autoBackupEnabled", "true".equals(autoBackupEnabled));
        result.put("autoBackupTime", autoBackupTime != null ? autoBackupTime : "03:00");
        
        result.put("status", "normal");
        
        return Result.ok(result);
    }

    /**
     * 立即备份
     */
    @PostMapping("/backup")
    public Result<DatabaseBackup> backup(@RequestParam(required = false) String remark) {
        try {
            DatabaseBackup backup = databaseBackupService.backupNow(remark != null ? remark : "手动备份");
            return Result.ok(backup);
        } catch (Exception e) {
            return Result.fail("备份失败：" + e.getMessage());
        }
    }

    /**
     * 获取备份列表
     */
    @GetMapping("/list")
    public Result<List<DatabaseBackup>> list() {
        List<DatabaseBackup> backups = databaseBackupService.lambdaQuery()
                .orderByDesc(DatabaseBackup::getBackupTime)
                .list();
        return Result.ok(backups);
    }

    /**
     * 删除备份记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        DatabaseBackup backup = databaseBackupService.getById(id);
        if (backup != null) {
            // 删除文件
            File file = new File(backup.getBackupFile());
            if (file.exists()) {
                file.delete();
            }
            databaseBackupService.removeById(id);
        }
        return Result.ok();
    }

    /**
     * 清理过期备份
     */
    @PostMapping("/cleanup")
    public Result<Void> cleanup() {
        databaseBackupService.cleanupOldBackups();
        return Result.ok();
    }
}
