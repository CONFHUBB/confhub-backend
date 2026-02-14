package com.capstone.confms.controller;

import com.capstone.confms.dto.ReviewerInterestDTO;
import com.capstone.confms.dto.response.ReviewerInterestResponseDTO;
import com.capstone.confms.service.ReviewerInterestService;
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
@Tag(name = "Reviewer Interest Management", description = "Operations related to Reviewer Interest setup and related entities")
public class ReviewerInterestController {

    private final ReviewerInterestService reviewerInterestService;

    @PostMapping("/interests")
    @Operation(summary = "Create a new Reviewer Interest")
    public ResponseEntity<ReviewerInterestResponseDTO> createReviewerInterest(@Valid @RequestBody ReviewerInterestDTO dto) {
        return new ResponseEntity<>(reviewerInterestService.createReviewerInterest(dto), HttpStatus.CREATED);
    }

    @GetMapping("/interests")
    @Operation(summary = "Get all Reviewer Interests")
    public ResponseEntity<List<ReviewerInterestResponseDTO>> getAllReviewerInterests() {
        return ResponseEntity.ok(reviewerInterestService.getAllReviewerInterests());
    }

    @GetMapping("/interests/{id}")
    @Operation(summary = "Get Reviewer Interest by ID")
    public ResponseEntity<ReviewerInterestResponseDTO> getReviewerInterestById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewerInterestService.getReviewerInterestById(id));
    }

    @PutMapping("/interests/{id}")
    @Operation(summary = "Update Reviewer Interest details")
    public ResponseEntity<ReviewerInterestResponseDTO> updateReviewerInterest(@Valid @PathVariable Integer id, @RequestBody ReviewerInterestDTO dto) {
        return ResponseEntity.ok(reviewerInterestService.updateReviewerInterest(id, dto));
    }

    @DeleteMapping("/interests/{id}")
    @Operation(summary = "Delete a Reviewer Interest")
    public ResponseEntity<Void> deleteReviewerInterest(@PathVariable Integer id) {
        reviewerInterestService.deleteReviewerInterest(id);
        return ResponseEntity.noContent().build();
    }
}