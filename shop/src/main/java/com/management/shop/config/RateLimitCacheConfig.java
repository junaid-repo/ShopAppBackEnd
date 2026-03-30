package com.management.shop.config;

import org.redisson.api.RedissonClient;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

@Configuration
public class RateLimitCacheConfig {

    // By explicitly returning javax.cache.CacheManager, Bucket4j will find it,
    // while Spring Boot continues to use Caffeine for your @Cacheable annotations!
    @Bean
    public CacheManager bucket4jCacheManager(RedissonClient redissonClient) {

        // 1. Force the system to load the Redisson JCache provider
        CachingProvider provider = Caching.getCachingProvider("org.redisson.jcache.JCachingProvider");
        CacheManager cacheManager = provider.getCacheManager();

        // 2. Bind it to your existing Spring Boot Redis connection
        javax.cache.configuration.Configuration<Object, Object> config =
                RedissonConfiguration.fromInstance(redissonClient, new MutableConfiguration<>());

        // 3. Create the rate limit buckets (checking if they exist first to prevent errors)
        if (cacheManager.getCache("global-rate-limit") == null) {
            cacheManager.createCache("global-rate-limit", config);
        }
        if (cacheManager.getCache("authenticated-user-limit") == null) {
            cacheManager.createCache("authenticated-user-limit", config);
        }


        // Add your other caches here if needed
        // if (cacheManager.getCache("premium-shop-limit") == null) {
        //     cacheManager.createCache("premium-shop-limit", config);
        // }

        return cacheManager;
    }
}