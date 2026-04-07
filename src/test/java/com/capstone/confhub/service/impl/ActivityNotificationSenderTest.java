package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.NotificationDTO;
import com.capstone.confhub.entity.ActivityAuditLog;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.service.EmailService;
import com.capstone.confhub.service.NotificationService;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityNotificationSenderTest {

    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ActivityNotificationSender sender;

    private Conference conference;
    private User chair;
    private User reviewer;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(10);
        conference.setName("ConfHub 2026");

        chair = new User();
        chair.setId(1);
        chair.setEmail("chair@test.com");

        reviewer = new User();
        reviewer.setId(2);
        reviewer.setEmail("reviewer@test.com");
    }

    @Test
    void sendNotificationsShouldSkipWhenNoMembers() {
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of());

        sender.sendNotifications(conference, List.of());

        verify(notificationService, never()).createNotification(any(NotificationDTO.class));
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void sendNotificationsShouldNotifyEnabledAction() {
        ConferenceUserTrack m1 = membership(chair, ConferenceTrackRole.CONFERENCE_CHAIR);
        ConferenceUserTrack m2 = membership(reviewer, ConferenceTrackRole.REVIEWER);
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(m1, m2));

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityType(ActivityType.PAPER_SUBMISSION);
        log.setAction("ENABLED");
        log.setNewValue("2026-08-01");

        sender.sendNotifications(conference, List.of(log));

        verify(notificationService, times(2)).createNotification(any(NotificationDTO.class));
        verify(emailService, times(2)).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void sendNotificationsShouldSkipDisabledAction() {
        ConferenceUserTrack m1 = membership(chair, ConferenceTrackRole.CONFERENCE_CHAIR);
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(m1));

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityType(ActivityType.REVIEW_SUBMISSION);
        log.setAction("DISABLED");

        sender.sendNotifications(conference, List.of(log));

        verify(notificationService, never()).createNotification(any(NotificationDTO.class));
    }

    @Test
    void sendNotificationsShouldHandleDeadlineChangedAction() {
        ConferenceUserTrack m1 = membership(chair, ConferenceTrackRole.CONFERENCE_CHAIR);
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(m1));

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityType(ActivityType.REVIEW_SUBMISSION);
        log.setAction("DEADLINE_CHANGED");
        log.setOldValue("2026-07-01");
        log.setNewValue("2026-07-15");

        sender.sendNotifications(conference, List.of(log));

        verify(notificationService).createNotification(any(NotificationDTO.class));
        verify(emailService).sendSimpleMessage(eq("chair@test.com"), contains("Deadline updated"), contains("changed from"));
    }

    @Test
    void sendNotificationsShouldCreateRoleSpecificLinks() {
        ConferenceUserTrack chairM = membership(chair, ConferenceTrackRole.CONFERENCE_CHAIR);
        ConferenceUserTrack reviewerM = membership(reviewer, ConferenceTrackRole.REVIEWER);
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(chairM, reviewerM));

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityType(ActivityType.PAPER_SUBMISSION);
        log.setAction("ENABLED");

        sender.sendNotifications(conference, List.of(log));

        ArgumentCaptor<NotificationDTO> captor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(notificationService, times(2)).createNotification(captor.capture());

        List<NotificationDTO> sent = captor.getAllValues();
        String chairLink = sent.stream().filter(n -> n.getUserId().equals(1)).findFirst().orElseThrow().getLink();
        String reviewerLink = sent.stream().filter(n -> n.getUserId().equals(2)).findFirst().orElseThrow().getLink();

        assertTrue(chairLink.contains("/conference/10/update"));
        assertTrue(reviewerLink.contains("/conference/10/reviewer"));
    }

    @Test
    void sendNotificationsShouldDeduplicateUsers() {
        ConferenceUserTrack m1 = membership(chair, ConferenceTrackRole.CONFERENCE_CHAIR);
        ConferenceUserTrack m2 = membership(chair, ConferenceTrackRole.PROGRAM_CHAIR);
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(m1, m2));

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityType(ActivityType.REVIEW_SUBMISSION);
        log.setAction("ENABLED");

        sender.sendNotifications(conference, List.of(log));

        verify(notificationService, times(1)).createNotification(any(NotificationDTO.class));
    }

    @Test
    void sendNotificationsShouldIgnoreUnknownAction() {
        ConferenceUserTrack m1 = membership(chair, ConferenceTrackRole.CONFERENCE_CHAIR);
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(m1));

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityType(ActivityType.REVIEW_SUBMISSION);
        log.setAction("SOMETHING_ELSE");

        sender.sendNotifications(conference, List.of(log));

        verify(notificationService, never()).createNotification(any(NotificationDTO.class));
    }

    @Test
    void sendNotificationsShouldContinueWhenSingleUserFails() {
        User brokenUser = new User();
        brokenUser.setId(3);
        brokenUser.setEmail("broken@test.com");

        ConferenceUserTrack m1 = membership(chair, ConferenceTrackRole.CONFERENCE_CHAIR);
        ConferenceUserTrack m2 = membership(brokenUser, ConferenceTrackRole.REVIEWER);
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(m1, m2));

        doAnswer(invocation -> {
            NotificationDTO dto = invocation.getArgument(0);
            if (dto.getUserId().equals(3)) {
                throw new RuntimeException("boom");
            }
            return null;
        }).when(notificationService).createNotification(any(NotificationDTO.class));

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityType(ActivityType.PAPER_SUBMISSION);
        log.setAction("ENABLED");

        sender.sendNotifications(conference, List.of(log));

        verify(notificationService, times(2)).createNotification(any(NotificationDTO.class));
    }

    @Test
    void sendNotificationsShouldWorkWithMultipleLogs() {
        ConferenceUserTrack m1 = membership(chair, ConferenceTrackRole.CONFERENCE_CHAIR);
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(m1));

        ActivityAuditLog l1 = new ActivityAuditLog();
        l1.setActivityType(ActivityType.PAPER_SUBMISSION);
        l1.setAction("ENABLED");

        ActivityAuditLog l2 = new ActivityAuditLog();
        l2.setActivityType(ActivityType.REVIEW_SUBMISSION);
        l2.setAction("DEADLINE_CHANGED");
        l2.setOldValue("none");
        l2.setNewValue("2026-09-01");

        sender.sendNotifications(conference, List.of(l1, l2));

        verify(notificationService, times(2)).createNotification(any(NotificationDTO.class));
        verify(emailService, times(2)).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void sendNotificationsShouldSetNotificationType() {
        ConferenceUserTrack m1 = membership(chair, ConferenceTrackRole.CONFERENCE_CHAIR);
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(m1));

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityType(ActivityType.PAPER_SUBMISSION);
        log.setAction("ENABLED");

        sender.sendNotifications(conference, List.of(log));

        verify(notificationService).createNotification(argThat(n -> "ACTIVITY_UPDATE".equals(n.getType())));
    }

    @Test
    void sendNotificationsShouldNotSendEmailWhenEmailMissing() {
        User noEmail = new User();
        noEmail.setId(4);
        noEmail.setEmail("");

        ConferenceUserTrack m1 = membership(noEmail, ConferenceTrackRole.REVIEWER);
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(m1));

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityType(ActivityType.PAPER_SUBMISSION);
        log.setAction("ENABLED");

        sender.sendNotifications(conference, List.of(log));

        verify(notificationService, times(1)).createNotification(any(NotificationDTO.class));
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    private ConferenceUserTrack membership(User u, ConferenceTrackRole role) {
        ConferenceUserTrack m = new ConferenceUserTrack();
        m.setUser(u);
        m.setConference(conference);
        m.setAssignedRole(role);
        return m;
    }
}


