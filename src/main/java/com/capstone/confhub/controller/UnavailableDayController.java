package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.UnavailableDayRequest;
import com.capstone.confhub.dto.response.UnavailableDayResponseDTO;
import com.capstone.confhub.entity.UnavailableDay;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.UnavailableDayRepository;
import com.capstone.confhub.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/unavailable-days")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Unavailable Days", description = "Manage date ranges when a user is unavailable for conference roles")
public class UnavailableDayController {

    private final UnavailableDayRepository unavailableDayRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get all unavailable day ranges for a user")
    public ResponseEntity<List<UnavailableDayResponseDTO>> getAll(@PathVariable Integer userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        List<UnavailableDayResponseDTO> result = unavailableDayRepository.findByUser_IdOrderByStartDateDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "Add a new unavailable day range")
    public ResponseEntity<UnavailableDayResponseDTO> create(
            @PathVariable Integer userId,
            @Valid @RequestBody UnavailableDayRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }

        UnavailableDay entity = new UnavailableDay();
        entity.setUser(user);
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setReason(request.getReason());

        UnavailableDay saved = unavailableDayRepository.save(entity);
        return new ResponseEntity<>(toDto(saved), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an unavailable day range")
    public ResponseEntity<Void> delete(@PathVariable Integer userId, @PathVariable Integer id) {
        UnavailableDay entity = unavailableDayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unavailable day not found with id " + id));

        if (!entity.getUser().getId().equals(userId)) {
            throw new BadRequestException("This unavailable day does not belong to the specified user");
        }

        unavailableDayRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an unavailable day range")
    public ResponseEntity<UnavailableDayResponseDTO> update(
            @PathVariable Integer userId,
            @PathVariable Integer id,
            @Valid @RequestBody UnavailableDayRequest request) {

        UnavailableDay entity = unavailableDayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unavailable day not found with id " + id));

        if (!entity.getUser().getId().equals(userId)) {
            throw new BadRequestException("This unavailable day does not belong to the specified user");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }

        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setReason(request.getReason());

        UnavailableDay saved = unavailableDayRepository.save(entity);
        return ResponseEntity.ok(toDto(saved));
    }

    private UnavailableDayResponseDTO toDto(UnavailableDay entity) {
        return UnavailableDayResponseDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
