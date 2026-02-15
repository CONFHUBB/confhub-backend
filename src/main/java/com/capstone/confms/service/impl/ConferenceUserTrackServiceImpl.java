package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.entity.User;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.service.ConferenceUserTrackService;
import java.time.LocalDateTime;
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

    @Override
    @Transactional
    public ConferenceUserTrackResponseDTO assignRoleToUserTrack(AssignConferenceUserTrackRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Conference conference = conferenceRepository.findById(request.getConferenceId())
                .orElseThrow(() -> new RuntimeException("Conference not found"));
        ConferenceTrack track = conferenceTrackRepository.findById(request.getTrackId())
                .orElseThrow(() -> new RuntimeException("Track not found"));
        ConferenceUserTrack entity = new ConferenceUserTrack();
        entity.setUser(user);
        entity.setConference(conference);
        entity.setConferenceTrack(track);
        entity.setAssignedRole(request.getAssignedRole());
        entity.setInvitedAt(LocalDateTime.now());
        ConferenceUserTrack saved = conferenceUserTrackRepository.save(entity);
        return mapToResponseDTO(saved);
    }

    private ConferenceUserTrackResponseDTO mapToResponseDTO(ConferenceUserTrack entity) {
        ConferenceUserTrackResponseDTO dto = new ConferenceUserTrackResponseDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser().getId());
        dto.setConferenceId(entity.getConference().getId());
        dto.setConferenceTrackId(entity.getConferenceTrack() != null ? entity.getConferenceTrack().getId() : null);
        dto.setAssignedRole(entity.getAssignedRole());
        dto.setInvitedAt(entity.getInvitedAt());
        dto.setIsAccepted(entity.getIsAccepted());
        dto.setIsRegistered(entity.getIsRegistered());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
