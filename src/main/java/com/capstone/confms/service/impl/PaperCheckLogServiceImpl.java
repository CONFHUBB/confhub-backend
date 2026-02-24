package com.capstone.confms.service.impl;

import com.capstone.confms.dto.PaperCheckLogDTO;
import com.capstone.confms.dto.response.PaperCheckLogResponseDTO;
import com.capstone.confms.entity.PaperCheckLog;
import com.capstone.confms.entity.PaperFile;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.PaperCheckLogRepository;
import com.capstone.confms.repository.PaperFileRepository;
import com.capstone.confms.service.PaperCheckLogService;
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
public class PaperCheckLogServiceImpl implements PaperCheckLogService {

    private final PaperCheckLogRepository paperCheckLogRepository;
    private final PaperFileRepository paperFileRepository;

    @Override
    public List<PaperCheckLogResponseDTO> getAllPaperCheckLogs() {
        return paperCheckLogRepository.findAll().stream()
                .map(this::mapToPaperCheckLogResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaperCheckLogResponseDTO createPaperCheckLog(PaperCheckLogDTO dto) {
        PaperCheckLog entity = new PaperCheckLog();
        mapDtoToPaperCheckLogEntity(dto, entity);
        return mapToPaperCheckLogResponseDTO(paperCheckLogRepository.save(entity));
    }

    @Override
    @Transactional
    public PaperCheckLogResponseDTO updatePaperCheckLog(Integer id, PaperCheckLogDTO dto) {
        PaperCheckLog entity = paperCheckLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaperCheckLog not found with id " + id));
        mapDtoToPaperCheckLogEntity(dto, entity);
        return mapToPaperCheckLogResponseDTO(paperCheckLogRepository.save(entity));
    }

    @Override
    public PaperCheckLogResponseDTO getPaperCheckLogById(Integer id) {
        return paperCheckLogRepository.findById(id)
                .map(this::mapToPaperCheckLogResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("PaperCheckLog not found with id " + id));
    }

    @Override
    @Transactional
    public void deletePaperCheckLog(Integer id) {
        if (!paperCheckLogRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. PaperCheckLog not found with id " + id);
        }
        paperCheckLogRepository.deleteById(id);
    }

    private void mapDtoToPaperCheckLogEntity(PaperCheckLogDTO dto, PaperCheckLog entity) {
        PaperFile paperFile = paperFileRepository.findById(dto.getPaperFileId())
                .orElseThrow(() -> new EntityNotFoundException("PaperFile not found with ID: " + dto.getPaperFileId()));

        entity.setPaperFile(paperFile);
        entity.setIsPassedPlagiarism(dto.getIsPassedPlagiarism());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperCheckLogResponseDTO mapToPaperCheckLogResponseDTO(PaperCheckLog entity) {
        return PaperCheckLogResponseDTO.builder()
                .id(entity.getId())
                .paperFile(entity.getPaperFile())
                .isPassedPlagiarism(entity.getIsPassedPlagiarism())
                .build();
    }
}