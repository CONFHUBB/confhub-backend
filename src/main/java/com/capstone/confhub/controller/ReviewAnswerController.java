package com.capstone.confhub.controller;

import com.capstone.confhub.dto.ReviewAnswerDTO;
import com.capstone.confhub.dto.response.ReviewAnswerResponseDTO;
import com.capstone.confhub.service.ReviewAnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/review-answers")
@RequiredArgsConstructor
@Tag(name = "Review Answers", description = "APIs cho Reviewer trả lời các review questions")
public class ReviewAnswerController {

    private final ReviewAnswerService reviewAnswerService;

    @PostMapping
    @Operation(summary = "Submit hoặc update 1 câu trả lời",
            description = "Nếu đã trả lời question này trong review → update. Nếu chưa → tạo mới.")
    public ResponseEntity<ReviewAnswerResponseDTO> submitOrUpdateAnswer(
            @Valid @RequestBody ReviewAnswerDTO dto) {
        return new ResponseEntity<>(reviewAnswerService.submitOrUpdateAnswer(dto), HttpStatus.OK);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Submit nhiều câu trả lời cùng lúc")
    public ResponseEntity<List<ReviewAnswerResponseDTO>> submitBulkAnswers(
            @Valid @RequestBody List<ReviewAnswerDTO> dtos) {
        return new ResponseEntity<>(reviewAnswerService.submitBulkAnswers(dtos), HttpStatus.OK);
    }

    @GetMapping("/review/{reviewId}")
    @Operation(summary = "Lấy tất cả câu trả lời của 1 review")
    public ResponseEntity<List<ReviewAnswerResponseDTO>> getAnswersByReview(
            @PathVariable Integer reviewId) {
        return ResponseEntity.ok(reviewAnswerService.getAnswersByReview(reviewId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy 1 câu trả lời theo ID")
    public ResponseEntity<ReviewAnswerResponseDTO> getAnswerById(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewAnswerService.getAnswerById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa 1 câu trả lời")
    public ResponseEntity<Void> deleteAnswer(@PathVariable Integer id) {
        reviewAnswerService.deleteAnswer(id);
        return ResponseEntity.noContent().build();
    }
}
