package com.spring.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.spring.service.impl.SongServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(SongServiceImpl.class);

    @Value("${app.firebase-config}")
    private String firebaseCredentials;

    @PostConstruct
    private void initialize() {
        try {
//            FirebaseOptions options = new FirebaseOptions.Builder()
//                    .setCredentials(GoogleCredentials.fromStream(
//                            new ClassPathResource(firebaseConfig).getInputStream()))
//                    .build();
//
//            if (FirebaseApp.getApps().isEmpty()) {
//                FirebaseApp.initializeApp(options);
//            }
            if (firebaseCredentials == null || firebaseCredentials.isEmpty()) {
                throw new IllegalArgumentException("FIREBASE_CREDENTIALS environment variable is not set");
            }
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(firebaseCredentials.getBytes(StandardCharsets.UTF_8)));
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);
            System.out.println("FirebaseApp initialized successfully");
        } catch (IOException e) {
            log.error("Create FirebaseApp Error", e);
            throw new RuntimeException("Failed to initialize FirebaseApp", e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
