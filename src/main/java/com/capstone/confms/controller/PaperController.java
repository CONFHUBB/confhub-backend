package com.capstone.confms.controller;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/paper")
@RequiredArgsConstructor
@Tag(name = "Paper Management", description = "Operations related to Paper setup and related entities")
public class PaperController {

    private final PaperService paperService;

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
    @Operation(summary = "Restore a withdrawn paper (BR-2.15, Chair only)")
    public ResponseEntity<PaperResponseDTO> restorePaper(@PathVariable Integer id) {
        return ResponseEntity.ok(paperService.restorePaper(id));
    }
<<<<<<< Updated upstream
=======

    @GetMapping("/conference/{conferenceId}")
    @Operation(summary = "Get all papers in a conference (for Chair/PC paper management)")
    public ResponseEntity<java.util.List<PaperResponseDTO>> getPapersByConference(
            @PathVariable Integer conferenceId) {
        return ResponseEntity.ok(paperService.getPapersByConference(conferenceId));
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
    public ResponseEntity<java.util.List<PaperResponseDTO>> bulkUpdatePaperStatus(
            @RequestBody java.util.List<PaperUpdateStatusDTO> dtos) {
        return ResponseEntity.ok(paperService.bulkUpdatePaperStatus(dtos));
    }

    @PutMapping("/bulk-discussion")
    @Operation(summary = "Bulk enable/disable discussion for papers (BR-3.30)")
    public ResponseEntity<java.util.List<PaperResponseDTO>> bulkToggleDiscussion(
            @RequestBody java.util.List<Integer> paperIds,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(paperService.bulkToggleDiscussion(paperIds, enabled));
    }
>>>>>>> Stashed changes
}