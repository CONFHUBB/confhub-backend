package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceSubmissionFormDTO;
import com.capstone.confms.dto.response.ConferenceSubmissionFormResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.entity.ConferenceSubmissionForm;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.repository.ConferenceSubmissionFormRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.service.ConferenceSubmissionFormService;
import com.capstone.confms.utils.PaginationUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConferenceSubmissionFormServiceImpl implements ConferenceSubmissionFormService {

    private final ConferenceSubmissionFormRepository submissionFormRepository;
    private final ConferenceTrackRepository trackRepository;

    @Override
    @Transactional
    public ConferenceSubmissionFormResponseDTO createSubmissionForm(ConferenceSubmissionFormDTO dto) {
        ConferenceTrack track = trackRepository.findById(dto.getTrackId())
                .orElseThrow(() -> new EntityNotFoundException("Track not found with ID: " + dto.getTrackId()));

        ConferenceSubmissionForm form = new ConferenceSubmissionForm();
        form.setTrack(track);
        mapDtoToEntity(dto, form);

        ConferenceSubmissionForm savedForm = submissionFormRepository.save(form);
        return mapEntityToResponse(savedForm);
    }

    @Override
    @Transactional
    public ConferenceSubmissionFormResponseDTO updateSubmissionForm(Integer id, ConferenceSubmissionFormDTO dto) {
        ConferenceSubmissionForm form = submissionFormRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Submission Form not found with ID: " + id));

        mapDtoToEntity(dto, form);
        ConferenceSubmissionForm updatedForm = submissionFormRepository.save(form);
        return mapEntityToResponse(updatedForm);
    }

    @Override
    public ConferenceSubmissionFormResponseDTO getSubmissionFormById(Integer id) {
        ConferenceSubmissionForm form = submissionFormRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Submission Form not found with ID: " + id));
        return mapEntityToResponse(form);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ConferenceSubmissionFormResponseDTO> getAllSubmissionForms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ConferenceSubmissionForm> forms = submissionFormRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(forms, this::mapEntityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ConferenceSubmissionFormResponseDTO> getSubmissionFormsByTrackId(Integer trackId, int page,
            int size) {
        if (!trackRepository.existsById(trackId)) {
            throw new EntityNotFoundException("Submission not found with Track ID: " + trackId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ConferenceSubmissionForm> forms = submissionFormRepository.findByTrackId(trackId, pageable);
        return PaginationUtils.toPagedResponse(forms, this::mapEntityToResponse);
    }

    @Override
    @Transactional
    public void deleteSubmissionForm(Integer id) {
        if (!submissionFormRepository.existsById(id)) {
            throw new EntityNotFoundException("Submission Form not found with ID: " + id);
        }
        submissionFormRepository.deleteById(id);
    }

    private void mapDtoToEntity(ConferenceSubmissionFormDTO dto, ConferenceSubmissionForm entity) {
        entity.setTitle(dto.getTitle());
        entity.setAbstractField(dto.getAbstractField());
        entity.setKeyword1(dto.getKeyword1());
        entity.setKeyword2(dto.getKeyword2());
        entity.setKeyword3(dto.getKeyword3());
        entity.setKeyword4(dto.getKeyword4());
    }

    private ConferenceSubmissionFormResponseDTO mapEntityToResponse(ConferenceSubmissionForm entity) {
        ConferenceSubmissionFormResponseDTO response = new ConferenceSubmissionFormResponseDTO();
        response.setId(entity.getId());
        response.setTrack(entity.getTrack());
        response.setTitle(entity.getTitle());
        response.setAbstractField(entity.getAbstractField());
        response.setKeyword1(entity.getKeyword1());
        response.setKeyword2(entity.getKeyword2());
        response.setKeyword3(entity.getKeyword3());
        response.setKeyword4(entity.getKeyword4());
        return response;
    }
}