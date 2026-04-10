package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.BiddingDTO;
import com.capstone.confhub.dto.response.BiddingResponseDTO;
import com.capstone.confhub.dto.response.BidsSummaryDTO;
import com.capstone.confhub.dto.response.PaperForBiddingDTO;
import com.capstone.confhub.entity.Bidding;
import com.capstone.confhub.entity.ConferenceActivity;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperAuthor;
import com.capstone.confhub.entity.ReviewerInterest;
import com.capstone.confhub.entity.SubjectArea;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.utils.DomainConflictUtil;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.BidValue;
import com.capstone.confhub.utils.enums.PaperStatus;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.BiddingRepository;
import com.capstone.confhub.repository.ConferenceActivityRepository;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.repository.PaperConflictRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewerInterestRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.service.BiddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BiddingServiceImpl implements BiddingService {

    private final BiddingRepository biddingRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final ConferenceActivityRepository activityRepository;
    private final PaperConflictRepository paperConflictRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final ReviewerInterestRepository reviewerInterestRepository;
    private final TrackReviewSettingRepository trackReviewSettingRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public BiddingResponseDTO submitOrUpdateBid(BiddingDTO dto) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + dto.getPaperId()));

        User reviewer = userRepository.findById(dto.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + dto.getReviewerId()));

        Integer conferenceId = paper.getTrack().getConference().getId();

        // BR-3.3: Check REVIEWER_BIDDING activity enabled
        validateBiddingPhaseOpen(conferenceId);

        // BR-3.1: Check requireSubjectAreas
        validateRequireSubjectAreas(paper.getTrack().getId(), dto.getReviewerId());

        // Kiểm tra conflict
        if (paperConflictRepository.existsByPaper_IdAndUser_Id(dto.getPaperId(), dto.getReviewerId())) {
            throw new BadRequestException("Cannot bid on a paper with conflict of interest");
        }

        Optional<Bidding> existingBid = biddingRepository
                .findByReviewer_IdAndPaper_Id(dto.getReviewerId(), dto.getPaperId());

        Bidding bidding;
        if (existingBid.isPresent()) {
            bidding = existingBid.get();
            bidding.setBidValue(dto.getBidValue());
            bidding.setUpdatedAt(LocalDateTime.now());
            log.info("Updated bid for reviewer {} on paper {} to {}", dto.getReviewerId(), dto.getPaperId(), dto.getBidValue());
        } else {
            bidding = new Bidding();
            bidding.setPaper(paper);
            bidding.setReviewer(reviewer);
            bidding.setBidValue(dto.getBidValue());
            bidding.setCreatedAt(LocalDateTime.now());
            bidding.setUpdatedAt(LocalDateTime.now());
            log.info("Created bid for reviewer {} on paper {} as {}", dto.getReviewerId(), dto.getPaperId(), dto.getBidValue());
        }

        Bidding saved = biddingRepository.save(bidding);
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiddingResponseDTO> getBidsByReviewerAndConference(Integer reviewerId, Integer conferenceId) {
        List<Bidding> bids = biddingRepository.findByReviewer_IdAndPaper_Track_Conference_Id(reviewerId, conferenceId);
        return bids.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiddingResponseDTO> getBidsByPaper(Integer paperId) {
        List<Bidding> bids = biddingRepository.findByPaper_Id(paperId);
        return bids.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BidsSummaryDTO getBidsSummary(Integer reviewerId, Integer conferenceId) {
        Map<String, Long> bidCounts = new LinkedHashMap<>();
        long totalBids = 0;

        for (BidValue value : BidValue.values()) {
            long count = biddingRepository.countByReviewer_IdAndBidValueAndPaper_Track_Conference_Id(
                    reviewerId, value, conferenceId);
            bidCounts.put(value.name(), count);
            totalBids += count;
        }

        long totalPapers = paperRepository.findByTrack_Conference_Id(conferenceId).stream()
                .filter(p -> p.getStatus() != PaperStatus.WITHDRAWN && p.getStatus() != PaperStatus.DRAFT)
                .count();

        return BidsSummaryDTO.builder()
                .reviewerId(reviewerId)
                .conferenceId(conferenceId)
                .bidCounts(bidCounts)
                .totalBids(totalBids)
                .totalPapers(totalPapers)
                .build();
    }

    @Override
    @Transactional
    public void deleteBid(Integer bidId) {
        if (!biddingRepository.existsById(bidId)) {
            throw new ResourceNotFoundException("Bid not found with id " + bidId);
        }
        biddingRepository.deleteById(bidId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperForBiddingDTO> getPapersForBidding(Integer reviewerId, Integer conferenceId) {
        // 0. Lấy reviewer info
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + reviewerId));

        // 1. Kiểm tra bắt buộc chọn subject areas trước khi bid
        List<ReviewerInterest> interests = reviewerInterestRepository.findByReviewer_Id(reviewerId);

        // Check if any track in this conference has papers — subject areas are always required
        if (interests.isEmpty()) {
            throw new BadRequestException(
                    "You must select your Subject Areas before viewing papers for bidding. " +
                    "Please go to the Subject Areas page first.");
        }

        // 2. Lấy tất cả papers trong conference
        List<Paper> allPapers = paperRepository.findByTrack_Conference_Id(conferenceId);

        // 3. Lấy paper IDs mà reviewer có conflict (manual)
        Set<Integer> conflictPaperIds = paperConflictRepository.findByUser_Id(reviewerId)
                .stream()
                .map(pc -> pc.getPaper().getId())
                .collect(Collectors.toSet());

        // 4. Domain conflict detection — lấy email domain của reviewer
        String reviewerDomain = DomainConflictUtil.extractDomain(reviewer.getEmail());

        // 5. Lấy bids hiện tại của reviewer
        Map<Integer, BidValue> existingBids = biddingRepository
                .findByReviewer_IdAndPaper_Track_Conference_Id(reviewerId, conferenceId)
                .stream()
                .collect(Collectors.toMap(b -> b.getPaper().getId(), Bidding::getBidValue));

        // 6. Lấy reviewer interests (full objects for CMT3 scoring)
        List<ReviewerInterest> reviewerInterests = interests;

        // 7. Check double blind setting per track
        Map<Integer, Boolean> trackDoubleBlindMap = new java.util.HashMap<>();
        // 8. Pre-fetch track settings for domain conflict config
        Map<Integer, Boolean> trackDomainConflictEnabled = new java.util.HashMap<>();

        // 9. Build danh sách papers
        List<PaperForBiddingDTO> result = new ArrayList<>();
        for (Paper paper : allPapers) {
            // BR-3.4: Lọc WITHDRAWN + DRAFT papers
            if (paper.getStatus() == PaperStatus.WITHDRAWN || paper.getStatus() == PaperStatus.DRAFT) {
                continue;
            }

            // Bỏ qua conflicting papers (manual conflicts)
            if (conflictPaperIds.contains(paper.getId())) {
                continue;
            }

            // Author self-review block: reviewer cannot bid on their own paper
            List<PaperAuthor> authors = paperAuthorRepository.findByPaperId(paper.getId());
            boolean isAuthor = authors.stream().anyMatch(pa -> pa.getUser().getId().equals(reviewerId));
            if (isAuthor) {
                continue;
            }

            // Domain conflict: only if enableDomainConflict is true for this track
            Integer trackId = paper.getTrack().getId();
            boolean domainEnabled = trackDomainConflictEnabled.computeIfAbsent(trackId, tid -> {
                var setting = trackReviewSettingRepository.findByTrackId(tid).orElse(null);
                return setting == null || Boolean.TRUE.equals(setting.getEnableDomainConflict());
            });

            if (domainEnabled && reviewerDomain != null && !DomainConflictUtil.isPublicDomain(reviewerDomain)) {
                boolean hasDomainConflict = authors.stream()
                        .map(pa -> DomainConflictUtil.extractDomain(pa.getUser().getEmail()))
                        .anyMatch(reviewerDomain::equalsIgnoreCase);
                if (hasDomainConflict) {
                    continue;
                }
            }

            // BR-3.2: Tính relevance score theo CMT3 formula
            double relevance = calculateRelevanceScoreCMT3(paper, reviewerInterests);

            // Lấy subject area names
            String primarySA = paper.getPrimarySubjectArea() != null
                    ? paper.getPrimarySubjectArea().getName() : null;
            List<String> secondarySAs = paper.getSecondarySubjectAreas() != null
                    ? paper.getSecondarySubjectAreas().stream()
                        .map(SubjectArea::getName)
                        .collect(Collectors.toList())
                    : List.of();

            // Parse keywords from JSON
            List<String> keywords = parseKeywords(paper.getKeywordsJson());

            // Track name
            String trackName = paper.getTrack().getName();

            // BR-3.4: Double Blind check — ẩn abstract nếu isDoubleBlind
            boolean isDoubleBlind = trackDoubleBlindMap.computeIfAbsent(trackId, tid -> {
                return trackReviewSettingRepository.findByTrackId(tid)
                        .map(setting -> Boolean.TRUE.equals(setting.getIsDoubleBlind()))
                        .orElse(false);
            });

            PaperForBiddingDTO dto = PaperForBiddingDTO.builder()
                    .paperId(paper.getId())
                    .title(paper.getTitle())
                    .abstractText(paper.getAbstractField())  // Always show abstract — reviewers need it for bidding
                    .primarySubjectArea(primarySA)
                    .secondarySubjectAreas(secondarySAs)
                    .keywords(keywords)
                    .trackName(trackName)
                    .relevanceScore(relevance)
                    .currentBid(existingBids.getOrDefault(paper.getId(), null))
                    .isDoubleBlind(isDoubleBlind)
                    .build();

            result.add(dto);
        }

        // Sắp xếp theo relevance score giảm dần
        result.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));

        return result;
    }

    // ========== Private helpers ==========

    /**
     * Parse keywords JSON string to List<String>.
     */
    private List<String> parseKeywords(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(keywordsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse keywords JSON: {}", keywordsJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * BR-3.1: Validate subject areas — always require at least 1 interest before bidding.
     */
    private void validateRequireSubjectAreas(Integer trackId, Integer reviewerId) {
        // Subject areas are always required (hardcoded business rule)
        List<ReviewerInterest> interests = reviewerInterestRepository.findByReviewer_Id(reviewerId);
        if (interests.isEmpty()) {
            throw new BadRequestException(
                    "You must select your Subject Areas before bidding.");
        }
    }

    /**
     * BR-3.2: CMT3-style relevance score.
     * Relevance = 0.80pp1 + 0.32pp1h + 0.16ps1 + 0.05ps1h + 0.16sp1 + 0.05sp1h + 0.04ss1 + 0.01ss1h
     * Normalized to [0,1] by dividing by max raw score (1.59).
     */
    private double calculateRelevanceScoreCMT3(Paper paper, List<ReviewerInterest> reviewerInterests) {
        if (reviewerInterests == null || reviewerInterests.isEmpty()) {
            return 0.0;
        }

        Set<Integer> reviewerPrimaryIds = reviewerInterests.stream()
                .filter(ri -> Boolean.TRUE.equals(ri.getIsPrimary()))
                .map(ri -> ri.getSubjectArea().getId())
                .collect(Collectors.toSet());
        Set<Integer> reviewerSecondaryIds = reviewerInterests.stream()
                .filter(ri -> !Boolean.TRUE.equals(ri.getIsPrimary()))
                .map(ri -> ri.getSubjectArea().getId())
                .collect(Collectors.toSet());

        Integer paperPrimaryId = paper.getPrimarySubjectArea() != null
                ? paper.getPrimarySubjectArea().getId() : null;
        Integer paperPrimaryParentId = paper.getPrimarySubjectArea() != null
                && paper.getPrimarySubjectArea().getParent() != null
                ? paper.getPrimarySubjectArea().getParent().getId() : null;

        Set<Integer> paperSecondaryIds = paper.getSecondarySubjectAreas() != null
                ? paper.getSecondarySubjectAreas().stream().map(SubjectArea::getId).collect(Collectors.toSet())
                : Set.of();
        Set<Integer> paperSecondaryParentIds = paper.getSecondarySubjectAreas() != null
                ? paper.getSecondarySubjectAreas().stream()
                    .filter(sa -> sa.getParent() != null)
                    .map(sa -> sa.getParent().getId())
                    .collect(Collectors.toSet())
                : Set.of();

        Map<Integer, Integer> reviewerSAParents = new java.util.HashMap<>();
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
            if (reviewerPrimaryIds.stream().anyMatch(paperSecondaryIds::contains)) {
                score += 0.16;
            }
        }
        // ps1h: Reviewer primary matches parent of secondary SA of paper (0.05)
        if (!reviewerPrimaryIds.isEmpty() && !paperSecondaryParentIds.isEmpty()) {
            if (reviewerPrimaryIds.stream().anyMatch(paperSecondaryParentIds::contains)) {
                score += 0.05;
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
            if (reviewerSecondaryIds.stream().anyMatch(paperSecondaryIds::contains)) {
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

        return Math.min(score / MAX_RAW, 1.0);
    }

    private void validateBiddingPhaseOpen(Integer conferenceId) {
        Optional<ConferenceActivity> activity = activityRepository
                .findByConferenceIdAndActivityType(conferenceId, ActivityType.REVIEWER_BIDDING);

        if (activity.isEmpty() || !Boolean.TRUE.equals(activity.get().getIsEnabled())) {
            throw new BadRequestException("Reviewer Bidding phase is not currently open for this conference");
        }

        // BR-3.3: Check deadline
        if (activity.get().getDeadline() != null && activity.get().getDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reviewer Bidding deadline has passed");
        }
    }

    private BiddingResponseDTO mapToResponseDTO(Bidding entity) {
        String reviewerName = entity.getReviewer().getFirstName() + " " + entity.getReviewer().getLastName();

        return BiddingResponseDTO.builder()
                .id(entity.getId())
                .paperId(entity.getPaper().getId())
                .paperTitle(entity.getPaper().getTitle())
                .reviewerId(entity.getReviewer().getId())
                .reviewerName(reviewerName)
                .bidValue(entity.getBidValue())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
