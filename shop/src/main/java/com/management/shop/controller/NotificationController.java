package com.management.shop.controller;

import com.management.shop.scheduler.NotificationsSaver;
import com.management.shop.service.FCMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private FCMService fcmService;

    @Autowired
    private NotificationsSaver notifications;

    @PostMapping("/send")
    public String sendNotification(@RequestBody NotificationRequest request) {
        return fcmService.sendNotification(
                request.getTitle(),
                request.getBody()
        );
    }
    @PostMapping("/save/firebase/permission/token")
    public String saveFirebasePermissionToken(@RequestBody Map<String, String> request) {
        try {
            fcmService.saveFirebaseToken(request);
            return "Token saved successfully!";
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Firebase token request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unable to save Firebase token. deviceType={}",
                    request == null ? null : request.get("deviceType"), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to save Firebase token",
                    e);
        }
    }
    @PostMapping("/demo/test/notification")
    public String dummyTestNotification() {

        notifications.paymentReminders();

        return "Token saved successfully!";
    }

}

// Simple DTO class
class NotificationRequest {
    private String token;
    private String title;
    private String body;
   private String username;

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
