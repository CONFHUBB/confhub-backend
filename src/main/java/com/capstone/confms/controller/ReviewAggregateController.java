package com.capstone.confms.controller;

import com.capstone.confms.dto.response.ReviewAggregateDTO;
import com.capstone.confms.service.ReviewAggregateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/review-aggregates")
@RequiredArgsConstructor
@Tag(name = "Review Aggregates", description = "Review score aggregation for Chair Console")
public class ReviewAggregateController {

    private final ReviewAggregateService reviewAggregateService;

    @GetMapping("/conference/{conferenceId}")
    @Operation(summary = "Get review aggregates for all papers in a conference")
    public ResponseEntity<List<ReviewAggregateDTO>> getByConference(@PathVariable Integer conferenceId) {
        return ResponseEntity.ok(reviewAggregateService.getAggregatesByConference(conferenceId));
    }

    @GetMapping("/paper/{paperId}")
    @Operation(summary = "Get review aggregate for a specific paper")
    public ResponseEntity<ReviewAggregateDTO> getByPaper(@PathVariable Integer paperId) {
        return ResponseEntity.ok(reviewAggregateService.getAggregateByPaper(paperId));
    }
}
