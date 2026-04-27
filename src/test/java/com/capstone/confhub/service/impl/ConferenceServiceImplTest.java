package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ConferenceDTO;
import com.capstone.confhub.dto.response.ConferenceStatsDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.Ticket;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.security.services.UserDetailsImpl;
import com.capstone.confhub.service.ConferenceActivityService;
import com.capstone.confhub.utils.enums.ConferenceStatus;
import com.capstone.confhub.utils.enums.PaperStatus;
import com.capstone.confhub.utils.enums.PaymentStatus;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.capstone.confhub.exception.ForbiddenException;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;

import java.time.LocalDateTime;
import java.util.List;
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
public class ConferenceServiceImplTest {

    private static final int USER_ID = 1;
    private static final int CONFERENCE_ID = 10;
    private static final String CONFERENCE_NAME = "ConfHub 2025";

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
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private ConferenceServiceImpl conferenceService;

    private User user;
    private Conference conference;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        user.setEmail("chair@example.com");
        user.setFirstName("Chair");
        user.setLastName("User");
        user.setCountry("Vietnam");
        user.setPassword("password");
        user.setIsActive(true);

        conference = new Conference();
        conference.setId(CONFERENCE_ID);
        conference.setName(CONFERENCE_NAME);
        conference.setAcronym("CMS");
        conference.setLocation("HCM");
        conference.setStatus(ConferenceStatus.PENDING_APPROVAL);
        conference.setStartDate(LocalDateTime.now().plusDays(10));
        conference.setEndDate(LocalDateTime.now().plusDays(12));

