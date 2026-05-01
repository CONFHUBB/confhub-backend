package com.capstone.confhub.controller;

import com.capstone.confhub.dto.ConferenceDTO;
import com.capstone.confhub.dto.response.ConferenceResponseDTO;
import com.capstone.confhub.dto.response.ConferenceStatsDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.ConferenceService;
import com.capstone.confhub.service.FirebaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/conferences")
@RequiredArgsConstructor
@Tag(name = "Conference Management", description = "Operations related to Conference setup")
public class ConferenceController {

    private final ConferenceService conferenceService;
    private final FirebaseStorageService firebaseStorageService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new conference")
    public ResponseEntity<ConferenceResponseDTO> createConference(@Valid @RequestBody ConferenceDTO dto) {
        return new ResponseEntity<>(conferenceService.createConference(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all conferences")
    public ResponseEntity<PagedResponse<ConferenceResponseDTO>> getAllConferences(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceService.getAllConferences(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conference by ID")
    public ResponseEntity<ConferenceResponseDTO> getByIdConference(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.getByIdConference(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update conference details")
    public ResponseEntity<ConferenceResponseDTO> updateConference(@Valid @PathVariable Integer id, @RequestBody ConferenceDTO dto) {
        return ResponseEntity.ok(conferenceService.updateConference(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a conference")
    public ResponseEntity<Void> deleteConference(@PathVariable Integer id) {
        conferenceService.deleteConference(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/open-submissions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Open submissions for a conference by setting status to ONGOING")
    public ResponseEntity<ConferenceResponseDTO> openSubmissions(@PathVariable Integer id) {
        ConferenceResponseDTO response = conferenceService.openSubmissions(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/approve-conference")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Approve for a conference by setting status to APPROVED")
    public ResponseEntity<ConferenceResponseDTO> approveConference(@PathVariable Integer id) {
        ConferenceResponseDTO response = conferenceService.approveConference(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Reject a conference with a reason")
    public ResponseEntity<ConferenceResponseDTO> rejectConference(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason specified");
        return ResponseEntity.ok(conferenceService.rejectConference(id, reason));
    }

    @PutMapping("/{id}/submit-for-approval")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit a conference for admin approval (SETUP/REJECTED → PENDING_APPROVAL)")
    public ResponseEntity<ConferenceResponseDTO> submitForApproval(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.submitForApproval(id));
    }

    @PutMapping("/{id}/select-plan")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Select a subscription plan for an approved conference")
    public ResponseEntity<java.util.Map<String, Object>> selectPlan(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, String> body,
            jakarta.servlet.http.HttpServletRequest request) {
        String plan = body.getOrDefault("plan", "");
        String ipAddr = request.getRemoteAddr();
        return ResponseEntity.ok(conferenceService.selectPlan(id, plan, ipAddr));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Complete a conference (ONGOING → COMPLETED)")
    public ResponseEntity<ConferenceResponseDTO> completeConference(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.completeConference(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a conference")
    public ResponseEntity<ConferenceResponseDTO> cancelConference(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.cancelConference(id));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get aggregate statistics for a conference")
    public ResponseEntity<ConferenceStatsDTO> getConferenceStats(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.getConferenceStats(id));
    }

    @GetMapping("/{id}/export/attendees")
    @PreAuthorize("isAuthenticated()") 
    @Operation(summary = "Export conference attendees as CSV")
    public ResponseEntity<byte[]> exportAttendees(@PathVariable("id") Integer id) {
        byte[] csvData = conferenceService.exportAttendeesCsv(id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendees.csv")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    // --- PROGRAM SCHEDULE (JSON Blob) ---
    @GetMapping(value = "/{id}/program", produces = "application/json")
    @Operation(summary = "Get conference schedule as raw JSON")
    public ResponseEntity<String> getProgramSchedule(@PathVariable("id") Integer id) {
        String json = conferenceService.getProgramSchedule(id);
        if (json == null || json.trim().isEmpty()) {
            json = "{\"rooms\":[],\"sessions\":[],\"published\":false}";
        }
        return ResponseEntity.ok(json);
    }

    @PutMapping("/{id}/program")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Save conference schedule JSON blob")
    public ResponseEntity<Void> updateProgramSchedule(@PathVariable("id") Integer id, @RequestBody java.util.Map<String, Object> programSchedule) {
        String jsonStr;
        try {
            jsonStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(programSchedule);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON", e);
        }
        conferenceService.updateProgramSchedule(id, jsonStr);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/upload-banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload a banner image for the conference")
    public ResponseEntity<Map<String, String>> uploadBannerImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String downloadUrl = firebaseStorageService.uploadImage(file, id);
        return ResponseEntity.ok(Map.of("url", downloadUrl));
    }

    @PostMapping(value = "/{id}/upload-paper-template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload a paper template file for the conference")
    public ResponseEntity<Map<String, String>> uploadPaperTemplate(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String downloadUrl = firebaseStorageService.uploadPaperTemplateFile(file, id);
        conferenceService.updatePaperTemplateUrl(id, downloadUrl);
        return ResponseEntity.ok(Map.of("url", downloadUrl));
    }
}