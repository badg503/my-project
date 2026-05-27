package com.lab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.Result;
import com.lab.entity.LabRecord;
import com.lab.service.LabRecordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/record")
public class LabRecordController {

    private final LabRecordService recordService;

    public LabRecordController(LabRecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/my")
    public Result<Page<LabRecord>> myList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return Result.ok(recordService.pageByStudent(new Page<>(current, size), userId));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @GetMapping("/teacher")
    public Result<Page<LabRecord>> teacherList(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest req) {
        Long teacherId = (Long) req.getAttribute("userId");
        return Result.ok(recordService.pageByTeacher(new Page<>(current, size), teacherId));
    }

    /**
     * 教师查看指定实验任务的学生提交情况
     */
    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @GetMapping("/task/{taskId}")
    public Result<Page<LabRecord>> getByTask(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest req) {
        Long teacherId = (Long) req.getAttribute("userId");
        return Result.ok(recordService.pageByTask(new Page<>(current, size), taskId, teacherId));
    }

    /**
     * 获取指定实验任务的提交统计
     */
    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @GetMapping("/task-stats/{taskId}")
    public Result<Map<String, Object>> getStats(
            @PathVariable Long taskId,
            HttpServletRequest req) {
        Long teacherId = (Long) req.getAttribute("userId");
        Map<String, Object> stats = recordService.getTaskStats(taskId, teacherId);
        return Result.ok(stats);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public Result<Void> submit(@RequestBody LabRecord record, HttpServletRequest req) {
        Long studentId = (Long) req.getAttribute("userId");
        record.setStudentId(studentId);
        record.setStatus("COMPLETED");
        recordService.save(record);
        return Result.ok();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody LabRecord record, HttpServletRequest req) {
        Long studentId = (Long) req.getAttribute("userId");
        LabRecord existing = recordService.getById(id);
        if (existing == null || !existing.getStudentId().equals(studentId)) {
            return Result.fail("记录不存在或无权操作");
        }
        existing.setTitle(record.getTitle());
        existing.setContent(record.getContent());
        existing.setAttachmentUrl(record.getAttachmentUrl());
        existing.setStatus("COMPLETED");
        recordService.updateById(existing);
        return Result.ok();
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @PostMapping("/grade")
    public Result<Void> grade(@RequestParam Long id, @RequestParam BigDecimal score,
                              @RequestParam(required = false) String remark) {
        LabRecord record = recordService.getById(id);
        if (record == null) return Result.fail("记录不存在");
        record.setScore(score);
        record.setRemark(remark);
        record.setStatus("GRADED");
        recordService.updateById(record);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        LabRecord record = recordService.getById(id);
        if (record == null || !record.getStudentId().equals(userId)) {
            return Result.fail("记录不存在或无权操作");
        }
        recordService.removeById(id);
        return Result.ok();
    }
}
