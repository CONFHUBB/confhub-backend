package com.capstone.confhub.integration.payment;

import java.util.Map;

public interface VnPayIntegrationService {
    String createPaymentUrl(long amount, String ipAddr, Integer ticketId);
    boolean verifyPaymentSignature(Map<String, String> fields, String vnp_SecureHash);
}
