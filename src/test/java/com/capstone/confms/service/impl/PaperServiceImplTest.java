package com.capstone.confms.service.impl;

import com.capstone.confms.dto.PaperDTO;
import com.capstone.confms.dto.PaperUpdateStatusDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceActivity;
import com.capstone.confms.entity.ConferenceSubmissionForm;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.entity.Notification;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.PaperAuthor;
import com.capstone.confms.entity.SubjectArea;
import com.capstone.confms.entity.User;
import com.capstone.confms.repository.ConferenceActivityRepository;
import com.capstone.confms.repository.ConferenceSubmissionFormRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.NotificationRepository;
import com.capstone.confms.repository.PaperAuthorRepository;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.ReviewRepository;
import com.capstone.confms.repository.SubjectAreaRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.utils.enums.ActivityType;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import com.capstone.confms.utils.enums.PaperStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperServiceImplTest {

    @Mock
    private PaperRepository paperRepository;
    @Mock
    private ConferenceTrackRepository conferenceTrackRepository;
    @Mock
    private SubjectAreaRepository subjectAreaRepository;
    @Mock
    private PaperAuthorRepository paperAuthorRepository;
    @Mock
    private ConferenceSubmissionFormRepository conferenceSubmissionFormRepository;
    @Mock
    private ConferenceActivityRepository conferenceActivityRepository;
    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaperServiceImpl paperService;

    private Conference conference;
    private ConferenceTrack track;
    private SubjectArea primaryArea;
    private ConferenceSubmissionForm submissionForm;
    private Paper paper;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(1);
        conference.setName("ConfMS 2025");

        track = new ConferenceTrack();
        track.setId(2);
        track.setConference(conference);

        primaryArea = new SubjectArea();
        primaryArea.setId(3);
        primaryArea.setName("AI");

        submissionForm = new ConferenceSubmissionForm();
        submissionForm.setId(4);
        submissionForm.setConference(conference);
        submissionForm.setDefinitionJson("{}");

        paper = new Paper();
        paper.setId(10);
        paper.setTrack(track);
        paper.setPrimarySubjectArea(primaryArea);
        paper.setSecondarySubjectAreas(List.of());
        paper.setSubmissionForm(submissionForm);
        paper.setTitle("Paper Title");
        paper.setAbstractField("Abstract");
        paper.setKeywordsJson("[\"AI\"]");
        paper.setStatus(PaperStatus.SUBMITTED);
        paper.setSubmissionTime(Instant.now());
    }

    @Test
    void shouldCreateService() {
        assertNotNull(paperService);
    }

    @Test
    void createPaperShouldReturnResponse() {
        PaperDTO dto = PaperDTO.builder()
                .conferenceTrackId(2)
                .primarySubjectAreaId(3)
                .submissionFormId(4)
                .title("Paper Title")
                .abstractField("Abstract")
                .keywords(List.of("AI"))
                .build();
        ConferenceActivity activity = new ConferenceActivity();
        activity.setActivityType(ActivityType.PAPER_SUBMISSION);
        activity.setIsEnabled(true);
        activity.setDeadline(LocalDateTime.now().plusDays(1));

        when(conferenceTrackRepository.findById(2)).thenReturn(Optional.of(track));
        when(conferenceActivityRepository.findByConferenceIdAndActivityType(1, ActivityType.PAPER_SUBMISSION)).thenReturn(Optional.of(activity));
        when(subjectAreaRepository.findById(3)).thenReturn(Optional.of(primaryArea));
        when(conferenceSubmissionFormRepository.findById(4)).thenReturn(Optional.of(submissionForm));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> {
            Paper saved = invocation.getArgument(0);
            saved.setId(10);
            return saved;
        });

        var result = paperService.createPaper(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(PaperStatus.SUBMITTED, result.getStatus());
    }

    @Test
    void updatePaperShouldReturnResponse() {
        PaperDTO dto = PaperDTO.builder()
                .conferenceTrackId(2)
                .primarySubjectAreaId(3)
                .submissionFormId(4)
                .title("Updated Title")
                .abstractField("Updated Abstract")
                .keywords(List.of("ML"))
                .build();
        ConferenceActivity activity = new ConferenceActivity();
        activity.setActivityType(ActivityType.PAPER_SUBMISSION);
        activity.setIsEnabled(true);
        activity.setDeadline(LocalDateTime.now().plusDays(1));

        when(paperRepository.findById(10)).thenReturn(Optional.of(paper));
        when(conferenceActivityRepository.findByConferenceIdAndActivityType(1, ActivityType.PAPER_SUBMISSION)).thenReturn(Optional.of(activity));
        when(conferenceTrackRepository.findById(2)).thenReturn(Optional.of(track));
        when(subjectAreaRepository.findById(3)).thenReturn(Optional.of(primaryArea));
        when(conferenceSubmissionFormRepository.findById(4)).thenReturn(Optional.of(submissionForm));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paperService.updatePaper(10, dto);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
    }

    @Test
    void updatePaperStatusShouldReturnResponse() {
        paper.setStatus(PaperStatus.SUBMITTED);
        when(paperRepository.findById(10)).thenReturn(Optional.of(paper));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paperService.updatePaperStatus(10, PaperUpdateStatusDTO.builder().status(PaperStatus.UNDER_REVIEW).build());

        assertNotNull(result);
        assertEquals(PaperStatus.UNDER_REVIEW, result.getStatus());
    }

    @Test
    void getPaperByIdShouldReturnResponse() {
        when(paperRepository.findById(10)).thenReturn(Optional.of(paper));

        var result = paperService.getPaperById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void getAllPapersShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(paper), PageRequest.of(0, 20), 1);
        when(paperRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = paperService.getAllPapers(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getPapersByAuthorShouldReturnPagedResponse() {
        PaperAuthor paperAuthor = new PaperAuthor();
        paperAuthor.setId(11);
        paperAuthor.setPaper(paper);
        var page = new PageImpl<>(List.of(paperAuthor), PageRequest.of(0, 20), 1);
        when(paperAuthorRepository.findByUserId(1, PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("createdAt").descending()))).thenReturn(page);

        var result = paperService.getPapersByAuthor(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void deletePaperShouldDelete() {
        when(paperRepository.findById(10)).thenReturn(Optional.of(paper));
        when(reviewRepository.countByPaper_Id(10)).thenReturn(0L);

        paperService.deletePaper(10);

        verify(paperRepository).deleteById(10);
    }

    @Test
    void withdrawPaperShouldReturnResponse() {
        User chair = new User();
        chair.setId(9);
        ConferenceUserTrack chairAssignment = new ConferenceUserTrack();
        chairAssignment.setUser(chair);
        chairAssignment.setConference(conference);

        when(paperRepository.findById(10)).thenReturn(Optional.of(paper));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(1, ConferenceTrackRole.CONFERENCE_CHAIR))
                .thenReturn(List.of(chairAssignment));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paperService.withdrawPaper(10);

        assertNotNull(result);
        assertEquals(PaperStatus.WITHDRAWN, result.getStatus());
    }

    @Test
    void restorePaperShouldReturnResponse() {
        paper.setStatus(PaperStatus.WITHDRAWN);
        when(paperRepository.findById(10)).thenReturn(Optional.of(paper));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paperService.restorePaper(10);

        assertNotNull(result);
        assertEquals(PaperStatus.SUBMITTED, result.getStatus());
    }
}



