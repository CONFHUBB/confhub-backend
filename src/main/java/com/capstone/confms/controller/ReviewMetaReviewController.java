package com.capstone.confms.controller;

import com.capstone.confms.dto.ReviewMetaReviewDTO;
import com.capstone.confms.dto.response.ReviewMetaReviewResponseDTO;
import com.capstone.confms.service.ReviewMetaReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
@Tag(name = "Review Meta Review Management", description = "Operations related to Review Meta Review setup and related entities")
public class ReviewMetaReviewController {

    private final ReviewMetaReviewService reviewMetaReviewService;

    @PostMapping("/meta-reviews")
    @Operation(summary = "Create a new Review Meta Review")
    public ResponseEntity<ReviewMetaReviewResponseDTO> createReviewMetaReview(@Valid @RequestBody ReviewMetaReviewDTO dto) {
        return new ResponseEntity<>(reviewMetaReviewService.createReviewMetaReview(dto), HttpStatus.CREATED);
    }

    @GetMapping("/meta-reviews")
    @Operation(summary = "Get all Review Meta Reviews")
    public ResponseEntity<List<ReviewMetaReviewResponseDTO>> getAllReviewMetaReviews() {
        return ResponseEntity.ok(reviewMetaReviewService.getAllReviewMetaReviews());
    }

    @GetMapping("/meta-reviews/{id}")
    @Operation(summary = "Get Review Meta Review by ID")
    public ResponseEntity<ReviewMetaReviewResponseDTO> getReviewMetaReviewById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewMetaReviewService.getReviewMetaReviewById(id));
    }

    @PutMapping("/meta-reviews/{id}")
    @Operation(summary = "Update Review Meta Review details")
    public ResponseEntity<ReviewMetaReviewResponseDTO> updateReviewMetaReview(@Valid @PathVariable Integer id, @RequestBody ReviewMetaReviewDTO dto) {
        return ResponseEntity.ok(reviewMetaReviewService.updateReviewMetaReview(id, dto));
    }

    @DeleteMapping("/meta-reviews/{id}")
    @Operation(summary = "Delete a Review Meta Review")
    public ResponseEntity<Void> deleteReviewMetaReview(@PathVariable Integer id) {
        reviewMetaReviewService.deleteReviewMetaReview(id);
        return ResponseEntity.noContent().build();
    }
}