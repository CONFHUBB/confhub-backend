package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.*;
import com.capstone.confhub.service.ReviewCommentService;
import com.capstone.confhub.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewCommentServiceImpl implements ReviewCommentService {

    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewRepository reviewRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final ConferenceActivityRepository conferenceActivityRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewCommentResponseDTO> getAllReviewComments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewComment> reviewComments = reviewCommentRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(reviewComments, this::mapToResponseDTO);
    }

    @Override
    @Transactional
    public ReviewCommentResponseDTO createReviewComment(ReviewCommentDTO dto) {
        ReviewComment entity = new ReviewComment();
        mapDtoToEntity(dto, entity);

        // Validate: discussion posts require REVIEW_DISCUSSION activity to be active
        if (Boolean.TRUE.equals(dto.getIsDiscussionPost()) && entity.getPaper() != null) {
            Integer conferenceId = entity.getPaper().getTrack().getConference().getId();
            boolean activityEnabled = conferenceActivityRepository
                    .findByConferenceIdAndActivityType(conferenceId, ActivityType.REVIEW_DISCUSSION)
                    .map(a -> Boolean.TRUE.equals(a.getIsEnabled()))
                    .orElse(false);
            if (!activityEnabled) {
                throw new BadRequestException("Discussion phase is not active for this conference");
            }
        }

        ReviewComment saved = reviewCommentRepository.save(entity);

        // --- Notification Logic for Discussions ---
        if (Boolean.TRUE.equals(dto.getIsDiscussionPost()) && entity.getPaper() != null && entity.getUser() != null) {
            try {
                Paper paper = entity.getPaper();
                Conference conference = paper.getTrack().getConference();
                String senderName = entity.getUser().getFirstName() + " " + entity.getUser().getLastName();
                
                Set<User> usersToNotify = new HashSet<>();

                // Notify all reviewers assigned to this paper (except the sender)
                List<Review> paperReviews = reviewRepository.findByPaper_Id(paper.getId());
                for (Review r : paperReviews) {
                    if (r.getReviewer() != null && !r.getReviewer().getId().equals(entity.getUser().getId())) {
                        usersToNotify.add(r.getReviewer());
                    }
                }

                // Notify Program Chairs and Conference Chairs
                List<ConferenceUserTrack> chairs = conferenceUserTrackRepository
                        .findByConference_IdAndAssignedRole(conference.getId(), ConferenceTrackRole.CONFERENCE_CHAIR);
                List<ConferenceUserTrack> pChairs = conferenceUserTrackRepository
                        .findByConference_IdAndAssignedRole(conference.getId(), ConferenceTrackRole.PROGRAM_CHAIR);
                chairs.forEach(c -> { if (!c.getUser().getId().equals(entity.getUser().getId())) usersToNotify.add(c.getUser()); });
                pChairs.forEach(c -> { if (!c.getUser().getId().equals(entity.getUser().getId())) usersToNotify.add(c.getUser()); });

                // Construct message & link
                boolean isReply = entity.getParentCommentId() != null;
                String title = isReply ? "New Discussion Reply" : "New Discussion Comment";
                String message = senderName + (isReply ? " replied to" : " commented on") + " the discussion for paper \"" + paper.getTitle() + "\"";
                String link = "/conference/" + conference.getId() + "/reviewer";

                // Save notifications
                for (User targetUser : usersToNotify) {
                    notificationRepository.save(Notification.builder()
                            .user(targetUser)
                            .conference(conference)
                            .title(title)
                            .message(message)
                            .type(isReply ? "DISCUSSION_REPLY" : "DISCUSSION_COMMENT")
                            .link(link)
                            .isRead(false)
                            .build());
                }
            } catch (Exception e) {
                log.warn("Failed to create discussion notification: {}", e.getMessage());
            }
        }

        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public ReviewCommentResponseDTO updateReviewComment(Integer id, ReviewCommentDTO dto) {
        ReviewComment entity = reviewCommentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewComment not found with id " + id));
        entity.setContent(dto.getContent());
        entity.setTitle(dto.getTitle());
        entity.setIsVisibleToAuthor(dto.getIsVisibleToAuthor());
        entity.setUpdatedAt(LocalDateTime.now());
        return mapToResponseDTO(reviewCommentRepository.save(entity));
    }

    @Override
    public ReviewCommentResponseDTO getReviewCommentById(Integer id) {
        return reviewCommentRepository.findById(id)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewComment not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReviewComment(Integer id) {
        if (!reviewCommentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. ReviewComment not found with id " + id);
        }
        reviewCommentRepository.deleteById(id);
    }

    // ==================== DISCUSSION APIs ====================

    @Override
    @Transactional(readOnly = true)
    public List<ReviewCommentResponseDTO> getDiscussionByPaper(Integer paperId) {
        return reviewCommentRepository.findByPaper_IdAndIsDiscussionPostTrueOrderByCreatedAtAsc(paperId)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewCommentResponseDTO> getCommentsByReview(Integer reviewId) {
        return reviewCommentRepository.findByReview_IdOrderByCreatedAtAsc(reviewId)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewCommentResponseDTO> getReplies(Integer parentCommentId) {
        return reviewCommentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    // ==================== MAPPING ====================

    private void mapDtoToEntity(ReviewCommentDTO dto, ReviewComment entity) {
        if (dto.getReviewId() != null) {
            Review review = reviewRepository.findById(dto.getReviewId())
                    .orElseThrow(() -> new ResourceNotFoundException("Review not found with id " + dto.getReviewId()));
            entity.setReview(review);
        }

        if (dto.getPaperId() != null) {
            Paper paper = paperRepository.findById(dto.getPaperId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + dto.getPaperId()));
            entity.setPaper(paper);
        }

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + dto.getUserId()));
            entity.setUser(user);
        }

        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setIsVisibleToAuthor(dto.getIsVisibleToAuthor() != null ? dto.getIsVisibleToAuthor() : true);
        entity.setParentCommentId(dto.getParentCommentId());
        entity.setIsDiscussionPost(dto.getIsDiscussionPost() != null ? dto.getIsDiscussionPost() : false);

        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewCommentResponseDTO mapToResponseDTO(ReviewComment entity) {
        return ReviewCommentResponseDTO.builder()
                .id(entity.getId())
                .reviewId(entity.getReview() != null ? entity.getReview().getId() : null)
                .paperId(entity.getPaper() != null ? entity.getPaper().getId() : null)
                .paperTitle(entity.getPaper() != null ? entity.getPaper().getTitle() : null)
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userFirstName(entity.getUser() != null ? entity.getUser().getFirstName() : null)
                .userLastName(entity.getUser() != null ? entity.getUser().getLastName() : null)
                .userEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .title(entity.getTitle())
                .content(entity.getContent())
                .isVisibleToAuthor(entity.getIsVisibleToAuthor())
                .parentCommentId(entity.getParentCommentId())
                .isDiscussionPost(entity.getIsDiscussionPost())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}