package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.ReviewService;
import com.capstone.confms.utils.PaginationUtils;
import com.capstone.confms.utils.enums.ActivityType;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import com.capstone.confms.utils.enums.ReviewStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final ConferenceActivityRepository activityRepository;
    private final ReviewAnswerRepository reviewAnswerRepository;
    private final ReviewQuestionRepository reviewQuestionRepository;
    private final NotificationRepository notificationRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;

    // BR-3.14: Valid review status transitions
    private static final Map<ReviewStatus, Set<ReviewStatus>> VALID_TRANSITIONS = Map.of(
            ReviewStatus.ASSIGNED, Set.of(ReviewStatus.IN_PROGRESS, ReviewStatus.COMPLETED, ReviewStatus.DECLINED),
            ReviewStatus.IN_PROGRESS, Set.of(ReviewStatus.COMPLETED),
            ReviewStatus.COMPLETED, Set.of(),
            ReviewStatus.DECLINED, Set.of()
    );

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponseDTO> getAllReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviews = reviewRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(reviews, this::mapToResponseDTO);
    }

    @Override
    @Transactional
    public ReviewResponseDTO createReview(ReviewDTO dto) {
        log.info("Creating new Review for paper ID: {}", dto.getPaperId());

        // BR-3.15: Check REVIEW_SUBMISSION activity enabled
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));
        validateReviewActivity(paper.getTrack().getConference().getId());

        Review review = new Review();
        mapDtoToEntity(dto, review);
        return mapToResponseDTO(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewResponseDTO updateReview(Integer id, ReviewDTO dto) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id " + id));

        // BR-3.14: Validate status transition nếu status thay đổi
        if (dto.getStatus() != null && dto.getStatus() != review.getStatus()) {
            validateStatusTransition(review.getStatus(), dto.getStatus());

            // BR-3.17: Nếu chuyển sang COMPLETED → validate required questions
            if (dto.getStatus() == ReviewStatus.COMPLETED) {
                validateRequiredQuestionsCompleted(review);
                // BR-3.18: Tính totalScore từ answers
                BigDecimal calculatedScore = calculateTotalScore(review.getId());
                review.setTotalScore(calculatedScore);
            }
        }

        mapDtoToEntity(dto, review);
        Review saved = reviewRepository.save(review);

        // Notification: review completed → notify chairs
        if (dto.getStatus() == ReviewStatus.COMPLETED) {
            try {
                Conference conference = saved.getPaper().getTrack().getConference();
                String reviewerName = saved.getReviewer().getFirstName() + " " + saved.getReviewer().getLastName();
                // Notify CONFERENCE_CHAIR + PROGRAM_CHAIR
                List<ConferenceUserTrack> chairs = conferenceUserTrackRepository
                        .findByConference_IdAndAssignedRole(conference.getId(), ConferenceTrackRole.CONFERENCE_CHAIR);
                List<ConferenceUserTrack> pChairs = conferenceUserTrackRepository
                        .findByConference_IdAndAssignedRole(conference.getId(), ConferenceTrackRole.PROGRAM_CHAIR);
                Set<Integer> notifiedIds = new java.util.HashSet<>();
                for (ConferenceUserTrack chair : chairs) {
                    if (notifiedIds.add(chair.getUser().getId())) {
                        notificationRepository.save(Notification.builder()
                                .user(chair.getUser()).conference(conference)
                                .title("Review completed by " + reviewerName)
                                .message(reviewerName + " has completed their review for \"" + saved.getPaper().getTitle() + "\".")
                                .type("REVIEW_COMPLETED")
                                .link("/conference/" + conference.getId() + "/update")
                                .isRead(false).build());
                    }
                }
                for (ConferenceUserTrack pc : pChairs) {
                    if (notifiedIds.add(pc.getUser().getId())) {
                        notificationRepository.save(Notification.builder()
                                .user(pc.getUser()).conference(conference)
                                .title("Review completed by " + reviewerName)
                                .message(reviewerName + " has completed their review for \"" + saved.getPaper().getTitle() + "\".")
                                .type("REVIEW_COMPLETED")
                                .link("/conference/" + conference.getId() + "/update")
                                .isRead(false).build());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to create review completed notification: {}", e.getMessage());
            }
        }

        return mapToResponseDTO(saved);
    }

    @Override
    public ReviewResponseDTO getReviewById(Integer id) {
        return reviewRepository.findById(id)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReview(Integer id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. Review not found with id " + id);
        }
        reviewRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByReviewerAndConference(Integer reviewerId, Integer conferenceId) {
        return reviewRepository.findByReviewer_IdAndPaper_Track_Conference_Id(reviewerId, conferenceId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByPaper(Integer paperId) {
        return reviewRepository.findByPaper_Id(paperId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    // ==================== Validation Helpers ====================

    /**
     * BR-3.14: Validate review status transition
     */
    private void validateStatusTransition(ReviewStatus current, ReviewStatus target) {
        Set<ReviewStatus> allowed = VALID_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new BadRequestException("Invalid review status transition: " + current + " → " + target);
        }
    }

    /**
     * BR-3.15: Check REVIEW_SUBMISSION activity enabled
     */
    private void validateReviewActivity(Integer conferenceId) {
        Optional<ConferenceActivity> activity = activityRepository
                .findByConferenceIdAndActivityType(conferenceId, ActivityType.REVIEW_SUBMISSION);
        if (activity.isEmpty() || !Boolean.TRUE.equals(activity.get().getIsEnabled())) {
            throw new BadRequestException("Review submission is not currently open for this conference");
        }
        if (activity.get().getDeadline() != null && activity.get().getDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Review submission deadline has passed");
        }
    }

    /**
     * BR-3.17: Validate all required questions answered before completing
     */
    private void validateRequiredQuestionsCompleted(Review review) {
        Integer trackId = review.getPaper().getTrack().getId();
        List<ReviewQuestion> requiredQuestions = reviewQuestionRepository.findByTrackIdOrderByOrderIndexAsc(trackId)
                .stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsRequired()))
                .toList();

        List<ReviewAnswer> answers = reviewAnswerRepository.findByReview_Id(review.getId());
        Set<Integer> answeredQuestionIds = answers.stream()
                .map(a -> a.getQuestion().getId())
                .collect(java.util.stream.Collectors.toSet());

        for (ReviewQuestion rq : requiredQuestions) {
            if (!answeredQuestionIds.contains(rq.getId())) {
                throw new BadRequestException(
                        "Cannot complete review: required question '" + rq.getText()
                                + "' (ID: " + rq.getId() + ") has not been answered");
            }
        }
    }

    /**
     * BR-3.18: Calculate totalScore from ReviewAnswers
     * Score = average of all numeric answer values (from selectedChoice.scoreValue)
     */
    private BigDecimal calculateTotalScore(Integer reviewId) {
        List<ReviewAnswer> answers = reviewAnswerRepository.findByReview_Id(reviewId);
        if (answers.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double totalScore = 0.0;
        int scoredAnswers = 0;
        for (ReviewAnswer answer : answers) {
            if (answer.getSelectedChoice() != null && answer.getSelectedChoice().getValue() != null) {
                totalScore += answer.getSelectedChoice().getValue().doubleValue();
                scoredAnswers++;
            }
        }

        if (scoredAnswers == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(totalScore / scoredAnswers).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // ==================== Mapping ====================

    private void mapDtoToEntity(ReviewDTO dto, Review entity) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        User reviewer = userRepository.findById(dto.getReviewerId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getReviewerId()));

        entity.setPaper(paper);
        entity.setReviewer(reviewer);
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getTotalScore() != null) {
            entity.setTotalScore(dto.getTotalScore());
        }
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewResponseDTO mapToResponseDTO(Review entity) {
        ReviewResponseDTO.PaperInfo paperInfo = null;
        if (entity.getPaper() != null) {
            paperInfo = ReviewResponseDTO.PaperInfo.builder()
                    .id(entity.getPaper().getId())
                    .title(entity.getPaper().getTitle())
                    .abstractField(entity.getPaper().getAbstractField())
                    .trackId(entity.getPaper().getTrack() != null ? entity.getPaper().getTrack().getId() : null)
                    .keywordsJson(entity.getPaper().getKeywordsJson())
                    .build();
        }

        ReviewResponseDTO.ReviewerInfo reviewerInfo = null;
        if (entity.getReviewer() != null) {
            reviewerInfo = ReviewResponseDTO.ReviewerInfo.builder()
                    .id(entity.getReviewer().getId())
                    .firstName(entity.getReviewer().getFirstName())
                    .lastName(entity.getReviewer().getLastName())
                    .email(entity.getReviewer().getEmail())
                    .build();
        }

        return ReviewResponseDTO.builder()
                .id(entity.getId())
                .paper(paperInfo)
                .reviewer(reviewerInfo)
                .status(entity.getStatus())
                .totalScore(entity.getTotalScore())
                .build();
    }
}