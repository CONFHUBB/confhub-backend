package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.RegistrationRequest;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.PaperAuthor;
import com.capstone.confhub.entity.Payment;
import com.capstone.confhub.entity.Ticket;
import com.capstone.confhub.entity.TicketType;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.integration.payment.VnPayIntegrationService;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.PaymentRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.repository.TicketTypeRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.utils.enums.PaymentStatus;
import com.capstone.confhub.utils.enums.TicketCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ConferenceRepository conferenceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private PaperAuthorRepository paperAuthorRepository;
    @Mock
    private VnPayIntegrationService vnPayIntegrationService;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private Conference conference;
    private User user;
    private TicketType ticketType;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(1);
        conference.setName("CONF");

        user = new User();
        user.setId(2);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@conf.com");

        ticketType = new TicketType();
        ticketType.setId(3);
        ticketType.setConference(conference);
        ticketType.setName("Standard");
        ticketType.setCategory(TicketCategory.STANDARD);
        ticketType.setCurrency("VND");
        ticketType.setIsActive(true);
        ticketType.setQuantitySold(0);
        ticketType.setMaxQuantity(10);
        ticketType.setDeadline(LocalDateTime.now().plusDays(3));
    }

    @Test
    void registerShouldReturnCompletedTicketWhenFree() {
        ticketType.setPrice(BigDecimal.ZERO);

        RegistrationRequest request = new RegistrationRequest();
        request.setTicketTypeId(3);

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(ticketRepository.findByUserAndConferenceId(user, 1)).thenReturn(Optional.empty());
        when(ticketTypeRepository.findById(3)).thenReturn(Optional.of(ticketType));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(100);
            return saved;
        });

        var result = registrationService.register(1, 2, request, "127.0.0.1");

        assertNotNull(result);
        assertNotNull(result.getTicket());
        assertEquals(PaymentStatus.COMPLETED, result.getTicket().getPaymentStatus());
        assertNull(result.getPaymentUrl());
        assertNotNull(result.getTicket().getQrCode());
        assertEquals(1, ticketType.getQuantitySold());

        verify(ticketTypeRepository).save(ticketType);
    }

    @Test
    void registerShouldReturnPendingTicketAndPaymentUrlWhenPaid() {
        ticketType.setPrice(BigDecimal.valueOf(1000000));

        RegistrationRequest request = new RegistrationRequest();
        request.setTicketTypeId(3);

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(ticketRepository.findByUserAndConferenceId(user, 1)).thenReturn(Optional.empty());
        when(ticketTypeRepository.findById(3)).thenReturn(Optional.of(ticketType));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(101);
            return saved;
        });
        when(vnPayIntegrationService.createPaymentUrl(1000000L, "127.0.0.1", 101)).thenReturn("https://pay.url");

        var result = registrationService.register(1, 2, request, "127.0.0.1");

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getTicket().getPaymentStatus());
        assertEquals("https://pay.url", result.getPaymentUrl());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment payment = paymentCaptor.getValue();
        assertEquals(1000000L, payment.getAmount());
        assertEquals("VNPAY", payment.getProvider());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void registerShouldSucceedWithPaperWhenUserIsAuthor() {
        ticketType.setPrice(BigDecimal.ZERO);

        RegistrationRequest request = new RegistrationRequest();
        request.setTicketTypeId(3);
        request.setPaperId(88);

        PaperAuthor paperAuthor = new PaperAuthor();
        paperAuthor.setUser(user);

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(ticketRepository.findByUserAndConferenceId(user, 1)).thenReturn(Optional.empty());
        when(ticketTypeRepository.findById(3)).thenReturn(Optional.of(ticketType));
        when(paperRepository.existsById(88)).thenReturn(true);
        when(paperAuthorRepository.findByPaperId(88)).thenReturn(List.of(paperAuthor));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket saved = invocation.getArgument(0);
            saved.setId(102);
            return saved;
        });

        var result = registrationService.register(1, 2, request, "127.0.0.1");

        assertNotNull(result);
        assertEquals(88, result.getTicket().getPaperId());
        verify(paperRepository).existsById(88);
        verify(paperAuthorRepository).findByPaperId(88);
    }

    @Test
    void getMyTicketShouldReturnMappedTicketResponse() {
        Ticket ticket = buildTicket(200, PaymentStatus.PENDING);
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(ticketRepository.findByUserAndConferenceId(user, 1)).thenReturn(Optional.of(ticket));

        var result = registrationService.getMyTicket(1, 2);

        assertEquals(200, result.getId());
        assertEquals(2, result.getUserId());
        assertEquals("John Doe", result.getUserName());
        assertEquals("CONF", result.getConferenceName());
        assertEquals("REG-200", result.getRegistrationNumber());
    }

    @Test
    void getMyTicketsShouldReturnMappedList() {
        Ticket ticket = buildTicket(201, PaymentStatus.PENDING);
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(ticketRepository.findByUser(user)).thenReturn(List.of(ticket));

        var result = registrationService.getMyTickets(2);

        assertEquals(1, result.size());
        assertEquals(201, result.get(0).getId());
        assertEquals("Standard", result.get(0).getTicketTypeName());
    }

    @Test
    void getAttendeesShouldReturnMappedList() {
        Ticket ticket = buildTicket(202, PaymentStatus.COMPLETED);
        when(ticketRepository.findByConferenceId(1)).thenReturn(List.of(ticket));

        var result = registrationService.getAttendees(1);

        assertEquals(1, result.size());
        assertEquals("john@conf.com", result.get(0).getUserEmail());
    }

    @Test
    void getAttendeesPageableShouldReturnPagedResponse() {
        Ticket ticket = buildTicket(203, PaymentStatus.COMPLETED);
        var page = new PageImpl<>(List.of(ticket), PageRequest.of(0, 20), 1);
        when(ticketRepository.findAttendees(eq(1), eq(PaymentStatus.COMPLETED), eq("john"), any(PageRequest.class))).thenReturn(page);

        var result = registrationService.getAttendeesPageable(1, 0, 20, " john ", "completed");

        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLast());
    }

    @Test
    void completePaymentShouldUpdateTicketAndPayment() {
        Ticket ticket = buildTicket(204, PaymentStatus.PENDING);
        Payment payment = new Payment();
        payment.setTicket(ticket);
        payment.setStatus(PaymentStatus.PENDING);

        when(ticketRepository.findById(204)).thenReturn(Optional.of(ticket));
        when(paymentRepository.findByTicket(ticket)).thenReturn(Optional.of(payment));

        registrationService.completePayment(204, "txn-ref", "provider-id");

        assertEquals(PaymentStatus.COMPLETED, ticket.getPaymentStatus());
        assertNotNull(ticket.getQrCode());
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals("txn-ref", payment.getVnpTxnRef());
        assertEquals("provider-id", payment.getProviderTransactionId());
        assertNotNull(payment.getTransactionTime());
        assertEquals(1, ticketType.getQuantitySold());

        verify(ticketRepository).save(ticket);
        verify(paymentRepository).save(payment);
        verify(ticketTypeRepository).save(ticketType);
    }

    @Test
    void failPaymentShouldUpdateTicketAndPayment() {
        Ticket ticket = buildTicket(205, PaymentStatus.PENDING);
        Payment payment = new Payment();
        payment.setTicket(ticket);
        payment.setStatus(PaymentStatus.PENDING);

        when(ticketRepository.findById(205)).thenReturn(Optional.of(ticket));
        when(paymentRepository.findByTicket(ticket)).thenReturn(Optional.of(payment));

        registrationService.failPayment(205, "txn-failed");

        assertEquals(PaymentStatus.FAILED, ticket.getPaymentStatus());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("txn-failed", payment.getVnpTxnRef());
        assertNotNull(payment.getTransactionTime());

        verify(ticketRepository).save(ticket);
        verify(paymentRepository).save(payment);
    }

    @Test
    void completePaymentAndGetShouldReturnPayment() {
        Ticket ticket = buildTicket(206, PaymentStatus.PENDING);
        Payment payment = new Payment();
        payment.setTicket(ticket);

        when(ticketRepository.findById(206)).thenReturn(Optional.of(ticket));
        when(paymentRepository.findByTicket(ticket)).thenReturn(Optional.of(payment));

        Payment result = registrationService.completePaymentAndGet(206, "txn-1", "provider-1");

        assertNotNull(result);
        assertEquals(ticket, result.getTicket());
    }

    @Test
    void failPaymentAndGetShouldReturnPayment() {
        Ticket ticket = buildTicket(207, PaymentStatus.PENDING);
        Payment payment = new Payment();
        payment.setTicket(ticket);

        when(ticketRepository.findById(207)).thenReturn(Optional.of(ticket));
        when(paymentRepository.findByTicket(ticket)).thenReturn(Optional.of(payment));

        Payment result = registrationService.failPaymentAndGet(207, "txn-2");

        assertNotNull(result);
        assertEquals(ticket, result.getTicket());
    }

    @Test
    void checkInShouldReturnSuccessWhenTicketIsPaidAndNotCheckedIn() {
        Ticket ticket = buildTicket(208, PaymentStatus.COMPLETED);
        ticket.setIsCheckedIn(false);
        when(ticketRepository.findByRegistrationNumber("CONF2026-00100")).thenReturn(Optional.of(ticket));

        var result = registrationService.checkIn("CONF2026-00100");

        assertEquals(208, result.getTicketId());
        assertTrue(result.getIsCheckedIn());
        assertEquals("Check-in successful!", result.getMessage());
        assertNotNull(ticket.getCheckInTime());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void retryPaymentShouldReturnNewPaymentUrl() {
        Ticket ticket = buildTicket(209, PaymentStatus.FAILED);
        when(userRepository.findById(2)).thenReturn(Optional.of(user));
        when(ticketRepository.findByUserAndConferenceId(user, 1)).thenReturn(Optional.of(ticket));
        when(vnPayIntegrationService.createPaymentUrl(1000000L, "127.0.0.1", 209)).thenReturn("https://retry.url");

        var result = registrationService.retryPayment(1, 2, "127.0.0.1");

        assertNotNull(result);
        assertEquals(PaymentStatus.PENDING, result.getTicket().getPaymentStatus());
        assertEquals("https://retry.url", result.getPaymentUrl());
        verify(ticketRepository).save(ticket);
    }

    private Ticket buildTicket(int id, PaymentStatus status) {
        ticketType.setPrice(BigDecimal.valueOf(1000000));

        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setUser(user);
        ticket.setConference(conference);
        ticket.setTicketType(ticketType);
        ticket.setTicketTypeName(ticketType.getName());
        ticket.setTicketTypeValue(ticketType.getName());
        ticket.setPrice(ticketType.getPrice());
        ticket.setPaymentStatus(status);
        ticket.setRegistrationNumber("REG-" + id);
        ticket.setIsCheckedIn(false);
        return ticket;
    }
}

