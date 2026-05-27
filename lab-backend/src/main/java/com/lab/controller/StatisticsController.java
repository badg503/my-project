package com.lab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.common.Result;
import com.lab.entity.*;
import com.lab.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statistics")
@PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
public class StatisticsController {

    private final LabInfoService labInfoService;
    private final LabDeviceService deviceService;
    private final LabReserveService reserveService;
    private final LabCheckService checkService;
    private final LabRepairService repairService;
    private final LabAttendanceService labAttendanceService;
    private final SysUserService sysUserService;

    public StatisticsController(LabInfoService labInfoService, LabDeviceService deviceService,
                               LabReserveService reserveService, LabCheckService checkService,
                               LabRepairService repairService, LabAttendanceService labAttendanceService,
                               SysUserService sysUserService) {
        this.labInfoService = labInfoService;
        this.deviceService = deviceService;
        this.reserveService = reserveService;
        this.checkService = checkService;
        this.repairService = repairService;
        this.labAttendanceService = labAttendanceService;
        this.sysUserService = sysUserService;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("labCount", labInfoService.count());
        data.put("deviceCount", deviceService.count());
        data.put("reserveCount", reserveService.count());
        data.put("checkCount", checkService.count());
        data.put("repairCount", repairService.count());
        data.put("userCount", sysUserService.count());
        
        long studentCount = sysUserService.lambdaQuery().eq(SysUser::getRole, "STUDENT").count();
        long teacherCount = sysUserService.lambdaQuery().eq(SysUser::getRole, "TEACHER").count();
        data.put("studentCount", studentCount);
        data.put("teacherCount", teacherCount);
        
        return Result.ok(data);
    }

