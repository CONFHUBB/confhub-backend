package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.RegistrationRequest;
import com.capstone.confhub.dto.response.CheckInResponse;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.dto.response.RegistrationResponse;
import com.capstone.confhub.dto.response.TicketResponse;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.integration.payment.VnPayIntegrationService;
import com.capstone.confhub.repository.*;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.service.RegistrationService;
import com.capstone.confhub.utils.enums.PaperStatus;
import com.capstone.confhub.utils.enums.PaymentStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final TicketRepository ticketRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final PaymentRepository paymentRepository;
    private final ConferenceRepository conferenceRepository;
    private final UserRepository userRepository;
    private final PaperRepository paperRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final VnPayIntegrationService vnPayIntegrationService;

    // Counter for registration number suffix — seeded from DB on startup to prevent duplicates after restarts
    private final AtomicInteger counter = new AtomicInteger(100);

    @PostConstruct
    public void initCounter() {
        String currentYear = String.valueOf(Year.now().getValue());
        Integer maxSuffix = ticketRepository.findMaxRegistrationSuffix(currentYear);
        if (maxSuffix != null) {
            counter.set(maxSuffix + 1);
            log.info("Registration counter initialized from DB: next value = {}", maxSuffix + 1);
        } else {
            log.info("No existing registrations found for year {}. Counter starts at 100.", currentYear);
        }
    }

    @Override
    @Transactional
    public RegistrationResponse register(Integer conferenceId, Integer userId,
                                         RegistrationRequest request, String clientIp) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found: " + conferenceId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Check duplicate registration — per paper (authors with multiple papers need separate tickets)
        if (request.getPaperId() != null) {
            if (ticketRepository.findByUser_IdAndPaperId(userId, request.getPaperId()).isPresent()) {
                throw new BadRequestException("You have already registered a ticket for this paper.");
            }
        } else {
            // Attendee (no paper) — only one ticket per conference
            if (ticketRepository.findByUserAndConferenceId(user, conferenceId).isPresent()) {
                throw new BadRequestException("You have already registered for this conference.");
            }
        }

        TicketType ticketType = ticketTypeRepository.findById(request.getTicketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("TicketType not found: " + request.getTicketTypeId()));

        // Validate ticket type belongs to this conference
        if (!ticketType.getConference().getId().equals(conferenceId)) {
            throw new BadRequestException("TicketType does not belong to this conference.");
        }

        if (!Boolean.TRUE.equals(ticketType.getIsActive())) {
            throw new BadRequestException("This ticket type is not currently available.");
        }

        // Check deadline
        if (ticketType.getDeadline() != null && LocalDateTime.now().isAfter(ticketType.getDeadline())) {
            throw new BadRequestException("Registration deadline for this ticket type has passed.");
        }

        // Check sold out
        if (ticketType.getMaxQuantity() != null &&
                ticketType.getQuantitySold() >= ticketType.getMaxQuantity()) {
            throw new BadRequestException("This ticket type is sold out.");
        }

        // Paper validation for author/presenter registration
        if (request.getPaperId() != null) {
            if (!paperRepository.existsById(request.getPaperId())) {
                throw new ResourceNotFoundException("Paper not found: " + request.getPaperId());
            }
            boolean isAuthor = paperAuthorRepository.findByPaperId(request.getPaperId())
                    .stream()
                    .anyMatch(pa -> pa.getUser().getId().equals(userId));
            if (!isAuthor) {
                throw new BadRequestException("You are not an author of this paper.");
            }
        }

        // Generate registration number
        String regNumber = String.format("CONF%d-%05d", Year.now().getValue(), counter.getAndIncrement());

        // Create ticket
        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setConference(conference);
        ticket.setTicketType(ticketType);
        ticket.setTicketTypeValue(ticketType.getName());
        ticket.setTicketTypeName(ticketType.getName());
        ticket.setPrice(ticketType.getPrice());
        ticket.setPaperId(request.getPaperId());
        ticket.setRegistrationNumber(regNumber);
        ticket.setIsCheckedIn(false);

        boolean isFree = ticketType.getPrice().compareTo(BigDecimal.ZERO) == 0;

        if (isFree) {
            // Free ticket — activate immediately
            ticket.setPaymentStatus(PaymentStatus.COMPLETED);
            ticket.setQrCode(UUID.randomUUID().toString());
            Ticket saved = ticketRepository.save(ticket);
            ticketType.setQuantitySold(ticketType.getQuantitySold() + 1);
            ticketTypeRepository.save(ticketType);
            // Transition paper to AWAITING_CAMERA_READY after free registration
            transitionPaperToAwaitingCameraReady(saved);
            log.info("Free ticket registered: {} for conference {}", regNumber, conferenceId);
            return RegistrationResponse.builder()
                    .ticket(mapToTicketResponse(saved))
                    .paymentUrl(null)
                    .build();
        } else {
            // Paid ticket — create pending payment
            ticket.setPaymentStatus(PaymentStatus.PENDING);
            Ticket saved = ticketRepository.save(ticket);

            // Create Payment record (PENDING)
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setTicket(saved);
            payment.setAmount(ticketType.getPrice().longValue());
            payment.setProvider("VNPAY");
            payment.setProviderTransactionId("PENDING");
            payment.setVnpTxnRef("PENDING");
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);

            // Generate VNPay URL
            String paymentUrl = vnPayIntegrationService.createPaymentUrl(
                    ticketType.getPrice().longValue(), clientIp, saved.getId());

            log.info("Pending ticket registered: {} for conference {}, VNPay URL created", regNumber, conferenceId);
            return RegistrationResponse.builder()
                    .ticket(mapToTicketResponse(saved))
                    .paymentUrl(paymentUrl)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getMyTicket(Integer conferenceId, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return ticketRepository.findByUserAndConferenceId(user, conferenceId)
                .map(this::mapToTicketResponse)
                .orElseThrow(() -> new ResourceNotFoundException("You are not registered for this conference."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return ticketRepository.findByUser(user).stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getAttendees(Integer conferenceId) {
        return ticketRepository.findByConferenceId(conferenceId).stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TicketResponse> getAttendeesPageable(
            Integer conferenceId, int page, int size, String search, String status) {

        // Parse optional payment status filter
        PaymentStatus paymentStatus = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            try {
                paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid payment status value: " + status);
            }
        }

        // Normalize search: pass null if blank so JPQL skips the filter
        String searchTerm = (search == null || search.isBlank()) ? null : search.trim();

        Page<Ticket> resultPage = ticketRepository.findAttendees(
                conferenceId, paymentStatus, searchTerm, PageRequest.of(page, size));

        List<TicketResponse> content = resultPage.getContent().stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());

        return PagedResponse.<TicketResponse>builder()
                .content(content)
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .last(resultPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void completePayment(Integer ticketId, String vnpTxnRef, String providerTransactionId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        ticket.setPaymentStatus(PaymentStatus.COMPLETED);
        ticket.setQrCode(UUID.randomUUID().toString());
        ticketRepository.save(ticket);

        paymentRepository.findByTicket(ticket).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setVnpTxnRef(vnpTxnRef);
            payment.setProviderTransactionId(providerTransactionId);
            payment.setTransactionTime(LocalDateTime.now());
            paymentRepository.save(payment);
        });

        // Increment sold count
        TicketType tt = ticket.getTicketType();
        if (tt != null) {
            tt.setQuantitySold(tt.getQuantitySold() + 1);
            ticketTypeRepository.save(tt);
        }

        // Transition paper to AWAITING_CAMERA_READY after paid registration
        transitionPaperToAwaitingCameraReady(ticket);

        log.info("Payment completed for ticket {}, txnRef={}", ticketId, vnpTxnRef);
    }

    @Override
    @Transactional
    public void failPayment(Integer ticketId, String vnpTxnRef) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        ticket.setPaymentStatus(PaymentStatus.FAILED);
        ticketRepository.save(ticket);

        paymentRepository.findByTicket(ticket).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setVnpTxnRef(vnpTxnRef);
            payment.setTransactionTime(LocalDateTime.now());
            paymentRepository.save(payment);
        });

        log.info("Payment failed for ticket {}", ticketId);
    }

    @Override
    @Transactional
    public Payment completePaymentAndGet(Integer ticketId, String vnpTxnRef, String providerTransactionId) {
        completePayment(ticketId, vnpTxnRef, providerTransactionId);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
        return paymentRepository.findByTicket(ticket).orElse(null);
    }

    @Override
    @Transactional
    public Payment failPaymentAndGet(Integer ticketId, String vnpTxnRef) {
        failPayment(ticketId, vnpTxnRef);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));
        return paymentRepository.findByTicket(ticket).orElse(null);
    }

    @Override
    @Transactional
    public CheckInResponse checkIn(String code) {
        Ticket ticket = code.startsWith("CONF")
                ? ticketRepository.findByRegistrationNumber(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + code))
                : ticketRepository.findByQrCode(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Invalid QR code: " + code));

        if (!PaymentStatus.COMPLETED.equals(ticket.getPaymentStatus())) {
            return CheckInResponse.builder()
                    .ticketId(ticket.getId())
                    .registrationNumber(ticket.getRegistrationNumber())
                    .attendeeName(ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName())
                    .attendeeEmail(ticket.getUser().getEmail())
                    .ticketTypeName(ticket.getTicketTypeName())
                    .isCheckedIn(false)
                    .message("Payment not completed for this ticket.")
                    .build();
        }

        if (Boolean.TRUE.equals(ticket.getIsCheckedIn())) {
            return CheckInResponse.builder()
                    .ticketId(ticket.getId())
                    .registrationNumber(ticket.getRegistrationNumber())
                    .attendeeName(ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName())
                    .attendeeEmail(ticket.getUser().getEmail())
                    .ticketTypeName(ticket.getTicketTypeName())
                    .isCheckedIn(true)
                    .message("Already checked in at " + ticket.getCheckInTime())
                    .build();
        }

        ticket.setIsCheckedIn(true);
        ticket.setCheckInTime(LocalDateTime.now());
        ticketRepository.save(ticket);

        log.info("Checked in attendee {} for conference {}",
                ticket.getRegistrationNumber(), ticket.getConference().getId());

        return CheckInResponse.builder()
                .ticketId(ticket.getId())
                .registrationNumber(ticket.getRegistrationNumber())
                .attendeeName(ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName())
                .attendeeEmail(ticket.getUser().getEmail())
                .ticketTypeName(ticket.getTicketTypeName())
                .isCheckedIn(true)
                .message("Check-in successful!")
                .build();
    }

        @Override
        @Transactional
        public CheckInResponse checkInForConference(String code, Integer conferenceId, Integer actorUserId) {
        Ticket ticket = code.startsWith("CONF")
            ? ticketRepository.findByRegistrationNumber(code)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + code))
            : ticketRepository.findByQrCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Invalid QR code: " + code));

        if (ticket.getConference() == null || !ticket.getConference().getId().equals(conferenceId)) {
            throw new BadRequestException("This QR code does not belong to conference: " + conferenceId);
        }

        // Check actor role: must be CONFERENCE_CHAIR or PROGRAM_CHAIR for the conference
        boolean isChair = conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            actorUserId, conferenceId, com.capstone.confhub.utils.enums.ConferenceTrackRole.CONFERENCE_CHAIR
        );
        boolean isProgramChair = conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            actorUserId, conferenceId, com.capstone.confhub.utils.enums.ConferenceTrackRole.PROGRAM_CHAIR
        );

        if (!isChair && !isProgramChair) {
            throw new BadRequestException("Unauthorized: user is not a chair for this conference.");
        }

        // Null-safe attendee values
        final String attendeeName = ticket.getUser() != null
            ? (ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName())
            : "";
        final String attendeeEmail = ticket.getUser() != null ? ticket.getUser().getEmail() : "";

        if (!PaymentStatus.COMPLETED.equals(ticket.getPaymentStatus())) {
            return CheckInResponse.builder()
                .ticketId(ticket.getId())
                .registrationNumber(ticket.getRegistrationNumber())
                .attendeeName(attendeeName)
                .attendeeEmail(attendeeEmail)
                .ticketTypeName(ticket.getTicketTypeName())
                .isCheckedIn(false)
                .message("Payment not completed for this ticket.")
                .build();
        }

        if (Boolean.TRUE.equals(ticket.getIsCheckedIn())) {
            return CheckInResponse.builder()
                .ticketId(ticket.getId())
                .registrationNumber(ticket.getRegistrationNumber())
                .attendeeName(attendeeName)
                .attendeeEmail(attendeeEmail)
                .ticketTypeName(ticket.getTicketTypeName())
                .isCheckedIn(true)
                .message("Already checked in at " + ticket.getCheckInTime())
                .build();
        }

        ticket.setIsCheckedIn(true);
        ticket.setCheckInTime(LocalDateTime.now());
        ticketRepository.save(ticket);

        log.info("Chair {} checked in attendee {} for conference {}",
            actorUserId, ticket.getRegistrationNumber(), ticket.getConference().getId());

        return CheckInResponse.builder()
            .ticketId(ticket.getId())
            .registrationNumber(ticket.getRegistrationNumber())
            .attendeeName(attendeeName)
            .attendeeEmail(attendeeEmail)
            .ticketTypeName(ticket.getTicketTypeName())
            .isCheckedIn(true)
            .message("Check-in successful!")
            .build();
        }

    @Override
    @Transactional
    public CheckInResponse checkInForMobileChair(String code, Integer actorUserId) {
        // Step 1: Find ticket by QR code or registration number
        Ticket ticket = code.startsWith("CONF")
            ? ticketRepository.findByRegistrationNumber(code)
                .orElse(null)
            : ticketRepository.findByQrCode(code)
                .orElse(null);

        if (ticket == null) {
            return CheckInResponse.builder()
                .message("Ticket not found. Invalid QR code or registration number.")
                .status("TICKET_NOT_FOUND")
                .build();
        }

        // Step 2: Check payment status
        if (!PaymentStatus.COMPLETED.equals(ticket.getPaymentStatus())) {
            final String attendeeName = ticket.getUser() != null
                ? (ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName())
                : "";
            final String attendeeEmail = ticket.getUser() != null ? ticket.getUser().getEmail() : "";
            
            return CheckInResponse.builder()
                .ticketId(ticket.getId())
                .registrationNumber(ticket.getRegistrationNumber())
                .attendeeName(attendeeName)
                .attendeeEmail(attendeeEmail)
                .ticketTypeName(ticket.getTicketTypeName())
                .isCheckedIn(false)
                .message("Payment not completed for this ticket.")
                .status("PAYMENT_NOT_COMPLETED")
                .build();
        }

        // Step 3: Verify actor is chair for this conference
        Integer conferenceId = ticket.getConference() != null ? ticket.getConference().getId() : null;
        if (conferenceId == null) {
            return CheckInResponse.builder()
                .ticketId(ticket.getId())
                .registrationNumber(ticket.getRegistrationNumber())
                .message("Conference information not found for this ticket.")
                .status("TICKET_NOT_FOUND")
                .build();
        }

        boolean isChair = conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            actorUserId, conferenceId, com.capstone.confhub.utils.enums.ConferenceTrackRole.CONFERENCE_CHAIR
        );
        boolean isProgramChair = conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            actorUserId, conferenceId, com.capstone.confhub.utils.enums.ConferenceTrackRole.PROGRAM_CHAIR
        );

        if (!isChair && !isProgramChair) {
            final String attendeeName = ticket.getUser() != null
                ? (ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName())
                : "";
            final String attendeeEmail = ticket.getUser() != null ? ticket.getUser().getEmail() : "";
            
            return CheckInResponse.builder()
                .ticketId(ticket.getId())
                .registrationNumber(ticket.getRegistrationNumber())
                .attendeeName(attendeeName)
                .attendeeEmail(attendeeEmail)
                .ticketTypeName(ticket.getTicketTypeName())
                .isCheckedIn(false)
                .message("You do not have permission to check in attendees for this conference.")
                .status("UNAUTHORIZED")
                .build();
        }

        // Step 4: Check if already checked in (after permission validation)
        if (Boolean.TRUE.equals(ticket.getIsCheckedIn())) {
            final String attendeeName = ticket.getUser() != null
                ? (ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName())
                : "";
            final String attendeeEmail = ticket.getUser() != null ? ticket.getUser().getEmail() : "";

            return CheckInResponse.builder()
                .ticketId(ticket.getId())
                .registrationNumber(ticket.getRegistrationNumber())
                .attendeeName(attendeeName)
                .attendeeEmail(attendeeEmail)
                .ticketTypeName(ticket.getTicketTypeName())
                .isCheckedIn(true)
                .message("Already checked in at " + ticket.getCheckInTime())
                .status("ALREADY_CHECKED_IN")
                .build();
        }

        // Step 5: All validations passed — proceed with check-in
        ticket.setIsCheckedIn(true);
        ticket.setCheckInTime(LocalDateTime.now());
        ticketRepository.save(ticket);

        final String attendeeName = ticket.getUser() != null
            ? (ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName())
            : "";
        final String attendeeEmail = ticket.getUser() != null ? ticket.getUser().getEmail() : "";

        log.info("Chair {} checked in attendee {} for conference {}",
            actorUserId, ticket.getRegistrationNumber(), conferenceId);

        return CheckInResponse.builder()
            .ticketId(ticket.getId())
            .registrationNumber(ticket.getRegistrationNumber())
            .attendeeName(attendeeName)
            .attendeeEmail(attendeeEmail)
            .ticketTypeName(ticket.getTicketTypeName())
            .isCheckedIn(true)
            .message("Check-in successful!")
            .status("SUCCESS")
            .build();
    }

    @Override
    @Transactional
    public RegistrationResponse retryPayment(Integer conferenceId, Integer userId, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Ticket ticket = ticketRepository.findByUserAndConferenceId(user, conferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("No registration found for this conference."));

        if (PaymentStatus.COMPLETED.equals(ticket.getPaymentStatus())) {
            throw new BadRequestException("Payment is already completed.");
        }

        // Reset to PENDING
        ticket.setPaymentStatus(PaymentStatus.PENDING);
        ticketRepository.save(ticket);

        // Generate new VNPay URL
        String paymentUrl = vnPayIntegrationService.createPaymentUrl(
                ticket.getPrice().longValue(), clientIp, ticket.getId());

        log.info("Retry payment for ticket {}, conference {}", ticket.getRegistrationNumber(), conferenceId);
        return RegistrationResponse.builder()
                .ticket(mapToTicketResponse(ticket))
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    @Transactional
    public void refundTicket(Integer conferenceId, Integer ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        if (!ticket.getConference().getId().equals(conferenceId)) {
            throw new BadRequestException("Ticket does not belong to this conference.");
        }

        if (PaymentStatus.REFUNDED.equals(ticket.getPaymentStatus())) {
            throw new BadRequestException("Ticket is already refunded.");
        }

        ticket.setPaymentStatus(PaymentStatus.REFUNDED);
        ticketRepository.save(ticket);

        paymentRepository.findByTicket(ticket).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setTransactionTime(LocalDateTime.now());
            paymentRepository.save(payment);
        });

        TicketType tt = ticket.getTicketType();
        if (tt != null && tt.getQuantitySold() > 0) {
            tt.setQuantitySold(tt.getQuantitySold() - 1);
            ticketTypeRepository.save(tt);
        }

        log.info("Ticket {} refunded for conference {}", ticket.getRegistrationNumber(), conferenceId);
    }

    @Override
    @Transactional
    public void cancelPendingTicket(Integer conferenceId, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Ticket ticket = ticketRepository.findByUserAndConferenceId(user, conferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("No registration found for this conference."));

        if (PaymentStatus.COMPLETED.equals(ticket.getPaymentStatus()) || PaymentStatus.REFUNDED.equals(ticket.getPaymentStatus())) {
            throw new BadRequestException("Cannot cancel a completed or refunded ticket.");
        }

        // Fully delete pending payment and ticket to allow re-registration
        paymentRepository.findByTicket(ticket).ifPresent(paymentRepository::delete);
        ticketRepository.delete(ticket);

        log.info("Cancelled pending ticket {} for user {} at conference {}", ticket.getRegistrationNumber(), userId, conferenceId);
    }

    // ========== Helpers ==========

    /**
     * Transition paper status to AWAITING_CAMERA_READY
     * when the author completes registration (ticket purchase).
     * Handles papers in either ACCEPTED or AWAITING_REGISTRATION status.
     */
    private void transitionPaperToAwaitingCameraReady(Ticket ticket) {
        if (ticket.getPaperId() == null) return;
        try {
            Paper paper = paperRepository.findById(ticket.getPaperId()).orElse(null);
            if (paper != null && (paper.getStatus() == PaperStatus.ACCEPTED
                    || paper.getStatus() == PaperStatus.AWAITING_REGISTRATION)) {
                paper.setStatus(PaperStatus.AWAITING_CAMERA_READY);
                paperRepository.save(paper);
                log.info("Paper {} transitioned to AWAITING_CAMERA_READY after registration (ticket {})",
                        paper.getId(), ticket.getRegistrationNumber());
            }
        } catch (Exception e) {
            log.warn("Failed to transition paper status for ticket {}: {}",
                    ticket.getRegistrationNumber(), e.getMessage());
        }
    }

    private TicketResponse mapToTicketResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .userId(t.getUser().getId())
                .userName(t.getUser().getFirstName() + " " + t.getUser().getLastName())
                .userEmail(t.getUser().getEmail())
                .conferenceId(t.getConference().getId())
                .conferenceName(t.getConference().getName())
                .ticketTypeId(t.getTicketType() != null ? t.getTicketType().getId() : null)
                .ticketTypeName(t.getTicketTypeName())
                .price(t.getPrice())
                .currency(t.getTicketType() != null ? t.getTicketType().getCurrency() : "VND")
                .paperId(t.getPaperId())
                .registrationNumber(t.getRegistrationNumber())
                .qrCode(t.getQrCode())
                .paymentStatus(t.getPaymentStatus())
                .isCheckedIn(t.getIsCheckedIn())
                .checkInTime(t.getCheckInTime())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
