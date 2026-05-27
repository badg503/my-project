package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.LabTask;
import com.lab.entity.SysUser;
import com.lab.entity.TeacherStudent;
import com.lab.mapper.LabTaskMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabTaskService extends ServiceImpl<LabTaskMapper, LabTask> {

    private final SysUserService sysUserService;

    public LabTaskService(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    public Page<LabTask> pageByTeacher(Page<LabTask> page, Long teacherId) {
        return page(page, new LambdaQueryWrapper<LabTask>().eq(LabTask::getTeacherId, teacherId).orderByDesc(LabTask::getCreateTime));
    }

    public Page<LabTask> pageForStudent(Page<LabTask> page, Integer status, Long studentId, TeacherStudentService teacherStudentService) {
        LambdaQueryWrapper<LabTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(LabTask::getStatus, status);
        }
        
        List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                .eq(TeacherStudent::getStudentId, studentId)
                .list();
        List<Long> teacherIds = relations.stream()
                .map(TeacherStudent::getTeacherId)
                .collect(Collectors.toList());
        
        if (!teacherIds.isEmpty()) {
            wrapper.in(LabTask::getTeacherId, teacherIds);
        }
        
        wrapper.orderByDesc(LabTask::getCreateTime);
        Page<LabTask> result = page(page, wrapper);
        
        List<LabTask> records = result.getRecords();
        if (!records.isEmpty()) {
            List<Long> taskTeacherIds = records.stream()
                    .map(LabTask::getTeacherId)
                    .distinct()
                    .collect(Collectors.toList());
            
            if (!taskTeacherIds.isEmpty()) {
                List<SysUser> teachers = sysUserService.listByIds(taskTeacherIds);
                Map<Long, SysUser> teacherMap = teachers.stream()
                        .collect(Collectors.toMap(SysUser::getId, t -> t));
                
                for (LabTask task : records) {
                    SysUser teacher = teacherMap.get(task.getTeacherId());
                    if (teacher != null) {
                        task.setTeacherName(teacher.getRealName());
                    }
                }
            }
        }
        
        return result;
    }
}
