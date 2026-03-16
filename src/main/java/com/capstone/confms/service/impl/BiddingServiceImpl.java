package com.capstone.confms.service.impl;

import com.capstone.confms.dto.BiddingDTO;
import com.capstone.confms.dto.response.BiddingResponseDTO;
import com.capstone.confms.dto.response.BidsSummaryDTO;
import com.capstone.confms.dto.response.PaperForBiddingDTO;
import com.capstone.confms.entity.Bidding;
import com.capstone.confms.entity.ConferenceActivity;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.PaperAuthor;
import com.capstone.confms.entity.ReviewerInterest;
import com.capstone.confms.entity.SubjectArea;
import com.capstone.confms.entity.User;
import com.capstone.confms.utils.DomainConflictUtil;
import com.capstone.confms.utils.enums.ActivityType;
import com.capstone.confms.utils.enums.BidValue;
import com.capstone.confms.utils.enums.Expertise;
import com.capstone.confms.utils.enums.PaperStatus;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.BiddingRepository;
import com.capstone.confms.repository.ConferenceActivityRepository;
import com.capstone.confms.repository.PaperAuthorRepository;
import com.capstone.confms.repository.PaperConflictRepository;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.ReviewerInterestRepository;
import com.capstone.confms.repository.TrackReviewSettingRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.BiddingService;
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
        if (interests.isEmpty()) {
            throw new BadRequestException(
                    "You must select your Subject Areas / Areas of Expertise before viewing papers for bidding. " +
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

        // 6. Lấy reviewer interests (có expertise weight)
        Map<Integer, Expertise> expertiseMap = interests.stream()
                .collect(Collectors.toMap(ri -> ri.getSubjectArea().getId(), ReviewerInterest::getExpertise));
        Set<Integer> reviewerSubjectAreaIds = expertiseMap.keySet();

        // 7. Check double blind setting per track
        Map<Integer, Boolean> trackDoubleBlindMap = new java.util.HashMap<>();

        // 8. Build danh sách papers
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

            // Domain conflict: check if reviewer shares institutional domain with any author
            if (reviewerDomain != null && !DomainConflictUtil.isPublicDomain(reviewerDomain)) {
                List<PaperAuthor> authors = paperAuthorRepository.findByPaperId(paper.getId());
                boolean hasDomainConflict = authors.stream()
                        .map(pa -> DomainConflictUtil.extractDomain(pa.getUser().getEmail()))
                        .anyMatch(reviewerDomain::equalsIgnoreCase);
                if (hasDomainConflict) {
                    continue;
                }
            }

            // BR-3.2: Tính relevance score với expertise weight
            double relevance = calculateRelevanceScoreWithExpertise(paper, reviewerSubjectAreaIds, expertiseMap);

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
            Integer trackId = paper.getTrack().getId();
            boolean isDoubleBlind = trackDoubleBlindMap.computeIfAbsent(trackId, tid -> {
                return trackReviewSettingRepository.findByTrackId(tid)
                        .map(setting -> Boolean.TRUE.equals(setting.getIsDoubleBlind()))
                        .orElse(false);
            });

            PaperForBiddingDTO dto = PaperForBiddingDTO.builder()
                    .paperId(paper.getId())
                    .title(paper.getTitle())
                    .abstractText(isDoubleBlind ? null : paper.getAbstractField())
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
        List<ReviewerInterest> interests = reviewerInterestRepository.findByReviewer_Id(reviewerId);
        if (interests.isEmpty()) {
            throw new BadRequestException(
                    "You must select your Subject Areas / Areas of Expertise before bidding.");
        }
    }

    /**
     * BR-3.2: Relevance score with expertise weight.
     * EXPERT=1.0, KNOWLEDGEABLE=0.7, INTERESTED=0.4
     */
    private double calculateRelevanceScoreWithExpertise(Paper paper, Set<Integer> reviewerSAIds,
                                                         Map<Integer, Expertise> expertiseMap) {
        if (reviewerSAIds.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;

        // Primary subject area match (weight 0.6)
        if (paper.getPrimarySubjectArea() != null) {
            Integer primaryId = paper.getPrimarySubjectArea().getId();
            if (reviewerSAIds.contains(primaryId)) {
                double expertiseWeight = getExpertiseWeight(expertiseMap.get(primaryId));
                score += 0.6 * expertiseWeight;
            }
        }

        // Secondary subject areas match (weight 0.4)
        List<SubjectArea> secondaryAreas = paper.getSecondarySubjectAreas();
        if (secondaryAreas != null && !secondaryAreas.isEmpty()) {
            double secondaryScore = 0.0;
            int matchCount = 0;
            for (SubjectArea sa : secondaryAreas) {
                if (reviewerSAIds.contains(sa.getId())) {
                    double expertiseWeight = getExpertiseWeight(expertiseMap.get(sa.getId()));
                    secondaryScore += expertiseWeight;
                    matchCount++;
                }
            }
            if (matchCount > 0) {
                score += 0.4 * (secondaryScore / secondaryAreas.size());
            }
        }

        return Math.min(score, 1.0);
    }

    private double getExpertiseWeight(Expertise expertise) {
        if (expertise == null) return 0.5;
        return switch (expertise) {
            case EXPERT -> 1.0;
            case KNOWLEDGEABLE -> 0.7;
            case INTERESTED -> 0.4;
        };
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
