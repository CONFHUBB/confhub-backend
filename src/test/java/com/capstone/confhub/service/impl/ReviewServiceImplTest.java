package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ReviewDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceActivity;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.Review;
import com.capstone.confhub.entity.ReviewAnswer;
import com.capstone.confhub.entity.ReviewQuestion;
import com.capstone.confhub.entity.ReviewQuestionChoice;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.ConferenceActivityRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewAnswerRepository;
import com.capstone.confhub.repository.ReviewQuestionRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import com.capstone.confhub.utils.enums.ReviewQuestionType;
import com.capstone.confhub.utils.enums.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private PaperRepository paperRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ConferenceActivityRepository conferenceActivityRepository;
    @Mock
    private ReviewAnswerRepository reviewAnswerRepository;
    @Mock
    private ReviewQuestionRepository reviewQuestionRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Paper paper;
    private User reviewer;
    private Review review;

    @BeforeEach
    void setUp() {
        Conference conference = new Conference();
        conference.setId(1);
        conference.setName("ConfHub 2025");

        ConferenceTrack track = new ConferenceTrack();
        track.setId(2);
        track.setConference(conference);

        paper = new Paper();
        paper.setId(3);
        paper.setTitle("Paper Title");
        paper.setTrack(track);

        reviewer = new User();
        reviewer.setId(4);
        reviewer.setFirstName("Jane");
        reviewer.setLastName("Doe");

        review = new Review();
        review.setId(10);
        review.setPaper(paper);
        review.setReviewer(reviewer);
        review.setStatus(ReviewStatus.ASSIGNED);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(reviewService);
    }

    @Test
    void getAllReviewsShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(review), PageRequest.of(0, 20), 1);
        when(reviewRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = reviewService.getAllReviews(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createReviewShouldReturnResponse() {
        ReviewDTO dto = ReviewDTO.builder().paperId(3).reviewerId(4).status(ReviewStatus.ASSIGNED).build();
        ConferenceActivity activity = new ConferenceActivity();
        activity.setActivityType(ActivityType.REVIEW_SUBMISSION);
        activity.setIsEnabled(true);
        activity.setDeadline(LocalDateTime.now().plusDays(1));

        when(paperRepository.findById(3)).thenReturn(Optional.of(paper));
        when(conferenceActivityRepository.findByConferenceIdAndActivityType(1, ActivityType.REVIEW_SUBMISSION)).thenReturn(Optional.of(activity));
        when(userRepository.findById(4)).thenReturn(Optional.of(reviewer));
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        var result = reviewService.createReview(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void getReviewByIdShouldReturnResponse() {
        when(reviewRepository.findById(10)).thenReturn(Optional.of(review));

        var result = reviewService.getReviewById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void deleteReviewShouldDelete() {
        when(reviewRepository.existsById(10)).thenReturn(true);

        reviewService.deleteReview(10);

        verify(reviewRepository).deleteById(10);
    }
}



