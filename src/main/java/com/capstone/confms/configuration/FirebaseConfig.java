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
            new FileInputStream("./src/main/resources/swp391-1f20e-firebase-adminsdk-qa9da-e001b755fa.json");

        FirebaseOptions options = FirebaseOptions.builder()
                                                 .setStorageBucket("swp391-1f20e.appspot.com")
                                                 .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                                 .build();

        FirebaseApp.initializeApp(options);
    }
    @Bean
    public StorageClient storageClient() {
        return StorageClient.getInstance();
    }

}