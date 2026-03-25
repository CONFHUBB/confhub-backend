package com.capstone.confms.controller;

import com.capstone.confms.dto.request.TicketTypeRequest;
import com.capstone.confms.dto.response.TicketTypeResponse;
import com.capstone.confms.service.TicketTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Ticket Types", description = "Chair manages conference ticket types and fee configuration")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    @PostMapping("/conferences/{conferenceId}/ticket-types")
    @Operation(summary = "Create ticket type (Chair)")
    public ResponseEntity<TicketTypeResponse> create(
            @PathVariable Integer conferenceId,
            @Valid @RequestBody TicketTypeRequest request) {
        return new ResponseEntity<>(ticketTypeService.create(conferenceId, request), HttpStatus.CREATED);
    }

    @GetMapping("/conferences/{conferenceId}/ticket-types")
    @Operation(summary = "List ticket types for a conference (public)")
    public ResponseEntity<List<TicketTypeResponse>> getByConference(
            @PathVariable Integer conferenceId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ticketTypeService.getByConference(conferenceId, activeOnly));
    }

    @PutMapping("/ticket-types/{id}")
    @Operation(summary = "Update ticket type (Chair)")
    public ResponseEntity<TicketTypeResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody TicketTypeRequest request) {
        return ResponseEntity.ok(ticketTypeService.update(id, request));
    }

    @DeleteMapping("/ticket-types/{id}")
    @Operation(summary = "Delete ticket type (Chair)")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        ticketTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
