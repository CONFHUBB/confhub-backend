package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.*;
import com.capstone.confhub.service.ReviewMetaReviewService;
import com.capstone.confhub.utils.PaginationUtils;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import com.capstone.confhub.utils.enums.Decision;
import com.capstone.confhub.utils.enums.PaperStatus;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewMetaReviewServiceImpl implements ReviewMetaReviewService {

    private final ReviewMetaReviewRepository reviewMetaReviewRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewMetaReviewResponseDTO> getAllReviewMetaReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewMetaReview> reviewMetaReviews = reviewMetaReviewRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(reviewMetaReviews, this::mapToReviewMetaReviewResponseDTO);
    }

    @Override
    @Transactional
    public ReviewMetaReviewResponseDTO createReviewMetaReview(ReviewMetaReviewDTO dto) {
        // Validate paper exists
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        // Validate user exists
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getUserId()));

        // BR: Role authorization — only PROGRAM_CHAIR or CONFERENCE_CHAIR can create meta-review
        validateChairRole(user.getId(), paper);

        // BR: Unique constraint — only 1 meta-review per paper
        if (reviewMetaReviewRepository.existsByPaper_Id(dto.getPaperId())) {
            throw new BadRequestException(
                    "Meta-review already exists for paper ID " + dto.getPaperId() + ". Use update instead.");
        }

        ReviewMetaReview entity = new ReviewMetaReview();
        mapDtoToReviewMetaReviewEntity(dto, entity, paper, user);
        ReviewMetaReview saved = reviewMetaReviewRepository.save(entity);

        // BR-3.21: Update paper status based on decision
        updatePaperStatusFromDecision(saved.getPaper(), saved.getFinalDecision());

        // Notification: notify authors about the decision
        notifyAuthorsAboutDecision(saved);

        return mapToReviewMetaReviewResponseDTO(saved);
    }

    @Override
    @Transactional
    public ReviewMetaReviewResponseDTO updateReviewMetaReview(Integer id, ReviewMetaReviewDTO dto) {
        ReviewMetaReview entity = reviewMetaReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewMetaReview not found with id " + id));

        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getUserId()));

        // BR: Role authorization
        validateChairRole(user.getId(), paper);

        mapDtoToReviewMetaReviewEntity(dto, entity, paper, user);
        ReviewMetaReview saved = reviewMetaReviewRepository.save(entity);

        // BR-3.21: Update paper status on decision change
        updatePaperStatusFromDecision(saved.getPaper(), saved.getFinalDecision());

        // Notification: notify authors about the updated decision
        notifyAuthorsAboutDecision(saved);

        return mapToReviewMetaReviewResponseDTO(saved);
    }

    @Override
    public ReviewMetaReviewResponseDTO getReviewMetaReviewById(Integer id) {
        return reviewMetaReviewRepository.findById(id)
                .map(this::mapToReviewMetaReviewResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewMetaReview not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReviewMetaReview(Integer id) {
        if (!reviewMetaReviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. ReviewMetaReview not found with id " + id);
        }
        reviewMetaReviewRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewMetaReviewResponseDTO> getMetaReviewsByConference(Integer conferenceId) {
        return reviewMetaReviewRepository.findByPaper_Track_Conference_Id(conferenceId)
                .stream()
                .map(this::mapToReviewMetaReviewResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewMetaReviewResponseDTO getMetaReviewByPaper(Integer paperId) {
        return reviewMetaReviewRepository.findByPaper_Id(paperId)
                .map(this::mapToReviewMetaReviewResponseDTO)
                .orElse(null);
    }

    /**
     * Validate that the user has PROGRAM_CHAIR or CONFERENCE_CHAIR role
     * in the conference that the paper belongs to.
     */
    private void validateChairRole(Integer userId, Paper paper) {
        Integer conferenceId = paper.getTrack().getConference().getId();
        List<ConferenceUserTrack> userRoles = conferenceUserTrackRepository
                .findAllByUser_IdAndConference_Id(userId, conferenceId);

        boolean isChair = userRoles.stream()
                .anyMatch(cut -> cut.getAssignedRole() == ConferenceTrackRole.PROGRAM_CHAIR
                        || cut.getAssignedRole() == ConferenceTrackRole.CONFERENCE_CHAIR);

        if (!isChair) {
            throw new BadRequestException(
                    "Only Program Chair or Conference Chair can create/update meta-reviews.");
        }
    }

    /**
     * BR-3.21: Map meta-review decision → paper status
     */
    private void updatePaperStatusFromDecision(Paper paper, Decision decision) {
        PaperStatus newStatus = switch (decision) {
            case APPROVE -> PaperStatus.ACCEPTED;
            case REJECT -> PaperStatus.REJECTED;
        };

        if (newStatus != paper.getStatus()) {
            log.info("Meta-review decision {} → updating paper {} status from {} to {}",
                    decision, paper.getId(), paper.getStatus(), newStatus);
            paper.setStatus(newStatus);
            paper.setUpdatedAt(LocalDateTime.now());
            paperRepository.save(paper);
        }
    }

    private void mapDtoToReviewMetaReviewEntity(ReviewMetaReviewDTO dto, ReviewMetaReview entity,
            Paper paper, User user) {
        entity.setPaper(paper);
        entity.setUser(user);
        entity.setFinalDecision(dto.getFinalDecision());
        entity.setReason(dto.getReason());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewMetaReviewResponseDTO mapToReviewMetaReviewResponseDTO(ReviewMetaReview entity) {
        Paper paper = entity.getPaper();
        User user = entity.getUser();
        ConferenceTrack track = paper.getTrack();

        return ReviewMetaReviewResponseDTO.builder()
                .id(entity.getId())
                .paper(ReviewMetaReviewResponseDTO.PaperInfo.builder()
                        .id(paper.getId())
                        .title(paper.getTitle())
                        .status(paper.getStatus().name())
                        .trackId(track != null ? track.getId() : null)
                        .trackName(track != null ? track.getName() : null)
                        .build())
                .user(ReviewMetaReviewResponseDTO.UserInfo.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .build())
                .finalDecision(entity.getFinalDecision())
                .reason(entity.getReason())
                .build();
    }

    private void notifyAuthorsAboutDecision(ReviewMetaReview metaReview) {
        try {
            Paper paper = metaReview.getPaper();
            Conference conference = paper.getTrack().getConference();
            Decision decision = metaReview.getFinalDecision();

            String decisionText;
            switch (decision) {
                case APPROVE -> decisionText = "ACCEPTED";
                case REJECT -> decisionText = "REJECTED";
            default -> {
                return;
            }
            }

            String title = "Paper " + decisionText + ": " + paper.getTitle();
            String message = "Your paper \"" + paper.getTitle() + "\" in \"" + conference.getName()
                    + "\" has been " + decisionText.toLowerCase() + ".";
            if (metaReview.getReason() != null && !metaReview.getReason().isBlank()) {
                message += " Reason: " + metaReview.getReason();
            }

            // Get all authors of the paper
            java.util.Set<Integer> notifiedIds = new java.util.HashSet<>();
            var paperAuthors = paperAuthorRepository.findByPaperId(paper.getId());
            for (var pa : paperAuthors) {
                if (pa.getUser() != null && notifiedIds.add(pa.getUser().getId())) {
                    User author = pa.getUser();
                    if (author != null) {
                        notificationRepository.save(Notification.builder()
                                .user(author)
                                .conference(conference)
                                .title(title)
                                .message(message)
                                .type("PAPER_DECISION")
                                .link("/paper/" + paper.getId())
                                .isRead(false)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to create meta-review decision notification: {}", e.getMessage());
        }
    }
}