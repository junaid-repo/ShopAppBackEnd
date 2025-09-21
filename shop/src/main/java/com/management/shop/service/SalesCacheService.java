package com.management.shop.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class SalesCacheService {

    @Autowired
    private CacheManager cacheManager;

    public void evictUserSales(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("sales");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("sales::" + username + "::"));
        }
    }
    public void evictUserPayments(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("payments");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("payments::" + username + "::"));
        }
    }
    public void evictUserCustomers(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("customers");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("customers::" + username + "::"));
        }
    }
    public void evictUserProducts(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("products");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("products::" + username + "::"));
        }
    }
    public void evictUserDasbhoard(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("dashboard");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("dashboard::" + username + "::"));
        }
    }
}
