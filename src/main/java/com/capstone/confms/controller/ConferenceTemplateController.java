package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceTemplateDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ConferenceTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conference-templates")
@RequiredArgsConstructor
@Tag(name = "Conference Template", description = "Operations related to Conference Template setup and related entities")
public class ConferenceTemplateController {

    private final ConferenceTemplateService conferenceTemplateService;

    @PostMapping
    @Operation(summary = "Create a new Conference Template")
    public ResponseEntity<ConferenceTemplateDTO> createTemplate(@Valid @RequestBody ConferenceTemplateDTO dto) {
        return new ResponseEntity<>(conferenceTemplateService.createTemplate(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Conference Templates")
    public ResponseEntity<PagedResponse<ConferenceTemplateDTO>> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceTemplateService.getAllTemplates(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Conference Template by ID")
    public ResponseEntity<ConferenceTemplateDTO> getTemplateById(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceTemplateService.getTemplateById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Conference Template details")
    public ResponseEntity<ConferenceTemplateDTO> updateTemplate(@Valid @PathVariable Integer id,
            @RequestBody ConferenceTemplateDTO dto) {
        return ResponseEntity.ok(conferenceTemplateService.updateTemplate(id, dto));
    }

    @GetMapping("/conference/{id}")
    @Operation(summary = "Get Conference Templates by Conference ID")
    public ResponseEntity<PagedResponse<ConferenceTemplateDTO>> getTemplatesByConferenceId(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceTemplateService.getTemplatesByConferenceId(id, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Conference Template")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Integer id) {
        conferenceTemplateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}