package com.lab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.annotation.LogOperation;
import com.lab.common.Result;
import com.lab.entity.LabReserve;
import com.lab.entity.DeviceBorrow;
import com.lab.entity.LabDevice;
import com.lab.entity.TeacherStudent;
import com.lab.entity.SysUser;
import com.lab.entity.ClassInfo;
import com.lab.entity.LabInfo;
import com.lab.entity.LabCheck;
import com.lab.entity.LabAttendance;
import com.lab.entity.LabTask;
import com.lab.service.LabReserveService;
import com.lab.service.DeviceBorrowService;
import com.lab.service.LabDeviceService;
import com.lab.service.TeacherStudentService;
import com.lab.service.SysUserService;
import com.lab.service.ClassService;
import com.lab.service.LabInfoService;
import com.lab.service.LabCheckService;
import com.lab.service.LabAttendanceService;
import com.lab.service.LabTaskService;
import com.lab.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/reserve")
public class LabReserveController {

    private final LabReserveService reserveService;
    private final DeviceBorrowService borrowService;
    private final LabDeviceService deviceService;
    private final TeacherStudentService teacherStudentService;
    private final SysUserService sysUserService;
    private final ClassService classService;
    private final LabInfoService labInfoService;
    private final LabCheckService checkService;
    private final LabAttendanceService attendanceService;
    private final LabTaskService taskService;
    private final SystemConfigService systemConfigService;

