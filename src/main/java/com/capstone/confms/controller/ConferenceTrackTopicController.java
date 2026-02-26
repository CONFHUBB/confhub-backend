package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceTrackTopicDTO;
import com.capstone.confms.dto.response.ConferenceTrackTopicResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ConferenceTrackTopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conference-track-topics")
@RequiredArgsConstructor
@Tag(name = "Conference Track Topic", description = "Operations related to Conference Track Topic setup and related entities")
public class ConferenceTrackTopicController {

    private final ConferenceTrackTopicService conferenceTrackTopicService;

    @PostMapping
    @Operation(summary = "Create a new Conference Track Topic")
    public ResponseEntity<ConferenceTrackTopicResponseDTO> createTopic(
            @Valid @RequestBody ConferenceTrackTopicDTO dto) {
        return new ResponseEntity<>(conferenceTrackTopicService.createTopic(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Conference Track Topics")
    public ResponseEntity<PagedResponse<ConferenceTrackTopicResponseDTO>> getAllTopics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceTrackTopicService.getAllTopics(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Conference Track Topic by ID")
    public ResponseEntity<ConferenceTrackTopicResponseDTO> getTopicById(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceTrackTopicService.getTopicById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Conference Track Topic details")
    public ResponseEntity<ConferenceTrackTopicResponseDTO> updateTopic(@Valid @PathVariable Integer id,
            @RequestBody ConferenceTrackTopicDTO dto) {
        return ResponseEntity.ok(conferenceTrackTopicService.updateTopic(id, dto));
    }

    @GetMapping("/track/{trackId}")
    @Operation(summary = "Get Conference Track Topics by Track ID")
    public ResponseEntity<PagedResponse<ConferenceTrackTopicResponseDTO>> getTopicsByTrackId(
            @PathVariable Integer trackId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceTrackTopicService.getTopicsByTrackId(trackId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Conference Track Topic")
    public ResponseEntity<Void> deleteTopic(@PathVariable Integer id) {
        conferenceTrackTopicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}