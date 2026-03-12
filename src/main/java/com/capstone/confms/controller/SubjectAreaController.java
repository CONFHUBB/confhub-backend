package com.capstone.confms.controller;

import com.capstone.confms.dto.SubjectAreaDTO;
import com.capstone.confms.dto.response.SubjectAreaResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.SubjectAreaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subject-areas")
@RequiredArgsConstructor
@Tag(name = "Subject Area", description = "Operations related to Subject Area management")
public class SubjectAreaController {

    private final SubjectAreaService subjectAreaService;

    @PostMapping
    @Operation(summary = "Create a new Subject Area")
    public ResponseEntity<SubjectAreaResponseDTO> createSubjectArea(
            @Valid @RequestBody SubjectAreaDTO dto) {
        return new ResponseEntity<>(subjectAreaService.createSubjectArea(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Subject Areas")
    public ResponseEntity<PagedResponse<SubjectAreaResponseDTO>> getAllSubjectAreas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(subjectAreaService.getAllSubjectAreas(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Subject Area by ID")
    public ResponseEntity<SubjectAreaResponseDTO> getSubjectAreaById(@PathVariable Integer id) {
        return ResponseEntity.ok(subjectAreaService.getSubjectAreaById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Subject Area details")
    public ResponseEntity<SubjectAreaResponseDTO> updateSubjectArea(@Valid @PathVariable Integer id,
            @RequestBody SubjectAreaDTO dto) {
        return ResponseEntity.ok(subjectAreaService.updateSubjectArea(id, dto));
    }

    @GetMapping("/track/{trackId}")
    @Operation(summary = "Get Subject Areas by Track ID")
    public ResponseEntity<PagedResponse<SubjectAreaResponseDTO>> getSubjectAreasByTrackId(
            @PathVariable Integer trackId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(subjectAreaService.getSubjectAreasByTrackId(trackId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Subject Area")
    public ResponseEntity<Void> deleteSubjectArea(@PathVariable Integer id) {
        subjectAreaService.deleteSubjectArea(id);
        return ResponseEntity.noContent().build();
    }
}
