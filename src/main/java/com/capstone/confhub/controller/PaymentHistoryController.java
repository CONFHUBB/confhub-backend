package com.capstone.confhub.controller;

import com.capstone.confhub.dto.response.PaymentHistoryResponse;
import com.capstone.confhub.entity.PaymentHistory;
import com.capstone.confhub.entity.Ticket;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.PaymentHistoryRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Payment History", description = "Retrieve payment audit history for tickets and subscriptions")
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
     * Returns all VNPay callbacks recorded for all tickets + subscription in this conference.
     */
    @GetMapping("/conferences/{conferenceId}/payment-history")
    @Operation(summary = "Get full payment history for a conference (tickets + subscription)")
    public ResponseEntity<List<PaymentHistoryResponse>> getConferencePaymentHistory(
            @PathVariable Integer conferenceId) {
        // Ticket payments
        List<Ticket> tickets = ticketRepository.findByConferenceId(conferenceId);
        List<PaymentHistory> ticketHistory = tickets.stream()
                .flatMap(t -> paymentHistoryRepository.findByTicketOrderByRecordedAtDesc(t).stream())
                .toList();

        // Subscription payments
        List<PaymentHistory> subHistory = paymentHistoryRepository
                .findByConference_IdOrderByRecordedAtDesc(conferenceId);

        // Merge and sort
        List<PaymentHistory> all = new ArrayList<>();
        all.addAll(ticketHistory);
        all.addAll(subHistory);
        all.sort((a, b) -> b.getRecordedAt().compareTo(a.getRecordedAt()));

        return ResponseEntity.ok(all.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    /**
     * GET /api/v1/my-payment-history
     * Returns all VNPay callbacks for the user: ticket payments + subscription payments (if user is chair).
     */
    @GetMapping("/my-payment-history")
    @Operation(summary = "Get full payment history for the current user across all conferences")
    public ResponseEntity<List<PaymentHistoryResponse>> getMyPaymentHistory(
            @RequestParam Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        List<PaymentHistory> all = new ArrayList<>();

        // 1. Ticket payments
        List<Ticket> tickets = ticketRepository.findByUser(user);
        if (!tickets.isEmpty()) {
            all.addAll(paymentHistoryRepository.findByTicketInOrderByRecordedAtDesc(tickets));
        }

        // 2. Subscription payments (where user is conference chair)
        all.addAll(paymentHistoryRepository.findSubscriptionPaymentsByChairUserId(userId));

        // Sort by recordedAt desc
        all.sort((a, b) -> b.getRecordedAt().compareTo(a.getRecordedAt()));

        return ResponseEntity.ok(all.stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    private PaymentHistoryResponse mapToResponse(PaymentHistory h) {
        // Determine payment type and conference info
        boolean isSubscription = h.getTicket() == null && h.getConference() != null;
        String paymentType = isSubscription ? "SUBSCRIPTION" : "TICKET";

        Integer conferenceId = null;
        String conferenceName = null;
        if (h.getConference() != null) {
            conferenceId = h.getConference().getId();
            conferenceName = h.getConference().getName();
        } else if (h.getTicket() != null && h.getTicket().getConference() != null) {
            conferenceId = h.getTicket().getConference().getId();
            conferenceName = h.getTicket().getConference().getName();
        }

        return PaymentHistoryResponse.builder()
                .id(h.getId())
                .ticketId(h.getTicket() != null ? h.getTicket().getId() : null)
                .registrationNumber(h.getTicket() != null ? h.getTicket().getRegistrationNumber() : null)
                .conferenceId(conferenceId)
                .conferenceName(conferenceName)
                .paymentType(paymentType)
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
