package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.PaperService;
import com.capstone.confms.utils.PaginationUtils;
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
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final ConferenceTrackRepository conferenceTrackRepository;
    private final SubjectAreaRepository subjectAreaRepository;
    private final com.capstone.confms.repository.PaperAuthorRepository paperAuthorRepository;
    private final ConferenceSubmissionFormRepository conferenceSubmissionFormRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaperResponseDTO> getAllPapers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Paper> papers = paperRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(papers, this::mapToResponseDTO);
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
    @Transactional
    public PaperResponseDTO updatePaperStatus(Integer id, PaperUpdateStatusDTO dto) {
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

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaperResponseDTO> getPapersByAuthor(Integer authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PaperAuthor> paperAuthors = paperAuthorRepository.findByUserId(authorId, pageable);
        return PaginationUtils.toPagedResponse(paperAuthors, pa -> mapToResponseDTO(pa.getPaper()));
    }

    private void mapDtoToEntity(PaperDTO dto, Paper entity) {
        ConferenceTrack conferenceTrack = conferenceTrackRepository.findById(dto.getConferenceTrackId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Track not found with ID: " + dto.getConferenceTrackId()));

        SubjectArea primarySA = null;
        if (dto.getPrimarySubjectAreaId() != null) {
            primarySA = subjectAreaRepository.findById(dto.getPrimarySubjectAreaId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Subject Area not found with ID: " + dto.getPrimarySubjectAreaId()));
        }

        List<SubjectArea> secondarySAs = new ArrayList<>();
        if (dto.getSecondarySubjectAreaIds() != null && !dto.getSecondarySubjectAreaIds().isEmpty()) {
            secondarySAs = subjectAreaRepository.findAllById(dto.getSecondarySubjectAreaIds());
            if (secondarySAs.size() != dto.getSecondarySubjectAreaIds().size()) {
                throw new EntityNotFoundException("One or more secondary Subject Area IDs not found");
            }
        }

        ConferenceSubmissionForm submissionForm = null;
        if (dto.getSubmissionFormId() != null) {
            submissionForm = conferenceSubmissionFormRepository.findById(dto.getSubmissionFormId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Submission Form not found with ID: " + dto.getSubmissionFormId()));
        }

        entity.setAbstractField(dto.getAbstractField());
        entity.setStatus(dto.getStatus());
        entity.setTitle(dto.getTitle());
        entity.setKeyword1(dto.getKeyword1());
        entity.setKeyword2(dto.getKeyword2());
        entity.setKeyword3(dto.getKeyword3());
        entity.setKeyword4(dto.getKeyword4());
        entity.setSubmissionForm(submissionForm);
        entity.setExtraAnswersJson(dto.getExtraAnswersJson());
        entity.setIsPassedPlagiarism(dto.getIsPassedPlagiarism());
        entity.setSubmissionTime(dto.getSubmissionTime());
        entity.setTrack(conferenceTrack);
        entity.setPrimarySubjectArea(primarySA);
        entity.setSecondarySubjectAreas(secondarySAs);
        // Handling auditing fields if not automatically handled by JPA Auditing
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private void mapDtoToEntity(PaperUpdateStatusDTO dto, Paper entity) {
        entity.setStatus(dto.getStatus());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private PaperResponseDTO mapToResponseDTO(Paper entity) {
        List<Integer> secondaryIds = entity.getSecondarySubjectAreas() != null
                ? entity.getSecondarySubjectAreas().stream().map(SubjectArea::getId).toList()
                : List.of();

        return PaperResponseDTO.builder()
                .id(entity.getId())
                .trackId(entity.getTrack().getId())
                .primarySubjectAreaId(
                        entity.getPrimarySubjectArea() != null ? entity.getPrimarySubjectArea().getId() : null)
                .secondarySubjectAreaIds(secondaryIds)
                .title(entity.getTitle())
                .abstractField(entity.getAbstractField())
                .keyword1(entity.getKeyword1())
                .keyword2(entity.getKeyword2())
                .keyword3(entity.getKeyword3())
                .keyword4(entity.getKeyword4())
                .isPassedPlagiarism(entity.getIsPassedPlagiarism())
                .submissionTime(entity.getSubmissionTime())
                .status(entity.getStatus())
                .submissionFormId(entity.getSubmissionForm() != null ? entity.getSubmissionForm().getId() : null)
                .extraAnswersJson(entity.getExtraAnswersJson())
                .build();
    }
}