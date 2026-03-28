package com.capstone.confhub.controller;

import com.capstone.confhub.entity.PaymentHistory;
import com.capstone.confhub.entity.Ticket;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.PaymentHistoryRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryControllerTest {

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;

    private PaymentHistoryController paymentHistoryController;

    @BeforeEach
    void setUp() {
        paymentHistoryController = new PaymentHistoryController(paymentHistoryRepository, ticketRepository, userRepository);
    }

    @Test
    void getPaymentHistoryShouldReturnMappedHistory() {
        Ticket ticket = new Ticket();
        ticket.setId(10);
        ticket.setRegistrationNumber("REG-10");

        PaymentHistory entry = new PaymentHistory();
        entry.setId(100L);
        entry.setTicket(ticket);
        entry.setOutcome("PAID");
        entry.setRecordedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(ticketRepository.findById(10)).thenReturn(Optional.of(ticket));
        when(paymentHistoryRepository.findByTicketOrderByRecordedAtDesc(ticket)).thenReturn(List.of(entry));

        var response = paymentHistoryController.getPaymentHistory(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(10, response.getBody().get(0).getTicketId());
        assertEquals("REG-10", response.getBody().get(0).getRegistrationNumber());
        assertEquals("PAID", response.getBody().get(0).getOutcome());

        verify(ticketRepository).findById(10);
        verify(paymentHistoryRepository).findByTicketOrderByRecordedAtDesc(ticket);
    }

    @Test
    void getConferencePaymentHistoryShouldReturnSortedHistory() {
        Ticket ticket1 = new Ticket();
        ticket1.setId(1);
        ticket1.setRegistrationNumber("REG-1");

        Ticket ticket2 = new Ticket();
        ticket2.setId(2);
        ticket2.setRegistrationNumber("REG-2");

        PaymentHistory older = new PaymentHistory();
        older.setId(1L);
        older.setTicket(ticket1);
        older.setRecordedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        older.setOutcome("FAILED");

        PaymentHistory newer = new PaymentHistory();
        newer.setId(2L);
        newer.setTicket(ticket2);
        newer.setRecordedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        newer.setOutcome("PAID");

        when(ticketRepository.findByConferenceId(99)).thenReturn(List.of(ticket1, ticket2));
        when(paymentHistoryRepository.findByTicketOrderByRecordedAtDesc(ticket1)).thenReturn(List.of(older));
        when(paymentHistoryRepository.findByTicketOrderByRecordedAtDesc(ticket2)).thenReturn(List.of(newer));

        var response = paymentHistoryController.getConferencePaymentHistory(99);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(2, response.getBody().get(0).getTicketId());
        assertEquals(1, response.getBody().get(1).getTicketId());

        verify(ticketRepository).findByConferenceId(99);
    }

    @Test
    void getMyPaymentHistoryShouldReturnMappedHistory() {
        User user = new User();
        user.setId(7);

        Ticket ticket = new Ticket();
        ticket.setId(20);
        ticket.setRegistrationNumber("REG-20");

        PaymentHistory entry = new PaymentHistory();
        entry.setId(200L);
        entry.setTicket(ticket);
        entry.setOutcome("PAID");
        entry.setRecordedAt(LocalDateTime.of(2026, 2, 1, 10, 0));

        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(ticketRepository.findByUser(user)).thenReturn(List.of(ticket));
        when(paymentHistoryRepository.findByTicketInOrderByRecordedAtDesc(List.of(ticket))).thenReturn(List.of(entry));

        var response = paymentHistoryController.getMyPaymentHistory(7);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(20, response.getBody().get(0).getTicketId());
        assertEquals("PAID", response.getBody().get(0).getOutcome());

        verify(userRepository).findById(7);
        verify(ticketRepository).findByUser(user);
        verify(paymentHistoryRepository).findByTicketInOrderByRecordedAtDesc(List.of(ticket));
    }
}

