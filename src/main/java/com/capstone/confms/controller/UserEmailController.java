package com.capstone.confms.controller;

import com.capstone.confms.dto.request.UserEmailRequest;
import com.capstone.confms.dto.response.UserEmailResponseDTO;
import com.capstone.confms.service.UserEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/emails")
@RequiredArgsConstructor
@Tag(name = "User Email Management", description = "Endpoints for managing user email addresses")
public class UserEmailController {

    private final UserEmailService userEmailService;

    @GetMapping
    @Operation(summary = "Get all emails for a user")
    public ResponseEntity<List<UserEmailResponseDTO>> getEmails(@PathVariable Integer userId) {
        return ResponseEntity.ok(userEmailService.getEmailsByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Add a new email address for a user")
    public ResponseEntity<UserEmailResponseDTO> addEmail(
            @PathVariable Integer userId,
            @Valid @RequestBody UserEmailRequest request) {
        return new ResponseEntity<>(userEmailService.addEmail(userId, request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{emailId}")
    @Operation(summary = "Delete an email address")
    public ResponseEntity<Void> deleteEmail(@PathVariable Integer userId, @PathVariable Integer emailId) {
        userEmailService.deleteEmail(emailId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{emailId}/set-primary")
    @Operation(summary = "Set an email as primary")
    public ResponseEntity<UserEmailResponseDTO> setPrimaryEmail(
            @PathVariable Integer userId, @PathVariable Integer emailId) {
        return ResponseEntity.ok(userEmailService.setPrimaryEmail(emailId));
    }
}
