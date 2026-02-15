package com.capstone.confms.controller;

import com.capstone.confms.dto.ReviewTypeDTO;
import com.capstone.confms.dto.response.ReviewTypeResponseDTO;
import com.capstone.confms.service.ReviewTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review-types")
@RequiredArgsConstructor
@Tag(name = "Review Type Management", description = "Operations related to Review Type configuration")
public class ReviewTypeController {
    private final ReviewTypeService reviewTypeService;

    @PostMapping
    @Operation(summary = "Create a new Review Type")
    public ResponseEntity<ReviewTypeResponseDTO> createReviewType(@Valid @RequestBody ReviewTypeDTO dto) {
        return new ResponseEntity<>(reviewTypeService.createReviewType(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Review Types")
    public ResponseEntity<List<ReviewTypeResponseDTO>> getAllReviewTypes() {
        return ResponseEntity.ok(reviewTypeService.getAllReviewTypes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Review Type by ID")
    public ResponseEntity<ReviewTypeResponseDTO> getReviewTypeById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewTypeService.getReviewTypeById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Review Type details")
    public ResponseEntity<ReviewTypeResponseDTO> updateReviewType(@Valid @PathVariable Integer id, @RequestBody ReviewTypeDTO dto) {
        return ResponseEntity.ok(reviewTypeService.updateReviewType(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Review Type")
    public ResponseEntity<Void> deleteReviewType(@PathVariable Integer id) {
        reviewTypeService.deleteReviewType(id);
        return ResponseEntity.noContent().build();
    }
}
