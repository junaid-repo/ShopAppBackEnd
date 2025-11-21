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
    public void evictsUserGoals(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("goals");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("goals::" + username + "::"));
        }
    }
    public void evictsUserAnalytics(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("analytics");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("analytics::" + username + "::"));
        }
    }
    public void evictsTopSelling(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("topSellings");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("topSellings::" + username + "::"));
        }
    }
    public void evictsTopOrders(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("topOrders");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("topOrders::" + username + "::"));
        }
    }
    public void evictsPaymentBreakdowns(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("paymentBreakdowns");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("paymentBreakdowns::" + username + "::"));
        }
    }
    public void evictsReportsCache(String username) {
        org.springframework.cache.Cache springCache = cacheManager.getCache("reports");
        if (springCache != null) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) springCache.getNativeCache();
            nativeCache.asMap().keySet()
                    .removeIf(k -> k.toString().startsWith("reports::" + username + "::"));
        }
    }
}
