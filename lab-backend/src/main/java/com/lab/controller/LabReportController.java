package com.lab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.Result;
import com.lab.entity.LabReport;
import com.lab.entity.LabTask;
import com.lab.entity.SysUser;
import com.lab.annotation.LogOperation;
import com.lab.service.LabReportService;
import com.lab.service.LabTaskService;
import com.lab.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/report")
public class LabReportController {

    private final LabReportService reportService;
    private final LabTaskService taskService;
    private final SysUserService sysUserService;

    public LabReportController(LabReportService reportService, LabTaskService taskService, SysUserService sysUserService) {
        this.reportService = reportService;
        this.taskService = taskService;
        this.sysUserService = sysUserService;
    }

    @GetMapping("/my")
    public Result<Page<LabReport>> myList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        Page<LabReport> page = reportService.pageByUser(new Page<>(current, size), userId);
        
        List<Long> taskIds = page.getRecords().stream()
                .map(LabReport::getTaskId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        if (!taskIds.isEmpty()) {
            Map<Long, String> taskTitleMap = taskService.listByIds(taskIds).stream()
                    .collect(Collectors.toMap(LabTask::getId, LabTask::getTitle));
            
            for (LabReport report : page.getRecords()) {
                if (report.getTaskId() != null) {
                    report.setTaskTitle(taskTitleMap.getOrDefault(report.getTaskId(), ""));
                }
            }
        }
        
        return Result.ok(page);
    }

    @PostMapping
    @LogOperation(module = "实验报告", type = "新增", value = "提交实验报告")
    public Result<Void> submit(@RequestBody LabReport report, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        
        LabReport existingReport = reportService.lambdaQuery()
                .eq(LabReport::getUserId, userId)
                .eq(LabReport::getTaskId, report.getTaskId())
                .one();
        
        if (existingReport != null) {
            existingReport.setContent(report.getContent());
            existingReport.setAttachmentUrl(report.getAttachmentUrl());
            existingReport.setStatus("SUBMITTED");
            reportService.updateById(existingReport);
        } else {
            report.setUserId(userId);
            report.setStatus("SUBMITTED");
            reportService.save(report);
        }
        
        return Result.ok();
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @GetMapping("/task/{taskId}")
    public Result<Page<LabReport>> byTask(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LabTask task = taskService.getById(taskId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        
        Page<LabReport> page = reportService.pageByTask(new Page<>(current, size), taskId);
        List<LabReport> submittedReports = page.getRecords();
        
        List<Long> assignedStudentIds = new java.util.ArrayList<>();
        if (task.getStudentIds() != null && !task.getStudentIds().isEmpty()) {
            try {
                assignedStudentIds = java.util.Arrays.stream(task.getStudentIds().split(","))
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            } catch (Exception e) {
            }
        }
        
        if (assignedStudentIds.isEmpty()) {
            List<SysUser> allStudents = sysUserService.lambdaQuery()
                    .eq(SysUser::getRole, "STUDENT")
                    .list();
            assignedStudentIds = allStudents.stream()
                    .map(SysUser::getId)
                    .collect(Collectors.toList());
        }
        
        List<Long> submittedUserIds = submittedReports.stream()
                .map(LabReport::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        java.util.Set<Long> allUserIds = new java.util.HashSet<>();
        allUserIds.addAll(assignedStudentIds);
        allUserIds.addAll(submittedUserIds);
        
        Map<Long, String> studentNameMap = new java.util.HashMap<>();
        if (!allUserIds.isEmpty()) {
            List<SysUser> students = sysUserService.listByIds(new java.util.ArrayList<>(allUserIds));
            studentNameMap = students.stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        }
        
        for (LabReport report : submittedReports) {
            if (report.getUserId() != null) {
                report.setUserName(studentNameMap.getOrDefault(report.getUserId(), "未知学生"));
            } else {
                report.setUserName("未知学生");
            }
        }
        
        List<Long> submittedUserIdsList = submittedReports.stream()
                .map(LabReport::getUserId)
                .collect(Collectors.toList());
        
        java.util.ArrayList<LabReport> allReports = new java.util.ArrayList<>(submittedReports);
        for (Long studentId : assignedStudentIds) {
            if (!submittedUserIdsList.contains(studentId)) {
                LabReport unsubmittedReport = new LabReport();
                unsubmittedReport.setTaskId(taskId);
                unsubmittedReport.setUserId(studentId);
                unsubmittedReport.setUserName(studentNameMap.getOrDefault(studentId, "未知学生"));
                unsubmittedReport.setStatus("PENDING");
                allReports.add(unsubmittedReport);
            }
        }
        
        int start = (int) ((current - 1) * size);
        int end = (int) (start + size);
        if (start > allReports.size()) {
            start = allReports.size();
        }
        if (end > allReports.size()) {
            end = allReports.size();
        }
        
        List<LabReport> pageReports = allReports.subList(start, end);
        Page<LabReport> resultPage = new Page<>(current, size);
        resultPage.setRecords(pageReports);
        resultPage.setTotal(allReports.size());
        
        return Result.ok(resultPage);
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @PostMapping("/grade")
    @LogOperation(module = "实验报告", type = "修改", value = "批改实验报告")
    public Result<Void> grade(@RequestParam Long id, @RequestParam BigDecimal score,
                              @RequestParam(required = false) String remark) {
        LabReport r = reportService.getById(id);
        if (r == null) return Result.fail("报告不存在");
        r.setScore(score);
        r.setRemark(remark);
        r.setStatus("GRADED");
        reportService.updateById(r);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @LogOperation(module = "实验报告", type = "修改", value = "修改实验报告")
    public Result<Void> update(@PathVariable Long id, @RequestBody LabReport report, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        LabReport r = reportService.getById(id);
        if (r == null) return Result.fail("报告不存在");
        if (!r.getUserId().equals(userId)) return Result.fail("无权操作");
        if ("GRADED".equals(r.getStatus())) return Result.fail("已评分的报告不能修改");
        r.setTaskId(report.getTaskId());
        r.setContent(report.getContent());
        r.setAttachmentUrl(report.getAttachmentUrl());
        reportService.updateById(r);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @LogOperation(module = "实验报告", type = "删除", value = "删除实验报告")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        LabReport r = reportService.getById(id);
        if (r == null) return Result.fail("报告不存在");
        if (!r.getUserId().equals(userId)) return Result.fail("无权操作");
        if ("GRADED".equals(r.getStatus())) return Result.fail("已评分的报告不能删除");
        reportService.removeById(id);
        return Result.ok();
    }
}
