package com.capstone.confhub.controller;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.ReviewService;
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

    @GetMapping("/top-reviewers")
    @Operation(summary = "Get top reviewers by completed reviews")
    public ResponseEntity<java.util.List<TopReviewerResponseDTO>> getTopReviewers(
            @RequestParam(defaultValue = "5") int limit) {
        if (limit <= 0 || limit > 20) {
            throw new BadRequestException("Limit must be between 1 and 20");
        }
        return ResponseEntity.ok(reviewService.getTopReviewers(limit));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get all versions of a specific review")
    public ResponseEntity<java.util.List<ReviewVersionResponseDTO>> getReviewVersions(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewService.getReviewVersions(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Review")
    public ResponseEntity<Void> deleteReview(@PathVariable Integer id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}