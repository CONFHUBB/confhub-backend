package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.PaperDTO;
import com.capstone.confhub.dto.PaperUpdateStatusDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceActivity;
import com.capstone.confhub.entity.ConferenceSubmissionForm;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperAuthor;
import com.capstone.confhub.entity.SubjectArea;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceActivityRepository;
import com.capstone.confhub.repository.ConferenceSubmissionFormRepository;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.SubjectAreaRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import com.capstone.confhub.utils.enums.PaperStatus;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperServiceImplTest {

    private static final int CONFERENCE_ID = 1;
    private static final int TRACK_ID = 2;
    private static final int PRIMARY_AREA_ID = 3;
    private static final int SUBMISSION_FORM_ID = 4;
    private static final int USER_ID = 1;
    private static final int CHAIR_ID = 9;
    private static final int PAPER_ID = 10;
    private static final String TITLE = "Paper Title";
    private static final String ABSTRACT = "Abstract";

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
    @Mock
    private TrackReviewSettingRepository trackReviewSettingRepository;
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
        conference.setId(CONFERENCE_ID);
        conference.setName("ConfHub 2025");

        track = new ConferenceTrack();
        track.setId(TRACK_ID);
        track.setConference(conference);

        primaryArea = new SubjectArea();
        primaryArea.setId(PRIMARY_AREA_ID);
        primaryArea.setName("AI");
        track.setName("AI Track");

        submissionForm = new ConferenceSubmissionForm();
        submissionForm.setId(SUBMISSION_FORM_ID);
        submissionForm.setConference(conference);
        submissionForm.setDefinitionJson("{}");

        paper = new Paper();
        paper.setId(PAPER_ID);
        paper.setTrack(track);
        paper.setPrimarySubjectArea(primaryArea);
        paper.setSecondarySubjectAreas(List.of());
        paper.setSubmissionForm(submissionForm);
        paper.setTitle(TITLE);
        paper.setAbstractField(ABSTRACT);
        paper.setKeywordsJson("[\"AI\"]");
        paper.setStatus(PaperStatus.SUBMITTED);
        paper.setSubmissionTime(Instant.now());
    }

    private void stubTrackReviewSettingLookup() {
        when(trackReviewSettingRepository.findByTrackId(TRACK_ID)).thenReturn(Optional.empty());
    }

    @Test
    void shouldCreateService() {
        assertNotNull(paperService);
    }

    @Test
    void createPaperShouldReturnResponse() {
        stubTrackReviewSettingLookup();
        PaperDTO dto = PaperDTO.builder()
                .conferenceTrackId(TRACK_ID)
                .primarySubjectAreaId(PRIMARY_AREA_ID)
                .submissionFormId(SUBMISSION_FORM_ID)
                .title(TITLE)
                .abstractField(ABSTRACT)
                .keywords(List.of("AI"))
                .build();
        ConferenceActivity activity = new ConferenceActivity();
        activity.setActivityType(ActivityType.PAPER_SUBMISSION);
        activity.setIsEnabled(true);
        activity.setDeadline(LocalDateTime.now().plusDays(1));

        when(conferenceTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(conferenceActivityRepository.findByConferenceIdAndActivityType(CONFERENCE_ID, ActivityType.PAPER_SUBMISSION)).thenReturn(Optional.of(activity));
        when(subjectAreaRepository.findById(PRIMARY_AREA_ID)).thenReturn(Optional.of(primaryArea));
        when(conferenceSubmissionFormRepository.findById(SUBMISSION_FORM_ID)).thenReturn(Optional.of(submissionForm));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> {
            Paper saved = invocation.getArgument(0);
            saved.setId(PAPER_ID);
            return saved;
        });

        var result = paperService.createPaper(dto);

        assertNotNull(result);
        assertEquals(PAPER_ID, result.getId());
        assertEquals(PaperStatus.SUBMITTED, result.getStatus());
    }

    @Test
    void createPaperShouldThrowWhenSubmissionDeadlinePassed() {
        PaperDTO dto = PaperDTO.builder()
                .conferenceTrackId(TRACK_ID)
                .primarySubjectAreaId(PRIMARY_AREA_ID)
                .submissionFormId(SUBMISSION_FORM_ID)
                .title(TITLE)
                .abstractField(ABSTRACT)
                .keywords(List.of("AI"))
                .build();
        ConferenceActivity activity = new ConferenceActivity();
        activity.setActivityType(ActivityType.PAPER_SUBMISSION);
        activity.setIsEnabled(true);
        activity.setDeadline(LocalDateTime.now().minusMinutes(1));

        when(conferenceTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(conferenceActivityRepository.findByConferenceIdAndActivityType(CONFERENCE_ID, ActivityType.PAPER_SUBMISSION)).thenReturn(Optional.of(activity));

        assertThrows(BadRequestException.class, () -> paperService.createPaper(dto));
    }

    @Test
    void updatePaperShouldReturnResponse() {
        stubTrackReviewSettingLookup();
        PaperDTO dto = PaperDTO.builder()
                .conferenceTrackId(TRACK_ID)
                .primarySubjectAreaId(PRIMARY_AREA_ID)
                .submissionFormId(SUBMISSION_FORM_ID)
                .title("Updated Title")
                .abstractField("Updated Abstract")
                .keywords(List.of("ML"))
                .build();
        ConferenceActivity activity = new ConferenceActivity();
        activity.setActivityType(ActivityType.PAPER_SUBMISSION);
        activity.setIsEnabled(true);
        activity.setDeadline(LocalDateTime.now().plusDays(1));

        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(conferenceActivityRepository.findByConferenceIdAndActivityType(CONFERENCE_ID, ActivityType.PAPER_SUBMISSION)).thenReturn(Optional.of(activity));
        when(conferenceTrackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
        when(subjectAreaRepository.findById(PRIMARY_AREA_ID)).thenReturn(Optional.of(primaryArea));
        when(conferenceSubmissionFormRepository.findById(SUBMISSION_FORM_ID)).thenReturn(Optional.of(submissionForm));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paperService.updatePaper(PAPER_ID, dto);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
    }

    @Test
    void updatePaperShouldThrowWhenPaperUnderReview() {
        paper.setStatus(PaperStatus.UNDER_REVIEW);
        PaperDTO dto = PaperDTO.builder()
                .conferenceTrackId(TRACK_ID)
                .title("Updated Title")
                .build();
        ConferenceActivity activity = new ConferenceActivity();
        activity.setActivityType(ActivityType.PAPER_SUBMISSION);
        activity.setIsEnabled(true);
        activity.setDeadline(LocalDateTime.now().plusDays(1));

        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(conferenceActivityRepository.findByConferenceIdAndActivityType(CONFERENCE_ID, ActivityType.PAPER_SUBMISSION)).thenReturn(Optional.of(activity));

        assertThrows(BadRequestException.class, () -> paperService.updatePaper(PAPER_ID, dto));
    }

    @Test
    void updatePaperStatusShouldReturnResponse() {
        stubTrackReviewSettingLookup();
        paper.setStatus(PaperStatus.SUBMITTED);
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paperService.updatePaperStatus(PAPER_ID, PaperUpdateStatusDTO.builder().status(PaperStatus.UNDER_REVIEW).build());

        assertNotNull(result);
        assertEquals(PaperStatus.UNDER_REVIEW, result.getStatus());
    }

    @Test
    void updatePaperStatusShouldThrowWhenTransitionIsInvalid() {
        paper.setStatus(PaperStatus.REJECTED);
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));

        assertThrows(BadRequestException.class,
                () -> paperService.updatePaperStatus(PAPER_ID,
                        PaperUpdateStatusDTO.builder().status(PaperStatus.UNDER_REVIEW).build()));
    }

    @Test
    void getPaperByIdShouldReturnResponse() {
        stubTrackReviewSettingLookup();
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));

        var result = paperService.getPaperById(PAPER_ID);

        assertNotNull(result);
        assertEquals(PAPER_ID, result.getId());
    }

    @Test
    void getPaperByIdShouldThrowWhenNotFound() {
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paperService.getPaperById(PAPER_ID));
    }

    @Test
    void getAllPapersShouldReturnPagedResponse() {
        stubTrackReviewSettingLookup();
        var page = new PageImpl<>(List.of(paper), PageRequest.of(0, 20), 1);
        when(paperRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = paperService.getAllPapers(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getAllPapersShouldReturnEmptyPage() {
        var page = new PageImpl<Paper>(List.of(), PageRequest.of(0, 20), 0);
        when(paperRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = paperService.getAllPapers(0, 20);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void getPapersByAuthorShouldReturnPagedResponse() {
        stubTrackReviewSettingLookup();
        PaperAuthor paperAuthor = new PaperAuthor();
        paperAuthor.setId(11);
        paperAuthor.setPaper(paper);
        var page = new PageImpl<>(List.of(paperAuthor), PageRequest.of(0, 20), 1);
        when(paperAuthorRepository.findByUserId(USER_ID, PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("createdAt").descending()))).thenReturn(page);

        var result = paperService.getPapersByAuthor(USER_ID, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getPapersByAuthorShouldReturnEmptyPage() {
        var page = new PageImpl<PaperAuthor>(List.of(), PageRequest.of(0, 20), 0);
        when(paperAuthorRepository.findByUserId(USER_ID, PageRequest.of(0, 20, org.springframework.data.domain.Sort.by("createdAt").descending()))).thenReturn(page);

        var result = paperService.getPapersByAuthor(USER_ID, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void withdrawPaperShouldReturnResponse() {
        stubTrackReviewSettingLookup();
        User chair = new User();
        chair.setId(CHAIR_ID);
        ConferenceUserTrack chairAssignment = new ConferenceUserTrack();
        chairAssignment.setUser(chair);
        chairAssignment.setConference(conference);

        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conferenceUserTrackRepository.findByConference_IdAndAssignedRole(CONFERENCE_ID, ConferenceTrackRole.CONFERENCE_CHAIR))
                .thenReturn(List.of(chairAssignment));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paperService.withdrawPaper(PAPER_ID);

        assertNotNull(result);
        assertEquals(PaperStatus.WITHDRAWN, result.getStatus());
    }

    @Test
    void withdrawPaperShouldThrowWhenStatusIsPublished() {
        paper.setStatus(PaperStatus.PUBLISHED);
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));

        assertThrows(BadRequestException.class, () -> paperService.withdrawPaper(PAPER_ID));
    }

    @Test
    void restorePaperShouldReturnResponse() {
        stubTrackReviewSettingLookup();
        paper.setStatus(PaperStatus.WITHDRAWN);
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = paperService.restorePaper(PAPER_ID);

        assertNotNull(result);
        assertEquals(PaperStatus.SUBMITTED, result.getStatus());
    }

    @Test
    void restorePaperShouldThrowWhenPaperIsNotWithdrawn() {
        paper.setStatus(PaperStatus.SUBMITTED);
        when(paperRepository.findById(PAPER_ID)).thenReturn(Optional.of(paper));

        assertThrows(BadRequestException.class, () -> paperService.restorePaper(PAPER_ID));
    }
}



