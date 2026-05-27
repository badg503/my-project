package com.lab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.Result;
import com.lab.entity.*;
import com.lab.annotation.LogOperation;
import com.lab.service.LabCheckService;
import com.lab.service.LabReserveService;
import com.lab.service.SysUserService;
import com.lab.service.LabInfoService;
import com.lab.service.ClassService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/check")
public class LabCheckController {

    private final LabCheckService checkService;
    private final LabReserveService reserveService;
    private final SysUserService sysUserService;
    private final LabInfoService labInfoService;
    private final ClassService classService;

    public LabCheckController(LabCheckService checkService, LabReserveService reserveService,
                              SysUserService sysUserService, LabInfoService labInfoService,
                              ClassService classService) {
        this.checkService = checkService;
        this.reserveService = reserveService;
        this.sysUserService = sysUserService;
        this.labInfoService = labInfoService;
        this.classService = classService;
    }

    @GetMapping("/my")
    public Result<Page<LabCheck>> myList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long labId,
            HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return Result.ok(checkService.pageByUser(new Page<>(current, size), userId, labId));
    }

    /**
     * 管理员查询所有签到记录（考勤管理页面使用）
     */
    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @GetMapping("/all")
    public Result<Page<Map<String, Object>>> getAllCheckRecords(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String status) {
        
        List<LabCheck> checkList = checkService.lambdaQuery()
                .orderByDesc(LabCheck::getCheckInTime)
                .list();
        
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        for (LabCheck check : checkList) {
            // 获取学生信息
            SysUser student = sysUserService.getById(check.getUserId());
            if (student == null) continue;
            
            // 获取实验室信息
            LabInfo lab = labInfoService.getById(check.getLabId());
            String labName = lab != null ? lab.getName() : "";
            
            // 获取班级名称
            String className = "-";
            if (student.getClassId() != null) {
                ClassInfo classInfo = classService.getById(student.getClassId());
                className = classInfo != null ? classInfo.getClassName() : "-";
            }
            
            // 获取预约信息（用于获取实验任务）
            String taskTitle = "-";
            if (check.getReserveId() != null) {
                LabReserve reserve = reserveService.getById(check.getReserveId());
                if (reserve != null && reserve.getLabId() != null) {
                    // 这里可以关联实验任务，但预约记录本身没有任务 ID
                    // 暂时显示为"实验室预约"
                    taskTitle = "实验室预约-" + labName;
                }
            }
            
            // 过滤条件
            if (labId != null && !labId.equals(check.getLabId())) {
                continue;
            }
            if (classId != null && !classId.equals(student.getClassId())) {
                continue;
            }
            if (status != null && !status.isEmpty() && !status.equals(check.getStatus())) {
                continue;
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("id", check.getId());
            item.put("userId", check.getUserId());
            item.put("realName", student.getRealName());
            item.put("className", className);
            item.put("labName", labName);
            item.put("taskTitle", taskTitle);
            item.put("status", check.getStatus());
            item.put("checkInStatus", check.getCheckInStatus());
            item.put("checkOutStatus", check.getCheckOutStatus());
            item.put("checkInTime", check.getCheckInTime());
            item.put("checkOutTime", check.getCheckOutTime());
            item.put("score", 0); // 预约签到没有成绩
            item.put("remark", "");
            
            resultList.add(item);
        }
        
        // 分页
        int total = resultList.size();
        int fromIndex = (int) ((current - 1) * size);
        int toIndex = (int) Math.min(fromIndex + size, total);
        
        Page<Map<String, Object>> page = new Page<>(current, size, total);
        if (fromIndex < total) {
            page.setRecords(resultList.subList(fromIndex, toIndex));
        } else {
            page.setRecords(new ArrayList<>());
        }
        
        return Result.ok(page);
    }

    /**
     * 获取考勤统计数据
     */
    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> result = new HashMap<>();
        
        // 查询所有签到记录
        List<LabCheck> allChecks = checkService.lambdaQuery().list();
        
        Map<String, Long> labTotalMap = new HashMap<>();
        Map<String, Long> labAttendanceMap = new HashMap<>();
        Map<String, Long> classTotalMap = new HashMap<>();
        Map<String, Long> classAttendanceMap = new HashMap<>();
        
        for (LabCheck check : allChecks) {
            // 获取实验室信息
            LabInfo lab = labInfoService.getById(check.getLabId());
            if (lab == null) continue;
            
            String labName = lab.getName();
            labTotalMap.put(labName, labTotalMap.getOrDefault(labName, 0L) + 1);
            
            // 统计有效签到（已签到且已签退）
            if ("PRESENT".equals(check.getStatus()) && check.getCheckOutTime() != null) {
                labAttendanceMap.put(labName, labAttendanceMap.getOrDefault(labName, 0L) + 1);
            }
            
            // 获取学生信息
            SysUser student = sysUserService.getById(check.getUserId());
            if (student == null) continue;
            
            // 按班级统计
            if (student.getClassId() != null) {
                ClassInfo classInfo = classService.getById(student.getClassId());
                if (classInfo != null) {
                    String className = classInfo.getClassName();
                    classTotalMap.put(className, classTotalMap.getOrDefault(className, 0L) + 1);
                    
                    if ("PRESENT".equals(check.getStatus()) && check.getCheckOutTime() != null) {
                        classAttendanceMap.put(className, classAttendanceMap.getOrDefault(className, 0L) + 1);
                    }
                }
            }
        }
        
        // 计算出勤率
        List<Map<String, Object>> labAttendanceRate = new ArrayList<>();
        for (Map.Entry<String, Long> entry : labTotalMap.entrySet()) {
            String labName = entry.getKey();
            long total = entry.getValue();
            long attendance = labAttendanceMap.getOrDefault(labName, 0L);
            double rate = total > 0 ? (double) attendance / total * 100 : 0;
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", labName);
            item.put("value", Math.round(rate * 100.0) / 100.0);
            labAttendanceRate.add(item);
        }
        
        List<Map<String, Object>> classAttendanceRate = new ArrayList<>();
        for (Map.Entry<String, Long> entry : classTotalMap.entrySet()) {
            String className = entry.getKey();
            long total = entry.getValue();
            long attendance = classAttendanceMap.getOrDefault(className, 0L);
            double rate = total > 0 ? (double) attendance / total * 100 : 0;
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", className);
            item.put("value", Math.round(rate * 100.0) / 100.0);
            classAttendanceRate.add(item);
        }
        
        result.put("labAttendanceRate", labAttendanceRate);
        result.put("classAttendanceRate", classAttendanceRate);
        
        return Result.ok(result);
    }

    @PostMapping("/sign-in")
    @LogOperation(module = "考勤签到", type = "签到", value = "实验室签到")
    public Result<Void> signIn(@RequestParam Long labId, @RequestParam(required = false) Long reserveId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        
        // 检查是否有有效的预约
        LabReserve reserve = null;
        if (reserveId != null) {
            reserve = reserveService.getById(reserveId);
        } else {
            // 查找当天该用户在该实验室的有效预约
            LocalDateTime now = LocalDateTime.now();
            List<LabReserve> reserves = reserveService.lambdaQuery()
                    .eq(LabReserve::getUserId, userId)
                    .eq(LabReserve::getLabId, labId)
                    .eq(LabReserve::getStatus, "APPROVED")
                    .eq(LabReserve::getReserveDate, now.toLocalDate())
                    .list();
            
            // 找到当前时间所在的预约时间段
            LocalTime nowTime = LocalTime.now();
            for (LabReserve r : reserves) {
                LocalTime startTime = LocalTime.parse(r.getTimeSlotStart());
                LocalTime endTime = LocalTime.parse(r.getTimeSlotEnd());
                if (!nowTime.isBefore(startTime) && !nowTime.isAfter(endTime)) {
                    reserve = r;
                    break;
                }
            }
            
            // 如果没有找到匹配的，取第一个
            if (reserve == null && !reserves.isEmpty()) {
                reserve = reserves.get(0);
            }
        }
        
        if (reserve == null) {
            return Result.fail("没有找到有效的预约记录");
        }
        
        // 检查预约是否已审核通过
        if (!"APPROVED".equals(reserve.getStatus())) {
            return Result.fail("预约尚未审核通过");
        }
        
        // 检查当前时间是否在预约时间段内
        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDate nowDate = nowDateTime.toLocalDate();
        LocalTime nowTime = nowDateTime.toLocalTime();
        
        // 检查是否是预约日期当天
        if (!nowDate.isEqual(reserve.getReserveDate())) {
            return Result.fail("只能在预约日期当天签到");
        }
        
        LocalTime startTime = LocalTime.parse(reserve.getTimeSlotStart());
        LocalTime endTime = LocalTime.parse(reserve.getTimeSlotEnd());
        
        if (nowTime.isBefore(startTime) || nowTime.isAfter(endTime)) {
            return Result.fail("当前时间不在预约时间段内");
        }
        
        // 检查该预约是否已经完成过签到签退（一个预约只能签到签退一次）
        LabCheck completedCheck = checkService.lambdaQuery()
                .eq(LabCheck::getUserId, userId)
                .eq(LabCheck::getReserveId, reserve.getId())
                .eq(LabCheck::getStatus, "PRESENT")
                .isNotNull(LabCheck::getCheckOutTime)
                .one();
        
        if (completedCheck != null) {
            return Result.fail("该预约时段已完成签到签退，不能重复操作");
        }
        
        // 检查是否已经签到过（未签退）
        LabCheck existingCheck = checkService.lambdaQuery()
                .eq(LabCheck::getUserId, userId)
                .eq(LabCheck::getReserveId, reserve.getId())
                .eq(LabCheck::getStatus, "PRESENT")
                .isNull(LabCheck::getCheckOutTime)
                .one();
        
        if (existingCheck != null) {
            return Result.fail("您已经签到过了，请先签退");
        }
        
        // 判断是否迟到（超过预约开始时间 10 分钟）
        LocalTime lateThreshold = startTime.plusMinutes(10);
        String checkInStatus = nowTime.isAfter(lateThreshold) ? "LATE" : "ON_TIME";
        
        // 创建签到记录
        LabCheck c = new LabCheck();
        c.setUserId(userId);
        c.setLabId(labId);
        c.setReserveId(reserve.getId());
        c.setCheckInTime(LocalDateTime.now());
        c.setStatus("PRESENT");
        c.setCheckInStatus(checkInStatus);
        checkService.save(c);
        return Result.ok();
    }

    @PostMapping("/sign-out")
    @LogOperation(module = "考勤签到", type = "签退", value = "实验室签退")
    public Result<Void> signOut(@RequestParam Long checkId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        LabCheck c = checkService.getById(checkId);
        if (c == null || !c.getUserId().equals(userId)) {
            return Result.fail("记录不存在");
        }
        // 检查是否已经签退过
        if (c.getCheckOutTime() != null) {
            return Result.fail("您已经签退过了，不能重复签退");
        }
        
        // 获取预约信息
        LabReserve reserve = reserveService.getById(c.getReserveId());
        if (reserve == null) {
            return Result.fail("预约信息不存在");
        }
        
        // 检查当前时间
        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDate nowDate = nowDateTime.toLocalDate();
        LocalTime nowTime = nowDateTime.toLocalTime();
        
        // 检查是否是预约日期当天
        if (!nowDate.isEqual(reserve.getReserveDate())) {
            return Result.fail("只能在预约日期当天签退");
        }
        
        LocalTime startTime = LocalTime.parse(reserve.getTimeSlotStart());
        LocalTime endTime = LocalTime.parse(reserve.getTimeSlotEnd());
        
        // 判断考勤状态：只看时间（是否最后 5 分钟内）
        String checkOutStatus;
        if (nowTime.isBefore(startTime) || nowTime.isAfter(endTime)) {
            // 不在预约时段内签退 = 早退
            checkOutStatus = "EARLY_LEAVE";
        } else {
            // 在预约时段内，判断是否在最后 5 分钟内签退（正常签退）
            LocalTime normalThreshold = endTime.minusMinutes(5);
            checkOutStatus = nowTime.isAfter(normalThreshold) ? "NORMAL" : "EARLY_LEAVE";
        }
        
        // 检查设备电源状态
        LabCheckService.DevicePowerCheckResult powerCheck = checkService.checkOutWithPowerCheck(checkId);
        
        if (powerCheck.isDeviceRunning()) {
            // 设备未关闭，返回前端让学生二次确认
            return Result.fail("设备仍在运转，请确认已关闭设备后再次点击签退");
        }
        
        // 设备已关闭，正常签退
        c.setCheckOutTime(LocalDateTime.now());
        c.setCheckOutStatus(checkOutStatus);
        checkService.updateById(c);
        
        return Result.ok();
    }

    /**
     * 签退前检查设备电源
     */
    @GetMapping("/check-power")
    public Result<LabCheckService.DevicePowerCheckResult> checkPowerBeforeSignOut(
            @RequestParam Long checkId,
            HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        LabCheck c = checkService.getById(checkId);
        if (c == null || !c.getUserId().equals(userId)) {
            return Result.fail("记录不存在");
        }
        
        LabCheckService.DevicePowerCheckResult result = checkService.checkOutWithPowerCheck(checkId);
        return Result.ok(result);
    }

    /**
     * 二次签退（设备未关闭但学生确认已关闭，或者设备仍未关闭）
     */
    @PostMapping("/confirm-sign-out")
    @LogOperation(module = "考勤签到", type = "签退", value = "确认签退")
    public Result<Void> confirmSignOut(@RequestParam Long checkId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        LabCheck c = checkService.getById(checkId);
        if (c == null || !c.getUserId().equals(userId)) {
            return Result.fail("记录不存在");
        }
        // 检查是否已经签退过
        if (c.getCheckOutTime() != null) {
            return Result.fail("您已经签退过了，不能重复签退");
        }
        
        // 获取预约信息
        LabReserve reserve = reserveService.getById(c.getReserveId());
        if (reserve == null) {
            return Result.fail("预约信息不存在");
        }
        
        // 检查当前时间
        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDate nowDate = nowDateTime.toLocalDate();
        LocalTime nowTime = nowDateTime.toLocalTime();
        
        // 检查是否是预约日期当天
        if (!nowDate.isEqual(reserve.getReserveDate())) {
            return Result.fail("只能在预约日期当天签退");
        }
        
        LocalTime startTime = LocalTime.parse(reserve.getTimeSlotStart());
        LocalTime endTime = LocalTime.parse(reserve.getTimeSlotEnd());
        
        // 判断考勤状态：只看时间（是否最后 5 分钟内）
        String checkOutStatus;
        if (nowTime.isBefore(startTime) || nowTime.isAfter(endTime)) {
            // 不在预约时段内签退 = 早退
            checkOutStatus = "EARLY_LEAVE";
        } else {
            // 在预约时段内，判断是否在最后 5 分钟内签退（正常签退）
            LocalTime normalThreshold = endTime.minusMinutes(5);
            checkOutStatus = nowTime.isAfter(normalThreshold) ? "NORMAL" : "EARLY_LEAVE";
        }
        
        // 第二次检查设备电源状态
        LabCheckService.DevicePowerCheckResult powerCheck = checkService.checkOutWithPowerCheck(checkId);
        
        // 签退（无论设备是否关闭都让学生签退）
        c.setCheckOutTime(LocalDateTime.now());
        c.setCheckOutStatus(checkOutStatus);
        checkService.updateById(c);
        
        if (powerCheck.isDeviceRunning()) {
            // 设备仍未关闭，发送邮件提醒
            checkService.sendDeviceNotClosedEmail(c.getUserId(), reserve);
        }
        
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN') or hasRole('TEACHER')")
    @GetMapping("/page")
    public Result<Page<LabCheck>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) Long userId) {
        return Result.ok(checkService.pageAll(new Page<>(current, size), labId, userId));
    }
}
