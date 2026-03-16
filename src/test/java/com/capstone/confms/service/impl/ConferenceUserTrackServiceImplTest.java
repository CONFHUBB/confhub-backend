package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.entity.Notification;
import com.capstone.confms.entity.User;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.NotificationRepository;
import com.capstone.confms.repository.ReviewRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConferenceUserTrackServiceImplTest {

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
        user.setId(1);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setIsActive(true);

        chairUser = new User();
        chairUser.setId(2);
        chairUser.setFirstName("Chair");
        chairUser.setLastName("User");
        chairUser.setEmail("chair@example.com");

        conference = new Conference();
        conference.setId(10);
        conference.setName("ConfMS 2025");
        conference.setAcronym("CMS");

        track = new ConferenceTrack();
        track.setId(20);
        track.setConference(conference);

        reviewerAssignment = new ConferenceUserTrack();
        reviewerAssignment.setId(30);
        reviewerAssignment.setUser(user);
        reviewerAssignment.setConference(conference);
        reviewerAssignment.setConferenceTrack(track);
        reviewerAssignment.setAssignedRole(ConferenceTrackRole.REVIEWER);
        reviewerAssignment.setIsAccepted(true);

        chairAssignment = new ConferenceUserTrack();
        chairAssignment.setId(31);
        chairAssignment.setUser(chairUser);
        chairAssignment.setConference(conference);
        chairAssignment.setConferenceTrack(track);
        chairAssignment.setAssignedRole(ConferenceTrackRole.CONFERENCE_CHAIR);
        chairAssignment.setIsAccepted(true);
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

        when(conferenceRepository.findById(10)).thenReturn(Optional.of(conference));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(10, ConferenceTrackRole.PROGRAM_CHAIR))
                .thenReturn(List.of(programChair));

        var result = conferenceUserTrackService.getTrackChairsByConferenceId(10, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getChairedConferencesByUserIdShouldReturnConferences() {
        ConferenceUserTrack programChair = new ConferenceUserTrack();
        programChair.setUser(user);
        programChair.setConference(conference);
        programChair.setAssignedRole(ConferenceTrackRole.PROGRAM_CHAIR);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.findByUser_IdAndAssignedRole(1, ConferenceTrackRole.PROGRAM_CHAIR))
                .thenReturn(List.of(programChair));

        var result = conferenceUserTrackService.getChairedConferencesByUserId(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getOrganizedConferencesByUserIdShouldReturnConferences() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.findByUser_IdAndAssignedRole(1, ConferenceTrackRole.CONFERENCE_CHAIR))
                .thenReturn(List.of(chairAssignment));

        var result = conferenceUserTrackService.getOrganizedConferencesByUserId(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getReviewerConferencesByUserIdShouldReturnAcceptedConferences() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.findByUser_IdAndAssignedRole(1, ConferenceTrackRole.REVIEWER))
                .thenReturn(List.of(reviewerAssignment));

        var result = conferenceUserTrackService.getReviewerConferencesByUserId(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getUserRoleAssignmentsShouldReturnAssignments() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.findByUser_Id(1)).thenReturn(List.of(reviewerAssignment));

        var result = conferenceUserTrackService.getUserRoleAssignments(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void assignRoleToUserTrackShouldReturnResponse() {
        AssignConferenceUserTrackRequest request = new AssignConferenceUserTrackRequest();
        request.setUserId(1);
        request.setConferenceId(10);
        request.setTrackId(20);
        request.setAssignedRole(ConferenceTrackRole.REVIEWER);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(conferenceRepository.findById(10)).thenReturn(Optional.of(conference));
        when(conferenceTrackRepository.findById(20)).thenReturn(Optional.of(track));
        when(conferenceUserTrackRepository.save(any(ConferenceUserTrack.class))).thenAnswer(invocation -> {
            ConferenceUserTrack saved = invocation.getArgument(0);
            saved.setId(30);
            return saved;
        });
        when(notificationRepository.existsByUser_IdAndConference_IdAndType(1, 10, "INVITATION")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceUserTrackService.assignRoleToUserTrack(request);

        assertNotNull(result);
        assertEquals(30, result.getId());
        assertEquals(ConferenceTrackRole.REVIEWER, result.getAssignedRole());
    }

    @Test
    void acceptInvitationShouldReturnResponse() {
        ConferenceUserTrack pending = new ConferenceUserTrack();
        pending.setId(30);
        pending.setUser(user);
        pending.setConference(conference);
        pending.setAssignedRole(ConferenceTrackRole.REVIEWER);
        pending.setIsAccepted(false);

        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(1, 10)).thenReturn(List.of(pending));
        when(conferenceUserTrackRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(10, ConferenceTrackRole.CONFERENCE_CHAIR))
                .thenReturn(List.of(chairAssignment));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceUserTrackService.acceptInvitation(1, 10);

        assertNotNull(result);
        assertEquals(true, result.getIsAccepted());
    }

    @Test
    void declineInvitationShouldReturnResponse() {
        ConferenceUserTrack pending = new ConferenceUserTrack();
        pending.setId(30);
        pending.setUser(user);
        pending.setConference(conference);
        pending.setAssignedRole(ConferenceTrackRole.REVIEWER);
        pending.setIsAccepted(true);

        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(1, 10)).thenReturn(List.of(pending));
        when(conferenceUserTrackRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(10, ConferenceTrackRole.CONFERENCE_CHAIR))
                .thenReturn(List.of(chairAssignment));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceUserTrackService.declineInvitation(1, 10);

        assertNotNull(result);
        assertEquals(false, result.getIsAccepted());
    }

    @Test
    void getConferenceUsersWithRolesShouldReturnGroupedUsers() {
        when(conferenceRepository.findById(10)).thenReturn(Optional.of(conference));
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(reviewerAssignment, chairAssignment));

        var result = conferenceUserTrackService.getConferenceUsersWithRoles(10, 0, 20);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void removeRoleFromUserShouldDeleteAssignment() {
        when(conferenceUserTrackRepository.findById(30)).thenReturn(Optional.of(reviewerAssignment));
        when(reviewRepository.countByReviewer_IdAndPaper_Track_Conference_Id(1, 10)).thenReturn(0L);
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(1, 10)).thenReturn(List.of(reviewerAssignment));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceUserTrackService.removeRoleFromUser(30);

        verify(conferenceUserTrackRepository).deleteById(30);
    }
}




