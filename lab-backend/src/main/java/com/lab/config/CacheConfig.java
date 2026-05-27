package com.lab.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置（Caffeine）
 * 生产环境可切换为 Redis
 */
@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 配置缓存：user（用户信息）
        cacheManager.registerCustomCache("user", 
            Caffeine.newBuilder()
                .maximumSize(1000)           // 最多 1000 条
                .expireAfterWrite(1, TimeUnit.HOURS)  // 1 小时过期
                .build());
        
        // 配置缓存：labInfo（实验室信息）
        cacheManager.registerCustomCache("labInfo", 
            Caffeine.newBuilder()
                .maximumSize(500)            // 最多 500 条
                .expireAfterWrite(10, TimeUnit.MINUTES)  // 10 分钟过期
                .build());
        
        // 配置缓存：systemConfig（系统配置）
        cacheManager.registerCustomCache("systemConfig", 
            Caffeine.newBuilder()
                .maximumSize(100)            // 最多 100 条
                .expireAfterWrite(24, TimeUnit.HOURS)  // 24 小时过期
                .build());
        
        return cacheManager;
    }
}

/*
// ============================================
// Redis 缓存配置（生产环境使用）
// ============================================
// 使用说明：
// 1. 安装 Redis：docker run -d -p 6379:6379 redis:latest
// 2. 在 pom.xml 启用 Redis 依赖
// 3. 注释掉上面的 Caffeine 配置
// 4. 取消注释下面的 Redis 配置
// ============================================

package com.lab.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        RedisCacheConfiguration userConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1));

        RedisCacheConfiguration labConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10));

        RedisCacheConfiguration configConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("user", userConfig)
                .withCacheConfiguration("labInfo", labConfig)
                .withCacheConfiguration("systemConfig", configConfig)
                .build();
    }
}
*/
