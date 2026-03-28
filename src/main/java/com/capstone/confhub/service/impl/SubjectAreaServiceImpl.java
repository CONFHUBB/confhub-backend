package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.SubjectAreaDTO;
import com.capstone.confhub.dto.response.SubjectAreaResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.SubjectArea;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.SubjectAreaRepository;
import com.capstone.confhub.service.SubjectAreaService;
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
public class SubjectAreaServiceImpl implements SubjectAreaService {

    private final SubjectAreaRepository subjectAreaRepository;
    private final ConferenceTrackRepository trackRepository;

    @Override
    @Transactional
    public SubjectAreaResponseDTO createSubjectArea(SubjectAreaDTO dto) {
        ConferenceTrack track = trackRepository.findById(dto.getTrackId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Conference Track not found with ID: " + dto.getTrackId()));

        SubjectArea parent = null;
        if (dto.getParentId() != null) {
            parent = subjectAreaRepository.findById(dto.getParentId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Parent Subject Area not found with ID: " + dto.getParentId()));
        }

        SubjectArea entity = new SubjectArea();
        entity.setTrack(track);
        entity.setParent(parent);
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());

        SubjectArea saved = subjectAreaRepository.save(entity);
        return mapEntityToResponse(saved);
    }

    @Override
    @Transactional
    public SubjectAreaResponseDTO updateSubjectArea(Integer id, SubjectAreaDTO dto) {
        SubjectArea entity = subjectAreaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subject Area not found with ID: " + id));

        if (dto.getTrackId() != null && !dto.getTrackId().equals(entity.getTrack().getId())) {
            ConferenceTrack newTrack = trackRepository.findById(dto.getTrackId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Conference Track not found with ID: " + dto.getTrackId()));
            entity.setTrack(newTrack);
        }

        if (dto.getParentId() != null) {
            SubjectArea parent = subjectAreaRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent Subject Area not found with ID: " + dto.getParentId()));
            entity.setParent(parent);
        } else {
            entity.setParent(null);
        }

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        SubjectArea updated = subjectAreaRepository.save(entity);
        return mapEntityToResponse(updated);
    }

    @Override
    public SubjectAreaResponseDTO getSubjectAreaById(Integer id) {
        SubjectArea entity = subjectAreaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subject Area not found with ID: " + id));
        return mapEntityToResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SubjectAreaResponseDTO> getAllSubjectAreas(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SubjectArea> areas = subjectAreaRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(areas, this::mapEntityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SubjectAreaResponseDTO> getSubjectAreasByTrackId(Integer trackId, int page, int size) {
        if (!trackRepository.existsById(trackId)) {
            throw new EntityNotFoundException("Track not found with ID: " + trackId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SubjectArea> areas = subjectAreaRepository.findByTrackId(trackId, pageable);
        return PaginationUtils.toPagedResponse(areas, this::mapEntityToResponse);
    }

    @Override
    @Transactional
    public void deleteSubjectArea(Integer id) {
        if (!subjectAreaRepository.existsById(id)) {
            throw new EntityNotFoundException("Subject Area not found with ID: " + id);
        }
        subjectAreaRepository.deleteById(id);
    }

    private SubjectAreaResponseDTO mapEntityToResponse(SubjectArea entity) {
        return SubjectAreaResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .trackId(entity.getTrack() != null ? entity.getTrack().getId() : null)
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .build();
    }
}
