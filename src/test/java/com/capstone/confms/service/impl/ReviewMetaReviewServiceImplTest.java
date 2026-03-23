package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ReviewMetaReviewDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.entity.Notification;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.PaperAuthor;
import com.capstone.confms.entity.ReviewMetaReview;
import com.capstone.confms.entity.User;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.NotificationRepository;
import com.capstone.confms.repository.PaperAuthorRepository;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.ReviewMetaReviewRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import com.capstone.confms.utils.enums.Decision;
import com.capstone.confms.utils.enums.PaperStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewMetaReviewServiceImplTest {

    @Mock
    private ReviewMetaReviewRepository reviewMetaReviewRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PaperAuthorRepository paperAuthorRepository;
    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;

    @InjectMocks
    private ReviewMetaReviewServiceImpl reviewMetaReviewService;

    private Conference conference;
    private ConferenceTrack track;
    private Paper paper;
    private User metaReviewer;
    private User author;
    private ReviewMetaReview metaReview;
    private ConferenceUserTrack chairRole;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(1);
        conference.setName("ConfMS 2025");

        track = new ConferenceTrack();
        track.setId(2);
        track.setName("AI Track");
        track.setConference(conference);

        paper = new Paper();
        paper.setId(3);
        paper.setTitle("Paper Title");
        paper.setTrack(track);
        paper.setStatus(PaperStatus.UNDER_REVIEW);

        metaReviewer = new User();
        metaReviewer.setId(4);
        metaReviewer.setFirstName("John");
        metaReviewer.setLastName("Doe");
        metaReviewer.setEmail("john@example.com");

        author = new User();
        author.setId(5);
        author.setFirstName("Jane");
        author.setLastName("Smith");
        author.setEmail("jane@example.com");

        chairRole = new ConferenceUserTrack();
        chairRole.setUser(metaReviewer);
        chairRole.setConference(conference);
        chairRole.setAssignedRole(ConferenceTrackRole.PROGRAM_CHAIR);

        metaReview = new ReviewMetaReview();
        metaReview.setId(10);
        metaReview.setPaper(paper);
        metaReview.setUser(metaReviewer);
        metaReview.setFinalDecision(Decision.APPROVE);
        metaReview.setReason("Strong paper");
    }

    @Test
    void shouldCreateService() {
        assertNotNull(reviewMetaReviewService);
    }

    @Test
    void getAllReviewMetaReviewsShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(metaReview), PageRequest.of(0, 20), 1);
        when(reviewMetaReviewRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = reviewMetaReviewService.getAllReviewMetaReviews(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createReviewMetaReviewShouldReturnResponse() {
        ReviewMetaReviewDTO dto = ReviewMetaReviewDTO.builder()
                .paperId(3)
                .userId(4)
                .finalDecision(Decision.APPROVE)
                .reason("Strong paper")
                .build();
        PaperAuthor paperAuthor = new PaperAuthor();
        paperAuthor.setUser(author);

        when(paperRepository.findById(3)).thenReturn(Optional.of(paper));
        when(userRepository.findById(4)).thenReturn(Optional.of(metaReviewer));
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(4, 1))
                .thenReturn(List.of(chairRole));
        when(reviewMetaReviewRepository.existsByPaper_Id(3)).thenReturn(false);
        when(reviewMetaReviewRepository.save(any(ReviewMetaReview.class))).thenReturn(metaReview);
        when(paperAuthorRepository.findByPaperId(3)).thenReturn(List.of(paperAuthor));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = reviewMetaReviewService.createReviewMetaReview(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(PaperStatus.ACCEPTED, paper.getStatus());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createReviewMetaReviewShouldThrowWhenDuplicate() {
        ReviewMetaReviewDTO dto = ReviewMetaReviewDTO.builder()
                .paperId(3)
                .userId(4)
                .finalDecision(Decision.APPROVE)
                .reason("Strong paper")
                .build();

        when(paperRepository.findById(3)).thenReturn(Optional.of(paper));
        when(userRepository.findById(4)).thenReturn(Optional.of(metaReviewer));
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(4, 1))
                .thenReturn(List.of(chairRole));
        when(reviewMetaReviewRepository.existsByPaper_Id(3)).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                reviewMetaReviewService.createReviewMetaReview(dto));
    }

    @Test
    void createReviewMetaReviewShouldThrowWhenNotChair() {
        ReviewMetaReviewDTO dto = ReviewMetaReviewDTO.builder()
                .paperId(3)
                .userId(4)
                .finalDecision(Decision.APPROVE)
                .reason("Strong paper")
                .build();

        when(paperRepository.findById(3)).thenReturn(Optional.of(paper));
        when(userRepository.findById(4)).thenReturn(Optional.of(metaReviewer));
        // Return empty list = user has no chair role
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(4, 1))
                .thenReturn(List.of());

        assertThrows(BadRequestException.class, () ->
                reviewMetaReviewService.createReviewMetaReview(dto));
    }

    @Test
    void createReviewMetaReviewWithRevisionShouldSetRevisionStatus() {
        ReviewMetaReviewDTO dto = ReviewMetaReviewDTO.builder()
                .paperId(3)
                .userId(4)
                .finalDecision(Decision.REVISION)
                .reason("Needs revision")
                .build();

        ReviewMetaReview revisionMetaReview = new ReviewMetaReview();
        revisionMetaReview.setId(11);
        revisionMetaReview.setPaper(paper);
        revisionMetaReview.setUser(metaReviewer);
        revisionMetaReview.setFinalDecision(Decision.REVISION);
        revisionMetaReview.setReason("Needs revision");

        PaperAuthor paperAuthor = new PaperAuthor();
        paperAuthor.setUser(author);

        when(paperRepository.findById(3)).thenReturn(Optional.of(paper));
        when(userRepository.findById(4)).thenReturn(Optional.of(metaReviewer));
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(4, 1))
                .thenReturn(List.of(chairRole));
        when(reviewMetaReviewRepository.existsByPaper_Id(3)).thenReturn(false);
        when(reviewMetaReviewRepository.save(any(ReviewMetaReview.class))).thenReturn(revisionMetaReview);
        when(paperAuthorRepository.findByPaperId(3)).thenReturn(List.of(paperAuthor));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = reviewMetaReviewService.createReviewMetaReview(dto);

        assertNotNull(result);
        assertEquals(PaperStatus.REVISION, paper.getStatus());
    }

    @Test
    void updateReviewMetaReviewShouldReturnResponse() {
        ReviewMetaReviewDTO dto = ReviewMetaReviewDTO.builder()
                .paperId(3)
                .userId(4)
                .finalDecision(Decision.REJECT)
                .reason("Not ready")
                .build();
        PaperAuthor paperAuthor = new PaperAuthor();
        paperAuthor.setUser(author);

        when(reviewMetaReviewRepository.findById(10)).thenReturn(Optional.of(metaReview));
        when(paperRepository.findById(3)).thenReturn(Optional.of(paper));
        when(userRepository.findById(4)).thenReturn(Optional.of(metaReviewer));
        when(conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(4, 1))
                .thenReturn(List.of(chairRole));
        when(reviewMetaReviewRepository.save(any(ReviewMetaReview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paperAuthorRepository.findByPaperId(3)).thenReturn(List.of(paperAuthor));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = reviewMetaReviewService.updateReviewMetaReview(10, dto);

        assertNotNull(result);
        assertEquals(Decision.REJECT, result.getFinalDecision());
        assertEquals(PaperStatus.REJECTED, paper.getStatus());
    }

    @Test
    void getReviewMetaReviewByIdShouldReturnResponse() {
        when(reviewMetaReviewRepository.findById(10)).thenReturn(Optional.of(metaReview));

        var result = reviewMetaReviewService.getReviewMetaReviewById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void deleteReviewMetaReviewShouldDelete() {
        when(reviewMetaReviewRepository.existsById(10)).thenReturn(true);

        reviewMetaReviewService.deleteReviewMetaReview(10);

        verify(reviewMetaReviewRepository).deleteById(10);
    }

    @Test
    void getMetaReviewsByConferenceShouldReturnList() {
        when(reviewMetaReviewRepository.findByPaper_Track_Conference_Id(1))
                .thenReturn(List.of(metaReview));

        var result = reviewMetaReviewService.getMetaReviewsByConference(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getId());
    }

    @Test
    void getMetaReviewByPaperShouldReturnResponse() {
        when(reviewMetaReviewRepository.findByPaper_Id(3))
                .thenReturn(Optional.of(metaReview));

        var result = reviewMetaReviewService.getMetaReviewByPaper(3);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void getMetaReviewByPaperShouldReturnNullWhenNotFound() {
        when(reviewMetaReviewRepository.findByPaper_Id(999))
                .thenReturn(Optional.empty());

        var result = reviewMetaReviewService.getMetaReviewByPaper(999);

        assertNull(result);
    }
}
