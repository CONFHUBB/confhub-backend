package com.capstone.confms.controller;

import com.capstone.confms.dto.PaperFileDTO;
import com.capstone.confms.dto.response.PaperFileResponseDTO;
import com.capstone.confms.service.PaperFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/paper-file")
@RequiredArgsConstructor
@Tag(name = "Paper File Management", description = "Operations related to Paper File setup and related entities")
public class PaperFileController {

    private final PaperFileService paperFileService;

    @PostMapping
    @Operation(summary = "Create a new Paper File")
    public ResponseEntity<PaperFileResponseDTO> createPaperFile(@Valid @RequestBody PaperFileDTO dto) {
        return new ResponseEntity<>(paperFileService.createPaperFile(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Paper Files")
    public ResponseEntity<List<PaperFileResponseDTO>> getAllPaperFiles() {
        return ResponseEntity.ok(paperFileService.getAllPaperFiles());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Paper File by ID")
    public ResponseEntity<PaperFileResponseDTO> getPaperFileById(@PathVariable Integer id) {
        return ResponseEntity.ok(paperFileService.getPaperFileById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Paper File details")
    public ResponseEntity<PaperFileResponseDTO> updatePaperFile(@Valid @PathVariable Integer id, @RequestBody PaperFileDTO dto) {
        return ResponseEntity.ok(paperFileService.updatePaperFile(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Paper File")
    public ResponseEntity<Void> deletePaperFile(@PathVariable Integer id) {
        paperFileService.deletePaperFile(id);
        return ResponseEntity.noContent().build();
    }
}