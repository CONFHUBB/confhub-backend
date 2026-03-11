package com.capstone.confms.controller;

import com.capstone.confms.dto.ReviewQuestionDTO;
import com.capstone.confms.service.ReviewQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tracks/{trackId}/review-questions")
@RequiredArgsConstructor
@Tag(name = "Review Questions", description = "Operations related to configuring Review Form questions for a track")
public class ReviewQuestionController {

    private final ReviewQuestionService reviewQuestionService;

    @GetMapping
    @Operation(summary = "Get all review questions for a track")
    public ResponseEntity<List<ReviewQuestionDTO>> getQuestions(@PathVariable Integer trackId) {
        return ResponseEntity.ok(reviewQuestionService.getQuestionsByTrackId(trackId));
    }

    @PostMapping
    @Operation(summary = "Create a new review question for a track")
    public ResponseEntity<ReviewQuestionDTO> createQuestion(
            @PathVariable Integer trackId,
            @RequestBody ReviewQuestionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewQuestionService.createQuestion(trackId, dto));
    }

    @PutMapping("/{questionId}")
    @Operation(summary = "Update a review question")
    public ResponseEntity<ReviewQuestionDTO> updateQuestion(
            @PathVariable Integer trackId,
            @PathVariable Integer questionId,
            @RequestBody ReviewQuestionDTO dto) {
        return ResponseEntity.ok(reviewQuestionService.updateQuestion(questionId, dto));
    }

    @DeleteMapping("/{questionId}")
    @Operation(summary = "Delete a review question")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Integer trackId,
            @PathVariable Integer questionId) {
        reviewQuestionService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    @Operation(summary = "Reorder review questions for a track")
    public ResponseEntity<List<ReviewQuestionDTO>> reorderQuestions(
            @PathVariable Integer trackId,
            @RequestBody List<Integer> questionIds) {
        return ResponseEntity.ok(reviewQuestionService.reorderQuestions(trackId, questionIds));
    }

    @PostMapping("/copy/{targetTrackId}")
    @Operation(summary = "Copy review questions from this track to another track")
    public ResponseEntity<Void> copyQuestions(
            @PathVariable Integer trackId,
            @PathVariable Integer targetTrackId) {
        reviewQuestionService.copyQuestionsToTrack(trackId, targetTrackId);
        return ResponseEntity.ok().build();
    }
}
