package com.capstone.confms.controller;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.service.ReviewTypeService;
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
@Tag(name = "Review Type Management", description = "Operations related to Review Type setup and related entities")
public class ReviewTypeController {

    private final ReviewTypeService reviewTypeService;

    @PostMapping("/types")
    @Operation(summary = "Create a new Review Type")
    public ResponseEntity<ReviewTypeResponseDTO> createReviewType(@Valid @RequestBody ReviewTypeDTO dto) {
        return new ResponseEntity<>(reviewTypeService.createReviewType(dto), HttpStatus.CREATED);
    }

    @GetMapping("/types")
    @Operation(summary = "Get all Review Types")
    public ResponseEntity<List<ReviewTypeResponseDTO>> getAllReviewTypes() {
        return ResponseEntity.ok(reviewTypeService.getAllReviewTypes());
    }

    @GetMapping("/types/{id}")
    @Operation(summary = "Get Review Type by ID")
    public ResponseEntity<ReviewTypeResponseDTO> getReviewTypeById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewTypeService.getReviewTypeById(id));
    }

    @PutMapping("/types/{id}")
    @Operation(summary = "Update Review Type details")
    public ResponseEntity<ReviewTypeResponseDTO> updateReviewType(@Valid @PathVariable Integer id, @RequestBody ReviewTypeDTO dto) {
        return ResponseEntity.ok(reviewTypeService.updateReviewType(id, dto));
    }

    @DeleteMapping("/types/{id}")
    @Operation(summary = "Delete a Review Type")
    public ResponseEntity<Void> deleteReviewType(@PathVariable Integer id) {
        reviewTypeService.deleteReviewType(id);
        return ResponseEntity.noContent().build();
    }
}