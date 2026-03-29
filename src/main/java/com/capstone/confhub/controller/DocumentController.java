package com.capstone.confhub.controller;

import com.capstone.confhub.security.services.UserDetailsImpl;
import com.capstone.confhub.service.DocumentGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentGenerationService documentGenerationService;

    @GetMapping("/papers/{id}/acceptance-letter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getAcceptanceLetter(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        byte[] pdfBytes = documentGenerationService.generateAcceptanceLetter(id, userId);
        return buildPdfResponse(pdfBytes, "acceptance_letter_" + id + ".pdf");
    }

    @GetMapping("/tickets/{id}/invoice")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getInvoice(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        byte[] pdfBytes = documentGenerationService.generateInvoice(id, userId);
        return buildPdfResponse(pdfBytes, "invoice_" + id + ".pdf");
    }

    @GetMapping("/tickets/{id}/certificate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getCertificate(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        byte[] pdfBytes = documentGenerationService.generateCertificate(id, userId);
        return buildPdfResponse(pdfBytes, "certificate_" + id + ".pdf");
    }

    @GetMapping("/conferences/{id}/proceedings")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Export all camera-ready PDFs as a ZIP archive (Chair/ProgramChair only)")
    public ResponseEntity<byte[]> exportProceedings(@PathVariable Integer id) {
        byte[] zipBytes = documentGenerationService.exportProceedings(id);
        return buildZipResponse(zipBytes, "proceedings_conference_" + id + ".zip");
    }

    @GetMapping("/conferences/{id}/invoices")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Export all completed-payment invoices as a ZIP archive (Chair only)")
    public ResponseEntity<byte[]> exportInvoices(@PathVariable Integer id) {
        byte[] zipBytes = documentGenerationService.exportInvoices(id);
        return buildZipResponse(zipBytes, "invoices_conference_" + id + ".zip");
    }

    private Integer getCurrentUserId() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getId();
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdfBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> buildZipResponse(byte[] zipBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
    }
}

