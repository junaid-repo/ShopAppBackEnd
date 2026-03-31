package com.management.shop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SalesCacheService {

    @Autowired
    private CacheManager cacheManager;

    private void evictByPrefix(String cacheName, String prefix) {
        org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
        if (springCache != null) {
             @SuppressWarnings("unchecked")
            Map<Object, Object> nativeCache = (Map<Object, Object>) springCache.getNativeCache();

             nativeCache.keySet().removeIf(k -> k.toString().startsWith(prefix));
        }
    }

    public void evictUserSales(String username) {
        evictByPrefix("sales", "sales::" + username + "::");
    }

    public void evictUserPayments(String username) {
        evictByPrefix("payments", "payments::" + username + "::");
    }

    public void evictUserCustomers(String username) {
        evictByPrefix("customers", "customers::" + username + "::");
    }

    public void evictUserProducts(String username) {
        evictByPrefix("products", "products::" + username + "::");
    }

    public void evictUserDasbhoard(String username) {
        evictByPrefix("dashboard", "dashboard::" + username + "::");
    }

    public void evictsUserGoals(String username) {
        evictByPrefix("goals", "goals::" + username + "::");
    }

    public void evictsUserAnalytics(String username) {
        evictByPrefix("analytics", "analytics::" + username + "::");
    }

    public void evictsTopSelling(String username) {
        evictByPrefix("topSellings", "topSellings::" + username + "::");
    }

    public void evictsTopOrders(String username) {
        evictByPrefix("topOrders", "topOrders::" + username + "::");
    }

    public void evictsPaymentBreakdowns(String username) {
        evictByPrefix("paymentBreakdowns", "paymentBreakdowns::" + username + "::");
    }

    public void evictsReportsCache(String username) {
        evictByPrefix("reports", "reports::" + username + "::");
    }
    public void evictsProductCategoriesCache(String username) {
        evictByPrefix("productCategories", "productCategories::" + username + "::");
    }
}