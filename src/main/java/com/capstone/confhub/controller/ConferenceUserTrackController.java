package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confhub.dto.response.ConferenceResponseDTO;
import com.capstone.confhub.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.dto.response.UserResponseDTO;
import com.capstone.confhub.dto.response.UserWithRolesResponseDTO;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.ConferenceUserTrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conference-user-tracks")
@RequiredArgsConstructor
@Tag(name = "Conference User Track Management", description = "Assign roles to users in conference tracks")
public class ConferenceUserTrackController {
    private final ConferenceUserTrackService conferenceUserTrackService;

    @PostMapping("/assign-role")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Assign role to user in a conference track")
    public ResponseEntity<ConferenceUserTrackResponseDTO> assignRole(
            @Valid @RequestBody AssignConferenceUserTrackRequest request) {
        ConferenceUserTrackResponseDTO result = conferenceUserTrackService.assignRoleToUserTrack(request);
        return new ResponseEntity<>(result, Boolean.TRUE.equals(result.getSkipped()) ? HttpStatus.OK : HttpStatus.CREATED);
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
    @Operation(summary = "Accept conference invitation", description = "Update isAccepted = true for the ConferenceUserTrack record matching userId and conferenceId. Optionally set reviewerQuota.")
    public ResponseEntity<ConferenceUserTrackResponseDTO> acceptInvitation(
            @RequestParam Integer userId,
            @RequestParam Integer conferenceId,
            @RequestParam(required = false) Integer reviewerQuota) {
        return ResponseEntity.ok(conferenceUserTrackService.acceptInvitation(userId, conferenceId, reviewerQuota));
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

    @GetMapping("/users/{userId}/reviewer-conferences")
    @Operation(summary = "Get all conferences where user is assigned as REVIEWER (accepted only)")
    public ResponseEntity<PagedResponse<ConferenceResponseDTO>> getReviewerConferencesByUserId(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters");
        }
        return ResponseEntity.ok(conferenceUserTrackService.getReviewerConferencesByUserId(userId, page, size));
    }

    @GetMapping("/users/{userId}/my-roles")
    @Operation(summary = "Get all role assignments for a user across all conferences")
    public ResponseEntity<java.util.List<ConferenceUserTrackResponseDTO>> getUserRoleAssignments(
            @PathVariable Integer userId) {
        return ResponseEntity.ok(conferenceUserTrackService.getUserRoleAssignments(userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove a role assignment from a user")
    public ResponseEntity<Void> removeRoleFromUser(@PathVariable Integer id) {
        conferenceUserTrackService.removeRoleFromUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/resend-invitation")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Resend invitation", description = "Regenerate token and reset invitation to pending. Old email link will be invalidated.")
    public ResponseEntity<ConferenceUserTrackResponseDTO> resendInvitation(@PathVariable Integer id) {
        return ResponseEntity.ok(conferenceUserTrackService.resendInvitation(id));
    }

    @PutMapping("/reviewer-quota")
    @Operation(summary = "Update reviewer quota", description = "Set/update the maximum number of papers a reviewer wants to review")
    public ResponseEntity<ConferenceUserTrackResponseDTO> updateReviewerQuota(
            @RequestParam Integer userId,
            @RequestParam Integer conferenceId,
            @RequestParam Integer reviewerQuota) {
        return ResponseEntity.ok(conferenceUserTrackService.updateReviewerQuota(userId, conferenceId, reviewerQuota));
    }

    @GetMapping("/reviewer-quota")
    @Operation(summary = "Get reviewer quota", description = "Get the current review quota for a reviewer in a conference")
    public ResponseEntity<Integer> getReviewerQuota(
            @RequestParam Integer userId,
            @RequestParam Integer conferenceId) {
        return ResponseEntity.ok(conferenceUserTrackService.getReviewerQuota(userId, conferenceId));
    }

}
