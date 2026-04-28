package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ActivityAuditLogDTO;
import com.capstone.confhub.dto.ConferenceActivityDTO;
import com.capstone.confhub.entity.ActivityAuditLog;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceActivity;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.Review;
import com.capstone.confhub.entity.SubjectArea;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.repository.ActivityAuditLogRepository;
import com.capstone.confhub.repository.ConferenceActivityRepository;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.SubjectAreaRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.utils.enums.ActivityType;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConferenceActivityServiceImplTest {

    @Mock
    private ConferenceActivityRepository activityRepository;
    @Mock
    private ConferenceRepository conferenceRepository;
    @Mock
    private ConferenceTrackRepository conferenceTrackRepository;
    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;
    @Mock
    private SubjectAreaRepository subjectAreaRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ActivityAuditLogRepository auditLogRepository;
    @Mock
    private ActivityNotificationSender notificationSender;
    @Mock
    private TrackReviewSettingRepository trackReviewSettingRepository;

    @InjectMocks
    private ConferenceActivityServiceImpl conferenceActivityService;

    private Conference conference;
    private ConferenceTrack track;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(1);
        conference.setName("ConfMS 2026");

        track = new ConferenceTrack();
        track.setId(10);
        track.setConference(conference);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("chair@test.com", null));
    }

    @Test
    void shouldCreateService() {
        assertNotNull(conferenceActivityService);
    }

    @Test
    void initializeDefaultActivitiesShouldThrowWhenConferenceMissing() {
        when(conferenceRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> conferenceActivityService.initializeDefaultActivitiesForConference(1));
    }

    @Test
    void initializeDefaultActivitiesShouldSaveMissingActivities() {
        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(List.of());

        conferenceActivityService.initializeDefaultActivitiesForConference(1);

        ArgumentCaptor<List<ConferenceActivity>> captor = ArgumentCaptor.forClass(List.class);
        verify(activityRepository).saveAll(captor.capture());
        assertEquals(ActivityType.values().length, captor.getValue().size());
        assertTrue(captor.getValue().stream().allMatch(a -> Boolean.FALSE.equals(a.getIsEnabled())));
    }

    @Test
    void initializeDefaultActivitiesShouldSkipSaveWhenAllExist() {
        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(allActivities());

        conferenceActivityService.initializeDefaultActivitiesForConference(1);

        verify(activityRepository, never()).saveAll(any());
    }

    @Test
    void getActivitiesShouldThrowWhenConferenceMissing() {
        when(conferenceRepository.existsById(1)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> conferenceActivityService.getActivitiesByConferenceId(1));
    }

    @Test
    void getActivitiesShouldReturnMappedDtos() {
        List<ConferenceActivity> activities = allActivities();
        when(conferenceRepository.existsById(1)).thenReturn(true);
        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(activities, activities);

        List<ConferenceActivityDTO> result = conferenceActivityService.getActivitiesByConferenceId(1);

        assertEquals(ActivityType.values().length, result.size());
        assertEquals(ActivityType.PAPER_SUBMISSION, result.get(0).getActivityType());
    }

    @Test
    void getActivitiesShouldAutoDisableExpiredEnabledActivity() {
        ConferenceActivity expired = activity(ActivityType.PAPER_SUBMISSION, true, LocalDateTime.now().minusHours(1));
        List<ConferenceActivity> list = List.of(expired);

        when(conferenceRepository.existsById(1)).thenReturn(true);
        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(list, list);

        List<ConferenceActivityDTO> result = conferenceActivityService.getActivitiesByConferenceId(1);

        assertFalse(result.get(0).getIsEnabled());
        verify(activityRepository).save(expired);
    }

    @Test
    void getActivitiesShouldNotDisableFutureEnabledActivity() {
        ConferenceActivity active = activity(ActivityType.PAPER_SUBMISSION, true, LocalDateTime.now().plusDays(1));
        List<ConferenceActivity> list = List.of(active);

        when(conferenceRepository.existsById(1)).thenReturn(true);
        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(list, list);

        conferenceActivityService.getActivitiesByConferenceId(1);

        verify(activityRepository, never()).save(any());
    }

    @Test
    void updateActivitiesShouldThrowWhenConferenceMissing() {
        when(conferenceRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> conferenceActivityService.updateActivities(1, List.of(new ConferenceActivityDTO())));
    }

    @Test
    void updateActivitiesShouldThrowWhenEnablingPaperSubmissionWithoutTracks() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO dto = dto(ActivityType.PAPER_SUBMISSION, true, LocalDateTime.now().plusDays(3), "Open");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(conferenceTrackRepository.findByConferenceId(1)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> conferenceActivityService.updateActivities(1, List.of(dto)));
    }

    @Test
    void updateActivitiesShouldThrowWhenEnablingPaperSubmissionWithoutSubjectAreas() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO dto = dto(ActivityType.PAPER_SUBMISSION, true, LocalDateTime.now().plusDays(3), "Open");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(conferenceTrackRepository.findByConferenceId(1)).thenReturn(List.of(track));
        when(subjectAreaRepository.findByTrackId(10)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> conferenceActivityService.updateActivities(1, List.of(dto)));
    }

    @Test
    void updateActivitiesShouldThrowWhenEnablingReviewerBiddingWithoutPapers() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO dto = dto(ActivityType.REVIEWER_BIDDING, true, LocalDateTime.now().plusDays(3), "Bid");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(paperRepository.findByTrack_Conference_Id(1)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> conferenceActivityService.updateActivities(1, List.of(dto)));
    }

    @Test
    void updateActivitiesShouldThrowWhenEnablingReviewSubmissionWithoutReviews() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO dto = dto(ActivityType.REVIEW_SUBMISSION, true, LocalDateTime.now().plusDays(3), "Review");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(reviewRepository.findByPaper_Track_Conference_Id(1)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> conferenceActivityService.updateActivities(1, List.of(dto)));
    }

    @Test
    void updateActivitiesShouldThrowWhenEnablingReviewDiscussionWithoutReviews() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO dto = dto(ActivityType.REVIEW_DISCUSSION, true, LocalDateTime.now().plusDays(3), "Discuss");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(reviewRepository.findByPaper_Track_Conference_Id(1)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> conferenceActivityService.updateActivities(1, List.of(dto)));
    }

    @Test
    void updateActivitiesShouldAutoEnableDiscussionForAllPapersWhenTrackSettingEnabled() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO dto = dto(ActivityType.REVIEW_DISCUSSION, true, LocalDateTime.now().plusDays(3), "Discuss");

        Paper p1 = new Paper();
        p1.setId(100);
        p1.setTrack(track);
        p1.setIsDiscussionEnabled(false);

        TrackReviewSetting setting = new TrackReviewSetting();
        setting.setEnableAllPapersForDiscussion(true);

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(reviewRepository.findByPaper_Track_Conference_Id(1)).thenReturn(List.of(new Review()));
        when(conferenceTrackRepository.findByConferenceId(1)).thenReturn(List.of(track));
        when(trackReviewSettingRepository.findByTrackId(10)).thenReturn(Optional.of(setting));
        when(paperRepository.findByTrack_Conference_Id(1)).thenReturn(List.of(p1));
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ConferenceActivityDTO> result = conferenceActivityService.updateActivities(1, List.of(dto));

        assertTrue(result.stream().anyMatch(a -> a.getActivityType() == ActivityType.REVIEW_DISCUSSION && Boolean.TRUE.equals(a.getIsEnabled())));
        assertTrue(Boolean.TRUE.equals(p1.getIsDiscussionEnabled()));
        verify(paperRepository).saveAll(List.of(p1));
        verify(auditLogRepository).saveAll(any());
        verify(notificationSender).sendNotifications(eq(conference), any());
    }

    @Test
    void updateActivitiesShouldNotAutoEnableDiscussionWhenTrackSettingDisabled() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO dto = dto(ActivityType.REVIEW_DISCUSSION, true, LocalDateTime.now().plusDays(3), "Discuss");

        TrackReviewSetting setting = new TrackReviewSetting();
        setting.setEnableAllPapersForDiscussion(false);

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(reviewRepository.findByPaper_Track_Conference_Id(1)).thenReturn(List.of(new Review()));
        when(conferenceTrackRepository.findByConferenceId(1)).thenReturn(List.of(track));
        when(trackReviewSettingRepository.findByTrackId(10)).thenReturn(Optional.of(setting));
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceActivityService.updateActivities(1, List.of(dto));

        verify(paperRepository, never()).saveAll(any());
    }

    @Test
    void updateActivitiesShouldDisableOtherActivitiesWhenOneGetsEnabled() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivity paperSubmission = find(existing, ActivityType.PAPER_SUBMISSION);
        ConferenceActivity reviewerBidding = find(existing, ActivityType.REVIEWER_BIDDING);
        reviewerBidding.setIsEnabled(true);

        ConferenceActivityDTO dto = dto(ActivityType.PAPER_SUBMISSION, true, LocalDateTime.now().plusDays(3), "Open");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(conferenceTrackRepository.findByConferenceId(1)).thenReturn(List.of(track));
        when(subjectAreaRepository.findByTrackId(10)).thenReturn(List.of(new SubjectArea()));
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceActivityService.updateActivities(1, List.of(dto));

        assertTrue(Boolean.TRUE.equals(paperSubmission.getIsEnabled()));
        assertFalse(Boolean.TRUE.equals(reviewerBidding.getIsEnabled()));
    }

    @Test
    void updateActivitiesShouldThrowWhenDeadlineOrderInvalid() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO d1 = dto(ActivityType.PAPER_SUBMISSION, false, LocalDateTime.now().plusDays(5), null);
        ConferenceActivityDTO d2 = dto(ActivityType.REVIEWER_BIDDING, false, LocalDateTime.now().plusDays(4), null);

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);

        assertThrows(BadRequestException.class, () -> conferenceActivityService.updateActivities(1, List.of(d1, d2)));
    }

    @Test
    void updateActivitiesShouldThrowWhenChangingDeadlineToPast() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO dto = dto(ActivityType.PAPER_SUBMISSION, false, LocalDateTime.now().minusHours(2), null);

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);

        assertThrows(BadRequestException.class, () -> conferenceActivityService.updateActivities(1, List.of(dto)));
    }

    @Test
    void updateActivitiesShouldAllowUnchangedPastDeadline() {
        List<ConferenceActivity> existing = allActivities();
        LocalDateTime oldPast = LocalDateTime.now().minusDays(1);
        find(existing, ActivityType.PAPER_SUBMISSION).setDeadline(oldPast);

        ConferenceActivityDTO dto = dto(ActivityType.PAPER_SUBMISSION, false, oldPast, "same");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ConferenceActivityDTO> result = conferenceActivityService.updateActivities(1, List.of(dto));

        assertEquals(ActivityType.values().length, result.size());
    }

    @Test
    void updateActivitiesShouldNotWriteAuditWhenNoChanges() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivity target = find(existing, ActivityType.PAPER_SUBMISSION);
        target.setIsEnabled(false);
        target.setDeadline(null);

        ConferenceActivityDTO dto = dto(ActivityType.PAPER_SUBMISSION, false, null, "");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceActivityService.updateActivities(1, List.of(dto));

        verify(auditLogRepository, never()).saveAll(any());
        verify(notificationSender, never()).sendNotifications(any(), any());
    }

    @Test
    void updateActivitiesShouldWriteAuditAndNotifyWhenChanged() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivity target = find(existing, ActivityType.PAPER_SUBMISSION);
        target.setIsEnabled(false);

        ConferenceActivityDTO dto = dto(ActivityType.PAPER_SUBMISSION, true, LocalDateTime.now().plusDays(2), "Open");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(conferenceTrackRepository.findByConferenceId(1)).thenReturn(List.of(track));
        when(subjectAreaRepository.findByTrackId(10)).thenReturn(List.of(new SubjectArea()));
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceActivityService.updateActivities(1, List.of(dto));

        verify(auditLogRepository).saveAll(any());
        verify(notificationSender).sendNotifications(eq(conference), any());
    }

    @Test
    void updateActivitiesShouldKeepExistingNameWhenIncomingNameIsBlank() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivity paperSubmission = find(existing, ActivityType.PAPER_SUBMISSION);
        paperSubmission.setName("Original Name");

        ConferenceActivityDTO dto = dto(ActivityType.PAPER_SUBMISSION, false, LocalDateTime.now().plusDays(2), "   ");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceActivityService.updateActivities(1, List.of(dto));

        assertEquals("Original Name", paperSubmission.getName());
    }

    @Test
    void updateActivitiesShouldAllowClearingDeadlineAndLogNoneAsNewValue() {
        List<ConferenceActivity> existing = allActivities();
        LocalDateTime future = LocalDateTime.now().plusDays(4);
        ConferenceActivity reviewSubmission = find(existing, ActivityType.REVIEW_SUBMISSION);
        reviewSubmission.setDeadline(future);
        ConferenceActivityDTO dto = dto(ActivityType.REVIEW_SUBMISSION, false, null, "Review");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceActivityService.updateActivities(1, List.of(dto));

        ArgumentCaptor<List<ActivityAuditLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditLogRepository).saveAll(captor.capture());
        ActivityAuditLog deadlineLog = captor.getValue().stream()
                .filter(log -> log.getActivityType() == ActivityType.REVIEW_SUBMISSION)
                .filter(log -> "DEADLINE_CHANGED".equals(log.getAction()))
                .findFirst()
                .orElseThrow();
        assertEquals(future.toString(), deadlineLog.getOldValue());
        assertEquals("none", deadlineLog.getNewValue());
    }

    @Test
    void updateActivitiesShouldLogEnabledAndDeadlineChangeWithCurrentUser() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivity target = find(existing, ActivityType.PAPER_SUBMISSION);
        target.setIsEnabled(false);
        target.setDeadline(null);

        LocalDateTime newDeadline = LocalDateTime.now().plusDays(5);
        ConferenceActivityDTO dto = dto(ActivityType.PAPER_SUBMISSION, true, newDeadline, "Open For Submission");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(conferenceTrackRepository.findByConferenceId(1)).thenReturn(List.of(track));
        when(subjectAreaRepository.findByTrackId(10)).thenReturn(List.of(new SubjectArea()));
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceActivityService.updateActivities(1, List.of(dto));

        ArgumentCaptor<List<ActivityAuditLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditLogRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().stream().anyMatch(log -> log.getActivityType() == ActivityType.PAPER_SUBMISSION
                && "ENABLED".equals(log.getAction())
                && "false".equals(log.getOldValue())
                && "true".equals(log.getNewValue())
                && "chair@test.com".equals(log.getPerformedBy())));
        assertTrue(captor.getValue().stream().anyMatch(log -> log.getActivityType() == ActivityType.PAPER_SUBMISSION
                && "DEADLINE_CHANGED".equals(log.getAction())
                && "none".equals(log.getOldValue())
                && newDeadline.toString().equals(log.getNewValue())
                && "chair@test.com".equals(log.getPerformedBy())));
    }

    @Test
    void updateActivitiesShouldFallbackPerformedBySystemWhenAuthenticationMissing() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivity target = find(existing, ActivityType.AUTHOR_NOTIFICATION);
        target.setIsEnabled(false);
        ConferenceActivityDTO dto = dto(ActivityType.AUTHOR_NOTIFICATION, true, LocalDateTime.now().plusDays(3), "Notify");

        SecurityContextHolder.clearContext();
        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceActivityService.updateActivities(1, List.of(dto));

        ArgumentCaptor<List<ActivityAuditLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditLogRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().stream().allMatch(log -> "system".equals(log.getPerformedBy())));
    }

    @Test
    void updateActivitiesShouldNotSavePaperDiscussionFlagsWhenNoTracksExist() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO dto = dto(ActivityType.REVIEW_DISCUSSION, true, LocalDateTime.now().plusDays(3), "Discuss");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(reviewRepository.findByPaper_Track_Conference_Id(1)).thenReturn(List.of(new Review()));
        when(conferenceTrackRepository.findByConferenceId(1)).thenReturn(List.of());
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceActivityService.updateActivities(1, List.of(dto));

        verify(trackReviewSettingRepository, never()).findByTrackId(any());
        verify(paperRepository, never()).saveAll(any());
    }

    @Test
    void getActivitiesShouldOnlyDisableExpiredEnabledActivitiesFromMixedList() {
        ConferenceActivity expiredEnabled = activity(ActivityType.PAPER_SUBMISSION, true, LocalDateTime.now().minusMinutes(30));
        ConferenceActivity expiredDisabled = activity(ActivityType.REVIEW_SUBMISSION, false, LocalDateTime.now().minusMinutes(30));
        ConferenceActivity futureEnabled = activity(ActivityType.REVIEWER_BIDDING, true, LocalDateTime.now().plusDays(1));
        List<ConferenceActivity> activities = List.of(expiredEnabled, expiredDisabled, futureEnabled);

        when(conferenceRepository.existsById(1)).thenReturn(true);
        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(activities, activities);

        List<ConferenceActivityDTO> result = conferenceActivityService.getActivitiesByConferenceId(1);

        assertEquals(3, result.size());
        assertFalse(expiredEnabled.getIsEnabled());
        assertFalse(expiredDisabled.getIsEnabled());
        assertTrue(futureEnabled.getIsEnabled());
        verify(activityRepository, times(1)).save(expiredEnabled);
    }

    @Test
    void updateActivitiesShouldMapConferenceIdInResponseDtos() {
        List<ConferenceActivity> existing = allActivities();
        ConferenceActivityDTO dto = dto(ActivityType.AUTHOR_NOTIFICATION, false, LocalDateTime.now().plusDays(8), "Notify");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(activityRepository.findByConferenceId(1)).thenReturn(existing, existing);
        when(activityRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ConferenceActivityDTO> result = conferenceActivityService.updateActivities(1, List.of(dto));

        assertEquals(ActivityType.values().length, result.size());
        assertTrue(result.stream().allMatch(r -> r.getConferenceId() == 1));
    }

    @Test
    void getAuditLogsShouldMapCreatedAtField() {
        ActivityAuditLog log = new ActivityAuditLog();
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(2);
        log.setId(100L);
        log.setConference(conference);
        log.setActivityType(ActivityType.AUTHOR_NOTIFICATION);
        log.setAction("DEADLINE_CHANGED");
        log.setOldValue("none");
        log.setNewValue(LocalDateTime.now().plusDays(3).toString());
        log.setPerformedBy("chair@test.com");
        log.setCreatedAt(createdAt);

        when(conferenceRepository.existsById(1)).thenReturn(true);
        when(auditLogRepository.findByConferenceIdOrderByCreatedAtDesc(1)).thenReturn(List.of(log));

        List<ActivityAuditLogDTO> result = conferenceActivityService.getAuditLogs(1);

        assertEquals(1, result.size());
        assertEquals(createdAt, result.get(0).getCreatedAt());
    }

    @Test
    void getAuditLogsShouldThrowWhenConferenceMissing() {
        when(conferenceRepository.existsById(1)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> conferenceActivityService.getAuditLogs(1));
    }

    @Test
    void getAuditLogsShouldReturnMappedDtos() {
        ActivityAuditLog log = new ActivityAuditLog();
        log.setId(99L);
        log.setConference(conference);
        log.setActivityType(ActivityType.REVIEW_SUBMISSION);
        log.setAction("ENABLED");
        log.setOldValue("false");
        log.setNewValue("true");
        log.setPerformedBy("chair@test.com");

        when(conferenceRepository.existsById(1)).thenReturn(true);
        when(auditLogRepository.findByConferenceIdOrderByCreatedAtDesc(1)).thenReturn(List.of(log));

        List<ActivityAuditLogDTO> result = conferenceActivityService.getAuditLogs(1);

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).getId());
        assertEquals(ActivityType.REVIEW_SUBMISSION, result.get(0).getActivityType());
        assertEquals("Review Submission", result.get(0).getActivityLabel());
        assertEquals("ENABLED", result.get(0).getAction());
        assertEquals("false", result.get(0).getOldValue());
        assertEquals("true", result.get(0).getNewValue());
    }

    private List<ConferenceActivity> allActivities() {
        List<ConferenceActivity> list = new ArrayList<>();
        for (ActivityType t : ActivityType.values()) {
            list.add(activity(t, false, null));
        }
        return list;
    }

    private ConferenceActivity activity(ActivityType type, boolean enabled, LocalDateTime deadline) {
        ConferenceActivity a = new ConferenceActivity();
        a.setConference(conference);
        a.setActivityType(type);
        a.setName(type.name());
        a.setIsEnabled(enabled);
        a.setDeadline(deadline);
        return a;
    }

    private ConferenceActivityDTO dto(ActivityType type, Boolean enabled, LocalDateTime deadline, String name) {
        ConferenceActivityDTO dto = new ConferenceActivityDTO();
        dto.setActivityType(type);
        dto.setIsEnabled(enabled);
        dto.setDeadline(deadline);
        dto.setName(name);
        return dto;
    }

    private ConferenceActivity find(List<ConferenceActivity> list, ActivityType type) {
        return list.stream().filter(a -> a.getActivityType() == type).findFirst().orElseThrow();
    }
}
