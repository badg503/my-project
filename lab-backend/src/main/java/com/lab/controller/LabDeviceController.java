package com.lab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lab.annotation.LogOperation;
import com.lab.common.Result;
import com.lab.entity.LabDevice;
import com.lab.entity.LabRepair;
import com.lab.entity.DeviceBorrow;
import com.lab.entity.LabReserve;
import com.lab.entity.LabInfo;
import com.lab.service.LabDeviceService;
import com.lab.service.LabRepairService;
import com.lab.service.DeviceBorrowService;
import com.lab.service.LabReserveService;
import com.lab.service.LabInfoService;
import com.lab.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/device")
public class LabDeviceController {

    private final LabDeviceService deviceService;

    @Resource
    private LabRepairService repairService;
    
    @Resource
    private DeviceBorrowService borrowService;
    
    @Resource
    private LabReserveService reserveService;
    
    @Resource
    private LabInfoService labInfoService;
    
    @Resource
    private AiService aiService;

    public LabDeviceController(LabDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/list")
    public Result<List<LabDevice>> listByLab(@RequestParam(required = false) Long labId) {
        if (labId == null) {
            // 不传 labId 时返回所有设备
            return Result.ok(deviceService.list());
        }
        return Result.ok(deviceService.lambdaQuery().eq(LabDevice::getLabId, labId).list());
    }

    @GetMapping("/page")
    public Result<Page<LabDevice>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceType) {
        return Result.ok(deviceService.pageByLab(new Page<>(current, size), labId, status, deviceType));
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PostMapping
    @LogOperation(module = "设备管理", type = "新增", value = "新增设备")
    public Result<Void> add(@RequestBody LabDevice device) {
        deviceService.save(device);
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PutMapping
    @LogOperation(module = "设备管理", type = "修改", value = "修改设备信息")
    public Result<Void> update(@RequestBody LabDevice device, HttpServletRequest req) {
        // 获取旧设备信息
        LabDevice oldDevice = deviceService.getById(device.getId());
        Long operatorId = (Long) req.getAttribute("userId");

        if (oldDevice != null && "REPAIR".equals(oldDevice.getStatus()) && "AVAILABLE".equals(device.getStatus())) {
            // 设备从维修中变为可用，更新对应的报修记录
            List<LabRepair> repairs = repairService.list(new LambdaQueryWrapper<LabRepair>()
                    .eq(LabRepair::getDeviceId, device.getId())
                    .in(LabRepair::getStatus, "PENDING", "PROCESSING"));

            for (LabRepair repair : repairs) {
                repair.setStatus("FIXED");
                repair.setRepairTime(LocalDateTime.now());
                repair.setRepairRemark("设备已修复");
                repair.setHandlerId(operatorId);
                repairService.updateById(repair);
            }
        }
        
        // 设备状态变为报废或维修中时，更新相关预约状态
        if (oldDevice != null && 
            ("AVAILABLE".equals(oldDevice.getStatus()) || "UNAVAILABLE".equals(oldDevice.getStatus())) &&
            ("SCRAP".equals(device.getStatus()) || "REPAIR".equals(device.getStatus()))) {
            updateReserveStatusWhenDeviceUnavailable(device.getId(), device.getStatus(), oldDevice.getLabId());
        }
        
        // 设备报废时，触发故障反馈学习
        if (oldDevice != null && 
            !"SCRAP".equals(oldDevice.getStatus()) && 
            "SCRAP".equals(device.getStatus())) {
            try {
                log.info("♻️ 设备 {} 报废，触发故障反馈学习", device.getName());
                aiService.learnFromFault(device.getId());
                log.info("✅ 设备 {} 故障反馈学习完成", device.getName());
            } catch (Exception e) {
                log.error("❌ 设备 {} 故障反馈学习失败：{}", device.getName(), e.getMessage());
                // 学习失败不影响设备报废操作
            }
        }

        deviceService.updateById(device);
        return Result.ok();
    }
    
    /**
     * 当设备变为报废或维修中时，更新相关预约状态
     */
    private void updateReserveStatusWhenDeviceUnavailable(Long deviceId, String newStatus, Long labId) {
        if (deviceId == null) return;
        
        LabDevice device = deviceService.getById(deviceId);
        String deviceName = device != null ? device.getName() : "未知设备";
        
        String reason = "SCRAP".equals(newStatus) ? "设备已报废" : "设备维修中";
        String remark = deviceName + reason + "，请重新预约";
        
        // 查找包含该设备的预约
        List<LabReserve> affectedReserves = reserveService.lambdaQuery()
                .like(LabReserve::getDeviceIds, String.valueOf(deviceId))
                .in(LabReserve::getStatus, "PENDING", "APPROVED")
                .list();
        
        for (LabReserve reserve : affectedReserves) {
            // 检查预约时间是否还没到
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reserveDateTime = reserve.getReserveDate().atTime(java.time.LocalTime.parse(reserve.getTimeSlotStart()));
            
            // 只有预约时间还没到的才更新状态
            if (now.isBefore(reserveDateTime)) {
                LambdaUpdateWrapper<LabReserve> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(LabReserve::getId, reserve.getId())
                        .set(LabReserve::getStatus, "DEVICE_UNAVAILABLE")
                        .set(LabReserve::getAuditRemark, remark);
                reserveService.update(updateWrapper);
            }
            // 预约时间已过的，状态不变（学生可能正在使用或已使用完）
        }
    }

    @GetMapping("/{id}")
    public Result<LabDevice> getById(@PathVariable Long id) {
        return Result.ok(deviceService.getById(id));
    }

    @GetMapping("/repairDesc/{deviceId}")
    public Result<String> getRepairDesc(@PathVariable Long deviceId) {
        // 查询该设备最新的报修记录
        List<LabRepair> repairs = repairService.lambdaQuery()
                .eq(LabRepair::getDeviceId, deviceId)
                .orderByDesc(LabRepair::getCreateTime)
                .last("LIMIT 1")
                .list();
        
        if (repairs.isEmpty()) {
            return Result.ok("");
        }
        
        LabRepair repair = repairs.get(0);
        String faultDesc = repair.getFaultDesc();
        return Result.ok(faultDesc != null ? faultDesc : "");
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @DeleteMapping("/{id}")
    @LogOperation(module = "设备管理", type = "删除", value = "删除设备")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查设备是否存在
        LabDevice device = deviceService.getById(id);
        if (device == null) {
            return Result.fail("设备不存在");
        }
        
        // 检查是否有预约记录包含该设备
        long reserveCount = reserveService.lambdaQuery()
                .like(LabReserve::getDeviceIds, String.valueOf(id))
                .in(LabReserve::getStatus, "PENDING", "APPROVED")
                .count();
        if (reserveCount > 0) {
            return Result.fail("该设备存在" + reserveCount + "条预约记录，无法删除！请先处理相关预约。");
        }
        
        // 检查是否有设备借用记录
        long borrowCount = borrowService.lambdaQuery()
                .eq(DeviceBorrow::getDeviceId, id)
                .in(DeviceBorrow::getStatus, "BORROWED", "APPROVED")
                .count();
        if (borrowCount > 0) {
            return Result.fail("该设备存在" + borrowCount + "条借用记录，无法删除！");
        }
        
        // 通过所有检查，执行删除
        deviceService.removeById(id);
        return Result.ok();
    }
}
