package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceDTO;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.service.ConferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<ConferenceResponseDTO>> getAllConferences() {
        return ResponseEntity.ok(conferenceService.getAllConferences());
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
}