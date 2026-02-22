package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceSubmissionFormDTO;
import com.capstone.confms.dto.response.ConferenceSubmissionFormResponseDTO;
import com.capstone.confms.service.ConferenceSubmissionFormService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conference-submission-forms")
@RequiredArgsConstructor
@Tag(name = "Conference Submission Form Management", description = "Operations related to Conference Submission Form setup")
public class ConferenceSubmissionFormController {

    private final ConferenceSubmissionFormService submissionFormService;

    @PostMapping
    public ResponseEntity<ConferenceSubmissionFormResponseDTO> createSubmissionForm(@RequestBody ConferenceSubmissionFormDTO dto) {
        ConferenceSubmissionFormResponseDTO created = submissionFormService.createSubmissionForm(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConferenceSubmissionFormResponseDTO> updateSubmissionForm(
            @PathVariable Integer id,
            @RequestBody ConferenceSubmissionFormDTO dto) {
        return ResponseEntity.ok(submissionFormService.updateSubmissionForm(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConferenceSubmissionFormResponseDTO> getSubmissionFormById(@PathVariable Integer id) {
        return ResponseEntity.ok(submissionFormService.getSubmissionFormById(id));
    }

    @GetMapping
    public ResponseEntity<List<ConferenceSubmissionFormResponseDTO>> getAllSubmissionForms() {
        return ResponseEntity.ok(submissionFormService.getAllSubmissionForms());
    }

    @GetMapping("/track/{trackId}")
    public ResponseEntity<List<ConferenceSubmissionFormResponseDTO>> getSubmissionFormsByTrackId(@PathVariable Integer trackId) {
        return ResponseEntity.ok(submissionFormService.getSubmissionFormsByTrackId(trackId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmissionForm(@PathVariable Integer id) {
        submissionFormService.deleteSubmissionForm(id);
        return ResponseEntity.noContent().build();
    }
}