package com.lab.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.ClassInfo;
import com.lab.mapper.ClassMapper;
import org.springframework.stereotype.Service;

@Service
public class ClassService extends ServiceImpl<ClassMapper, ClassInfo> {
}