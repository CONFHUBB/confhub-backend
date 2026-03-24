package com.capstone.confms.controller;

import com.capstone.confms.dto.ActivityAuditLogDTO;
import com.capstone.confms.dto.ConferenceActivityDTO;
import com.capstone.confms.service.ConferenceActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conferences/{conferenceId}/activities")
@RequiredArgsConstructor
@Tag(name = "Conference Timeline Activities", description = "Operations related to configuring Deadlines and Activities globally for a conference")
public class ConferenceActivityController {

    private final ConferenceActivityService activityService;

    @GetMapping
    @Operation(summary = "Get timeline activities for a specific conference")
    public ResponseEntity<List<ConferenceActivityDTO>> getActivities(@PathVariable Integer conferenceId) {
        return ResponseEntity.ok(activityService.getActivitiesByConferenceId(conferenceId));
    }

    @PutMapping
    @Operation(summary = "Bulk update timeline activities for a conference")
    public ResponseEntity<List<ConferenceActivityDTO>> updateActivities(
            @PathVariable Integer conferenceId,
            @RequestBody List<ConferenceActivityDTO> dtos) {
        return ResponseEntity.ok(activityService.updateActivities(conferenceId, dtos));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs for activity changes in a conference")
    public ResponseEntity<List<ActivityAuditLogDTO>> getAuditLogs(@PathVariable Integer conferenceId) {
        return ResponseEntity.ok(activityService.getAuditLogs(conferenceId));
    }
}
