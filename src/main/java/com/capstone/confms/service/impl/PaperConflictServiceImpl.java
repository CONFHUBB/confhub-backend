package com.capstone.confms.service.impl;

import com.capstone.confms.dto.PaperConflictDTO;
import com.capstone.confms.dto.response.PaperConflictResponseDTO;
import com.capstone.confms.entity.PaperConflict;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.PaperConflictRepository;
import com.capstone.confms.service.PaperConflictService;
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
public class PaperConflictServiceImpl implements PaperConflictService {

    private final PaperConflictRepository paperConflictRepository;

    @Override
    public List<PaperConflictResponseDTO> getAllPaperConflicts() {
        return paperConflictRepository.findAll().stream()
                .map(this::mapToPaperConflictResponseDTO)
                .collect(Collectors.toList());
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

    private void mapDtoToPaperConflictEntity(PaperConflictDTO dto, PaperConflict entity) {
        entity.setPaper(dto.getPaper());
        entity.setUser(dto.getUser());
        entity.setConflictType(dto.getConflictType());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperConflictResponseDTO mapToPaperConflictResponseDTO(PaperConflict entity) {
        return PaperConflictResponseDTO.builder()
                .id(entity.getId())
                .paper(entity.getPaper())
                .user(entity.getUser())
                .conflictType(entity.getConflictType())
                .build();
    }
}