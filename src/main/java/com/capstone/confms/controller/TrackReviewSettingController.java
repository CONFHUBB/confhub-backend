package com.capstone.confms.controller;

import com.capstone.confms.dto.TrackReviewSettingDTO;
import com.capstone.confms.service.TrackReviewSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracks/{trackId}/review-settings")
@RequiredArgsConstructor
@Tag(name = "Track Review Settings", description = "Operations related to Conference Track Review Settings")
public class TrackReviewSettingController {

    private final TrackReviewSettingService reviewSettingService;

    @GetMapping
    @Operation(summary = "Get review settings for a specific track")
    public ResponseEntity<TrackReviewSettingDTO> getReviewSettings(@PathVariable Integer trackId) {
        return ResponseEntity.ok(reviewSettingService.getReviewSettingsByTrackId(trackId));
    }

    @PutMapping
    @Operation(summary = "Update review settings for a specific track")
    public ResponseEntity<TrackReviewSettingDTO> updateReviewSettings(
            @PathVariable Integer trackId,
            @RequestBody TrackReviewSettingDTO dto) {
        return ResponseEntity.ok(reviewSettingService.updateReviewSettings(trackId, dto));
    }

    @PostMapping("/copy")
    @Operation(summary = "Copy review settings from another track")
    public ResponseEntity<Void> copyReviewSettings(
            @PathVariable Integer trackId,
            @RequestParam Integer sourceTrackId) {
        reviewSettingService.copyReviewSettings(sourceTrackId, trackId);
        return ResponseEntity.ok().build();
    }
}
