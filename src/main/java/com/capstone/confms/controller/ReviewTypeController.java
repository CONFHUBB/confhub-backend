package com.capstone.confms.controller;

import com.capstone.confms.dto.request.CreateReviewTypeRequest;
import com.capstone.confms.dto.response.ReviewTypeResponseDTO;
import com.capstone.confms.service.ReviewTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    @Operation(summary = "Create a new review type for a conference")
    public ResponseEntity<ReviewTypeResponseDTO> configureReviewType(@Valid @RequestBody CreateReviewTypeRequest request) {
        ReviewTypeResponseDTO response = reviewTypeService.configureReviewType(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
