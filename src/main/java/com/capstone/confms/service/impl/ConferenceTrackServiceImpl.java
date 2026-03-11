package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceTrackDTO;
import com.capstone.confms.dto.response.ConferenceTrackResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.dto.TrackReviewSettingDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.TrackReviewSetting;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.service.ConferenceTrackService;
import com.capstone.confms.utils.PaginationUtils;
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

    @Override
    @Transactional
    public ConferenceTrackResponseDTO createTrack(ConferenceTrackDTO dto) {
        Conference conference = conferenceRepository.findById(dto.getConferenceId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Conference not found with ID: " + dto.getConferenceId()));

        ConferenceTrack track = new ConferenceTrack();
        track.setConference(conference);
        
        // Initialize default review setting for every new track
        TrackReviewSetting defaultSetting = new TrackReviewSetting();
        defaultSetting.setTrack(track);
        defaultSetting.setIsDoubleBlind(false);
        defaultSetting.setRequireSubjectAreas(false);
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
        trackRepository.deleteById(id);
    }

    private ConferenceTrackResponseDTO mapEntityToResponse(ConferenceTrack entity) {
        ConferenceTrackResponseDTO response = new ConferenceTrackResponseDTO();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setMaxSubmissions(entity.getMaxSubmissions());

        // Map the full Conference object
        response.setConference(entity.getConference());

        if (entity.getTrackReviewSetting() != null) {
            TrackReviewSettingDTO settingDTO = new TrackReviewSettingDTO();
            TrackReviewSetting entitySetting = entity.getTrackReviewSetting();
            settingDTO.setIsDoubleBlind(entitySetting.getIsDoubleBlind());
            settingDTO.setReviewerInstructions(entitySetting.getReviewerInstructions());
            settingDTO.setRequireSubjectAreas(entitySetting.getRequireSubjectAreas());
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
        entity.setMaxSubmissions(dto.getMaxSubmissions());

        if (dto.getTrackReviewSetting() != null) {
            TrackReviewSetting setting = entity.getTrackReviewSetting();
            if (setting == null) {
                setting = new TrackReviewSetting();
                setting.setTrack(entity);
            }
            TrackReviewSettingDTO dtoSetting = dto.getTrackReviewSetting();
            if (dtoSetting.getIsDoubleBlind() != null) setting.setIsDoubleBlind(dtoSetting.getIsDoubleBlind());
            if (dtoSetting.getReviewerInstructions() != null) setting.setReviewerInstructions(dtoSetting.getReviewerInstructions());
            if (dtoSetting.getRequireSubjectAreas() != null) setting.setRequireSubjectAreas(dtoSetting.getRequireSubjectAreas());
            if (dtoSetting.getAllowReviewerQuota() != null) setting.setAllowReviewerQuota(dtoSetting.getAllowReviewerQuota());
            if (dtoSetting.getReviewerInviteExpirationDays() != null) setting.setReviewerInviteExpirationDays(dtoSetting.getReviewerInviteExpirationDays());
            if (dtoSetting.getAllowOthersReviewAccessAfterSubmit() != null) setting.setAllowOthersReviewAccessAfterSubmit(dtoSetting.getAllowOthersReviewAccessAfterSubmit());
            if (dtoSetting.getAllowReviewUpdateDuringDiscussion() != null) setting.setAllowReviewUpdateDuringDiscussion(dtoSetting.getAllowReviewUpdateDuringDiscussion());
            
            entity.setTrackReviewSetting(setting);
        }
    }
}