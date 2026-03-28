package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ReviewAnswerDTO;
import com.capstone.confhub.dto.response.ReviewAnswerResponseDTO;
import com.capstone.confhub.entity.Review;
import com.capstone.confhub.entity.ReviewAnswer;
import com.capstone.confhub.entity.ReviewQuestion;
import com.capstone.confhub.entity.ReviewQuestionChoice;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ReviewAnswerRepository;
import com.capstone.confhub.repository.ReviewQuestionChoiceRepository;
import com.capstone.confhub.repository.ReviewQuestionRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.service.ReviewAnswerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewAnswerServiceImpl implements ReviewAnswerService {

    private final ReviewAnswerRepository reviewAnswerRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewQuestionRepository reviewQuestionRepository;
    private final ReviewQuestionChoiceRepository reviewQuestionChoiceRepository;

    @Override
    @Transactional
    public ReviewAnswerResponseDTO submitOrUpdateAnswer(ReviewAnswerDTO dto) {
        Review review = reviewRepository.findById(dto.getReviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id " + dto.getReviewId()));

        ReviewQuestion question = reviewQuestionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewQuestion not found with id " + dto.getQuestionId()));

        // Tìm answer hiện tại hoặc tạo mới
        Optional<ReviewAnswer> existing = reviewAnswerRepository
                .findByReview_IdAndQuestion_Id(dto.getReviewId(), dto.getQuestionId());

        ReviewAnswer answer;
        if (existing.isPresent()) {
            answer = existing.get();
            answer.setUpdatedAt(LocalDateTime.now());
        } else {
            answer = new ReviewAnswer();
            answer.setReview(review);
            answer.setQuestion(question);
            answer.setCreatedAt(LocalDateTime.now());
            answer.setUpdatedAt(LocalDateTime.now());
        }

        answer.setAnswerValue(dto.getAnswerValue());

        // Set selected choice nếu có
        if (dto.getSelectedChoiceId() != null) {
            ReviewQuestionChoice choice = reviewQuestionChoiceRepository.findById(dto.getSelectedChoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ReviewQuestionChoice not found with id " + dto.getSelectedChoiceId()));
            answer.setSelectedChoice(choice);
        } else {
            answer.setSelectedChoice(null);
        }

        ReviewAnswer saved = reviewAnswerRepository.save(answer);
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public List<ReviewAnswerResponseDTO> submitBulkAnswers(List<ReviewAnswerDTO> dtos) {
        List<ReviewAnswerResponseDTO> results = new ArrayList<>();
        for (ReviewAnswerDTO dto : dtos) {
            results.add(submitOrUpdateAnswer(dto));
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewAnswerResponseDTO> getAnswersByReview(Integer reviewId) {
        return reviewAnswerRepository.findByReview_Id(reviewId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewAnswerResponseDTO getAnswerById(Integer id) {
        ReviewAnswer answer = reviewAnswerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewAnswer not found with id " + id));
        return mapToResponseDTO(answer);
    }

    @Override
    @Transactional
    public void deleteAnswer(Integer id) {
        if (!reviewAnswerRepository.existsById(id)) {
            throw new ResourceNotFoundException("ReviewAnswer not found with id " + id);
        }
        reviewAnswerRepository.deleteById(id);
    }

    private ReviewAnswerResponseDTO mapToResponseDTO(ReviewAnswer entity) {
        return ReviewAnswerResponseDTO.builder()
                .id(entity.getId())
                .reviewId(entity.getReview().getId())
                .questionId(entity.getQuestion().getId())
                .questionText(entity.getQuestion().getText())
                .questionType(entity.getQuestion().getType().name())
                .answerValue(entity.getAnswerValue())
                .selectedChoiceId(entity.getSelectedChoice() != null
                        ? entity.getSelectedChoice().getId() : null)
                .selectedChoiceText(entity.getSelectedChoice() != null
                        ? entity.getSelectedChoice().getText() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
