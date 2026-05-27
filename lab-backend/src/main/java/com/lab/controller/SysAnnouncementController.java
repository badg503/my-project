package com.lab.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.Result;
import com.lab.entity.SysAnnouncement;
import com.lab.annotation.LogOperation;
import com.lab.service.SysAnnouncementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/announcement")
public class SysAnnouncementController {

    private final SysAnnouncementService announcementService;

    public SysAnnouncementController(SysAnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /** 前台：已发布公告列表 */
    @GetMapping("/list")
    public Result<List<SysAnnouncement>> list() {
        return Result.ok(announcementService.lambdaQuery()
                .eq(SysAnnouncement::getStatus, 1)
                .orderByDesc(SysAnnouncement::getCreateTime)
                .last("LIMIT 10")
                .list());
    }

    @GetMapping("/page")
    public Result<Page<SysAnnouncement>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(announcementService.page(new Page<>(current, size)));
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PostMapping
    @LogOperation(module = "公告管理", type = "新增", value = "发布公告")
    public Result<Void> add(@RequestBody SysAnnouncement ann, HttpServletRequest req) {
        ann.setPublisherId((Long) req.getAttribute("userId"));
        ann.setStatus(1);
        announcementService.save(ann);
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PutMapping
    @LogOperation(module = "公告管理", type = "修改", value = "修改公告")
    public Result<Void> update(@RequestBody SysAnnouncement ann) {
        announcementService.updateById(ann);
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @DeleteMapping("/{id}")
    @LogOperation(module = "公告管理", type = "删除", value = "删除公告")
    public Result<Void> delete(@PathVariable Long id) {
        // 使用 MyBatis-Plus 的物理删除
        announcementService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<SysAnnouncement> getById(@PathVariable Long id) {
        return Result.ok(announcementService.getById(id));
    }
}
