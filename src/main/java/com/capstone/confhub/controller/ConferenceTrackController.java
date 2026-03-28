package com.capstone.confhub.controller;

import com.capstone.confhub.dto.ConferenceTrackDTO;
import com.capstone.confhub.dto.response.ConferenceTrackResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.ConferenceTrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conferences-track")
@RequiredArgsConstructor
@Tag(name = "Conference Track", description = "Operations related to Conference Track setup and related entities")
public class ConferenceTrackController {

    private final ConferenceTrackService conferenceTrackService;

    @PostMapping
    @Operation(summary = "Create a new Conference Track")
    public ResponseEntity<ConferenceTrackResponseDTO> createTrack(@Valid @RequestBody ConferenceTrackDTO dto) {
        return new ResponseEntity<>(conferenceTrackService.createTrack(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Conference Tracks")
    public ResponseEntity<PagedResponse<ConferenceTrackResponseDTO>> getAllConferenceTrack(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceTrackService.getAllTracks(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Conference Track by ID")
    public ResponseEntity<ConferenceTrackResponseDTO> getTrackById(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceTrackService.getTrackById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Conference Track details")
    public ResponseEntity<ConferenceTrackResponseDTO> updateTrack(@Valid @PathVariable Integer id,
            @RequestBody ConferenceTrackDTO dto) {
        return ResponseEntity.ok(conferenceTrackService.updateTrack(id, dto));
    }

    @GetMapping("/conferenceId/{id}")
    @Operation(summary = "Get Conference Tracks by Conference ID")
    public ResponseEntity<PagedResponse<ConferenceTrackResponseDTO>> getTracksByConferenceId(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceTrackService.getTracksByConferenceId(id, page, size));
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Conference Track")
    public ResponseEntity<Void> deleteTrack(@PathVariable Integer id) {
        conferenceTrackService.deleteTrack(id);
        return ResponseEntity.noContent().build();
    }
}