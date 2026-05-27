package com.lab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.TeacherStudent;
import com.lab.mapper.TeacherStudentMapper;
import com.lab.service.TeacherStudentService;
import org.springframework.stereotype.Service;

@Service
public class TeacherStudentServiceImpl extends ServiceImpl<TeacherStudentMapper, TeacherStudent> implements TeacherStudentService {
}
