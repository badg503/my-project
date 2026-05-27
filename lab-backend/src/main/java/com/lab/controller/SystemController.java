package com.lab.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.Result;
import com.lab.entity.SysAnnouncement;
import com.lab.entity.AiModelConfig;
import com.lab.service.SysAnnouncementService;
import com.lab.service.AiModelConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system")
public class SystemController {

    private final SysAnnouncementService announcementService;
    private final AiModelConfigService aiModelConfigService;

    public SystemController(SysAnnouncementService announcementService, AiModelConfigService aiModelConfigService) {
        this.announcementService = announcementService;
        this.aiModelConfigService = aiModelConfigService;
    }

    // ==================== 公告管理 ====================
    
    @GetMapping("/announcements")
    public Result<List<SysAnnouncement>> listAnnouncements() {
        return Result.ok(announcementService.lambdaQuery()
                .eq(SysAnnouncement::getStatus, 1)
                .orderByDesc(SysAnnouncement::getCreateTime)
                .list());
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @GetMapping("/announcements/page")
    public Result<Page<SysAnnouncement>> pageAnnouncements(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(announcementService.page(new Page<>(current, size),
                new LambdaQueryWrapper<SysAnnouncement>()
                        .orderByDesc(SysAnnouncement::getCreateTime)));
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PostMapping("/announcements")
    public Result<Void> addAnnouncement(@RequestBody SysAnnouncement announcement, HttpServletRequest req) {
        announcement.setPublisherId((Long) req.getAttribute("userId"));
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        announcementService.save(announcement);
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @PutMapping("/announcements")
    public Result<Void> updateAnnouncement(@RequestBody SysAnnouncement announcement) {
        announcement.setUpdateTime(LocalDateTime.now());
        announcementService.updateById(announcement);
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN') or hasRole('LAB_ADMIN')")
    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.removeById(id);
        return Result.ok();
    }

    // ==================== AI模型配置 ====================
    
    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/ai-config")
    public Result<List<AiModelConfig>> listAiConfigs() {
        return Result.ok(aiModelConfigService.lambdaQuery()
                .orderByAsc(AiModelConfig::getModelType)
                .list());
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PutMapping("/ai-config")
    public Result<Void> updateAiConfig(@RequestBody AiModelConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        aiModelConfigService.updateById(config);
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PostMapping("/ai-config")
    public Result<Void> addAiConfig(@RequestBody AiModelConfig config) {
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        aiModelConfigService.save(config);
        return Result.ok();
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @DeleteMapping("/ai-config/{id}")
    public Result<Void> deleteAiConfig(@PathVariable Long id) {
        aiModelConfigService.removeById(id);
        return Result.ok();
    }
}
