package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ConferenceActivityDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceActivity;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.SubjectArea;
import com.capstone.confhub.repository.ConferenceActivityRepository;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.SubjectAreaRepository;
import com.capstone.confhub.utils.enums.ActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConferenceActivityServiceImplTest {

    @Mock
    private ConferenceActivityRepository conferenceActivityRepository;
    @Mock
    private ConferenceRepository conferenceRepository;
    @Mock
    private ConferenceTrackRepository conferenceTrackRepository;
    @Mock
    private SubjectAreaRepository subjectAreaRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ConferenceActivityServiceImpl conferenceActivityService;

    private Conference conference;
    private ConferenceActivity activity;
    private ConferenceTrack track;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(1);
        conference.setName("ConfHub 2025");

        track = new ConferenceTrack();
        track.setId(10);
        track.setConference(conference);

        activity = new ConferenceActivity();
        activity.setId(20);
        activity.setConference(conference);
        activity.setActivityType(ActivityType.PAPER_SUBMISSION);
        activity.setName("Paper Submission");
        activity.setIsEnabled(false);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(conferenceActivityService);
    }

    @Test
    void initializeDefaultActivitiesForConferenceShouldSaveDefaults() {
        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(conferenceActivityRepository.findByConferenceId(1)).thenReturn(List.of());

        conferenceActivityService.initializeDefaultActivitiesForConference(1);

        verify(conferenceActivityRepository).saveAll(any());
    }

    @Test
    void getActivitiesByConferenceIdShouldReturnResponses() {
        when(conferenceRepository.existsById(1)).thenReturn(true);
        when(conferenceActivityRepository.findByConferenceId(1)).thenReturn(List.of(activity));

        var result = conferenceActivityService.getActivitiesByConferenceId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ActivityType.PAPER_SUBMISSION, result.get(0).getActivityType());
    }

    @Test
    void updateActivitiesShouldReturnUpdatedResponses() {
        ConferenceActivityDTO dto = new ConferenceActivityDTO();
        dto.setActivityType(ActivityType.PAPER_SUBMISSION);
        dto.setIsEnabled(true);
        dto.setName("Open Submission");
        dto.setDeadline(LocalDateTime.now().plusDays(3));

        SubjectArea subjectArea = new SubjectArea();
        subjectArea.setId(30);
        subjectArea.setTrack(track);

        when(conferenceRepository.existsById(1)).thenReturn(true);
        when(conferenceActivityRepository.findByConferenceId(1)).thenReturn(List.of(activity));
        when(conferenceTrackRepository.findByConferenceId(1)).thenReturn(List.of(track));
        when(subjectAreaRepository.findByTrackId(10)).thenReturn(List.of(subjectArea));
        when(conferenceActivityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceActivityService.updateActivities(1, List.of(dto));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(true, result.get(0).getIsEnabled());
        assertEquals("Open Submission", result.get(0).getName());
    }
}




