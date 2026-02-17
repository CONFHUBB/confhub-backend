package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceTrackDTO;
import com.capstone.confms.dto.response.ConferenceTrackResponseDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.service.ConferenceTrackService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConferenceTrackServiceImpl implements ConferenceTrackService {

    private final ConferenceTrackRepository trackRepository;
    private final ConferenceRepository conferenceRepository;

    @Override
    @Transactional
    public ConferenceTrackResponseDTO createTrack(ConferenceTrackDTO dto) {
        validateDates(dto);

        Conference conference = conferenceRepository.findById(dto.getConferenceId())
                .orElseThrow(() -> new EntityNotFoundException("Conference not found with ID: " + dto.getConferenceId()));

        ConferenceTrack track = new ConferenceTrack();
        mapRequestDtoToEntity(dto, track);
        track.setConference(conference);

        ConferenceTrack savedTrack = trackRepository.save(track);

        return mapEntityToResponse(savedTrack);
    }

    @Override
    @Transactional
    public ConferenceTrackResponseDTO updateTrack(Integer id, ConferenceTrackDTO dto) {
        validateDates(dto);

        ConferenceTrack track = trackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Track not found with ID: " + id));

        if (dto.getConferenceId() != null && !dto.getConferenceId().equals(track.getConference().getId())) {
            Conference newConference = conferenceRepository.findById(dto.getConferenceId())
                    .orElseThrow(() -> new EntityNotFoundException("Conference not found with ID: " + dto.getConferenceId()));
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
    public List<ConferenceTrackResponseDTO> getAllTracks() {
        return trackRepository.findAll().stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConferenceTrackResponseDTO> getTracksByConferenceId(Integer conferenceId){
        if(!conferenceRepository.existsById(conferenceId)) {
            throw new EntityNotFoundException("Track not found with Conference ID: " + conferenceId);
        }

        return trackRepository.findByConferenceId(conferenceId).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTrack(Integer id) {
        if (!trackRepository.existsById(id)) {
            throw new EntityNotFoundException("Track not found with ID: " + id);
        }
        trackRepository.deleteById(id);
    }


    private void validateDates(ConferenceTrackDTO dto) {
        if (dto.getSubmissionStart() != null && dto.getSubmissionEnd() != null 
            && dto.getSubmissionStart().isAfter(dto.getSubmissionEnd())) {
            throw new IllegalArgumentException("Submission Start date cannot be after End date");
        }
    }

    private ConferenceTrackResponseDTO mapEntityToResponse(ConferenceTrack entity) {
        ConferenceTrackResponseDTO response = new ConferenceTrackResponseDTO();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setMaxSubmissions(entity.getMaxSubmissions());

        // Map the full Conference object
        response.setConference(entity.getConference());

        // Dates
        response.setSubmissionStart(entity.getSubmissionStart());
        response.setSubmissionEnd(entity.getSubmissionEnd());
        response.setRegistrationStart(entity.getRegistrationStart());
        response.setRegistrationEnd(entity.getRegistrationEnd());
        response.setCameraReadyStart(entity.getCameraReadyStart());
        response.setCameraReadyEnd(entity.getCameraReadyEnd());
        response.setBiddingStart(entity.getBiddingStart());
        response.setBiddingEnd(entity.getBiddingEnd());
        response.setReviewStart(entity.getReviewStart());
        response.setReviewEnd(entity.getReviewEnd());

        return response;
    }

    /**
     * Maps the Input DTO (Request) to the Entity
     */
    private void mapRequestDtoToEntity(ConferenceTrackDTO dto, ConferenceTrack entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setMaxSubmissions(dto.getMaxSubmissions());

        entity.setSubmissionStart(dto.getSubmissionStart());
        entity.setSubmissionEnd(dto.getSubmissionEnd());
        entity.setRegistrationStart(dto.getRegistrationStart());
        entity.setRegistrationEnd(dto.getRegistrationEnd());
        entity.setCameraReadyStart(dto.getCameraReadyStart());
        entity.setCameraReadyEnd(dto.getCameraReadyEnd());
        entity.setBiddingStart(dto.getBiddingStart());
        entity.setBiddingEnd(dto.getBiddingEnd());
        entity.setReviewStart(dto.getReviewStart());
        entity.setReviewEnd(dto.getReviewEnd());
    }
}