        var principal = new UserDetailsImpl(USER_ID, user.getEmail(), user.getFirstName(), user.getLastName(), user.getCountry(), user.getPassword(), true, List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private ConferenceDTO buildConferenceDto(String name) {
        return ConferenceDTO.builder()
                .name(name)
                .acronym("CMS")
                .description("Description")
                .location("HCM")
                .startDate(LocalDateTime.now().plusDays(10))
                .endDate(LocalDateTime.now().plusDays(12))
                .websiteUrl("https://example.com")
                .build();
    }

    private void stubChairAuthorization(boolean isChair) {
        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
                USER_ID, CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR)).thenReturn(isChair);
    }

    private Paper buildPaper(PaperStatus status) {
        Paper paper = new Paper();
        paper.setStatus(status);
        return paper;
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
        ConferenceDTO dto = buildConferenceDto(CONFERENCE_NAME);

        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> {
            Conference saved = invocation.getArgument(0);
            saved.setId(CONFERENCE_ID);
            return saved;
        });
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conferenceUserTrackRepository.save(any(ConferenceUserTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceService.createConference(dto);

        assertNotNull(result);
        assertEquals(CONFERENCE_ID, result.getId());
        verify(conferenceActivityService).initializeDefaultActivitiesForConference(CONFERENCE_ID);
    }

    @Test
    void createConferenceShouldThrowWhenNoAuthenticatedUser() {
        SecurityContextHolder.clearContext();
        ConferenceDTO dto = buildConferenceDto(CONFERENCE_NAME);

        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> {
            Conference saved = invocation.getArgument(0);
            saved.setId(CONFERENCE_ID);
            return saved;
        });

        assertThrows(BadRequestException.class, () -> conferenceService.createConference(dto));
    }

    @Test
    void createConferenceShouldThrowWhenAuthenticatedUserMissingInDatabase() {
        ConferenceDTO dto = buildConferenceDto(CONFERENCE_NAME);

        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> {
            Conference saved = invocation.getArgument(0);
            saved.setId(CONFERENCE_ID);
            return saved;
        });
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> conferenceService.createConference(dto));
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
    void getAllConferencesShouldReturnEmptyPage() {
        var page = new PageImpl<Conference>(List.of(), PageRequest.of(0, 20), 0);
        when(conferenceRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = conferenceService.getAllConferences(0, 20);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void getByIdConferenceShouldReturnResponse() {
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));

        var result = conferenceService.getByIdConference(CONFERENCE_ID);

        assertNotNull(result);
        assertEquals(CONFERENCE_ID, result.getId());
    }

    @Test
    void getByIdConferenceShouldThrowWhenNotFound() {
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conferenceService.getByIdConference(CONFERENCE_ID));
    }

    @Test
    void updateConferenceShouldReturnResponse() {
        ConferenceDTO dto = buildConferenceDto("Updated Conf");
        conference.setStatus(ConferenceStatus.PENDING_APPROVAL);

        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceService.updateConference(CONFERENCE_ID, dto);

        assertNotNull(result);
        assertEquals("Updated Conf", result.getName());
    }

    @Test
    void updateConferenceShouldThrowWhenNotFound() {
        ConferenceDTO dto = buildConferenceDto("Updated Conf");
        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> conferenceService.updateConference(CONFERENCE_ID, dto));
    }

    @Test
    void updateConferenceShouldThrowWhenCompleted() {
        ConferenceDTO dto = buildConferenceDto("Updated Conf");
        conference.setStatus(ConferenceStatus.COMPLETED);
        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));

        assertThrows(BadRequestException.class,
                () -> conferenceService.updateConference(CONFERENCE_ID, dto));
    }

    @Test
    void deleteConferenceShouldDelete() {
        stubChairAuthorization(true);
        when(conferenceRepository.existsById(CONFERENCE_ID)).thenReturn(true);

        conferenceService.deleteConference(CONFERENCE_ID);

        verify(conferenceRepository).deleteById(CONFERENCE_ID);
    }

    @Test
    void deleteConferenceShouldThrowWhenNotFound() {
        stubChairAuthorization(true);
        when(conferenceRepository.existsById(CONFERENCE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> conferenceService.deleteConference(CONFERENCE_ID));
    }

    @Test
    void openSubmissionsShouldThrowWhenStatusIsNotScheduled() {
        conference.setStatus(ConferenceStatus.PENDING_APPROVAL);
        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));

        assertThrows(BadRequestException.class,
                () -> conferenceService.openSubmissions(CONFERENCE_ID));
    }

    @Test
    void approveConferenceShouldThrowWhenStatusNotPending() {
        conference.setStatus(ConferenceStatus.SETUP);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));

        assertThrows(BadRequestException.class,
                () -> conferenceService.approveConference(CONFERENCE_ID));
    }

    @Test
    void completeConferenceShouldReturnResponse() {
        conference.setStatus(ConferenceStatus.OPEN);
        ConferenceUserTrack member = new ConferenceUserTrack();
        member.setUser(user);
        member.setConference(conference);

        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_Id(CONFERENCE_ID)).thenReturn(List.of(member));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubChairAuthorization(true);

        var result = conferenceService.completeConference(CONFERENCE_ID);

        assertNotNull(result);
        assertEquals(ConferenceStatus.COMPLETED, result.getStatus());
    }

    @Test
    void completeConferenceShouldThrowWhenStatusNotOngoing() {
        conference.setStatus(ConferenceStatus.SETUP);
        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));

        assertThrows(BadRequestException.class,
                () -> conferenceService.completeConference(CONFERENCE_ID));
    }

    @Test
    void cancelConferenceShouldReturnResponse() {
        conference.setStatus(ConferenceStatus.SETUP);
        ConferenceUserTrack member = new ConferenceUserTrack();
        member.setUser(user);
        member.setConference(conference);

        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_Id(CONFERENCE_ID)).thenReturn(List.of(member));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubChairAuthorization(true);

        var result = conferenceService.cancelConference(CONFERENCE_ID);

        assertNotNull(result);
        assertEquals(ConferenceStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelConferenceShouldThrowWhenConferenceCompleted() {
        conference.setStatus(ConferenceStatus.COMPLETED);
        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));

        assertThrows(BadRequestException.class,
                () -> conferenceService.cancelConference(CONFERENCE_ID));
    }

    @Test
    void updateConferenceShouldThrowForbiddenWhenNotChair() {
        ConferenceDTO dto = buildConferenceDto("Unauthorized Update");
        stubChairAuthorization(false);

        assertThrows(ForbiddenException.class, () -> conferenceService.updateConference(CONFERENCE_ID, dto));
    }

    @Test
    void deleteConferenceShouldThrowForbiddenWhenNotChair() {
        stubChairAuthorization(false);

        assertThrows(ForbiddenException.class, () -> conferenceService.deleteConference(CONFERENCE_ID));
    }

    @Test
    void getProgramScheduleShouldReturnValue() {
        conference.setProgramSchedule("{\"schedule\":{\"days\":[]}}");
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));

        var result = conferenceService.getProgramSchedule(CONFERENCE_ID);

        assertEquals("{\"schedule\":{\"days\":[]}}", result);
    }

    @Test
    void getProgramScheduleShouldReturnNullWhenConferenceNotFound() {
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        var result = conferenceService.getProgramSchedule(CONFERENCE_ID);

        assertNull(result);
    }

    @Test
    void updateProgramScheduleShouldSaveWhenValidAndAuthorizedChair() {
        String scheduleJson = "{\"schedule\":{\"days\":[{\"date\":\"2026-05-10\",\"sessions\":[{\"title\":\"S1\",\"startTime\":\"09:00\",\"endTime\":\"10:00\",\"isGlobal\":false,\"locationId\":\"A\"},{\"title\":\"S2\",\"startTime\":\"10:00\",\"endTime\":\"11:00\",\"isGlobal\":false,\"locationId\":\"A\"}]}]}}";
        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        conferenceService.updateProgramSchedule(CONFERENCE_ID, scheduleJson);

        verify(conferenceRepository).save(any(Conference.class));
    }

    @Test
    void updateProgramScheduleShouldThrowWhenUnauthorized() {
        stubChairAuthorization(false);
        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
                USER_ID, CONFERENCE_ID, ConferenceTrackRole.PROGRAM_CHAIR)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> conferenceService.updateProgramSchedule(CONFERENCE_ID, "{\"schedule\":{\"days\":[]}}"));
    }

    @Test
    void updateProgramScheduleShouldThrowWhenJsonInvalid() {
        stubChairAuthorization(true);

        assertThrows(BadRequestException.class,
                () -> conferenceService.updateProgramSchedule(CONFERENCE_ID, "{invalid-json"));
    }

    @Test
    void updateProgramScheduleShouldThrowWhenSessionOverlapDetected() {
        String overlapJson = "{\"schedule\":{\"days\":[{\"date\":\"2026-05-10\",\"sessions\":[{\"title\":\"S1\",\"startTime\":\"09:00\",\"endTime\":\"10:30\",\"isGlobal\":false,\"locationId\":\"A\"},{\"title\":\"S2\",\"startTime\":\"10:00\",\"endTime\":\"11:00\",\"isGlobal\":false,\"locationId\":\"A\"}]}]}}";
        stubChairAuthorization(true);

        assertThrows(BadRequestException.class,
                () -> conferenceService.updateProgramSchedule(CONFERENCE_ID, overlapJson));
    }

    @Test
    void updateProgramScheduleShouldThrowWhenConferenceMissing() {
        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> conferenceService.updateProgramSchedule(CONFERENCE_ID, "{\"schedule\":{\"days\":[]}}"));
    }

    @Test
    void getConferenceStatsShouldThrowForbiddenWhenNoChairOrProgramChairRole() {
        stubChairAuthorization(false);
        when(conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
                USER_ID, CONFERENCE_ID, ConferenceTrackRole.PROGRAM_CHAIR)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> conferenceService.getConferenceStats(CONFERENCE_ID));
    }

    @Test
    void exportAttendeesCsvShouldReturnCsvForChair() {
        Ticket ticket = new Ticket();
        ticket.setRegistrationNumber("REG-001");
        ticket.setUser(user);
        ticket.setPaymentStatus(PaymentStatus.COMPLETED);
        ticket.setIsCheckedIn(true);
        user.setGender("Female");

        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        stubChairAuthorization(true);
        when(ticketRepository.findByConferenceId(CONFERENCE_ID)).thenReturn(List.of(ticket));

        byte[] csvBytes = conferenceService.exportAttendeesCsv(CONFERENCE_ID);
        String csv = new String(csvBytes);

        assertTrue(csv.contains("Registration Number,First Name,Last Name,Email,Gender,Payment Status,Checked In"));
        assertTrue(csv.contains("REG-001,Chair,User,chair@example.com,Female,COMPLETED,Yes"));
    }

    @Test
    void exportAttendeesCsvShouldThrowWhenUserIsUnauthorized() {
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        stubChairAuthorization(false);

        assertThrows(ForbiddenException.class, () -> conferenceService.exportAttendeesCsv(CONFERENCE_ID));
    }

    @Test
    void exportAttendeesCsvShouldAllowAdminWithoutChairRole() {
        var principal = new UserDetailsImpl(USER_ID, user.getEmail(), user.getFirstName(), user.getLastName(), user.getCountry(), user.getPassword(), true, List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(ticketRepository.findByConferenceId(CONFERENCE_ID)).thenReturn(List.of());

        byte[] csvBytes = conferenceService.exportAttendeesCsv(CONFERENCE_ID);
        String csv = new String(csvBytes);

        assertTrue(csv.startsWith("Registration Number,First Name,Last Name,Email,Gender,Payment Status,Checked In"));
    }

    @Test
    void exportAttendeesCsvShouldThrowWhenConferenceMissing() {
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conferenceService.exportAttendeesCsv(CONFERENCE_ID));
    }

    @Test
    void openSubmissionsShouldThrowForbiddenWhenNotChair() {
        stubChairAuthorization(false);

        assertThrows(ForbiddenException.class, () -> conferenceService.openSubmissions(CONFERENCE_ID));
    }

    @Test
    void completeConferenceShouldThrowForbiddenWhenNotChair() {
        stubChairAuthorization(false);

        assertThrows(ForbiddenException.class, () -> conferenceService.completeConference(CONFERENCE_ID));
    }

    @Test
    void cancelConferenceShouldThrowForbiddenWhenNotChair() {
        stubChairAuthorization(false);

        assertThrows(ForbiddenException.class, () -> conferenceService.cancelConference(CONFERENCE_ID));
    }

    @Test
    void approveConferenceShouldThrowWhenConferenceMissing() {
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conferenceService.approveConference(CONFERENCE_ID));
    }

    @Test
    void openSubmissionsShouldThrowWhenConferenceMissing() {
        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conferenceService.openSubmissions(CONFERENCE_ID));
    }

    @Test
    void completeConferenceShouldThrowWhenConferenceMissing() {
        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conferenceService.completeConference(CONFERENCE_ID));
    }

    @Test
    void cancelConferenceShouldThrowWhenConferenceMissing() {
        stubChairAuthorization(true);
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conferenceService.cancelConference(CONFERENCE_ID));
    }

}




