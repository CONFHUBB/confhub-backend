package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
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
        public PagedResponse<UserResponseDTO> getTrackChairsByConferenceId(Integer conferenceId, int page, int size) {
                conferenceRepository.findById(conferenceId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Conference not found with id " + conferenceId));

                List<ConferenceUserTrack> cuts = conferenceUserTrackRepository
                                .findByConference_IdAndAssignedRole(conferenceId, ConferenceTrackRole.TRACK_CHAIR);

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
                                .findByUser_IdAndAssignedRole(userId, ConferenceTrackRole.TRACK_CHAIR);

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
                                .findByUser_IdAndAssignedRole(userId, ConferenceTrackRole.ORGANIZER);

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

        @Override
        @Transactional
        public ConferenceUserTrackResponseDTO acceptInvitation(Integer userId, Integer conferenceId) {
                ConferenceUserTrack cut = conferenceUserTrackRepository
                                .findByUser_IdAndConference_Id(userId, conferenceId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "ConferenceUserTrack not found for userId=" + userId + " conferenceId="
                                                                + conferenceId));
                cut.setIsAccepted(true);
                return mapToResponseDTO(conferenceUserTrackRepository.save(cut));
        }

        @Override
        @Transactional
        public ConferenceUserTrackResponseDTO declineInvitation(Integer userId, Integer conferenceId) {
                ConferenceUserTrack cut = conferenceUserTrackRepository
                                .findByUser_IdAndConference_Id(userId, conferenceId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "ConferenceUserTrack not found for userId=" + userId + " conferenceId="
                                                                + conferenceId));
                cut.setIsAccepted(false);
                return mapToResponseDTO(conferenceUserTrackRepository.save(cut));
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
}
