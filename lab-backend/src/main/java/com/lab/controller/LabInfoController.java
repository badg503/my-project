package com.lab.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.Result;
import com.lab.entity.DeviceBorrow;
import com.lab.entity.LabDevice;
import com.lab.entity.LabInfo;
import com.lab.entity.LabReserve;
import com.lab.annotation.LogOperation;
import com.lab.service.DeviceBorrowService;
import com.lab.service.LabDeviceService;
import com.lab.service.LabInfoService;
import com.lab.service.LabReserveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/lab")
public class LabInfoController {

    private final LabInfoService labInfoService;
    private final LabReserveService labReserveService;
    private final DeviceBorrowService deviceBorrowService;
    private final LabDeviceService labDeviceService;

    public LabInfoController(LabInfoService labInfoService, LabReserveService labReserveService, DeviceBorrowService deviceBorrowService, LabDeviceService labDeviceService) {
        this.labInfoService = labInfoService;
        this.labReserveService = labReserveService;
        this.deviceBorrowService = deviceBorrowService;
        this.labDeviceService = labDeviceService;
    }

    /** 开放实验室列表（所有人可查） */
    @GetMapping("/list")
    public Result<List<LabInfo>> list(@RequestParam(required = false) Integer status) {
        List<LabInfo> list = labInfoService.lambdaQuery()
                .eq(status != null, LabInfo::getStatus, status)
                .orderByAsc(LabInfo::getId).list();
        return Result.ok(list);
    }

    @GetMapping("/page")
    public Result<Page<LabInfo>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<LabInfo> page = new Page<>(current, size);
        return Result.ok(labInfoService.pageList(page, name, status));
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PostMapping
    @LogOperation(module = "实验室管理", type = "新增", value = "新增实验室")
    public Result<Void> add(@RequestBody LabInfo lab) {
        labInfoService.save(lab);
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PutMapping
    @LogOperation(module = "实验室管理", type = "修改", value = "修改实验室信息")
    public Result<Void> update(@RequestBody LabInfo lab) {
        LabInfo oldLab = labInfoService.getById(lab.getId());
        
        log.info("更新实验室：实验室ID={}, 原状态={}, 新状态={}", lab.getId(), 
                 oldLab != null ? oldLab.getStatus() : null, lab.getStatus());
        
        labInfoService.updateById(lab);
        
        if (oldLab != null && oldLab.getStatus() == 1 && lab.getStatus() == 0) {
            log.info("检测到实验室从可用变为不可用，开始更新预约状态，实验室ID={}", lab.getId());
            updateReservationsWhenLabUnavailable(lab.getId());
        }
        
        if (oldLab != null && oldLab.getStatus() == 0 && lab.getStatus() == 1) {
            log.info("检测到实验室从不可用变为可用，开始恢复当天预约状态，实验室ID={}", lab.getId());
            restoreReservationsWhenLabAvailable(lab.getId());
        }
        
        return Result.ok();
    }
    
    private void updateReservationsWhenLabUnavailable(Long labId) {
        // 更新该实验室的所有设备状态为不可用
        LambdaUpdateWrapper<LabDevice> deviceUpdateWrapper = new LambdaUpdateWrapper<>();
        deviceUpdateWrapper.eq(LabDevice::getLabId, labId)
                .eq(LabDevice::getStatus, "AVAILABLE")
                .set(LabDevice::getStatus, "UNAVAILABLE");
        boolean devicesUpdated = labDeviceService.update(deviceUpdateWrapper);
        log.info("实验室设备状态更新{}，实验室ID={}", devicesUpdated ? "成功" : "失败", labId);
        
        // 更新预约状态和设备借用记录
        List<LabReserve> affectedReservations = labReserveService.lambdaQuery()
                .eq(LabReserve::getLabId, labId)
                .eq(LabReserve::getStatus, "APPROVED")
                .list();
        
        LambdaUpdateWrapper<LabReserve> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LabReserve::getLabId, labId)
                    .eq(LabReserve::getStatus, "APPROVED")
                    .set(LabReserve::getStatus, "LAB_UNAVAILABLE");
        
        boolean updated = labReserveService.update(updateWrapper);
        log.info("预约状态更新{}，实验室ID={}", updated ? "成功" : "失败", labId);
        
        for (LabReserve reserve : affectedReservations) {
            updateDeviceBorrowStatus(reserve.getId(), "LAB_UNAVAILABLE", "实验室不可用");
        }
    }
    
