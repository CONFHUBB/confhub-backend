package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ReviewAnswerDTO;
import com.capstone.confhub.entity.Review;
import com.capstone.confhub.entity.ReviewAnswer;
import com.capstone.confhub.entity.ReviewQuestion;
import com.capstone.confhub.entity.ReviewQuestionChoice;
import com.capstone.confhub.repository.ReviewAnswerRepository;
import com.capstone.confhub.repository.ReviewQuestionChoiceRepository;
import com.capstone.confhub.repository.ReviewQuestionRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.utils.enums.ReviewQuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewAnswerServiceImplTest {

    @Mock
    private ReviewAnswerRepository reviewAnswerRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewQuestionRepository reviewQuestionRepository;
    @Mock
    private ReviewQuestionChoiceRepository reviewQuestionChoiceRepository;

    @InjectMocks
    private ReviewAnswerServiceImpl reviewAnswerService;

    private Review review;
    private ReviewQuestion question;
    private ReviewQuestionChoice choice;
    private ReviewAnswer answer;

    @BeforeEach
    void setUp() {
        review = new Review();
        review.setId(1);

        question = new ReviewQuestion();
        question.setId(2);
        question.setText("Overall score");
        question.setType(ReviewQuestionType.OPTIONS_WITH_VALUE);

        choice = new ReviewQuestionChoice();
        choice.setId(3);
        choice.setText("Strong accept");
        choice.setValue(5);

        answer = new ReviewAnswer();
        answer.setId(10);
        answer.setReview(review);
        answer.setQuestion(question);
        answer.setSelectedChoice(choice);
        answer.setAnswerValue("Strong accept");
        answer.setCreatedAt(LocalDateTime.now());
        answer.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void shouldCreateService() {
        assertNotNull(reviewAnswerService);
    }

    @Test
    void submitOrUpdateAnswerShouldReturnResponse() {
        ReviewAnswerDTO dto = new ReviewAnswerDTO();
        dto.setReviewId(1);
        dto.setQuestionId(2);
        dto.setAnswerValue("Strong accept");
        dto.setSelectedChoiceId(3);

        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        when(reviewQuestionRepository.findById(2)).thenReturn(Optional.of(question));
        when(reviewAnswerRepository.findByReview_IdAndQuestion_Id(1, 2)).thenReturn(Optional.empty());
        when(reviewQuestionChoiceRepository.findById(3)).thenReturn(Optional.of(choice));
        when(reviewAnswerRepository.save(any(ReviewAnswer.class))).thenReturn(answer);

        var result = reviewAnswerService.submitOrUpdateAnswer(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(3, result.getSelectedChoiceId());
    }

    @Test
    void submitBulkAnswersShouldReturnResponses() {
        ReviewAnswerDTO dto = new ReviewAnswerDTO();
        dto.setReviewId(1);
        dto.setQuestionId(2);
        dto.setAnswerValue("Strong accept");
        dto.setSelectedChoiceId(3);

        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        when(reviewQuestionRepository.findById(2)).thenReturn(Optional.of(question));
        when(reviewAnswerRepository.findByReview_IdAndQuestion_Id(1, 2)).thenReturn(Optional.empty());
        when(reviewQuestionChoiceRepository.findById(3)).thenReturn(Optional.of(choice));
        when(reviewAnswerRepository.save(any(ReviewAnswer.class))).thenReturn(answer);

        var result = reviewAnswerService.submitBulkAnswers(List.of(dto));

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAnswersByReviewShouldReturnResponses() {
        when(reviewAnswerRepository.findByReview_Id(1)).thenReturn(List.of(answer));

        var result = reviewAnswerService.getAnswersByReview(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getId());
    }

    @Test
    void getAnswerByIdShouldReturnResponse() {
        when(reviewAnswerRepository.findById(10)).thenReturn(Optional.of(answer));

        var result = reviewAnswerService.getAnswerById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void deleteAnswerShouldDelete() {
        when(reviewAnswerRepository.existsById(10)).thenReturn(true);

        reviewAnswerService.deleteAnswer(10);

        verify(reviewAnswerRepository).deleteById(10);
    }
}



