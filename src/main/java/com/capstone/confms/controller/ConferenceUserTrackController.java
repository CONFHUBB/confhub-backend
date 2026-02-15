package com.capstone.confms.controller;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confms.service.ConferenceUserTrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conference-user-tracks")
@RequiredArgsConstructor
@Tag(name = "Conference User Track Management", description = "Assign roles to users in conference tracks")
public class ConferenceUserTrackController {
    private final ConferenceUserTrackService conferenceUserTrackService;

    @PostMapping("/assign-role")
    @Operation(summary = "Assign role to user in a conference track")
    public ResponseEntity<ConferenceUserTrackResponseDTO> assignRole(@Valid @RequestBody AssignConferenceUserTrackRequest request) {
        ConferenceUserTrackResponseDTO result = conferenceUserTrackService.assignRoleToUserTrack(request);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }
}
