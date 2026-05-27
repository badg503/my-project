package com.lab.scheduled;

import com.lab.service.*;
import com.lab.entity.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class DataCleanupTask {
    
    private final LabReserveService labReserveService;
    private final LabCheckService labCheckService;
    private final LabRepairService labRepairService;
    private final LabReportService labReportService;
    private final DeviceBorrowService deviceBorrowService;
    private final SystemConfigService systemConfigService;

    public DataCleanupTask(LabReserveService labReserveService, 
                         LabCheckService labCheckService, 
                         LabRepairService labRepairService, 
                         LabReportService labReportService, 
                         DeviceBorrowService deviceBorrowService,
                         SystemConfigService systemConfigService) {
        this.labReserveService = labReserveService;
        this.labCheckService = labCheckService;
        this.labRepairService = labRepairService;
        this.labReportService = labReportService;
        this.deviceBorrowService = deviceBorrowService;
        this.systemConfigService = systemConfigService;
    }

    // 每天凌晨 2 点执行数据清理
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldData() {
        // 获取数据保留天数配置（默认 365 天）
        String retentionDaysStr = systemConfigService.getConfigValue("data.retention.days");
        int retentionDays = retentionDaysStr != null ? Integer.parseInt(retentionDaysStr) : 365;
        
        LocalDateTime expireTime = LocalDateTime.now().minus(retentionDays, ChronoUnit.DAYS);
        LocalDate expireDate = LocalDate.now().minus(retentionDays, ChronoUnit.DAYS);

        // 清理过期的预约记录
        labReserveService.remove(new LambdaQueryWrapper<LabReserve>()
                .lt(LabReserve::getCreateTime, expireTime));

        // 清理过期的考勤记录
        labCheckService.remove(new LambdaQueryWrapper<LabCheck>()
                .lt(LabCheck::getCreateTime, expireTime));

        // 清理过期的设备报修记录
        labRepairService.remove(new LambdaQueryWrapper<LabRepair>()
                .lt(LabRepair::getCreateTime, expireTime));

        // 清理过期的实验报告
        labReportService.remove(new LambdaQueryWrapper<LabReport>()
                .lt(LabReport::getCreateTime, expireTime));

        // 清理过期的设备借用记录
        deviceBorrowService.remove(new LambdaQueryWrapper<DeviceBorrow>()
                .lt(DeviceBorrow::getCreateTime, expireTime));
    }
}