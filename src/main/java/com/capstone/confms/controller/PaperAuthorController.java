package com.capstone.confms.controller;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.service.PaperAuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/paper-author")
@RequiredArgsConstructor
@Tag(name = "Paper Author Management", description = "Operations related to Paper Author setup and related entities")
public class PaperAuthorController {

    private final PaperAuthorService paperAuthorService;

    @PostMapping("/authors")
    @Operation(summary = "Create a new Paper Author")
    public ResponseEntity<PaperAuthorResponseDTO> createPaperAuthor(@Valid @RequestBody PaperAuthorDTO dto) {
        return new ResponseEntity<>(paperAuthorService.createPaperAuthor(dto), HttpStatus.CREATED);
    }

    @GetMapping("/authors")
    @Operation(summary = "Get all Paper Authors")
    public ResponseEntity<List<PaperAuthorResponseDTO>> getAllPaperAuthors() {
        return ResponseEntity.ok(paperAuthorService.getAllPaperAuthors());
    }

    @GetMapping("/authors/{id}")
    @Operation(summary = "Get Paper Author by ID")
    public ResponseEntity<PaperAuthorResponseDTO> getPaperAuthorById(@PathVariable Integer id) {
        return ResponseEntity.ok(paperAuthorService.getPaperAuthorById(id));
    }

    @PutMapping("/authors/{id}")
    @Operation(summary = "Update Paper Author details")
    public ResponseEntity<PaperAuthorResponseDTO> updatePaperAuthor(@Valid @PathVariable Integer id, @RequestBody PaperAuthorDTO dto) {
        return ResponseEntity.ok(paperAuthorService.updatePaperAuthor(id, dto));
    }

    @DeleteMapping("/authors/{id}")
    @Operation(summary = "Delete a Paper Author")
    public ResponseEntity<Void> deletePaperAuthor(@PathVariable Integer id) {
        paperAuthorService.deletePaperAuthor(id);
        return ResponseEntity.noContent().build();
    }
}