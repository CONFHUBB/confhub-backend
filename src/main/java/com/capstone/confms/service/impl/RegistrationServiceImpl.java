package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.RegistrationRequest;
import com.capstone.confms.dto.response.CheckInResponse;
import com.capstone.confms.dto.response.RegistrationResponse;
import com.capstone.confms.dto.response.TicketResponse;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.integration.payment.VnPayIntegrationService;
import com.capstone.confms.repository.*;
import com.capstone.confms.repository.PaperAuthorRepository;
import com.capstone.confms.service.RegistrationService;
import com.capstone.confms.utils.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final VnPayIntegrationService vnPayIntegrationService;

    // Simple counter for registration number suffix (reset on restart; use sequence in production)
    private static final AtomicInteger counter = new AtomicInteger(100);

    @Override
    @Transactional
    public RegistrationResponse register(Integer conferenceId, Integer userId,
                                         RegistrationRequest request, String clientIp) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found: " + conferenceId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Check duplicate registration
        if (ticketRepository.findByUserAndConferenceId(user, conferenceId).isPresent()) {
            throw new BadRequestException("You have already registered for this conference.");
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

    // ========== Helpers ==========

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
