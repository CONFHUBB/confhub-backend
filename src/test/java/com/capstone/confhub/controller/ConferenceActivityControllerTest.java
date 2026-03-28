package com.capstone.confhub.controller;

import com.capstone.confhub.dto.ConferenceActivityDTO;
import com.capstone.confhub.service.ConferenceActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConferenceActivityControllerTest {

    @Mock
    private ConferenceActivityService conferenceActivityService;

    private ConferenceActivityController conferenceActivityController;

    @BeforeEach
    void setUp() {
        conferenceActivityController = new ConferenceActivityController(conferenceActivityService);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(conferenceActivityController);
    }

    @Test
    void getActivitiesShouldReturnOk() {
        List<ConferenceActivityDTO> payload = List.of(new ConferenceActivityDTO());
        when(conferenceActivityService.getActivitiesByConferenceId(1)).thenReturn(payload);

        var result = conferenceActivityController.getActivities(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void updateActivitiesShouldReturnOk() {
        List<ConferenceActivityDTO> request = List.of(new ConferenceActivityDTO());
        List<ConferenceActivityDTO> payload = List.of(new ConferenceActivityDTO());
        when(conferenceActivityService.updateActivities(1, request)).thenReturn(payload);

        var result = conferenceActivityController.updateActivities(1, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }
}
