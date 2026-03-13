package com.capstone.confms.controller;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.dto.response.UserResponseDTO;
import com.capstone.confms.dto.response.UserWithRolesResponseDTO;
import com.capstone.confms.exception.BadRequestException;
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
    public ResponseEntity<ConferenceUserTrackResponseDTO> assignRole(
            @Valid @RequestBody AssignConferenceUserTrackRequest request) {
        ConferenceUserTrackResponseDTO result = conferenceUserTrackService.assignRoleToUserTrack(request);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping("/conferences/{conferenceId}/track-chairs")
    @Operation(summary = "Get all users with PROGRAM_CHAIR role for a conference")
    public ResponseEntity<PagedResponse<UserResponseDTO>> getTrackChairsByConferenceId(
            @PathVariable Integer conferenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceUserTrackService.getTrackChairsByConferenceId(conferenceId, page, size));
    }

    @GetMapping("/users/{userId}/chaired-conferences")
    @Operation(summary = "Get all conferences where user is assigned as PROGRAM_CHAIR")
    public ResponseEntity<PagedResponse<ConferenceResponseDTO>> getChairedConferencesByUserId(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceUserTrackService.getChairedConferencesByUserId(userId, page, size));
    }

    @GetMapping("/users/{userId}/organized-conferences")
    @Operation(summary = "Get all conferences where user is assigned as CONFERENCE_CHAIR")
    public ResponseEntity<PagedResponse<ConferenceResponseDTO>> getOrganizedConferencesByUserId(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceUserTrackService.getOrganizedConferencesByUserId(userId, page, size));
    }

    @PutMapping("/accept")
    @Operation(summary = "Accept conference invitation", description = "Update isAccepted = true for the ConferenceUserTrack record matching userId and conferenceId")
    public ResponseEntity<ConferenceUserTrackResponseDTO> acceptInvitation(
            @RequestParam Integer userId,
            @RequestParam Integer conferenceId) {
        return ResponseEntity.ok(conferenceUserTrackService.acceptInvitation(userId, conferenceId));
    }

    @PutMapping("/decline")
    @Operation(summary = "Decline conference invitation", description = "Update isAccepted = false for the ConferenceUserTrack record matching userId and conferenceId")
    public ResponseEntity<ConferenceUserTrackResponseDTO> declineInvitation(
            @RequestParam Integer userId,
            @RequestParam Integer conferenceId) {
        return ResponseEntity.ok(conferenceUserTrackService.declineInvitation(userId, conferenceId));
    }

    @GetMapping("/conferences/{conferenceId}/users-roles")
    @Operation(summary = "Get all users with their roles in a conference")
    public ResponseEntity<PagedResponse<UserWithRolesResponseDTO>> getConferenceUsersWithRoles(
            @PathVariable Integer conferenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceUserTrackService.getConferenceUsersWithRoles(conferenceId, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a role assignment from a user")
    public ResponseEntity<Void> removeRoleFromUser(@PathVariable Integer id) {
        conferenceUserTrackService.removeRoleFromUser(id);
        return ResponseEntity.noContent().build();
    }

}
