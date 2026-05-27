package com.lab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.LabAttendance;
import com.lab.mapper.LabAttendanceMapper;
import org.springframework.stereotype.Service;

@Service
public class LabAttendanceService extends ServiceImpl<LabAttendanceMapper, LabAttendance> {

    public Page<LabAttendance> pageByTask(Page<LabAttendance> page, Long taskId) {
        return page(page, new LambdaQueryWrapper<LabAttendance>().eq(LabAttendance::getTaskId, taskId));
    }

    public long countByTaskAndStatus(Long taskId, String status) {
        return count(new LambdaQueryWrapper<LabAttendance>().eq(LabAttendance::getTaskId, taskId).eq(LabAttendance::getStatus, status));
    }

    public long countByTaskAndGender(Long taskId, String gender) {
        // 这里需要联表查询，后续会实现
        return 0;
    }
}
