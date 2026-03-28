package com.capstone.confhub.controller;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.service.EmailService;
import com.capstone.confhub.service.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/paper")
@RequiredArgsConstructor
@Tag(name = "Paper Management", description = "Operations related to Paper setup and related entities")
public class PaperController {

    private final PaperService paperService;
    private final EmailService emailService;
    private final PaperRepository paperRepository;
    private final ConferenceRepository conferenceRepository;

    @PostMapping
    @Operation(summary = "Create a new Paper")
    public ResponseEntity<PaperResponseDTO> createPaper(@Valid @RequestBody PaperDTO dto) {
        return new ResponseEntity<>(paperService.createPaper(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Papers")
    public ResponseEntity<PagedResponse<PaperResponseDTO>> getAllPaper(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(paperService.getAllPapers(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Paper by ID")
    public ResponseEntity<PaperResponseDTO> getPaperById(@PathVariable Integer id) {
        return ResponseEntity.ok(paperService.getPaperById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Paper details")
    public ResponseEntity<PaperResponseDTO> updatePaper(@Valid @PathVariable Integer id, @RequestBody PaperDTO dto) {
        return ResponseEntity.ok(paperService.updatePaper(id, dto));
    }

    @PutMapping("/status/{id}")
    @Operation(summary = "Update Paper Status details")
    public ResponseEntity<PaperResponseDTO> updatePaperStatus(@Valid @PathVariable Integer id,
            @RequestBody PaperUpdateStatusDTO dto) {
        return ResponseEntity.ok(paperService.updatePaperStatus(id, dto));
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "Get all Papers submitted by a specific author")
    public ResponseEntity<PagedResponse<PaperResponseDTO>> getPapersByAuthor(
            @PathVariable Integer authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(paperService.getPapersByAuthor(authorId, page, size));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a Paper")
    public ResponseEntity<Void> deletePaper(@PathVariable Integer id) {
        paperService.deletePaper(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw a submitted paper (BR-2.15)")
    public ResponseEntity<PaperResponseDTO> withdrawPaper(@PathVariable Integer id) {
        return ResponseEntity.ok(paperService.withdrawPaper(id));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Restore a withdrawn paper (BR-2.15, Chair only)")
    public ResponseEntity<PaperResponseDTO> restorePaper(@PathVariable Integer id) {
        return ResponseEntity.ok(paperService.restorePaper(id));
    }

    /**
     * Task 5: Track Chair Filtered View
     * Optional ?trackIds=1,2,3 — if provided, only papers in those tracks are returned.
     * Track Chairs supply their own trackIds; full Chairs omit the param to see all.
     */
    @GetMapping("/conference/{conferenceId}")
    @Operation(summary = "Get all papers in a conference (optional ?trackIds= filter for Track Chairs)")
    public ResponseEntity<List<PaperResponseDTO>> getPapersByConference(
            @PathVariable Integer conferenceId,
            @RequestParam(required = false) List<Integer> trackIds) {
        if (trackIds != null && !trackIds.isEmpty()) {
            List<PaperResponseDTO> filtered = paperRepository
                    .findByTrack_Conference_IdAndTrack_IdIn(conferenceId, trackIds)
                    .stream()
                    .map(p -> paperService.getPaperById(p.getId()))
                    .toList();
            return ResponseEntity.ok(filtered);
        }
        return ResponseEntity.ok(paperService.getPapersByConference(conferenceId));
    }

    /**
     * Task 3: Batch Decision Email Notification
     * POST /api/v1/paper/conference/{conferenceId}/batch-notify
     * Returns immediately; emails are sent asynchronously in background thread.
     */
    @PostMapping("/conference/{conferenceId}/batch-notify")
    @Operation(summary = "Async batch-send decision emails to all paper authors (Task 3)")
    public ResponseEntity<String> batchNotifyDecisions(@PathVariable Integer conferenceId) {
        var conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BadRequestException("Conference not found: " + conferenceId));
        List<Paper> papers = paperRepository.findByTrack_Conference_Id(conferenceId);
        emailService.sendBatchDecisionNotifications(papers, conference.getName());
        return ResponseEntity.ok("Batch notification queued for " + papers.size() + " papers.");
    }

    @PutMapping("/{id}/review-read-only")
    @Operation(summary = "Toggle review read-only for a paper (BR-3.28)")
    public ResponseEntity<PaperResponseDTO> toggleReviewReadOnly(
            @PathVariable Integer id,
            @RequestParam boolean readOnly) {
        return ResponseEntity.ok(paperService.toggleReviewReadOnly(id, readOnly));
    }

    @PutMapping("/{id}/discussion")
    @Operation(summary = "Enable/disable discussion for a paper (BR-3.30)")
    public ResponseEntity<PaperResponseDTO> toggleDiscussion(
            @PathVariable Integer id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(paperService.toggleDiscussion(id, enabled));
    }

    @PutMapping("/bulk-status")
    @Operation(summary = "Bulk update paper status (BR-3.43)")
    public ResponseEntity<List<PaperResponseDTO>> bulkUpdatePaperStatus(
            @RequestBody List<PaperUpdateStatusDTO> dtos) {
        return ResponseEntity.ok(paperService.bulkUpdatePaperStatus(dtos));
    }

    @PutMapping("/bulk-discussion")
    @Operation(summary = "Bulk enable/disable discussion for papers (BR-3.30)")
    public ResponseEntity<List<PaperResponseDTO>> bulkToggleDiscussion(
            @RequestBody List<Integer> paperIds,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(paperService.bulkToggleDiscussion(paperIds, enabled));
    }

    /**
     * Public endpoint: Get all PUBLISHED papers with pagination + optional title search.
     * No auth required — used by the "Published Papers" public page.
     */
    @GetMapping("/published")
    @Operation(summary = "Get all published papers (public, paginated)")
    public ResponseEntity<PagedResponse<PaperResponseDTO>> getPublishedPapers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(paperService.getPublishedPapers(page, size, search));
    }
}