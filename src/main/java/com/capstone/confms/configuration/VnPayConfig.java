package com.capstone.confms.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VnPayConfig {
    @Value("${vnpay.url}")
    private String vnp_PayUrl;
    @Value("${vnpay.return-url}")
    private String vnp_ReturnUrl;
    @Value("${vnpay.tmn-code}")
    private String vnp_TmnCode;
    @Value("${vnpay.secret-key}")
    private String vnp_HashSecret;
    @Value("${vnpay.api-url}")
    private String vnp_ApiUrl;
}