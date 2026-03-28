package com.capstone.confhub.controller;

import com.capstone.confhub.dto.ReviewerInterestDTO;
import com.capstone.confhub.dto.response.ReviewerInterestResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.ReviewerInterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviewer-interest")
@RequiredArgsConstructor
@Tag(name = "Reviewer Interest Management", description = "Operations related to Reviewer Interest setup and related entities")
public class ReviewerInterestController {

    private final ReviewerInterestService reviewerInterestService;

    @PostMapping
    @Operation(summary = "Create a new Reviewer Interest")
    public ResponseEntity<ReviewerInterestResponseDTO> createReviewerInterest(
            @Valid @RequestBody ReviewerInterestDTO dto) {
        return new ResponseEntity<>(reviewerInterestService.createReviewerInterest(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Reviewer Interests")
    public ResponseEntity<PagedResponse<ReviewerInterestResponseDTO>> getAllReviewerInterests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(reviewerInterestService.getAllReviewerInterests(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Reviewer Interest by ID")
    public ResponseEntity<ReviewerInterestResponseDTO> getReviewerInterestById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewerInterestService.getReviewerInterestById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Reviewer Interest details")
    public ResponseEntity<ReviewerInterestResponseDTO> updateReviewerInterest(@Valid @PathVariable Integer id,
            @RequestBody ReviewerInterestDTO dto) {
        return ResponseEntity.ok(reviewerInterestService.updateReviewerInterest(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Reviewer Interest")
    public ResponseEntity<Void> deleteReviewerInterest(@PathVariable Integer id) {
        reviewerInterestService.deleteReviewerInterest(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reviewer/{reviewerId}")
    @Operation(summary = "Get all interests for a specific reviewer")
    public ResponseEntity<java.util.List<ReviewerInterestResponseDTO>> getInterestsByReviewerId(
            @PathVariable Integer reviewerId) {
        return ResponseEntity.ok(reviewerInterestService.getInterestsByReviewerId(reviewerId));
    }
}