package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.*;
import com.capstone.confhub.service.PaperFileService;
import com.capstone.confhub.utils.PaginationUtils;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.PaperStatus;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaperFileServiceImpl implements PaperFileService {

    private final PaperFileRepository paperFileRepository;
    private final PaperRepository paperRepository;
    private final ConferenceActivityRepository conferenceActivityRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaperFileResponseDTO> getAllPaperFiles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaperFile> paperFiles = paperFileRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(paperFiles, this::mapToPaperFileResponseDTO);
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
    @Transactional(readOnly = true)
    public List<PaperFileResponseDTO> getFilesByPaperId(Integer paperId) {
        return paperFileRepository.findByPaper_Id(paperId).stream()
                .map(this::mapToPaperFileResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deletePaperFile(Integer id) {
        if (!paperFileRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. PaperFile not found with id " + id);
        }
        paperFileRepository.deleteById(id);
    }

    // ===================== Camera-Ready Methods =====================

    @Override
    @Transactional
    public PaperFileResponseDTO createCameraReadyFile(PaperFileDTO dto) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        // Validate: only ACCEPTED papers can upload camera-ready
        if (paper.getStatus() != PaperStatus.ACCEPTED) {
            throw new BadRequestException(
                    "Camera-ready upload is only allowed for papers with status ACCEPTED. Current status: "
                            + paper.getStatus());
        }

        // Validate: CAMERA_READY_SUBMISSION activity must be enabled
        Integer conferenceId = paper.getTrack().getConference().getId();
        ConferenceActivity activity = conferenceActivityRepository
                .findByConferenceIdAndActivityType(conferenceId, ActivityType.CAMERA_READY_SUBMISSION)
                .orElseThrow(() -> new BadRequestException(
                        "Camera-ready submission activity is not configured for this conference."));

        if (!Boolean.TRUE.equals(activity.getIsEnabled())) {
            throw new BadRequestException("Camera-ready submission is not currently enabled.");
        }

        // Check deadline
        if (activity.getDeadline() != null && LocalDateTime.now().isAfter(activity.getDeadline())) {
            throw new BadRequestException("Camera-ready submission deadline has passed.");
        }

        PaperFile entity = new PaperFile();
        entity.setPaper(paper);
        entity.setUrl(dto.getUrl());
        entity.setIsActive(true);
        entity.setIsCameraReady(true);
        entity.setIsRevision(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        log.info("Camera-ready file uploaded for paper {} in conference {}", paper.getId(), conferenceId);
        return mapToPaperFileResponseDTO(paperFileRepository.save(entity));
    }

    @Override
    @Transactional
    public void approveCameraReady(Integer paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + paperId));

        if (paper.getStatus() != PaperStatus.ACCEPTED) {
            throw new BadRequestException(
                    "Only ACCEPTED papers can be approved for camera-ready. Current status: " + paper.getStatus());
        }

        // Verify camera-ready file exists
        List<PaperFile> cameraReadyFiles = paperFileRepository.findByPaper_Id(paperId).stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsCameraReady()))
                .toList();

        if (cameraReadyFiles.isEmpty()) {
            throw new BadRequestException("No camera-ready file uploaded for this paper yet.");
        }

        // Transition to PUBLISHED
        paper.setStatus(PaperStatus.PUBLISHED);
        paper.setUpdatedAt(LocalDateTime.now());
        paperRepository.save(paper);

        log.info("Paper {} approved as camera-ready → status PUBLISHED", paperId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperFileResponseDTO> getCameraReadyFilesByConference(Integer conferenceId) {
        List<Paper> papers = paperRepository.findByTrack_Conference_Id(conferenceId);
        return papers.stream()
                .flatMap(p -> paperFileRepository.findByPaper_Id(p.getId()).stream())
                .filter(f -> Boolean.TRUE.equals(f.getIsCameraReady()))
                .map(this::mapToPaperFileResponseDTO)
                .collect(Collectors.toList());
    }

    // ===================== Private Helpers =====================

    private void mapDtoToPaperFileEntity(PaperFileDTO dto, PaperFile entity) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        entity.setPaper(paper);
        entity.setUrl(dto.getUrl());
        entity.setIsActive(dto.getIsActive());
        entity.setIsCameraReady(dto.getIsCameraReady() != null ? dto.getIsCameraReady() : false);
        entity.setIsRevision(dto.getIsRevision() != null ? dto.getIsRevision() : false);
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperFileResponseDTO mapToPaperFileResponseDTO(PaperFile entity) {
        return PaperFileResponseDTO.builder()
                .id(entity.getId())
                .paperId(entity.getPaper().getId())
                .url(entity.getUrl())
                .isActive(entity.getIsActive())
                .isCameraReady(entity.getIsCameraReady())
                .isRevision(entity.getIsRevision())
                .build();
    }
}