    public LabReserveController(LabReserveService reserveService, DeviceBorrowService borrowService, 
                               LabDeviceService deviceService, TeacherStudentService teacherStudentService,
                               SysUserService sysUserService, ClassService classService, LabInfoService labInfoService,
                               LabCheckService checkService, LabAttendanceService attendanceService,
                               LabTaskService taskService, SystemConfigService systemConfigService) {
        this.reserveService = reserveService;
        this.borrowService = borrowService;
        this.deviceService = deviceService;
        this.teacherStudentService = teacherStudentService;
        this.sysUserService = sysUserService;
        this.classService = classService;
        this.labInfoService = labInfoService;
        this.checkService = checkService;
        this.attendanceService = attendanceService;
        this.taskService = taskService;
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/my")
    public Result<Page<LabReserve>> myList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest req) {
        // 验证分页参数
        if (current < 1) {
            current = 1;
        }
        if (size < 1 || size > 100) {
            size = 10;
        }
        
        Long userId = (Long) req.getAttribute("userId");
        Page<LabReserve> page = reserveService.pageByUser(new Page<>(current, size), userId);
        
        // 填充实验室名称
        List<Long> labIds = page.getRecords().stream()
                .map(LabReserve::getLabId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        if (!labIds.isEmpty()) {
            Map<Long, LabInfo> labMap = labInfoService.listByIds(labIds).stream()
                    .collect(Collectors.toMap(LabInfo::getId, l -> l));
            
            for (LabReserve reserve : page.getRecords()) {
                LabInfo labInfo = labMap.get(reserve.getLabId());
                if (labInfo != null) {
                    reserve.setLabName(labInfo.getName());
                }
            }
        }
        
        return Result.ok(page);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    @LogOperation(module = "预约管理", type = "新增", value = "学生提交预约申请")
    public Result<Void> add(@RequestBody LabReserve reserve, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        reserve.setUserId(userId);
        reserve.setStatus("PENDING");
        
        // 验证 0：检查预约时间是否在当前时间之后（精确到分秒）
        LocalDateTime now = LocalDateTime.now();
        if (reserve.getReserveDate() == null) {
            return Result.fail("预约日期不能为空");
        }
        
        // 验证预约日期不能是过去的时间
        if (reserve.getReserveDate().isBefore(LocalDate.now())) {
            return Result.fail("预约日期不能是过去的日期");
        }
        
        // 验证预约日期范围（使用系统参数配置）
        String reserveAdvanceDaysStr = systemConfigService.getConfigValue("reserve.advance.days");
        int reserveAdvanceDays = reserveAdvanceDaysStr != null ? Integer.parseInt(reserveAdvanceDaysStr) : 7;
        
        if (reserve.getReserveDate().isAfter(LocalDate.now().plusDays(reserveAdvanceDays))) {
            return Result.fail("最多只能预约未来 " + reserveAdvanceDays + " 天内的实验室");
        }
        
        // 验证时间段格式
        if (reserve.getTimeSlotStart() == null || reserve.getTimeSlotEnd() == null || 
            reserve.getTimeSlotStart().isEmpty() || reserve.getTimeSlotEnd().isEmpty()) {
            return Result.fail("预约开始时间和结束时间不能为空");
        }
        
        // 验证结束时间必须晚于开始时间
        try {
            java.time.LocalTime startTime = java.time.LocalTime.parse(reserve.getTimeSlotStart());
            java.time.LocalTime endTime = java.time.LocalTime.parse(reserve.getTimeSlotEnd());
            
            if (!endTime.isAfter(startTime)) {
                return Result.fail("预约结束时间必须晚于开始时间");
            }
            
            // 验证预约时长（使用系统参数配置）
            String maxReserveHoursStr = systemConfigService.getConfigValue("reserve.max.hours");
            int maxReserveHours = maxReserveHoursStr != null ? Integer.parseInt(maxReserveHoursStr) : 4;
            
            long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
            long maxDurationMinutes = maxReserveHours * 60;
            
            if (durationMinutes > maxDurationMinutes) {
                return Result.fail("单次预约时长最多为 " + maxReserveHours + " 小时");
            }
            
            if (durationMinutes <= 0) {
                return Result.fail("预约时长必须大于 0");
            }
        } catch (Exception e) {
            return Result.fail("时间格式不正确，应为 HH:mm 格式");
        }
        
        // 构建预约开始时间
        LocalDateTime reserveDateTime = reserve.getReserveDate().atTime(java.time.LocalTime.parse(reserve.getTimeSlotStart()));
        
        // 预约时间必须至少在当前时间 1 分钟之后，防止预约过去的时间
        if (reserveDateTime.isBefore(now.plusMinutes(1))) {
            return Result.fail("预约时间必须在当前时间之后（至少 1 分钟后）");
        }
        
        // 验证 1：检查预约时间是否在实验室开放时间内
        LabInfo labInfo = labInfoService.getById(reserve.getLabId());
        if (labInfo == null) {
            return Result.fail("实验室不存在");
        }
        
        if (!isTimeInLabOpenTime(reserve.getTimeSlotStart(), reserve.getTimeSlotEnd(), 
                                 labInfo.getOpenTimeStart(), labInfo.getOpenTimeEnd())) {
            return Result.fail("预约时间不在实验室开放时间内（" + 
                              labInfo.getOpenTimeStart() + " - " + labInfo.getOpenTimeEnd() + "）");
        }
        
        // 验证 2：检查同一天同一实验室是否有时间段冲突的预约（已通过的预约）
        if (hasTimeConflict(reserve)) {
            return Result.fail("该时间段已有预约，请选择其他时间段");
        }
        
        // 验证 2.5：检查用户是否有重复预约
        if (hasUserRepeatReserve(reserve)) {
            return Result.fail("您在同一时段已有其他预约，不能重复预约");
        }
        
        // 验证 3：检查实验室容量是否已满
        if (isLabCapacityFull(reserve)) {
            return Result.fail("该时段实验室容量已满，无法预约");
        }
        
        // 验证 4：检查设备预约限制
        if (reserve.getDeviceIds() != null && !reserve.getDeviceIds().isEmpty()) {
            String deviceLimitResult = checkDeviceLimit(reserve);
            if (!"OK".equals(deviceLimitResult)) {
                return Result.fail(deviceLimitResult);
            }
            
            // 验证设备是否都属于该实验室
            String deviceValidateResult = validateDevicesBelongToLab(reserve.getLabId(), reserve.getDeviceIds());
            if (!"OK".equals(deviceValidateResult)) {
                return Result.fail(deviceValidateResult);
            }
        }
        
        // 验证预约目的（长度限制）
        if (reserve.getPurpose() != null && reserve.getPurpose().length() > 200) {
            return Result.fail("预约目的不能超过 200 字");
        }
        
        reserveService.save(reserve);
        return Result.ok();
    }
    
    /**
     * 检查用户是否有重复预约
     */
    private boolean hasUserRepeatReserve(LabReserve newReserve) {
        Long userId = newReserve.getUserId();
        if (userId == null) {
            return false;
        }
        
        // 查询该用户在同一天的所有预约
        List<LabReserve> userReservations = reserveService.lambdaQuery()
                .eq(LabReserve::getUserId, userId)
                .eq(LabReserve::getReserveDate, newReserve.getReserveDate())
                .in(LabReserve::getStatus, "PENDING", "APPROVED")
                .list();
        
        for (LabReserve existing : userReservations) {
            if (isTimeOverlap(newReserve.getTimeSlotStart(), newReserve.getTimeSlotEnd(),
                            existing.getTimeSlotStart(), existing.getTimeSlotEnd())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 验证设备是否都属于指定实验室
     */
    private String validateDevicesBelongToLab(Long labId, String deviceIds) {
        if (labId == null || deviceIds == null || deviceIds.isEmpty()) {
            return "OK";
        }
        
        String[] deviceIdArray = deviceIds.split(",");
        for (String deviceIdStr : deviceIdArray) {
            if (deviceIdStr == null || deviceIdStr.trim().isEmpty()) {
                continue;
            }
            
            try {
                Long deviceId = Long.parseLong(deviceIdStr.trim());
                LabDevice device = deviceService.getById(deviceId);
                
                if (device == null) {
                    return "设备 ID 为 " + deviceId + " 的设备不存在";
                }
                
                if (!device.getLabId().equals(labId)) {
                    return "设备【" + device.getName() + "】不属于当前预约的实验室";
                }
            } catch (NumberFormatException e) {
                return "设备 ID 格式不正确：" + deviceIdStr;
            }
        }
        
        return "OK";
    }
    
    /**
     * 检查预约时间是否在实验室开放时间内
     */
    private boolean isTimeInLabOpenTime(String reserveStart, String reserveEnd, 
                                        String labOpenStart, String labOpenEnd) {
        try {
            java.time.LocalTime rStart = java.time.LocalTime.parse(reserveStart);
            java.time.LocalTime rEnd = java.time.LocalTime.parse(reserveEnd);
            java.time.LocalTime lStart = java.time.LocalTime.parse(labOpenStart);
            java.time.LocalTime lEnd = java.time.LocalTime.parse(labOpenEnd);
            
            // 放宽验证：只要预约开始时间不晚于开放结束时间，且预约结束时间不早于开放开始时间
            // 这样允许预约时间与开放时间有部分重叠，而不是完全包含
            return !rStart.isAfter(lEnd) && !rEnd.isBefore(lStart);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查是否有时间段冲突
     */
    private boolean hasTimeConflict(LabReserve newReserve) {
        // 查询同一天同一实验室的已通过预约
        List<LabReserve> existingReservations = reserveService.lambdaQuery()
                .eq(LabReserve::getLabId, newReserve.getLabId())
                .eq(LabReserve::getReserveDate, newReserve.getReserveDate())
                .eq(LabReserve::getStatus, "APPROVED")
                .list();
        
        for (LabReserve existing : existingReservations) {
            if (isTimeOverlap(newReserve.getTimeSlotStart(), newReserve.getTimeSlotEnd(),
                            existing.getTimeSlotStart(), existing.getTimeSlotEnd())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查两个时间段是否重叠
     */
    private boolean isTimeOverlap(String start1, String end1, String start2, String end2) {
        try {
            java.time.LocalTime s1 = java.time.LocalTime.parse(start1);
            java.time.LocalTime e1 = java.time.LocalTime.parse(end1);
            java.time.LocalTime s2 = java.time.LocalTime.parse(start2);
            java.time.LocalTime e2 = java.time.LocalTime.parse(end2);
            
            return !s1.isAfter(e2) && !s2.isAfter(e1);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查实验室容量是否已满
     */
    private boolean isLabCapacityFull(LabReserve newReserve) {
        // 查询该时段已通过的预约数量
        List<LabReserve> existingReservations = reserveService.lambdaQuery()
                .eq(LabReserve::getLabId, newReserve.getLabId())
                .eq(LabReserve::getReserveDate, newReserve.getReserveDate())
                .eq(LabReserve::getStatus, "APPROVED")
                .list();
        
        // 检查时间段是否有重叠
        int currentCount = 0;
        for (LabReserve existing : existingReservations) {
            if (isTimeOverlap(newReserve.getTimeSlotStart(), newReserve.getTimeSlotEnd(),
                            existing.getTimeSlotStart(), existing.getTimeSlotEnd())) {
                currentCount++;
            }
        }
        
        // 获取实验室容量
        LabInfo labInfo = labInfoService.getById(newReserve.getLabId());
        if (labInfo == null || labInfo.getCapacity() == null) {
            return false; // 实验室不存在或无容量限制
        }
        
        return currentCount >= labInfo.getCapacity();
    }
    
    /**
     * 检查设备预约限制
     * @return "OK" 表示可以通过，否则返回错误信息
     */
    private String checkDeviceLimit(LabReserve newReserve) {
        String[] deviceIds = newReserve.getDeviceIds().split(",");
        
        for (String deviceIdStr : deviceIds) {
            if (deviceIdStr == null || deviceIdStr.trim().isEmpty()) {
                continue;
            }
            
            Long deviceId = Long.parseLong(deviceIdStr.trim());
            LabDevice device = deviceService.getById(deviceId);
            if (device == null) {
                continue; // 设备不存在，跳过
            }
            
            String deviceType = device.getDeviceType();
            int maxReservations = getMaxReservationsByType(deviceType);
            
            // 查询该设备在该时段已被预约的次数
            int currentReservations = countDeviceReservations(deviceId, newReserve);
            
            if (currentReservations >= maxReservations) {
                String typeName = getDeviceTypeName(deviceType);
                return String.format("设备【%s】属于%s类型，该时段最多可预约%d人，当前已达上限", 
                                   device.getName(), typeName, maxReservations);
            }
        }
        
        return "OK";
    }
    
    /**
     * 根据设备类型获取最大预约人数
     */
    private int getMaxReservationsByType(String deviceType) {
        if (deviceType == null) {
            return 1; // 默认精密型
        }
        
        switch (deviceType) {
            case "PRECISE_MANUAL":
                return 1; // 精密型设备：1 人/小时
            case "NON_PRECISE_MANUAL":
                return 5; // 非精密型设备：5 人/小时
            case "AUTO_BATCH":
                return 10; // 自动批量型：10 人/小时
            case "MULTI_USER":
                return 20; // 多用户型：20 人/小时
            default:
                return 1; // 默认 1 人
        }
    }
    
    /**
     * 获取设备类型的中文名称
     */
    private String getDeviceTypeName(String deviceType) {
        if (deviceType == null) {
            return "精密型";
        }
        
        switch (deviceType) {
            case "PRECISE_MANUAL":
                return "精密型";
            case "NON_PRECISE_MANUAL":
                return "非精密型";
            case "AUTO_BATCH":
                return "自动批量型";
            case "MULTI_USER":
                return "多用户型";
            default:
                return "未知类型";
        }
    }
    
    /**
     * 统计设备在指定时段的预约数量（按分钟精确计算）
     */
    private int countDeviceReservations(Long deviceId, LabReserve newReserve) {
        // 查询该设备在该时段已通过的预约
        List<LabReserve> existingReservations = reserveService.lambdaQuery()
                .eq(LabReserve::getLabId, newReserve.getLabId())
                .eq(LabReserve::getReserveDate, newReserve.getReserveDate())
                .eq(LabReserve::getStatus, "APPROVED")
                .list();
        
        int count = 0;
        for (LabReserve existing : existingReservations) {
            // 检查时间段是否有重叠
            if (isTimeOverlap(newReserve.getTimeSlotStart(), newReserve.getTimeSlotEnd(),
                            existing.getTimeSlotStart(), existing.getTimeSlotEnd())) {
                // 检查该预约是否包含此设备
                if (existing.getDeviceIds() != null && !existing.getDeviceIds().isEmpty()) {
                    String[] existingDeviceIds = existing.getDeviceIds().split(",");
                    for (String existingDeviceIdStr : existingDeviceIds) {
                        if (existingDeviceIdStr != null && existingDeviceIdStr.trim().equals(deviceId.toString())) {
                            count++;
                            break; // 每个预约只计数一次
                        }
                    }
                }
            }
        }
        
        return count;
    }
    
    /**
     * 查询某天所有可用时段
     */
    @GetMapping("/available-slots")
    public Result<List<Map<String, Object>>> getAvailableSlots(
            @RequestParam Long labId,
            @RequestParam LocalDate reserveDate,
            @RequestParam(required = false) List<Long> deviceIds) {
        
        // 验证实验室是否存在
        LabInfo labInfo = labInfoService.getById(labId);
        if (labInfo == null) {
            return Result.fail("实验室不存在");
        }
        
        // 验证预约日期
        if (reserveDate == null) {
            return Result.fail("预约日期不能为空");
        }
        
        if (reserveDate.isBefore(LocalDate.now())) {
            return Result.fail("预约日期不能是过去的日期");
        }
        
        // 使用系统参数配置验证
        String reserveAdvanceDaysStr = systemConfigService.getConfigValue("reserve.advance.days");
        int reserveAdvanceDays = reserveAdvanceDaysStr != null ? Integer.parseInt(reserveAdvanceDaysStr) : 7;
        
        if (reserveDate.isAfter(LocalDate.now().plusDays(reserveAdvanceDays))) {
            return Result.fail("最多只能查询未来 " + reserveAdvanceDays + " 天内的可用时段");
        }
        
        // 解析实验室开放时间
        java.time.LocalTime openStart = java.time.LocalTime.parse(labInfo.getOpenTimeStart());
        java.time.LocalTime openEnd = java.time.LocalTime.parse(labInfo.getOpenTimeEnd());
        
        // 生成所有可能的时间段（每 60 分钟一个时段）
        List<Map<String, Object>> availableSlots = new ArrayList<>();
        java.time.LocalTime currentTime = openStart;
        int slotIndex = 0;
        
        while (currentTime.isBefore(openEnd)) {
            java.time.LocalTime slotEnd = currentTime.plusMinutes(60); // 标准时段 60 分钟（1 小时）
            
            // 如果时段结束时间超过开放时间，则调整为开放时间（处理最后一段少于 45 分钟的情况）
            if (slotEnd.isAfter(openEnd)) {
                slotEnd = openEnd;
            }
            
            // 如果时段开始时间等于或超过开放时间，跳出循环
            if (currentTime.isAfter(openEnd) || currentTime.equals(openEnd)) {
                break;
            }
            
            String slotStartStr = currentTime.toString().substring(0, 5);
            String slotEndStr = slotEnd.toString().substring(0, 5);
            
            // 检查该时段是否可用（叠加检查实验室容量和设备余量）
            boolean isAvailable = checkSlotAvailabilityWithDevices(
                labId, reserveDate, slotStartStr, slotEndStr, labInfo.getCapacity(), deviceIds);
            
            Map<String, Object> slotInfo = new HashMap<>();
            slotInfo.put("timeSlotStart", slotStartStr);
            slotInfo.put("timeSlotEnd", slotEndStr);
            slotInfo.put("isAvailable", isAvailable);
            slotInfo.put("slotIndex", slotIndex); // 添加时段索引，用于前端选择多个连续时段
            
            availableSlots.add(slotInfo);
            currentTime = slotEnd;
            slotIndex++;
        }
        
        return Result.ok(availableSlots);
    }
    
    /**
     * 检查时段是否可用
     */
    private boolean checkSlotAvailability(Long labId, LocalDate reserveDate, 
                                          String slotStart, String slotEnd, Integer capacity) {
        // 查询该时段已通过的预约数量
        List<LabReserve> existingReservations = reserveService.lambdaQuery()
                .eq(LabReserve::getLabId, labId)
                .eq(LabReserve::getReserveDate, reserveDate)
                .eq(LabReserve::getStatus, "APPROVED")
                .list();
        
        int currentCount = 0;
        for (LabReserve existing : existingReservations) {
            if (isTimeOverlap(slotStart, slotEnd,
                            existing.getTimeSlotStart(), existing.getTimeSlotEnd())) {
                currentCount++;
            }
        }
        
        // 检查实验室容量
        if (capacity != null && currentCount >= capacity) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查时段是否可用（叠加检查实验室容量和设备余量）
     */
    private boolean checkSlotAvailabilityWithDevices(Long labId, LocalDate reserveDate, 
                                                     String slotStart, String slotEnd, 
                                                     Integer capacity, List<Long> deviceIds) {
        // 1. 检查实验室容量
        List<LabReserve> existingReservations = reserveService.lambdaQuery()
                .eq(LabReserve::getLabId, labId)
                .eq(LabReserve::getReserveDate, reserveDate)
                .eq(LabReserve::getStatus, "APPROVED")
                .list();
        
        int currentCount = 0;
        for (LabReserve existing : existingReservations) {
            if (isTimeOverlap(slotStart, slotEnd,
                            existing.getTimeSlotStart(), existing.getTimeSlotEnd())) {
                currentCount++;
            }
        }
        
        // 检查实验室容量
        if (capacity != null && currentCount >= capacity) {
            return false;
        }
        
        // 2. 如果选择了设备，检查设备余量
        if (deviceIds != null && !deviceIds.isEmpty()) {
            // 统计每个设备已被预约的次数
            Map<Long, Integer> deviceUsageCount = new HashMap<>();
            for (LabReserve reserve : existingReservations) {
                if (reserve.getDeviceIds() != null && !reserve.getDeviceIds().isEmpty()) {
                    String[] deviceIdsArray = reserve.getDeviceIds().split(",");
                    for (String deviceIdStr : deviceIdsArray) {
                        if (deviceIdStr != null && !deviceIdStr.trim().isEmpty()) {
                            Long deviceId = Long.parseLong(deviceIdStr.trim());
                            deviceUsageCount.put(deviceId, deviceUsageCount.getOrDefault(deviceId, 0) + 1);
                        }
                    }
                }
            }
            
            // 检查每个选中的设备是否有余量
            for (Long deviceId : deviceIds) {
                LabDevice device = deviceService.getById(deviceId);
                if (device == null) {
                    return false;
                }
                
                // 获取设备类型对应的最大预约次数
                int maxQuota = getMaxReservationsByType(device.getDeviceType());
                int usedQuota = deviceUsageCount.getOrDefault(deviceId, 0);
                
                // 可预约学生上限 = 设备数量 × 3
                int maxStudents = maxQuota * 3;
                int remainingQuota = maxStudents - usedQuota;
                
                // 设备余量不足
                if (remainingQuota <= 0) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 查询设备可预约余量（学生端预约前使用）
     */
    @GetMapping("/device-quota")
    public Result<Map<String, Object>> getDeviceQuota(
            @RequestParam Long labId,
            @RequestParam LocalDate reserveDate,
            @RequestParam String timeSlotStart,
            @RequestParam String timeSlotEnd) {
        
        // 验证实验室是否存在
        LabInfo labInfo = labInfoService.getById(labId);
        if (labInfo == null) {
            return Result.fail("实验室不存在");
        }
        
        // 验证预约日期
        if (reserveDate == null) {
            return Result.fail("预约日期不能为空");
        }
        
        if (reserveDate.isBefore(LocalDate.now())) {
            return Result.fail("预约日期不能是过去的日期");
        }
        
        // 使用系统参数配置验证
        String reserveAdvanceDaysStr = systemConfigService.getConfigValue("reserve.advance.days");
        int reserveAdvanceDays = reserveAdvanceDaysStr != null ? Integer.parseInt(reserveAdvanceDaysStr) : 7;
        
        if (reserveDate.isAfter(LocalDate.now().plusDays(reserveAdvanceDays))) {
            return Result.fail("最多只能查询未来 " + reserveAdvanceDays + " 天内的设备余量");
        }
        
        // 验证时间段格式
        if (timeSlotStart == null || timeSlotEnd == null || 
            timeSlotStart.isEmpty() || timeSlotEnd.isEmpty()) {
            return Result.fail("开始时间和结束时间不能为空");
        }
        
        // 验证结束时间必须晚于开始时间
        try {
            java.time.LocalTime startTime = java.time.LocalTime.parse(timeSlotStart);
            java.time.LocalTime endTime = java.time.LocalTime.parse(timeSlotEnd);
            
            if (!endTime.isAfter(startTime)) {
                return Result.fail("结束时间必须晚于开始时间");
            }
        } catch (Exception e) {
            return Result.fail("时间格式不正确，应为 HH:mm 格式");
        }
        
        Map<String, Object> result = new HashMap<>();
        
        // 查询该时段已通过的预约
        List<LabReserve> existingReservations = reserveService.lambdaQuery()
                .eq(LabReserve::getLabId, labId)
                .eq(LabReserve::getReserveDate, reserveDate)
                .eq(LabReserve::getStatus, "APPROVED")
                .list();
        
        // 过滤出时间段重叠的预约
        List<LabReserve> overlappingReservations = new ArrayList<>();
        for (LabReserve reserve : existingReservations) {
            if (isTimeOverlap(timeSlotStart, timeSlotEnd, 
                            reserve.getTimeSlotStart(), reserve.getTimeSlotEnd())) {
                overlappingReservations.add(reserve);
            }
        }
        
        // 统计每个设备已被预约的次数
        Map<Long, Integer> deviceUsageCount = new HashMap<>();
        for (LabReserve reserve : overlappingReservations) {
            if (reserve.getDeviceIds() != null && !reserve.getDeviceIds().isEmpty()) {
                String[] deviceIds = reserve.getDeviceIds().split(",");
                for (String deviceIdStr : deviceIds) {
                    if (deviceIdStr != null && !deviceIdStr.trim().isEmpty()) {
                        Long deviceId = Long.parseLong(deviceIdStr.trim());
                        deviceUsageCount.put(deviceId, deviceUsageCount.getOrDefault(deviceId, 0) + 1);
                    }
                }
            }
        }
        
        // 查询实验室所有设备
        List<LabDevice> devices = deviceService.lambdaQuery()
                .eq(LabDevice::getLabId, labId)
                .list();
        
        // 计算每个设备的可预约余量
        List<Map<String, Object>> deviceQuotas = new ArrayList<>();
        for (LabDevice device : devices) {
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceId", device.getId());
            deviceInfo.put("deviceName", device.getName());
            deviceInfo.put("deviceType", device.getDeviceType());
            deviceInfo.put("deviceTypeName", getDeviceTypeName(device.getDeviceType()));
            
            int maxQuota = getMaxReservationsByType(device.getDeviceType());
            int usedQuota = deviceUsageCount.getOrDefault(device.getId(), 0);
            
            // 可预约学生上限 = 设备数量 × 3
            int maxStudents = maxQuota * 3;
            int remainingQuota = maxStudents - usedQuota;
            
            deviceInfo.put("maxQuota", maxQuota);
            deviceInfo.put("usedQuota", usedQuota);
            deviceInfo.put("remainingQuota", remainingQuota);
            deviceInfo.put("isAvailable", remainingQuota > 0);
            
            deviceQuotas.add(deviceInfo);
        }
        
        // 检查实验室容量
        int labCapacity = labInfo.getCapacity() != null ? labInfo.getCapacity() : 0;
        int currentLabUsage = overlappingReservations.size();
        int remainingLabCapacity = labCapacity - currentLabUsage;
        
        result.put("labCapacity", labCapacity);
        result.put("currentLabUsage", currentLabUsage);
        result.put("remainingLabCapacity", remainingLabCapacity);
        result.put("isLabAvailable", remainingLabCapacity > 0);
        result.put("devices", deviceQuotas);
        
        return Result.ok(result);
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN') or hasRole('TEACHER')")
    @GetMapping("/page")
    public Result<Page<LabReserve>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reserveDate,
            HttpServletRequest req) {
        // 验证分页参数
        if (current < 1) {
            current = 1;
        }
        if (size < 1 || size > 100) {
            size = 10;
        }
        
        String userRole = (String) req.getAttribute("userRole");
        Long userId = (Long) req.getAttribute("userId");
        
        Page<LabReserve> page = reserveService.pageAll(new Page<>(current, size), labId, status, reserveDate, userRole, userId, teacherStudentService);
        
        // 填充实验室名称
        List<Long> labIds = page.getRecords().stream()
                .map(LabReserve::getLabId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        if (!labIds.isEmpty()) {
            Map<Long, LabInfo> labMap = labInfoService.listByIds(labIds).stream()
                    .collect(Collectors.toMap(LabInfo::getId, l -> l));
            
            for (LabReserve reserve : page.getRecords()) {
                LabInfo labInfo = labMap.get(reserve.getLabId());
                if (labInfo != null) {
                    reserve.setLabName(labInfo.getName());
                }
            }
        }
        
        // 填充用户名称和班级信息
        List<Long> userIds = page.getRecords().stream()
                .map(LabReserve::getUserId)
                .distinct()
                .collect(Collectors.toList());
        
        if (!userIds.isEmpty()) {
            Map<Long, SysUser> userMap = sysUserService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u));
            
            // 过滤掉用户不存在的记录（虚空数据）
            List<LabReserve> validRecords = page.getRecords().stream()
                    .filter(reserve -> userMap.containsKey(reserve.getUserId()))
                    .collect(Collectors.toList());
            page.setRecords(validRecords);
            // 不修改总记录数，保持分页的正确性
            
            // 获取所有班级ID
            List<Long> classIds = validRecords.stream()
                    .map(r -> userMap.get(r.getUserId()))
                    .filter(u -> u != null && u.getClassId() != null)
                    .map(SysUser::getClassId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 查询班级名称
            Map<Long, String> classNameMap = new HashMap<>();
            if (!classIds.isEmpty()) {
                classNameMap = classService.listByIds(classIds).stream()
                        .collect(Collectors.toMap(ClassInfo::getId, c -> c.getClassName(), (v1, v2) -> v1));
            }
            
            for (LabReserve reserve : page.getRecords()) {
                SysUser user = userMap.get(reserve.getUserId());
                if (user != null) {
                    reserve.setUserName(user.getRealName());
                    // 设置班级名称
                    if (user.getClassId() != null) {
                        reserve.setClassName(classNameMap.getOrDefault(user.getClassId(), "-"));
                    } else {
                        reserve.setClassName("-");
                    }
                }
            }
        }
        
        return Result.ok(page);
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN') or hasRole('TEACHER')")
    @PostMapping("/audit")
    @LogOperation(module = "预约管理", type = "审核", value = "审核预约申请")
    public Result<Void> audit(@RequestParam Long id, @RequestParam String status,
                              @RequestParam(required = false) String remark,
                              HttpServletRequest req) {
        LabReserve r = reserveService.getById(id);
        if (r == null) return Result.fail("预约不存在");
        
        // 检查权限：教师只能审核其学员的预约
        String userRole = (String) req.getAttribute("userRole");
        Long teacherId = (Long) req.getAttribute("userId");
        
        if ("TEACHER".equals(userRole)) {
            // 检查该学生是否是该教师的学员
            boolean isMyStudent = teacherStudentService.lambdaQuery()
                    .eq(TeacherStudent::getTeacherId, teacherId)
                    .eq(TeacherStudent::getStudentId, r.getUserId())
                    .count() > 0;
            if (!isMyStudent) {
                return Result.fail("您只能审核自己学员的预约申请");
            }
        }
        
        r.setStatus(status);
        r.setAuditRemark(remark);
        r.setAuditUserId((Long) req.getAttribute("userId"));
        r.setAuditTime(LocalDateTime.now());
        reserveService.updateById(r);
        
        // 如果审核通过，创建设备借用记录和考勤记录
        if ("APPROVED".equals(status)) {
            // 检查设备余量，如果已满则取消当前预约
            if (r.getDeviceIds() != null && !r.getDeviceIds().isEmpty()) {
                DeviceBorrowCheckResult checkResult = checkDeviceQuotaAndCancelExcess(r);
                if (!checkResult.isAllApproved()) {
                    // 有超额预约，取消当前预约
                    r.setStatus("CANCELLED");
                    r.setAuditRemark("设备余量已满，预约自动取消。" + (remark != null ? remark : ""));
                    reserveService.updateById(r);
                    return Result.fail("设备余量已满，您的预约已被取消。" + checkResult.getMessage());
                }
            }
            
            // 创建设备借用记录
            if (r.getDeviceIds() != null && !r.getDeviceIds().isEmpty()) {
                createDeviceBorrowRecords(r);
            }
            // 创建考勤记录
            createAttendanceRecord(r);
        }
        
        // 如果审核拒绝，取消设备借用记录
        if ("REJECTED".equals(status)) {
            updateDeviceBorrowStatus(id, "CANCELLED", "预约被驳回");
        }
        
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "预约管理", type = "取消", value = "取消预约申请")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest req) {
        LabReserve r = reserveService.getById(id);
        if (r == null) return Result.fail("预约不存在");
        
        String userRole = (String) req.getAttribute("userRole");
        Long userId = (Long) req.getAttribute("userId");
        boolean isAdmin = "SYS_ADMIN".equals(userRole) || "LAB_ADMIN".equals(userRole) || "TEACHER".equals(userRole);
        
        // 检查权限：学生只能取消自己的预约
        if (!isAdmin && !r.getUserId().equals(userId)) {
            return Result.fail("只能取消自己的预约");
        }
        
        // 检查预约状态：学生只能取消待审核的预约，管理员可以取消已通过的预约
        if (!isAdmin && !"PENDING".equals(r.getStatus())) {
            return Result.fail("仅待审核的预约可取消");
        }
        
        // 记录更新前的状态
        String oldStatus = r.getStatus();
        
        r.setStatus("CANCELLED");
        reserveService.updateById(r);
        
        // 如果之前是已通过状态，把设备记录更新为已取消
        if ("APPROVED".equals(oldStatus)) {
            updateDeviceBorrowStatus(id, "CANCELLED", isAdmin ? "管理员取消预约" : "学生取消预约");
        } else {
            // 否则直接取消设备借用记录
            updateDeviceBorrowStatus(id, "CANCELLED", isAdmin ? "管理员取消预约" : "学生取消预约");
        }
        
        return Result.ok();
    }
    
    /**
     * 检查设备余量并取消超额预约
     * @return 检查结果
     */
    private DeviceBorrowCheckResult checkDeviceQuotaAndCancelExcess(LabReserve currentReserve) {
        DeviceBorrowCheckResult result = new DeviceBorrowCheckResult();
        result.setAllApproved(true);
        
        // 查询该时段所有已通过的预约（包括当前预约）
        List<LabReserve> approvedReservations = reserveService.lambdaQuery()
                .eq(LabReserve::getLabId, currentReserve.getLabId())
                .eq(LabReserve::getReserveDate, currentReserve.getReserveDate())
                .eq(LabReserve::getStatus, "APPROVED")
                .list();
        
        // 统计每个设备已被预约的次数
        Map<Long, Integer> deviceUsageCount = new HashMap<>();
        for (LabReserve reserve : approvedReservations) {
            if (reserve.getDeviceIds() != null && !reserve.getDeviceIds().isEmpty()) {
                String[] deviceIds = reserve.getDeviceIds().split(",");
                for (String deviceIdStr : deviceIds) {
                    if (deviceIdStr != null && !deviceIdStr.trim().isEmpty()) {
                        Long deviceId = Long.parseLong(deviceIdStr.trim());
                        deviceUsageCount.put(deviceId, deviceUsageCount.getOrDefault(deviceId, 0) + 1);
                    }
                }
            }
        }
        
        // 检查每个设备是否超额
        if (currentReserve.getDeviceIds() != null && !currentReserve.getDeviceIds().isEmpty()) {
            String[] deviceIds = currentReserve.getDeviceIds().split(",");
            for (String deviceIdStr : deviceIds) {
                if (deviceIdStr != null && !deviceIdStr.trim().isEmpty()) {
                    Long deviceId = Long.parseLong(deviceIdStr.trim());
                    LabDevice device = deviceService.getById(deviceId);
                    if (device == null) continue;
                    
                    int maxQuota = getMaxReservationsByType(device.getDeviceType());
                    int maxStudents = maxQuota * 3; // 可预约学生上限 = 设备数量 × 3
                    int usedQuota = deviceUsageCount.getOrDefault(deviceId, 0);
                    
                    // 如果已预约人数 >= 上限，说明当前预约是超额的
                    if (usedQuota >= maxStudents) {
                        result.setAllApproved(false);
                        result.setMessage("设备【" + device.getName() + "】余量已满（" + usedQuota + "/" + maxStudents + "人）");
                        
                        // 取消当前预约
                        currentReserve.setStatus("CANCELLED");
                        currentReserve.setAuditRemark("设备【" + device.getName() + "】余量已满，预约自动取消。");
                        reserveService.updateById(currentReserve);
                        
                        log.info("设备余量已满，自动取消预约：预约 ID={}, 设备 ID={}, 已预约{}/{}人", 
                                currentReserve.getId(), deviceId, usedQuota, maxStudents);
                        break;
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 设备借用检查结果
     */
    private static class DeviceBorrowCheckResult {
        private boolean allApproved;
        private String message;
        
        public boolean isAllApproved() { return allApproved; }
        public void setAllApproved(boolean allApproved) { this.allApproved = allApproved; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    private void updateDeviceBorrowStatus(Long reserveId, String status, String remark) {
        LambdaUpdateWrapper<DeviceBorrow> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DeviceBorrow::getReserveId, reserveId)
                    .set(DeviceBorrow::getStatus, status)
                    .set(DeviceBorrow::getBorrowRemark, remark);
        
        boolean updated = borrowService.update(updateWrapper);
        log.info("设备借用记录状态更新{}，预约ID={}, 新状态={}", updated ? "成功" : "失败", reserveId, status);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest req) {
        LabReserve r = reserveService.getById(id);
        if (r == null) return Result.fail("预约不存在");
        if (!r.getUserId().equals(req.getAttribute("userId"))) return Result.fail("只能删除自己的预约");
        
        // 如果预约已通过，先把设备记录更新为已取消
        if ("APPROVED".equals(r.getStatus())) {
            updateDeviceBorrowStatus(id, "CANCELLED", "学生删除预约");
        } else {
            // 否则删除设备借用记录
            borrowService.remove(new LambdaQueryWrapper<DeviceBorrow>().eq(DeviceBorrow::getReserveId, id));
        }
        
        // 删除相关的考勤记录
        // 删除实验室预约签到记录 (lab_check)
        checkService.remove(new LambdaQueryWrapper<LabCheck>()
                .eq(LabCheck::getReserveId, id));
        
        // 先查找该预约对应的实验任务
        List<LabTask> tasks = taskService.lambdaQuery()
                .eq(LabTask::getLabId, r.getLabId())
                .list();
        
        if (!tasks.isEmpty()) {
            // 删除该学生在这些任务下的考勤记录
            for (LabTask task : tasks) {
                attendanceService.remove(new LambdaQueryWrapper<LabAttendance>()
                        .eq(LabAttendance::getTaskId, task.getId())
                        .eq(LabAttendance::getUserId, r.getUserId()));
            }
        }
        
        // 删除预约记录
        reserveService.removeById(id);
        
        return Result.ok();
    }
    
    private void createDeviceBorrowRecords(LabReserve reserve) {
        if (reserve.getDeviceIds() == null || reserve.getDeviceIds().isEmpty()) return;
        
        String[] deviceIdArray = reserve.getDeviceIds().split(",");
        for (String deviceIdStr : deviceIdArray) {
            Long deviceId = Long.parseLong(deviceIdStr.trim());
            LabDevice device = deviceService.getById(deviceId);
            if (device == null) continue;
            
            DeviceBorrow borrow = new DeviceBorrow();
            borrow.setReserveId(reserve.getId());
            borrow.setDeviceId(deviceId);
            borrow.setDeviceName(device.getName());
            borrow.setLabId(device.getLabId());
            borrow.setLabName(device.getLabName());
            borrow.setUserId(reserve.getUserId());
            borrow.setStatus("PENDING");
            borrow.setBorrowRemark(reserve.getBorrowRemark());
            borrow.setBorrowTime(LocalDateTime.now());
            borrowService.save(borrow);
        }
    }
    
    private void approveDeviceBorrowRecords(LabReserve reserve) {
        if (reserve.getDeviceIds() == null || reserve.getDeviceIds().isEmpty()) return;
        
        String[] deviceIdArray = reserve.getDeviceIds().split(",");
        for (String deviceIdStr : deviceIdArray) {
            Long deviceId = Long.parseLong(deviceIdStr.trim());
            List<DeviceBorrow> borrows = borrowService.lambdaQuery()
                    .eq(DeviceBorrow::getReserveId, reserve.getId())
                    .eq(DeviceBorrow::getDeviceId, deviceId)
                    .list();
            
            for (DeviceBorrow borrow : borrows) {
                borrow.setStatus("APPROVED");
                borrowService.updateById(borrow);
            }
        }
    }
    
    private void createAttendanceRecord(LabReserve reserve) {
        // 检查是否已经存在考勤记录
        long count = checkService.lambdaQuery()
                .eq(LabCheck::getReserveId, reserve.getId())
                .count();
        if (count > 0) {
            log.info("该预约已存在考勤记录，跳过创建，预约ID={}", reserve.getId());
            return;
        }
        
        LabCheck check = new LabCheck();
        check.setUserId(reserve.getUserId());
        check.setLabId(reserve.getLabId());
        check.setReserveId(reserve.getId());
        check.setStatus("ABSENT");  // 初始状态为未签到
        checkService.save(check);
        log.info("考勤记录创建成功，预约ID={}", reserve.getId());
    }
}
