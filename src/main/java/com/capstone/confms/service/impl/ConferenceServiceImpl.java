package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceDTO;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.entity.User;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.NotificationRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.security.services.UserDetailsImpl;
import com.capstone.confms.service.ConferenceService;
import com.capstone.confms.service.ConferenceActivityService;
import com.capstone.confms.utils.PaginationUtils;
import com.capstone.confms.utils.enums.ConferenceStatus;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import com.capstone.confms.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    @Transactional
    public ConferenceResponseDTO createConference(ConferenceDTO dto) {
        log.info("Creating conference: {}", dto.getName());
        Conference conference = new Conference();
        mapDtoToEntity(dto, conference);
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
                .message("Your conference \"" + savedConference.getName() + "\" has been created and is pending approval.")
                .type("CONFERENCE_CREATED")
                .link("/conference/" + savedConference.getId() + "/update")
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
        log.warn("Deleting conference ID: {}", id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. Conference not found with id " + id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public ConferenceResponseDTO openSubmissions(Integer id) {
        log.info("Opening submissions for conference ID: {}", id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        // BR-1.3: Chỉ SCHEDULED mới chuyển sang ONGOING
        if (conference.getStatus() != ConferenceStatus.SCHEDULED) {
            throw new BadRequestException(
                    "Can only open submissions for SCHEDULED conferences. Current status: " + conference.getStatus());
        }

        conference.setStatus(ConferenceStatus.ONGOING);
        Conference saved = repository.save(conference);
        notifyAllMembers(saved, "Conference is now live",
                "\"" + saved.getName() + "\" is now open for submissions.", "CONFERENCE_STATUS");
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public ConferenceResponseDTO approveConference(Integer id) {
        log.info("Approving conference ID: {}", id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        // BR-1.3: Chỉ PENDING mới approve
        if (conference.getStatus() != ConferenceStatus.PENDING) {
            throw new BadRequestException("Only conferences with PENDING status can be approved.");
        }
        conference.setStatus(ConferenceStatus.SCHEDULED);
        Conference saved = repository.save(conference);
        notifyAllMembers(saved, "Conference has been scheduled",
                "\"" + saved.getName() + "\" has been approved and scheduled.", "CONFERENCE_STATUS");
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional
    public ConferenceResponseDTO completeConference(Integer id) {
        log.info("Completing conference ID: {}", id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        // BR-1.3: Chỉ ONGOING mới complete
        if (conference.getStatus() != ConferenceStatus.ONGOING) {
            throw new BadRequestException(
                    "Can only complete ONGOING conferences. Current status: " + conference.getStatus());
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

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new BadRequestException("No authenticated user found");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    private void mapDtoToEntity(ConferenceDTO dto, Conference entity) {
        entity.setName(dto.getName());
        entity.setAcronym(dto.getAcronym());
        entity.setDescription(dto.getDescription());
        entity.setLocation(dto.getLocation());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setStatus(ConferenceStatus.PENDING);
        entity.setCreatedAt(LocalDateTime.now());
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
}