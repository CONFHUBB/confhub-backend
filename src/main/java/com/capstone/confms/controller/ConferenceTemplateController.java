package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceTemplateDTO;
import com.capstone.confms.service.ConferenceTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conference-templates")
@RequiredArgsConstructor
public class ConferenceTemplateController {

    private final ConferenceTemplateService templateService;

    @PostMapping
    public ResponseEntity<ConferenceTemplateDTO> createTemplate(@RequestBody ConferenceTemplateDTO dto) {
        ConferenceTemplateDTO createdTemplate = templateService.createTemplate(dto);
        return new ResponseEntity<>(createdTemplate, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConferenceTemplateDTO> updateTemplate(
            @PathVariable Integer id,
            @RequestBody ConferenceTemplateDTO dto) {
        return ResponseEntity.ok(templateService.updateTemplate(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConferenceTemplateDTO> getTemplateById(@PathVariable Integer id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @GetMapping
    public ResponseEntity<List<ConferenceTemplateDTO>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/conference/{conferenceId}")
    public ResponseEntity<List<ConferenceTemplateDTO>> getTemplatesByConferenceTemplateId(@PathVariable Integer conferenceTemplateId) {
        return ResponseEntity.ok(templateService.getTemplatesByConferenceId(conferenceTemplateId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Integer id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}