package com.lab.controller;

import com.lab.common.Result;
import com.lab.entity.SystemConfig;
import com.lab.service.SystemConfigService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/system-config")
@PreAuthorize("hasRole('SYS_ADMIN')")
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 获取所有配置
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有配置
        List<SystemConfig> configs = systemConfigService.list();
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        
        return Result.ok(result);
    }

    /**
     * 更新配置
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            systemConfigService.updateConfig(entry.getKey(), entry.getValue());
        }
        return Result.ok();
    }

    /**
     * 更新单个配置
     */
    @PostMapping("/{key}")
    public Result<Void> updateByKey(@PathVariable String key, @RequestParam String value) {
        systemConfigService.updateConfig(key, value);
        return Result.ok();
    }

    /**
     * 获取配置值
     */
    @GetMapping("/{key}")
    public Result<String> get(@PathVariable String key) {
        String value = systemConfigService.getConfigValue(key);
        return Result.ok(value);
    }
}
