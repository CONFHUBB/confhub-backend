package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceDTO;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ConferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conferences")
@RequiredArgsConstructor
@Tag(name = "Conference Management", description = "Operations related to Conference setup")
public class ConferenceController {

    private final ConferenceService conferenceService;

    @PostMapping
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
    @Operation(summary = "Update conference details")
    public ResponseEntity<ConferenceResponseDTO> updateConference(@Valid @PathVariable Integer id, @RequestBody ConferenceDTO dto) {
        return ResponseEntity.ok(conferenceService.updateConference(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a conference")
    public ResponseEntity<Void> deleteConference(@PathVariable Integer id) {
        conferenceService.deleteConference(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/open-submissions")
    @Operation(summary = "Open submissions for a conference by setting status to ONGOING")
    public ResponseEntity<ConferenceResponseDTO> openSubmissions(@PathVariable Integer id) {
        ConferenceResponseDTO response = conferenceService.openSubmissions(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/approve-conference")
    @Operation(summary = "Approve for a conference by setting status to SCHEDULED")
    public ResponseEntity<ConferenceResponseDTO> approveConference(@PathVariable Integer id) {
        ConferenceResponseDTO response = conferenceService.approveConference(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Complete a conference (ONGOING → COMPLETED)")
    public ResponseEntity<ConferenceResponseDTO> completeConference(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.completeConference(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a conference")
    public ResponseEntity<ConferenceResponseDTO> cancelConference(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.cancelConference(id));
    }
}