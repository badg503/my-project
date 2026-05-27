package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.LabReserve;
import com.lab.entity.TeacherStudent;
import com.lab.mapper.LabReserveMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LabReserveService extends ServiceImpl<LabReserveMapper, LabReserve> {

    public Page<LabReserve> pageByUser(Page<LabReserve> page, Long userId) {
        return page(page, new LambdaQueryWrapper<LabReserve>()
                .eq(LabReserve::getUserId, userId).orderByDesc(LabReserve::getCreateTime));
    }

    public Page<LabReserve> pageAll(Page<LabReserve> page, Long labId, String status, LocalDate reserveDate, 
                                   String userRole, Long userId, TeacherStudentService teacherStudentService) {
        LambdaQueryWrapper<LabReserve> q = new LambdaQueryWrapper<>();
        q.eq(labId != null, LabReserve::getLabId, labId)
                .eq(status != null && !status.isEmpty(), LabReserve::getStatus, status)
                .eq(reserveDate != null, LabReserve::getReserveDate, reserveDate);
        
        if ("TEACHER".equals(userRole) && teacherStudentService != null) {
            // 教师只能看到其学员的预约
            List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                    .eq(TeacherStudent::getTeacherId, userId)
                    .list();
            List<Long> studentIds = relations.stream()
                    .map(TeacherStudent::getStudentId)
                    .collect(Collectors.toList());
            
            if (studentIds.isEmpty()) {
                studentIds.add(-1L); // 没有学员时返回空结果
            }
            q.in(LabReserve::getUserId, studentIds);
        }
        
        q.orderByDesc(LabReserve::getCreateTime);
        return page(page, q);
    }
    
    public List<LabReserve> listAll(Long labId, String status, LocalDate reserveDate, 
                                   String userRole, Long userId, TeacherStudentService teacherStudentService) {
        LambdaQueryWrapper<LabReserve> q = new LambdaQueryWrapper<>();
        q.eq(labId != null, LabReserve::getLabId, labId)
                .eq(status != null && !status.isEmpty(), LabReserve::getStatus, status)
                .eq(reserveDate != null, LabReserve::getReserveDate, reserveDate);
        
        if ("TEACHER".equals(userRole) && teacherStudentService != null) {
            // 教师只能看到其学员的预约
            List<TeacherStudent> relations = teacherStudentService.lambdaQuery()
                    .eq(TeacherStudent::getTeacherId, userId)
                    .list();
            List<Long> studentIds = relations.stream()
                    .map(TeacherStudent::getStudentId)
                    .collect(Collectors.toList());
            
            if (studentIds.isEmpty()) {
                studentIds.add(-1L); // 没有学员时返回空结果
            }
            q.in(LabReserve::getUserId, studentIds);
        }
        
        q.orderByDesc(LabReserve::getCreateTime);
        return list(q);
    }
}
