package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.AuthorNotificationRequestDTO;
import com.capstone.confhub.service.AuthorNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/author-notification")
@RequiredArgsConstructor
@Tag(name = "Author Notification", description = "Send decision notifications to paper authors")
public class AuthorNotificationController {

    private final AuthorNotificationService authorNotificationService;

    @PostMapping("/conference/{conferenceId}")
    @Operation(summary = "Send notifications to authors based on paper status")
    public ResponseEntity<Map<Integer, String>> sendNotifications(
            @PathVariable Integer conferenceId,
            @RequestBody AuthorNotificationRequestDTO request) {
        return ResponseEntity.ok(authorNotificationService.sendAuthorNotifications(conferenceId, request));
    }
}
