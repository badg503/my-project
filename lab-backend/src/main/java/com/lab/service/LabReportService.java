package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.LabReport;
import com.lab.mapper.LabReportMapper;
import org.springframework.stereotype.Service;

@Service
public class LabReportService extends ServiceImpl<LabReportMapper, LabReport> {

    public Page<LabReport> pageByUser(Page<LabReport> page, Long userId) {
        return page(page, new LambdaQueryWrapper<LabReport>().eq(LabReport::getUserId, userId).orderByDesc(LabReport::getCreateTime));
    }

    public Page<LabReport> pageByTask(Page<LabReport> page, Long taskId) {
        return page(page, new LambdaQueryWrapper<LabReport>().eq(LabReport::getTaskId, taskId).orderByDesc(LabReport::getCreateTime));
    }
}
