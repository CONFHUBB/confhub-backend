package com.capstone.confhub.controller;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.PaperConflictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/paper-conflict")
@RequiredArgsConstructor
@Tag(name = "Paper Conflict Management", description = "Operations related to Paper Conflict setup and related entities")
public class PaperConflictController {

    private final PaperConflictService paperConflictService;

    @PostMapping
    @Operation(summary = "Create a new Paper Conflict")
    public ResponseEntity<PaperConflictResponseDTO> createPaperConflict(@Valid @RequestBody PaperConflictDTO dto) {
        return new ResponseEntity<>(paperConflictService.createPaperConflict(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Paper Conflicts")
    public ResponseEntity<PagedResponse<PaperConflictResponseDTO>> getAllPaperConflicts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(paperConflictService.getAllPaperConflicts(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Paper Conflict by ID")
    public ResponseEntity<PaperConflictResponseDTO> getPaperConflictById(@PathVariable Integer id) {
        return ResponseEntity.ok(paperConflictService.getPaperConflictById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Paper Conflict details")
    public ResponseEntity<PaperConflictResponseDTO> updatePaperConflict(@Valid @PathVariable Integer id,
            @RequestBody PaperConflictDTO dto) {
        return ResponseEntity.ok(paperConflictService.updatePaperConflict(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Paper Conflict")
    public ResponseEntity<Void> deletePaperConflict(@PathVariable Integer id) {
        paperConflictService.deletePaperConflict(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/paper/{paperId}")
    @Operation(summary = "Get all conflicts for a specific paper")
    public ResponseEntity<java.util.List<PaperConflictResponseDTO>> getConflictsByPaper(@PathVariable Integer paperId) {
        return ResponseEntity.ok(paperConflictService.getConflictsByPaperId(paperId));
    }

    @GetMapping("/conference/{conferenceId}")
    @Operation(summary = "Get all conflicts in a conference")
    public ResponseEntity<java.util.List<PaperConflictResponseDTO>> getConflictsByConference(@PathVariable Integer conferenceId) {
        return ResponseEntity.ok(paperConflictService.getConflictsByConferenceId(conferenceId));
    }
}