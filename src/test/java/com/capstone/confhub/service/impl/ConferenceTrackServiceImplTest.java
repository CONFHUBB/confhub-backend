package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ConferenceTrackDTO;
import com.capstone.confhub.dto.TrackReviewSettingDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConferenceTrackServiceImplTest {

    @Mock
    private ConferenceTrackRepository conferenceTrackRepository;
    @Mock
    private ConferenceRepository conferenceRepository;

    @InjectMocks
    private ConferenceTrackServiceImpl conferenceTrackService;

    private Conference conference;
    private ConferenceTrack track;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(1);

        TrackReviewSetting setting = new TrackReviewSetting();
        setting.setIsDoubleBlind(false);

        track = new ConferenceTrack();
        track.setId(10);
        track.setConference(conference);
        track.setName("Main Track");
        track.setDescription("Desc");
        track.setTrackReviewSetting(setting);
        setting.setTrack(track);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(conferenceTrackService);
    }

    @Test
    void createTrackShouldReturnResponse() {
        ConferenceTrackDTO dto = new ConferenceTrackDTO();
        dto.setConferenceId(1);
        dto.setName("Main Track");
        dto.setDescription("Desc");
        dto.setTrackReviewSetting(new TrackReviewSettingDTO());

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(conferenceTrackRepository.save(any(ConferenceTrack.class))).thenReturn(track);

        var result = conferenceTrackService.createTrack(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("Main Track", result.getName());
    }

    @Test
    void updateTrackShouldReturnResponse() {
        ConferenceTrackDTO dto = new ConferenceTrackDTO();
        dto.setConferenceId(1);
        dto.setName("Updated Track");
        dto.setDescription("Updated Desc");

        when(conferenceTrackRepository.findById(10)).thenReturn(Optional.of(track));
        when(conferenceTrackRepository.save(any(ConferenceTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceTrackService.updateTrack(10, dto);

        assertNotNull(result);
        assertEquals("Updated Track", result.getName());
    }

    @Test
    void getTrackByIdShouldReturnResponse() {
        when(conferenceTrackRepository.findById(10)).thenReturn(Optional.of(track));

        var result = conferenceTrackService.getTrackById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void getAllTracksShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(track), PageRequest.of(0, 20), 1);
        when(conferenceTrackRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = conferenceTrackService.getAllTracks(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getTracksByConferenceIdShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(track), PageRequest.of(0, 20), 1);
        when(conferenceRepository.existsById(1)).thenReturn(true);
        when(conferenceTrackRepository.findByConferenceId(org.mockito.ArgumentMatchers.eq(1), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = conferenceTrackService.getTracksByConferenceId(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void deleteTrackShouldDelete() {
        when(conferenceTrackRepository.existsById(10)).thenReturn(true);

        conferenceTrackService.deleteTrack(10);

        verify(conferenceTrackRepository).deleteById(10);
    }

    @Test
    void createTrackShouldThrowExceptionWhenConferenceNotFound() {
        ConferenceTrackDTO dto = new ConferenceTrackDTO();
        dto.setConferenceId(999);
        when(conferenceRepository.findById(999)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            jakarta.persistence.EntityNotFoundException.class,
            () -> conferenceTrackService.createTrack(dto)
        );
    }

    @Test
    void updateTrackShouldThrowExceptionWhenTrackNotFound() {
        ConferenceTrackDTO dto = new ConferenceTrackDTO();
        when(conferenceTrackRepository.findById(999)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            jakarta.persistence.EntityNotFoundException.class,
            () -> conferenceTrackService.updateTrack(999, dto)
        );
    }
}
