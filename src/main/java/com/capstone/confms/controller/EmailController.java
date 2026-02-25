package com.capstone.confms.controller;


import com.capstone.confms.dto.EmailDTO;
import com.capstone.confms.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Tag(name = "Email Management", description = "Operations related to Email setup")
public class EmailController {


    private final EmailService emailService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.redirect.accept}")
    private String acceptRedirectUrl;

    @Value("${app.redirect.decline}")
    private String declineRedirectUrl;


    @PostMapping
    @Operation(summary = "Send an email", description = "Send an email to a specified recipient with subject and text content")
    public ResponseEntity<String> sendSimpleEmail(@Valid @RequestBody EmailDTO emailDTO) {
        try{
            emailService.sendSimpleMessage(emailDTO.getTo(), emailDTO.getSubject(), emailDTO.getText());
            return ResponseEntity.ok("Email sent successfully to: " + emailDTO.getTo());
        } catch (MailException e){
            return ResponseEntity.internalServerError().body("Send email unsuccessfully. Internal mail server error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Unexpected error occurred while sending email: " + e.getMessage());
        }
    }

    @PostMapping(value = "/invite", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Send an HTML email report with optional file attachment", description = "Send an email with HTML content and optional file attachment. The email includes Accept and Decline links for user interaction.")
    public ResponseEntity<String> sendAdvancedEmail(
            @RequestParam("to") String to,
            @RequestParam("doctorName") String doctorName,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        try {
            ByteArrayResource fileData = null;
            String fileName = null;

            if(file != null && !file.isEmpty()) {
                fileData = new ByteArrayResource(file.getBytes());
                fileName = file.getOriginalFilename();
            }

            emailService.sendAdvancedEmail(to, doctorName, acceptRedirectUrl, declineRedirectUrl, fileData, fileName);
            return ResponseEntity.ok("Email sent successfully to: " + to);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Send email unsuccessfully. Internal mail server error: " + e.getMessage());
        }
    }


    @GetMapping("/accept/{token}")
    @Operation(summary = "Execute when user clicks Accept in the email", description = "Handle the logic when a user clicks the Accept link in the email. The token is used to identify the specific request/transaction.")
    public ResponseEntity<String> acceptEmail(@PathVariable String token) {
        // TODO: Query database to find the request/transaction associated with the token

        return ResponseEntity.ok("<h1>Cảm ơn bạn!</h1>\n" +
                "<p>Bạn đã XÁC NHẬN yêu cầu thành công.</p>\n" +
                "<br>\n" +
                "<a href=\"https://youtu.be/YzKMMbBsa8s?si=0PYHeI46Td-8PV6b\" style=\"display: inline-block; padding: 10px 20px; background-color: #28a745; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;\">Tiếp tục</a>");
    }

    @GetMapping("/decline/{token}")
    @Operation(summary = "Execute when user clicks Decline in the email", description = "Handle the logic when a user clicks the Decline link in the email. The token is used to identify the specific request/transaction.")
    public ResponseEntity<String> declineEmail(@PathVariable String token) {
        // TODO: Query database to find the request/transaction associated with the token

        return ResponseEntity.ok("<h1>Đã hủy!</h1><p>Bạn đã TỪ CHỐI yêu cầu này.</p>");
    }
}
