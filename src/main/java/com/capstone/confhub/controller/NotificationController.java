package com.capstone.confhub.controller;

import com.capstone.confhub.dto.NotificationDTO;
import com.capstone.confhub.dto.response.NotificationResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.FcmPushService;
import com.capstone.confhub.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "APIs for managing user notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final FcmPushService fcmPushService;

    @PostMapping
    @Operation(summary = "Create a notification")
    public ResponseEntity<NotificationResponseDTO> createNotification(
            @Valid @RequestBody NotificationDTO dto) {
        return new ResponseEntity<>(notificationService.createNotification(dto), HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get notifications for a user (paginated, newest first)")
    public ResponseEntity<PagedResponse<NotificationResponseDTO>> getNotificationsByUser(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId, page, size));
    }

    @GetMapping("/user/{userId}/unread-count")
    @Operation(summary = "Get unread notification count for a user")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Integer userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Integer id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark all notifications as read for a user")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Integer userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> deleteNotification(@PathVariable Integer id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    // ── FCM Device Token Management ──

    @PostMapping("/register-device")
    @Operation(summary = "Register a FCM device token for push notifications")
    public ResponseEntity<Map<String, String>> registerDevice(@RequestBody Map<String, Object> body) {
        Integer userId = (Integer) body.get("userId");
        String fcmToken = (String) body.get("fcmToken");
        String deviceType = (String) body.getOrDefault("deviceType", "MOBILE");

        if (userId == null || fcmToken == null || fcmToken.isBlank()) {
            throw new BadRequestException("userId and fcmToken are required");
        }

        fcmPushService.registerToken(userId, fcmToken, deviceType);
        return ResponseEntity.ok(Map.of("status", "registered"));
    }

    @PostMapping("/remove-device")
    @Operation(summary = "Remove a FCM device token (e.g., on logout)")
    public ResponseEntity<Void> removeDevice(@RequestBody Map<String, String> body) {
        String fcmToken = body.get("fcmToken");
        if (fcmToken == null || fcmToken.isBlank()) {
            throw new BadRequestException("fcmToken is required");
        }
        fcmPushService.removeToken(fcmToken);
        return ResponseEntity.noContent().build();
    }
}
