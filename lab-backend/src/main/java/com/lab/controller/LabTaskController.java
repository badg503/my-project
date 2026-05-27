package com.lab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.Result;
import com.lab.entity.LabInfo;
import com.lab.entity.LabTask;
import com.lab.entity.TaskStudent;
import com.lab.entity.TeacherStudent;
import com.lab.annotation.LogOperation;
import com.lab.service.LabInfoService;
import com.lab.service.LabTaskService;
import com.lab.service.TaskStudentService;
import com.lab.service.TeacherStudentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/task")
public class LabTaskController {

    private final LabTaskService taskService;
    private final LabInfoService labInfoService;
    private final TaskStudentService taskStudentService;
    private final TeacherStudentService teacherStudentService;

    public LabTaskController(LabTaskService taskService, LabInfoService labInfoService, 
                           TaskStudentService taskStudentService, TeacherStudentService teacherStudentService) {
        this.taskService = taskService;
        this.labInfoService = labInfoService;
        this.taskStudentService = taskStudentService;
        this.teacherStudentService = teacherStudentService;
    }

    /** 学生：实验任务列表 */
    @GetMapping("/list")
    public Result<Page<LabTask>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status,
            HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return Result.ok(taskService.pageForStudent(new Page<>(current, size), status, userId, teacherStudentService));
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @GetMapping("/my")
    public Result<Page<LabTask>> myTasks(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpServletRequest req) {
        Long teacherId = (Long) req.getAttribute("userId");
        
        LambdaQueryWrapper<LabTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LabTask::getTeacherId, teacherId);
        if (status != null) {
            wrapper.eq(LabTask::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(LabTask::getTitle, keyword);
        }
        wrapper.orderByDesc(LabTask::getCreateTime);
        
        Page<LabTask> page = taskService.page(new Page<>(current, size), wrapper);
        
        // 填充实验室名称
        List<Long> labIds = page.getRecords().stream()
                .map(LabTask::getLabId)
                .distinct()
                .collect(Collectors.toList());
        
        if (!labIds.isEmpty()) {
            Map<Long, String> labNameMap = labInfoService.listByIds(labIds).stream()
                    .collect(Collectors.toMap(LabInfo::getId, LabInfo::getName));
            
            for (LabTask task : page.getRecords()) {
                task.setLabName(labNameMap.getOrDefault(task.getLabId(), ""));
            }
        }
        
        return Result.ok(page);
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @PostMapping
    @LogOperation(module = "实验任务", type = "新增", value = "发布实验任务")
    public Result<Void> add(@RequestBody LabTask task, HttpServletRequest req) {
        Long teacherId = (Long) req.getAttribute("userId");
        task.setTeacherId(teacherId);
        task.setStatus(1);
        taskService.save(task);
        
        // 保存任务与学生的关联关系
        String studentIds = task.getStudentIds();
        if (studentIds != null && !studentIds.isEmpty()) {
            List<Long> studentIdList = Arrays.stream(studentIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            
            for (Long studentId : studentIdList) {
                TaskStudent ts = new TaskStudent();
                ts.setTaskId(task.getId());
                ts.setStudentId(studentId);
                taskStudentService.save(ts);
            }
        }
        
        return Result.ok();
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @PutMapping
    @LogOperation(module = "实验任务", type = "修改", value = "修改实验任务")
    public Result<Void> update(@RequestBody LabTask task) {
        taskService.updateById(task);
        return Result.ok();
    }

    @PreAuthorize("hasRole('TEACHER') or hasRole('SYS_ADMIN')")
    @DeleteMapping("/{id}")
    @LogOperation(module = "实验任务", type = "删除", value = "删除实验任务")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<LabTask> getById(@PathVariable Long id) {
        LabTask task = taskService.getById(id);
        if (task != null && task.getLabId() != null) {
            LabInfo lab = labInfoService.getById(task.getLabId());
            if (lab != null) {
                task.setLabName(lab.getName());
            }
        }
        return Result.ok(task);
    }
}
