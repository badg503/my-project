package com.lab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.Result;
import com.lab.entity.*;
import com.lab.annotation.LogOperation;
import com.lab.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attendance")
public class LabAttendanceController {

    private final LabAttendanceService attendanceService;
    private final SysUserService sysUserService;
    private final LabReportService reportService;
    private final LabTaskService taskService;
    private final LabCheckService checkService;
    private final TaskStudentService taskStudentService;
    private final TeacherStudentService teacherStudentService;
    private final LabInfoService labInfoService;
    private final ClassService classService;

    public LabAttendanceController(LabAttendanceService attendanceService, SysUserService sysUserService,
                                   LabReportService reportService, LabTaskService taskService,
                                   LabCheckService checkService, TaskStudentService taskStudentService,
                                   TeacherStudentService teacherStudentService, LabInfoService labInfoService,
                                   ClassService classService) {
        this.attendanceService = attendanceService;
        this.sysUserService = sysUserService;
        this.reportService = reportService;
        this.taskService = taskService;
        this.checkService = checkService;
        this.taskStudentService = taskStudentService;
        this.teacherStudentService = teacherStudentService;
        this.labInfoService = labInfoService;
        this.classService = classService;
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @GetMapping("/task/{taskId}")
    public Result<Page<LabAttendance>> byTask(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LabTask task = taskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }

        List<TaskStudent> taskStudents = taskStudentService.lambdaQuery()
                .eq(TaskStudent::getTaskId, taskId)
                .list();

        List<Long> studentIds = new ArrayList<>();
        if (!taskStudents.isEmpty()) {
            studentIds = taskStudents.stream()
                    .map(TaskStudent::getStudentId)
                    .collect(Collectors.toList());
        } else {
            List<TeacherStudent> teacherStudents = teacherStudentService.lambdaQuery()
                    .eq(TeacherStudent::getTeacherId, task.getTeacherId())
                    .list();
            studentIds = teacherStudents.stream()
                    .map(TeacherStudent::getStudentId)
                    .collect(Collectors.toList());
        }

        if (studentIds.isEmpty()) {
            return Result.ok(new Page<>());
        }

        Map<Long, SysUser> studentMap = sysUserService.listByIds(studentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, s -> s));

        Map<Long, LabReport> reportMap = reportService.lambdaQuery()
                .eq(LabReport::getTaskId, taskId)
                .in(LabReport::getUserId, studentIds)
                .list()
                .stream()
                .collect(Collectors.toMap(LabReport::getUserId, r -> r, (r1, r2) -> r1));

        LocalDate taskDate = task.getCreateTime().toLocalDate();
        List<LabCheck> checkRecords = checkService.lambdaQuery()
                .eq(LabCheck::getLabId, task.getLabId())
                .in(LabCheck::getUserId, studentIds)
                .ge(LabCheck::getCheckInTime, taskDate.atStartOfDay())
                .lt(LabCheck::getCheckInTime, taskDate.plusDays(1).atStartOfDay())
                .list();

        Map<Long, LabCheck> checkMap = checkRecords.stream()
                .collect(Collectors.toMap(LabCheck::getUserId, c -> c, (c1, c2) -> c1));

        List<LabAttendance> attendanceList = new ArrayList<>();

        List<LabAttendance> existingAttendances = attendanceService.lambdaQuery()
                .eq(LabAttendance::getTaskId, taskId)
                .list();
        Map<Long, LabAttendance> existingAttendanceMap = existingAttendances.stream()
                .collect(Collectors.toMap(LabAttendance::getUserId, a -> a, (a1, a2) -> a1));

        // 获取所有班级ID
        List<Long> classIds = studentMap.values().stream()
                .filter(s -> s.getClassId() != null)
                .map(SysUser::getClassId)
                .distinct()
                .collect(Collectors.toList());

        // 查询班级名称
        Map<Long, String> classNameMap = new HashMap<>();
        if (!classIds.isEmpty()) {
            classNameMap = classService.listByIds(classIds).stream()
                    .collect(Collectors.toMap(ClassInfo::getId, c -> c.getClassName(), (v1, v2) -> v1));
        }

