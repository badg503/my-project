package com.lab.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.DeviceBorrow;
import com.lab.mapper.DeviceBorrowMapper;
import org.springframework.stereotype.Service;

@Service
public class DeviceBorrowService extends ServiceImpl<DeviceBorrowMapper, DeviceBorrow> {
    
    public Page<DeviceBorrow> pageByStatus(Page<DeviceBorrow> page, String status) {
        if (status != null && !status.isEmpty()) {
            return lambdaQuery().eq(DeviceBorrow::getStatus, status).orderByDesc(DeviceBorrow::getCreateTime).page(page);
        }
        return lambdaQuery().orderByDesc(DeviceBorrow::getCreateTime).page(page);
    }
}
