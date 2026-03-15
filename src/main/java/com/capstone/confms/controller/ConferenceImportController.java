package com.capstone.confms.controller;

import com.capstone.confms.dto.response.ImportResultDTO;
import com.capstone.confms.service.ConferenceImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Data Import", description = "Import conference data from Excel files")
public class ConferenceImportController {

    private final ConferenceImportService importService;

    private static final MediaType XLSX_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    // ── Conference ──

    @GetMapping("/conferences/import/template")
    @Operation(summary = "Download conference Excel template")
    public ResponseEntity<byte[]> conferenceTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=conference_template.xlsx")
                .contentType(XLSX_TYPE)
                .body(importService.generateConferenceTemplate());
    }

    @PostMapping("/conferences/import/preview")
    @Operation(summary = "Preview conference data from Excel (no DB write)")
    public ResponseEntity<ImportResultDTO> previewConference(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.previewConferenceFromExcel(file));
    }

    @PostMapping("/conferences/import")
    @Operation(summary = "Import conference from Excel file")
    public ResponseEntity<ImportResultDTO> importConference(@RequestParam("file") MultipartFile file) {
        ImportResultDTO result = importService.importConferenceFromExcel(file);
        return result.isSuccess()
                ? ResponseEntity.status(HttpStatus.CREATED).body(result)
                : ResponseEntity.badRequest().body(result);
    }

    // ── Tracks ──

    @GetMapping("/conferences/{conferenceId}/tracks/import/template")
    @Operation(summary = "Download track Excel template")
    public ResponseEntity<byte[]> trackTemplate(@PathVariable Integer conferenceId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=track_template.xlsx")
                .contentType(XLSX_TYPE)
                .body(importService.generateTrackTemplate());
    }

    @PostMapping("/conferences/{conferenceId}/tracks/import/preview")
    @Operation(summary = "Preview tracks from Excel (no DB write)")
    public ResponseEntity<ImportResultDTO> previewTracks(
            @PathVariable Integer conferenceId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.previewTracksFromExcel(file));
    }

    @PostMapping("/conferences/{conferenceId}/tracks/import")
    @Operation(summary = "Import tracks for a conference from Excel file")
    public ResponseEntity<ImportResultDTO> importTracks(
            @PathVariable Integer conferenceId,
            @RequestParam("file") MultipartFile file) {
        ImportResultDTO result = importService.importTracksFromExcel(conferenceId, file);
        return result.isSuccess()
                ? ResponseEntity.status(HttpStatus.CREATED).body(result)
                : ResponseEntity.badRequest().body(result);
    }

    // ── Subject Areas ──

    @GetMapping("/subject-areas/import/template")
    @Operation(summary = "Download subject area Excel template")
    public ResponseEntity<byte[]> subjectAreaTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subject_area_template.xlsx")
                .contentType(XLSX_TYPE)
                .body(importService.generateSubjectAreaTemplate());
    }

    @PostMapping("/subject-areas/import/preview")
    @Operation(summary = "Preview subject areas from Excel (no DB write)")
    public ResponseEntity<ImportResultDTO> previewSubjectAreas(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.previewSubjectAreasFromExcel(file));
    }

    @PostMapping("/subject-areas/import")
    @Operation(summary = "Import subject areas for a track from Excel file")
    public ResponseEntity<ImportResultDTO> importSubjectAreas(
            @RequestParam("trackId") Integer trackId,
            @RequestParam("file") MultipartFile file) {
        ImportResultDTO result = importService.importSubjectAreasFromExcel(trackId, file);
        return result.isSuccess()
                ? ResponseEntity.status(HttpStatus.CREATED).body(result)
                : ResponseEntity.badRequest().body(result);
    }
}
