package com.management.shop.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    // Spring will automatically call this constructor when starting up
    public FirebaseConfig() {
        System.out.println("⏳ Attempting to initialize Firebase...");
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
            System.err.println("🔴 CRITICAL: Failed to initialize Firebase Admin SDK");
            e.printStackTrace();
        }
    }
}