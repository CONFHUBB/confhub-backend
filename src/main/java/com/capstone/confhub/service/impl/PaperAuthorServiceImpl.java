package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.PaperAuthorDTO;
import com.capstone.confhub.dto.response.PaperAuthorResponseDTO;
import com.capstone.confhub.dto.response.UserResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperAuthor;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import com.capstone.confhub.security.services.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.capstone.confhub.service.PaperAuthorService;
import com.capstone.confhub.utils.PaginationUtils;
import java.util.List;
import java.util.Collections;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class PaperAuthorServiceImpl implements PaperAuthorService {

    private final PaperAuthorRepository paperAuthorRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final TrackReviewSettingRepository trackReviewSettingRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaperAuthorResponseDTO> getAllPaperAuthors(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaperAuthor> paperAuthors = paperAuthorRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(paperAuthors, this::mapToPaperAuthorResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaperAuthorResponseDTO> getAuthorsByPaper(Integer paperId, int page, int size) {
        if (shouldMaskAuthors(paperId)) {
            return PagedResponse.<PaperAuthorResponseDTO>builder()
                    .content(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaperAuthor> paperAuthors = paperAuthorRepository.findByPaperId(paperId, pageable);
        return PaginationUtils.toPagedResponse(paperAuthors, this::mapToPaperAuthorResponseDTO);
    }

    @Override
    @Transactional
    public PaperAuthorResponseDTO createPaperAuthor(PaperAuthorDTO dto) {
        // Duplicate check
        if (paperAuthorRepository.existsByPaperIdAndUserId(dto.getPaperId(), dto.getUserId())) {
            throw new BadRequestException("This author is already added to the paper.");
        }
        PaperAuthor entity = new PaperAuthor();
        mapDtoToPaperAuthorEntity(dto, entity);
        return mapToPaperAuthorResponseDTO(paperAuthorRepository.save(entity));
    }

    @Override
    @Transactional
    public PaperAuthorResponseDTO updatePaperAuthor(Integer id, PaperAuthorDTO dto) {
        PaperAuthor entity = paperAuthorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaperAuthor not found with id " + id));
        mapDtoToPaperAuthorEntity(dto, entity);
        return mapToPaperAuthorResponseDTO(paperAuthorRepository.save(entity));
    }

    @Override
    public PaperAuthorResponseDTO getPaperAuthorById(Integer id) {
        return paperAuthorRepository.findById(id)
                .map(this::mapToPaperAuthorResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("PaperAuthor not found with id " + id));
    }

    @Override
    @Transactional
    public void deletePaperAuthor(Integer id) {
        PaperAuthor pa = paperAuthorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot delete. PaperAuthor not found with id " + id));

        User removedUser = pa.getUser();
        Paper paper = pa.getPaper();

        paperAuthorRepository.deleteById(id);

        // Notify the removed co-author
        try {
            Notification notification = Notification.builder()
                    .user(removedUser)
                    .conference(paper.getTrack().getConference())
                    .title("You have been removed as co-author")
                    .message("You have been removed as a co-author from the paper \"" + paper.getTitle() + "\".")
                    .type("AUTHOR_REMOVED")
                    .link("/paper")
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to send notification to removed co-author {}: {}", removedUser.getEmail(), e.getMessage());
        }
    }

    private void mapDtoToPaperAuthorEntity(PaperAuthorDTO dto, PaperAuthor entity) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getUserId()));

        entity.setPaper(paper);
        entity.setUser(user);

        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperAuthorResponseDTO mapToPaperAuthorResponseDTO(PaperAuthor entity) {
        User user = entity.getUser();
        return PaperAuthorResponseDTO.builder()
                .id(entity.getId())
                .paperId(entity.getPaper().getId())
                .user(UserResponseDTO.builder()
                        .id(user.getId())
                        .title(user.getTitle())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .country(user.getCountry())
                        .isActive(user.getIsActive())
                        .build())
                .build();
    }

    private boolean shouldMaskAuthors(Integer paperId) {
        Paper paper = paperRepository.findById(paperId).orElse(null);
        if (paper == null || paper.getTrack() == null) return false;

        Integer trackId = paper.getTrack().getId();
        TrackReviewSetting setting = trackReviewSettingRepository.findByTrackId(trackId).orElse(null);
        boolean isDoubleBlind = setting != null && Boolean.TRUE.equals(setting.getIsDoubleBlind());

        if (!isDoubleBlind) return false;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"))) {
            return false;
        }

        if (!(auth.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return true;
        }

        Integer userId = userDetails.getId();
        Integer conferenceId = paper.getTrack().getConference().getId();

        List<ConferenceUserTrack> roles = conferenceUserTrackRepository.findAllByUser_IdAndConference_Id(userId, conferenceId);
        boolean isChair = roles.stream().anyMatch(r ->
                r.getAssignedRole() == ConferenceTrackRole.CONFERENCE_CHAIR ||
                r.getAssignedRole() == ConferenceTrackRole.PROGRAM_CHAIR);

        if (isChair) return false;

        boolean isAuthor = paperAuthorRepository.existsByPaperIdAndUserId(paperId, userId);
        if (isAuthor) return false;

        return true;
    }
}