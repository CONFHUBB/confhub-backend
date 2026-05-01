package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.*;
import com.capstone.confhub.service.NotificationService;
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
    private final PaperAuthorRepository paperAuthorRepository;
    private final NotificationService notificationService;

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
        // Duplicate detection is now advisory — handled by findDuplicateInfo
        // Just log if duplicate found (info is already in plagiarism report)
        if (dto.getFileHash() != null && !dto.getFileHash().isBlank()) {
            List<PaperFile> duplicates = paperFileRepository.findDuplicatesByHash(
                    dto.getFileHash(), dto.getPaperId());
            if (!duplicates.isEmpty()) {
                PaperFile dup = duplicates.get(0);
                log.warn("[DuplicateFile] File hash {} matches paper '{}' — proceeding with upload (advisory)",
                        dto.getFileHash().substring(0, 8), dup.getPaper().getTitle());
            }
        }

        // Auto-delete existing manuscript files for this paper (keep only the latest)
        List<PaperFile> existingManuscripts = paperFileRepository.findByPaper_Id(dto.getPaperId()).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getIsCameraReady())
                        && !Boolean.TRUE.equals(f.getIsCopyrightSubmission())
                        && !Boolean.TRUE.equals(f.getIsSupplementary()))
                .toList();
        for (PaperFile old : existingManuscripts) {
            log.info("Auto-deleting old manuscript file {} for paper {}", old.getId(), dto.getPaperId());
            paperFileRepository.delete(old);
        }

        PaperFile entity = new PaperFile();
        mapDtoToPaperFileEntity(dto, entity);
        return mapToPaperFileResponseDTO(paperFileRepository.save(entity));
    }

    @Override
    @Transactional
    public PaperFileResponseDTO createSupplementaryFile(PaperFileDTO dto) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        PaperFile entity = new PaperFile();
        entity.setPaper(paper);
        entity.setUrl(dto.getUrl());
        entity.setIsActive(true);
        entity.setIsCameraReady(false);
        entity.setIsRevision(false);
        entity.setIsCopyrightSubmission(false);
        entity.setIsSupplementary(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        log.info("Supplementary file uploaded for paper {}", paper.getId());
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

    @Override
    @Transactional
    public PaperFileResponseDTO setActiveFile(Integer fileId) {
        PaperFile target = paperFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("PaperFile not found with id " + fileId));

        // Deactivate all other manuscript files for this paper
        List<PaperFile> allManuscripts = paperFileRepository.findByPaper_Id(target.getPaper().getId()).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getIsCameraReady())
                        && !Boolean.TRUE.equals(f.getIsCopyrightSubmission())
                        && !Boolean.TRUE.equals(f.getIsSupplementary()))
                .toList();

        for (PaperFile pf : allManuscripts) {
            pf.setIsActive(false);
            pf.setUpdatedAt(LocalDateTime.now());
            paperFileRepository.save(pf);
        }

        // Activate the target file
        target.setIsActive(true);
        target.setUpdatedAt(LocalDateTime.now());
        log.info("Set file {} as active manuscript for paper {}", fileId, target.getPaper().getId());
        return mapToPaperFileResponseDTO(paperFileRepository.save(target));
    }

    // ===================== Camera-Ready Methods =====================

    @Override
    @Transactional
    public PaperFileResponseDTO createCameraReadyFile(PaperFileDTO dto) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        // Validate: only REGISTERED, AWAITING_CAMERA_READY, or CAMERA_READY_REJECTED papers can upload camera-ready
        if (paper.getStatus() != PaperStatus.REGISTERED
                && paper.getStatus() != PaperStatus.AWAITING_CAMERA_READY
                && paper.getStatus() != PaperStatus.CAMERA_READY_REJECTED) {
            throw new BadRequestException(
                    "Camera-ready upload is only allowed for papers with status REGISTERED, AWAITING_CAMERA_READY, or CAMERA_READY_REJECTED. Current status: "
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
        PaperFile savedFile = paperFileRepository.save(entity);

        // Transition paper status to CAMERA_READY_SUBMITTED
        paper.setStatus(PaperStatus.CAMERA_READY_SUBMITTED);
        paper.setUpdatedAt(LocalDateTime.now());
        paperRepository.save(paper);

        return mapToPaperFileResponseDTO(savedFile);
    }

    @Override
    @Transactional
    public PaperFileResponseDTO createCopyrightSubmission(PaperFileDTO dto) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        // Validate: only papers in camera-ready phase can upload copyright
        if (paper.getStatus() != PaperStatus.REGISTERED
                && paper.getStatus() != PaperStatus.AWAITING_CAMERA_READY
                && paper.getStatus() != PaperStatus.CAMERA_READY_SUBMITTED
                && paper.getStatus() != PaperStatus.CAMERA_READY_REJECTED) {
            throw new BadRequestException(
                    "Copyright submission is only allowed for papers in camera-ready phase. Current status: "
                            + paper.getStatus());
        }

        // Validate: CAMERA_READY_SUBMISSION activity must be enabled (copyright shares the same phase)
        Integer conferenceId = paper.getTrack().getConference().getId();
        ConferenceActivity activity = conferenceActivityRepository
                .findByConferenceIdAndActivityType(conferenceId, ActivityType.CAMERA_READY_SUBMISSION)
                .orElseThrow(() -> new BadRequestException(
                        "Camera-ready / copyright submission activity is not configured for this conference."));

        if (!Boolean.TRUE.equals(activity.getIsEnabled())) {
            throw new BadRequestException("Camera-ready / copyright submission is not currently enabled.");
        }

        // Check deadline
        if (activity.getDeadline() != null && LocalDateTime.now().isAfter(activity.getDeadline())) {
            throw new BadRequestException("Camera-ready / copyright submission deadline has passed.");
        }

        PaperFile entity = new PaperFile();
        entity.setPaper(paper);
        entity.setUrl(dto.getUrl());
        entity.setIsActive(true);
        entity.setIsCameraReady(false);
        entity.setIsCopyrightSubmission(true);
        entity.setIsRevision(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        log.info("Copyright submission uploaded for paper {} in conference {}", paper.getId(), conferenceId);
        return mapToPaperFileResponseDTO(paperFileRepository.save(entity));
    }

    @Override
    @Transactional
    public void approveCameraReady(Integer paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + paperId));

        if (paper.getStatus() != PaperStatus.CAMERA_READY_SUBMITTED) {
            throw new BadRequestException(
                    "Only CAMERA_READY_SUBMITTED papers can be approved. Current status: " + paper.getStatus());
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

        // Send notification to all authors
        notifyAuthors(paper, "✅ Camera-Ready Approved",
                "Your camera-ready submission for paper \"" + paper.getTitle() + "\" has been approved. The paper is now published!",
                "CAMERA_READY_APPROVED");
    }

    @Override
    @Transactional
    public void rejectCameraReady(Integer paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + paperId));

        if (paper.getStatus() != PaperStatus.CAMERA_READY_SUBMITTED) {
            throw new BadRequestException(
                    "Only CAMERA_READY_SUBMITTED papers can be rejected. Current status: " + paper.getStatus());
        }

        paper.setStatus(PaperStatus.CAMERA_READY_REJECTED);
        paper.setUpdatedAt(LocalDateTime.now());
        paperRepository.save(paper);

        log.info("Paper {} camera-ready rejected → status CAMERA_READY_REJECTED", paperId);

        // Send notification to all authors
        notifyAuthors(paper, "❌ Camera-Ready Revision Required",
                "Your camera-ready submission for paper \"" + paper.getTitle() + "\" requires revisions. Please update and re-submit.",
                "CAMERA_READY_REJECTED");
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

    @Override
    public void validateNoDuplicateHash(PaperFileDTO dto) {
        // Deprecated — duplicate detection is now advisory
        String info = findDuplicateInfo(dto);
        if (info != null) {
            throw new BadRequestException(info);
        }
    }

    @Override
    public String findDuplicateInfo(PaperFileDTO dto) {
        if (dto.getFileHash() == null || dto.getFileHash().isBlank()) {
            return null;
        }
        List<PaperFile> duplicates = paperFileRepository.findDuplicatesByHash(
                dto.getFileHash(), dto.getPaperId());
        if (duplicates.isEmpty()) {
            return null;
        }
        PaperFile dup = duplicates.get(0);
        String dupPaperTitle = dup.getPaper().getTitle();
        String dupConferenceName = dup.getPaper().getTrack().getConference().getName();
        return "This manuscript file has already been submitted for paper \"" + dupPaperTitle
                + "\" at conference \"" + dupConferenceName
                + "\". Duplicate submission detected.";
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
        entity.setIsCopyrightSubmission(dto.getIsCopyrightSubmission() != null ? dto.getIsCopyrightSubmission() : false);
        entity.setIsSupplementary(dto.getIsSupplementary() != null ? dto.getIsSupplementary() : false);
        entity.setFileHash(dto.getFileHash());
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
                .isCopyrightSubmission(entity.getIsCopyrightSubmission())
                .isSupplementary(entity.getIsSupplementary())
                .build();
    }

    /**
     * Send a notification to all authors of a paper.
     */
    private void notifyAuthors(Paper paper, String title, String message, String type) {
        try {
            Integer conferenceId = paper.getTrack() != null ? paper.getTrack().getConference().getId() : null;
            String link = conferenceId != null
                    ? "/conference/" + conferenceId + "/paper/" + paper.getId()
                    : "/paper/" + paper.getId();

            List<PaperAuthor> authors = paperAuthorRepository.findByPaperId(paper.getId());
            for (PaperAuthor pa : authors) {
                NotificationDTO notifDTO = new NotificationDTO();
                notifDTO.setUserId(pa.getUser().getId());
                notifDTO.setConferenceId(conferenceId);
                notifDTO.setTitle(title);
                notifDTO.setMessage(message);
                notifDTO.setType(type);
                notifDTO.setLink(link);
                notificationService.createNotification(notifDTO);
            }
            log.info("Sent {} notification to {} authors for paper {}", type, authors.size(), paper.getId());
        } catch (Exception e) {
            log.error("Failed to send {} notifications for paper {}: {}", type, paper.getId(), e.getMessage());
        }
    }
}