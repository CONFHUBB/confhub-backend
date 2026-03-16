package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ReviewQuestionChoiceDTO;
import com.capstone.confms.dto.ReviewQuestionDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.ReviewQuestion;
import com.capstone.confms.entity.ReviewQuestionChoice;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.ReviewQuestionRepository;
import com.capstone.confms.utils.enums.ReviewQuestionType;
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
public class ReviewQuestionServiceImplTest {

    @Mock
    private ReviewQuestionRepository reviewQuestionRepository;
    @Mock
    private ConferenceTrackRepository conferenceTrackRepository;

    @InjectMocks
    private ReviewQuestionServiceImpl reviewQuestionService;

    private ConferenceTrack sourceTrack;
    private ConferenceTrack targetTrack;
    private ReviewQuestion question;

    @BeforeEach
    void setUp() {
        Conference conference = new Conference();
        conference.setId(1);

        sourceTrack = new ConferenceTrack();
        sourceTrack.setId(10);
        sourceTrack.setConference(conference);

        targetTrack = new ConferenceTrack();
        targetTrack.setId(11);
        targetTrack.setConference(conference);

        question = new ReviewQuestion();
        question.setId(100);
        question.setTrack(sourceTrack);
        question.setText("Overall score");
        question.setType(ReviewQuestionType.OPTIONS_WITH_VALUE);
        question.setOrderIndex(1);
        question.setChoices(new java.util.ArrayList<>());

        ReviewQuestionChoice choice = new ReviewQuestionChoice();
        choice.setId(200);
        choice.setText("Strong accept");
        choice.setValue(5);
        choice.setOrderIndex(1);
        choice.setQuestion(question);
        question.getChoices().add(choice);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(reviewQuestionService);
    }

    @Test
    void getQuestionsByTrackIdShouldReturnList() {
        when(conferenceTrackRepository.existsById(10)).thenReturn(true);
        when(reviewQuestionRepository.findByTrackIdOrderByOrderIndexAsc(10)).thenReturn(List.of(question));

        var result = reviewQuestionService.getQuestionsByTrackId(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Overall score", result.get(0).getText());
    }

    @Test
    void createQuestionShouldReturnResponse() {
        ReviewQuestionDTO dto = new ReviewQuestionDTO();
        dto.setText("Overall score");
        dto.setType(ReviewQuestionType.OPTIONS_WITH_VALUE);
        ReviewQuestionChoiceDTO choiceDTO = new ReviewQuestionChoiceDTO();
        choiceDTO.setText("Strong accept");
        choiceDTO.setValue(5);
        dto.setChoices(List.of(choiceDTO));

        when(conferenceTrackRepository.findById(10)).thenReturn(Optional.of(sourceTrack));
        when(reviewQuestionRepository.countByTrackId(10)).thenReturn(0);
        when(reviewQuestionRepository.save(any(ReviewQuestion.class))).thenReturn(question);

        var result = reviewQuestionService.createQuestion(10, dto);

        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals(1, result.getChoices().size());
    }

    @Test
    void updateQuestionShouldReturnResponse() {
        ReviewQuestionDTO dto = new ReviewQuestionDTO();
        dto.setText("Updated score");
        dto.setType(ReviewQuestionType.COMMENT);

        when(reviewQuestionRepository.findById(100)).thenReturn(Optional.of(question));
        when(reviewQuestionRepository.save(any(ReviewQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = reviewQuestionService.updateQuestion(100, dto);

        assertNotNull(result);
        assertEquals("Updated score", result.getText());
        assertEquals(ReviewQuestionType.COMMENT, result.getType());
    }

    @Test
    void deleteQuestionShouldDelete() {
        when(reviewQuestionRepository.existsById(100)).thenReturn(true);

        reviewQuestionService.deleteQuestion(100);

        verify(reviewQuestionRepository).deleteById(100);
    }

    @Test
    void reorderQuestionsShouldReturnReorderedList() {
        ReviewQuestion secondQuestion = new ReviewQuestion();
        secondQuestion.setId(101);
        secondQuestion.setTrack(sourceTrack);
        secondQuestion.setText("Confidence");
        secondQuestion.setType(ReviewQuestionType.COMMENT);
        secondQuestion.setOrderIndex(2);
        secondQuestion.setChoices(new java.util.ArrayList<>());

        when(conferenceTrackRepository.existsById(10)).thenReturn(true);
        when(reviewQuestionRepository.findByTrackIdOrderByOrderIndexAsc(10)).thenReturn(List.of(question, secondQuestion));
        when(reviewQuestionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = reviewQuestionService.reorderQuestions(10, List.of(101, 100));

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, question.getOrderIndex());
        assertEquals(1, secondQuestion.getOrderIndex());
    }

    @Test
    void copyQuestionsToTrackShouldSaveCopiedQuestions() {
        when(conferenceTrackRepository.findById(10)).thenReturn(Optional.of(sourceTrack));
        when(conferenceTrackRepository.findById(11)).thenReturn(Optional.of(targetTrack));
        when(reviewQuestionRepository.findByTrackIdOrderByOrderIndexAsc(10)).thenReturn(List.of(question));
        when(reviewQuestionRepository.countByTrackId(11)).thenReturn(0);

        reviewQuestionService.copyQuestionsToTrack(10, 11);

        verify(reviewQuestionRepository).saveAll(any());
    }
}




