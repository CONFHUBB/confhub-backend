package com.capstone.confhub.service;

public interface DocumentGenerationService {
    byte[] generateAcceptanceLetter(Integer paperId, Integer userId);
    byte[] generateInvoice(Integer ticketId, Integer userId);
    byte[] generateCertificate(Integer ticketId, Integer userId);
}
