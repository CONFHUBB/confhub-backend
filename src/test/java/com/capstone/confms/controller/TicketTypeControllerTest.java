package com.capstone.confms.controller;

import com.capstone.confms.dto.request.TicketTypeRequest;
import com.capstone.confms.dto.response.TicketTypeResponse;
import com.capstone.confms.service.TicketTypeService;
import com.capstone.confms.utils.enums.TicketCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketTypeControllerTest {

    @Mock
    private TicketTypeService ticketTypeService;

    private TicketTypeController ticketTypeController;

    @BeforeEach
    void setUp() {
        ticketTypeController = new TicketTypeController(ticketTypeService);
    }

    @Test
    void createShouldReturnCreatedResponse() {
        TicketTypeRequest request = new TicketTypeRequest();
        request.setName("Early Bird");
        request.setPrice(BigDecimal.valueOf(1000000));
        request.setCategory(TicketCategory.STANDARD);

        TicketTypeResponse payload = TicketTypeResponse.builder().id(1).name("Early Bird").build();
        when(ticketTypeService.create(5, request)).thenReturn(payload);

        var response = ticketTypeController.create(5, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getId());
        verify(ticketTypeService).create(5, request);
    }

    @Test
    void getByConferenceShouldReturnOkResponse() {
        TicketTypeResponse payload = TicketTypeResponse.builder().id(2).name("Standard").build();
        when(ticketTypeService.getByConference(5, true)).thenReturn(List.of(payload));

        var response = ticketTypeController.getByConference(5, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Standard", response.getBody().get(0).getName());
        verify(ticketTypeService).getByConference(5, true);
    }

    @Test
    void updateShouldReturnOkResponse() {
        TicketTypeRequest request = new TicketTypeRequest();
        request.setName("Updated");
        request.setPrice(BigDecimal.valueOf(2000000));
        request.setCategory(TicketCategory.AUTHOR);

        TicketTypeResponse payload = TicketTypeResponse.builder().id(2).name("Updated").build();
        when(ticketTypeService.update(2, request)).thenReturn(payload);

        var response = ticketTypeController.update(2, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Updated", response.getBody().getName());
        verify(ticketTypeService).update(2, request);
    }

    @Test
    void deleteShouldReturnNoContent() {
        doNothing().when(ticketTypeService).delete(2);

        var response = ticketTypeController.delete(2);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(ticketTypeService).delete(2);
    }
}

