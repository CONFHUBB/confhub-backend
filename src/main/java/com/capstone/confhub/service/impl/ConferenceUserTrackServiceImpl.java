package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confhub.dto.response.ConferenceResponseDTO;
import com.capstone.confhub.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.dto.response.UserResponseDTO;
import com.capstone.confhub.dto.response.UserWithRolesResponseDTO;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ForbiddenException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.service.ConferenceUserTrackService;
import com.capstone.confhub.service.EmailService;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.capstone.confhub.security.services.UserDetailsImpl;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConferenceUserTrackServiceImpl implements ConferenceUserTrackService {
        private final ConferenceUserTrackRepository conferenceUserTrackRepository;
        private final UserRepository userRepository;
        private final ConferenceRepository conferenceRepository;
        private final ConferenceTrackRepository conferenceTrackRepository;
        private final ReviewRepository reviewRepository;
        private final NotificationRepository notificationRepository;
        private final TrackReviewSettingRepository trackReviewSettingRepository;
        private final EmailService emailService;

        @Value("${app.base-url}")
        private String baseUrl;

        @Value("${app.frontend-url}")
        private String frontendUrl;

        @Override
        @Transactional(readOnly = true)
        public PagedResponse<UserResponseDTO> getTrackChairsByConferenceId(Integer conferenceId, int page, int size) {
                conferenceRepository.findById(conferenceId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Conference not found with id " + conferenceId));

                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                                .findByConference_IdAndAssignedRole(conferenceId, ConferenceTrackRole.PROGRAM_CHAIR);

                List<User> distinctUsers = cuts.stream()
                                .map(ConferenceUserTrack::getUser)
                                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a,
                                                LinkedHashMap::new))
                                .values().stream()
                                .toList();

                List<UserResponseDTO> all = distinctUsers.stream()
                                .map(this::mapUserToResponseDTO)
                                .toList();

                return paginateList(all, page, size);
        }

        @Override
        @Transactional(readOnly = true)
        public PagedResponse<ConferenceResponseDTO> getChairedConferencesByUserId(Integer userId, int page, int size) {
                userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                                .findByUser_IdAndAssignedRole(userId, ConferenceTrackRole.PROGRAM_CHAIR);

                List<Conference> distinctConferences = cuts.stream()
                                .map(ConferenceUserTrack::getConference)
                                .collect(Collectors.toMap(Conference::getId, Function.identity(), (a, b) -> a,
                                                LinkedHashMap::new))
                                .values().stream()
                                .toList();

                List<ConferenceResponseDTO> all = distinctConferences.stream()
                                .map(this::mapConferenceToResponseDTO)
                                .toList();

                return paginateList(all, page, size);
        }

        @Override
        @Transactional(readOnly = true)
        public PagedResponse<ConferenceResponseDTO> getOrganizedConferencesByUserId(Integer userId, int page,
                        int size) {
                userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                                .findByUser_IdAndAssignedRole(userId, ConferenceTrackRole.CONFERENCE_CHAIR);

                List<Conference> distinctConferences = cuts.stream()
                                .map(ConferenceUserTrack::getConference)
                                .collect(Collectors.toMap(Conference::getId, Function.identity(), (a, b) -> a,
                                                LinkedHashMap::new))
                                .values().stream()
                                .toList();

                List<ConferenceResponseDTO> all = distinctConferences.stream()
                                .map(this::mapConferenceToResponseDTO)
                                .toList();

                return paginateList(all, page, size);
        }

        @Override
        @Transactional(readOnly = true)
        public PagedResponse<ConferenceResponseDTO> getReviewerConferencesByUserId(Integer userId, int page, int size) {
                userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                                .findByUser_IdAndAssignedRole(userId, ConferenceTrackRole.REVIEWER);

                List<Conference> distinctConferences = cuts.stream()
                                .filter(cut -> Boolean.TRUE.equals(cut.getIsAccepted()))
                                .map(ConferenceUserTrack::getConference)
                                .collect(Collectors.toMap(Conference::getId, Function.identity(), (a, b) -> a,
                                                LinkedHashMap::new))
                                .values().stream()
                                .toList();

                List<ConferenceResponseDTO> all = distinctConferences.stream()
                                .map(this::mapConferenceToResponseDTO)
                                .toList();

                return paginateList(all, page, size);
        }

        @Override
        @Transactional(readOnly = true)
        public List<ConferenceUserTrackResponseDTO> getUserRoleAssignments(Integer userId) {
                userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository.findByUser_Id(userId);

                return cuts.stream()
                                .map(this::mapToResponseDTO)
                                .toList();
        }

        @Override
        @Transactional
        public ConferenceUserTrackResponseDTO assignRoleToUserTrack(AssignConferenceUserTrackRequest request) {
                requireChairOf(request.getConferenceId());
                User user = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User not found with id " + request.getUserId()));
                Conference conference = conferenceRepository.findById(request.getConferenceId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Conference not found with id " + request.getConferenceId()));

                ConferenceTrack track = null;
                if (request.getTrackId() != null) {
                        track = conferenceTrackRepository.findById(request.getTrackId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Track not found with id " + request.getTrackId()));
                }

                ConferenceUserTrack entity = new ConferenceUserTrack();
                entity.setUser(user);
                entity.setConference(conference);
                entity.setConferenceTrack(track);
                entity.setAssignedRole(request.getAssignedRole());
                entity.setInvitedAt(LocalDateTime.now());
                entity.setInvitationToken(UUID.randomUUID().toString());
                entity.setTokenExpiresAt(LocalDateTime.now().plusDays(7));
                ConferenceUserTrack saved = conferenceUserTrackRepository.save(entity);

                // Always create invitation notification (even for re-invited users)
                String roleName = formatRoleName(request.getAssignedRole());
                String link = "/my-profile/invitations";
                Notification notification = Notification.builder()
                                .user(user)
                                .conference(conference)
                                .title("You have been invited as " + roleName)
                                .message("You have been invited to join \"" + conference.getName() + "\" as "
                                                + roleName + ".")
                                .type("INVITATION")
                                .link(link)
                                .isRead(false)
                                .build();
                notificationRepository.save(notification);

                // Auto-send invitation email
                try {
                        String trackName = track != null ? track.getName() : null;
                        String trackLabel = trackName != null ? " — " + trackName : "";
                        String acceptLink = baseUrl + "/api/v1/email/accept/" + saved.getInvitationToken();
                        String declineLink = baseUrl + "/api/v1/email/decline/" + saved.getInvitationToken();
                        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " "
                                        + (user.getLastName() != null ? user.getLastName() : "");

                        emailService.sendInvitationEmail(
                                        user.getEmail(),
                                        fullName.trim(),
                                        "Invitation to " + conference.getName() + " as " + roleName + trackLabel,
                                        conference.getName(),
                                        roleName,
                                        trackName,
                                        acceptLink,
                                        declineLink,
                                        null,
                                        null);
                } catch (Exception e) {
                        log.error("Failed to send invitation email to {}: {}", user.getEmail(), e.getMessage());
                        // Don't fail the role assignment if email fails
                }

                return mapToResponseDTO(saved);
        }

        @Override
        @Transactional
        public ConferenceUserTrackResponseDTO acceptInvitation(Integer userId, Integer conferenceId, Integer reviewerQuota) {
                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                                .findAllByUser_IdAndConference_Id(userId, conferenceId);
                if (cuts.isEmpty()) {
                        throw new ResourceNotFoundException(
                                        "ConferenceUserTrack not found for userId=" + userId + " conferenceId="
                                                        + conferenceId);
                }
                cuts.forEach(cut -> {
                        cut.setIsAccepted(true);
                        if (reviewerQuota != null && cut.getAssignedRole() == ConferenceTrackRole.REVIEWER
                                        && isReviewerQuotaAllowed(cut)) {
                                cut.setReviewerQuota(reviewerQuota);
                        }
                });
                List<ConferenceUserTrack> saved = conferenceUserTrackRepository.saveAll(cuts);

                // Notify conference chairs that user accepted
                User user = cuts.get(0).getUser();
                Conference conference = cuts.get(0).getConference();
                String roleName = formatRoleName(cuts.get(0).getAssignedRole());
                List<ConferenceUserTrack> chairs = conferenceUserTrackRepository
                                .findByConference_IdAndAssignedRole(conferenceId, ConferenceTrackRole.CONFERENCE_CHAIR);
                for (ConferenceUserTrack chair : chairs) {
                        Notification notification = Notification.builder()
                                        .user(chair.getUser())
                                        .conference(conference)
                                        .title(user.getFirstName() + " " + user.getLastName() + " accepted invitation")
                                        .message(user.getEmail() + " has accepted the " + roleName + " role in \""
                                                        + conference.getName() + "\".")
                                        .type("ROLE_ACCEPTED")
                                        .link("/conference/" + conferenceId + "/update")
                                        .isRead(false)
                                        .build();
                        notificationRepository.save(notification);
                }

                return mapToResponseDTO(saved.get(0));
        }

        @Override
        @Transactional
        public ConferenceUserTrackResponseDTO declineInvitation(Integer userId, Integer conferenceId) {
                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                                .findAllByUser_IdAndConference_Id(userId, conferenceId);
                if (cuts.isEmpty()) {
                        throw new ResourceNotFoundException(
                                        "ConferenceUserTrack not found for userId=" + userId + " conferenceId="
                                                        + conferenceId);
                }
                cuts.forEach(cut -> cut.setIsAccepted(false));
                List<ConferenceUserTrack> saved = conferenceUserTrackRepository.saveAll(cuts);

                // Notify conference chairs that user declined
                User user = cuts.get(0).getUser();
                Conference conference = cuts.get(0).getConference();
                String roleName = formatRoleName(cuts.get(0).getAssignedRole());
                List<ConferenceUserTrack> chairs = conferenceUserTrackRepository
                                .findByConference_IdAndAssignedRole(conferenceId, ConferenceTrackRole.CONFERENCE_CHAIR);
                for (ConferenceUserTrack chair : chairs) {
                        Notification notification = Notification.builder()
                                        .user(chair.getUser())
                                        .conference(conference)
                                        .title(user.getFirstName() + " " + user.getLastName() + " declined invitation")
                                        .message(user.getEmail() + " has declined the " + roleName + " role in \""
                                                        + conference.getName() + "\".")
                                        .type("ROLE_DECLINED")
                                        .link("/conference/" + conferenceId + "/update")
                                        .isRead(false)
                                        .build();
                        notificationRepository.save(notification);
                }

                return mapToResponseDTO(saved.get(0));
        }

        @Override
        @Transactional
        public ConferenceUserTrackResponseDTO acceptByToken(String token, Integer reviewerQuota) {
                ConferenceUserTrack cut = conferenceUserTrackRepository.findByInvitationToken(token)
                                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

                if (cut.getTokenExpiresAt() != null && LocalDateTime.now().isAfter(cut.getTokenExpiresAt())) {
                        throw new BadRequestException(
                                        "Invitation token has expired. Please ask the chair to resend the invitation.");
                }
                if (Boolean.TRUE.equals(cut.getIsAccepted())) {
                        throw new BadRequestException("This invitation has already been accepted.");
                }
                if (Boolean.FALSE.equals(cut.getIsAccepted())) {
                        throw new BadRequestException("This invitation has already been declined.");
                }

                cut.setIsAccepted(true);
                if (reviewerQuota != null && cut.getAssignedRole() == ConferenceTrackRole.REVIEWER
                                && isReviewerQuotaAllowed(cut)) {
                        cut.setReviewerQuota(reviewerQuota);
                }
                ConferenceUserTrack saved = conferenceUserTrackRepository.save(cut);

                // Notify conference chairs
                User user = cut.getUser();
                Conference conference = cut.getConference();
                String roleName = formatRoleName(cut.getAssignedRole());
                List<ConferenceUserTrack> chairs = conferenceUserTrackRepository
                                .findByConference_IdAndAssignedRole(conference.getId(),
                                                ConferenceTrackRole.CONFERENCE_CHAIR);
                for (ConferenceUserTrack chair : chairs) {
                        Notification notification = Notification.builder()
                                        .user(chair.getUser())
                                        .conference(conference)
                                        .title(user.getFirstName() + " " + user.getLastName()
                                                        + " accepted invitation (via email)")
                                        .message(user.getEmail() + " has accepted the " + roleName + " role in \""
                                                        + conference.getName() + "\".")
                                        .type("ROLE_ACCEPTED")
                                        .link("/conference/" + conference.getId() + "/update")
                                        .isRead(false)
                                        .build();
                        notificationRepository.save(notification);
                }

                return mapToResponseDTO(saved);
        }

        @Override
        @Transactional
        public ConferenceUserTrackResponseDTO declineByToken(String token) {
                ConferenceUserTrack cut = conferenceUserTrackRepository.findByInvitationToken(token)
                                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

                if (cut.getTokenExpiresAt() != null && LocalDateTime.now().isAfter(cut.getTokenExpiresAt())) {
                        throw new BadRequestException("Invitation token has expired.");
                }
                if (Boolean.TRUE.equals(cut.getIsAccepted())) {
                        throw new BadRequestException("This invitation has already been accepted.");
                }
                if (Boolean.FALSE.equals(cut.getIsAccepted())) {
                        throw new BadRequestException("This invitation has already been declined.");
                }

                cut.setIsAccepted(false);
                ConferenceUserTrack saved = conferenceUserTrackRepository.save(cut);

                // Notify conference chairs
                User user = cut.getUser();
                Conference conference = cut.getConference();
                String roleName = formatRoleName(cut.getAssignedRole());
                List<ConferenceUserTrack> chairs = conferenceUserTrackRepository
                                .findByConference_IdAndAssignedRole(conference.getId(),
                                                ConferenceTrackRole.CONFERENCE_CHAIR);
                for (ConferenceUserTrack chair : chairs) {
                        Notification notification = Notification.builder()
                                        .user(chair.getUser())
                                        .conference(conference)
                                        .title(user.getFirstName() + " " + user.getLastName()
                                                        + " declined invitation (via email)")
                                        .message(user.getEmail() + " has declined the " + roleName + " role in \""
                                                        + conference.getName() + "\".")
                                        .type("ROLE_DECLINED")
                                        .link("/conference/" + conference.getId() + "/update")
                                        .isRead(false)
                                        .build();
                        notificationRepository.save(notification);
                }

                return mapToResponseDTO(saved);
        }

        @Override
        @Transactional
        public ConferenceUserTrackResponseDTO resendInvitation(Integer conferenceUserTrackId) {
                ConferenceUserTrack cut = conferenceUserTrackRepository.findById(conferenceUserTrackId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "ConferenceUserTrack not found with id " + conferenceUserTrackId));
                requireChairOf(cut.getConference().getId());

                // Reset token and expiry — invalidates old link
                cut.setInvitationToken(UUID.randomUUID().toString());
                cut.setTokenExpiresAt(LocalDateTime.now().plusDays(7));
                cut.setIsAccepted(null); // Reset to pending
                ConferenceUserTrack saved = conferenceUserTrackRepository.save(cut);

                return mapToResponseDTO(saved);
        }

        @Override
        @Transactional(readOnly = true)
        public PagedResponse<UserWithRolesResponseDTO> getConferenceUsersWithRoles(Integer conferenceId, int page,
                        int size) {
                conferenceRepository.findById(conferenceId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Conference not found with id " + conferenceId));

                List<ConferenceUserTrack> allCuts = conferenceUserTrackRepository.findByConference_Id(conferenceId);

                // Group by user, preserving insertion order
                Map<Integer, List<ConferenceUserTrack>> grouped = new LinkedHashMap<>();
                for (ConferenceUserTrack cut : allCuts) {
                        grouped.computeIfAbsent(cut.getUser().getId(), k -> new ArrayList<>()).add(cut);
                }

                List<UserWithRolesResponseDTO> all = grouped.values().stream()
                                .map(cuts -> {
                                        User user = cuts.get(0).getUser();
                                        List<ConferenceUserTrackResponseDTO> roleDtos = cuts.stream()
                                                        .map(this::mapToResponseDTO)
                                                        .toList();
                                        return UserWithRolesResponseDTO.builder()
                                                        .user(mapUserToResponseDTO(user))
                                                        .roles(roleDtos)
                                                        .build();
                                })
                                .toList();

                return paginateList(all, page, size);
        }

        @Override
        @Transactional
        public void removeRoleFromUser(Integer conferenceUserTrackId) {
                ConferenceUserTrack cut = conferenceUserTrackRepository.findById(conferenceUserTrackId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "ConferenceUserTrack not found with id " + conferenceUserTrackId));
                requireChairOf(cut.getConference().getId());

                // BR-1.9a: Không xóa CONFERENCE_CHAIR cuối cùng
                if (cut.getAssignedRole() == ConferenceTrackRole.CONFERENCE_CHAIR) {
                        long chairCount = conferenceUserTrackRepository
                                        .findByConference_IdAndAssignedRole(
                                                        cut.getConference().getId(),
                                                        ConferenceTrackRole.CONFERENCE_CHAIR)
                                        .size();
                        if (chairCount <= 1) {
                                throw new BadRequestException(
                                                "Cannot remove the last Conference Chair. A conference must have at least one chair.");
                        }
                }

                // BR-1.9b: Không gỡ REVIEWER nếu còn assignments
                if (cut.getAssignedRole() == ConferenceTrackRole.REVIEWER) {
                        long assignmentCount = reviewRepository.countByReviewer_IdAndPaper_Track_Conference_Id(
                                        cut.getUser().getId(), cut.getConference().getId());
                        if (assignmentCount > 0) {
                                throw new BadRequestException(
                                                "Cannot remove REVIEWER role: this reviewer still has "
                                                                + assignmentCount
                                                                + " paper assignment(s). Remove assignments first.");
                        }
                }

                // Always notify the removed user (per-role notification)
                String roleName = formatRoleName(cut.getAssignedRole());
                String trackLabel = cut.getConferenceTrack() != null
                                ? " (Track: " + cut.getConferenceTrack().getName() + ")"
                                : "";
                Notification notification = Notification.builder()
                                .user(cut.getUser())
                                .conference(cut.getConference())
                                .title("Your " + roleName + " role has been removed")
                                .message("Your " + roleName + " role" + trackLabel + " in \""
                                                + cut.getConference().getName()
                                                + "\" has been removed by the conference chair.")
                                .type("ROLE_REMOVED")
                                .link("/conference/" + cut.getConference().getId())
                                .isRead(false)
                                .build();
                notificationRepository.save(notification);

                // Send removal email
                try {
                        User removedUser = cut.getUser();
                        String fullName = (removedUser.getFirstName() != null ? removedUser.getFirstName() : "") + " "
                                        + (removedUser.getLastName() != null ? removedUser.getLastName() : "");
                        emailService.sendSimpleMessage(
                                        removedUser.getEmail(),
                                        "Role Removed — " + cut.getConference().getName(),
                                        "Dear " + fullName.trim() + ",\n\n"
                                                        + "Your " + roleName + " role" + trackLabel + " in \""
                                                        + cut.getConference().getName()
                                                        + "\" has been removed by the conference chair.\n\n"
                                                        + "If you believe this was a mistake, please contact the conference organizer.\n\n"
                                                        + "Best regards,\nConfHub System");
                } catch (Exception e) {
                        log.error("Failed to send role-removal email to {}: {}", cut.getUser().getEmail(),
                                        e.getMessage());
                }

                conferenceUserTrackRepository.deleteById(conferenceUserTrackId);
        }

        private ConferenceUserTrackResponseDTO mapToResponseDTO(ConferenceUserTrack entity) {
                ConferenceUserTrackResponseDTO dto = new ConferenceUserTrackResponseDTO();
                dto.setId(entity.getId());
                dto.setUserId(entity.getUser().getId());
                dto.setConferenceId(entity.getConference().getId());
                dto.setConferenceTrackId(
                                entity.getConferenceTrack() != null ? entity.getConferenceTrack().getId() : null);
                dto.setAssignedRole(entity.getAssignedRole());
                dto.setInvitedAt(entity.getInvitedAt());
                dto.setIsAccepted(entity.getIsAccepted());
                dto.setIsRegistered(entity.getIsRegistered());
                dto.setInvitationToken(entity.getInvitationToken());
                dto.setTokenExpiresAt(entity.getTokenExpiresAt());
                dto.setReviewerQuota(entity.getReviewerQuota());
                dto.setCreatedAt(entity.getCreatedAt());
                dto.setUpdatedAt(entity.getUpdatedAt());
                return dto;
        }

        private UserResponseDTO mapUserToResponseDTO(User entity) {
                return UserResponseDTO.builder()
                                .id(entity.getId())
                                .title(entity.getTitle())
                                .firstName(entity.getFirstName())
                                .lastName(entity.getLastName())
                                .gender(entity.getGender())
                                .email(entity.getEmail())
                                .country(entity.getCountry())
                                .isActive(entity.getIsActive())
                                .createdAt(entity.getCreatedAt())
                                .build();
        }

        private ConferenceResponseDTO mapConferenceToResponseDTO(Conference entity) {
                return ConferenceResponseDTO.builder()
                                .id(entity.getId())
                                .name(entity.getName())
                                .acronym(entity.getAcronym())
                                .description(entity.getDescription())
                                .location(entity.getLocation())
                                .startDate(entity.getStartDate())
                                .endDate(entity.getEndDate())
                                .status(entity.getStatus())
                                .createdAt(entity.getCreatedAt())
                                .build();
        }

        private <T> PagedResponse<T> paginateList(List<T> items, int page, int size) {
                int totalElements = items.size();
                int fromIndex = Math.min(page * size, totalElements);
                int toIndex = Math.min(fromIndex + size, totalElements);

                List<T> content = items.subList(fromIndex, toIndex);

                int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
                boolean last = totalPages == 0 || page >= totalPages - 1;

                return PagedResponse.<T>builder()
                                .content(content)
                                .page(page)
                                .size(size)
                                .totalElements(totalElements)
                                .totalPages(totalPages)
                                .last(last)
                                .build();
        }

        private String formatRoleName(ConferenceTrackRole role) {
                return switch (role) {
                        case CONFERENCE_CHAIR -> "Conference Chair";
                        case PROGRAM_CHAIR -> "Program Chair";
                        case REVIEWER -> "Reviewer";
                        case AUTHOR -> "Author";
                        case ATTENDEE -> "Attendee";
                };
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
                boolean isChair = conferenceUserTrackRepository
                                .existsByUser_IdAndConference_IdAndAssignedRole(
                                                u.getId(), conferenceId, ConferenceTrackRole.CONFERENCE_CHAIR);
                if (!isChair) throw new ForbiddenException(
                                "Only the CONFERENCE_CHAIR of this conference can perform this action.");
        }

        /**
         * Check if reviewer quota is allowed for the track associated with this ConferenceUserTrack.
         */
        private boolean isReviewerQuotaAllowed(ConferenceUserTrack cut) {
                ConferenceTrack track = cut.getConferenceTrack();
                if (track == null) return false;
                return trackReviewSettingRepository.findByTrackId(track.getId())
                                .map(TrackReviewSetting::getAllowReviewerQuota)
                                .orElse(false);
        }

        @Override
        @Transactional
        public ConferenceUserTrackResponseDTO updateReviewerQuota(Integer userId, Integer conferenceId, Integer reviewerQuota) {
                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                                .findAllByUser_IdAndConference_Id(userId, conferenceId);
                if (cuts.isEmpty()) {
                        throw new ResourceNotFoundException(
                                        "ConferenceUserTrack not found for userId=" + userId + " conferenceId="
                                                        + conferenceId);
                }
                // Update quota on all REVIEWER records where allowReviewerQuota is enabled
                cuts.stream()
                        .filter(cut -> cut.getAssignedRole() == ConferenceTrackRole.REVIEWER)
                        .filter(this::isReviewerQuotaAllowed)
                        .forEach(cut -> cut.setReviewerQuota(reviewerQuota));
                List<ConferenceUserTrack> saved = conferenceUserTrackRepository.saveAll(cuts);
                return mapToResponseDTO(saved.get(0));
        }

        @Override
        @Transactional(readOnly = true)
        public Integer getReviewerQuota(Integer userId, Integer conferenceId) {
                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                                .findAllByUser_IdAndConference_Id(userId, conferenceId);
                return cuts.stream()
                        .filter(cut -> cut.getAssignedRole() == ConferenceTrackRole.REVIEWER)
                        .map(ConferenceUserTrack::getReviewerQuota)
                        .filter(q -> q != null)
                        .findFirst()
                        .orElse(null);
        }
}
