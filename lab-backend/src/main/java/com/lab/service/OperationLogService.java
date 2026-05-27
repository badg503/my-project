package com.lab.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.OperationLog;
import com.lab.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务
 */
@Service
public class OperationLogService extends ServiceImpl<OperationLogMapper, OperationLog> {

    /**
     * 分页查询操作日志
     */
    public Page<OperationLog> page(Page<OperationLog> page, String username, String module, String operationType, String startTime, String endTime) {
        return lambdaQuery()
                .like(username != null && !username.isEmpty(), OperationLog::getUserName, username)
                .eq(module != null && !module.isEmpty(), OperationLog::getModule, module)
                .eq(operationType != null && !operationType.isEmpty(), OperationLog::getOperationType, operationType)
                .ge(startTime != null && !startTime.isEmpty(), OperationLog::getCreateTime, startTime + " 00:00:00")
                .le(endTime != null && !endTime.isEmpty(), OperationLog::getCreateTime, endTime + " 23:59:59")
                .orderByDesc(OperationLog::getCreateTime)
                .page(page);
    }
}
