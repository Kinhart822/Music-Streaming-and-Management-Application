package com.spring.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Configuration
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.type}")
    private String type;

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.private-key-id}")
    private String privateKeyId;

    @Value("${firebase.private-key}")
    private String privateKey;

    @Value("${firebase.client-email}")
    private String clientEmail;

    @Value("${firebase.client-id}")
    private String clientId;

    @Value("${firebase.auth-uri}")
    private String authUri;

    @Value("${firebase.token-uri}")
    private String tokenUri;

    @Value("${firebase.auth-provider-x509-cert-url}")
    private String authProviderX509CertUrl;

    @Value("${firebase.client-x509-cert-url}")
    private String clientX509CertUrl;

    @Value("${firebase.universe-domain}")
    private String universeDomain;

    @PostConstruct
    private void initialize() {
        try {
            // Validate required fields
            if (projectId == null || projectId.trim().isEmpty()) {
                log.error("firebase.project-id is not set");
                throw new IllegalArgumentException("firebase.project-id is not set");
            }
            if (privateKey == null || privateKey.trim().isEmpty()) {
                log.error("firebase.private-key is not set");
                throw new IllegalArgumentException("firebase.private-key is not set");
            }
            if (clientEmail == null || clientEmail.trim().isEmpty()) {
                log.error("firebase.client-email is not set");
                throw new IllegalArgumentException("firebase.client-email is not set");
            }
            if (clientId == null || clientId.trim().isEmpty()) {
                log.error("firebase.client-id is not set");
                throw new IllegalArgumentException("firebase.client-id is not set");
            }

            // Reconstruct JSON for ServiceAccountCredentials
            String json = String.format(
                "{"
                    + "\"type\": \"%s\","
                    + "\"project_id\": \"%s\","
                    + "\"private_key_id\": \"%s\", "
                    + "\"private_key\": \"%s\", "
                    + "\"client_email\": \"%s\", "
                    + "\"client_id\": \"%s\", "
                    + "\"auth_uri\": \"%s\", "
                    + "\"token_uri\": \"%s\", "
                    + "\"auth_provider_x509_cert_url\": \"%s\", "
                    + "\"client_x509_cert_url\": \"%s\", "
                    + "\"universe_domain\": \"%s\""
                + "}",
                type,
                projectId,
                privateKeyId,
                privateKey.replace("\\n", "\n"),
                clientEmail,
                clientId,
                authUri,
                tokenUri,
                authProviderX509CertUrl,
                clientX509CertUrl,
                universeDomain
            );

            log.info("Constructed Firebase credentials JSON (first 50 chars): {}", json.substring(0, Math.min(50, json.length())));

            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
            ).createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized successfully");
            } else {
                log.info("FirebaseApp already initialized");
            }
        } catch (IOException e) {
            log.error("Failed to initialize FirebaseApp", e);
            throw new RuntimeException("Failed to initialize FirebaseApp", e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}