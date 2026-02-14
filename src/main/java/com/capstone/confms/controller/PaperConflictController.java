package com.capstone.confms.controller;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.service.PaperConflictService;
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
@RequestMapping("/api/v1/paper-conflict")
@RequiredArgsConstructor
@Tag(name = "Paper Conflict Management", description = "Operations related to Paper Conflict setup and related entities")
public class PaperConflictController {

    private final PaperConflictService paperConflictService;

    @PostMapping("/conflicts")
    @Operation(summary = "Create a new Paper Conflict")
    public ResponseEntity<PaperConflictResponseDTO> createPaperConflict(@Valid @RequestBody PaperConflictDTO dto) {
        return new ResponseEntity<>(paperConflictService.createPaperConflict(dto), HttpStatus.CREATED);
    }

    @GetMapping("/conflicts")
    @Operation(summary = "Get all Paper Conflicts")
    public ResponseEntity<List<PaperConflictResponseDTO>> getAllPaperConflicts() {
        return ResponseEntity.ok(paperConflictService.getAllPaperConflicts());
    }

    @GetMapping("/conflicts/{id}")
    @Operation(summary = "Get Paper Conflict by ID")
    public ResponseEntity<PaperConflictResponseDTO> getPaperConflictById(@PathVariable Integer id) {
        return ResponseEntity.ok(paperConflictService.getPaperConflictById(id));
    }

    @PutMapping("/conflicts/{id}")
    @Operation(summary = "Update Paper Conflict details")
    public ResponseEntity<PaperConflictResponseDTO> updatePaperConflict(@Valid @PathVariable Integer id, @RequestBody PaperConflictDTO dto) {
        return ResponseEntity.ok(paperConflictService.updatePaperConflict(id, dto));
    }

    @DeleteMapping("/conflicts/{id}")
    @Operation(summary = "Delete a Paper Conflict")
    public ResponseEntity<Void> deletePaperConflict(@PathVariable Integer id) {
        paperConflictService.deletePaperConflict(id);
        return ResponseEntity.noContent().build();
    }

}