package com.capstone.confms.controller;

import com.capstone.confms.dto.ReviewCommentDTO;
import com.capstone.confms.dto.response.ReviewCommentResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ReviewCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review-comment")
@RequiredArgsConstructor
@Tag(name = "Review Comment Management", description = "Operations related to Review Comment setup and related entities")
public class ReviewCommentController {

    private final ReviewCommentService reviewCommentService;

    @PostMapping
    @Operation(summary = "Create a new Review Comment")
    public ResponseEntity<ReviewCommentResponseDTO> createReviewComment(@Valid @RequestBody ReviewCommentDTO dto) {
        return new ResponseEntity<>(reviewCommentService.createReviewComment(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Review Comments")
    public ResponseEntity<PagedResponse<ReviewCommentResponseDTO>> getAllReviewComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(reviewCommentService.getAllReviewComments(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Review Comment by ID")
    public ResponseEntity<ReviewCommentResponseDTO> getReviewCommentById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewCommentService.getReviewCommentById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Review Comment details")
    public ResponseEntity<ReviewCommentResponseDTO> updateReviewComment(@Valid @PathVariable Integer id,
            @RequestBody ReviewCommentDTO dto) {
        return ResponseEntity.ok(reviewCommentService.updateReviewComment(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Review Comment")
    public ResponseEntity<Void> deleteReviewComment(@PathVariable Integer id) {
        reviewCommentService.deleteReviewComment(id);
        return ResponseEntity.noContent().build();
    }
}