        for (Long studentId : studentIds) {
            // 获取学生信息，如果学生不存在则跳过
            SysUser student = studentMap.get(studentId);
            if (student == null) {
                continue;
            }

            LabAttendance attendance;

            if (existingAttendanceMap.containsKey(studentId)) {
                attendance = existingAttendanceMap.get(studentId);

                attendance.setRealName(student.getRealName());
                attendance.setGender(student.getGender());
                // 设置班级名称
                if (student.getClassId() != null) {
                    attendance.setClassName(classNameMap.getOrDefault(student.getClassId(), "-"));
                } else {
                    attendance.setClassName("-");
                }

                // 从实验报告获取成绩
                LabReport report = reportMap.get(studentId);
                if (report != null && report.getScore() != null) {
                    attendance.setScore(report.getScore().intValue());
                }
            } else {
                attendance = new LabAttendance();
                attendance.setTaskId(taskId);
                attendance.setUserId(studentId);
                attendance.setRealName(student.getRealName());
                attendance.setGender(student.getGender());
                // 设置班级名称
                if (student.getClassId() != null) {
                    attendance.setClassName(classNameMap.getOrDefault(student.getClassId(), "-"));
                } else {
                    attendance.setClassName("-");
                }

                LabCheck check = checkMap.get(studentId);
                if (check != null && check.getCheckInTime() != null && check.getCheckOutTime() != null) {
                    Duration duration = Duration.between(check.getCheckInTime(), check.getCheckOutTime());
                    if (duration.toMinutes() >= 45) {
                        attendance.setStatus("ATTENDANCE");
                    } else {
                        attendance.setStatus("ABSENCE");
                    }
                    attendance.setCheckInTime(check.getCheckInTime().toString());
                } else {
                    attendance.setStatus("NOT_SIGNED");
                }

                LabReport report = reportMap.get(studentId);
                if (report != null && report.getScore() != null) {
                    attendance.setScore(report.getScore().intValue());
                }
            }

            attendanceList.add(attendance);
        }

        int total = attendanceList.size();
        int fromIndex = (int) ((current - 1) * size);
        int toIndex = (int) Math.min(fromIndex + size, total);

        Page<LabAttendance> page = new Page<>(current, size, total);
        if (fromIndex < total) {
            page.setRecords(attendanceList.subList(fromIndex, toIndex));
        } else {
            page.setRecords(new ArrayList<>());
        }

