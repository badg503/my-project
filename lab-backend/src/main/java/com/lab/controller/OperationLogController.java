package com.lab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.Result;
import com.lab.entity.OperationLog;
import com.lab.service.OperationLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 操作日志控制器
 */
@RestController
@RequestMapping("/operation-log")
@PreAuthorize("hasRole('SYS_ADMIN')")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping("/page")
    public Result<Page<OperationLog>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.ok(operationLogService.page(new Page<>(current, size), username, module, operationType, startTime, endTime));
    }

    /**
     * 根据 ID 删除日志
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        operationLogService.removeById(id);
        return Result.ok();
    }

    /**
     * 清空所有日志
     */
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        operationLogService.remove(null);
        return Result.ok();
    }
}
