package com.spring.outfit_rater.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = getCredentialsInputStream();
                
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setStorageBucket(storageBucket)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully with bucket: {}", storageBucket);
            } else {
                log.info("Firebase already initialized");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    private InputStream getCredentialsInputStream() throws IOException {
        try {
            Resource resource = new ClassPathResource(credentialsPath);
            if (resource.exists()) {
                log.info("Loading Firebase credentials from classpath: {}", credentialsPath);
                return resource.getInputStream();
            }
        } catch (Exception e) {
            log.warn("Could not load credentials from classpath: {}", credentialsPath);
        }

        try {
            java.io.File file = new java.io.File(credentialsPath);
            if (file.exists()) {
                log.info("Loading Firebase credentials from file system: {}", credentialsPath);
                return new java.io.FileInputStream(file);
            }
        } catch (Exception e) {
            log.warn("Could not load credentials from file system: {}", credentialsPath);
        }

        throw new IOException("Firebase service account file not found at: " + credentialsPath + 
                            ". Please ensure the file exists in src/main/resources/ or provide absolute path.");
    }
}