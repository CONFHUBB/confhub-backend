package com.capstone.confhub.controller;

import com.capstone.confhub.dto.ConferenceTrackDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.ConferenceTrackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConferenceTrackControllerTest {

    @Mock
    private ConferenceTrackService conferenceTrackService;

    private ConferenceTrackController conferenceTrackController;

    @BeforeEach
    void setUp() {
        conferenceTrackController = new ConferenceTrackController(conferenceTrackService);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(conferenceTrackController);
    }

    @Test
    void createTrackShouldReturnCreated() {
        ConferenceTrackDTO dto = new ConferenceTrackDTO();
        var payload = mock(com.capstone.confhub.dto.response.ConferenceTrackResponseDTO.class);
        when(conferenceTrackService.createTrack(dto)).thenReturn(payload);

        var result = conferenceTrackController.createTrack(dto);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void getAllConferenceTrackShouldReturnOk() {
        PagedResponse<?> payload = PagedResponse.builder().content(Collections.emptyList()).page(0).size(20).totalElements(0).totalPages(0).last(true).build();
        when(conferenceTrackService.getAllTracks(0, 20)).thenReturn((PagedResponse) payload);

        var result = conferenceTrackController.getAllConferenceTrack(0, 20);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getAllConferenceTrackShouldThrowOnInvalidPagination() {
        assertThrows(BadRequestException.class, () -> conferenceTrackController.getAllConferenceTrack(-1, 20));
        assertThrows(BadRequestException.class, () -> conferenceTrackController.getAllConferenceTrack(0, 0));
        assertThrows(BadRequestException.class, () -> conferenceTrackController.getAllConferenceTrack(0, 101));
    }

    @Test
    void getTrackByIdShouldReturnOk() {
        var payload = mock(com.capstone.confhub.dto.response.ConferenceTrackResponseDTO.class);
        when(conferenceTrackService.getTrackById(1)).thenReturn(payload);

        var result = conferenceTrackController.getTrackById(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void updateTrackShouldReturnOk() {
        ConferenceTrackDTO dto = new ConferenceTrackDTO();
        var payload = mock(com.capstone.confhub.dto.response.ConferenceTrackResponseDTO.class);
        when(conferenceTrackService.updateTrack(1, dto)).thenReturn(payload);

        var result = conferenceTrackController.updateTrack(1, dto);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void getTracksByConferenceIdShouldReturnOk() {
        PagedResponse<?> payload = PagedResponse.builder().content(Collections.emptyList()).page(0).size(20).totalElements(0).totalPages(0).last(true).build();
        when(conferenceTrackService.getTracksByConferenceId(1, 0, 20)).thenReturn((PagedResponse) payload);

        var result = conferenceTrackController.getTracksByConferenceId(1, 0, 20);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getTracksByConferenceIdShouldThrowOnInvalidPagination() {
        assertThrows(BadRequestException.class, () -> conferenceTrackController.getTracksByConferenceId(1, -1, 20));
        assertThrows(BadRequestException.class, () -> conferenceTrackController.getTracksByConferenceId(1, 0, 0));
        assertThrows(BadRequestException.class, () -> conferenceTrackController.getTracksByConferenceId(1, 0, 101));
    }

    @Test
    void deleteTrackShouldReturnNoContent() {
        doNothing().when(conferenceTrackService).deleteTrack(1);

        var result = conferenceTrackController.deleteTrack(1);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
