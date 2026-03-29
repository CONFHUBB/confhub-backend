package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.RegistrationRequest;
import com.capstone.confhub.dto.response.CheckInResponse;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.dto.response.RegistrationResponse;
import com.capstone.confhub.dto.response.TicketResponse;
import com.capstone.confhub.service.RegistrationService;
import com.capstone.confhub.utils.VnPayUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Conference Registration", description = "APIs for registering and attending conferences")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/conferences/{conferenceId}/register")
    @Operation(summary = "Register for conference (Author/Attendee)")
    public ResponseEntity<RegistrationResponse> register(
            @PathVariable Integer conferenceId,
            @RequestParam Integer userId,
            @Valid @RequestBody RegistrationRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = VnPayUtil.getIpAddress(httpRequest);
        return new ResponseEntity<>(
                registrationService.register(conferenceId, userId, request, clientIp),
                HttpStatus.CREATED);
    }

    @GetMapping("/conferences/{conferenceId}/my-ticket")
    @Operation(summary = "Get current user's ticket for this conference")
    public ResponseEntity<TicketResponse> getMyTicket(
            @PathVariable Integer conferenceId,
            @RequestParam Integer userId) {
        return ResponseEntity.ok(registrationService.getMyTicket(conferenceId, userId));
    }

    @GetMapping("/my-tickets")
    @Operation(summary = "Get all tickets for the current user across all conferences")
    public ResponseEntity<List<TicketResponse>> getMyTickets(@RequestParam Integer userId) {
        return ResponseEntity.ok(registrationService.getMyTickets(userId));
    }

    @GetMapping("/conferences/{conferenceId}/attendees")
    @Operation(summary = "Get all attendees for a conference (Chair only) — paginated with optional search and status filter")
    public ResponseEntity<PagedResponse<TicketResponse>> getAttendees(
            @PathVariable Integer conferenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(registrationService.getAttendeesPageable(conferenceId, page, size, search, status));
    }

    @PostMapping("/check-in")
    @Operation(summary = "Check in an attendee by QR code or registration number (Staff/Chair)")
    public ResponseEntity<CheckInResponse> checkIn(@RequestParam String code) {
        return ResponseEntity.ok(registrationService.checkIn(code));
    }

    @PostMapping("/conferences/{conferenceId}/retry-payment")
    @Operation(summary = "Retry payment for a pending/failed registration")
    public ResponseEntity<RegistrationResponse> retryPayment(
            @PathVariable Integer conferenceId,
            @RequestParam Integer userId,
            HttpServletRequest httpRequest) {
        String clientIp = VnPayUtil.getIpAddress(httpRequest);
        return ResponseEntity.ok(registrationService.retryPayment(conferenceId, userId, clientIp));
    }

    @PostMapping("/conferences/{conferenceId}/refund")
    @PreAuthorize("@conferenceSecurity.hasRole(#conferenceId, T(com.capstone.confhub.utils.enums.ConferenceRole).CHAIR)")
    @Operation(summary = "Refund a returned ticket (Chair only)")
    public ResponseEntity<Void> refundTicket(
            @PathVariable Integer conferenceId,
            @RequestParam Integer ticketId) {
        registrationService.refundTicket(conferenceId, ticketId);
        return ResponseEntity.ok().build();
    }
}
