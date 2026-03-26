package com.capstone.confms.controller;

import com.capstone.confms.dto.response.PaymentHistoryResponse;
import com.capstone.confms.entity.PaymentHistory;
import com.capstone.confms.entity.Ticket;
import com.capstone.confms.entity.User;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.PaymentHistoryRepository;
import com.capstone.confms.repository.TicketRepository;
import com.capstone.confms.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Payment History", description = "Retrieve payment audit history for tickets")
public class PaymentHistoryController {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    /**
     * GET /api/v1/tickets/{ticketId}/payment-history
     * Returns all VNPay callbacks recorded for this ticket in reverse chronological order.
     */
    @GetMapping("/tickets/{ticketId}/payment-history")
    @Operation(summary = "Get full payment history for a ticket")
    public ResponseEntity<List<PaymentHistoryResponse>> getPaymentHistory(@PathVariable Integer ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        List<PaymentHistoryResponse> history = paymentHistoryRepository
                .findByTicketOrderByRecordedAtDesc(ticket)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/v1/conferences/{conferenceId}/payment-history
     * Returns all VNPay callbacks recorded for all tickets in this conference.
     */
    @GetMapping("/conferences/{conferenceId}/payment-history")
    @Operation(summary = "Get full payment history for all tickets in a conference (Chair view)")
    public ResponseEntity<List<PaymentHistoryResponse>> getConferencePaymentHistory(
            @PathVariable Integer conferenceId) {
        List<Ticket> tickets = ticketRepository.findByConferenceId(conferenceId);
        List<PaymentHistoryResponse> history = tickets.stream()
                .flatMap(t -> paymentHistoryRepository.findByTicketOrderByRecordedAtDesc(t).stream())
                .sorted((a, b) -> b.getRecordedAt().compareTo(a.getRecordedAt()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/v1/my-payment-history
     * Returns all VNPay callbacks recorded for all tickets belonging to a specific user.
     */
    @GetMapping("/my-payment-history")
    @Operation(summary = "Get full payment history for the current user across all conferences")
    public ResponseEntity<List<PaymentHistoryResponse>> getMyPaymentHistory(
            @RequestParam Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        List<Ticket> tickets = ticketRepository.findByUser(user);

        // If user has no tickets, return empty list immediately to avoid querying with empty list
        if (tickets.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<PaymentHistoryResponse> history = paymentHistoryRepository
                .findByTicketInOrderByRecordedAtDesc(tickets)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    private PaymentHistoryResponse mapToResponse(PaymentHistory h) {
        return PaymentHistoryResponse.builder()
                .id(h.getId())
                .ticketId(h.getTicket() != null ? h.getTicket().getId() : null)
                .registrationNumber(h.getTicket() != null ? h.getTicket().getRegistrationNumber() : null)
                .vnpTxnRef(h.getVnpTxnRef())
                .vnpTransactionNo(h.getVnpTransactionNo())
                .vnpTransactionStatus(h.getVnpTransactionStatus())
                .vnpResponseCode(h.getVnpResponseCode())
                .amount(h.getAmount())
                .bankCode(h.getBankCode())
                .payDate(h.getPayDate())
                .signatureValid(h.getSignatureValid())
                .outcome(h.getOutcome())
                .recordedAt(h.getRecordedAt())
                .build();
    }
}
