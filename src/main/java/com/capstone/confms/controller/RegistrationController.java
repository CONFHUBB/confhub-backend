package com.capstone.confms.controller;

import com.capstone.confms.dto.request.RegistrationRequest;
import com.capstone.confms.dto.response.CheckInResponse;
import com.capstone.confms.dto.response.RegistrationResponse;
import com.capstone.confms.dto.response.TicketResponse;
import com.capstone.confms.service.RegistrationService;
import com.capstone.confms.utils.VnPayUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/conferences/{conferenceId}/attendees")
    @Operation(summary = "Get all attendees for a conference (Chair only)")
    public ResponseEntity<List<TicketResponse>> getAttendees(
            @PathVariable Integer conferenceId) {
        return ResponseEntity.ok(registrationService.getAttendees(conferenceId));
    }

    @PostMapping("/check-in")
    @Operation(summary = "Check in an attendee by QR code or registration number (Staff/Chair)")
    public ResponseEntity<CheckInResponse> checkIn(@RequestParam String code) {
        return ResponseEntity.ok(registrationService.checkIn(code));
    }
}
