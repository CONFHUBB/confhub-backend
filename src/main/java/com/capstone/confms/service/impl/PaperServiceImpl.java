package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.PaperService;
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
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;

    @Override
    public List<PaperResponseDTO> getAllPapers() {
        return paperRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaperResponseDTO createPaper(PaperDTO dto) {
        log.info("Registering new Paper with title: {}", dto.getTitle());
        Paper paper = new Paper();
        mapDtoToEntity(dto, paper);
        return mapToResponseDTO(paperRepository.save(paper));
    }

    @Override
    @Transactional
    public PaperResponseDTO updatePaper(Integer id, PaperDTO dto) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + id));
        mapDtoToEntity(dto, paper);
        return mapToResponseDTO(paperRepository.save(paper));
    }

    @Override
    public PaperResponseDTO getPaperById(Integer id) {
        return paperRepository.findById(id)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + id));
    }

    @Override
    @Transactional
    public void deletePaper(Integer id) {
        if (!paperRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. Paper not found with id " + id);
        }
        paperRepository.deleteById(id);
    }

    private void mapDtoToEntity(PaperDTO dto, Paper entity) {
        entity.setAbstractField(dto.getAbstractField());
        entity.setStatus(dto.getStatus());
        entity.setTitle(dto.getTitle());
        entity.setKeyword1(dto.getKeyword1());
        entity.setKeyword2(dto.getKeyword2());
        entity.setKeyword3(dto.getKeyword3());
        entity.setKeyword4(dto.getKeyword4());
        entity.setIsPassedPlagiarism(dto.getIsPassedPlagiarism());
        entity.setSubmissionTime(dto.getSubmissionTime());
        entity.setTrack(dto.getTrack());
        // Handling auditing fields if not automatically handled by JPA Auditing
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperResponseDTO mapToResponseDTO(Paper entity) {
        return PaperResponseDTO.builder()
                .id(entity.getId())
                .track(entity.getTrack())
                .title(entity.getTitle())
                .abstractField(entity.getAbstractField())
                .keyword1(entity.getKeyword1())
                .keyword2(entity.getKeyword2())
                .keyword3(entity.getKeyword3())
                .keyword4(entity.getKeyword4())
                .isPassedPlagiarism(entity.getIsPassedPlagiarism())
                .submissionTime(entity.getSubmissionTime())
                .status(entity.getStatus())
                .build();
    }
}