package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.dto.response.UserResponseDTO;
import com.capstone.confms.dto.response.UserWithRolesResponseDTO;
import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.User;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.ReviewRepository;
import com.capstone.confms.repository.NotificationRepository;
import com.capstone.confms.entity.Notification;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.ConferenceUserTrackService;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConferenceUserTrackServiceImpl implements ConferenceUserTrackService {
        private final ConferenceUserTrackRepository conferenceUserTrackRepository;
        private final UserRepository userRepository;
        private final ConferenceRepository conferenceRepository;
        private final ConferenceTrackRepository conferenceTrackRepository;
        private final ReviewRepository reviewRepository;
        private final NotificationRepository notificationRepository;

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
                User user = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + request.getUserId()));
                Conference conference = conferenceRepository.findById(request.getConferenceId())
                                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + request.getConferenceId()));

                ConferenceTrack track = null;
                if (request.getTrackId() != null) {
                        track = conferenceTrackRepository.findById(request.getTrackId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Track not found with id " + request.getTrackId()));
                }

                ConferenceUserTrack entity = new ConferenceUserTrack();
                entity.setUser(user);
                entity.setConference(conference);
                entity.setConferenceTrack(track);
                entity.setAssignedRole(request.getAssignedRole());
                entity.setInvitedAt(LocalDateTime.now());
                ConferenceUserTrack saved = conferenceUserTrackRepository.save(entity);

                // Auto-create notification — only if not already sent for this user+conference
                boolean alreadyNotified = notificationRepository
                                .existsByUser_IdAndConference_IdAndType(user.getId(), conference.getId(), "INVITATION");
                if (!alreadyNotified) {
                        String roleName = formatRoleName(request.getAssignedRole());
                        Notification notification = Notification.builder()
                                        .user(user)
                                        .conference(conference)
                                        .title("You have been invited as " + roleName)
                                        .message("You have been invited to join \"" + conference.getName() + "\" as " + roleName + ".")
                                        .type("INVITATION")
                                        .link("/conference/reviewer-select")
                                        .isRead(false)
                                        .build();
                        notificationRepository.save(notification);
                }

                return mapToResponseDTO(saved);
        }

        @Override
        @Transactional
        public ConferenceUserTrackResponseDTO acceptInvitation(Integer userId, Integer conferenceId) {
                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                                .findAllByUser_IdAndConference_Id(userId, conferenceId);
                if (cuts.isEmpty()) {
                        throw new ResourceNotFoundException(
                                        "ConferenceUserTrack not found for userId=" + userId + " conferenceId="
                                                        + conferenceId);
                }
                cuts.forEach(cut -> cut.setIsAccepted(true));
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
                                        .message(user.getEmail() + " has accepted the " + roleName + " role in \"" + conference.getName() + "\".")
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
                                        .message(user.getEmail() + " has declined the " + roleName + " role in \"" + conference.getName() + "\".")
                                        .type("ROLE_DECLINED")
                                        .link("/conference/" + conferenceId + "/update")
                                        .isRead(false)
                                        .build();
                        notificationRepository.save(notification);
                }

                return mapToResponseDTO(saved.get(0));
        }

        @Override
        @Transactional(readOnly = true)
        public PagedResponse<UserWithRolesResponseDTO> getConferenceUsersWithRoles(Integer conferenceId, int page, int size) {
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

                // BR-1.9a: Không xóa CONFERENCE_CHAIR cuối cùng
                if (cut.getAssignedRole() == ConferenceTrackRole.CONFERENCE_CHAIR) {
                        long chairCount = conferenceUserTrackRepository
                                        .findByConference_IdAndAssignedRole(
                                                        cut.getConference().getId(), ConferenceTrackRole.CONFERENCE_CHAIR)
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
                                                "Cannot remove REVIEWER role: this reviewer still has " + assignmentCount
                                                                + " paper assignment(s). Remove assignments first.");
                        }
                }

                // Notify the removed user
                String roleName = formatRoleName(cut.getAssignedRole());
                boolean hasOtherRolesInConference = conferenceUserTrackRepository
                                .findAllByUser_IdAndConference_Id(cut.getUser().getId(), cut.getConference().getId())
                                .stream().anyMatch(c -> !c.getId().equals(conferenceUserTrackId));
                if (!hasOtherRolesInConference) {
                        // Only notify when removing the last role entry (avoid spam for multi-track removals)
                        Notification notification = Notification.builder()
                                        .user(cut.getUser())
                                        .conference(cut.getConference())
                                        .title("Your " + roleName + " role has been removed")
                                        .message("Your " + roleName + " role in \"" + cut.getConference().getName() + "\" has been removed by the conference chair.")
                                        .type("ROLE_REMOVED")
                                        .link("/conference/" + cut.getConference().getId())
                                        .isRead(false)
                                        .build();
                        notificationRepository.save(notification);
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
}
