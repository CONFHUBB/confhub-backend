package com.capstone.confhub.service;

public interface DocumentGenerationService {
    byte[] generateAcceptanceLetter(Integer paperId, Integer userId);
    byte[] generateInvoice(Integer ticketId, Integer userId);
    byte[] generateCertificate(Integer ticketId, Integer userId);

    /** Export all camera-ready PDFs for a conference as a ZIP archive (Chair/ProgramChair only). */
    byte[] exportProceedings(Integer conferenceId);

    /** Export all invoices for COMPLETED tickets in a conference as a ZIP archive (Chair only). */
    byte[] exportInvoices(Integer conferenceId);
}
