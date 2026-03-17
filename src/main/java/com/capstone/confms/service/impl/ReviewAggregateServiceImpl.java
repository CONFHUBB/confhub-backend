package com.capstone.confms.service.impl;

import com.capstone.confms.dto.response.ReviewAggregateDTO;
import com.capstone.confms.entity.*;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.ReviewAggregateService;
import com.capstone.confms.utils.enums.ReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewAggregateServiceImpl implements ReviewAggregateService {

    private final ReviewRepository reviewRepository;
    private final ReviewAnswerRepository reviewAnswerRepository;
    private final PaperRepository paperRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewAggregateDTO> getAggregatesByConference(Integer conferenceId) {
        List<Review> allReviews = reviewRepository.findByPaper_Track_Conference_Id(conferenceId);

        // Group reviews by paper
        Map<Integer, List<Review>> reviewsByPaper = allReviews.stream()
                .collect(Collectors.groupingBy(r -> r.getPaper().getId()));

        List<ReviewAggregateDTO> aggregates = new ArrayList<>();
        for (Map.Entry<Integer, List<Review>> entry : reviewsByPaper.entrySet()) {
            aggregates.add(buildAggregate(entry.getValue()));
        }

        // Sort by paperId
        aggregates.sort(Comparator.comparing(ReviewAggregateDTO::getPaperId));
        return aggregates;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewAggregateDTO getAggregateByPaper(Integer paperId) {
        List<Review> reviews = reviewRepository.findByPaper_Id(paperId);
        if (reviews.isEmpty()) {
            Paper paper = paperRepository.findById(paperId).orElse(null);
            return ReviewAggregateDTO.builder()
                    .paperId(paperId)
                    .paperTitle(paper != null ? paper.getTitle() : "Unknown")
                    .paperStatus(paper != null ? paper.getStatus().name() : "UNKNOWN")
                    .reviewCount(0)
                    .completedReviewCount(0)
                    .averageTotalScore(BigDecimal.ZERO)
                    .questionAggregates(List.of())
                    .build();
        }
        return buildAggregate(reviews);
    }

    private ReviewAggregateDTO buildAggregate(List<Review> reviews) {
        Paper paper = reviews.get(0).getPaper();
        int completed = (int) reviews.stream()
                .filter(r -> r.getStatus() == ReviewStatus.COMPLETED)
                .count();

        // Average total score from completed reviews
        BigDecimal avgScore = reviews.stream()
                .filter(r -> r.getStatus() == ReviewStatus.COMPLETED && r.getTotalScore() != null)
                .map(Review::getTotalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (completed > 0) {
            avgScore = avgScore.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP);
        }

        // Per-question aggregates from completed reviews
        List<Review> completedReviews = reviews.stream()
                .filter(r -> r.getStatus() == ReviewStatus.COMPLETED)
                .toList();

        Map<Integer, List<ReviewAnswer>> answersByQuestion = new HashMap<>();
        for (Review review : completedReviews) {
            List<ReviewAnswer> answers = reviewAnswerRepository.findByReview_Id(review.getId());
            for (ReviewAnswer answer : answers) {
                answersByQuestion
                        .computeIfAbsent(answer.getQuestion().getId(), k -> new ArrayList<>())
                        .add(answer);
            }
        }

        List<ReviewAggregateDTO.QuestionAggregate> questionAggregates = new ArrayList<>();
        for (Map.Entry<Integer, List<ReviewAnswer>> entry : answersByQuestion.entrySet()) {
            List<ReviewAnswer> answers = entry.getValue();
            ReviewQuestion question = answers.get(0).getQuestion();

            // Try to parse numeric values
            List<BigDecimal> numericValues = answers.stream()
                    .map(a -> {
                        try {
                            // If there's a selected choice with a value, use that
                            if (a.getSelectedChoice() != null && a.getSelectedChoice().getValue() != null) {
                                return BigDecimal.valueOf(a.getSelectedChoice().getValue());
                            }
                            // Otherwise try to parse the answer value
                            if (a.getAnswerValue() != null) {
                                return new BigDecimal(a.getAnswerValue());
                            }
                            return null;
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            BigDecimal avg = BigDecimal.ZERO, min = BigDecimal.ZERO, max = BigDecimal.ZERO;
            if (!numericValues.isEmpty()) {
                avg = numericValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(numericValues.size()), 2, RoundingMode.HALF_UP);
                min = numericValues.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                max = numericValues.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            }

            questionAggregates.add(ReviewAggregateDTO.QuestionAggregate.builder()
                    .questionId(question.getId())
                    .questionText(question.getText())
                    .questionType(question.getType().name())
                    .averageScore(avg)
                    .minScore(min)
                    .maxScore(max)
                    .answerCount(answers.size())
                    .build());
        }

        // Sort by question ID for consistent order
        questionAggregates.sort(Comparator.comparing(ReviewAggregateDTO.QuestionAggregate::getQuestionId));

        return ReviewAggregateDTO.builder()
                .paperId(paper.getId())
                .paperTitle(paper.getTitle())
                .paperStatus(paper.getStatus().name())
                .reviewCount(reviews.size())
                .completedReviewCount(completed)
                .averageTotalScore(avgScore)
                .questionAggregates(questionAggregates)
                .build();
    }
}
