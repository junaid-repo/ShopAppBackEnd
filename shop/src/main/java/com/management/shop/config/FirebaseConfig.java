package com.management.shop.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Check if Firebase is already initialized to avoid conflicts during hot-reloads
        if (FirebaseApp.getApps().isEmpty()) {

            InputStream serviceAccount;
            String firebaseEnv = System.getenv("FIREBASE_CREDENTIALS");

            // Check if we are in production (Render) and have the environment variable
            if (firebaseEnv != null && !firebaseEnv.trim().isEmpty()) {
                serviceAccount = new ByteArrayInputStream(firebaseEnv.getBytes(StandardCharsets.UTF_8));
            } else {
                // Fallback for local development using the gitignored file
                ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
                serviceAccount = resource.getInputStream();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }
}