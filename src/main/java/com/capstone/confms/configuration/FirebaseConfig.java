package com.capstone.confms.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() throws IOException {
        FileInputStream serviceAccount =
            new FileInputStream("./src/main/resources/filestorage-71871-firebase-adminsdk-fbsvc-7ba8487811.json");

        FirebaseOptions options = FirebaseOptions.builder()
                                                 .setStorageBucket("filestorage-71871.firebasestorage.app")
                                                 .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                                 .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
    @Bean
    public StorageClient storageClient() {
        return StorageClient.getInstance();
    }

}