package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceReviewFormDTO;
import com.capstone.confms.dto.response.ConferenceReviewFormResponseDTO;
import com.capstone.confms.service.ConferenceReviewFormService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conference-review-forms")
@RequiredArgsConstructor
@Tag(name = "Conference Review Forms Management", description = "Operations related to Conference Review Forms setup")
public class ConferenceReviewFormController {

    private final ConferenceReviewFormService reviewFormService;

    @PostMapping
    public ResponseEntity<ConferenceReviewFormResponseDTO> createReviewForm(@RequestBody ConferenceReviewFormDTO dto) {
        ConferenceReviewFormResponseDTO created = reviewFormService.createReviewForm(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConferenceReviewFormResponseDTO> updateReviewForm(
            @PathVariable Integer id,
            @RequestBody ConferenceReviewFormDTO dto) {
        return ResponseEntity.ok(reviewFormService.updateReviewForm(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConferenceReviewFormResponseDTO> getReviewFormById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewFormService.getReviewFormById(id));
    }

    @GetMapping
    public ResponseEntity<List<ConferenceReviewFormResponseDTO>> getAllReviewForms() {
        return ResponseEntity.ok(reviewFormService.getAllReviewForms());
    }

    @GetMapping("/track/{trackId}")
    public ResponseEntity<List<ConferenceReviewFormResponseDTO>> getReviewFormsByTrackId(@PathVariable Integer trackId) {
        return ResponseEntity.ok(reviewFormService.getReviewFormsByTrackId(trackId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReviewForm(@PathVariable Integer id) {
        reviewFormService.deleteReviewForm(id);
        return ResponseEntity.noContent().build();
    }
}