package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.TrackReviewSettingDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TrackReviewSettingServiceImplTest {

    private static final int CONFERENCE_ID = 1;
    private static final int SOURCE_TRACK_ID = 10;
    private static final int TARGET_TRACK_ID = 11;

    @Mock
    private TrackReviewSettingRepository trackReviewSettingRepository;
    @Mock
    private ConferenceTrackRepository conferenceTrackRepository;

    @InjectMocks
    private TrackReviewSettingServiceImpl trackReviewSettingService;

    private Conference conference;
    private ConferenceTrack sourceTrack;
    private ConferenceTrack targetTrack;
    private TrackReviewSetting setting;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(CONFERENCE_ID);

        sourceTrack = new ConferenceTrack();
        sourceTrack.setId(SOURCE_TRACK_ID);
        sourceTrack.setConference(conference);

        targetTrack = new ConferenceTrack();
        targetTrack.setId(TARGET_TRACK_ID);
        targetTrack.setConference(conference);

        setting = new TrackReviewSetting();
        setting.setTrack(sourceTrack);
        setting.setIsDoubleBlind(true);
        setting.setReviewerInstructions("Review carefully");
        setting.setAllowReviewerQuota(true);
        setting.setReviewerInviteExpirationDays(7);
        setting.setAllowOthersReviewAccessAfterSubmit(true);
        setting.setAllowReviewUpdateDuringDiscussion(true);
        sourceTrack.setTrackReviewSetting(setting);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(trackReviewSettingService);
    }

    @Test
    void getReviewSettingsByTrackIdShouldReturnResponse() {
        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.of(sourceTrack));

        var result = trackReviewSettingService.getReviewSettingsByTrackId(SOURCE_TRACK_ID);

        assertNotNull(result);
        assertEquals(true, result.getIsDoubleBlind());
        assertEquals("Review carefully", result.getReviewerInstructions());
    }

    @Test
    void getReviewSettingsByTrackIdShouldReturnDefaultDtoWhenSettingMissing() {
        sourceTrack.setTrackReviewSetting(null);
        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.of(sourceTrack));

        var result = trackReviewSettingService.getReviewSettingsByTrackId(SOURCE_TRACK_ID);

        assertNotNull(result);
        assertEquals(true, result.getIsDoubleBlind());
        assertEquals(false, result.getAllowReviewerQuota());
        assertEquals(7, result.getReviewerInviteExpirationDays());
    }

    @Test
    void getReviewSettingsByTrackIdShouldThrowWhenTrackNotFound() {
        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> trackReviewSettingService.getReviewSettingsByTrackId(SOURCE_TRACK_ID));
    }

    @Test
    void updateReviewSettingsShouldReturnResponse() {
        TrackReviewSettingDTO dto = new TrackReviewSettingDTO();
        dto.setIsDoubleBlind(false);
        dto.setReviewerInstructions("Updated instructions");
        dto.setAllowReviewerQuota(false);
        dto.setReviewerInviteExpirationDays(5);
        dto.setAllowOthersReviewAccessAfterSubmit(false);
        dto.setAllowReviewUpdateDuringDiscussion(false);

        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.of(sourceTrack));
        when(trackReviewSettingRepository.save(any(TrackReviewSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceTrackRepository.save(any(ConferenceTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = trackReviewSettingService.updateReviewSettings(SOURCE_TRACK_ID, dto);

        assertNotNull(result);
        assertEquals(false, result.getIsDoubleBlind());
        assertEquals("Updated instructions", result.getReviewerInstructions());
    }

    @Test
    void updateReviewSettingsShouldCreateSettingWhenTrackHasNone() {
        sourceTrack.setTrackReviewSetting(null);
        TrackReviewSettingDTO dto = new TrackReviewSettingDTO();
        dto.setReviewerInstructions("Initial instructions");

        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.of(sourceTrack));
        when(trackReviewSettingRepository.save(any(TrackReviewSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceTrackRepository.save(any(ConferenceTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = trackReviewSettingService.updateReviewSettings(SOURCE_TRACK_ID, dto);

        assertNotNull(result);
        assertEquals("Initial instructions", result.getReviewerInstructions());
        assertNotNull(sourceTrack.getTrackReviewSetting());
    }

    @Test
    void updateReviewSettingsShouldKeepExistingValuesWhenDtoFieldsNull() {
        TrackReviewSettingDTO dto = new TrackReviewSettingDTO();
        dto.setReviewerInstructions(null);
        dto.setAllowReviewerQuota(null);
        dto.setReviewerInviteExpirationDays(null);

        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.of(sourceTrack));
        when(trackReviewSettingRepository.save(any(TrackReviewSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceTrackRepository.save(any(ConferenceTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = trackReviewSettingService.updateReviewSettings(SOURCE_TRACK_ID, dto);

        assertNotNull(result);
        assertEquals("Review carefully", result.getReviewerInstructions());
        assertEquals(true, result.getAllowReviewerQuota());
        assertEquals(7, result.getReviewerInviteExpirationDays());
    }

    @Test
    void updateReviewSettingsShouldThrowWhenTrackNotFound() {
        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> trackReviewSettingService.updateReviewSettings(SOURCE_TRACK_ID, new TrackReviewSettingDTO()));
    }

    @Test
    void copyReviewSettingsShouldCopyValuesToTargetTrack() {
        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.of(sourceTrack));
        when(conferenceTrackRepository.findById(TARGET_TRACK_ID)).thenReturn(Optional.of(targetTrack));
        when(trackReviewSettingRepository.save(any(TrackReviewSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceTrackRepository.save(any(ConferenceTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        trackReviewSettingService.copyReviewSettings(SOURCE_TRACK_ID, TARGET_TRACK_ID);

        assertNotNull(targetTrack.getTrackReviewSetting());
        assertEquals(true, targetTrack.getTrackReviewSetting().getIsDoubleBlind());
        assertEquals("Review carefully", targetTrack.getTrackReviewSetting().getReviewerInstructions());
        verify(trackReviewSettingRepository).save(any(TrackReviewSetting.class));
    }

    @Test
    void copyReviewSettingsShouldThrowWhenSourceEqualsTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> trackReviewSettingService.copyReviewSettings(SOURCE_TRACK_ID, SOURCE_TRACK_ID));
    }

    @Test
    void copyReviewSettingsShouldThrowWhenSourceTrackNotFound() {
        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> trackReviewSettingService.copyReviewSettings(SOURCE_TRACK_ID, TARGET_TRACK_ID));
    }

    @Test
    void copyReviewSettingsShouldThrowWhenTargetTrackNotFound() {
        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.of(sourceTrack));
        when(conferenceTrackRepository.findById(TARGET_TRACK_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> trackReviewSettingService.copyReviewSettings(SOURCE_TRACK_ID, TARGET_TRACK_ID));
    }

    @Test
    void copyReviewSettingsShouldThrowWhenTracksBelongToDifferentConferences() {
        Conference otherConference = new Conference();
        otherConference.setId(99);
        targetTrack.setConference(otherConference);

        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.of(sourceTrack));
        when(conferenceTrackRepository.findById(TARGET_TRACK_ID)).thenReturn(Optional.of(targetTrack));

        assertThrows(IllegalArgumentException.class,
                () -> trackReviewSettingService.copyReviewSettings(SOURCE_TRACK_ID, TARGET_TRACK_ID));
    }

    @Test
    void copyReviewSettingsShouldResetTargetToDefaultsWhenSourceSettingMissing() {
        sourceTrack.setTrackReviewSetting(null);
        TrackReviewSetting targetSetting = new TrackReviewSetting();
        targetSetting.setTrack(targetTrack);
        targetSetting.setIsDoubleBlind(true);
        targetSetting.setReviewerInstructions("Old");
        targetTrack.setTrackReviewSetting(targetSetting);

        when(conferenceTrackRepository.findById(SOURCE_TRACK_ID)).thenReturn(Optional.of(sourceTrack));
        when(conferenceTrackRepository.findById(TARGET_TRACK_ID)).thenReturn(Optional.of(targetTrack));
        when(trackReviewSettingRepository.save(any(TrackReviewSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceTrackRepository.save(any(ConferenceTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        trackReviewSettingService.copyReviewSettings(SOURCE_TRACK_ID, TARGET_TRACK_ID);

        assertNotNull(targetTrack.getTrackReviewSetting());
        assertEquals(false, targetTrack.getTrackReviewSetting().getIsDoubleBlind());
        assertNull(targetTrack.getTrackReviewSetting().getReviewerInstructions());
        assertEquals(false, targetTrack.getTrackReviewSetting().getAllowReviewerQuota());
        assertEquals(true, targetTrack.getTrackReviewSetting().getEnableDomainConflict());
        assertTrue(targetTrack.getTrackReviewSetting().getEnableAuthorSelfConflict());
    }
}




