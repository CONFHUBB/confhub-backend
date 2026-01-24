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
    public ResponseEntity<ConferenceResponseDTO> create(@Valid @RequestBody ConferenceDTO dto) {
        return new ResponseEntity<>(conferenceService.create(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all conferences")
    public ResponseEntity<List<ConferenceResponseDTO>> getAll() {
        return ResponseEntity.ok(conferenceService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get conference by ID")
    public ResponseEntity<ConferenceResponseDTO> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update conference details")
    public ResponseEntity<ConferenceResponseDTO> update(@Valid @PathVariable Integer id, @RequestBody ConferenceDTO dto) {
        return ResponseEntity.ok(conferenceService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a conference")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        conferenceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}