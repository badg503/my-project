package com.lab.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.LabInfo;
import com.lab.mapper.LabInfoMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class LabInfoService extends ServiceImpl<LabInfoMapper, LabInfo> {

    @Cacheable(value = "labInfo", key = "#id")
    public LabInfo getById(Long id) {
        return super.getById(id);
    }

    public Page<LabInfo> pageList(Page<LabInfo> page, String name, Integer status) {
        return page(page, new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LabInfo>()
                .like(name != null && !name.isEmpty(), LabInfo::getName, name)
                .eq(status != null, LabInfo::getStatus, status)
                .orderByDesc(LabInfo::getCreateTime));
    }
}
