package com.capstone.confms.service.impl;

import com.capstone.confms.entity.Payment;
import com.capstone.confms.integration.payment.VnPayIntegrationService;
import com.capstone.confms.repository.PaymentRepository;
import com.capstone.confms.service.PaymentService;
import com.capstone.confms.utils.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final VnPayIntegrationService vnPayIntegrationService;
    private final PaymentRepository PaymentRepository;

    public String createVnPayPayment(Long amount, HttpServletRequest request) {
        String ipAddr = VnPayUtil.getIpAddress(request);

        return vnPayIntegrationService.createPaymentUrl(amount, ipAddr);
    }

    public String processVnPayCallback(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = (String) params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        boolean isVerified = vnPayIntegrationService.verifyPaymentSignature(fields, vnp_SecureHash);

        String paymentIdStr = request.getParameter("vnp_OrderInfo");
        String returnUrl = "url" + paymentIdStr;

        if (isVerified) {
            String transactionStatus = request.getParameter("vnp_TransactionStatus");
            if ("00".equals(transactionStatus)) {
                //TODO: Logic update DB
                return returnUrl + "?status=success";
            } else {
                return returnUrl + "?status=failed";
            }
        }
        return returnUrl + "?status=invalid_signature";
    }
}