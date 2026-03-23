package com.capstone.confms.controller;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
@Tag(name = "Review Management", description = "Operations related to Review setup and related entities")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Create a new Review")
    public ResponseEntity<ReviewResponseDTO> createReview(@Valid @RequestBody ReviewDTO dto) {
        return new ResponseEntity<>(reviewService.createReview(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Reviews")
    public ResponseEntity<PagedResponse<ReviewResponseDTO>> getAllReview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(reviewService.getAllReviews(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Review by ID")
    public ResponseEntity<ReviewResponseDTO> getReviewById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Review details")
    public ResponseEntity<ReviewResponseDTO> updateReview(@Valid @PathVariable Integer id, @RequestBody ReviewDTO dto) {
        return ResponseEntity.ok(reviewService.updateReview(id, dto));
    }

    @GetMapping("/reviewer/{reviewerId}/conference/{conferenceId}")
    @Operation(summary = "Get Reviews by reviewer and conference")
    public ResponseEntity<java.util.List<ReviewResponseDTO>> getReviewsByReviewerAndConference(
            @PathVariable Integer reviewerId,
            @PathVariable Integer conferenceId) {
        return ResponseEntity.ok(reviewService.getReviewsByReviewerAndConference(reviewerId, conferenceId));
    }

    @GetMapping("/paper/{paperId}")
    @Operation(summary = "Get all Reviews for a specific paper")
    public ResponseEntity<java.util.List<ReviewResponseDTO>> getReviewsByPaper(@PathVariable Integer paperId) {
        return ResponseEntity.ok(reviewService.getReviewsByPaper(paperId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Review")
    public ResponseEntity<Void> deleteReview(@PathVariable Integer id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}