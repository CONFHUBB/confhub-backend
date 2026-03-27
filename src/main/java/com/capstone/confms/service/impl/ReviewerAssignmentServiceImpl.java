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
import com.capstone.confms.entity.ReviewerInterest;
import com.capstone.confms.entity.SubjectArea;
import com.capstone.confms.entity.User;
import com.capstone.confms.utils.DomainConflictUtil;
import com.capstone.confms.utils.enums.BidValue;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.BiddingRepository;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.NotificationRepository;
import com.capstone.confms.repository.PaperAuthorRepository;
import com.capstone.confms.repository.PaperConflictRepository;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.ReviewRepository;
import com.capstone.confms.repository.ReviewerInterestRepository;
import com.capstone.confms.repository.TrackReviewSettingRepository;
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
    private final PaperAuthorRepository paperAuthorRepository;
    private final ReviewerInterestRepository reviewerInterestRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final NotificationRepository notificationRepository;
    private final TrackReviewSettingRepository trackReviewSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public AssignmentPreviewDTO runAutoAssign(AutoAssignConfigDTO config) {
        Integer conferenceId = config.getConferenceId();
        double bidWeight = config.getBidWeight() != null ? config.getBidWeight() : 0.6;
        double relevanceWeight = config.getRelevanceWeight() != null ? config.getRelevanceWeight() : 0.4;
        boolean loadBalancing = config.getLoadBalancing() != null && config.getLoadBalancing();

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

        // 2b. Pre-compute reviewer personal quotas
        Map<Integer, Integer> reviewerQuotaMap = new HashMap<>();
        for (ConferenceUserTrack rTrack : reviewerTracks) {
            if (rTrack.getReviewerQuota() != null) {
                reviewerQuotaMap.put(rTrack.getUser().getId(), rTrack.getReviewerQuota());
            }
        }

        // 3. Pre-compute: bids, conflicts, subject areas
        Map<String, BidValue> bidMap = new HashMap<>(); // key = "paperId-reviewerId"
        for (Paper paper : papers) {
            List<Bidding> paperBids = biddingRepository.findByPaper_Id(paper.getId());
            for (Bidding bid : paperBids) {
                bidMap.put(paper.getId() + "-" + bid.getReviewer().getId(), bid.getBidValue());
            }
        }

        // Map reviewer interests (full objects for CMT3 scoring)
        Map<Integer, List<ReviewerInterest>> reviewerInterestsMap = new HashMap<>();
        for (User reviewer : reviewers) {
            List<ReviewerInterest> interests = reviewerInterestRepository.findByReviewer_Id(reviewer.getId());
            reviewerInterestsMap.put(reviewer.getId(), interests);
        }

        // 3b. Pre-compute domain conflict data (only if any track has it enabled)
        // Also pre-compute author IDs per paper for self-review block
        Map<Integer, Set<String>> paperAuthorDomains = new HashMap<>();
        Map<Integer, Set<Integer>> paperAuthorIds = new HashMap<>();
        Map<Integer, Boolean> trackDomainConflictEnabled = new HashMap<>();

        for (Paper paper : papers) {
            var paperAuthors = paperAuthorRepository.findByPaperId(paper.getId());
            paperAuthorIds.put(paper.getId(), paperAuthors.stream()
                    .map(pa -> pa.getUser().getId())
                    .collect(Collectors.toSet()));

            Set<String> domains = paperAuthors.stream()
                    .map(pa -> pa.getUser().getEmail())
                    .map(DomainConflictUtil::extractDomain)
                    .filter(d -> d != null && !DomainConflictUtil.isPublicDomain(d))
                    .collect(Collectors.toSet());
            paperAuthorDomains.put(paper.getId(), domains);
        }

        // 4. Build candidate list: score mỗi cặp (paper, reviewer)
        List<AssignmentPreviewItemDTO> candidates = new ArrayList<>();

        for (Paper paper : papers) {
            for (User reviewer : reviewers) {
                // Author self-review block: reviewer cannot be assigned to own paper
                Set<Integer> authorIds = paperAuthorIds.getOrDefault(paper.getId(), Set.of());
                if (authorIds.contains(reviewer.getId())) {
                    continue;
                }

                // Bỏ qua nếu có conflict (PaperConflict table)
                if (paperConflictRepository.existsByPaper_IdAndUser_Id(paper.getId(), reviewer.getId())) {
                    continue;
                }

                // Domain conflict: only if enableDomainConflict is true for this track
                Integer trackId = paper.getTrack().getId();
                boolean domainEnabled = trackDomainConflictEnabled.computeIfAbsent(trackId, tid -> {
                    var setting = trackReviewSettingRepository.findByTrackId(tid).orElse(null);
                    return setting == null || Boolean.TRUE.equals(setting.getEnableDomainConflict());
                });

                if (domainEnabled) {
                    String reviewerDomain = DomainConflictUtil.extractDomain(reviewer.getEmail());
                    if (reviewerDomain != null && !DomainConflictUtil.isPublicDomain(reviewerDomain)) {
                        Set<String> authorDomains = paperAuthorDomains.getOrDefault(paper.getId(), Set.of());
                        if (authorDomains.contains(reviewerDomain)) {
                            continue;
                        }
                    }
                }

                // Bỏ qua nếu đã assigned
                if (reviewRepository.existsByPaper_IdAndReviewer_Id(paper.getId(), reviewer.getId())) {
                    continue;
                }

                // Tính bid score
                double bidScore = getBidScore(bidMap.get(paper.getId() + "-" + reviewer.getId()));

                // Tính relevance score
                double relevanceScore = calculateRelevanceScore(paper, reviewerInterestsMap.get(reviewer.getId()));

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
            paperAssignCount.merge(review.getPaper().getId(), 1, (a, b) -> a + b);
            reviewerAssignCount.merge(review.getReviewer().getId(), 1, (a, b) -> a + b);
        }

        List<AssignmentPreviewItemDTO> selectedAssignments = new ArrayList<>();

        for (AssignmentPreviewItemDTO candidate : candidates) {
            int currentPaperCount = paperAssignCount.getOrDefault(candidate.getPaperId(), 0);
            int currentReviewerCount = reviewerAssignCount.getOrDefault(candidate.getReviewerId(), 0);

            // Kiểm tra constraints
            if (currentPaperCount >= config.getMinReviewersPerPaper()) {
                continue; // Paper đã đủ reviewer
            }
            // Ưu tiên reviewer's personal quota, fallback to config default
            Integer personalQuota = reviewerQuotaMap.getOrDefault(candidate.getReviewerId(), null);
            int effectiveMax = personalQuota != null ? personalQuota : config.getMaxPapersPerReviewer();
            if (currentReviewerCount >= effectiveMax) {
                continue; // Reviewer đã đạt max (personal quota or default)
            }

            // Load balancing: ưu tiên reviewer ít paper hơn
            if (loadBalancing && currentReviewerCount > 0) {
                // Tìm xem có reviewer nào khác cho paper này có ít assignment hơn
                int avgLoad = (int) Math.ceil((double) papers.size() * config.getMinReviewersPerPaper() / reviewers.size());
                if (currentReviewerCount >= avgLoad + 1) {
                    continue; // Bỏ qua reviewer đã vượt mức trung bình
                }
            }

            selectedAssignments.add(candidate);
            paperAssignCount.merge(candidate.getPaperId(), 1, (a, b) -> a + b);
            reviewerAssignCount.merge(candidate.getReviewerId(), 1, (a, b) -> a + b);
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
                .reviewId(review.getId())
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

        // Pre-compute bids + reviewer subject areas for score calculation
        Map<String, BidValue> bidMap = new HashMap<>();
        for (Paper paper : papers) {
            List<Bidding> paperBids = biddingRepository.findByPaper_Id(paper.getId());
            for (Bidding bid : paperBids) {
                bidMap.put(paper.getId() + "-" + bid.getReviewer().getId(), bid.getBidValue());
            }
        }

        Map<Integer, List<ReviewerInterest>> reviewerInterestsMap = new HashMap<>();
        for (Review review : reviews) {
            Integer reviewerId = review.getReviewer().getId();
            if (!reviewerInterestsMap.containsKey(reviewerId)) {
                List<ReviewerInterest> interests = reviewerInterestRepository.findByReviewer_Id(reviewerId);
                reviewerInterestsMap.put(reviewerId, interests);
            }
        }

        List<AssignmentPreviewItemDTO> items = reviews.stream()
                .map(review -> {
                    double bidScore = getBidScore(bidMap.get(review.getPaper().getId() + "-" + review.getReviewer().getId()));
                    double relevanceScore = calculateRelevanceScore(review.getPaper(), reviewerInterestsMap.get(review.getReviewer().getId()));
                    double combinedScore = bidScore * 0.6 + relevanceScore * 0.4;

                    return AssignmentPreviewItemDTO.builder()
                            .reviewId(review.getId())
                            .paperId(review.getPaper().getId())
                            .paperTitle(review.getPaper().getTitle())
                            .reviewerId(review.getReviewer().getId())
                            .reviewerName(review.getReviewer().getFirstName() + " " + review.getReviewer().getLastName())
                            .reviewerEmail(review.getReviewer().getEmail())
                            .score(combinedScore)
                            .bidScore(bidScore)
                            .relevanceScore(relevanceScore)
                            .build();
                })
                .collect(Collectors.toList());

        Map<Integer, Integer> reviewersPerPaper = new HashMap<>();
        Map<Integer, Integer> papersPerReviewer = new HashMap<>();
        for (Review review : reviews) {
            reviewersPerPaper.merge(review.getPaper().getId(), 1, (a, b) -> a + b);
            papersPerReviewer.merge(review.getReviewer().getId(), 1, (a, b) -> a + b);
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

    private double calculateRelevanceScore(Paper paper, List<ReviewerInterest> reviewerInterests) {
        if (reviewerInterests == null || reviewerInterests.isEmpty()) {
            return 0.0;
        }

        // Split reviewer interests into primary and secondary SA IDs
        Set<Integer> reviewerPrimaryIds = reviewerInterests.stream()
                .filter(ri -> Boolean.TRUE.equals(ri.getIsPrimary()))
                .map(ri -> ri.getSubjectArea().getId())
                .collect(Collectors.toSet());
        Set<Integer> reviewerSecondaryIds = reviewerInterests.stream()
                .filter(ri -> !Boolean.TRUE.equals(ri.getIsPrimary()))
                .map(ri -> ri.getSubjectArea().getId())
                .collect(Collectors.toSet());

        // Paper subject areas
        Integer paperPrimaryId = paper.getPrimarySubjectArea() != null
                ? paper.getPrimarySubjectArea().getId() : null;
        Integer paperPrimaryParentId = paper.getPrimarySubjectArea() != null
                && paper.getPrimarySubjectArea().getParent() != null
                ? paper.getPrimarySubjectArea().getParent().getId() : null;

        Set<Integer> paperSecondaryIds = paper.getSecondarySubjectAreas() != null
                ? paper.getSecondarySubjectAreas().stream().map(SubjectArea::getId).collect(Collectors.toSet())
                : Set.of();
        // Parents of secondary paper SAs
        Set<Integer> paperSecondaryParentIds = paper.getSecondarySubjectAreas() != null
                ? paper.getSecondarySubjectAreas().stream()
                    .filter(sa -> sa.getParent() != null)
                    .map(sa -> sa.getParent().getId())
                    .collect(Collectors.toSet())
                : Set.of();

        // Reviewer SA parents
        Map<Integer, Integer> reviewerSAParents = new HashMap<>();
        for (ReviewerInterest ri : reviewerInterests) {
            if (ri.getSubjectArea().getParent() != null) {
                reviewerSAParents.put(ri.getSubjectArea().getId(), ri.getSubjectArea().getParent().getId());
            }
        }

        double score = 0.0;
        final double MAX_RAW = 1.59;

        // pp1: Paper primary == Reviewer primary (0.80)
        if (paperPrimaryId != null && reviewerPrimaryIds.contains(paperPrimaryId)) {
            score += 0.80;
        }

        // pp1h: Parent(Paper primary) == Reviewer primary (0.32)
        if (paperPrimaryParentId != null && reviewerPrimaryIds.contains(paperPrimaryParentId)) {
            score += 0.32;
        }

        // ps1: Reviewer primary matches secondary SA of paper (0.16)
        if (!reviewerPrimaryIds.isEmpty() && !paperSecondaryIds.isEmpty()) {
            for (Integer rpId : reviewerPrimaryIds) {
                if (paperSecondaryIds.contains(rpId)) {
                    score += 0.16;
                    break;
                }
            }
        }

        // ps1h: Reviewer primary matches parent of secondary SA of paper (0.05)
        if (!reviewerPrimaryIds.isEmpty() && !paperSecondaryParentIds.isEmpty()) {
            for (Integer rpId : reviewerPrimaryIds) {
                if (paperSecondaryParentIds.contains(rpId)) {
                    score += 0.05;
                    break;
                }
            }
        }

        // sp1: Paper primary matches secondary SA of reviewer (0.16)
        if (paperPrimaryId != null && reviewerSecondaryIds.contains(paperPrimaryId)) {
            score += 0.16;
        }

        // sp1h: Parent(Paper primary) matches secondary SA of reviewer (0.05)
        if (paperPrimaryParentId != null && reviewerSecondaryIds.contains(paperPrimaryParentId)) {
            score += 0.05;
        }

        // ss1: Secondary reviewer overlaps with secondary paper (0.04)
        if (!reviewerSecondaryIds.isEmpty() && !paperSecondaryIds.isEmpty()) {
            boolean hasOverlap = reviewerSecondaryIds.stream().anyMatch(paperSecondaryIds::contains);
            if (hasOverlap) {
                score += 0.04;
            }
        }

        // ss1h: Parent of secondary reviewer overlaps with secondary paper (0.01)
        if (!reviewerSecondaryIds.isEmpty() && !paperSecondaryIds.isEmpty()) {
            boolean hasParentOverlap = reviewerSecondaryIds.stream()
                    .map(reviewerSAParents::get)
                    .filter(java.util.Objects::nonNull)
                    .anyMatch(paperSecondaryIds::contains);
            if (hasParentOverlap) {
                score += 0.01;
            }
        }

        // Normalize to [0, 1]
        return Math.min(score / MAX_RAW, 1.0);
    }
}
