package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.TicketTypeRequest;
import com.capstone.confhub.dto.response.TicketTypeResponse;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.TicketType;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.TicketTypeRepository;
import com.capstone.confhub.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketTypeServiceImpl implements TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final ConferenceRepository conferenceRepository;

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
