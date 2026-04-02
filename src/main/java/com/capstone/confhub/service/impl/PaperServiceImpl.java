package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.*;
import com.capstone.confhub.security.services.UserDetailsImpl;
import com.capstone.confhub.service.PaperService;
import com.capstone.confhub.utils.PaginationUtils;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import com.capstone.confhub.utils.enums.PaperStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final ConferenceTrackRepository conferenceTrackRepository;
    private final SubjectAreaRepository subjectAreaRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final ConferenceSubmissionFormRepository conferenceSubmissionFormRepository;
    private final ConferenceActivityRepository conferenceActivityRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final TrackReviewSettingRepository trackReviewSettingRepository;
    private final ObjectMapper objectMapper;

    // ==================== VALID STATUS TRANSITIONS (BR-2.16) ====================
    private static final Map<PaperStatus, Set<PaperStatus>> VALID_TRANSITIONS = Map.of(
            PaperStatus.DRAFT, Set.of(PaperStatus.SUBMITTED, PaperStatus.WITHDRAWN),
            PaperStatus.SUBMITTED, Set.of(PaperStatus.UNDER_REVIEW, PaperStatus.WITHDRAWN),
            PaperStatus.UNDER_REVIEW, Set.of(PaperStatus.ACCEPTED, PaperStatus.REJECTED, PaperStatus.WITHDRAWN),
            PaperStatus.ACCEPTED, Set.of(PaperStatus.PUBLISHED),
            PaperStatus.REJECTED, Set.of(),
            PaperStatus.WITHDRAWN, Set.of(PaperStatus.SUBMITTED),
            PaperStatus.PUBLISHED, Set.of()
    );

    // ==================== CREATE ====================
    @Override
    @Transactional
    public PaperResponseDTO createPaper(PaperDTO dto) {
        log.info("Creating new Paper with title: {}", dto.getTitle());

        ConferenceTrack track = conferenceTrackRepository.findById(dto.getConferenceTrackId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Track not found with ID: " + dto.getConferenceTrackId()));

        // BR-2.1: Check PAPER_SUBMISSION enabled + deadline
        validatePaperSubmissionActivity(track.getConference().getId());

        Paper paper = new Paper();
        mapDtoToEntity(dto, paper, track);

        // BR-2.4: DRAFT if no status specified or explicitly DRAFT
        if (dto.getStatus() == null) {
            paper.setStatus(PaperStatus.SUBMITTED);
        }
        paper.setSubmissionTime(Instant.now());

        Paper saved = paperRepository.save(paper);

        // BR-2.2: Auto-assign AUTHOR role
        autoAssignAuthorRole(track.getConference().getId());

        // Notification: paper submitted
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
                User currentUser = userRepository.findById(userDetails.getId()).orElse(null);
                if (currentUser != null) {
                    Notification notification = Notification.builder()
                            .user(currentUser)
                            .conference(track.getConference())
                            .title("Paper submitted successfully")
                            .message("Your paper \"" + saved.getTitle() + "\" has been submitted to \"" + track.getConference().getName() + "\".")
                            .type("PAPER_SUBMITTED")
                            .link("/paper")
                            .isRead(false)
                            .build();
                    notificationRepository.save(notification);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to create paper submission notification: {}", e.getMessage());
        }

        return mapToResponseDTO(saved);
    }

    // ==================== UPDATE ====================
    @Override
    @Transactional
    public PaperResponseDTO updatePaper(Integer id, PaperDTO dto) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + id));

        // BR-2.13: Check activity enabled before edit
        validatePaperSubmissionActivity(paper.getTrack().getConference().getId());

        // BR-2.17: Không cho edit khi UNDER_REVIEW
        if (paper.getStatus() == PaperStatus.UNDER_REVIEW) {
            throw new BadRequestException("Cannot edit paper while it is under review");
        }
        if (paper.getStatus() == PaperStatus.WITHDRAWN) {
            throw new BadRequestException("Cannot edit a withdrawn paper");
        }

        ConferenceTrack track = conferenceTrackRepository.findById(dto.getConferenceTrackId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Track not found with ID: " + dto.getConferenceTrackId()));
        mapDtoToEntity(dto, paper, track);
        return mapToResponseDTO(paperRepository.save(paper));
    }

    // ==================== UPDATE STATUS ====================
    @Override
    @Transactional
    public PaperResponseDTO updatePaperStatus(Integer id, PaperUpdateStatusDTO dto) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + id));

        // BR-2.16: Validate status transition
        validateStatusTransition(paper.getStatus(), dto.getStatus());

        paper.setStatus(dto.getStatus());
        paper.setUpdatedAt(LocalDateTime.now());
        return mapToResponseDTO(paperRepository.save(paper));
    }

    // ==================== GET ====================
    @Override
    public PaperResponseDTO getPaperById(Integer id) {
        return paperRepository.findById(id)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaperResponseDTO> getAllPapers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Paper> papers = paperRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(papers, this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaperResponseDTO> getPapersByAuthor(Integer authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaperAuthor> paperAuthors = paperAuthorRepository.findByUserId(authorId, pageable);
        return PaginationUtils.toPagedResponse(paperAuthors, pa -> mapToResponseDTO(pa.getPaper()));
    }

    // ==================== DELETE ====================
    @Override
    @Transactional
    public void deletePaper(Integer id) {
        if (!paperRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. Paper not found with id " + id);
        }

        // BR-2.14: Không xóa nếu đã có reviews
        long reviewCount = reviewRepository.countByPaper_Id(id);
        if (reviewCount > 0) {
            throw new BadRequestException(
                    "Cannot delete paper: it has " + reviewCount + " review(s). Use 'withdraw' instead.");
        }

        paperRepository.deleteById(id);
    }

    // ==================== WITHDRAW (BR-2.15) ====================
    @Override
    @Transactional
    public PaperResponseDTO withdrawPaper(Integer id) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + id));

        // Chỉ withdraw từ SUBMITTED hoặc UNDER_REVIEW
        if (paper.getStatus() != PaperStatus.SUBMITTED
                && paper.getStatus() != PaperStatus.UNDER_REVIEW
                && paper.getStatus() != PaperStatus.DRAFT) {
            throw new BadRequestException(
                    "Can only withdraw papers with status DRAFT, SUBMITTED, or UNDER_REVIEW. Current: "
                            + paper.getStatus());
        }

        paper.setStatus(PaperStatus.WITHDRAWN);
        paper.setUpdatedAt(LocalDateTime.now());
        Paper saved = paperRepository.save(paper);

        // Notification: notify chairs about withdrawal
        try {
            Conference conference = saved.getTrack().getConference();
            List<ConferenceUserTrack> chairs = conferenceUserTrackRepository
                    .findByConference_IdAndAssignedRole(conference.getId(), ConferenceTrackRole.CONFERENCE_CHAIR);
            for (ConferenceUserTrack chair : chairs) {
                Notification notification = Notification.builder()
                        .user(chair.getUser())
                        .conference(conference)
                        .title("Paper withdrawn")
                        .message("Paper \"" + saved.getTitle() + "\" has been withdrawn from \"" + conference.getName() + "\".")
                        .type("PAPER_WITHDRAWN")
                        .link("/conference/" + conference.getId() + "/update")
                        .isRead(false)
                        .build();
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            log.warn("Failed to create paper withdrawal notification: {}", e.getMessage());
        }

        return mapToResponseDTO(saved);
    }

    // ==================== RESTORE (BR-2.15) ====================
    @Override
    @Transactional
    public PaperResponseDTO restorePaper(Integer id) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + id));

        if (paper.getStatus() != PaperStatus.WITHDRAWN) {
            throw new BadRequestException("Can only restore WITHDRAWN papers. Current: " + paper.getStatus());
        }

        paper.setStatus(PaperStatus.SUBMITTED);
        paper.setUpdatedAt(LocalDateTime.now());
        return mapToResponseDTO(paperRepository.save(paper));
    }
    // ==================== GET BY CONFERENCE (Chair/PC) ====================
    @Override
    @Transactional(readOnly = true)
    public List<PaperResponseDTO> getPapersByConference(Integer conferenceId) {
        List<Paper> papers = paperRepository.findByTrack_Conference_Id(conferenceId);
        if (papers.isEmpty()) return List.of();

        // ── Batch pre-load to avoid N+1 ──
        List<Integer> paperIds = papers.stream().map(Paper::getId).toList();
        Set<Integer> trackIds = papers.stream().map(p -> p.getTrack().getId()).collect(java.util.stream.Collectors.toSet());

        // 1) Batch load review settings for all tracks
        Map<Integer, Boolean> doubleBlindByTrack = new HashMap<>();
        for (Integer trackId : trackIds) {
            doubleBlindByTrack.put(trackId,
                    trackReviewSettingRepository.findByTrackId(trackId)
                            .map(s -> Boolean.TRUE.equals(s.getIsDoubleBlind()))
                            .orElse(true));
        }

        // 2) Batch load all authors for all papers
        List<PaperAuthor> allAuthors = paperAuthorRepository.findByPaper_IdInOrderByOrderIndexAsc(paperIds);
        Map<Integer, List<String>> authorNamesByPaperId = new HashMap<>();
        Map<Integer, Set<Integer>> authorUserIdsByPaperId = new HashMap<>();
        for (PaperAuthor pa : allAuthors) {
            int pid = pa.getPaper().getId();
            authorNamesByPaperId.computeIfAbsent(pid, k -> new ArrayList<>())
                    .add(pa.getUser().getFirstName() + " " + pa.getUser().getLastName());
            authorUserIdsByPaperId.computeIfAbsent(pid, k -> new HashSet<>())
                    .add(pa.getUser().getId());
        }

        // 3) Determine current user context once
        Integer currentUserId = null;
        boolean isAdminOrStaff = false;
        boolean isChair = false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            isAdminOrStaff = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
            if (!isAdminOrStaff && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
                currentUserId = userDetails.getId();
                List<ConferenceUserTrack> roles = conferenceUserTrackRepository
                        .findAllByUser_IdAndConference_Id(currentUserId, conferenceId);
                isChair = roles.stream().anyMatch(r ->
                        r.getAssignedRole() == ConferenceTrackRole.CONFERENCE_CHAIR ||
                        r.getAssignedRole() == ConferenceTrackRole.PROGRAM_CHAIR);
            }
        }

        // 4) Map all papers using pre-loaded data
        final Integer userId = currentUserId;
        final boolean finalIsAdminOrStaff = isAdminOrStaff;
        final boolean finalIsChair = isChair;

        return papers.stream().map(entity -> {
            int trackId = entity.getTrack().getId();
            boolean isDoubleBlind = doubleBlindByTrack.getOrDefault(trackId, true);

            boolean shouldMask = false;
            if (isDoubleBlind && !finalIsAdminOrStaff && !finalIsChair) {
                if (userId == null) {
                    shouldMask = true;
                } else {
                    Set<Integer> authorUserIds = authorUserIdsByPaperId.getOrDefault(entity.getId(), Set.of());
                    shouldMask = !authorUserIds.contains(userId);
                }
            }

            List<String> authorNames = shouldMask ? List.of()
                    : authorNamesByPaperId.getOrDefault(entity.getId(), List.of());

            List<Integer> secondaryIds = entity.getSecondarySubjectAreas() != null
                    ? entity.getSecondarySubjectAreas().stream().map(SubjectArea::getId).toList()
                    : List.of();

            return PaperResponseDTO.builder()
                    .id(entity.getId())
                    .conferenceId(entity.getTrack().getConference().getId())
                    .trackId(trackId)
                    .trackName(entity.getTrack().getName())
                    .primarySubjectAreaId(
                            entity.getPrimarySubjectArea() != null ? entity.getPrimarySubjectArea().getId() : null)
                    .secondarySubjectAreaIds(secondaryIds)
                    .title(entity.getTitle())
                    .abstractField(entity.getAbstractField())
                    .keywords(deserializeKeywords(entity.getKeywordsJson()))
                    .submissionTime(entity.getSubmissionTime())
                    .status(entity.getStatus())
                    .submissionFormId(entity.getSubmissionForm() != null ? entity.getSubmissionForm().getId() : null)
                    .extraAnswersJson(entity.getExtraAnswersJson())
                    .isDoubleBlind(isDoubleBlind)
                    .authorNames(authorNames)
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }

    // ==================== TOGGLE REVIEW READ-ONLY (BR-3.28) ====================
    @Override
    @Transactional
    public PaperResponseDTO toggleReviewReadOnly(Integer id, boolean readOnly) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + id));
        paper.setIsReviewReadOnly(readOnly);
        paper.setUpdatedAt(LocalDateTime.now());
        return mapToResponseDTO(paperRepository.save(paper));
    }

    // ==================== TOGGLE DISCUSSION (BR-3.30) ====================
    @Override
    @Transactional
    public PaperResponseDTO toggleDiscussion(Integer id, boolean enabled) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + id));
        paper.setIsDiscussionEnabled(enabled);
        paper.setUpdatedAt(LocalDateTime.now());
        return mapToResponseDTO(paperRepository.save(paper));
    }

    // ==================== BULK UPDATE PAPER STATUS (BR-3.43) ====================
    @Override
    @Transactional
    public List<PaperResponseDTO> bulkUpdatePaperStatus(List<PaperUpdateStatusDTO> dtos) {
        List<PaperResponseDTO> results = new ArrayList<>();
        for (PaperUpdateStatusDTO dto : dtos) {
            Paper paper = paperRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + dto.getId()));
            validateStatusTransition(paper.getStatus(), dto.getStatus());
            paper.setStatus(dto.getStatus());
            paper.setUpdatedAt(LocalDateTime.now());
            results.add(mapToResponseDTO(paperRepository.save(paper)));
        }
        return results;
    }

    // ==================== BULK TOGGLE DISCUSSION (BR-3.30) ====================
    @Override
    @Transactional
    public List<PaperResponseDTO> bulkToggleDiscussion(List<Integer> paperIds, boolean enabled) {
        List<PaperResponseDTO> results = new ArrayList<>();
        for (Integer paperId : paperIds) {
            Paper paper = paperRepository.findById(paperId)
                    .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + paperId));
            paper.setIsDiscussionEnabled(enabled);
            paper.setUpdatedAt(LocalDateTime.now());
            results.add(mapToResponseDTO(paperRepository.save(paper)));
        }
        return results;
    }

    // ==================== PUBLISHED PAPERS (Public endpoint) ====================
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaperResponseDTO> getPublishedPapers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submissionTime").descending());
        String searchParam = (search == null || search.isBlank()) ? null : search.trim();
        Page<Paper> papers;
        if (searchParam == null) {
            papers = paperRepository.findByStatus(PaperStatus.PUBLISHED, pageable);
        } else {
            papers = paperRepository.findByStatusAndTitleContainingIgnoreCase(PaperStatus.PUBLISHED, searchParam, pageable);
        }

        // Collect all paper IDs in one go to batch-load authors (prevents N+1)
        List<Integer> paperIds = papers.getContent().stream().map(Paper::getId).toList();
        Map<Integer, List<String>> authorsByPaperId = new java.util.HashMap<>();
        if (!paperIds.isEmpty()) {
            List<PaperAuthor> allAuthors = paperAuthorRepository.findByPaper_IdInOrderByOrderIndexAsc(paperIds);
            for (PaperAuthor pa : allAuthors) {
                String fullName = pa.getUser().getFirstName() + " " + pa.getUser().getLastName();
                authorsByPaperId.computeIfAbsent(pa.getPaper().getId(), k -> new ArrayList<>()).add(fullName);
            }
        }

        return PaginationUtils.toPagedResponse(papers, paper -> mapToPublishedResponseDTO(paper, authorsByPaperId));
    }

    private PaperResponseDTO mapToPublishedResponseDTO(Paper entity, Map<Integer, List<String>> authorsByPaperId) {
        List<Integer> secondaryIds = entity.getSecondarySubjectAreas() != null
                ? entity.getSecondarySubjectAreas().stream().map(SubjectArea::getId).toList()
                : List.of();
        List<String> authorNames = authorsByPaperId.getOrDefault(entity.getId(), List.of());
        return PaperResponseDTO.builder()
                .id(entity.getId())
                .conferenceId(entity.getTrack().getConference().getId())
                .conferenceName(entity.getTrack().getConference().getName())
                .trackId(entity.getTrack().getId())
                .trackName(entity.getTrack().getName())
                .primarySubjectAreaId(
                        entity.getPrimarySubjectArea() != null ? entity.getPrimarySubjectArea().getId() : null)
                .secondarySubjectAreaIds(secondaryIds)
                .title(entity.getTitle())
                .abstractField(entity.getAbstractField())
                .keywords(deserializeKeywords(entity.getKeywordsJson()))
                .submissionTime(entity.getSubmissionTime())
                .status(entity.getStatus())
                .submissionFormId(entity.getSubmissionForm() != null ? entity.getSubmissionForm().getId() : null)
                .extraAnswersJson(entity.getExtraAnswersJson())
                .authorNames(authorNames)
                .build();
    }

    // ==================== VALIDATION HELPERS ====================

    /**
     * BR-2.1 + BR-2.13: Check PAPER_SUBMISSION activity enabled + deadline
     */
    private void validatePaperSubmissionActivity(Integer conferenceId) {
        ConferenceActivity activity = conferenceActivityRepository
                .findByConferenceIdAndActivityType(conferenceId, ActivityType.PAPER_SUBMISSION)
                .orElse(null);

        if (activity == null || !Boolean.TRUE.equals(activity.getIsEnabled())) {
            throw new BadRequestException("Paper submission is not currently open for this conference");
        }

        if (activity.getDeadline() != null && activity.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Paper submission deadline has passed: " + activity.getDeadline());
        }
    }

    /**
     * BR-2.16: Validate paper status transition
     */
    private void validateStatusTransition(PaperStatus current, PaperStatus target) {
        Set<PaperStatus> allowed = VALID_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new BadRequestException(
                    "Invalid status transition: " + current + " → " + target);
        }
    }

    /**
     * BR-2.2: Auto-assign AUTHOR role khi submit paper
     */
    private void autoAssignAuthorRole(Integer conferenceId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl userDetails)) {
                return;
            }
            Integer userId = userDetails.getId();

            // Check if user already has AUTHOR role in this conference
            List<ConferenceUserTrack> existing = conferenceUserTrackRepository
                    .findAllByUser_IdAndConference_Id(userId, conferenceId);
            boolean hasAuthorRole = existing.stream()
                    .anyMatch(cut -> cut.getAssignedRole() == ConferenceTrackRole.AUTHOR);

            if (!hasAuthorRole) {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) return;

                Conference conference = conferenceTrackRepository.findByConferenceId(conferenceId)
                        .stream().findFirst()
                        .map(ConferenceTrack::getConference)
                        .orElse(null);
                if (conference == null) return;

                ConferenceUserTrack authorTrack = new ConferenceUserTrack();
                authorTrack.setUser(user);
                authorTrack.setConference(conference);
                authorTrack.setAssignedRole(ConferenceTrackRole.AUTHOR);
                authorTrack.setInvitedAt(LocalDateTime.now());
                authorTrack.setIsAccepted(true);
                authorTrack.setIsRegistered(true);
                conferenceUserTrackRepository.save(authorTrack);
                log.info("Auto-assigned AUTHOR role to user {} in conference {}", userId, conferenceId);
            }
        } catch (Exception e) {
            log.warn("Could not auto-assign AUTHOR role: {}", e.getMessage());
        }
    }

    // ==================== MAPPING ====================

    private void mapDtoToEntity(PaperDTO dto, Paper entity, ConferenceTrack track) {
        SubjectArea primarySA = null;
        if (dto.getPrimarySubjectAreaId() != null) {
            primarySA = subjectAreaRepository.findById(dto.getPrimarySubjectAreaId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Subject Area not found with ID: " + dto.getPrimarySubjectAreaId()));
        }

        List<SubjectArea> secondarySAs = new ArrayList<>();
        if (dto.getSecondarySubjectAreaIds() != null && !dto.getSecondarySubjectAreaIds().isEmpty()) {
            secondarySAs = subjectAreaRepository.findAllById(dto.getSecondarySubjectAreaIds());
            if (secondarySAs.size() != dto.getSecondarySubjectAreaIds().size()) {
                throw new EntityNotFoundException("One or more secondary Subject Area IDs not found");
            }
        }

        ConferenceSubmissionForm submissionForm = null;
        if (dto.getSubmissionFormId() != null) {
            submissionForm = conferenceSubmissionFormRepository.findById(dto.getSubmissionFormId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Submission Form not found with ID: " + dto.getSubmissionFormId()));
        }

        entity.setTitle(dto.getTitle());
        entity.setAbstractField(dto.getAbstractField());
        entity.setKeywordsJson(serializeKeywords(dto.getKeywords()));
        entity.setSubmissionForm(submissionForm);
        entity.setExtraAnswersJson(dto.getExtraAnswersJson());
        entity.setTrack(track);
        entity.setPrimarySubjectArea(primarySA);
        entity.setSecondarySubjectAreas(secondarySAs);

        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getSubmissionTime() != null) {
            entity.setSubmissionTime(dto.getSubmissionTime());
        }

        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperResponseDTO mapToResponseDTO(Paper entity) {
        List<Integer> secondaryIds = entity.getSecondarySubjectAreas() != null
                ? entity.getSecondarySubjectAreas().stream().map(SubjectArea::getId).toList()
                : List.of();

        Integer trackId = entity.getTrack().getId();
        boolean isDoubleBlind = trackReviewSettingRepository.findByTrackId(trackId)
                .map(setting -> Boolean.TRUE.equals(setting.getIsDoubleBlind()))
                .orElse(true);

        boolean shouldMask = false;
        if (isDoubleBlind) {
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                boolean isAdminOrStaff = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
                if (!isAdminOrStaff && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
                    Integer userId = userDetails.getId();
                    Integer conferenceId = entity.getTrack().getConference().getId();
                    List<ConferenceUserTrack> roles = conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(userId, conferenceId);
                    boolean isChair = roles.stream().anyMatch(r ->
                            r.getAssignedRole() == ConferenceTrackRole.CONFERENCE_CHAIR ||
                            r.getAssignedRole() == ConferenceTrackRole.PROGRAM_CHAIR);
                    boolean isAuthor = paperAuthorRepository.existsByPaperIdAndUserId(entity.getId(), userId);
                    if (!isChair && !isAuthor) {
                         shouldMask = true;
                    }
                }
            } else {
                shouldMask = true;
            }
        }

        // Fetch actual author names unless masked
        List<String> authorNames = List.of();
        if (!shouldMask) {
            List<PaperAuthor> authors = paperAuthorRepository.findByPaper_IdOrderByOrderIndexAsc(entity.getId());
            authorNames = authors.stream()
                    .map(pa -> pa.getUser().getFirstName() + " " + pa.getUser().getLastName())
                    .toList();
        }

        return PaperResponseDTO.builder()
                .id(entity.getId())
                .conferenceId(entity.getTrack().getConference().getId())
                .trackId(entity.getTrack().getId())
                .trackName(entity.getTrack().getName())
                .primarySubjectAreaId(
                        entity.getPrimarySubjectArea() != null ? entity.getPrimarySubjectArea().getId() : null)
                .secondarySubjectAreaIds(secondaryIds)
                .title(entity.getTitle())
                .abstractField(entity.getAbstractField())
                .keywords(deserializeKeywords(entity.getKeywordsJson()))
                .submissionTime(entity.getSubmissionTime())
                .status(entity.getStatus())
                .submissionFormId(entity.getSubmissionForm() != null ? entity.getSubmissionForm().getId() : null)
                .extraAnswersJson(entity.getExtraAnswersJson())
                .isDoubleBlind(isDoubleBlind)
                .authorNames(authorNames)
                .build();
    }

    private String serializeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(keywords);
        } catch (JsonProcessingException e) {
            return String.join(",", keywords);
        }
    }

    private List<String> deserializeKeywords(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of(json.split(","));
        }
    }
}