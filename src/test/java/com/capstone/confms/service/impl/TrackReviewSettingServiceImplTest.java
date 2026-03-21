package com.capstone.confms.service.impl;

import com.capstone.confms.dto.TrackReviewSettingDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.TrackReviewSetting;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.TrackReviewSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TrackReviewSettingServiceImplTest {

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
        conference.setId(1);

        sourceTrack = new ConferenceTrack();
        sourceTrack.setId(10);
        sourceTrack.setConference(conference);

        targetTrack = new ConferenceTrack();
        targetTrack.setId(11);
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
        when(conferenceTrackRepository.findById(10)).thenReturn(Optional.of(sourceTrack));

        var result = trackReviewSettingService.getReviewSettingsByTrackId(10);

        assertNotNull(result);
        assertEquals(true, result.getIsDoubleBlind());
        assertEquals("Review carefully", result.getReviewerInstructions());
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

        when(conferenceTrackRepository.findById(10)).thenReturn(Optional.of(sourceTrack));
        when(trackReviewSettingRepository.save(any(TrackReviewSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceTrackRepository.save(any(ConferenceTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = trackReviewSettingService.updateReviewSettings(10, dto);

        assertNotNull(result);
        assertEquals(false, result.getIsDoubleBlind());
        assertEquals("Updated instructions", result.getReviewerInstructions());
    }

    @Test
    void copyReviewSettingsShouldCopyValuesToTargetTrack() {
        when(conferenceTrackRepository.findById(10)).thenReturn(Optional.of(sourceTrack));
        when(conferenceTrackRepository.findById(11)).thenReturn(Optional.of(targetTrack));
        when(trackReviewSettingRepository.save(any(TrackReviewSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceTrackRepository.save(any(ConferenceTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        trackReviewSettingService.copyReviewSettings(10, 11);

        assertNotNull(targetTrack.getTrackReviewSetting());
        assertEquals(true, targetTrack.getTrackReviewSetting().getIsDoubleBlind());
        assertEquals("Review carefully", targetTrack.getTrackReviewSetting().getReviewerInstructions());
        verify(trackReviewSettingRepository).save(any(TrackReviewSetting.class));
    }
}




