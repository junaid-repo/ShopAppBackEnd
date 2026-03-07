package com.management.shop.controller;

import com.management.shop.service.FCMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private FCMService fcmService;

    @PostMapping("/send")
    public String sendNotification(@RequestBody NotificationRequest request) {
        return fcmService.sendNotification(
                request.getTitle(),
                request.getBody()
        );
    }
    @PostMapping("/save/firebase/permission/token")
    public String saveFirebasePermissionToken(@RequestBody Map<String, String> request) {

        fcmService.saveFirebaseToken(request);

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
