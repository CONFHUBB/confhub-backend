package com.capstone.confms.integration.payment;

import java.util.Map;

public interface VnPayIntegrationService {
    String createPaymentUrl(long amount, String ipAddr);
    boolean verifyPaymentSignature(Map<String, String> fields, String vnp_SecureHash);
}
