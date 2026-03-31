package com.management.shop.config;

import org.redisson.api.RedissonClient;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager; // Spring's CacheManager
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider; // <-- Added missing import

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RateLimitCacheConfig {

     @Bean
    @Primary
    public CacheManager springCacheManager(RedissonClient redissonClient) {

        Map<String, CacheConfig> config = new HashMap<>();

         long tenMinutes = 10 * 60 * 1000;
        long fiveMinutes = 5 * 60 * 1000;

         config.put("customers", new CacheConfig(tenMinutes, tenMinutes));
        config.put("orders", new CacheConfig(tenMinutes, tenMinutes));
        config.put("sales", new CacheConfig(tenMinutes, tenMinutes));
        config.put("products", new CacheConfig(tenMinutes, tenMinutes));
        config.put("userSettings", new CacheConfig(tenMinutes, tenMinutes));
         config.put("productCategories", new CacheConfig(tenMinutes, tenMinutes));

         config.put("dashboard", new CacheConfig(fiveMinutes, fiveMinutes));
        config.put("analytics", new CacheConfig(fiveMinutes, fiveMinutes));
        config.put("topSellings", new CacheConfig(fiveMinutes, fiveMinutes));

         return new RedissonSpringCacheManager(redissonClient, config);
    }

     @Bean
    public javax.cache.CacheManager bucket4jCacheManager(RedissonClient redissonClient) {

         CachingProvider provider = Caching.getCachingProvider("org.redisson.jcache.JCachingProvider");
        javax.cache.CacheManager cacheManager = provider.getCacheManager();

         javax.cache.configuration.Configuration<Object, Object> config =
                RedissonConfiguration.fromInstance(redissonClient, new MutableConfiguration<>());

         if (cacheManager.getCache("global-rate-limit") == null) {
            cacheManager.createCache("global-rate-limit", config);
        }
        if (cacheManager.getCache("authenticated-user-limit") == null) {
            cacheManager.createCache("authenticated-user-limit", config);
        }

        return cacheManager;
    }
}