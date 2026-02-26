package com.capstone.confms.controller;

import com.capstone.confms.dto.ReviewScoreDTO;
import com.capstone.confms.dto.response.ReviewScoreResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ReviewScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review-controller")
@RequiredArgsConstructor
@Tag(name = "Review Score Management", description = "Operations related to Review Score setup and related entities")
public class ReviewScoreController {

    private final ReviewScoreService reviewScoreService;

    @PostMapping
    @Operation(summary = "Create a new Review Score")
    public ResponseEntity<ReviewScoreResponseDTO> createReviewScore(@Valid @RequestBody ReviewScoreDTO dto) {
        return new ResponseEntity<>(reviewScoreService.createReviewScore(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Review Scores")
    public ResponseEntity<PagedResponse<ReviewScoreResponseDTO>> getAllReviewScores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(reviewScoreService.getAllReviewScores(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Review Score by ID")
    public ResponseEntity<ReviewScoreResponseDTO> getReviewScoreById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewScoreService.getReviewScoreById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Review Score details")
    public ResponseEntity<ReviewScoreResponseDTO> updateReviewScore(@Valid @PathVariable Integer id,
            @RequestBody ReviewScoreDTO dto) {
        return ResponseEntity.ok(reviewScoreService.updateReviewScore(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Review Score")
    public ResponseEntity<Void> deleteReviewScore(@PathVariable Integer id) {
        reviewScoreService.deleteReviewScore(id);
        return ResponseEntity.noContent().build();
    }
}