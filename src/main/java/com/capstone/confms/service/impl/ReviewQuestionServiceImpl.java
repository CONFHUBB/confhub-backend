package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ReviewQuestionChoiceDTO;
import com.capstone.confms.dto.ReviewQuestionDTO;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.ReviewQuestion;
import com.capstone.confms.entity.ReviewQuestionChoice;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.ReviewQuestionRepository;
import com.capstone.confms.service.ReviewQuestionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewQuestionServiceImpl implements ReviewQuestionService {

    private final ReviewQuestionRepository questionRepository;
    private final ConferenceTrackRepository trackRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewQuestionDTO> getQuestionsByTrackId(Integer trackId) {
        if (!trackRepository.existsById(trackId)) {
            throw new EntityNotFoundException("Track not found with ID: " + trackId);
        }
        List<ReviewQuestion> questions = questionRepository.findByTrackIdOrderByOrderIndexAsc(trackId);
        return questions.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewQuestionDTO createQuestion(Integer trackId, ReviewQuestionDTO dto) {
        ConferenceTrack track = trackRepository.findById(trackId)
                .orElseThrow(() -> new EntityNotFoundException("Track not found with ID: " + trackId));

        Integer currentCount = questionRepository.countByTrackId(trackId);

        ReviewQuestion question = new ReviewQuestion();
        question.setTrack(track);
        mapDtoToEntity(dto, question);

        // Auto-set order index to end of list if not provided
        if (dto.getOrderIndex() == null) {
            question.setOrderIndex(currentCount + 1);
        }

        // Handle choices
        if (dto.getChoices() != null && !dto.getChoices().isEmpty()) {
            for (int i = 0; i < dto.getChoices().size(); i++) {
                ReviewQuestionChoiceDTO choiceDTO = dto.getChoices().get(i);
                ReviewQuestionChoice choice = new ReviewQuestionChoice();
                choice.setText(choiceDTO.getText());
                choice.setValue(choiceDTO.getValue());
                choice.setOrderIndex(choiceDTO.getOrderIndex() != null ? choiceDTO.getOrderIndex() : i + 1);
                choice.setQuestion(question);
                question.getChoices().add(choice);
            }
        }

        ReviewQuestion saved = questionRepository.save(question);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public ReviewQuestionDTO updateQuestion(Integer questionId, ReviewQuestionDTO dto) {
        ReviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new EntityNotFoundException("Review question not found with ID: " + questionId));

        mapDtoToEntity(dto, question);

        // Handle choices: clear old and add new
        question.getChoices().clear();
        if (dto.getChoices() != null) {
            for (int i = 0; i < dto.getChoices().size(); i++) {
                ReviewQuestionChoiceDTO choiceDTO = dto.getChoices().get(i);
                ReviewQuestionChoice choice = new ReviewQuestionChoice();
                choice.setText(choiceDTO.getText());
                choice.setValue(choiceDTO.getValue());
                choice.setOrderIndex(choiceDTO.getOrderIndex() != null ? choiceDTO.getOrderIndex() : i + 1);
                choice.setQuestion(question);
                question.getChoices().add(choice);
            }
        }

        ReviewQuestion saved = questionRepository.save(question);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteQuestion(Integer questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new EntityNotFoundException("Review question not found with ID: " + questionId);
        }
        questionRepository.deleteById(questionId);
    }

    @Override
    @Transactional
    public List<ReviewQuestionDTO> reorderQuestions(Integer trackId, List<Integer> questionIds) {
        if (!trackRepository.existsById(trackId)) {
            throw new EntityNotFoundException("Track not found with ID: " + trackId);
        }

        List<ReviewQuestion> questions = questionRepository.findByTrackIdOrderByOrderIndexAsc(trackId);

        for (int i = 0; i < questionIds.size(); i++) {
            Integer qId = questionIds.get(i);
            for (ReviewQuestion q : questions) {
                if (q.getId().equals(qId)) {
                    q.setOrderIndex(i + 1);
                    break;
                }
            }
        }

        questionRepository.saveAll(questions);
        return questions.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void copyQuestionsToTrack(Integer sourceTrackId, Integer targetTrackId) {
        if (sourceTrackId.equals(targetTrackId)) {
            throw new IllegalArgumentException("Source and target track IDs cannot be the same");
        }

        ConferenceTrack sourceTrack = trackRepository.findById(sourceTrackId)
                .orElseThrow(() -> new EntityNotFoundException("Source track not found with ID: " + sourceTrackId));
        ConferenceTrack targetTrack = trackRepository.findById(targetTrackId)
                .orElseThrow(() -> new EntityNotFoundException("Target track not found with ID: " + targetTrackId));

        if (!sourceTrack.getConference().getId().equals(targetTrack.getConference().getId())) {
            throw new IllegalArgumentException("Cannot copy questions between different conferences");
        }

        List<ReviewQuestion> sourceQuestions = questionRepository.findByTrackIdOrderByOrderIndexAsc(sourceTrackId);
        Integer existingCount = questionRepository.countByTrackId(targetTrackId);

        List<ReviewQuestion> newQuestions = new ArrayList<>();
        int offset = existingCount;

        for (ReviewQuestion src : sourceQuestions) {
            ReviewQuestion copy = new ReviewQuestion();
            copy.setTrack(targetTrack);
            copy.setText(src.getText());
            copy.setNote(src.getNote());
            copy.setType(src.getType());
            copy.setOrderIndex(++offset);
            copy.setMaxLength(src.getMaxLength());
            copy.setShowAs(src.getShowAs());
            copy.setIsRequired(src.getIsRequired());
            copy.setLockedForEdit(src.getLockedForEdit());
            copy.setVisibleToOtherReviewers(src.getVisibleToOtherReviewers());
            copy.setVisibleToAuthorsDuringFeedback(src.getVisibleToAuthorsDuringFeedback());
            copy.setVisibleToAuthorsAfterNotification(src.getVisibleToAuthorsAfterNotification());
            copy.setVisibleToMetaReviewers(src.getVisibleToMetaReviewers());
            copy.setVisibleToSeniorMetaReviewers(src.getVisibleToSeniorMetaReviewers());

            // Copy choices
            for (ReviewQuestionChoice srcChoice : src.getChoices()) {
                ReviewQuestionChoice copyChoice = new ReviewQuestionChoice();
                copyChoice.setText(srcChoice.getText());
                copyChoice.setValue(srcChoice.getValue());
                copyChoice.setOrderIndex(srcChoice.getOrderIndex());
                copyChoice.setQuestion(copy);
                copy.getChoices().add(copyChoice);
            }

            newQuestions.add(copy);
        }

        questionRepository.saveAll(newQuestions);
    }

    // ========== Mapping Helpers ==========

    private void mapDtoToEntity(ReviewQuestionDTO dto, ReviewQuestion entity) {
        if (dto.getText() != null) entity.setText(dto.getText());
        if (dto.getNote() != null) entity.setNote(dto.getNote());
        if (dto.getType() != null) entity.setType(dto.getType());
        if (dto.getOrderIndex() != null) entity.setOrderIndex(dto.getOrderIndex());
        if (dto.getMaxLength() != null) entity.setMaxLength(dto.getMaxLength());
        if (dto.getShowAs() != null) entity.setShowAs(dto.getShowAs());
        if (dto.getIsRequired() != null) entity.setIsRequired(dto.getIsRequired());
        if (dto.getLockedForEdit() != null) entity.setLockedForEdit(dto.getLockedForEdit());
        if (dto.getVisibleToOtherReviewers() != null) entity.setVisibleToOtherReviewers(dto.getVisibleToOtherReviewers());
        if (dto.getVisibleToAuthorsDuringFeedback() != null) entity.setVisibleToAuthorsDuringFeedback(dto.getVisibleToAuthorsDuringFeedback());
        if (dto.getVisibleToAuthorsAfterNotification() != null) entity.setVisibleToAuthorsAfterNotification(dto.getVisibleToAuthorsAfterNotification());
        if (dto.getVisibleToMetaReviewers() != null) entity.setVisibleToMetaReviewers(dto.getVisibleToMetaReviewers());
        if (dto.getVisibleToSeniorMetaReviewers() != null) entity.setVisibleToSeniorMetaReviewers(dto.getVisibleToSeniorMetaReviewers());
    }

    private ReviewQuestionDTO mapToDTO(ReviewQuestion entity) {
        ReviewQuestionDTO dto = new ReviewQuestionDTO();
        dto.setId(entity.getId());
        dto.setTrackId(entity.getTrack().getId());
        dto.setText(entity.getText());
        dto.setNote(entity.getNote());
        dto.setType(entity.getType());
        dto.setOrderIndex(entity.getOrderIndex());
        dto.setMaxLength(entity.getMaxLength());
        dto.setShowAs(entity.getShowAs());
        dto.setIsRequired(entity.getIsRequired());
        dto.setLockedForEdit(entity.getLockedForEdit());
        dto.setVisibleToOtherReviewers(entity.getVisibleToOtherReviewers());
        dto.setVisibleToAuthorsDuringFeedback(entity.getVisibleToAuthorsDuringFeedback());
        dto.setVisibleToAuthorsAfterNotification(entity.getVisibleToAuthorsAfterNotification());
        dto.setVisibleToMetaReviewers(entity.getVisibleToMetaReviewers());
        dto.setVisibleToSeniorMetaReviewers(entity.getVisibleToSeniorMetaReviewers());

        if (entity.getChoices() != null && !entity.getChoices().isEmpty()) {
            List<ReviewQuestionChoiceDTO> choiceDTOs = entity.getChoices().stream().map(choice -> {
                ReviewQuestionChoiceDTO cDto = new ReviewQuestionChoiceDTO();
                cDto.setId(choice.getId());
                cDto.setText(choice.getText());
                cDto.setValue(choice.getValue());
                cDto.setOrderIndex(choice.getOrderIndex());
                return cDto;
            }).collect(Collectors.toList());
            dto.setChoices(choiceDTOs);
        }

        return dto;
    }
}
