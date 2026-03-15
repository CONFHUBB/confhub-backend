package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.AutoAssignConfigDTO;
import com.capstone.confms.dto.response.AssignmentPreviewDTO;
import com.capstone.confms.dto.response.AssignmentPreviewItemDTO;
import com.capstone.confms.entity.Bidding;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.entity.Notification;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.Review;
import com.capstone.confms.entity.SubjectArea;
import com.capstone.confms.entity.User;
import com.capstone.confms.utils.enums.BidValue;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.BiddingRepository;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.NotificationRepository;
import com.capstone.confms.repository.PaperConflictRepository;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.ReviewRepository;
import com.capstone.confms.repository.ReviewerInterestRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.ReviewerAssignmentService;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import com.capstone.confms.utils.enums.ReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewerAssignmentServiceImpl implements ReviewerAssignmentService {

    private final ReviewRepository reviewRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final BiddingRepository biddingRepository;
    private final PaperConflictRepository paperConflictRepository;
    private final ReviewerInterestRepository reviewerInterestRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public AssignmentPreviewDTO runAutoAssign(AutoAssignConfigDTO config) {
        Integer conferenceId = config.getConferenceId();
        double bidWeight = config.getBidWeight() != null ? config.getBidWeight() : 0.6;
        double relevanceWeight = config.getRelevanceWeight() != null ? config.getRelevanceWeight() : 0.4;

        // 1. Lấy tất cả papers trong conference
        List<Paper> papers = paperRepository.findByTrack_Conference_Id(conferenceId);
        if (papers.isEmpty()) {
            throw new BadRequestException("No papers found in this conference");
        }

        // 2. Lấy tất cả reviewers trong conference
        List<ConferenceUserTrack> reviewerTracks = conferenceUserTrackRepository
                .findByConference_IdAndAssignedRole(conferenceId, ConferenceTrackRole.REVIEWER);
        List<User> reviewers = reviewerTracks.stream()
                .map(ConferenceUserTrack::getUser)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();

        if (reviewers.isEmpty()) {
            throw new BadRequestException("No reviewers found in this conference");
        }

        // 3. Pre-compute: bids, conflicts, subject areas
        Map<String, BidValue> bidMap = new HashMap<>(); // key = "paperId-reviewerId"
        for (Paper paper : papers) {
            List<Bidding> paperBids = biddingRepository.findByPaper_Id(paper.getId());
            for (Bidding bid : paperBids) {
                bidMap.put(paper.getId() + "-" + bid.getReviewer().getId(), bid.getBidValue());
            }
        }

        // Map reviewer subject area IDs
        Map<Integer, Set<Integer>> reviewerSubjectAreas = new HashMap<>();
        for (User reviewer : reviewers) {
            Set<Integer> saIds = reviewerInterestRepository.findByReviewer_Id(reviewer.getId())
                    .stream()
                    .map(ri -> ri.getSubjectArea().getId())
                    .collect(Collectors.toSet());
            reviewerSubjectAreas.put(reviewer.getId(), saIds);
        }

        // 4. Build candidate list: score mỗi cặp (paper, reviewer)
        List<AssignmentPreviewItemDTO> candidates = new ArrayList<>();

        for (Paper paper : papers) {
            for (User reviewer : reviewers) {
                // Bỏ qua nếu có conflict
                if (paperConflictRepository.existsByPaper_IdAndUser_Id(paper.getId(), reviewer.getId())) {
                    continue;
                }

                // Bỏ qua nếu đã assigned
                if (reviewRepository.existsByPaper_IdAndReviewer_Id(paper.getId(), reviewer.getId())) {
                    continue;
                }

                // Tính bid score
                double bidScore = getBidScore(bidMap.get(paper.getId() + "-" + reviewer.getId()));

                // Tính relevance score
                double relevanceScore = calculateRelevanceScore(paper, reviewerSubjectAreas.get(reviewer.getId()));

                // Combined score
                double combinedScore = bidScore * bidWeight + relevanceScore * relevanceWeight;

                candidates.add(AssignmentPreviewItemDTO.builder()
                        .paperId(paper.getId())
                        .paperTitle(paper.getTitle())
                        .reviewerId(reviewer.getId())
                        .reviewerName(reviewer.getFirstName() + " " + reviewer.getLastName())
                        .reviewerEmail(reviewer.getEmail())
                        .score(combinedScore)
                        .bidScore(bidScore)
                        .relevanceScore(relevanceScore)
                        .build());
            }
        }

        // 5. Greedy assignment: sắp xếp theo score giảm dần, assign theo constraints
        candidates.sort(Comparator.comparingDouble(AssignmentPreviewItemDTO::getScore).reversed());

        Map<Integer, Integer> paperAssignCount = new HashMap<>();  // paperId -> count
        Map<Integer, Integer> reviewerAssignCount = new HashMap<>(); // reviewerId -> count

        // Đếm existing assignments
        List<Review> existingReviews = reviewRepository.findByPaper_Track_Conference_Id(conferenceId);
        for (Review review : existingReviews) {
            paperAssignCount.merge(review.getPaper().getId(), 1, Integer::sum);
            reviewerAssignCount.merge(review.getReviewer().getId(), 1, Integer::sum);
        }

        List<AssignmentPreviewItemDTO> selectedAssignments = new ArrayList<>();

        for (AssignmentPreviewItemDTO candidate : candidates) {
            int currentPaperCount = paperAssignCount.getOrDefault(candidate.getPaperId(), 0);
            int currentReviewerCount = reviewerAssignCount.getOrDefault(candidate.getReviewerId(), 0);

            // Kiểm tra constraints
            if (currentPaperCount >= config.getMinReviewersPerPaper()) {
                continue; // Paper đã đủ reviewer
            }
            if (currentReviewerCount >= config.getMaxPapersPerReviewer()) {
                continue; // Reviewer đã đạt max
            }

            selectedAssignments.add(candidate);
            paperAssignCount.merge(candidate.getPaperId(), 1, Integer::sum);
            reviewerAssignCount.merge(candidate.getReviewerId(), 1, Integer::sum);
        }

        // 6. Build preview
        int unassignedPapers = 0;
        for (Paper paper : papers) {
            if (paperAssignCount.getOrDefault(paper.getId(), 0) < config.getMinReviewersPerPaper()) {
                unassignedPapers++;
            }
        }

        return AssignmentPreviewDTO.builder()
                .conferenceId(conferenceId)
                .totalPapers(papers.size())
                .totalReviewers(reviewers.size())
                .totalAssignments(selectedAssignments.size())
                .unassignedPapers(unassignedPapers)
                .overloadedReviewers(0)
                .assignments(selectedAssignments)
                .reviewersPerPaper(paperAssignCount)
                .papersPerReviewer(reviewerAssignCount)
                .build();
    }

    @Override
    @Transactional
    public List<AssignmentPreviewItemDTO> confirmAssignments(Integer conferenceId,
                                                              List<AssignmentPreviewItemDTO> assignments) {
        List<AssignmentPreviewItemDTO> confirmed = new ArrayList<>();

        for (AssignmentPreviewItemDTO item : assignments) {
            // Kiểm tra chưa assigned
            if (reviewRepository.existsByPaper_IdAndReviewer_Id(item.getPaperId(), item.getReviewerId())) {
                log.warn("Skipping duplicate assignment: paper {} -> reviewer {}", item.getPaperId(), item.getReviewerId());
                continue;
            }

            Paper paper = paperRepository.findById(item.getPaperId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paper not found: " + item.getPaperId()));
            User reviewer = userRepository.findById(item.getReviewerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + item.getReviewerId()));

            Review review = new Review();
            review.setPaper(paper);
            review.setReviewer(reviewer);
            review.setStatus(ReviewStatus.ASSIGNED);
            review.setTotalScore(BigDecimal.ZERO);
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());

            reviewRepository.save(review);
            confirmed.add(item);
            log.info("Assigned paper {} to reviewer {}", item.getPaperId(), item.getReviewerId());

            // Notification: review assigned
            Conference conference = paper.getTrack().getConference();
            Notification notification = Notification.builder()
                    .user(reviewer)
                    .conference(conference)
                    .title("New paper assigned for review")
                    .message("You have been assigned to review \"" + paper.getTitle() + "\" in \"" + conference.getName() + "\".")
                    .type("REVIEW_ASSIGNED")
                    .link("/conference/" + conference.getId() + "/reviewer")
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
        }

        return confirmed;
    }

    @Override
    @Transactional
    public AssignmentPreviewItemDTO manualAssign(Integer paperId, Integer reviewerId) {
        // Kiểm tra conflict
        if (paperConflictRepository.existsByPaper_IdAndUser_Id(paperId, reviewerId)) {
            throw new BadRequestException("Cannot assign: conflict of interest exists");
        }

        // Kiểm tra chưa assigned
        if (reviewRepository.existsByPaper_IdAndReviewer_Id(paperId, reviewerId)) {
            throw new BadRequestException("Reviewer is already assigned to this paper");
        }

        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found: " + paperId));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + reviewerId));

        Review review = new Review();
        review.setPaper(paper);
        review.setReviewer(reviewer);
        review.setStatus(ReviewStatus.ASSIGNED);
        review.setTotalScore(BigDecimal.ZERO);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        reviewRepository.save(review);

        // Notification: review assigned (manual)
        Conference conference = paper.getTrack().getConference();
        Notification notification = Notification.builder()
                .user(reviewer)
                .conference(conference)
                .title("New paper assigned for review")
                .message("You have been assigned to review \"" + paper.getTitle() + "\" in \"" + conference.getName() + "\".")
                .type("REVIEW_ASSIGNED")
                .link("/conference/" + conference.getId() + "/reviewer")
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        return AssignmentPreviewItemDTO.builder()
                .paperId(paper.getId())
                .paperTitle(paper.getTitle())
                .reviewerId(reviewer.getId())
                .reviewerName(reviewer.getFirstName() + " " + reviewer.getLastName())
                .reviewerEmail(reviewer.getEmail())
                .score(0.0)
                .bidScore(0.0)
                .relevanceScore(0.0)
                .build();
    }

    @Override
    @Transactional
    public void removeAssignment(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review assignment not found: " + reviewId));

        // BR-3.13: Không remove nếu review đã COMPLETED
        if (review.getStatus() == ReviewStatus.COMPLETED) {
            throw new BadRequestException(
                    "Cannot remove assignment: review is already COMPLETED. Status must be ASSIGNED or IN_PROGRESS.");
        }

        reviewRepository.deleteById(reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentPreviewDTO getCurrentAssignments(Integer conferenceId) {
        List<Review> reviews = reviewRepository.findByPaper_Track_Conference_Id(conferenceId);
        List<Paper> papers = paperRepository.findByTrack_Conference_Id(conferenceId);

        List<AssignmentPreviewItemDTO> items = reviews.stream()
                .map(review -> AssignmentPreviewItemDTO.builder()
                        .paperId(review.getPaper().getId())
                        .paperTitle(review.getPaper().getTitle())
                        .reviewerId(review.getReviewer().getId())
                        .reviewerName(review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName())
                        .reviewerEmail(review.getReviewer().getEmail())
                        .score(0.0)
                        .bidScore(0.0)
                        .relevanceScore(0.0)
                        .build())
                .collect(Collectors.toList());

        Map<Integer, Integer> reviewersPerPaper = new HashMap<>();
        Map<Integer, Integer> papersPerReviewer = new HashMap<>();
        for (Review review : reviews) {
            reviewersPerPaper.merge(review.getPaper().getId(), 1, Integer::sum);
            papersPerReviewer.merge(review.getReviewer().getId(), 1, Integer::sum);
        }

        List<ConferenceUserTrack> reviewerTracks = conferenceUserTrackRepository
                .findByConference_IdAndAssignedRole(conferenceId, ConferenceTrackRole.REVIEWER);

        return AssignmentPreviewDTO.builder()
                .conferenceId(conferenceId)
                .totalPapers(papers.size())
                .totalReviewers(reviewerTracks.size())
                .totalAssignments(reviews.size())
                .unassignedPapers(0)
                .overloadedReviewers(0)
                .assignments(items)
                .reviewersPerPaper(reviewersPerPaper)
                .papersPerReviewer(papersPerReviewer)
                .build();
    }

    // ========== Private helpers ==========

    private double getBidScore(BidValue bidValue) {
        if (bidValue == null) return 0.25; // Not Entered → neutral
        return switch (bidValue) {
            case EAGER -> 1.0;
            case WILLING -> 0.75;
            case IN_A_PINCH -> 0.25;
            case NOT_WILLING -> 0.0;
        };
    }

    private double calculateRelevanceScore(Paper paper, Set<Integer> reviewerSubjectAreaIds) {
        if (reviewerSubjectAreaIds == null || reviewerSubjectAreaIds.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;

        if (paper.getPrimarySubjectArea() != null
                && reviewerSubjectAreaIds.contains(paper.getPrimarySubjectArea().getId())) {
            score += 0.6;
        }

        List<SubjectArea> secondaryAreas = paper.getSecondarySubjectAreas();
        if (secondaryAreas != null && !secondaryAreas.isEmpty()) {
            long matchCount = secondaryAreas.stream()
                    .filter(sa -> reviewerSubjectAreaIds.contains(sa.getId()))
                    .count();
            if (matchCount > 0) {
                score += 0.4 * ((double) matchCount / secondaryAreas.size());
            }
        }

        return Math.min(score, 1.0);
    }
}
