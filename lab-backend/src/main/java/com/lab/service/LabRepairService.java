package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.LabRepair;
import com.lab.entity.LabDevice;
import com.lab.entity.LabInfo;
import com.lab.mapper.LabRepairMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabRepairService extends ServiceImpl<LabRepairMapper, LabRepair> {

    @Resource
    private LabDeviceService labDeviceService;
    
    @Resource
    private LabInfoService labInfoService;

    public Page<LabRepair> pageByReporter(Page<LabRepair> page, Long reporterId) {
        Page<LabRepair> result = page(page, new LambdaQueryWrapper<LabRepair>().eq(LabRepair::getReporterId, reporterId).orderByDesc(LabRepair::getCreateTime));
        fillDeviceAndLabInfo(result.getRecords());
        return result;
    }

    public Page<LabRepair> pageAll(Page<LabRepair> page, Long deviceId, String status) {
        LambdaQueryWrapper<LabRepair> q = new LambdaQueryWrapper<>();
        q.eq(deviceId != null, LabRepair::getDeviceId, deviceId).eq(status != null && !status.isEmpty(), LabRepair::getStatus, status).orderByDesc(LabRepair::getCreateTime);
        Page<LabRepair> result = page(page, q);
        fillDeviceAndLabInfo(result.getRecords());
        return result;
    }
    
    private void fillDeviceAndLabInfo(List<LabRepair> repairs) {
        if (repairs.isEmpty()) return;
        
        // 获取所有设备ID
        List<Long> deviceIds = repairs.stream().map(LabRepair::getDeviceId).distinct().collect(Collectors.toList());
        if (deviceIds.isEmpty()) return;
        
        // 批量获取设备信息
        List<LabDevice> devices = labDeviceService.listByIds(deviceIds);
        Map<Long, LabDevice> deviceMap = devices.stream().collect(Collectors.toMap(LabDevice::getId, d -> d));
        
        // 获取所有实验室ID
        List<Long> labIds = devices.stream().map(LabDevice::getLabId).distinct().collect(Collectors.toList());
        if (!labIds.isEmpty()) {
            List<LabInfo> labs = labInfoService.listByIds(labIds);
            Map<Long, String> labNameMap = labs.stream().collect(Collectors.toMap(LabInfo::getId, LabInfo::getName));
            
            // 填充设备和实验室信息
            for (LabRepair repair : repairs) {
                LabDevice device = deviceMap.get(repair.getDeviceId());
                if (device != null) {
                    repair.setDeviceName(device.getName());
                    repair.setLabId(device.getLabId());
                    repair.setLabName(labNameMap.get(device.getLabId()));
                }
            }
        }
    }
}
