package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceReviewFormDTO;
import com.capstone.confms.dto.response.ConferenceReviewFormResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ConferenceReviewFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conference-review-forms")
@RequiredArgsConstructor
@Tag(name = "Conference Review Form", description = "Operations related to Conference Review Form setup and related entities")
public class ConferenceReviewFormController {

    private final ConferenceReviewFormService conferenceReviewFormService;

    @PostMapping
    @Operation(summary = "Create a new Conference Review Form")
    public ResponseEntity<ConferenceReviewFormResponseDTO> createReviewForm(
            @Valid @RequestBody ConferenceReviewFormDTO dto) {
        return new ResponseEntity<>(conferenceReviewFormService.createReviewForm(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Conference Review Forms")
    public ResponseEntity<PagedResponse<ConferenceReviewFormResponseDTO>> getAllReviewForms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceReviewFormService.getAllReviewForms(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Conference Review Form by ID")
    public ResponseEntity<ConferenceReviewFormResponseDTO> getReviewFormById(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceReviewFormService.getReviewFormById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Conference Review Form details")
    public ResponseEntity<ConferenceReviewFormResponseDTO> updateReviewForm(@Valid @PathVariable Integer id,
            @RequestBody ConferenceReviewFormDTO dto) {
        return ResponseEntity.ok(conferenceReviewFormService.updateReviewForm(id, dto));
    }

    @GetMapping("/track/{trackId}")
    @Operation(summary = "Get Conference Review Forms by Track ID")
    public ResponseEntity<PagedResponse<ConferenceReviewFormResponseDTO>> getReviewFormsByTrackId(
            @PathVariable Integer trackId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceReviewFormService.getReviewFormsByTrackId(trackId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Conference Review Form")
    public ResponseEntity<Void> deleteReviewForm(@PathVariable Integer id) {
        conferenceReviewFormService.deleteReviewForm(id);
        return ResponseEntity.noContent().build();
    }
}