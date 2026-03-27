package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceDTO;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.ConferenceStatsDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ConferenceService;
import com.capstone.confms.service.FirebaseStorageService;
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasAnyRole('CHAIR', 'ADMIN')")
    @Operation(summary = "Update conference details")
    public ResponseEntity<ConferenceResponseDTO> updateConference(@Valid @PathVariable Integer id, @RequestBody ConferenceDTO dto) {
        return ResponseEntity.ok(conferenceService.updateConference(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CHAIR', 'ADMIN')")
    @Operation(summary = "Delete a conference")
    public ResponseEntity<Void> deleteConference(@PathVariable Integer id) {
        conferenceService.deleteConference(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/open-submissions")
    @PreAuthorize("hasAnyRole('CHAIR', 'ADMIN')")
    @Operation(summary = "Open submissions for a conference by setting status to ONGOING")
    public ResponseEntity<ConferenceResponseDTO> openSubmissions(@PathVariable Integer id) {
        ConferenceResponseDTO response = conferenceService.openSubmissions(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/approve-conference")
    @PreAuthorize("hasAnyRole('CHAIR', 'ADMIN')")
    @Operation(summary = "Approve for a conference by setting status to SCHEDULED")
    public ResponseEntity<ConferenceResponseDTO> approveConference(@PathVariable Integer id) {
        ConferenceResponseDTO response = conferenceService.approveConference(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('CHAIR', 'ADMIN')")
    @Operation(summary = "Complete a conference (ONGOING → COMPLETED)")
    public ResponseEntity<ConferenceResponseDTO> completeConference(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.completeConference(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CHAIR', 'ADMIN')")
    @Operation(summary = "Cancel a conference")
    public ResponseEntity<ConferenceResponseDTO> cancelConference(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.cancelConference(id));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('CHAIR', 'ADMIN')")
    @Operation(summary = "Get aggregate statistics for a conference")
    public ResponseEntity<ConferenceStatsDTO> getConferenceStats(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.getConferenceStats(id));
    }

    // --- PROGRAM SCHEDULE (JSON Blob) ---
    @GetMapping("/{id}/program")
    @Operation(summary = "Get conference schedule as raw JSON")
    public ResponseEntity<String> getProgramSchedule(@PathVariable("id") Integer id) {
        String json = conferenceService.getProgramSchedule(id);
        if (json == null || json.trim().isEmpty()) {
            json = "{\"rooms\":[],\"sessions\":[],\"published\":false}";
        }
        return ResponseEntity.ok().header("Content-Type", "application/json").body(json);
    }

    @PutMapping("/{id}/program")
    @PreAuthorize("hasAnyRole('CHAIR', 'ADMIN')")
    @Operation(summary = "Save conference schedule JSON blob")
    public ResponseEntity<Void> updateProgramSchedule(@PathVariable("id") Integer id, @RequestBody JsonNode programSchedule) {
        conferenceService.updateProgramSchedule(id, programSchedule.toString());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/{id}/upload-banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CHAIR', 'ADMIN')")
    @Operation(summary = "Upload a banner image for the conference")
    public ResponseEntity<Map<String, String>> uploadBannerImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) throws IOException {
        String downloadUrl = firebaseStorageService.uploadImage(file, id);
        return ResponseEntity.ok(Map.of("url", downloadUrl));
    }
}