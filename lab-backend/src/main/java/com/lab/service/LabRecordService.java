package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.LabRecord;
import com.lab.entity.LabTask;
import com.lab.entity.SysUser;
import com.lab.entity.TeacherStudent;
import com.lab.mapper.LabRecordMapper;
import com.lab.service.LabTaskService;
import com.lab.service.SysUserService;
import com.lab.service.TeacherStudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LabRecordService extends ServiceImpl<LabRecordMapper, LabRecord> {
    
    @Autowired(required = false)
    private LabTaskService taskService;
    
    @Autowired(required = false)
    private SysUserService sysUserService;
    
    @Autowired(required = false)
    private TeacherStudentService teacherStudentService;
    
    public Page<LabRecord> pageByStudent(Page<LabRecord> page, Long studentId) {
        return page(page, new LambdaQueryWrapper<LabRecord>()
                .eq(LabRecord::getStudentId, studentId)
                .orderByDesc(LabRecord::getCreateTime));
    }

    public Page<LabRecord> pageByTeacher(Page<LabRecord> page, Long teacherId) {
        return page(page, new LambdaQueryWrapper<LabRecord>()
                .eq(LabRecord::getTeacherId, teacherId)
                .orderByDesc(LabRecord::getCreateTime));
    }
    
    /**
     * 教师查看指定实验任务的学生提交情况
     */
    public Page<LabRecord> pageByTask(Page<LabRecord> page, Long taskId, Long teacherId) {
        // 获取任务信息
        LabTask task = taskService.getById(taskId);
        if (task == null) {
            return new Page<>(page.getCurrent(), 0, 0);
        }
        
        // 获取教师的所有学员
        List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getTeacherId, teacherId)
                .list();
        
        if (relations.isEmpty()) {
            return new Page<>(page.getCurrent(), 0, 0);
        }
        
        List<Long> studentIds = relations.stream()
                .map(TeacherStudent::getStudentId)
                .collect(Collectors.toList());
        
        // 查询该任务下这些学生的提交记录
        Page<LabRecord> recordPage = page(page, new LambdaQueryWrapper<LabRecord>()
                .eq(LabRecord::getTaskId, taskId)
                .in(LabRecord::getStudentId, studentIds)
                .orderByDesc(LabRecord::getSubmitTime));
        
        // 填充学生姓名和性别
        List<LabRecord> filledRecords = new ArrayList<>();
        for (LabRecord record : recordPage.getRecords()) {
            SysUser student = sysUserService.getById(record.getStudentId());
            if (student != null) {
                record.setRealName(student.getRealName());
                record.setGender(student.getGender());
            }
            filledRecords.add(record);
        }
        recordPage.setRecords(filledRecords);
        
        // 如果没有提交记录，返回所有学员的空记录
        if (recordPage.getRecords().isEmpty()) {
            List<LabRecord> emptyRecords = new ArrayList<>();
            for (Long studentId : studentIds) {
                SysUser student = sysUserService.getById(studentId);
                if (student != null) {
                    LabRecord emptyRecord = new LabRecord();
                    emptyRecord.setStudentId(studentId);
                    emptyRecord.setTaskId(taskId);
                    emptyRecord.setRealName(student.getRealName());
                    emptyRecord.setGender(student.getGender());
                    emptyRecord.setStatus("NOT_SUBMITTED");
                    emptyRecords.add(emptyRecord);
                }
            }
            recordPage.setRecords(emptyRecords);
            recordPage.setTotal(emptyRecords.size());
        }
        
        return recordPage;
    }
    
    /**
     * 获取任务统计信息
     */
    public Map<String, Object> getTaskStats(Long taskId, Long teacherId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取教师的所有学员
        List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getTeacherId, teacherId)
                .list();
        
        if (relations.isEmpty()) {
            stats.put("total", 0);
            stats.put("submittedCount", 0);
            stats.put("gradedCount", 0);
            stats.put("submissionRate", 0.0);
            return stats;
        }
        
        List<Long> studentIds = relations.stream()
                .map(TeacherStudent::getStudentId)
                .collect(Collectors.toList());
        
        int total = studentIds.size();
        
        // 查询该任务下的提交记录
        List<LabRecord> records = list(new LambdaQueryWrapper<LabRecord>()
                .eq(LabRecord::getTaskId, taskId)
                .in(LabRecord::getStudentId, studentIds));
        
        int submittedCount = 0;
        int gradedCount = 0;
        
        for (LabRecord record : records) {
            if ("SUBMITTED".equals(record.getStatus()) || "GRADED".equals(record.getStatus())) {
                submittedCount++;
            }
            if ("GRADED".equals(record.getStatus())) {
                gradedCount++;
            }
        }
        
        double submissionRate = total > 0 ? (submittedCount * 100.0 / total) : 0.0;
        
        stats.put("total", total);
        stats.put("submittedCount", submittedCount);
        stats.put("gradedCount", gradedCount);
        stats.put("submissionRate", submissionRate);
        
        return stats;
    }
}