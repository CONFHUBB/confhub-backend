package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.TicketTypeRequest;
import com.capstone.confhub.dto.response.TicketTypeResponse;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.PaperAuthor;
import com.capstone.confhub.entity.TicketType;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.repository.TicketTypeRepository;
import com.capstone.confhub.service.TicketTypeService;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import com.capstone.confhub.utils.enums.PaperStatus;
import com.capstone.confhub.utils.enums.TicketCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketTypeServiceImpl implements TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final ConferenceRepository conferenceRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;

    @Override
    @Transactional
    public TicketTypeResponse create(Integer conferenceId, TicketTypeRequest request) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + conferenceId));

        TicketType ticketType = new TicketType();
        ticketType.setConference(conference);
        mapRequestToEntity(request, ticketType);

        TicketType saved = ticketTypeRepository.save(ticketType);
        log.info("Created TicketType [{}] for conference {}", saved.getName(), conferenceId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TicketTypeResponse update(Integer id, TicketTypeRequest request) {
        TicketType ticketType = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TicketType not found with id " + id));
        mapRequestToEntity(request, ticketType);
        return mapToResponse(ticketTypeRepository.save(ticketType));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (!ticketTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("TicketType not found with id " + id);
        }
        ticketTypeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getByConference(Integer conferenceId, boolean activeOnly) {
        List<TicketType> types = activeOnly
                ? ticketTypeRepository.findByConferenceIdAndIsActiveTrue(conferenceId)
                : ticketTypeRepository.findByConferenceId(conferenceId);
        return types.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getForUser(Integer conferenceId, Integer userId) {
        // Get all active ticket types
        List<TicketType> allTypes = ticketTypeRepository.findByConferenceIdAndIsActiveTrue(conferenceId);

        // Determine eligible categories for this user
        Set<TicketCategory> eligible = EnumSet.of(TicketCategory.STANDARD, TicketCategory.VIP, TicketCategory.STUDENT);

        // Check if user is an author with accepted/published paper in this conference
        boolean isAuthor = false;
        List<PaperAuthor> authorships = paperAuthorRepository.findByUserId(userId);
        for (PaperAuthor pa : authorships) {
            var paper = pa.getPaper();
            if (paper.getTrack() != null
                    && paper.getTrack().getConference() != null
                    && paper.getTrack().getConference().getId().equals(conferenceId)
                    && (paper.getStatus() == PaperStatus.ACCEPTED || paper.getStatus() == PaperStatus.AWAITING_REGISTRATION || paper.getStatus() == PaperStatus.REGISTERED || paper.getStatus() == PaperStatus.AWAITING_CAMERA_READY || paper.getStatus() == PaperStatus.CAMERA_READY_SUBMITTED || paper.getStatus() == PaperStatus.PUBLISHED)) {
                isAuthor = true;
                break;
            }
        }
        if (isAuthor) {
            eligible.add(TicketCategory.AUTHOR);
        }

        // Check if user is staff (REVIEWER, PROGRAM_CHAIR, or CONFERENCE_CHAIR)
        boolean isStaff = conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
                userId, conferenceId, ConferenceTrackRole.REVIEWER)
                || conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
                userId, conferenceId, ConferenceTrackRole.PROGRAM_CHAIR)
                || conferenceUserTrackRepository.existsByUser_IdAndConference_IdAndAssignedRole(
                userId, conferenceId, ConferenceTrackRole.CONFERENCE_CHAIR);
        if (isStaff) {
            eligible.add(TicketCategory.STAFF);
        }

        // Filter ticket types by eligible categories
        return allTypes.stream()
                .filter(t -> eligible.contains(t.getCategory()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== Helpers ==========

    private void mapRequestToEntity(TicketTypeRequest request, TicketType entity) {
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setPrice(request.getPrice());
        entity.setCurrency(request.getCurrency() != null ? request.getCurrency() : "VND");
        entity.setDeadline(request.getDeadline());
        entity.setMaxQuantity(request.getMaxQuantity());
        entity.setCategory(request.getCategory());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
    }

    public TicketTypeResponse mapToResponse(TicketType t) {
        boolean isDeadlinePassed = t.getDeadline() != null && LocalDateTime.now().isAfter(t.getDeadline());
        Integer available = t.getMaxQuantity() != null ? t.getMaxQuantity() - t.getQuantitySold() : null;
        boolean isSoldOut = available != null && available <= 0;

        return TicketTypeResponse.builder()
                .id(t.getId())
                .conferenceId(t.getConference().getId())
                .name(t.getName())
                .description(t.getDescription())
                .price(t.getPrice())
                .currency(t.getCurrency())
                .deadline(t.getDeadline())
                .maxQuantity(t.getMaxQuantity())
                .quantitySold(t.getQuantitySold())
                .availableSlots(available)
                .category(t.getCategory())
                .isActive(t.getIsActive())
                .isSoldOut(isSoldOut)
                .isDeadlinePassed(isDeadlinePassed)
                .build();
    }
}
