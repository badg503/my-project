package com.lab.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.entity.SystemConfig;
import com.lab.mapper.SystemConfigMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务
 */
@Service
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    /**
     * 获取所有配置
     */
    public Map<String, String> getAllConfig() {
        List<SystemConfig> configs = list();
        Map<String, String> configMap = new HashMap<>();
        for (SystemConfig config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        return configMap;
    }

    /**
     * 更新配置
     */
    @CacheEvict(value = "systemConfig", key = "#key")
    public void updateConfig(String key, String value) {
        SystemConfig config = lambdaQuery()
                .eq(SystemConfig::getConfigKey, key)
                .one();
        
        if (config != null) {
            config.setConfigValue(value);
            updateById(config);
        } else {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            save(config);
        }
    }

    /**
     * 获取配置值
     */
    @Cacheable(value = "systemConfig", key = "#key")
    public String getConfigValue(String key) {
        SystemConfig config = lambdaQuery()
                .eq(SystemConfig::getConfigKey, key)
                .one();
        return config != null ? config.getConfigValue() : null;
    }
}
