package com.capstone.confms.controller;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.PaperAuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/paper-author")
@RequiredArgsConstructor
@Tag(name = "Paper Author Management", description = "Operations related to Paper Author setup and related entities")
public class PaperAuthorController {

    private final PaperAuthorService paperAuthorService;

    @PostMapping
    @Operation(summary = "Create a new Paper Author")
    public ResponseEntity<PaperAuthorResponseDTO> createPaperAuthor(@Valid @RequestBody PaperAuthorDTO dto) {
        return new ResponseEntity<>(paperAuthorService.createPaperAuthor(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Paper Authors")
    public ResponseEntity<PagedResponse<PaperAuthorResponseDTO>> getAllPaperAuthors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(paperAuthorService.getAllPaperAuthors(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Paper Author by ID")
    public ResponseEntity<PaperAuthorResponseDTO> getPaperAuthorById(@PathVariable Integer id) {
        return ResponseEntity.ok(paperAuthorService.getPaperAuthorById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Paper Author details")
    public ResponseEntity<PaperAuthorResponseDTO> updatePaperAuthor(@Valid @PathVariable Integer id,
            @RequestBody PaperAuthorDTO dto) {
        return ResponseEntity.ok(paperAuthorService.updatePaperAuthor(id, dto));
    }

    @GetMapping("/paper/{paperId}")
    @Operation(summary = "Get all authors associated with a specific paper")
    public ResponseEntity<PagedResponse<PaperAuthorResponseDTO>> getAuthorsByPaper(
            @PathVariable Integer paperId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(paperAuthorService.getAuthorsByPaper(paperId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Paper Author")
    public ResponseEntity<Void> deletePaperAuthor(@PathVariable Integer id) {
        paperAuthorService.deletePaperAuthor(id);
        return ResponseEntity.noContent().build();
    }
}