package com.management.shop.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Component
@RestController
public class KeepAliveTask {

    private final RestTemplate restTemplate = new RestTemplate();
    // Replace with the actual Render URL for this specific app
    private final String APP_URL = "https://shopappbackend-bybd.onrender.com/ping";

    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }

    // Runs every 10 minutes, from 10:00 AM to 10:59 PM, restricted to IST
    @Scheduled(cron = "0 */10 10-22 * * *", zone = "Asia/Kolkata")
    public void keepAwake() {
        try {
            restTemplate.getForObject(APP_URL, String.class);
            System.out.println("Keep-alive ping sent to: " + APP_URL);
        } catch (Exception e) {
            System.err.println("Failed to ping self: " + e.getMessage());
        }
    }
}
