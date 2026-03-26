package com.management.shop.controller;


import com.management.shop.dto.ReportSchedulerSettings;
import com.management.shop.dto.SchedulerSettings;
import com.management.shop.dto.ShopSettings;
import com.management.shop.dto.UiSettings;
import com.management.shop.service.SettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class SettingsController {

    @Autowired
    SettingsService serv;

    @PutMapping("api/shop/settings/user/save/ui")
    ResponseEntity<Map<String, String>> saveUserUISettings(@RequestBody UiSettings request) {

        String response = serv.saveUserUISettings(request);

        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("message", "UI settings updated");
        return ResponseEntity.status(HttpStatus.OK).body(responseMap);

    }

    @PutMapping("api/shop/settings/user/save/scheduler")
    ResponseEntity<Map<String, String>> saveUserSchedulerSettings(@RequestBody SchedulerSettings request) {
        System.out.println("Received scheduler settings request: " + request);
        String response = serv.saveUserSchedulerSettings(request);
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("message", "UI settings updated");


        return ResponseEntity.status(HttpStatus.OK).body(responseMap);

    }

    @PutMapping("api/shop/settings/user/save/reports")
    @PreAuthorize("hasRole('PREMIUM')")
    ResponseEntity<Map<String, String>> saveUserReportSchedulerSettings(@RequestBody ReportSchedulerSettings request) {
        System.out.println("Received report scheduler settings request: " + request);
        String response = serv.saveUserReportSchedulerSettings(request);
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("message", "UI settings updated");


        return ResponseEntity.status(HttpStatus.OK).body(responseMap);

    }

    @PutMapping("api/shop/settings/user/save/billing")
    ResponseEntity<Map<String, String>> saveBillingSettings(@RequestBody Map<String, Object> request) {

        String response = serv.saveBillingSettings(request);
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("message", "UI settings updated");


        return ResponseEntity.status(HttpStatus.OK).body(responseMap);

    }

    @PutMapping("api/shop/settings/user/save/invoice")
    ResponseEntity<Map<String, String>> saveInvoice(@RequestBody Map<String, Object> request) {

        String response = serv.saveInvoiceSetting(request);
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("message", "UI settings updated");


        return ResponseEntity.status(HttpStatus.OK).body(responseMap);

    }

    @GetMapping("api/shop/get/user/settings")
    ResponseEntity<ShopSettings> getFullUserSettings() {

        ShopSettings response = serv.getFullUserSettings();


        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping("api/shop/notifications/settings/save")
    ResponseEntity<Map<String, String>> saveNotificationSettings(@RequestBody Map<String, Object> request) {

        String response = serv.updateNotificationSettings(request);
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("message", "Notification settings updated");

        return ResponseEntity.status(HttpStatus.OK).body(responseMap);
    }
    @GetMapping("/api/shop/notifications/settings/get")
    ResponseEntity<Map<String, Object>> getNotificationSettings() {

        Map<String, Object> response = serv.getNotificationSettings();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("api/shop/check-import-limit")
    ResponseEntity<Map<String, Object>> getCheckImportLimit() {

        Map<String, Object> response = serv.checkImportLimit();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}