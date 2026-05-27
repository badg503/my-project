package com.lab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.Result;
import com.lab.entity.DeviceBorrow;
import com.lab.entity.LabDevice;
import com.lab.entity.LabRepair;
import com.lab.entity.SysUser;
import com.lab.service.DeviceBorrowService;
import com.lab.service.LabDeviceService;
import com.lab.service.LabRepairService;
import com.lab.service.SysUserService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/device/record")
public class DeviceRecordController {

    private final DeviceBorrowService deviceBorrowService;
    private final LabRepairService labRepairService;
    private final LabDeviceService labDeviceService;
    private final SysUserService sysUserService;

    public DeviceRecordController(DeviceBorrowService deviceBorrowService, LabRepairService labRepairService,
                                  LabDeviceService labDeviceService, SysUserService sysUserService) {
        this.deviceBorrowService = deviceBorrowService;
        this.labRepairService = labRepairService;
        this.labDeviceService = labDeviceService;
        this.sysUserService = sysUserService;
    }

    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {

        List<Map<String, Object>> allRecords = new ArrayList<>();

        // 借用记录
        if (type == null || "BORROW".equals(type) || "LAB_UNAVAILABLE".equals(type) || "CANCELLED".equals(type)) {
            LambdaQueryWrapper<DeviceBorrow> borrowWrapper = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.isEmpty()) {
                try {
                    Long deviceId = Long.parseLong(keyword);
                    borrowWrapper.eq(DeviceBorrow::getDeviceId, deviceId);
                } catch (NumberFormatException e) {
                    borrowWrapper.like(DeviceBorrow::getDeviceName, keyword);
                }
            }
            List<DeviceBorrow> borrows = deviceBorrowService.list(borrowWrapper);
            
            for (DeviceBorrow borrow : borrows) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", borrow.getId());
                record.put("deviceId", borrow.getDeviceId());
                record.put("deviceName", borrow.getDeviceName());
                record.put("userId", borrow.getUserId());
                
                String userName = borrow.getUserName();
                if (userName == null || userName.isEmpty()) {
                    SysUser user = sysUserService.getById(borrow.getUserId());
                    if (user != null) {
                        userName = user.getRealName();
                    }
                }
                record.put("userName", userName);
                String recordType = borrow.getStatus();
                if (recordType == null || recordType.isEmpty() || "PENDING".equals(recordType) || "APPROVED".equals(recordType) || "BORROWED".equals(recordType)) {
                    recordType = "BORROW";
                }
                record.put("type", recordType);
                record.put("remark", borrow.getBorrowRemark());
                record.put("createTime", borrow.getCreateTime());
                
                // 如果指定了类型，只添加匹配的记录
                if (type == null || type.equals(recordType)) {
                    allRecords.add(record);
                }
            }
        }

        // 报修记录
        if (type == null || "REPAIR".equals(type)) {
            LambdaQueryWrapper<LabRepair> repairWrapper = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.isEmpty()) {
                try {
                    Long deviceId = Long.parseLong(keyword);
                    repairWrapper.eq(LabRepair::getDeviceId, deviceId);
                } catch (NumberFormatException e) {
                    repairWrapper.like(LabRepair::getDeviceName, keyword);
                }
            }
            List<LabRepair> repairs = labRepairService.list(repairWrapper);
            
            for (LabRepair repair : repairs) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", repair.getId());
                record.put("deviceId", repair.getDeviceId());
                String deviceName = repair.getDeviceName();
                if (deviceName == null || deviceName.isEmpty()) {
                    LabDevice device = labDeviceService.getById(repair.getDeviceId());
                    if (device != null) {
                        deviceName = device.getName();
                    }
                }
                record.put("deviceName", deviceName);
                record.put("userId", repair.getReporterId());
                
                String userName = "";
                SysUser user = sysUserService.getById(repair.getReporterId());
                if (user != null) {
                    userName = user.getRealName();
                }
                record.put("userName", userName);
                record.put("type", "REPAIR");
                record.put("remark", repair.getRepairRemark());
                record.put("createTime", repair.getCreateTime());
                allRecords.add(record);
            }
        }

        // 报废记录 - 从设备表中获取状态为SCRAP的设备
        if (type == null || "SCRAP".equals(type)) {
            LambdaQueryWrapper<LabDevice> deviceWrapper = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.isEmpty()) {
                try {
                    Long deviceId = Long.parseLong(keyword);
                    deviceWrapper.eq(LabDevice::getId, deviceId);
                } catch (NumberFormatException e) {
                    deviceWrapper.like(LabDevice::getName, keyword);
                }
            }
            deviceWrapper.eq(LabDevice::getStatus, "SCRAP");
            List<LabDevice> scrapDevices = labDeviceService.list(deviceWrapper);
            
            for (LabDevice device : scrapDevices) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", "SCRAP_" + device.getId());
                record.put("deviceId", device.getId());
                record.put("deviceName", device.getName());
                
                // 查找该设备的报修记录，获取报修人信息
                LabRepair repair = labRepairService.lambdaQuery()
                        .eq(LabRepair::getDeviceId, device.getId())
                        .orderByDesc(LabRepair::getCreateTime)
                        .last("LIMIT 1")
                        .one();
                
                Long userId = null;
                String userName = "";
                String remark = "设备已报废";
                LocalDateTime createTime = device.getUpdateTime();
                
                if (repair != null) {
                    userId = repair.getReporterId();
                    SysUser user = sysUserService.getById(repair.getReporterId());
                    if (user != null) {
                        userName = user.getRealName();
                    }
                    remark = "设备报废，原报修：" + (repair.getRepairRemark() != null ? repair.getRepairRemark() : "无");
                    if (device.getUpdateTime() != null) {
                        createTime = device.getUpdateTime();
                    }
                } else {
                    // 如果没有报修记录，使用设备创建者或系统管理员
                    userId = 1L; // 默认系统管理员
                    SysUser admin = sysUserService.getById(1L);
                    if (admin != null) {
                        userName = admin.getRealName();
                    }
                }
                
                record.put("userId", userId);
                record.put("userName", userName);
                record.put("type", "SCRAP");
                record.put("remark", remark);
                record.put("createTime", createTime);
                allRecords.add(record);
            }
        }

        allRecords.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("createTime");
            LocalDateTime timeB = (LocalDateTime) b.get("createTime");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA);
        });

        int total = allRecords.size();
        int start = (current - 1) * size;
        int end = Math.min(start + size, total);
        List<Map<String, Object>> records = start < total ? allRecords.subList(start, end) : new ArrayList<>();

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("current", current);
        result.put("size", size);
        return Result.ok(result);
    }
}
