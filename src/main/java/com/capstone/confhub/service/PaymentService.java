package com.capstone.confhub.service;

import jakarta.servlet.http.HttpServletRequest;

public interface PaymentService {
    String createVnPayPayment(Long amount, HttpServletRequest request);
    String processVnPayCallback(HttpServletRequest request);
}