        return Result.ok(page);
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @PostMapping
    @LogOperation(module = "考勤管理", type = "新增", value = "保存考勤记录")
    public Result<Void> save(@RequestBody LabAttendance attendance) {
        attendanceService.save(attendance);
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PutMapping
    @LogOperation(module = "考勤管理", type = "修改", value = "修改考勤记录")
    public Result<Void> update(@RequestBody LabAttendance attendance) {
        // 只更新状态和备注字段
        LabAttendance existing = attendanceService.getById(attendance.getId());
        if (existing == null) {
            return Result.fail("考勤记录不存在");
        }
        
        if (attendance.getStatus() != null) {
            existing.setStatus(attendance.getStatus());
        }
        if (attendance.getRemark() != null) {
            existing.setRemark(attendance.getRemark());
        }
        
        attendanceService.updateById(existing);
        return Result.ok();
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @GetMapping("/stats/{taskId}")
    public Result<Map<String, Object>> getStats(@PathVariable Long taskId) {
        Map<String, Object> stats = new HashMap<>();

        long total = attendanceService.countByTaskAndStatus(taskId, "ATTENDANCE") + 
                    attendanceService.countByTaskAndStatus(taskId, "ABSENCE") + 
                    attendanceService.countByTaskAndStatus(taskId, "LATE") +
                    attendanceService.countByTaskAndStatus(taskId, "NOT_SIGNED");
        long attendanceCount = attendanceService.countByTaskAndStatus(taskId, "ATTENDANCE");
        long absenceCount = attendanceService.countByTaskAndStatus(taskId, "ABSENCE");
        double attendanceRate = total > 0 ? (double) attendanceCount / total * 100 : 0;

        stats.put("total", total);
        stats.put("attendanceCount", attendanceCount);
        stats.put("absenceCount", absenceCount);
        stats.put("attendanceRate", attendanceRate);

        return Result.ok(stats);
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @GetMapping("/all")
    public Result<Page<LabAttendance>> all(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long classId) {
        
        List<LabAttendance> attendanceList = new ArrayList<>();
        
        List<LabTask> tasks = taskService.lambdaQuery()
                .orderByDesc(LabTask::getCreateTime)
                .list();
        
        if (labId != null) {
            tasks = tasks.stream()
                    .filter(t -> labId.equals(t.getLabId()))
                    .collect(Collectors.toList());
        }
        
        List<Long> labIds = tasks.stream()
                .map(LabTask::getLabId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, LabInfo> labMap = labIds.isEmpty() ? Map.of() :
                labInfoService.listByIds(labIds).stream()
                        .collect(Collectors.toMap(LabInfo::getId, l -> l));
        
        for (LabTask task : tasks) {
            List<TaskStudent> taskStudents = taskStudentService.lambdaQuery()
                    .eq(TaskStudent::getTaskId, task.getId())
                    .list();

            List<Long> studentIds = new ArrayList<>();
            if (!taskStudents.isEmpty()) {
                studentIds = taskStudents.stream()
                        .map(TaskStudent::getStudentId)
                        .collect(Collectors.toList());
            } else {
                List<TeacherStudent> teacherStudents = teacherStudentService.lambdaQuery()
                        .eq(TeacherStudent::getTeacherId, task.getTeacherId())
                        .list();
                studentIds = teacherStudents.stream()
                        .map(TeacherStudent::getStudentId)
                        .collect(Collectors.toList());
            }
            
            if (studentIds.isEmpty()) continue;
            
            List<SysUser> students = sysUserService.listByIds(studentIds);
            
            Map<Long, SysUser> studentMap = students.stream()
                    .collect(Collectors.toMap(SysUser::getId, s -> s));
            
            // 获取所有班级ID
            List<Long> classIds = students.stream()
                    .filter(s -> s.getClassId() != null)
                    .map(SysUser::getClassId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 查询班级名称
            Map<Long, String> classNameMap = new HashMap<>();
            if (!classIds.isEmpty()) {
                classNameMap = classService.listByIds(classIds).stream()
                        .collect(Collectors.toMap(ClassInfo::getId, c -> c.getClassName(), (v1, v2) -> v1));
            }
            
            List<LabAttendance> existingAttendances = attendanceService.lambdaQuery()
                    .eq(LabAttendance::getTaskId, task.getId())
                    .in(LabAttendance::getUserId, studentIds)
                    .list();
            Map<Long, LabAttendance> existingMap = existingAttendances.stream()
                    .collect(Collectors.toMap(LabAttendance::getUserId, a -> a, (a1, a2) -> a1));
            
            Map<Long, LabReport> reportMap = reportService.lambdaQuery()
                    .eq(LabReport::getTaskId, task.getId())
                    .in(LabReport::getUserId, studentIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(LabReport::getUserId, r -> r, (r1, r2) -> r1));
            
            LocalDate taskDate = task.getCreateTime().toLocalDate();
            List<LabCheck> checkRecords = checkService.lambdaQuery()
                    .eq(LabCheck::getLabId, task.getLabId())
                    .in(LabCheck::getUserId, studentIds)
                    .ge(LabCheck::getCheckInTime, taskDate.atStartOfDay())
                    .lt(LabCheck::getCheckInTime, taskDate.plusDays(1).atStartOfDay())
                    .list();
            Map<Long, LabCheck> checkMap = checkRecords.stream()
                    .collect(Collectors.toMap(LabCheck::getUserId, c -> c, (c1, c2) -> c1));
            
            LabInfo lab = labMap.get(task.getLabId());
            String labName = lab != null ? lab.getName() : "";
            
            for (Long studentId : studentIds) {
                SysUser student = studentMap.get(studentId);
                if (student == null) continue;
                
                LabAttendance attendance;
                if (existingMap.containsKey(studentId)) {
                    attendance = existingMap.get(studentId);
                } else {
                    attendance = new LabAttendance();
                    attendance.setTaskId(task.getId());
                    attendance.setUserId(studentId);
                    
                    LabCheck check = checkMap.get(studentId);
                    if (check != null && check.getCheckInTime() != null && check.getCheckOutTime() != null) {
                        Duration duration = Duration.between(check.getCheckInTime(), check.getCheckOutTime());
                        if (duration.toMinutes() >= 45) {
                            attendance.setStatus("ATTENDANCE");
                        } else {
                            attendance.setStatus("ABSENCE");
                        }
                        attendance.setCheckInTime(check.getCheckInTime().toString());
                    } else {
                        attendance.setStatus("NOT_SIGNED");
                    }
                    
                    LabReport report = reportMap.get(studentId);
                    if (report != null && report.getScore() != null) {
                        attendance.setScore(report.getScore().intValue());
                    }
                }
                
                attendance.setTaskTitle(task.getTitle());
                attendance.setLabName(labName);
                attendance.setRealName(student.getRealName());
                // 设置班级名称
                if (student.getClassId() != null) {
                    attendance.setClassName(classNameMap.getOrDefault(student.getClassId(), "-"));
                } else {
                    attendance.setClassName("-");
                }
                
                // 班级过滤
                if (classId != null && !classId.equals(student.getClassId())) {
                    continue;
                }
                
                if (status != null && !status.isEmpty()) {
                    if (!status.equals(attendance.getStatus())) {
                        continue;
                    }
                }
                
                attendanceList.add(attendance);
            }
        }
        
        int total = attendanceList.size();
        int fromIndex = (int) ((current - 1) * size);
        int toIndex = (int) Math.min(fromIndex + size, total);
        
        Page<LabAttendance> page = new Page<>(current, size, total);
        if (fromIndex < total) {
            page.setRecords(attendanceList.subList(fromIndex, toIndex));
        } else {
            page.setRecords(new ArrayList<>());
        }
        
        return Result.ok(page);
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @GetMapping("/stats/all")
    public Result<Map<String, Object>> getAllStats() {
        Map<String, Object> result = new HashMap<>();
        
        List<LabTask> tasks = taskService.lambdaQuery().list();
        
        List<Long> labIds = tasks.stream()
                .map(LabTask::getLabId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, LabInfo> labMap = labIds.isEmpty() ? Map.of() :
                labInfoService.listByIds(labIds).stream()
                        .collect(Collectors.toMap(LabInfo::getId, l -> l));
        
        Map<String, Long> labTotalMap = new HashMap<>();
        Map<String, Long> labAttendanceMap = new HashMap<>();
        Map<String, Long> classTotalMap = new HashMap<>();
        Map<String, Long> classAttendanceMap = new HashMap<>();
        
        for (LabTask task : tasks) {
            List<TaskStudent> taskStudents = taskStudentService.lambdaQuery()
                    .eq(TaskStudent::getTaskId, task.getId())
                    .list();

            List<Long> studentIds = new ArrayList<>();
            if (!taskStudents.isEmpty()) {
                studentIds = taskStudents.stream()
                        .map(TaskStudent::getStudentId)
                        .collect(Collectors.toList());
            } else {
                List<TeacherStudent> teacherStudents = teacherStudentService.lambdaQuery()
                        .eq(TeacherStudent::getTeacherId, task.getTeacherId())
                        .list();
                studentIds = teacherStudents.stream()
                        .map(TeacherStudent::getStudentId)
                        .collect(Collectors.toList());
            }
            
            if (studentIds.isEmpty()) continue;
            
            List<LabAttendance> existingAttendances = attendanceService.lambdaQuery()
                    .eq(LabAttendance::getTaskId, task.getId())
                    .in(LabAttendance::getUserId, studentIds)
                    .list();
            Map<Long, LabAttendance> existingMap = existingAttendances.stream()
                    .collect(Collectors.toMap(LabAttendance::getUserId, a -> a, (a1, a2) -> a1));
            
            LocalDate taskDate = task.getCreateTime().toLocalDate();
            List<LabCheck> checkRecords = checkService.lambdaQuery()
                    .eq(LabCheck::getLabId, task.getLabId())
                    .in(LabCheck::getUserId, studentIds)
                    .ge(LabCheck::getCheckInTime, taskDate.atStartOfDay())
                    .lt(LabCheck::getCheckInTime, taskDate.plusDays(1).atStartOfDay())
                    .list();
            Map<Long, LabCheck> checkMap = checkRecords.stream()
                    .collect(Collectors.toMap(LabCheck::getUserId, c -> c, (c1, c2) -> c1));
            
            LabInfo lab = labMap.get(task.getLabId());
            String labName = lab != null ? lab.getName() : "未知实验室";
            
            // 获取学生班级信息
            List<SysUser> students = sysUserService.listByIds(studentIds);
            Map<Long, String> studentClassMap = new HashMap<>();
            for (SysUser student : students) {
                if (student.getClassId() != null) {
                    ClassInfo cls = classService.getById(student.getClassId());
                    if (cls != null) {
                        studentClassMap.put(student.getId(), cls.getClassName());
                    }
                }
            }
            
            for (Long studentId : studentIds) {
                LabAttendance attendance;
                if (existingMap.containsKey(studentId)) {
                    attendance = existingMap.get(studentId);
                } else {
                    attendance = new LabAttendance();
                    LabCheck check = checkMap.get(studentId);
                    if (check != null && check.getCheckInTime() != null && check.getCheckOutTime() != null) {
                        Duration duration = Duration.between(check.getCheckInTime(), check.getCheckOutTime());
                        if (duration.toMinutes() >= 45) {
                            attendance.setStatus("ATTENDANCE");
                        } else {
                            attendance.setStatus("ABSENCE");
                        }
                    } else {
                        attendance.setStatus("NOT_SIGNED");
                    }
                }
                
                // 统计实验室出勤率
                labTotalMap.merge(labName, 1L, Long::sum);
                if ("ATTENDANCE".equals(attendance.getStatus())) {
                    labAttendanceMap.merge(labName, 1L, Long::sum);
                }
                
                // 统计班级出勤率
                String className = studentClassMap.getOrDefault(studentId, "未知班级");
                classTotalMap.merge(className, 1L, Long::sum);
                if ("ATTENDANCE".equals(attendance.getStatus())) {
                    classAttendanceMap.merge(className, 1L, Long::sum);
                }
            }
        }
        
        // 构建实验室出勤率数据
        List<Map<String, Object>> labAttendanceRate = new ArrayList<>();
        for (String labName : labTotalMap.keySet()) {
            long total = labTotalMap.get(labName);
            long attendanceCount = labAttendanceMap.getOrDefault(labName, 0L);
            double rate = total > 0 ? (double) attendanceCount / total * 100 : 0;
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", labName);
            item.put("rate", rate);
            item.put("total", total);
            item.put("attendanceCount", attendanceCount);
            labAttendanceRate.add(item);
        }
        
        // 构建班级出勤率数据
        List<Map<String, Object>> classAttendanceRate = new ArrayList<>();
        for (String className : classTotalMap.keySet()) {
            long total = classTotalMap.get(className);
            long attendanceCount = classAttendanceMap.getOrDefault(className, 0L);
            double rate = total > 0 ? (double) attendanceCount / total * 100 : 0;
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", className);
            item.put("rate", rate);
            item.put("total", total);
            item.put("attendanceCount", attendanceCount);
            classAttendanceRate.add(item);
        }
        
        result.put("labAttendanceRate", labAttendanceRate);
        result.put("classAttendanceRate", classAttendanceRate);
        
        return Result.ok(result);
    }
}
