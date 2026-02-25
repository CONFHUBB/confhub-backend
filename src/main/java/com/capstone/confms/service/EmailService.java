package com.capstone.confms.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;

    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    @Async
    public void sendAdvancedEmail(String to, String doctorName, String acceptLink, String declineLink, ByteArrayResource fileData, String fileName) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("[Khẩn] Yêu cầu xác nhận kết quả chẩn đoán bệnh truyền nhiễm");

        Context context = new Context();
        context.setVariable("doctorName", doctorName);
        context.setVariable("acceptLink", acceptLink);
        context.setVariable("declineLink", declineLink);

        String htmlBody = templateEngine.process("diagnosis-report", context);
        helper.setText(htmlBody, true);

        if (fileData != null && fileData.contentLength() > 0 && fileName != null && !fileName.isEmpty()) {
            helper.addAttachment(Objects.requireNonNull(fileName), fileData);
        }

        emailSender.send(message);
    }
}
