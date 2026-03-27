package com.capstone.confms.service.impl;

import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.Review;
import com.capstone.confms.entity.ReviewAnswer;
import com.capstone.confms.entity.ReviewQuestion;
import com.capstone.confms.entity.ReviewQuestionChoice;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.ReviewAnswerRepository;
import com.capstone.confms.repository.ReviewRepository;
import com.capstone.confms.utils.enums.PaperStatus;
import com.capstone.confms.utils.enums.ReviewQuestionType;
import com.capstone.confms.utils.enums.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ReviewAggregateServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewAnswerRepository reviewAnswerRepository;
    @Mock
    @SuppressWarnings("unused")
    private PaperRepository paperRepository;

    @InjectMocks
    private ReviewAggregateServiceImpl reviewAggregateService;

    private Paper paper1;
    private Paper paper2;
    private ReviewQuestion question1;
    private ReviewQuestion question2;

    @BeforeEach
    void setUp() {
        ConferenceTrack track = new ConferenceTrack();
        track.setId(1);

        paper1 = new Paper();
        paper1.setId(1);
        paper1.setTitle("Paper One");
        paper1.setStatus(PaperStatus.UNDER_REVIEW);
        paper1.setTrack(track);

        paper2 = new Paper();
        paper2.setId(2);
        paper2.setTitle("Paper Two");
        paper2.setStatus(PaperStatus.ACCEPTED);
        paper2.setTrack(track);

        question1 = new ReviewQuestion();
        question1.setId(11);
        question1.setText("Overall quality");
        question1.setType(ReviewQuestionType.OPTIONS_WITH_VALUE);

        question2 = new ReviewQuestion();
        question2.setId(22);
        question2.setText("Novelty");
        question2.setType(ReviewQuestionType.COMMENT);
    }

    @Test
    void getAggregatesByConferenceShouldReturnSortedAggregatesWithComputedScores() {
        Review review1 = buildReview(101, paper1, ReviewStatus.COMPLETED, "8");
        Review review2 = buildReview(102, paper1, ReviewStatus.COMPLETED, "6");
        Review review3 = buildReview(103, paper1, ReviewStatus.ASSIGNED, "0");
        Review review4 = buildReview(201, paper2, ReviewStatus.COMPLETED, "9");

        when(reviewRepository.findByPaper_Track_Conference_Id(7)).thenReturn(List.of(review4, review3, review2, review1));
        when(reviewAnswerRepository.findByReview_Id(101)).thenReturn(List.of(buildChoiceAnswer(question1, 4)));
        when(reviewAnswerRepository.findByReview_Id(102)).thenReturn(List.of(buildChoiceAnswer(question1, 2)));
        when(reviewAnswerRepository.findByReview_Id(201)).thenReturn(List.of(buildTextAnswer(question2)));

        var result = reviewAggregateService.getAggregatesByConference(7);

        assertNotNull(result);
        assertEquals(2, result.size());

        var paper1Aggregate = result.get(0);
        assertEquals(1, paper1Aggregate.getPaperId());
        assertEquals(3, paper1Aggregate.getReviewCount());
        assertEquals(2, paper1Aggregate.getCompletedReviewCount());
        assertEquals(new BigDecimal("7.00"), paper1Aggregate.getAverageTotalScore());
        assertEquals(1, paper1Aggregate.getQuestionAggregates().size());
        assertEquals(new BigDecimal("3.00"), paper1Aggregate.getQuestionAggregates().get(0).getAverageScore());
        assertEquals(new BigDecimal("2"), paper1Aggregate.getQuestionAggregates().get(0).getMinScore());
        assertEquals(new BigDecimal("4"), paper1Aggregate.getQuestionAggregates().get(0).getMaxScore());
        assertEquals(2, paper1Aggregate.getQuestionAggregates().get(0).getAnswerCount());

        var paper2Aggregate = result.get(1);
        assertEquals(2, paper2Aggregate.getPaperId());
        assertEquals(new BigDecimal("9.00"), paper2Aggregate.getAverageTotalScore());

        verify(reviewRepository).findByPaper_Track_Conference_Id(7);
    }

    @Test
    void getAggregateByPaperShouldReturnAggregateForPaper() {
        Review review = buildReview(301, paper1, ReviewStatus.COMPLETED, "8");
        when(reviewRepository.findByPaper_Id(1)).thenReturn(List.of(review));
        when(reviewAnswerRepository.findByReview_Id(301)).thenReturn(List.of(buildChoiceAnswer(question1, 5)));

        var result = reviewAggregateService.getAggregateByPaper(1);

        assertNotNull(result);
        assertEquals(1, result.getPaperId());
        assertEquals("Paper One", result.getPaperTitle());
        assertEquals("UNDER_REVIEW", result.getPaperStatus());
        assertEquals(1, result.getReviewCount());
        assertEquals(1, result.getCompletedReviewCount());
        assertEquals(new BigDecimal("8.00"), result.getAverageTotalScore());
        assertEquals(1, result.getQuestionAggregates().size());
        assertEquals(11, result.getQuestionAggregates().get(0).getQuestionId());

        verify(reviewRepository).findByPaper_Id(1);
    }

    @Test
    void getAggregateByPaperShouldReturnFallbackWhenNoReviews() {
        assertNotNull(paperRepository);
        when(reviewRepository.findByPaper_Id(2)).thenReturn(List.of());
        when(paperRepository.findById(2)).thenReturn(Optional.of(paper2));

        var result = reviewAggregateService.getAggregateByPaper(2);

        assertNotNull(result);
        assertEquals(2, result.getPaperId());
        assertEquals("Paper Two", result.getPaperTitle());
        assertEquals("ACCEPTED", result.getPaperStatus());
        assertEquals(0, result.getReviewCount());
        assertEquals(0, result.getCompletedReviewCount());
    }

    private Review buildReview(int id, Paper paper, ReviewStatus status, String totalScore) {
        Review review = new Review();
        review.setId(id);
        review.setPaper(paper);
        review.setStatus(status);
        review.setTotalScore(new BigDecimal(totalScore));
        return review;
    }

    private ReviewAnswer buildChoiceAnswer(ReviewQuestion question, int value) {
        ReviewQuestionChoice choice = new ReviewQuestionChoice();
        choice.setValue(value);

        ReviewAnswer answer = new ReviewAnswer();
        answer.setQuestion(question);
        answer.setSelectedChoice(choice);
        return answer;
    }

    private ReviewAnswer buildTextAnswer(ReviewQuestion question) {
        ReviewAnswer answer = new ReviewAnswer();
        answer.setQuestion(question);
        answer.setAnswerValue("5");
        return answer;
    }
}

