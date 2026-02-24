package com.capstone.confms.service.impl;

import com.capstone.confms.dto.PaperAuthorDTO;
import com.capstone.confms.dto.response.PaperAuthorResponseDTO;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.PaperAuthor;
import com.capstone.confms.entity.User;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.PaperAuthorRepository;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.PaperAuthorService;
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
public class PaperAuthorServiceImpl implements PaperAuthorService {

    private final PaperAuthorRepository paperAuthorRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;

    @Override
    public List<PaperAuthorResponseDTO> getAllPaperAuthors() {
        return paperAuthorRepository.findAll().stream()
                .map(this::mapToPaperAuthorResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaperAuthorResponseDTO> getAuthorsByPaper(Integer paperId) {
        return paperAuthorRepository.findByPaperId(paperId).stream()
                .map(this::mapToPaperAuthorResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaperAuthorResponseDTO createPaperAuthor(PaperAuthorDTO dto) {
        PaperAuthor entity = new PaperAuthor();
        mapDtoToPaperAuthorEntity(dto, entity);
        return mapToPaperAuthorResponseDTO(paperAuthorRepository.save(entity));
    }

    @Override
    @Transactional
    public PaperAuthorResponseDTO updatePaperAuthor(Integer id, PaperAuthorDTO dto) {
        PaperAuthor entity = paperAuthorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaperAuthor not found with id " + id));
        mapDtoToPaperAuthorEntity(dto, entity);
        return mapToPaperAuthorResponseDTO(paperAuthorRepository.save(entity));
    }

    @Override
    public PaperAuthorResponseDTO getPaperAuthorById(Integer id) {
        return paperAuthorRepository.findById(id)
                .map(this::mapToPaperAuthorResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("PaperAuthor not found with id " + id));
    }

    @Override
    @Transactional
    public void deletePaperAuthor(Integer id) {
        if (!paperAuthorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. PaperAuthor not found with id " + id);
        }
        paperAuthorRepository.deleteById(id);
    }

    private void mapDtoToPaperAuthorEntity(PaperAuthorDTO dto, PaperAuthor entity) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getUserId()));

        entity.setPaper(paper);
        entity.setUser(user);

        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperAuthorResponseDTO mapToPaperAuthorResponseDTO(PaperAuthor entity) {
        return PaperAuthorResponseDTO.builder()
                .id(entity.getId())
                .paper(entity.getPaper())
                .user(entity.getUser())
                .build();
    }
}