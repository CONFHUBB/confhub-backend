package com.capstone.confms.service;

import com.capstone.confms.dto.ReviewQuestionDTO;

import java.util.List;

public interface ReviewQuestionService {

    List<ReviewQuestionDTO> getQuestionsByTrackId(Integer trackId);

    ReviewQuestionDTO createQuestion(Integer trackId, ReviewQuestionDTO dto);

    ReviewQuestionDTO updateQuestion(Integer questionId, ReviewQuestionDTO dto);

    void deleteQuestion(Integer questionId);

    List<ReviewQuestionDTO> reorderQuestions(Integer trackId, List<Integer> questionIds);

    void copyQuestionsToTrack(Integer sourceTrackId, Integer targetTrackId);
}
