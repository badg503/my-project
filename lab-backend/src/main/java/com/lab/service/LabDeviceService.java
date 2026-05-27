package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.LabDevice;
import com.lab.entity.LabInfo;
import com.lab.mapper.LabDeviceMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabDeviceService extends ServiceImpl<LabDeviceMapper, LabDevice> {

    @Resource
    private LabInfoService labInfoService;

    public Page<LabDevice> pageByLab(Page<LabDevice> page, Long labId, String status, String deviceType) {
        LambdaQueryWrapper<LabDevice> q = new LambdaQueryWrapper<>();
        q.eq(labId != null, LabDevice::getLabId, labId)
                .eq(status != null && !status.isEmpty(), LabDevice::getStatus, status)
                .eq(deviceType != null && !deviceType.isEmpty(), LabDevice::getDeviceType, deviceType)
                .orderByDesc(LabDevice::getCreateTime);
        Page<LabDevice> result = page(page, q);
        fillLabName(result.getRecords());
        return result;
    }
    
    private void fillLabName(List<LabDevice> devices) {
        if (devices.isEmpty()) return;
        
        // 获取所有实验室ID
        List<Long> labIds = devices.stream().map(LabDevice::getLabId).distinct().collect(Collectors.toList());
        if (labIds.isEmpty()) return;
        
        // 批量获取实验室信息
        List<LabInfo> labs = labInfoService.listByIds(labIds);
        Map<Long, String> labNameMap = labs.stream().collect(Collectors.toMap(LabInfo::getId, LabInfo::getName));
        
        // 填充实验室名称
        for (LabDevice device : devices) {
            device.setLabName(labNameMap.get(device.getLabId()));
        }
    }
}
