package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.PaperFileService;
import jakarta.persistence.EntityNotFoundException;
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
public class PaperFileServiceImpl implements PaperFileService {

    private final PaperFileRepository paperFileRepository;
    private final PaperRepository paperRepository;

    @Override
    public List<PaperFileResponseDTO> getAllPaperFiles() {
        return paperFileRepository.findAll().stream()
                .map(this::mapToPaperFileResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaperFileResponseDTO createPaperFile(PaperFileDTO dto) {
        PaperFile entity = new PaperFile();
        mapDtoToPaperFileEntity(dto, entity);
        return mapToPaperFileResponseDTO(paperFileRepository.save(entity));
    }

    @Override
    @Transactional
    public PaperFileResponseDTO updatePaperFile(Integer id, PaperFileDTO dto) {
        PaperFile entity = paperFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaperFile not found with id " + id));
        mapDtoToPaperFileEntity(dto, entity);
        return mapToPaperFileResponseDTO(paperFileRepository.save(entity));
    }

    @Override
    public PaperFileResponseDTO getPaperFileById(Integer id) {
        return paperFileRepository.findById(id)
                .map(this::mapToPaperFileResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("PaperFile not found with id " + id));
    }

    @Override
    @Transactional
    public void deletePaperFile(Integer id) {
        if (!paperFileRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. PaperFile not found with id " + id);
        }
        paperFileRepository.deleteById(id);
    }

    private void mapDtoToPaperFileEntity(PaperFileDTO dto, PaperFile entity) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        entity.setPaper(paper);
        entity.setUrl(dto.getUrl());
        entity.setIsActive(dto.getIsActive());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperFileResponseDTO mapToPaperFileResponseDTO(PaperFile entity) {
        return PaperFileResponseDTO.builder()
                .id(entity.getId())
                .paper(entity.getPaper())
                .url(entity.getUrl())
                .isActive(entity.getIsActive())
                .build();
    }
}