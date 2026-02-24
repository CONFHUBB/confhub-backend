package com.capstone.confms.controller;

import com.capstone.confms.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "Operations related to Payment")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/vnpay/create")
    public String createPayment(Long amount, HttpServletRequest request) {
        return paymentService.createVnPayPayment(amount, request);
    }

    @GetMapping("/vnpay-callback")
    public void vnpayCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String redirectUrl = paymentService.processVnPayCallback(request);
        response.sendRedirect(redirectUrl);
    }
}