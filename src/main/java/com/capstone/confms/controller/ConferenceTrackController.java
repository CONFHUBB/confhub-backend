package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceTrackDTO;
import com.capstone.confms.dto.response.ConferenceTrackResponseDTO;
import com.capstone.confms.service.ConferenceService;
import com.capstone.confms.service.ConferenceTrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conferences-track")
@RequiredArgsConstructor
@Tag(name = "Conference Track Management", description = "Operations related to Conference Track setup")
public class ConferenceTrackController {

    private final ConferenceTrackService conferenceTrackService;

    @PostMapping
    @Operation(summary = "Create a new conference")
    public ResponseEntity<ConferenceTrackResponseDTO> createConferenceTrack(@Valid @RequestBody ConferenceTrackDTO dto) {
        return new ResponseEntity<>(conferenceTrackService.createTrack(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all conferences")
    public ResponseEntity<List<ConferenceTrackResponseDTO>> getAllConferenceTrack() {
        return ResponseEntity.ok(conferenceTrackService.getAllTracks());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conference by ID")
    public ResponseEntity<ConferenceTrackResponseDTO> getByIdConferenceTrack(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceTrackService.getTrackById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update conference details")
    public ResponseEntity<ConferenceTrackResponseDTO> updateConferenceTrack(@Valid @PathVariable Integer id, @RequestBody ConferenceTrackDTO dto) {
        return ResponseEntity.ok(conferenceTrackService.updateTrack(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a conference")
    public ResponseEntity<Void> deleteConferenceTrack(@PathVariable Integer id) {
        conferenceTrackService.deleteTrack(id);
        return ResponseEntity.noContent().build();
    }
}