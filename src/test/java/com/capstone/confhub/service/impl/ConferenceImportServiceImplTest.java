package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.response.ImportResultDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.Role;
import com.capstone.confhub.entity.TicketType;
import com.capstone.confhub.entity.SubjectArea;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.entity.UserProfile;
import com.capstone.confhub.entity.UserRole;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.RoleRepository;
import com.capstone.confhub.repository.TicketTypeRepository;
import com.capstone.confhub.repository.SubjectAreaRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.repository.UserProfileRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.repository.UserRoleRepository;
import com.capstone.confhub.security.services.UserDetailsImpl;
import com.capstone.confhub.service.ConferenceActivityService;
import com.capstone.confhub.service.EmailService;
import com.capstone.confhub.utils.enums.ConferenceStatus;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import com.capstone.confhub.utils.enums.TicketCategory;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConferenceImportServiceImplTest {

    @Mock
    private ConferenceRepository conferenceRepository;
    @Mock
    private ConferenceTrackRepository conferenceTrackRepository;
    @Mock
    private TrackReviewSettingRepository trackReviewSettingRepository;
    @Mock
    private SubjectAreaRepository subjectAreaRepository;
    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private ConferenceActivityService conferenceActivityService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ConferenceImportServiceImpl conferenceImportService;

    private User currentUser;
    private Conference conference;
    private ConferenceTrack track;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(conferenceImportService, "baseUrl", "https://api.test");
        ReflectionTestUtils.setField(conferenceImportService, "frontendUrl", "https://web.test");

        currentUser = new User();
        currentUser.setId(7);
        currentUser.setEmail("chair@test.com");
        currentUser.setFirstName("Chair");
        currentUser.setLastName("User");

        conference = new Conference();
        conference.setId(11);
        conference.setName("ConfHub 2027");

        track = new ConferenceTrack();
        track.setId(21);
        track.setName("Machine Learning");
        track.setConference(conference);

        setAuth(currentUser.getId());
        lenient().when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateService() {
        assertNotNull(conferenceImportService);
    }

    @Test
    void previewConferenceFromExcelShouldParseValidData() {
        MultipartFile file = conferenceFile(validConferenceRow());

        ImportResultDTO result = conferenceImportService.previewConferenceFromExcel(file);

        assertTrue(result.isSuccess());
        assertEquals("ConfHub Summit", result.getConferencePreview().get("name"));
        assertEquals("CHS2027", result.getConferencePreview().get("acronym"));
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void previewConferenceFromExcelShouldFailWhenMissingRequiredFields() {
        Map<String, String> row = validConferenceRow();
        row.put("name", "");
        row.put("acronym", "");
        MultipartFile file = conferenceFile(row);

        ImportResultDTO result = conferenceImportService.previewConferenceFromExcel(file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> "name".equals(e.getColumn())));
        assertTrue(result.getErrors().stream().anyMatch(e -> "acronym".equals(e.getColumn())));
    }

    @Test
    void previewConferenceFromExcelShouldFailWhenDatesInvalid() {
        Map<String, String> row = validConferenceRow();
        row.put("startDate", "bad-date");
        row.put("endDate", "also-bad");
        MultipartFile file = conferenceFile(row);

        ImportResultDTO result = conferenceImportService.previewConferenceFromExcel(file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> "startDate".equals(e.getColumn())));
        assertTrue(result.getErrors().stream().anyMatch(e -> "endDate".equals(e.getColumn())));
    }

    @Test
    void previewConferenceFromExcelShouldThrowWhenFileEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", "conference.xlsx", "application/octet-stream", new byte[0]);

        assertThrows(BadRequestException.class, () -> conferenceImportService.previewConferenceFromExcel(file));
    }

    @Test
    void previewConferenceFromExcelShouldThrowWhenFileExtensionInvalid() {
        MockMultipartFile file = new MockMultipartFile("file", "conference.csv", "text/plain", "a,b,c".getBytes());

        assertThrows(BadRequestException.class, () -> conferenceImportService.previewConferenceFromExcel(file));
    }

    @Test
    void importConferenceFromExcelShouldPersistConferenceAndChairMembership() {
        Map<String, String> row = validConferenceRow();
        MultipartFile file = conferenceFile(row);

        when(conferenceRepository.save(any(Conference.class))).thenAnswer(invocation -> {
            Conference saved = invocation.getArgument(0);
            saved.setId(1001);
            return saved;
        });

        ImportResultDTO result = conferenceImportService.importConferenceFromExcel(file);

        assertTrue(result.isSuccess());
        assertEquals(1001, result.getConferenceId());
        verify(conferenceActivityService).initializeDefaultActivitiesForConference(1001);
        verify(conferenceUserTrackRepository).save(any(ConferenceUserTrack.class));

        ArgumentCaptor<Conference> conferenceCaptor = ArgumentCaptor.forClass(Conference.class);
        verify(conferenceRepository).save(conferenceCaptor.capture());
        assertEquals(ConferenceStatus.PENDING, conferenceCaptor.getValue().getStatus());
        assertEquals("ConfHub Summit", conferenceCaptor.getValue().getName());
    }

    @Test
    void importConferenceFromExcelShouldReturnPreviewErrorsWithoutPersisting() {
        Map<String, String> invalid = validConferenceRow();
        invalid.put("name", "");
        MultipartFile file = conferenceFile(invalid);

        ImportResultDTO result = conferenceImportService.importConferenceFromExcel(file);

        assertFalse(result.isSuccess());
        verify(conferenceRepository, never()).save(any(Conference.class));
        verify(conferenceActivityService, never()).initializeDefaultActivitiesForConference(any());
    }

    @Test
    void importConferenceFromExcelShouldThrowWhenNoAuthenticatedUser() {
        SecurityContextHolder.clearContext();
        MultipartFile file = conferenceFile(validConferenceRow());

        assertThrows(BadRequestException.class, () -> conferenceImportService.importConferenceFromExcel(file));
    }

    @Test
    void importConferenceFromExcelShouldThrowWhenPrincipalUserMissingInDatabase() {
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.empty());
        MultipartFile file = conferenceFile(validConferenceRow());

        assertThrows(BadRequestException.class, () -> conferenceImportService.importConferenceFromExcel(file));
    }

    @Test
    void generateConferenceTemplateShouldReturnNonEmptyXlsxBytes() {
        byte[] bytes = conferenceImportService.generateConferenceTemplate();

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void previewTracksFromExcelShouldParseRows() {
        MultipartFile file = tracksFile(List.of(
                row("name", "Machine Learning", "description", "ML papers"),
                row("name", "NLP", "description", "NLP papers")
        ));

        ImportResultDTO result = conferenceImportService.previewTracksFromExcel(file);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTrackPreviews().size());
        assertEquals("Machine Learning", result.getTrackPreviews().get(0).get("name"));
    }

    @Test
    void previewTracksFromExcelShouldFailOnDuplicateName() {
        MultipartFile file = tracksFile(List.of(
                row("name", "Machine Learning", "description", "ML papers"),
                row("name", "Machine Learning", "description", "Duplicate")
        ));

        ImportResultDTO result = conferenceImportService.previewTracksFromExcel(file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> "Duplicate: Machine Learning".equals(e.getMessage())));
    }

    @Test
    void previewTracksFromExcelShouldFailOnMissingDescription() {
        MultipartFile file = tracksFile(List.of(
                row("name", "Machine Learning", "description", "")
        ));

        ImportResultDTO result = conferenceImportService.previewTracksFromExcel(file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> "description".equals(e.getColumn())));
    }

    @Test
    void importTracksFromExcelShouldCreateTracksAndReviewSettings() {
        MultipartFile file = tracksFile(List.of(
                row("name", "Machine Learning", "description", "ML papers"),
                row("name", "NLP", "description", "NLP papers")
        ));

        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(conferenceTrackRepository.save(any(ConferenceTrack.class))).thenAnswer(invocation -> {
            ConferenceTrack saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(saved.getName().equals("Machine Learning") ? 30 : 31);
            }
            return saved;
        });
        when(trackReviewSettingRepository.save(any(TrackReviewSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResultDTO result = conferenceImportService.importTracksFromExcel(11, file);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTracksCreated());
        verify(conferenceTrackRepository, times(2)).save(any(ConferenceTrack.class));
        verify(trackReviewSettingRepository, times(2)).save(any(TrackReviewSetting.class));
    }

    @Test
    void importTracksFromExcelShouldThrowWhenConferenceMissing() {
        MultipartFile file = tracksFile(List.of(row("name", "ML", "description", "x")));
        when(conferenceRepository.findById(11)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> conferenceImportService.importTracksFromExcel(11, file));
    }

    @Test
    void importTracksFromExcelShouldReturnPreviewFailureWithoutPersisting() {
        MultipartFile file = tracksFile(List.of(row("name", "", "description", "")));

        ImportResultDTO result = conferenceImportService.importTracksFromExcel(11, file);

        assertFalse(result.isSuccess());
        verify(conferenceRepository, never()).findById(any());
        verify(conferenceTrackRepository, never()).save(any());
    }

    @Test
    void generateTrackTemplateShouldReturnBytes() {
        byte[] bytes = conferenceImportService.generateTrackTemplate();
        assertTrue(bytes.length > 0);
    }

    @Test
    void previewSubjectAreasFromExcelShouldParseRows() {
        MultipartFile file = subjectAreasFile(List.of(
                row("trackName", "Machine Learning", "name", "Deep Learning", "description", "DL", "parentName", ""),
                row("trackName", "Machine Learning", "name", "Transfer Learning", "description", "TL", "parentName", "Deep Learning")
        ));

        ImportResultDTO result = conferenceImportService.previewSubjectAreasFromExcel(file);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSubjectAreaPreviews().size());
    }

    @Test
    void previewSubjectAreasFromExcelShouldFailWhenParentAppearsAfterChild() {
        MultipartFile file = subjectAreasFile(List.of(
                row("trackName", "Machine Learning", "name", "Child", "description", "d", "parentName", "Parent"),
                row("trackName", "Machine Learning", "name", "Parent", "description", "p", "parentName", "")
        ));

        ImportResultDTO result = conferenceImportService.previewSubjectAreasFromExcel(file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> "parentName".equals(e.getColumn())));
    }

    @Test
    void previewSubjectAreasFromExcelShouldFailOnDuplicatePerTrack() {
        MultipartFile file = subjectAreasFile(List.of(
                row("trackName", "Machine Learning", "name", "Deep Learning", "description", "d", "parentName", ""),
                row("trackName", "Machine Learning", "name", "Deep Learning", "description", "x", "parentName", "")
        ));

        ImportResultDTO result = conferenceImportService.previewSubjectAreasFromExcel(file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("Duplicate")));
    }

    @Test
    void importSubjectAreasFromExcelShouldCreateAndLinkParent() {
        MultipartFile file = subjectAreasFile(List.of(
                row("trackName", "Machine Learning", "name", "Deep Learning", "description", "d", "parentName", ""),
                row("trackName", "Machine Learning", "name", "Transfer Learning", "description", "t", "parentName", "Deep Learning")
        ));

        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(conferenceTrackRepository.findByConferenceAndName(conference, "Machine Learning")).thenReturn(Optional.of(track));
        when(subjectAreaRepository.save(any(SubjectArea.class))).thenAnswer(invocation -> {
            SubjectArea sa = invocation.getArgument(0);
            if (sa.getId() == null) {
                sa.setId(sa.getName().equals("Deep Learning") ? 70 : 71);
            }
            return sa;
        });

        ImportResultDTO result = conferenceImportService.importSubjectAreasFromExcel(11, file);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSubjectAreasCreated());
        verify(subjectAreaRepository, times(3)).save(any(SubjectArea.class));
    }

    @Test
    void importSubjectAreasFromExcelShouldReturnErrorWhenTrackDoesNotExist() {
        MultipartFile file = subjectAreasFile(List.of(
                row("trackName", "Unknown Track", "name", "Deep Learning", "description", "d", "parentName", "")
        ));

        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(conferenceTrackRepository.findByConferenceAndName(conference, "Unknown Track")).thenReturn(Optional.empty());

        ImportResultDTO result = conferenceImportService.importSubjectAreasFromExcel(11, file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("not found")));
    }

    @Test
    void importSubjectAreasFromExcelShouldThrowWhenConferenceMissing() {
        MultipartFile file = subjectAreasFile(List.of(
                row("trackName", "Machine Learning", "name", "Deep Learning", "description", "d", "parentName", "")
        ));
        when(conferenceRepository.findById(11)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> conferenceImportService.importSubjectAreasFromExcel(11, file));
    }

    @Test
    void generateSubjectAreaTemplateShouldReturnBytes() {
        byte[] bytes = conferenceImportService.generateSubjectAreaTemplate();
        assertTrue(bytes.length > 0);
    }

    @Test
    void previewMembersFromExcelShouldResolveExistingAndNewStatus() {
        MultipartFile file = membersFile(List.of(
                row("email", "existing@test.com", "role", "REVIEWER", "trackName", "Machine Learning"),
                row("email", "new@test.com", "role", "CONFERENCE_CHAIR", "trackName", "")
        ));

        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        ImportResultDTO result = conferenceImportService.previewMembersFromExcel(file);

        assertTrue(result.isSuccess());
        assertEquals("EXISTING", result.getMemberPreviews().get(0).get("status"));
        assertEquals("NEW", result.getMemberPreviews().get(1).get("status"));
    }

    @Test
    void previewMembersFromExcelShouldFailOnDuplicateRows() {
        MultipartFile file = membersFile(List.of(
                row("email", "dup@test.com", "role", "REVIEWER", "trackName", "Machine Learning"),
                row("email", "dup@test.com", "role", "REVIEWER", "trackName", "Machine Learning")
        ));

        ImportResultDTO result = conferenceImportService.previewMembersFromExcel(file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("Duplicate row")));
    }

    @Test
    void previewMembersFromExcelShouldFailOnInvalidRoleValue() {
        MultipartFile file = membersFile(List.of(
                row("email", "x@test.com", "role", "NOT_A_ROLE", "trackName", "Machine Learning")
        ));

        ImportResultDTO result = conferenceImportService.previewMembersFromExcel(file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> "role".equals(e.getColumn())));
    }

    @Test
    void importMembersFromExcelShouldCreateAssignmentForExistingReviewer() throws Exception {
        MultipartFile file = membersFile(List.of(
                row("email", "reviewer@test.com", "role", "REVIEWER", "trackName", "Machine Learning")
        ));

        User reviewer = new User();
        reviewer.setId(101);
        reviewer.setEmail("reviewer@test.com");
        reviewer.setFirstName("Rev");
        reviewer.setLastName("One");

        when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(conferenceTrackRepository.findByConferenceAndName(conference, "Machine Learning")).thenReturn(Optional.of(track));
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(101, 11)).thenReturn(List.of());
        when(notificationRepository.existsByUser_IdAndConference_IdAndType(101, 11, "INVITATION")).thenReturn(false);
        when(conferenceUserTrackRepository.save(any(ConferenceUserTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResultDTO result = conferenceImportService.importMembersFromExcel(11, file);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMembersCreated());
        verify(emailService).sendInvitationEmail(eq("reviewer@test.com"), any(), any(), any(), eq("Reviewer"), eq("Machine Learning"), any(), any(), any(), any());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void importMembersFromExcelShouldCreatePlaceholderForNewUser() throws Exception {
        MultipartFile file = membersFile(List.of(
                row("email", "new-reviewer@test.com", "role", "REVIEWER", "trackName", "Machine Learning")
        ));

        User newUser = new User();
        newUser.setId(301);
        newUser.setEmail("new-reviewer@test.com");

        Role authorRole = new Role();
        authorRole.setId(8);
        authorRole.setName("AUTHOR");

        when(userRepository.findByEmail("new-reviewer@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            if (u.getId() == null) {
                u.setId(newUser.getId());
            }
            return u;
        });
        when(roleRepository.findByName("AUTHOR")).thenReturn(Optional.of(authorRole));
        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(conferenceTrackRepository.findByConferenceAndName(conference, "Machine Learning")).thenReturn(Optional.of(track));
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(301, 11)).thenReturn(List.of());
        when(conferenceUserTrackRepository.save(any(ConferenceUserTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResultDTO result = conferenceImportService.importMembersFromExcel(11, file);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMembersCreated());
        verify(userRoleRepository).save(any(UserRole.class));
        verify(userProfileRepository).save(any(UserProfile.class));
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emailService).sendInvitationEmail(eq("new-reviewer@test.com"), any(), any(), any(), eq("Reviewer"), eq("Machine Learning"), any(), any(), any(), any());
    }

    @Test
    void importMembersFromExcelShouldReturnErrorWhenRoleRequiresTrackButMissing() {
        MultipartFile file = membersFile(List.of(
                row("email", "reviewer@test.com", "role", "REVIEWER", "trackName", "")
        ));

        User reviewer = new User();
        reviewer.setId(55);
        reviewer.setEmail("reviewer@test.com");

        when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));

        ImportResultDTO result = conferenceImportService.importMembersFromExcel(11, file);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getMembersCreated());
        assertTrue(result.getErrors().stream().anyMatch(e -> "trackName".equals(e.getColumn())));
    }

    @Test
    void importMembersFromExcelShouldReturnErrorWhenTrackNotFound() {
        MultipartFile file = membersFile(List.of(
                row("email", "reviewer@test.com", "role", "REVIEWER", "trackName", "Ghost Track")
        ));

        User reviewer = new User();
        reviewer.setId(56);
        reviewer.setEmail("reviewer@test.com");

        when(userRepository.findByEmail("reviewer@test.com")).thenReturn(Optional.of(reviewer));
        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(conferenceTrackRepository.findByConferenceAndName(conference, "Ghost Track")).thenReturn(Optional.empty());

        ImportResultDTO result = conferenceImportService.importMembersFromExcel(11, file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("Track not found")));
    }

    @Test
    void importMembersFromExcelShouldReturnErrorWhenDuplicateAssignmentExistsForChair() {
        MultipartFile file = membersFile(List.of(
                row("email", "chair2@test.com", "role", "CONFERENCE_CHAIR", "trackName", "")
        ));

        User chair = new User();
        chair.setId(88);
        chair.setEmail("chair2@test.com");

        ConferenceUserTrack existing = new ConferenceUserTrack();
        existing.setAssignedRole(ConferenceTrackRole.CONFERENCE_CHAIR);
        existing.setConferenceTrack(null);

        when(userRepository.findByEmail("chair2@test.com")).thenReturn(Optional.of(chair));
        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(88, 11)).thenReturn(List.of(existing));

        ImportResultDTO result = conferenceImportService.importMembersFromExcel(11, file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("already has role")));
        verify(conferenceUserTrackRepository, never()).save(any());
    }

    @Test
    void importMembersFromExcelShouldReturnErrorWhenRoleStringInvalid() {
        MultipartFile file = membersFile(List.of(
                row("email", "person@test.com", "role", "WRONG_ROLE", "trackName", "Machine Learning")
        ));

        User person = new User();
        person.setId(89);
        person.setEmail("person@test.com");

        when(userRepository.findByEmail("person@test.com")).thenReturn(Optional.of(person));

        ImportResultDTO result = conferenceImportService.importMembersFromExcel(11, file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> "role".equals(e.getColumn())));
    }

    @Test
    void importMembersFromExcelShouldSkipNotificationWhenAlreadyExists() {
        MultipartFile file = membersFile(List.of(
                row("email", "reviewer2@test.com", "role", "REVIEWER", "trackName", "Machine Learning")
        ));

        User reviewer = new User();
        reviewer.setId(102);
        reviewer.setEmail("reviewer2@test.com");
        reviewer.setFirstName("R");
        reviewer.setLastName("Two");

        when(userRepository.findByEmail("reviewer2@test.com")).thenReturn(Optional.of(reviewer));
        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(conferenceTrackRepository.findByConferenceAndName(conference, "Machine Learning")).thenReturn(Optional.of(track));
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(102, 11)).thenReturn(List.of());
        when(notificationRepository.existsByUser_IdAndConference_IdAndType(102, 11, "INVITATION")).thenReturn(true);
        when(conferenceUserTrackRepository.save(any(ConferenceUserTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResultDTO result = conferenceImportService.importMembersFromExcel(11, file);

        assertTrue(result.isSuccess());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void importMembersFromExcelShouldThrowWhenConferenceMissing() {
        MultipartFile file = membersFile(List.of(
                row("email", "reviewer@test.com", "role", "REVIEWER", "trackName", "Machine Learning")
        ));
        when(conferenceRepository.findById(11)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> conferenceImportService.importMembersFromExcel(11, file));
    }

    @Test
    void generateMemberTemplateShouldReturnBytes() {
        byte[] bytes = conferenceImportService.generateMemberTemplate();

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void previewTicketTypesFromExcelShouldParseRows() {
        MultipartFile file = ticketTypesFile(List.of(
                row("name", "Early-Bird Standard", "description", "Discounted rate", "price", "3500000", "currency", "VND", "category", "STANDARD", "maxQuantity", "150", "deadline", "2026-06-15T23:59:00Z", "active", "true"),
                row("name", "Regular Standard", "description", "Standard admission", "price", "5000000", "currency", "VND", "category", "STANDARD", "maxQuantity", "500", "deadline", "2026-08-10T23:59:00Z", "active", "true")
        ));

        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(ticketTypeRepository.findByConferenceIdAndNameIgnoreCase(eq(11), any())).thenReturn(Optional.empty());

        ImportResultDTO result = conferenceImportService.previewTicketTypesFromExcel(11, file);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTicketTypePreviews().size());
        assertEquals("Early-Bird Standard", result.getTicketTypePreviews().get(0).get("name"));
    }

    @Test
    void previewTicketTypesFromExcelShouldFailOnDuplicateNameAndInvalidFields() {
        MultipartFile file = ticketTypesFile(List.of(
                row("name", "VIP & Gala Dinner", "description", "x", "price", "-1", "currency", "VNDVND", "category", "BAD", "maxQuantity", "abc", "deadline", "bad-date", "active", "maybe"),
                row("name", "VIP & Gala Dinner", "description", "y", "price", "5000000", "currency", "VND", "category", "VIP", "maxQuantity", "50", "deadline", "2026-07-30T23:59:00Z", "active", "true")
        ));

        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(ticketTypeRepository.findByConferenceIdAndNameIgnoreCase(eq(11), any())).thenReturn(Optional.empty());

        ImportResultDTO result = conferenceImportService.previewTicketTypesFromExcel(11, file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> "name".equals(e.getColumn()) && e.getMessage().contains("Duplicate row")));
        assertTrue(result.getErrors().stream().anyMatch(e -> "price".equals(e.getColumn())));
        assertTrue(result.getErrors().stream().anyMatch(e -> "category".equals(e.getColumn())));
        assertTrue(result.getErrors().stream().anyMatch(e -> "deadline".equals(e.getColumn())));
        assertTrue(result.getErrors().stream().anyMatch(e -> "active".equals(e.getColumn())));
    }

    @Test
    void previewTicketTypesFromExcelShouldFailWhenTicketTypeAlreadyExists() {
        MultipartFile file = ticketTypesFile(List.of(
                row("name", "Regular Standard", "description", "x", "price", "5000000", "currency", "VND", "category", "STANDARD", "maxQuantity", "500", "deadline", "2026-08-10T23:59:00Z", "active", "true")
        ));

        TicketType existing = new TicketType();
        existing.setId(900);

        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(ticketTypeRepository.findByConferenceIdAndNameIgnoreCase(11, "Regular Standard")).thenReturn(Optional.of(existing));

        ImportResultDTO result = conferenceImportService.previewTicketTypesFromExcel(11, file);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("already exists")));
    }

    @Test
    void importTicketTypesFromExcelShouldCreateTicketTypes() {
        MultipartFile file = ticketTypesFile(List.of(
                row("name", "Early-Bird Standard", "description", "Discounted rate", "price", "3500000", "currency", "VND", "category", "STANDARD", "maxQuantity", "150", "deadline", "2026-06-15T23:59:00Z", "active", "true"),
                row("name", "VIP & Gala Dinner", "description", "Exclusive event", "price", "8500000", "currency", "VND", "category", "VIP", "maxQuantity", "50", "deadline", "2026-07-30T23:59:00Z", "active", "true")
        ));

        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(ticketTypeRepository.findByConferenceIdAndNameIgnoreCase(eq(11), any())).thenReturn(Optional.empty());
        when(ticketTypeRepository.save(any(TicketType.class))).thenAnswer(invocation -> {
            TicketType type = invocation.getArgument(0);
            if (type.getId() == null) {
                type.setId(type.getName().equals("Early-Bird Standard") ? 501 : 502);
            }
            return type;
        });

        ImportResultDTO result = conferenceImportService.importTicketTypesFromExcel(11, file);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTicketTypesCreated());
        verify(ticketTypeRepository, times(2)).save(any(TicketType.class));
    }

    @Test
    void generateTicketTypeTemplateShouldReturnBytes() {
        byte[] bytes = conferenceImportService.generateTicketTypeTemplate();

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void previewMembersFromExcelShouldThrowOnUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "members.csv", "text/plain", "a,b,c".getBytes());

        assertThrows(BadRequestException.class, () -> conferenceImportService.previewMembersFromExcel(file));
    }

    @Test
    void previewTracksFromExcelShouldThrowOnEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "tracks.xlsx", "application/octet-stream", new byte[0]);

        assertThrows(BadRequestException.class, () -> conferenceImportService.previewTracksFromExcel(file));
    }

    @Test
    void previewSubjectAreasFromExcelShouldThrowOnUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "sa.txt", "text/plain", "x".getBytes());

        assertThrows(BadRequestException.class, () -> conferenceImportService.previewSubjectAreasFromExcel(file));
    }

    @Test
    void importMembersFromExcelShouldAcceptConferenceChairWithoutTrack() throws Exception {
        MultipartFile file = membersFile(List.of(
                row("email", "chair3@test.com", "role", "CONFERENCE_CHAIR", "trackName", "")
        ));

        User chair = new User();
        chair.setId(104);
        chair.setEmail("chair3@test.com");
        chair.setFirstName("Chair");
        chair.setLastName("Three");

        when(userRepository.findByEmail("chair3@test.com")).thenReturn(Optional.of(chair));
        when(conferenceRepository.findById(11)).thenReturn(Optional.of(conference));
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(104, 11)).thenReturn(List.of());
        when(notificationRepository.existsByUser_IdAndConference_IdAndType(104, 11, "INVITATION")).thenReturn(false);
        when(conferenceUserTrackRepository.save(any(ConferenceUserTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ImportResultDTO result = conferenceImportService.importMembersFromExcel(11, file);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMembersCreated());
        verify(conferenceTrackRepository, never()).findByConferenceAndName(any(), any());
        verify(emailService).sendInvitationEmail(eq("chair3@test.com"), any(), any(), any(), eq("Conference Chair"), isNull(), any(), any(), any(), any());
    }

    private void setAuth(Integer userId) {
        UserDetailsImpl principal = new UserDetailsImpl(userId, "chair@test.com", "Chair", "User", "VN", "pwd", true, List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private Map<String, String> validConferenceRow() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name", "ConfHub Summit");
        row.put("acronym", "CHS2027");
        row.put("description", "A conference for testing");
        row.put("location", "HCMC");
        row.put("startDate", "2027-06-01");
        row.put("endDate", "2027-06-03");
        row.put("websiteUrl", "https://confhub.test");
        row.put("country", "Vietnam");
        row.put("province", "HCM");
        row.put("area", "CS");
        row.put("contactInformation", "info@confhub.test");
        row.put("chairEmails", "chair@test.com");
        row.put("bannerImageUrl", "https://img.test/banner.png");
        row.put("societySponsor", "IEEE");
        return row;
    }

    private MultipartFile conferenceFile(Map<String, String> row) {
        List<String> headers = List.of("name", "acronym", "description", "location", "startDate", "endDate",
                "websiteUrl", "country", "province", "area", "contactInformation", "chairEmails", "bannerImageUrl", "societySponsor");
        List<List<String>> rows = List.of(headers.stream().map(row::get).toList());
        return xlsxFile("Conference", headers, rows, "conference.xlsx");
    }

    private MultipartFile tracksFile(List<Map<String, String>> rowsMap) {
        List<String> headers = List.of("name", "description");
        List<List<String>> rows = rowsMap.stream().map(m -> List.of(m.get("name"), m.get("description"))).toList();
        return xlsxFile("Tracks", headers, rows, "tracks.xlsx");
    }

    private MultipartFile subjectAreasFile(List<Map<String, String>> rowsMap) {
        List<String> headers = List.of("trackName", "name", "description", "parentName");
        List<List<String>> rows = rowsMap.stream().map(m -> List.of(
                m.get("trackName"), m.get("name"), m.get("description"), m.get("parentName"))).toList();
        return xlsxFile("SubjectAreas", headers, rows, "subject-areas.xlsx");
    }

    private MultipartFile membersFile(List<Map<String, String>> rowsMap) {
        List<String> headers = List.of("email", "role", "trackName");
        List<List<String>> rows = rowsMap.stream().map(m -> List.of(m.get("email"), m.get("role"), m.get("trackName"))).toList();
        return xlsxFile("Members", headers, rows, "members.xlsx");
    }

    private MultipartFile ticketTypesFile(List<Map<String, String>> rowsMap) {
        List<String> headers = List.of("name", "description", "price", "currency", "category", "maxQuantity", "deadline", "active");
        List<List<String>> rows = rowsMap.stream().map(m -> List.of(
                m.get("name"), m.get("description"), m.get("price"), m.get("currency"),
                m.get("category"), m.get("maxQuantity"), m.get("deadline"), m.get("active"))).toList();
        return xlsxFile("TicketTypes", headers, rows, "ticket-types.xlsx");
    }

    private Map<String, String> row(String k1, String v1, String k2, String v2) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    private Map<String, String> row(String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    private Map<String, String> row(String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

    private Map<String, String> row(String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4,
                                    String k5, String v5, String k6, String v6, String k7, String v7, String k8, String v8) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        map.put(k8, v8);
        return map;
    }

    private MockMultipartFile xlsxFile(String sheetName, List<String> headers, List<List<String>> rows, String filename) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }

            for (int r = 0; r < rows.size(); r++) {
                Row dataRow = sheet.createRow(r + 1);
                List<String> values = rows.get(r);
                for (int c = 0; c < values.size(); c++) {
                    dataRow.createCell(c).setCellValue(values.get(c) == null ? "" : values.get(c));
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new MockMultipartFile("file", filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
