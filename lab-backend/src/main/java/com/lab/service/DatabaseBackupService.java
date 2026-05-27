package com.lab.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.DatabaseBackup;
import com.lab.entity.SystemConfig;
import com.lab.mapper.DatabaseBackupMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 数据库备份服务
 */
@Service
public class DatabaseBackupService extends ServiceImpl<DatabaseBackupMapper, DatabaseBackup> {

    @Resource
    private SystemConfigService systemConfigService;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String dbDriver;

    private static final String BACKUP_DIR = "backups/database/";

    /**
     * 手动立即备份
     */
    @Transactional(rollbackFor = Exception.class)
    public DatabaseBackup backupNow(String remark) throws IOException, InterruptedException {
        // 生成备份文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFileName = "backup_" + timestamp + ".sql";
        String backupFilePath = BACKUP_DIR + backupFileName;

        // 确保备份目录存在
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        // 提取数据库名
        String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1).split("\\?")[0];

        // 执行 mysqldump 命令
        ProcessBuilder processBuilder = new ProcessBuilder(
            "mysqldump",
            "-h", "localhost",
            "-u", dbUsername,
            "-p" + dbPassword,
            "--default-character-set=utf8mb4",
            dbName
        );
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // 将输出重定向到文件
        java.nio.file.Files.copy(
            process.getInputStream(),
            java.nio.file.Paths.get(backupFilePath)
        );

        process.waitFor();

        // 获取文件大小
        File backupFile = new File(backupFilePath);
        long fileSize = backupFile.length();

        // 保存备份记录
        DatabaseBackup backup = new DatabaseBackup();
        backup.setBackupFile(backupFilePath);
        backup.setFileSize(fileSize);
        backup.setBackupTime(LocalDateTime.now());
        backup.setRemark(remark);
        save(backup);

        // 清理过期备份（保留 1 天）
        cleanupOldBackups();

        return backup;
    }

    /**
     * 自动备份（每天凌晨 3 点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoBackup() {
        try {
            // 检查是否启用自动备份
            SystemConfig config = systemConfigService.lambdaQuery()
                    .eq(SystemConfig::getConfigKey, "auto.backup.enabled")
                    .one();
            
            if (config == null || !"true".equals(config.getConfigValue())) {
                return;
            }

            backupNow("自动备份");
        } catch (Exception e) {
            System.err.println("自动备份失败：" + e.getMessage());
        }
    }

    /**
     * 清理过期备份（保留 1 天）
     */
    public void cleanupOldBackups() {
        // 获取保留天数配置
        SystemConfig config = systemConfigService.lambdaQuery()
                .eq(SystemConfig::getConfigKey, "backup.retention.days")
                .one();
        
        int retentionDays = config != null ? Integer.parseInt(config.getConfigValue()) : 1;

        // 计算过期时间
        LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);

        // 查询过期备份
        List<DatabaseBackup> oldBackups = lambdaQuery()
                .lt(DatabaseBackup::getBackupTime, expireTime)
                .list();

        // 删除文件并移除记录
        for (DatabaseBackup backup : oldBackups) {
            File backupFile = new File(backup.getBackupFile());
            if (backupFile.exists()) {
                backupFile.delete();
            }
            removeById(backup.getId());
        }
    }

    /**
     * 获取最近的备份记录
     */
    public LocalDateTime getLastBackupTime() {
        DatabaseBackup lastBackup = lambdaQuery()
                .orderByDesc(DatabaseBackup::getBackupTime)
                .last("LIMIT 1")
                .one();
        return lastBackup != null ? lastBackup.getBackupTime() : null;
    }
}
