package com.capstone.confms.controller;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.service.PaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<PaperResponseDTO>> getAllPaper() {
        return ResponseEntity.ok(paperService.getAllPapers());
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
    public ResponseEntity<PaperResponseDTO> updatePaperStatus(@Valid @PathVariable Integer id, @RequestBody PaperUpdateStatusDTO dto) {
        return ResponseEntity.ok(paperService.updatePaperStatus(id, dto));
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "Get all Papers submitted by a specific author")
    public ResponseEntity<List<PaperResponseDTO>> getPapersByAuthor(@PathVariable Integer authorId) {
        return ResponseEntity.ok(paperService.getPapersByAuthor(authorId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Paper")
    public ResponseEntity<Void> deletePaper(@PathVariable Integer id) {
        paperService.deletePaper(id);
        return ResponseEntity.noContent().build();
    }
}