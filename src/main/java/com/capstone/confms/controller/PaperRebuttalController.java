package com.capstone.confms.controller;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.service.PaperRebuttalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/paper-rebuttal")
@RequiredArgsConstructor
@Tag(name = "Paper Rebuttal Management", description = "Operations related to Paper Rebuttal setup and related entities")
public class PaperRebuttalController {

    private final PaperRebuttalService paperRebuttalService;

    @PostMapping("/rebuttals")
    @Operation(summary = "Create a new Paper Rebuttal")
    public ResponseEntity<PaperRebuttalResponseDTO> createPaperRebuttal(@Valid @RequestBody PaperRebuttalDTO dto) {
        return new ResponseEntity<>(paperRebuttalService.createPaperRebuttal(dto), HttpStatus.CREATED);
    }

    @GetMapping("/rebuttals")
    @Operation(summary = "Get all Paper Rebuttals")
    public ResponseEntity<List<PaperRebuttalResponseDTO>> getAllPaperRebuttals() {
        return ResponseEntity.ok(paperRebuttalService.getAllPaperRebuttals());
    }

    @GetMapping("/rebuttals/{id}")
    @Operation(summary = "Get Paper Rebuttal by ID")
    public ResponseEntity<PaperRebuttalResponseDTO> getPaperRebuttalById(@PathVariable Integer id) {
        return ResponseEntity.ok(paperRebuttalService.getPaperRebuttalById(id));
    }

    @PutMapping("/rebuttals/{id}")
    @Operation(summary = "Update Paper Rebuttal details")
    public ResponseEntity<PaperRebuttalResponseDTO> updatePaperRebuttal(@Valid @PathVariable Integer id, @RequestBody PaperRebuttalDTO dto) {
        return ResponseEntity.ok(paperRebuttalService.updatePaperRebuttal(id, dto));
    }

    @DeleteMapping("/rebuttals/{id}")
    @Operation(summary = "Delete a Paper Rebuttal")
    public ResponseEntity<Void> deletePaperRebuttal(@PathVariable Integer id) {
        paperRebuttalService.deletePaperRebuttal(id);
        return ResponseEntity.noContent().build();
    }
}