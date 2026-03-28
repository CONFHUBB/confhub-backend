package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.TicketTypeRequest;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.TicketType;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.TicketTypeRepository;
import com.capstone.confhub.utils.enums.TicketCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceImplTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private ConferenceRepository conferenceRepository;

    @InjectMocks
    private TicketTypeServiceImpl ticketTypeService;

    private Conference conference;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(1);
        conference.setName("Conf");
    }

    @Test
    void createShouldReturnResponseWithDefaults() {
        TicketTypeRequest request = new TicketTypeRequest();
        request.setName("Early Bird");
        request.setDescription("Discount ticket");
        request.setPrice(BigDecimal.valueOf(1000000));
        request.setCurrency(null);
        request.setCategory(TicketCategory.STANDARD);
        request.setIsActive(null);
        request.setMaxQuantity(100);

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(ticketTypeRepository.save(any(TicketType.class))).thenAnswer(invocation -> {
            TicketType entity = invocation.getArgument(0);
            entity.setId(10);
            entity.setQuantitySold(5);
            return entity;
        });

        var result = ticketTypeService.create(1, request);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(1, result.getConferenceId());
        assertEquals("VND", result.getCurrency());
        assertTrue(result.getIsActive());
        assertEquals(95, result.getAvailableSlots());

        ArgumentCaptor<TicketType> captor = ArgumentCaptor.forClass(TicketType.class);
        verify(ticketTypeRepository).save(captor.capture());
        assertEquals("Early Bird", captor.getValue().getName());
    }

    @Test
    void updateShouldReturnUpdatedResponse() {
        TicketType existing = new TicketType();
        existing.setId(10);
        existing.setConference(conference);
        existing.setQuantitySold(0);

        TicketTypeRequest request = new TicketTypeRequest();
        request.setName("Standard");
        request.setDescription("Standard ticket");
        request.setPrice(BigDecimal.valueOf(1500000));
        request.setCurrency("USD");
        request.setCategory(TicketCategory.AUTHOR);
        request.setIsActive(false);
        request.setMaxQuantity(50);

        when(ticketTypeRepository.findById(10)).thenReturn(Optional.of(existing));
        when(ticketTypeRepository.save(any(TicketType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = ticketTypeService.update(10, request);

        assertNotNull(result);
        assertEquals("Standard", result.getName());
        assertEquals("USD", result.getCurrency());
        assertFalse(result.getIsActive());
        verify(ticketTypeRepository).findById(10);
        verify(ticketTypeRepository).save(existing);
    }

    @Test
    void getByConferenceShouldUseActiveFilterWhenRequested() {
        TicketType ticketType = new TicketType();
        ticketType.setId(1);
        ticketType.setConference(conference);
        ticketType.setName("Early Bird");
        ticketType.setPrice(BigDecimal.ONE);
        ticketType.setCurrency("VND");
        ticketType.setCategory(TicketCategory.STANDARD);
        ticketType.setIsActive(true);
        ticketType.setQuantitySold(1);
        ticketType.setMaxQuantity(10);

        when(ticketTypeRepository.findByConferenceIdAndIsActiveTrue(1)).thenReturn(List.of(ticketType));

        var result = ticketTypeService.getByConference(1, true);

        assertEquals(1, result.size());
        assertEquals("Early Bird", result.get(0).getName());
        verify(ticketTypeRepository).findByConferenceIdAndIsActiveTrue(1);
    }

    @Test
    void getByConferenceShouldReturnAllWhenActiveOnlyIsFalse() {
        TicketType ticketType = new TicketType();
        ticketType.setId(2);
        ticketType.setConference(conference);
        ticketType.setName("Standard");
        ticketType.setPrice(BigDecimal.TEN);
        ticketType.setCurrency("VND");
        ticketType.setCategory(TicketCategory.STANDARD);
        ticketType.setIsActive(false);
        ticketType.setQuantitySold(0);

        when(ticketTypeRepository.findByConferenceId(1)).thenReturn(List.of(ticketType));

        var result = ticketTypeService.getByConference(1, false);

        assertEquals(1, result.size());
        assertEquals("Standard", result.get(0).getName());
        verify(ticketTypeRepository).findByConferenceId(1);
    }

    @Test
    void deleteShouldCallRepository() {
        when(ticketTypeRepository.existsById(5)).thenReturn(true);

        ticketTypeService.delete(5);

        verify(ticketTypeRepository).existsById(5);
        verify(ticketTypeRepository).deleteById(5);
    }

    @Test
    void mapToResponseShouldComputeSoldOutAndDeadlineFlags() {
        TicketType ticketType = new TicketType();
        ticketType.setId(7);
        ticketType.setConference(conference);
        ticketType.setName("Late");
        ticketType.setDescription("Late registration");
        ticketType.setPrice(BigDecimal.valueOf(2000000));
        ticketType.setCurrency("VND");
        ticketType.setCategory(TicketCategory.STANDARD);
        ticketType.setIsActive(true);
        ticketType.setMaxQuantity(10);
        ticketType.setQuantitySold(10);
        ticketType.setDeadline(LocalDateTime.now().minusDays(1));

        var result = ticketTypeService.mapToResponse(ticketType);

        assertEquals(0, result.getAvailableSlots());
        assertTrue(result.getIsSoldOut());
        assertTrue(result.getIsDeadlinePassed());
    }
}

