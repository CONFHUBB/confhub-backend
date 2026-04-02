package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.CheckTrackFitRequest;
import com.capstone.confhub.dto.request.CheckWritingRequest;
import com.capstone.confhub.dto.request.SuggestKeywordsRequest;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.service.impl.AIAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI Assistant features.
 * Provides endpoints for Author, Reviewer, and Chair AI tools.
 */
@RestController
@RequestMapping("/api/v1/ai/assistant")
@RequiredArgsConstructor
public class AIAssistantController {

    private final AIAssistantService assistantService;

    // ═══════════════ AUTHOR FEATURES ═══════════════

    /**
     * POST /api/v1/ai/assistant/suggest-keywords
     * Suggest academic keywords from abstract text.
     */
    @PostMapping("/suggest-keywords")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SuggestKeywordsResponse> suggestKeywords(
            @Valid @RequestBody SuggestKeywordsRequest request) {
        return ResponseEntity.ok(assistantService.suggestKeywords(request));
    }

    /**
     * POST /api/v1/ai/assistant/check-track-fit
     * Check how well a paper matches a conference track.
     */
    @PostMapping("/check-track-fit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TrackFitResponse> checkTrackFit(
            @Valid @RequestBody CheckTrackFitRequest request) {
        return ResponseEntity.ok(assistantService.checkTrackFit(request));
    }

    /**
     * POST /api/v1/ai/assistant/check-writing
     * Check academic tone and grammar of title/abstract.
     */
    @PostMapping("/check-writing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WritingCheckResponse> checkWriting(
            @Valid @RequestBody CheckWritingRequest request) {
        return ResponseEntity.ok(assistantService.checkAcademicWriting(request));
    }

    // ═══════════════ REVIEWER FEATURES ═══════════════

    /**
     * GET /api/v1/ai/assistant/paper/{paperId}/summary
     * Get an AI-generated summary of a paper for a reviewer.
     */
    @GetMapping("/paper/{paperId}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaperSummaryResponse> summarizePaper(
            @PathVariable Integer paperId) {
        return ResponseEntity.ok(assistantService.summarizePaper(paperId));
    }

    /**
     * GET /api/v1/ai/assistant/paper/{paperId}/strengths-weaknesses
     * Get AI-analyzed strengths and weaknesses of a paper.
     */
    @GetMapping("/paper/{paperId}/strengths-weaknesses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StrengthWeaknessResponse> analyzeStrengthsWeaknesses(
            @PathVariable Integer paperId) {
        return ResponseEntity.ok(assistantService.analyzeStrengthsWeaknesses(paperId));
    }

    // ═══════════════ CHAIR FEATURES ═══════════════

    /**
     * GET /api/v1/ai/assistant/paper/{paperId}/review-consensus
     * Analyze reviewer consensus for a paper.
     */
    @GetMapping("/paper/{paperId}/review-consensus")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConsensusResponse> analyzeReviewConsensus(
            @PathVariable Integer paperId) {
        return ResponseEntity.ok(assistantService.analyzeReviewConsensus(paperId));
    }

    // ═══════════════════════════════════════════════

}
