package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ForbiddenException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.security.services.UserDetailsImpl;
import com.capstone.confhub.service.EmailService;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConferenceUserTrackServiceImplTest {

    private static final int USER_ID = 1;
    private static final int CHAIR_USER_ID = 2;
    private static final int CONFERENCE_ID = 10;
    private static final int TRACK_ID = 20;
    private static final int REVIEWER_ASSIGNMENT_ID = 30;
    private static final int CHAIR_ASSIGNMENT_ID = 31;

    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ConferenceRepository conferenceRepository;
    @Mock
    private ConferenceTrackRepository conferenceTrackRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private TrackReviewSettingRepository trackReviewSettingRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ConferenceUserTrackServiceImpl conferenceUserTrackService;

    private User user;
    private User chairUser;
    private Conference conference;
    private ConferenceTrack track;
    private ConferenceUserTrack reviewerAssignment;
    private ConferenceUserTrack chairAssignment;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setIsActive(true);

        chairUser = new User();
        chairUser.setId(CHAIR_USER_ID);
        chairUser.setFirstName("Chair");
        chairUser.setLastName("User");
        chairUser.setEmail("chair@example.com");

        conference = new Conference();
        conference.setId(CONFERENCE_ID);
        conference.setName("ConfHub 2025");
        conference.setAcronym("CMS");

        track = new ConferenceTrack();
        track.setId(TRACK_ID);
        track.setConference(conference);

        reviewerAssignment = new ConferenceUserTrack();
        reviewerAssignment.setId(REVIEWER_ASSIGNMENT_ID);
        reviewerAssignment.setUser(user);
        reviewerAssignment.setConference(conference);
        reviewerAssignment.setConferenceTrack(track);
        reviewerAssignment.setAssignedRole(ConferenceTrackRole.REVIEWER);
        reviewerAssignment.setIsAccepted(true);

        chairAssignment = new ConferenceUserTrack();
        chairAssignment.setId(CHAIR_ASSIGNMENT_ID);
        chairAssignment.setUser(chairUser);
        chairAssignment.setConference(conference);
        chairAssignment.setConferenceTrack(track);
        chairAssignment.setAssignedRole(ConferenceTrackRole.CONFERENCE_CHAIR);
        chairAssignment.setIsAccepted(true);

        // Set up SecurityContext: chair user is the authenticated caller.
        var principal = new UserDetailsImpl(CHAIR_USER_ID, chairUser.getEmail(), chairUser.getFirstName(),
            chairUser.getLastName(), null, "password", true, List.of());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateService() {
        assertNotNull(conferenceUserTrackService);
    }

    @Test
    void getTrackChairsByConferenceIdShouldReturnUsers() {
        ConferenceUserTrack programChair = new ConferenceUserTrack();
        programChair.setId(40);
        programChair.setUser(chairUser);
        programChair.setConference(conference);
        programChair.setAssignedRole(ConferenceTrackRole.PROGRAM_CHAIR);

        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(CONFERENCE_ID, ConferenceTrackRole.PROGRAM_CHAIR))
            .thenReturn(List.of(programChair));

        var result = conferenceUserTrackService.getTrackChairsByConferenceId(CONFERENCE_ID, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getTrackChairsByConferenceIdShouldThrowWhenConferenceMissing() {
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> conferenceUserTrackService.getTrackChairsByConferenceId(CONFERENCE_ID, 0, 20));
    }

    @Test
    void getTrackChairsByConferenceIdShouldReturnDistinctUsers() {
        ConferenceUserTrack first = new ConferenceUserTrack();
        first.setId(40);
        first.setUser(chairUser);
        first.setConference(conference);
        first.setAssignedRole(ConferenceTrackRole.PROGRAM_CHAIR);

        ConferenceUserTrack second = new ConferenceUserTrack();
        second.setId(41);
        second.setUser(chairUser);
        second.setConference(conference);
        second.setAssignedRole(ConferenceTrackRole.PROGRAM_CHAIR);

        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(CONFERENCE_ID, ConferenceTrackRole.PROGRAM_CHAIR))
            .thenReturn(List.of(first, second));

        var result = conferenceUserTrackService.getTrackChairsByConferenceId(CONFERENCE_ID, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getChairedConferencesByUserIdShouldReturnConferences() {
        ConferenceUserTrack programChair = new ConferenceUserTrack();
        programChair.setUser(user);
        programChair.setConference(conference);
        programChair.setAssignedRole(ConferenceTrackRole.PROGRAM_CHAIR);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.findByUser_IdAndAssignedRole(USER_ID, ConferenceTrackRole.PROGRAM_CHAIR))
            .thenReturn(List.of(programChair));

        var result = conferenceUserTrackService.getChairedConferencesByUserId(USER_ID, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getChairedConferencesByUserIdShouldThrowWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> conferenceUserTrackService.getChairedConferencesByUserId(USER_ID, 0, 20));
    }

    @Test
    void getOrganizedConferencesByUserIdShouldReturnConferences() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.findByUser_IdAndAssignedRole(USER_ID, ConferenceTrackRole.CONFERENCE_CHAIR))
            .thenReturn(List.of(chairAssignment));

        var result = conferenceUserTrackService.getOrganizedConferencesByUserId(USER_ID, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getOrganizedConferencesByUserIdShouldThrowWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> conferenceUserTrackService.getOrganizedConferencesByUserId(USER_ID, 0, 20));
    }

    @Test
    void getReviewerConferencesByUserIdShouldReturnAcceptedConferences() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.findByUser_IdAndAssignedRole(USER_ID, ConferenceTrackRole.REVIEWER))
            .thenReturn(List.of(reviewerAssignment));

        var result = conferenceUserTrackService.getReviewerConferencesByUserId(USER_ID, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getUserRoleAssignmentsShouldReturnAssignments() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.findByUser_Id(USER_ID)).thenReturn(List.of(reviewerAssignment));

        var result = conferenceUserTrackService.getUserRoleAssignments(USER_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getUserRoleAssignmentsShouldThrowWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> conferenceUserTrackService.getUserRoleAssignments(USER_ID));
    }

    @Test
    void assignRoleToUserTrackShouldReturnResponse() throws Exception {
        AssignConferenceUserTrackRequest request = new AssignConferenceUserTrackRequest();
        request.setUserId(USER_ID);
        request.setConferenceId(CONFERENCE_ID);
        request.setTrackId(TRACK_ID);
        request.setAssignedRole(ConferenceTrackRole.REVIEWER);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(conferenceTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            CHAIR_USER_ID, CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR)).thenReturn(true);
        when(conferenceUserTrackRepository.save(any(ConferenceUserTrack.class))).thenAnswer(invocation -> {
            ConferenceUserTrack saved = invocation.getArgument(0);
            saved.setId(REVIEWER_ASSIGNMENT_ID);
            return saved;
        });
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceUserTrackService.assignRoleToUserTrack(request);

        assertNotNull(result);
        assertEquals(REVIEWER_ASSIGNMENT_ID, result.getId());
        assertEquals(ConferenceTrackRole.REVIEWER, result.getAssignedRole());
        verify(emailService).sendInvitationEmail(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void assignRoleToUserTrackShouldThrowWhenUserMissing() {
        AssignConferenceUserTrackRequest request = new AssignConferenceUserTrackRequest();
        request.setUserId(USER_ID);
        request.setConferenceId(CONFERENCE_ID);
        request.setTrackId(TRACK_ID);
        request.setAssignedRole(ConferenceTrackRole.REVIEWER);

        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            CHAIR_USER_ID, CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conferenceUserTrackService.assignRoleToUserTrack(request));
    }

    @Test
    void assignRoleToUserTrackShouldAllowNullTrackForConferenceLevelRole() {
        AssignConferenceUserTrackRequest request = new AssignConferenceUserTrackRequest();
        request.setUserId(USER_ID);
        request.setConferenceId(CONFERENCE_ID);
        request.setTrackId(null);
        request.setAssignedRole(ConferenceTrackRole.ATTENDEE);

        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            CHAIR_USER_ID, CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(conferenceUserTrackRepository.save(any(ConferenceUserTrack.class))).thenAnswer(invocation -> {
            ConferenceUserTrack saved = invocation.getArgument(0);
            saved.setId(REVIEWER_ASSIGNMENT_ID);
            return saved;
        });
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceUserTrackService.assignRoleToUserTrack(request);

        assertNotNull(result);
        assertEquals(ConferenceTrackRole.ATTENDEE, result.getAssignedRole());
        assertNull(result.getConferenceTrackId());
    }

    @Test
    void assignRoleToUserTrackShouldThrowWhenCallerNotChair() {
        AssignConferenceUserTrackRequest request = new AssignConferenceUserTrackRequest();
        request.setUserId(USER_ID);
        request.setConferenceId(CONFERENCE_ID);
        request.setTrackId(TRACK_ID);
        request.setAssignedRole(ConferenceTrackRole.REVIEWER);

        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            CHAIR_USER_ID, CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> conferenceUserTrackService.assignRoleToUserTrack(request));
    }

    @Test
    void acceptInvitationShouldReturnResponse() {
        ConferenceUserTrack pending = new ConferenceUserTrack();
        pending.setId(REVIEWER_ASSIGNMENT_ID);
        pending.setUser(user);
        pending.setConference(conference);
        pending.setAssignedRole(ConferenceTrackRole.REVIEWER);
        pending.setIsAccepted(false);
        pending.setConferenceTrack(track);

        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(USER_ID, CONFERENCE_ID)).thenReturn(List.of(pending));
        when(conferenceUserTrackRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR))
            .thenReturn(List.of(chairAssignment));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceUserTrackService.acceptInvitation(USER_ID, CONFERENCE_ID, null);

        assertNotNull(result);
        assertEquals(true, result.getIsAccepted());
    }

    @Test
    void acceptInvitationShouldThrowWhenAssignmentMissing() {
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(USER_ID, CONFERENCE_ID)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
            () -> conferenceUserTrackService.acceptInvitation(USER_ID, CONFERENCE_ID, null));
    }

    @Test
    void acceptInvitationShouldSetQuotaWhenAllowedByTrackSetting() {
        ConferenceUserTrack pending = new ConferenceUserTrack();
        pending.setId(REVIEWER_ASSIGNMENT_ID);
        pending.setUser(user);
        pending.setConference(conference);
        pending.setAssignedRole(ConferenceTrackRole.REVIEWER);
        pending.setIsAccepted(false);
        pending.setConferenceTrack(track);

        TrackReviewSetting setting = new TrackReviewSetting();
        setting.setAllowReviewerQuota(true);

        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(USER_ID, CONFERENCE_ID)).thenReturn(List.of(pending));
        when(trackReviewSettingRepository.findByTrackId(TRACK_ID)).thenReturn(Optional.of(setting));
        when(conferenceUserTrackRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR))
            .thenReturn(List.of(chairAssignment));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceUserTrackService.acceptInvitation(USER_ID, CONFERENCE_ID, 5);

        assertNotNull(result);
        assertEquals(true, result.getIsAccepted());
        assertEquals(5, result.getReviewerQuota());
    }

    @Test
    void declineInvitationShouldReturnResponse() {
        ConferenceUserTrack pending = new ConferenceUserTrack();
        pending.setId(REVIEWER_ASSIGNMENT_ID);
        pending.setUser(user);
        pending.setConference(conference);
        pending.setAssignedRole(ConferenceTrackRole.REVIEWER);
        pending.setIsAccepted(true);

        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(USER_ID, CONFERENCE_ID)).thenReturn(List.of(pending));
        when(conferenceUserTrackRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR))
            .thenReturn(List.of(chairAssignment));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceUserTrackService.declineInvitation(USER_ID, CONFERENCE_ID);

        assertNotNull(result);
        assertEquals(false, result.getIsAccepted());
    }

    @Test
    void declineInvitationShouldThrowWhenAssignmentMissing() {
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(USER_ID, CONFERENCE_ID)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
            () -> conferenceUserTrackService.declineInvitation(USER_ID, CONFERENCE_ID));
    }

    @Test
    void getConferenceUsersWithRolesShouldReturnGroupedUsers() {
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(conferenceUserTrackRepository.findByConference_Id(CONFERENCE_ID)).thenReturn(List.of(reviewerAssignment, chairAssignment));

        var result = conferenceUserTrackService.getConferenceUsersWithRoles(CONFERENCE_ID, 0, 20);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void getConferenceUsersWithRolesShouldThrowWhenConferenceMissing() {
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> conferenceUserTrackService.getConferenceUsersWithRoles(CONFERENCE_ID, 0, 20));
    }

    @Test
    void removeRoleFromUserShouldDeleteAssignment() {
        when(conferenceUserTrackRepository.findById(REVIEWER_ASSIGNMENT_ID)).thenReturn(Optional.of(reviewerAssignment));
        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            CHAIR_USER_ID, CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR)).thenReturn(true);
        when(reviewRepository.countByReviewer_IdAndPaper_Track_Conference_Id(USER_ID, CONFERENCE_ID)).thenReturn(0L);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceUserTrackService.removeRoleFromUser(REVIEWER_ASSIGNMENT_ID);

        verify(conferenceUserTrackRepository).deleteById(REVIEWER_ASSIGNMENT_ID);
        verify(emailService).sendSimpleMessage(any(), any(), any());
    }

    @Test
    void removeRoleFromUserShouldThrowWhenAssignmentMissing() {
        when(conferenceUserTrackRepository.findById(REVIEWER_ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> conferenceUserTrackService.removeRoleFromUser(REVIEWER_ASSIGNMENT_ID));
    }

    @Test
    void removeRoleFromUserShouldThrowWhenCallerNotChair() {
        when(conferenceUserTrackRepository.findById(REVIEWER_ASSIGNMENT_ID)).thenReturn(Optional.of(reviewerAssignment));
        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            CHAIR_USER_ID, CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR)).thenReturn(false);

        assertThrows(ForbiddenException.class,
            () -> conferenceUserTrackService.removeRoleFromUser(REVIEWER_ASSIGNMENT_ID));
    }

    @Test
    void removeRoleFromUserShouldThrowWhenRemovingLastConferenceChair() {
        when(conferenceUserTrackRepository.findById(CHAIR_ASSIGNMENT_ID)).thenReturn(Optional.of(chairAssignment));
        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            CHAIR_USER_ID, CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR)).thenReturn(true);
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR))
            .thenReturn(List.of(chairAssignment));

        assertThrows(BadRequestException.class,
            () -> conferenceUserTrackService.removeRoleFromUser(CHAIR_ASSIGNMENT_ID));
    }

    @Test
    void removeRoleFromUserShouldThrowWhenReviewerHasAssignments() {
        when(conferenceUserTrackRepository.findById(REVIEWER_ASSIGNMENT_ID)).thenReturn(Optional.of(reviewerAssignment));
        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
            CHAIR_USER_ID, CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR)).thenReturn(true);
        when(reviewRepository.countByReviewer_IdAndPaper_Track_Conference_Id(USER_ID, CONFERENCE_ID)).thenReturn(2L);

        assertThrows(BadRequestException.class,
            () -> conferenceUserTrackService.removeRoleFromUser(REVIEWER_ASSIGNMENT_ID));
    }

    @Test
    void getReviewerConferencesByUserIdShouldSkipUnacceptedAssignments() {
        reviewerAssignment.setIsAccepted(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.findByUser_IdAndAssignedRole(USER_ID, ConferenceTrackRole.REVIEWER))
            .thenReturn(List.of(reviewerAssignment));

        var result = conferenceUserTrackService.getReviewerConferencesByUserId(USER_ID, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }
}
