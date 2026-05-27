package com.lab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.TaskStudent;
import com.lab.mapper.TaskStudentMapper;
import com.lab.service.TaskStudentService;
import org.springframework.stereotype.Service;

@Service
public class TaskStudentServiceImpl extends ServiceImpl<TaskStudentMapper, TaskStudent> implements TaskStudentService {
}
