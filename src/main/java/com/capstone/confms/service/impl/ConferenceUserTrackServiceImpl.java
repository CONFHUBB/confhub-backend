package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confms.dto.response.UserResponseDTO;
import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.User;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.ConferenceUserTrackService;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getTrackChairsByConferenceId(Integer conferenceId) {
        conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + conferenceId));

        List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                .findByConference_IdAndAssignedRole(conferenceId, ConferenceTrackRole.TRACK_CHAIR);

        List<User> distinctUsers = cuts.stream()
                .map(ConferenceUserTrack::getUser)
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .toList();

        return distinctUsers.stream()
                .map(this::mapUserToResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConferenceResponseDTO> getChairedConferencesByUserId(Integer userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                .findByUser_IdAndAssignedRole(userId, ConferenceTrackRole.TRACK_CHAIR);

        List<Conference> distinctConferences = cuts.stream()
                .map(ConferenceUserTrack::getConference)
                .collect(Collectors.toMap(Conference::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .toList();

        return distinctConferences.stream()
                .map(this::mapConferenceToResponseDTO)
                .toList();
    }

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

    private UserResponseDTO mapUserToResponseDTO(User entity) {
        return UserResponseDTO.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
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
}
