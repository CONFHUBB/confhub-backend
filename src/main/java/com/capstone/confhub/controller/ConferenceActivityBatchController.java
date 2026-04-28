package com.capstone.confhub.controller;

import com.capstone.confhub.dto.ConferenceActivityDTO;
import com.capstone.confhub.service.ConferenceActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/conferences/activities")
@RequiredArgsConstructor
@Tag(name = "Conference Timeline Activities", description = "Batch operations for conference activity timelines")
public class ConferenceActivityBatchController {

    private final ConferenceActivityService activityService;

    @GetMapping("/upcoming-deadlines")
    @Operation(summary = "Get nearest upcoming enabled activity per conference")
    public ResponseEntity<Map<Integer, ConferenceActivityDTO>> getUpcomingDeadlines(
            @RequestParam(required = false) List<Integer> conferenceIds
    ) {
        return ResponseEntity.ok(activityService.getUpcomingActivitiesByConferenceIds(conferenceIds));
    }
}