    private void restoreReservationsWhenLabAvailable(Long labId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 恢复该实验室的所有设备状态为可用（如果不是维修中或报废状态）
        LambdaUpdateWrapper<LabDevice> deviceUpdateWrapper = new LambdaUpdateWrapper<>();
        deviceUpdateWrapper.eq(LabDevice::getLabId, labId)
                .eq(LabDevice::getStatus, "UNAVAILABLE")
                .set(LabDevice::getStatus, "AVAILABLE");
        boolean devicesUpdated = labDeviceService.update(deviceUpdateWrapper);
        log.info("实验室设备状态恢复{}，实验室ID={}", devicesUpdated ? "成功" : "失败", labId);
        
        // 恢复预约状态和设备借用记录
        List<LabReserve> affectedReservations = labReserveService.lambdaQuery()
                .eq(LabReserve::getLabId, labId)
                .eq(LabReserve::getStatus, "LAB_UNAVAILABLE")
                .list();
        
        int count = 0;
        for (LabReserve reserve : affectedReservations) {
            LocalDateTime startTime = parseStartTime(reserve);
            if (startTime != null && now.isBefore(startTime)) {
                reserve.setStatus("APPROVED");
                labReserveService.updateById(reserve);
                count++;
                
                updateDeviceBorrowStatus(reserve.getId(), "BORROWED", null);
            }
        }
        
        log.info("恢复了 {} 条预约状态，实验室ID={}", count, labId);
    }
    
    private LocalDateTime parseStartTime(LabReserve reserve) {
        try {
            LocalDate date = reserve.getReserveDate();
            String timeStr = reserve.getTimeSlotStart();
            if (date != null && timeStr != null) {
                String[] timeParts = timeStr.split(":");
                if (timeParts.length >= 2) {
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    return date.atTime(hour, minute);
                }
            }
        } catch (Exception e) {
            log.error("解析预约开始时间失败: {}", e.getMessage());
        }
        return null;
    }
    
    private void updateDeviceBorrowStatus(Long reserveId, String status, String remark) {
        LambdaUpdateWrapper<DeviceBorrow> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DeviceBorrow::getReserveId, reserveId)
                    .set(DeviceBorrow::getStatus, status);
        
        if (remark != null) {
            updateWrapper.set(DeviceBorrow::getBorrowRemark, remark);
        } else {
            updateWrapper.set(DeviceBorrow::getBorrowRemark, "");
        }
        
        boolean updated = deviceBorrowService.update(updateWrapper);
        log.info("设备借用记录状态更新{}，预约ID={}, 新状态={}", updated ? "成功" : "失败", reserveId, status);
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @DeleteMapping("/{id}")
    @LogOperation(module = "实验室管理", type = "删除", value = "删除实验室")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查是否有预约记录
        long reserveCount = labReserveService.lambdaQuery()
                .eq(LabReserve::getLabId, id)
                .count();
        if (reserveCount > 0) {
            return Result.fail("该实验室存在" + reserveCount + "条预约记录，无法删除！请先处理相关预约。");
        }
        
        // 检查是否有设备借用记录
        long borrowCount = deviceBorrowService.lambdaQuery()
                .eq(DeviceBorrow::getLabId, id)
                .count();
        if (borrowCount > 0) {
            return Result.fail("该实验室存在" + borrowCount + "条设备借用记录，无法删除！");
        }
        
        // 检查是否有设备记录
        long deviceCount = labDeviceService.lambdaQuery()
                .eq(LabDevice::getLabId, id)
                .count();
        if (deviceCount > 0) {
            return Result.fail("该实验室存在" + deviceCount + "条设备记录，无法删除！请先删除设备。");
        }
        
        // 通过所有检查，执行删除
        labInfoService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<LabInfo> getById(@PathVariable Long id) {
        return Result.ok(labInfoService.getById(id));
    }
}
