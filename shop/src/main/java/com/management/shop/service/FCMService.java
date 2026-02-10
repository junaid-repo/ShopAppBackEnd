package com.management.shop.service;

import com.google.firebase.messaging.*;
import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.FirebaseTokenEntity;
import com.management.shop.repository.FirebaseTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class FCMService {

    @Autowired
    private FirebaseTokenRepository firebaseRepo;
    private static final String FRONTEND_BASE_URL = "http://localhost:3000";

    private final ReentrantLock reentrantLock = new ReentrantLock();

    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // For testing purposes, you might uncomment the line below
        // username="junaid1";
        return username;
    }

    public String sendNotification(String title, String body) {
        try {
            Notification notification = Notification.builder().setTitle(title).setBody(body).build();
            Message message = Message.builder().setToken(getToken(extractUsername())).setNotification(notification).build();
            String response = FirebaseMessaging.getInstance().send(message);
            return "Successfully sent message: " + response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error sending message: " + e.getMessage();
        }
    }

    public String sendNotification(String title, String body, String username) {
        try {
            Notification notification = Notification.builder().setTitle(title).setBody(body).build();
            Message message = Message.builder().setToken(getToken(username)).setNotification(notification).build();
            String response = FirebaseMessaging.getInstance().send(message);
            return "Successfully sent message: " + response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error sending message: " + e.getMessage();
        }
    }


    public void saveFirebaseToken(Map<String, String> request) {
        String token = request.get("token");
        String deviceType = request.get("deviceType");
        FirebaseTokenEntity existingToken = firebaseRepo.findByDeviceIdAndUsername(token, extractUsername());
        if (existingToken != null){
            if(!(existingToken.getFirebaseToken().equals(token))){
                firebaseRepo.updateExistingToken(token,extractUsername(), LocalDateTime.now());
            }


        }
           else {
            FirebaseTokenEntity fireBaseTokenEntity = FirebaseTokenEntity.builder().firebaseToken(token).deviceType(deviceType).username(extractUsername()).lastUpdatedBy(extractUsername()).lastUpdatedDate(LocalDateTime.now()).build();
            FirebaseTokenEntity firebaseTokenEntity1 = firebaseRepo.save(fireBaseTokenEntity);
        }
    }

    public String getToken(String username) {
        FirebaseTokenEntity tokenEntity = firebaseRepo.findTopByUsernameOrderByLastUpdatedDateDesc(username);
        if (tokenEntity != null)
            return tokenEntity.getFirebaseToken();
        return null;
    }
}