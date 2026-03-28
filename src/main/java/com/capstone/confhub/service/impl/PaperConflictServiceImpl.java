package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.PaperConflictDTO;
import com.capstone.confhub.dto.response.PaperConflictResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperConflict;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.PaperConflictRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.service.PaperConflictService;
import com.capstone.confhub.utils.PaginationUtils;
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
public class PaperConflictServiceImpl implements PaperConflictService {

    private final PaperConflictRepository paperConflictRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaperConflictResponseDTO> getAllPaperConflicts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaperConflict> paperConflicts = paperConflictRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(paperConflicts, this::mapToPaperConflictResponseDTO);
    }

    @Override
    @Transactional
    public PaperConflictResponseDTO createPaperConflict(PaperConflictDTO dto) {
        PaperConflict entity = new PaperConflict();
        mapDtoToPaperConflictEntity(dto, entity);
        return mapToPaperConflictResponseDTO(paperConflictRepository.save(entity));
    }

    @Override
    @Transactional
    public PaperConflictResponseDTO updatePaperConflict(Integer id, PaperConflictDTO dto) {
        PaperConflict entity = paperConflictRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaperConflict not found with id " + id));
        mapDtoToPaperConflictEntity(dto, entity);
        return mapToPaperConflictResponseDTO(paperConflictRepository.save(entity));
    }

    @Override
    public PaperConflictResponseDTO getPaperConflictById(Integer id) {
        return paperConflictRepository.findById(id)
                .map(this::mapToPaperConflictResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("PaperConflict not found with id " + id));
    }

    @Override
    @Transactional
    public void deletePaperConflict(Integer id) {
        if (!paperConflictRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. PaperConflict not found with id " + id);
        }
        paperConflictRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<PaperConflictResponseDTO> getConflictsByPaperId(Integer paperId) {
        return paperConflictRepository.findByPaper_Id(paperId).stream()
                .map(this::mapToPaperConflictResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<PaperConflictResponseDTO> getConflictsByConferenceId(Integer conferenceId) {
        java.util.List<Paper> papers = paperRepository.findByTrack_Conference_Id(conferenceId);
        java.util.List<Integer> paperIds = papers.stream().map(Paper::getId).toList();
        if (paperIds.isEmpty()) return java.util.List.of();
        return paperConflictRepository.findAll().stream()
                .filter(pc -> paperIds.contains(pc.getPaper().getId()))
                .map(this::mapToPaperConflictResponseDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    private void mapDtoToPaperConflictEntity(PaperConflictDTO dto, PaperConflict entity) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getUserId()));

        entity.setPaper(paper);
        entity.setUser(user);
        entity.setConflictType(dto.getConflictType());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperConflictResponseDTO mapToPaperConflictResponseDTO(PaperConflict entity) {
        Paper paper = entity.getPaper();
        User user = entity.getUser();
        return PaperConflictResponseDTO.builder()
                .id(entity.getId())
                .paper(PaperConflictResponseDTO.PaperInfo.builder()
                        .id(paper.getId())
                        .title(paper.getTitle())
                        .build())
                .user(PaperConflictResponseDTO.UserInfo.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .build())
                .conflictType(entity.getConflictType())
                .build();
    }
}