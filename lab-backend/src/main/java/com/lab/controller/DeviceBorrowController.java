package com.lab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.Result;
import com.lab.entity.DeviceBorrow;
import com.lab.annotation.LogOperation;
import com.lab.service.DeviceBorrowService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/device-borrow")
public class DeviceBorrowController {

    private final DeviceBorrowService borrowService;

    public DeviceBorrowController(DeviceBorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @GetMapping("/page")
    public Result<Page<DeviceBorrow>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        return Result.ok(borrowService.pageByStatus(new Page<>(current, size), status));
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PostMapping("/approve")
    @LogOperation(module = "设备借用", type = "审核", value = "审核设备借用")
    public Result<Void> approve(@RequestParam Long id, @RequestParam String status) {
        DeviceBorrow borrow = borrowService.getById(id);
        if (borrow == null) return Result.fail("借用记录不存在");
        
        borrow.setStatus(status);
        borrowService.updateById(borrow);
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PostMapping("/return")
    @LogOperation(module = "设备借用", type = "归还", value = "归还设备")
    public Result<Void> returnDevice(@RequestParam Long id, @RequestParam String returnRemark) {
        DeviceBorrow borrow = borrowService.getById(id);
        if (borrow == null) return Result.fail("借用记录不存在");
        
        borrow.setStatus("RETURNED");
        borrow.setReturnRemark(returnRemark);
        borrow.setReturnTime(LocalDateTime.now());
        borrowService.updateById(borrow);
        return Result.ok();
    }
}