    @GetMapping("/lab-usage")
    public Result<Map<String, Object>> labUsageRate(@RequestParam(required = false) LocalDate startDate, 
                                                  @RequestParam(required = false) LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<LabInfo> labs = labInfoService.list();
        List<Map<String, Object>> labUsageData = new ArrayList<>();

        for (LabInfo lab : labs) {
            // 1. 计算实验室开放总时长（小时）
            double totalOpenHours = calculateLabOpenHours(lab, startDate, endDate);
            
            // 2. 计算学生实际使用总时长（小时）
            double totalUsedHours = calculateStudentUsedHours(lab.getId(), startDate, endDate);
            
            // 3. 计算使用率
            double usageRate = totalOpenHours > 0 ? (totalUsedHours / totalOpenHours * 100) : 0;
            
            // 4. 查询预约次数
            long reserveCount = reserveService.lambdaQuery()
                    .eq(LabReserve::getLabId, lab.getId())
                    .eq(LabReserve::getStatus, "APPROVED")
                    .ge(LabReserve::getReserveDate, startDate)
                    .le(LabReserve::getReserveDate, endDate)
                    .count();

            Map<String, Object> labData = new HashMap<>();
            labData.put("labId", lab.getId());
            labData.put("labName", lab.getName());
            labData.put("usageRate", Math.round(usageRate * 100) / 100.0);
            labData.put("reserveCount", reserveCount);
            labData.put("totalOpenHours", totalOpenHours);
            labData.put("totalUsedHours", totalUsedHours);
            labUsageData.add(labData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labUsageData", labUsageData);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        return Result.ok(result);
    }
    
    /**
     * 计算实验室开放总时长（小时）
     */
    private double calculateLabOpenHours(LabInfo lab, LocalDate startDate, LocalDate endDate) {
        String openTimeStart = lab.getOpenTimeStart(); // 如 "08:00"
        String openTimeEnd = lab.getOpenTimeEnd();     // 如 "18:00"
        
        if (openTimeStart == null || openTimeEnd == null || openTimeStart.isEmpty() || openTimeEnd.isEmpty()) {
            return 0;
        }
        
        try {
            // 解析开放时间
            String[] startParts = openTimeStart.split(":");
            String[] endParts = openTimeEnd.split(":");
            
            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);
            
            // 计算每天开放时长（小时）
            double dailyOpenHours = (endHour - startHour) + (endMinute - startMinute) / 60.0;
            
            // 计算总天数
            long totalDays = endDate.toEpochDay() - startDate.toEpochDay() + 1;
            
            return dailyOpenHours * totalDays;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 计算学生实际使用总时长（小时）
     * 正常签到：使用时长 = 预约结束时间 - 预约开始时间
     * 迟到签到：使用时长 = 预约结束时间 - 签到时间
     */
    private double calculateStudentUsedHours(Long labId, LocalDate startDate, LocalDate endDate) {
        // 查询该实验室在日期范围内的所有签到记录
        List<LabCheck> checks = checkService.lambdaQuery()
                .eq(LabCheck::getLabId, labId)
                .ge(LabCheck::getCheckInTime, startDate.atStartOfDay())
                .le(LabCheck::getCheckInTime, endDate.atTime(23, 59, 59))
                .eq(LabCheck::getStatus, "PRESENT") // 只统计签到成功的
                .list();
        
        double totalHours = 0;
        
        for (LabCheck check : checks) {
            try {
                Long reserveId = check.getReserveId();
                if (reserveId == null) continue;
                
                // 获取预约信息
                LabReserve reserve = reserveService.getById(reserveId);
                if (reserve == null) continue;
                
                String timeSlotStart = reserve.getTimeSlotStart(); // 如 "08:00"
                String timeSlotEnd = reserve.getTimeSlotEnd();     // 如 "18:00"
                LocalDateTime checkInTime = check.getCheckInTime();
                LocalDate reserveDate = reserve.getReserveDate();
                
                if (timeSlotStart == null || timeSlotEnd == null || checkInTime == null || reserveDate == null) continue;
                
                // 解析时间段
                String[] startParts = timeSlotStart.split(":");
                String[] endParts = timeSlotEnd.split(":");
                
                int startHour = Integer.parseInt(startParts[0]);
                int startMinute = Integer.parseInt(startParts[1]);
                int endHour = Integer.parseInt(endParts[0]);
                int endMinute = Integer.parseInt(endParts[1]);
                
                // 构建预约开始和结束时间的 LocalDateTime
                LocalDateTime reserveStartTime = LocalDateTime.of(reserveDate, java.time.LocalTime.of(startHour, startMinute));
                LocalDateTime reserveEndTime = LocalDateTime.of(reserveDate, java.time.LocalTime.of(endHour, endMinute));
                
                double usedHours;
                
                // 判断是否迟到（签到时间晚于预约开始时间 15 分钟以上）
                long lateMinutes = java.time.Duration.between(reserveStartTime, checkInTime).toMinutes();
                
                if (lateMinutes > 15) {
                    // 迟到：使用时长 = 预约结束时间 - 签到时间
                    usedHours = (double) java.time.Duration.between(checkInTime, reserveEndTime).toMinutes() / 60.0;
                } else {
                    // 正常：使用时长 = 预约结束时间 - 预约开始时间
                    usedHours = (double) java.time.Duration.between(reserveStartTime, reserveEndTime).toMinutes() / 60.0;
                }
                
                // 确保不为负数
                if (usedHours > 0) {
                    totalHours += usedHours;
                }
            } catch (Exception e) {
                // 忽略错误记录
            }
        }
        
        return totalHours;
    }

    @GetMapping("/device-failure")
    public Result<Map<String, Object>> deviceFailureRate() {
        long totalDevices = deviceService.count();
        long failedDevices = repairService.lambdaQuery()
                .eq(LabRepair::getStatus, "FIXED")
                .count();

        double failureRate = totalDevices > 0 ? (double) failedDevices / totalDevices * 100 : 0;

        List<LabDevice> devices = deviceService.list();
        Map<String, Long> deviceStatusCount = new HashMap<>();

        for (LabDevice device : devices) {
            String status = device.getStatus();
            deviceStatusCount.put(status, deviceStatusCount.getOrDefault(status, 0L) + 1);
        }

        List<Map<String, Object>> statusFailureData = new ArrayList<>();
        for (Map.Entry<String, Long> entry : deviceStatusCount.entrySet()) {
            Map<String, Object> data = new HashMap<>();
            data.put("status", entry.getKey());
            data.put("count", entry.getValue());
            statusFailureData.add(data);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalDevices", totalDevices);
        result.put("failedDevices", failedDevices);
        result.put("failureRate", Math.round(failureRate * 100) / 100.0);
        result.put("statusFailureData", statusFailureData);
        return Result.ok(result);
    }

    @GetMapping("/attendance")
    public Result<Map<String, Object>> studentAttendanceRate(@RequestParam(required = false) LocalDate startDate, 
                                                           @RequestParam(required = false) LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // 查询指定日期范围内的所有签到记录
        List<LabCheck> checks = checkService.lambdaQuery()
                .ge(LabCheck::getCheckInTime, startDate.atStartOfDay())
                .le(LabCheck::getCheckInTime, endDate.atTime(23, 59, 59))
                .list();

        // 统计每个学生的有效出勤次数
        // 有效出勤 = 签到成功 (ON_TIME 或 LATE) AND 签退成功 (NORMAL)
        Map<Long, Integer> studentAttendanceMap = new HashMap<>();

        for (LabCheck check : checks) {
            Long studentId = check.getUserId();
            String checkInStatus = check.getCheckInStatus();  // ON_TIME, LATE
            String checkOutStatus = check.getCheckOutStatus(); // NORMAL, EARLY_LEAVE
            
            // 判断是否为有效出勤
            boolean isValidCheckIn = "ON_TIME".equals(checkInStatus) || "LATE".equals(checkInStatus);
            boolean isValidCheckOut = "NORMAL".equals(checkOutStatus);
            
            if (isValidCheckIn && isValidCheckOut) {
                studentAttendanceMap.put(studentId, studentAttendanceMap.getOrDefault(studentId, 0) + 1);
            }
        }

        // 获取所有学生
        List<SysUser> students = sysUserService.lambdaQuery()
                .eq(SysUser::getRole, "STUDENT")
                .list();

        // 计算每个学生的总预约次数（作为总次数）
        Map<Long, Integer> studentTotalMap = new HashMap<>();
        for (SysUser student : students) {
            long totalReserves = reserveService.lambdaQuery()
                    .eq(LabReserve::getUserId, student.getId())
                    .eq(LabReserve::getStatus, "APPROVED")
                    .ge(LabReserve::getReserveDate, startDate)
                    .le(LabReserve::getReserveDate, endDate)
                    .count();
            studentTotalMap.put(student.getId(), (int) totalReserves);
        }

        // 构建返回数据
        List<Map<String, Object>> attendanceData = new ArrayList<>();
        for (SysUser student : students) {
            Long studentId = student.getId();
            int attended = studentAttendanceMap.getOrDefault(studentId, 0);
            int total = studentTotalMap.getOrDefault(studentId, 0);
            double rate = total > 0 ? (double) attended / total * 100 : 0;

            Map<String, Object> data = new HashMap<>();
            data.put("studentId", studentId);
            data.put("studentName", student.getRealName());
            data.put("attended", attended);
            data.put("total", total);
            data.put("attendanceRate", Math.round(rate * 100) / 100.0);
            attendanceData.add(data);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("attendanceData", attendanceData);
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        return Result.ok(result);
    }

    @GetMapping("/repair-stats")
    public Result<Map<String, Object>> repairStatistics() {
        List<LabRepair> repairs = repairService.list();
        Map<String, Integer> statusCount = new HashMap<>();

        for (LabRepair repair : repairs) {
            String status = repair.getStatus();
            statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("statusCount", statusCount);
        result.put("totalRepairs", repairs.size());
        return Result.ok(result);
    }
}
