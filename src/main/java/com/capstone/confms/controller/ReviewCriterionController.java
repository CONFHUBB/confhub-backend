package com.capstone.confms.controller;

import com.capstone.confms.dto.ReviewCriterionDTO;
import com.capstone.confms.dto.response.ReviewCriterionResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ReviewCriterionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review-criterion")
@RequiredArgsConstructor
@Tag(name = "Review Criterion Management", description = "Operations related to Review Criterion setup and related entities")
public class ReviewCriterionController {

    private final ReviewCriterionService reviewCriterionService;

    @PostMapping
    @Operation(summary = "Create a new Review Criterion")
    public ResponseEntity<ReviewCriterionResponseDTO> createReviewCriterion(
            @Valid @RequestBody ReviewCriterionDTO dto) {
        return new ResponseEntity<>(reviewCriterionService.createReviewCriterion(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Review Criteria")
    public ResponseEntity<PagedResponse<ReviewCriterionResponseDTO>> getAllReviewCriteria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(reviewCriterionService.getAllReviewCriteria(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Review Criterion by ID")
    public ResponseEntity<ReviewCriterionResponseDTO> getReviewCriterionById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewCriterionService.getReviewCriterionById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Review Criterion details")
    public ResponseEntity<ReviewCriterionResponseDTO> updateReviewCriterion(@Valid @PathVariable Integer id,
            @RequestBody ReviewCriterionDTO dto) {
        return ResponseEntity.ok(reviewCriterionService.updateReviewCriterion(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Review Criterion")
    public ResponseEntity<Void> deleteReviewCriterion(@PathVariable Integer id) {
        reviewCriterionService.deleteReviewCriterion(id);
        return ResponseEntity.noContent().build();
    }
}