package com.management.shop.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.FirebaseTokenEntity;
import com.management.shop.repository.FirebaseTokenRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class FCMService {

    @Autowired
    private FirebaseTokenRepository firebaseRepo;
    @Value("${app.frontend.base-url:https://clearbills.info}")
    private String frontendBaseUrl;

    @PostConstruct
    public void initializeFirebase() {
        System.out.println("⏳ FCMService: Attempting to initialize Firebase...");
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
                InputStream inputStream = resource.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(inputStream))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("🟢 Firebase Admin SDK initialized successfully!");
            }
        } catch (Exception e) {
            System.err.println("🔴 CRITICAL: Failed to initialize Firebase in FCMService");
            e.printStackTrace();
        }
    }

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

            List<String> allToken= firebaseRepo.findAllTokenByUsername(username);

            Notification notification = Notification.builder().setTitle(title).setBody(body).build();
          //  Message message = Message.builder().setToken(getToken(username)).setNotification(notification).putData("url", "https://clearbills.info").build();


            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(allToken) // 🟢 Pass the entire list of tokens here
                    .setNotification(notification)
                    .putData("url", frontendBaseUrl + "/notifications")
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        // If Firebase says the token is dead (Unregistered), delete it!
                        String deadToken = allToken.get(i);
                        System.out.println("⚠️ Token is dead/unregistered. Deleting: " + deadToken);

                        firebaseRepo.deleteByFirebaseToken(deadToken);
                    }
                }
            }
            return "Successfully sent message: " + response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error sending message: " + e.getMessage();
        }
    }


    public void saveFirebaseToken(Map<String, String> request) {
        String token = request.get("token");
        String deviceType = request.get("deviceType");
        FirebaseTokenEntity existingToken = firebaseRepo.findByDeviceIdAndUsername(token, extractUsername(), deviceType);
        if (existingToken != null){
            if(!(existingToken.getFirebaseToken().equals(token))){
                firebaseRepo.updateExistingToken(token,extractUsername(), LocalDateTime.now(), deviceType);
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
