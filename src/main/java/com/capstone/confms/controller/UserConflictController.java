package com.capstone.confms.controller;

import com.capstone.confms.dto.request.UserConflictRequest;
import com.capstone.confms.dto.response.UserConflictResponseDTO;
import com.capstone.confms.service.UserConflictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/conflicts")
@RequiredArgsConstructor
@Tag(name = "User Conflict of Interest Management", description = "Endpoints for managing user conflicts of interest")
public class UserConflictController {

    private final UserConflictService userConflictService;

    @GetMapping
    @Operation(summary = "Get all conflicts of interest for a user")
    public ResponseEntity<List<UserConflictResponseDTO>> getConflicts(@PathVariable Integer userId) {
        return ResponseEntity.ok(userConflictService.getConflictsByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "Add a new conflict of interest")
    public ResponseEntity<UserConflictResponseDTO> addConflict(
            @PathVariable Integer userId,
            @Valid @RequestBody UserConflictRequest request) {
        return new ResponseEntity<>(userConflictService.addConflict(userId, request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{conflictId}")
    @Operation(summary = "Delete a conflict of interest")
    public ResponseEntity<Void> deleteConflict(@PathVariable Integer userId, @PathVariable Integer conflictId) {
        userConflictService.deleteConflict(conflictId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{conflictId}/toggle-active")
    @Operation(summary = "Toggle active status of a conflict")
    public ResponseEntity<UserConflictResponseDTO> toggleActive(
            @PathVariable Integer userId, @PathVariable Integer conflictId) {
        return ResponseEntity.ok(userConflictService.toggleConflictActive(conflictId));
    }
}
