package com.capstone.confms.controller;

import com.capstone.confms.dto.ReviewMetaReviewDTO;
import com.capstone.confms.dto.response.ReviewMetaReviewResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
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
@RequestMapping("/api/v1/review-meta-review")
@RequiredArgsConstructor
@Tag(name = "Review Meta Review Management", description = "Operations related to Review Meta Review setup and related entities")
public class ReviewMetaReviewController {

    private final ReviewMetaReviewService reviewMetaReviewService;

    @PostMapping
    @Operation(summary = "Create a new Review Meta Review")
    public ResponseEntity<ReviewMetaReviewResponseDTO> createReviewMetaReview(
            @Valid @RequestBody ReviewMetaReviewDTO dto) {
        return new ResponseEntity<>(reviewMetaReviewService.createReviewMetaReview(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Review Meta Reviews")
    public ResponseEntity<PagedResponse<ReviewMetaReviewResponseDTO>> getAllReviewMetaReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(reviewMetaReviewService.getAllReviewMetaReviews(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Review Meta Review by ID")
    public ResponseEntity<ReviewMetaReviewResponseDTO> getReviewMetaReviewById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewMetaReviewService.getReviewMetaReviewById(id));
    }

    @GetMapping("/by-conference/{conferenceId}")
    @Operation(summary = "Get all Review Meta Reviews for a conference")
    public ResponseEntity<List<ReviewMetaReviewResponseDTO>> getMetaReviewsByConference(
            @PathVariable Integer conferenceId) {
        return ResponseEntity.ok(reviewMetaReviewService.getMetaReviewsByConference(conferenceId));
    }

    @GetMapping("/by-paper/{paperId}")
    @Operation(summary = "Get Review Meta Review for a specific paper")
    public ResponseEntity<ReviewMetaReviewResponseDTO> getMetaReviewByPaper(@PathVariable Integer paperId) {
        ReviewMetaReviewResponseDTO result = reviewMetaReviewService.getMetaReviewByPaper(paperId);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Review Meta Review details")
    public ResponseEntity<ReviewMetaReviewResponseDTO> updateReviewMetaReview(@Valid @PathVariable Integer id,
            @RequestBody ReviewMetaReviewDTO dto) {
        return ResponseEntity.ok(reviewMetaReviewService.updateReviewMetaReview(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Review Meta Review")
    public ResponseEntity<Void> deleteReviewMetaReview(@PathVariable Integer id) {
        reviewMetaReviewService.deleteReviewMetaReview(id);
        return ResponseEntity.noContent().build();
    }
}