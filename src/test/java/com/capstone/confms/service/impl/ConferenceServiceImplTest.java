package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.entity.Notification;
import com.capstone.confms.entity.User;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.NotificationRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.security.services.UserDetailsImpl;
import com.capstone.confms.service.ConferenceActivityService;
import com.capstone.confms.utils.enums.ConferenceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConferenceServiceImplTest {

    @Mock
    private ConferenceRepository conferenceRepository;
    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ConferenceActivityService conferenceActivityService;

    @InjectMocks
    private ConferenceServiceImpl conferenceService;

    private User user;
    private Conference conference;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail("chair@example.com");
        user.setFirstName("Chair");
        user.setLastName("User");
        user.setCountry("Vietnam");
        user.setPassword("password");
        user.setIsActive(true);

        conference = new Conference();
        conference.setId(10);
        conference.setName("ConfMS 2025");
        conference.setAcronym("CMS");
        conference.setLocation("HCM");
        conference.setStatus(ConferenceStatus.PENDING);
        conference.setStartDate(LocalDateTime.now().plusDays(10));
        conference.setEndDate(LocalDateTime.now().plusDays(12));

        var principal = new UserDetailsImpl(1, user.getEmail(), user.getFirstName(), user.getLastName(), user.getCountry(), user.getPassword(), true, List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateService() {
        assertNotNull(conferenceService);
    }

    @Test
    void createConferenceShouldReturnResponse() {
        ConferenceDTO dto = ConferenceDTO.builder()
                .name("ConfMS 2025")
                .acronym("CMS")
                .description("Description")
                .location("HCM")
                .startDate(LocalDateTime.now().plusDays(10))
                .endDate(LocalDateTime.now().plusDays(12))
                .websiteUrl("https://example.com")
                .build();

        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> {
            Conference saved = invocation.getArgument(0);
            saved.setId(10);
            return saved;
        });
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.save(any(ConferenceUserTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceService.createConference(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        verify(conferenceActivityService).initializeDefaultActivitiesForConference(10);
    }

    @Test
    void getAllConferencesShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(conference), PageRequest.of(0, 20), 1);
        when(conferenceRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = conferenceService.getAllConferences(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getByIdConferenceShouldReturnResponse() {
        when(conferenceRepository.findById(10)).thenReturn(Optional.of(conference));

        var result = conferenceService.getByIdConference(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void updateConferenceShouldReturnResponse() {
        ConferenceDTO dto = ConferenceDTO.builder()
                .name("Updated Conf")
                .acronym("UC")
                .description("Updated")
                .location("HN")
                .startDate(LocalDateTime.now().plusDays(20))
                .endDate(LocalDateTime.now().plusDays(22))
                .websiteUrl("https://updated.com")
                .build();
        conference.setStatus(ConferenceStatus.PENDING);

        when(conferenceRepository.findById(10)).thenReturn(Optional.of(conference));
        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceService.updateConference(10, dto);

        assertNotNull(result);
        assertEquals("Updated Conf", result.getName());
    }

    @Test
    void deleteConferenceShouldDelete() {
        when(conferenceRepository.existsById(10)).thenReturn(true);

        conferenceService.deleteConference(10);

        verify(conferenceRepository).deleteById(10);
    }

    @Test
    void openSubmissionsShouldReturnResponse() {
        conference.setStatus(ConferenceStatus.SCHEDULED);
        ConferenceUserTrack member = new ConferenceUserTrack();
        member.setUser(user);
        member.setConference(conference);

        when(conferenceRepository.findById(10)).thenReturn(Optional.of(conference));
        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(member));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceService.openSubmissions(10);

        assertNotNull(result);
        assertEquals(ConferenceStatus.ONGOING, result.getStatus());
    }

    @Test
    void approveConferenceShouldReturnResponse() {
        ConferenceUserTrack member = new ConferenceUserTrack();
        member.setUser(user);
        member.setConference(conference);

        when(conferenceRepository.findById(10)).thenReturn(Optional.of(conference));
        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(member));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceService.approveConference(10);

        assertNotNull(result);
        assertEquals(ConferenceStatus.SCHEDULED, result.getStatus());
    }

    @Test
    void completeConferenceShouldReturnResponse() {
        conference.setStatus(ConferenceStatus.ONGOING);
        ConferenceUserTrack member = new ConferenceUserTrack();
        member.setUser(user);
        member.setConference(conference);

        when(conferenceRepository.findById(10)).thenReturn(Optional.of(conference));
        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(member));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceService.completeConference(10);

        assertNotNull(result);
        assertEquals(ConferenceStatus.COMPLETED, result.getStatus());
    }

    @Test
    void cancelConferenceShouldReturnResponse() {
        conference.setStatus(ConferenceStatus.SCHEDULED);
        ConferenceUserTrack member = new ConferenceUserTrack();
        member.setUser(user);
        member.setConference(conference);

        when(conferenceRepository.findById(10)).thenReturn(Optional.of(conference));
        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_Id(10)).thenReturn(List.of(member));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceService.cancelConference(10);

        assertNotNull(result);
        assertEquals(ConferenceStatus.CANCELLED, result.getStatus());
    }
}




