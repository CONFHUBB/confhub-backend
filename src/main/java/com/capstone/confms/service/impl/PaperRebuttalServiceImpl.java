package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.PaperRebuttalService;
import com.capstone.confms.service.PaperService;
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
public class PaperRebuttalServiceImpl implements PaperRebuttalService {

    private final PaperRebuttalRepository paperRebuttalRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public List<PaperRebuttalResponseDTO> getAllPaperRebuttals() {
        return paperRebuttalRepository.findAll().stream()
                .map(this::mapToPaperRebuttalResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaperRebuttalResponseDTO createPaperRebuttal(PaperRebuttalDTO dto) {
        PaperRebuttal entity = new PaperRebuttal();
        mapDtoToPaperRebuttalEntity(dto, entity);
        return mapToPaperRebuttalResponseDTO(paperRebuttalRepository.save(entity));
    }

    @Override
    @Transactional
    public PaperRebuttalResponseDTO updatePaperRebuttal(Integer id, PaperRebuttalDTO dto) {
        PaperRebuttal entity = paperRebuttalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaperRebuttal not found with id " + id));
        mapDtoToPaperRebuttalEntity(dto, entity);
        return mapToPaperRebuttalResponseDTO(paperRebuttalRepository.save(entity));
    }

    @Override
    public PaperRebuttalResponseDTO getPaperRebuttalById(Integer id) {
        return paperRebuttalRepository.findById(id)
                .map(this::mapToPaperRebuttalResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("PaperRebuttal not found with id " + id));
    }

    @Override
    @Transactional
    public void deletePaperRebuttal(Integer id) {
        if (!paperRebuttalRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. PaperRebuttal not found with id " + id);
        }
        paperRebuttalRepository.deleteById(id);
    }

    private void mapDtoToPaperRebuttalEntity(PaperRebuttalDTO dto, PaperRebuttal entity) {

        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getUserId()));

        Review review = reviewRepository.findById(dto.getReviewId())
                .orElseThrow(() -> new EntityNotFoundException("Review not found with ID: " + dto.getReviewId()));

        entity.setPaper(paper);
        entity.setReview(review);
        entity.setUser(user);
        entity.setContent(dto.getContent());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperRebuttalResponseDTO mapToPaperRebuttalResponseDTO(PaperRebuttal entity) {
        return PaperRebuttalResponseDTO.builder()
                .id(entity.getId())
                .paper(entity.getPaper())
                .review(entity.getReview())
                .user(entity.getUser())
                .content(entity.getContent())
                .build();
    }
}