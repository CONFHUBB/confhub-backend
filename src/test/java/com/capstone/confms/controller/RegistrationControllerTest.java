package com.capstone.confms.controller;

import com.capstone.confms.dto.request.RegistrationRequest;
import com.capstone.confms.dto.response.CheckInResponse;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.dto.response.RegistrationResponse;
import com.capstone.confms.dto.response.TicketResponse;
import com.capstone.confms.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private RegistrationService registrationService;

    private RegistrationController registrationController;

    @BeforeEach
    void setUp() {
        registrationController = new RegistrationController(registrationService);
    }

    @Test
    void registerShouldReturnCreatedResponse() {
        RegistrationRequest request = new RegistrationRequest();
        request.setTicketTypeId(3);

        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(httpRequest.getHeader("X-FORWARDED-FOR")).thenReturn("10.0.0.1");

        RegistrationResponse payload = RegistrationResponse.builder()
                .ticket(TicketResponse.builder().id(1).build())
                .paymentUrl("https://pay.url")
                .build();

        when(registrationService.register(1, 2, request, "10.0.0.1")).thenReturn(payload);

        var response = registrationController.register(1, 2, request, httpRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(payload, response.getBody());
        verify(registrationService).register(1, 2, request, "10.0.0.1");
    }

    @Test
    void getMyTicketShouldReturnOkResponse() {
        TicketResponse payload = TicketResponse.builder().id(10).ticketTypeName("Standard").build();
        when(registrationService.getMyTicket(1, 2)).thenReturn(payload);

        var response = registrationController.getMyTicket(1, 2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TicketResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(10, body.getId());
        verify(registrationService).getMyTicket(1, 2);
    }

    @Test
    void getMyTicketsShouldReturnOkResponse() {
        TicketResponse payload = TicketResponse.builder().id(10).build();
        when(registrationService.getMyTickets(2)).thenReturn(List.of(payload));

        var response = registrationController.getMyTickets(2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(registrationService).getMyTickets(2);
    }

    @Test
    void getAttendeesShouldReturnOkResponse() {
        TicketResponse payload = TicketResponse.builder().id(10).build();
        PagedResponse<TicketResponse> paged = PagedResponse.<TicketResponse>builder()
                .content(List.of(payload))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(registrationService.getAttendeesPageable(1, 0, 20, "john", "COMPLETED")).thenReturn(paged);

        var response = registrationController.getAttendees(1, 0, 20, "john", "COMPLETED");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PagedResponse<TicketResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.getContent().size());
        verify(registrationService).getAttendeesPageable(1, 0, 20, "john", "COMPLETED");
    }

    @Test
    void checkInShouldReturnOkResponse() {
        CheckInResponse payload = CheckInResponse.builder().ticketId(10).message("Check-in successful!").build();
        when(registrationService.checkIn("CONF2026-00001")).thenReturn(payload);

        var response = registrationController.checkIn("CONF2026-00001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CheckInResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Check-in successful!", body.getMessage());
        verify(registrationService).checkIn("CONF2026-00001");
    }

    @Test
    void retryPaymentShouldReturnOkResponse() {
        HttpServletRequest httpRequest = org.mockito.Mockito.mock(HttpServletRequest.class);
        when(httpRequest.getHeader("X-FORWARDED-FOR")).thenReturn("10.0.0.2");

        RegistrationResponse payload = RegistrationResponse.builder()
                .ticket(TicketResponse.builder().id(20).build())
                .paymentUrl("https://retry.url")
                .build();

        when(registrationService.retryPayment(1, 2, "10.0.0.2")).thenReturn(payload);

        var response = registrationController.retryPayment(1, 2, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        RegistrationResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("https://retry.url", body.getPaymentUrl());
        verify(registrationService).retryPayment(1, 2, "10.0.0.2");
    }
}

