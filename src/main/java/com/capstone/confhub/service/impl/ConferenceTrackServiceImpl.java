package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ConferenceTrackDTO;
import com.capstone.confhub.dto.response.ConferenceTrackResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.dto.TrackReviewSettingDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.ConferenceTrackService;
import com.capstone.confhub.utils.PaginationUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConferenceTrackServiceImpl implements ConferenceTrackService {

    private final ConferenceTrackRepository trackRepository;
    private final ConferenceRepository conferenceRepository;
    private final PaperRepository paperRepository;

    @Override
    @Transactional
    public ConferenceTrackResponseDTO createTrack(ConferenceTrackDTO dto) {
        Conference conference = conferenceRepository.findById(dto.getConferenceId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Conference not found with ID: " + dto.getConferenceId()));

        // Check for duplicate track name within the same conference
        boolean exists = trackRepository.findByConferenceId(dto.getConferenceId()).stream()
                .anyMatch(t -> t.getName().trim().equalsIgnoreCase(dto.getName().trim()));
        if (exists) {
            throw new BadRequestException("A track named '" + dto.getName() + "' already exists in this conference.");
        }

        ConferenceTrack track = new ConferenceTrack();
        track.setConference(conference);
        
        // Initialize default review setting for every new track
        TrackReviewSetting defaultSetting = new TrackReviewSetting();
        defaultSetting.setTrack(track);
        defaultSetting.setIsDoubleBlind(false);
        defaultSetting.setAllowReviewerQuota(false);
        defaultSetting.setAllowOthersReviewAccessAfterSubmit(false);
        defaultSetting.setAllowReviewUpdateDuringDiscussion(false);
        track.setTrackReviewSetting(defaultSetting);

        mapRequestDtoToEntity(dto, track);

        ConferenceTrack savedTrack = trackRepository.save(track);

        return mapEntityToResponse(savedTrack);
    }

    @Override
    @Transactional
    public ConferenceTrackResponseDTO updateTrack(Integer id, ConferenceTrackDTO dto) {
        ConferenceTrack track = trackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Track not found with ID: " + id));

        if (dto.getConferenceId() != null && !dto.getConferenceId().equals(track.getConference().getId())) {
            Conference newConference = conferenceRepository.findById(dto.getConferenceId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Conference not found with ID: " + dto.getConferenceId()));
            track.setConference(newConference);
        }

        mapRequestDtoToEntity(dto, track);

        ConferenceTrack updatedTrack = trackRepository.save(track);
        return mapEntityToResponse(updatedTrack);
    }

    @Override
    public ConferenceTrackResponseDTO getTrackById(Integer id) {
        ConferenceTrack track = trackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Track not found with ID: " + id));
        return mapEntityToResponse(track);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ConferenceTrackResponseDTO> getAllTracks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ConferenceTrack> tracks = trackRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(tracks, this::mapEntityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ConferenceTrackResponseDTO> getTracksByConferenceId(Integer conferenceId, int page, int size) {
        if (!conferenceRepository.existsById(conferenceId)) {
            throw new EntityNotFoundException("Track not found with Conference ID: " + conferenceId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ConferenceTrack> tracks = trackRepository.findByConferenceId(conferenceId, pageable);
        return PaginationUtils.toPagedResponse(tracks, this::mapEntityToResponse);
    }

    @Override
    @Transactional
    public void deleteTrack(Integer id) {
        if (!trackRepository.existsById(id)) {
            throw new EntityNotFoundException("Track not found with ID: " + id);
        }

        // Safety check: block deletion if papers exist in this track
        long paperCount = paperRepository.countByTrack_Id(id);
        if (paperCount > 0) {
            throw new BadRequestException(
                    "Cannot delete track: it has " + paperCount + " paper(s). Remove or reassign papers first.");
        }

        trackRepository.deleteById(id);
    }

    private ConferenceTrackResponseDTO mapEntityToResponse(ConferenceTrack entity) {
        ConferenceTrackResponseDTO response = new ConferenceTrackResponseDTO();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());

        // Map the full Conference object
        response.setConference(entity.getConference());

        if (entity.getTrackReviewSetting() != null) {
            TrackReviewSettingDTO settingDTO = new TrackReviewSettingDTO();
            TrackReviewSetting entitySetting = entity.getTrackReviewSetting();
            settingDTO.setIsDoubleBlind(entitySetting.getIsDoubleBlind());
            settingDTO.setReviewerInstructions(entitySetting.getReviewerInstructions());
            settingDTO.setAllowReviewerQuota(entitySetting.getAllowReviewerQuota());
            settingDTO.setReviewerInviteExpirationDays(entitySetting.getReviewerInviteExpirationDays());
            settingDTO.setAllowOthersReviewAccessAfterSubmit(entitySetting.getAllowOthersReviewAccessAfterSubmit());
            settingDTO.setAllowReviewUpdateDuringDiscussion(entitySetting.getAllowReviewUpdateDuringDiscussion());
            response.setTrackReviewSetting(settingDTO);
        }

        return response;
    }

    /**
     * Maps the Input DTO (Request) to the Entity
     */
    private void mapRequestDtoToEntity(ConferenceTrackDTO dto, ConferenceTrack entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());

        if (dto.getTrackReviewSetting() != null) {
            TrackReviewSetting setting = entity.getTrackReviewSetting();
            if (setting == null) {
                setting = new TrackReviewSetting();
                setting.setTrack(entity);
            }
            TrackReviewSettingDTO dtoSetting = dto.getTrackReviewSetting();
            if (dtoSetting.getIsDoubleBlind() != null) setting.setIsDoubleBlind(dtoSetting.getIsDoubleBlind());
            if (dtoSetting.getReviewerInstructions() != null) setting.setReviewerInstructions(dtoSetting.getReviewerInstructions());
            if (dtoSetting.getAllowReviewerQuota() != null) setting.setAllowReviewerQuota(dtoSetting.getAllowReviewerQuota());
            if (dtoSetting.getReviewerInviteExpirationDays() != null) setting.setReviewerInviteExpirationDays(dtoSetting.getReviewerInviteExpirationDays());
            if (dtoSetting.getAllowOthersReviewAccessAfterSubmit() != null) setting.setAllowOthersReviewAccessAfterSubmit(dtoSetting.getAllowOthersReviewAccessAfterSubmit());
            if (dtoSetting.getAllowReviewUpdateDuringDiscussion() != null) setting.setAllowReviewUpdateDuringDiscussion(dtoSetting.getAllowReviewUpdateDuringDiscussion());
            
            entity.setTrackReviewSetting(setting);
        }
    }
}