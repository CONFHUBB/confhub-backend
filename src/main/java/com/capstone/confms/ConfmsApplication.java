package com.capstone.confms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
@Slf4j
public class ConfmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfmsApplication.class, args);
        openSwaggerUI();
    }

    private static void openSwaggerUI() {
        String url = "http://localhost:8080/swagger-ui.html";
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
