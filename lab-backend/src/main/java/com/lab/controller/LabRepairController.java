package com.lab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.annotation.LogOperation;
import com.lab.common.Result;
import com.lab.entity.LabRepair;
import com.lab.entity.LabDevice;
import com.lab.service.LabRepairService;
import com.lab.service.AiService;
import com.lab.service.LabDeviceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/repair")
public class LabRepairController {

    private final LabRepairService repairService;
    private final AiService aiService;
    private final LabDeviceService deviceService;

    public LabRepairController(LabRepairService repairService, AiService aiService, LabDeviceService deviceService) {
        this.repairService = repairService;
        this.aiService = aiService;
        this.deviceService = deviceService;
    }

    @GetMapping("/my")
    public Result<Page<LabRepair>> myList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return Result.ok(repairService.pageByReporter(new Page<>(current, size), userId));
    }

    @PostMapping
    @LogOperation(module = "设备报修", type = "新增", value = "提交设备报修")
    public Result<Void> add(@RequestBody LabRepair repair, HttpServletRequest req) {
        try {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) return Result.fail("用户未登录");
            
            if (repair == null || repair.getDeviceId() == null) return Result.fail("设备ID不能为空");
            
            // 检查设备是否存在
            LabDevice device = deviceService.getById(repair.getDeviceId());
            if (device == null) return Result.fail("设备不存在，请查看设备ID或联系实验室管理员");
            
            // 检查设备是否已报废
            if ("SCRAP".equals(device.getStatus())) {
                return Result.fail("该设备已报废，无法报修");
            }
            
            // 检查该设备是否已有未完成的报修记录（PENDING 或 PROCESSING 状态）
            boolean hasPendingRepair = repairService.lambdaQuery()
                    .eq(LabRepair::getDeviceId, repair.getDeviceId())
                    .in(LabRepair::getStatus, "PENDING", "PROCESSING")
                    .exists();
            
            if (hasPendingRepair) {
                return Result.fail("该设备已有未完成的报修记录，请勿重复报修");
            }
            
            repair.setReporterId(userId);
            repair.setStatus("PENDING");
            repair.setDeviceName(device.getName()); // 设置设备名称
            
            // 只有可用状态的设备才更新为维修中
            if ("AVAILABLE".equals(device.getStatus())) {
                device.setStatus("REPAIR");
                deviceService.updateById(device);
            }
            
            String suggestion = aiService.qa("设备故障 " + repair.getFaultDesc());
            if (suggestion != null && !suggestion.contains("暂未找到")) {
                repair.setAiSuggestion(suggestion);
            }
            
            repairService.save(repair);
            return Result.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("操作失败：" + e.getMessage());
        }
    }

    @PutMapping
    @LogOperation(module = "设备报修", type = "修改", value = "修改报修记录")
    public Result<Void> update(@RequestBody LabRepair repair, HttpServletRequest req) {
        try {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) return Result.fail("用户未登录");
            
            if (repair == null || repair.getId() == null) return Result.fail("报修记录ID不能为空");
            
            LabRepair r = repairService.getById(repair.getId());
            if (r == null) return Result.fail("报修记录不存在");
            
            if (r.getReporterId() == null) return Result.fail("报修记录信息不完整");
            if (!r.getReporterId().equals(userId)) return Result.fail("无权操作");
            
            if (r.getStatus() == null) return Result.fail("报修记录状态不完整");
            if (!"PENDING".equals(r.getStatus())) return Result.fail("只能修改待处理状态的报修");
            
            r.setFaultDesc(repair.getFaultDesc());
            repairService.updateById(r);
            return Result.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("操作失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "设备报修", type = "取消", value = "取消报修申请")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest req) {
        try {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) return Result.fail("用户未登录");
            
            LabRepair r = repairService.getById(id);
            if (r == null) return Result.fail("报修记录不存在");
            
            if (r.getReporterId() == null) return Result.fail("报修记录信息不完整");
            if (!r.getReporterId().equals(userId)) return Result.fail("无权操作");
            
            if (r.getStatus() == null) return Result.fail("报修记录状态不完整");
            if (!"PENDING".equals(r.getStatus())) return Result.fail("只能取消待处理状态的报修");
            
            repairService.removeById(id);
            return Result.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("操作失败：" + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @GetMapping("/page")
    public Result<Page<LabRepair>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String status) {
        return Result.ok(repairService.pageAll(new Page<>(current, size), deviceId, status));
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PostMapping("/handle")
    @LogOperation(module = "设备报修", type = "处理", value = "处理报修申请")
    public Result<Void> handle(@RequestParam Long id, @RequestParam String status,
                               @RequestParam(required = false) String repairRemark,
                               HttpServletRequest req) {
        try {
            LabRepair r = repairService.getById(id);
            if (r == null) return Result.fail("报修记录不存在");
            
            r.setStatus(status);
            r.setRepairRemark(repairRemark);
            if ("FIXED".equals(status) || "CLOSED".equals(status)) r.setRepairTime(LocalDateTime.now());
            r.setHandlerId((Long) req.getAttribute("userId"));
            repairService.updateById(r);
            
            // 如果设备被修复，更新设备状态为可用
            if ("FIXED".equals(status)) {
                LabDevice device = deviceService.getById(r.getDeviceId());
                if (device != null) {
                    device.setStatus("AVAILABLE");
                    deviceService.updateById(device);
                }
            }
            
            return Result.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("操作失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/check-device/{deviceId}")
    public Result<Boolean> checkDeviceRepairStatus(@PathVariable Long deviceId) {
        try {
            boolean hasPendingRepair = repairService.lambdaQuery()
                    .eq(LabRepair::getDeviceId, deviceId)
                    .in(LabRepair::getStatus, "PENDING", "PROCESSING")
                    .exists();
            return Result.ok(hasPendingRepair);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("查询失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/pending-devices")
    public Result<java.util.List<Long>> getPendingRepairDevices() {
        try {
            java.util.List<LabRepair> pendingRepairs = repairService.lambdaQuery()
                    .in(LabRepair::getStatus, "PENDING", "PROCESSING")
                    .list();
            java.util.List<Long> deviceIds = pendingRepairs.stream()
                    .map(LabRepair::getDeviceId)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            return Result.ok(deviceIds);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("查询失败：" + e.getMessage());
        }
    }
}
