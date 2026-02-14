package com.capstone.confms.controller;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.service.PaperCheckLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/paper-check-logs")
@RequiredArgsConstructor
@Tag(name = "Paper CheckLog Management", description = "Operations related to Paper CheckLog setup and related entities")
public class PaperCheckLogController {

    private final PaperCheckLogService paperCheckLogService;


    @PostMapping("/check-logs")
    @Operation(summary = "Create a new Paper Check Log")
    public ResponseEntity<PaperCheckLogResponseDTO> createPaperCheckLog(@Valid @RequestBody PaperCheckLogDTO dto) {
        return new ResponseEntity<>(paperCheckLogService.createPaperCheckLog(dto), HttpStatus.CREATED);
    }

    @GetMapping("/check-logs")
    @Operation(summary = "Get all Paper Check Logs")
    public ResponseEntity<List<PaperCheckLogResponseDTO>> getAllPaperCheckLogs() {
        return ResponseEntity.ok(paperCheckLogService.getAllPaperCheckLogs());
    }

    @GetMapping("/check-logs/{id}")
    @Operation(summary = "Get Paper Check Log by ID")
    public ResponseEntity<PaperCheckLogResponseDTO> getPaperCheckLogById(@PathVariable Integer id) {
        return ResponseEntity.ok(paperCheckLogService.getPaperCheckLogById(id));
    }

    @PutMapping("/check-logs/{id}")
    @Operation(summary = "Update Paper Check Log details")
    public ResponseEntity<PaperCheckLogResponseDTO> updatePaperCheckLog(@Valid @PathVariable Integer id, @RequestBody PaperCheckLogDTO dto) {
        return ResponseEntity.ok(paperCheckLogService.updatePaperCheckLog(id, dto));
    }

    @DeleteMapping("/check-logs/{id}")
    @Operation(summary = "Delete a Paper Check Log")
    public ResponseEntity<Void> deletePaperCheckLog(@PathVariable Integer id) {
        paperCheckLogService.deletePaperCheckLog(id);
        return ResponseEntity.noContent().build();
    }
}