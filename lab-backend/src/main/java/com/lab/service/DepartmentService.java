package com.lab.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.Department;
import com.lab.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;

@Service
public class DepartmentService extends ServiceImpl<DepartmentMapper, Department> {
}