package com.capstone.confhub.integration.payment;

import java.util.Map;

public interface VnPayIntegrationService {
    String createPaymentUrl(long amount, String ipAddr, Integer ticketId);
    String createPaymentUrl(long amount, String ipAddr, String orderInfo, String txnRef);
    boolean verifyPaymentSignature(Map<String, String> fields, String vnp_SecureHash);
}
