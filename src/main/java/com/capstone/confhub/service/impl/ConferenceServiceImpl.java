package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ConferenceDTO;
import com.capstone.confhub.dto.response.ConferenceResponseDTO;
import com.capstone.confhub.dto.response.ConferenceStatsDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ForbiddenException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.security.services.UserDetailsImpl;
import com.capstone.confhub.integration.payment.VnPayIntegrationService;
import com.capstone.confhub.service.ConferenceService;
import com.capstone.confhub.service.ConferenceActivityService;
import com.capstone.confhub.utils.PaginationUtils;
import com.capstone.confhub.utils.enums.ConferenceStatus;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import com.capstone.confhub.utils.enums.PaperStatus;
import com.capstone.confhub.utils.enums.SubscriptionPlan;
import com.capstone.confhub.entity.Notification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ConferenceServiceImpl implements ConferenceService {

    private final ConferenceRepository repository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ConferenceActivityService conferenceActivityService;
    private final PaperRepository paperRepository;
    private final ReviewRepository reviewRepository;
    private final TicketRepository ticketRepository;
    private final VnPayIntegrationService vnPayIntegrationService;

    @Override
    @Transactional
    public ConferenceResponseDTO createConference(ConferenceDTO dto) {
        log.info("Creating conference: {}", dto.getName());
        Conference conference = new Conference();
        mapDtoToEntity(dto, conference);
        conference.setStatus(ConferenceStatus.SETUP);
        Conference savedConference = repository.save(conference);

        User currentUser = getCurrentAuthenticatedUser();

        ConferenceUserTrack organizerTrack = new ConferenceUserTrack();
        organizerTrack.setUser(currentUser);
        organizerTrack.setConference(savedConference);
        organizerTrack.setAssignedRole(ConferenceTrackRole.CONFERENCE_CHAIR);
        organizerTrack.setInvitedAt(LocalDateTime.now());
        organizerTrack.setIsAccepted(true);
        organizerTrack.setIsRegistered(true);
        conferenceUserTrackRepository.save(organizerTrack);
        
        // Auto-initialize standard timeline activities
        conferenceActivityService.initializeDefaultActivitiesForConference(savedConference.getId());

        // Notification: conference created
        Notification notification = Notification.builder()
                .user(currentUser)
                .conference(savedConference)
                .title("Conference created successfully")
                .message("Your conference \"" + savedConference.getName() + "\" has been created. Complete the setup and submit for approval.")
                .type("CONFERENCE_CREATED")
                .link("/conference/" + savedConference.getId() + "/update?tab=dashboard")
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        return mapToResponseDTO(savedConference);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ConferenceResponseDTO> getAllConferences(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Conference> conferences = repository.findAll(pageable);

        return PaginationUtils.toPagedResponse(conferences, this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ConferenceResponseDTO getByIdConference(Integer id) {
        return repository.findById(id)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));
    }

    @Override
    @Transactional
    public ConferenceResponseDTO updateConference(Integer id, ConferenceDTO dto) {
        requireChairOf(id);
        Conference existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        // BR-1.3: Không cho sửa conference đã COMPLETED hoặc CANCELLED
        if (existing.getStatus() == ConferenceStatus.COMPLETED || existing.getStatus() == ConferenceStatus.CANCELLED) {
            throw new BadRequestException("Cannot update a conference with status " + existing.getStatus());
        }

        mapDtoToEntity(dto, existing);
        return mapToResponseDTO(repository.save(existing));
    }

    @Override
    @Transactional
    public void deleteConference(Integer id) {
        requireChairOf(id);
        log.warn("Deleting conference ID: {}", id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. Conference not found with id " + id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public ConferenceResponseDTO openSubmissions(Integer id) {
        requireChairOf(id);
        log.info("Opening submissions for conference ID: {}", id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        // Must have a subscription plan selected (APPROVED + plan chosen)
        if (conference.getStatus() != ConferenceStatus.OPEN) {
            throw new BadRequestException(
                    "Can only manage submissions for OPEN conferences. Current status: " + conference.getStatus());
        }

        return mapToResponseDTO(conference);
    }

    @Override
    @Transactional
    public ConferenceResponseDTO approveConference(Integer id) {
        log.info("Approving conference ID: {}", id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        if (conference.getStatus() != ConferenceStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only conferences with PENDING_APPROVAL status can be approved.");
        }
        conference.setStatus(ConferenceStatus.PENDING_PAYMENT);
        conference.setRejectionReason(null);
        Conference saved = repository.save(conference);
        notifyAllMembers(saved, "Conference approved!",
                "\"" + saved.getName() + "\" has been approved. Please select a subscription plan to activate it.", "CONFERENCE_STATUS");
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public ConferenceResponseDTO rejectConference(Integer id, String reason) {
        log.info("Rejecting conference ID: {}", id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        if (conference.getStatus() != ConferenceStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only conferences with PENDING_APPROVAL status can be rejected.");
        }
        conference.setStatus(ConferenceStatus.REJECTED);
        conference.setRejectionReason(reason);
        Conference saved = repository.save(conference);
        notifyAllMembers(saved, "Conference rejected",
                "\"" + saved.getName() + "\" has been rejected. Reason: " + reason, "CONFERENCE_STATUS");
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public ConferenceResponseDTO submitForApproval(Integer id) {
        requireChairOf(id);
        log.info("Submitting conference ID: {} for approval", id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        if (conference.getStatus() != ConferenceStatus.SETUP && conference.getStatus() != ConferenceStatus.REJECTED) {
            throw new BadRequestException(
                    "Can only submit for approval from SETUP or REJECTED status. Current status: " + conference.getStatus());
        }
        conference.setStatus(ConferenceStatus.PENDING_APPROVAL);
        conference.setRejectionReason(null);
        Conference saved = repository.save(conference);

        // Notify admins/staff (notify all members for simplicity)
        notifyAllMembers(saved, "Conference submitted for approval",
                "\"" + saved.getName() + "\" has been submitted for approval.", "CONFERENCE_STATUS");
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public Map<String, Object> selectPlan(Integer id, String plan, String ipAddr) {
        requireChairOf(id);
        log.info("Selecting plan {} for conference ID: {}", plan, id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        if (conference.getStatus() != ConferenceStatus.PENDING_PAYMENT) {
            throw new BadRequestException(
                    "Can only select a plan for PENDING_PAYMENT conferences. Current status: " + conference.getStatus());
        }

        SubscriptionPlan selectedPlan;
        try {
            selectedPlan = SubscriptionPlan.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid subscription plan: " + plan);
        }

        conference.setSubscriptionPlan(selectedPlan);

        Map<String, Object> result = new HashMap<>();

        // For STARTER (free), activate immediately
        if (selectedPlan == SubscriptionPlan.STARTER) {
            conference.setStatus(ConferenceStatus.SETUP);
            Conference saved = repository.save(conference);
            notifyAllMembers(saved, "Conference is now active",
                    "\"" + saved.getName() + "\" has been activated with Starter plan.", "CONFERENCE_STATUS");
            result.put("conference", mapToResponseDTO(saved));
            return result;
        }

        // For paid plans: set PENDING_PAYMENT and generate VNPay URL
        conference.setStatus(ConferenceStatus.PENDING_PAYMENT);
        Conference saved = repository.save(conference);

        long amount = selectedPlan == SubscriptionPlan.PROFESSIONAL ? 500000L : 2000000L;
        String orderInfo = "CONF_SUB_" + saved.getId();
        String txnRef = "CONFSUB" + saved.getId() + "_" + System.currentTimeMillis();
        String paymentUrl = vnPayIntegrationService.createPaymentUrl(amount, ipAddr, orderInfo, txnRef);

        result.put("conference", mapToResponseDTO(saved));
        result.put("paymentUrl", paymentUrl);
        return result;
    }

    @Override
    @Transactional
    public ConferenceResponseDTO completeConference(Integer id) {
        requireChairOf(id);
        log.info("Completing conference ID: {}", id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        // BR-1.3: Chỉ OPEN mới complete
        if (conference.getStatus() != ConferenceStatus.OPEN) {
            throw new BadRequestException(
                    "Can only complete OPEN conferences. Current status: " + conference.getStatus());
        }
        conference.setStatus(ConferenceStatus.COMPLETED);
        Conference saved = repository.save(conference);
        notifyAllMembers(saved, "Conference completed",
                "\"" + saved.getName() + "\" has been marked as completed.", "CONFERENCE_STATUS");
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public ConferenceResponseDTO cancelConference(Integer id) {
        requireChairOf(id);
        log.info("Cancelling conference ID: {}", id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        // BR-1.3: Không cancel nếu đã COMPLETED hoặc đã CANCELLED
        if (conference.getStatus() == ConferenceStatus.COMPLETED
                || conference.getStatus() == ConferenceStatus.CANCELLED) {
            throw new BadRequestException(
                    "Cannot cancel a conference with status " + conference.getStatus());
        }
        conference.setStatus(ConferenceStatus.CANCELLED);
        Conference saved = repository.save(conference);
        notifyAllMembers(saved, "Conference cancelled",
                "\"" + saved.getName() + "\" has been cancelled by the chair.", "CONFERENCE_STATUS");
        return mapToResponseDTO(saved);
    }

    @Override
    public String getProgramSchedule(Integer conferenceId) {
        return repository.findById(conferenceId)
                .map(Conference::getProgramSchedule)
                .orElse(null);
    }

    @Override
    public void updateProgramSchedule(Integer conferenceId, String programScheduleJson) {
        requireChairOrProgramChairOf(conferenceId);

        // Parse and validate schedule for overlaps
        if (programScheduleJson != null && !programScheduleJson.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(programScheduleJson);
                JsonNode days = root.path("schedule").path("days");
                if (days.isArray()) {
                    for (JsonNode day : days) {
                        String date = day.path("date").asText();
                        JsonNode sessions = day.path("sessions");
                        if (sessions.isArray()) {
                            List<JsonNode> sessionNodes = new ArrayList<>();
                            sessions.forEach(sessionNodes::add);

                            Map<String, List<JsonNode>> locSessions = new HashMap<>();
                            List<JsonNode> globalSessions = new ArrayList<>();

                            for (JsonNode s : sessionNodes) {
                                if (s.path("isGlobal").asBoolean()) {
                                    globalSessions.add(s);
                                } else {
                                    String loc = s.has("location") ? s.path("location").asText() : s.path("locationId").asText();
                                    if (loc == null || loc.isBlank()) {
                                        loc = "unassigned-" + s.path("id").asText();
                                    }
                                    locSessions.computeIfAbsent(loc, k -> new ArrayList<>()).add(s);
                                }
                            }

                            // Check global overlaps
                            checkOverlap(globalSessions, date);
                            // Combine global + local and check overlaps per location
                            for (Map.Entry<String, List<JsonNode>> entry : locSessions.entrySet()) {
                                List<JsonNode> combined = new ArrayList<>(entry.getValue());
                                combined.addAll(globalSessions);
                                checkOverlap(combined, date);
                            }
                        }
                    }
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new BadRequestException("Invalid program schedule JSON format.");
            }
        }

        Conference conf = repository.findById(conferenceId)
            .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + conferenceId));
        conf.setProgramSchedule(programScheduleJson);
        repository.save(conf);
    }

    private void checkOverlap(List<JsonNode> sessions, String date) {
        if (sessions.size() < 2) return;
        
        sessions.sort((s1, s2) -> {
            String t1 = s1.path("startTime").asText();
            String t2 = s2.path("startTime").asText();
            return t1.compareTo(t2);
        });

        for (int i = 0; i < sessions.size() - 1; i++) {
            JsonNode current = sessions.get(i);
            JsonNode next = sessions.get(i + 1);
            
            String currentEnd = current.path("endTime").asText();
            String nextStart = next.path("startTime").asText();
            
            if (currentEnd.compareTo(nextStart) > 0) {
                String title1 = current.path("title").asText();
                String title2 = next.path("title").asText();
                throw new BadRequestException("Time overlap detected on " + date + " between '" + title1 + "' and '" + title2 + "'. Please adjust the schedule.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConferenceStatsDTO getConferenceStats(Integer conferenceId) {
        requireChairOrProgramChairOf(conferenceId);
        List<Paper> papers = paperRepository.findByTrack_Conference_Id(conferenceId);
        int total = papers.size();
        int submitted = (int) papers.stream().filter(p -> p.getStatus() == PaperStatus.SUBMITTED).count();
        int underReview = (int) papers.stream().filter(p -> p.getStatus() == PaperStatus.UNDER_REVIEW || p.getStatus() == PaperStatus.AWAITING_DECISION).count();
        int accepted = (int) papers.stream().filter(p -> p.getStatus() != null && (p.getStatus() == PaperStatus.ACCEPTED || p.getStatus() == PaperStatus.AWAITING_REGISTRATION || p.getStatus() == PaperStatus.REGISTERED || p.getStatus() == PaperStatus.CAMERA_READY_SUBMITTED || p.getStatus() == PaperStatus.PUBLISHED)).count();
        int rejected = (int) papers.stream().filter(p -> p.getStatus() == PaperStatus.REJECTED).count();

        long totalReviews = reviewRepository.countByConferenceId(conferenceId);
        long completedReviews = reviewRepository.countCompletedByConferenceId(conferenceId);

        long registrations = ticketRepository.countByConference_Id(conferenceId);
        long checkedIn = ticketRepository.countCheckedInByConferenceId(conferenceId, true);

        double acceptanceRate = submitted > 0 ? Math.round((accepted * 100.0 / submitted) * 10.0) / 10.0 : 0;
        double reviewRate = totalReviews > 0 ? Math.round((completedReviews * 100.0 / totalReviews) * 10.0) / 10.0 : 0;

        return ConferenceStatsDTO.builder()
                .totalPapers(total)
                .submitted(submitted)
                .underReview(underReview)
                .accepted(accepted)
                .rejected(rejected)
                .totalReviews((int) totalReviews)
                .completedReviews((int) completedReviews)
                .totalRegistrations((int) registrations)
                .checkedIn((int) checkedIn)
                .acceptanceRate(acceptanceRate)
                .reviewCompletionRate(reviewRate)
                .build();
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new BadRequestException("No authenticated user found");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    private UserDetailsImpl getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new BadRequestException("No authenticated user found");
        }
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    private void requireChairOf(Integer conferenceId) {
        UserDetailsImpl u = getCurrentUserDetails();
        boolean isAdmin = u.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return; // ADMIN bypasses conference-level checks
        boolean isChair = conferenceUserTrackRepository
                .existsByUser_IdAndConference_IdAndAssignedRole(
                        u.getId(), conferenceId, ConferenceTrackRole.CONFERENCE_CHAIR);
        if (!isChair) throw new ForbiddenException(
                "Only the CONFERENCE_CHAIR of this conference can perform this action.");
    }

    private void requireChairOrProgramChairOf(Integer conferenceId) {
        UserDetailsImpl u = getCurrentUserDetails();
        boolean isAdmin = u.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return; // ADMIN bypasses conference-level checks
        boolean ok = conferenceUserTrackRepository
                .existsByUser_IdAndConference_IdAndAssignedRole(
                        u.getId(), conferenceId, ConferenceTrackRole.CONFERENCE_CHAIR)
                || conferenceUserTrackRepository
                        .existsByUser_IdAndConference_IdAndAssignedRole(
                                u.getId(), conferenceId, ConferenceTrackRole.PROGRAM_CHAIR);
        if (!ok) throw new ForbiddenException(
                "Only CONFERENCE_CHAIR or PROGRAM_CHAIR of this conference can perform this action.");
    }

    private void mapDtoToEntity(ConferenceDTO dto, Conference entity) {
        entity.setName(dto.getName());
        entity.setAcronym(dto.getAcronym());
        entity.setDescription(dto.getDescription());
        entity.setLocation(dto.getLocation());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setWebsiteUrl(dto.getWebsiteUrl());
        entity.setArea(dto.getArea());
        entity.setSocietySponsor(dto.getSocietySponsor());

        entity.setCountry(dto.getCountry());
        entity.setProvince(dto.getProvince());
        entity.setBannerImageUrl(dto.getBannerImageUrl());
        entity.setContactInformation(dto.getContactInformation());
        entity.setChairEmails(dto.getChairEmails());
    }

    private ConferenceResponseDTO mapToResponseDTO(Conference entity) {
        return ConferenceResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .acronym(entity.getAcronym())
                .description(entity.getDescription())
                .location(entity.getLocation())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .websiteUrl(entity.getWebsiteUrl())
                .createdAt(entity.getCreatedAt())
                .area(entity.getArea())
                .societySponsor(entity.getSocietySponsor())

                .country(entity.getCountry())
                .province(entity.getProvince())
                .bannerImageUrl(entity.getBannerImageUrl())
                .contactInformation(entity.getContactInformation())
                .chairEmails(entity.getChairEmails())
                .programSchedule(entity.getProgramSchedule())
                .rejectionReason(entity.getRejectionReason())
                .subscriptionPlan(entity.getSubscriptionPlan())
                .build();
    }

    private void notifyAllMembers(Conference conference, String title, String message, String type) {
        List<ConferenceUserTrack> allMembers = conferenceUserTrackRepository.findByConference_Id(conference.getId());
        // Deduplicate by userId
        allMembers.stream()
                .map(cut -> cut.getUser().getId())
                .distinct()
                .forEach(uid -> {
                    User user = userRepository.findById(uid).orElse(null);
                    if (user != null) {
                        Notification n = Notification.builder()
                                .user(user)
                                .conference(conference)
                                .title(title)
                                .message(message)
                                .type(type)
                                .link("/conference/" + conference.getId())
                                .isRead(false)
                                .build();
                        notificationRepository.save(n);
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAttendeesCsv(Integer conferenceId) {
        // Validate if conference exists
        repository.findById(conferenceId).orElseThrow(() -> new ResourceNotFoundException("Conference not found with id: " + conferenceId));

        // Check permissions: requires CHAIR or ADMIN/STAFF
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Unauthorized");
        }
        boolean isStaffOrAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        
        if (!isStaffOrAdmin) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            boolean isChair = conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
                    userDetails.getId(), conferenceId, ConferenceTrackRole.CONFERENCE_CHAIR);
            if (!isChair) {
                throw new ForbiddenException("User is not authorized to export attendees");
            }
        }

        List<com.capstone.confhub.entity.Ticket> tickets = ticketRepository.findByConferenceId(conferenceId);
        StringBuilder sb = new StringBuilder();
        sb.append("Registration Number,First Name,Last Name,Email,Gender,Payment Status,Checked In\n");
        for (com.capstone.confhub.entity.Ticket t : tickets) {
            sb.append(escapeCsv(t.getRegistrationNumber())).append(",")
              .append(escapeCsv(t.getUser().getFirstName())).append(",")
              .append(escapeCsv(t.getUser().getLastName())).append(",")
              .append(escapeCsv(t.getUser().getEmail())).append(",")
              .append(escapeCsv(t.getUser().getGender())).append(",")
              .append(t.getPaymentStatus() != null ? t.getPaymentStatus().name() : "").append(",")
              .append(t.getIsCheckedIn() != null && t.getIsCheckedIn() ? "Yes" : "No").append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}