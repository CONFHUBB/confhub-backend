package com.capstone.confhub.controller;

import com.capstone.confhub.dto.ConferenceSubmissionFormDTO;
import com.capstone.confhub.dto.response.ConferenceSubmissionFormResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.ConferenceSubmissionFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conference-submission-forms")
@RequiredArgsConstructor
@Tag(name = "Conference Submission Form", description = "Operations related to Conference Submission Form setup and related entities")
public class ConferenceSubmissionFormController {

    private final ConferenceSubmissionFormService conferenceSubmissionFormService;

    @PostMapping
    @Operation(summary = "Create a new Conference Submission Form")
    public ResponseEntity<ConferenceSubmissionFormResponseDTO> createSubmissionForm(
            @Valid @RequestBody ConferenceSubmissionFormDTO dto) {
        return new ResponseEntity<>(conferenceSubmissionFormService.createSubmissionForm(dto), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all Conference Submission Forms")
    public ResponseEntity<PagedResponse<ConferenceSubmissionFormResponseDTO>> getAllSubmissionForms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceSubmissionFormService.getAllSubmissionForms(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Conference Submission Form by ID")
    public ResponseEntity<ConferenceSubmissionFormResponseDTO> getSubmissionFormById(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceSubmissionFormService.getSubmissionFormById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Conference Submission Form details")
    public ResponseEntity<ConferenceSubmissionFormResponseDTO> updateSubmissionForm(@Valid @PathVariable Integer id,
            @RequestBody ConferenceSubmissionFormDTO dto) {
        return ResponseEntity.ok(conferenceSubmissionFormService.updateSubmissionForm(id, dto));
    }

    @GetMapping("/conference/{conferenceId}")
    @Operation(summary = "Get Conference Submission Forms by Conference ID")
    public ResponseEntity<PagedResponse<ConferenceSubmissionFormResponseDTO>> getSubmissionFormsByConferenceId(
            @PathVariable Integer conferenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceSubmissionFormService.getSubmissionFormsByConferenceId(conferenceId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Conference Submission Form")
    public ResponseEntity<Void> deleteSubmissionForm(@PathVariable Integer id) {
        conferenceSubmissionFormService.deleteSubmissionForm(id);
        return ResponseEntity.noContent().build();
    }
}