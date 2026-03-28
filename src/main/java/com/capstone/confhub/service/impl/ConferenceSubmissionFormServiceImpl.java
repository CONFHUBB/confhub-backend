package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ConferenceSubmissionFormDTO;
import com.capstone.confhub.dto.response.ConferenceSubmissionFormResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceSubmissionForm;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceSubmissionFormRepository;
import com.capstone.confhub.service.ConferenceSubmissionFormService;
import com.capstone.confhub.utils.PaginationUtils;
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
    private final ConferenceRepository conferenceRepository;

    @Override
    @Transactional
    public ConferenceSubmissionFormResponseDTO createSubmissionForm(ConferenceSubmissionFormDTO dto) {
        Conference conference = conferenceRepository.findById(dto.getConferenceId())
                .orElseThrow(() -> new EntityNotFoundException("Conference not found with ID: " + dto.getConferenceId()));

        ConferenceSubmissionForm form = new ConferenceSubmissionForm();
        form.setConference(conference);
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
    public PagedResponse<ConferenceSubmissionFormResponseDTO> getSubmissionFormsByConferenceId(Integer conferenceId, int page,
            int size) {
        if (!conferenceRepository.existsById(conferenceId)) {
            throw new EntityNotFoundException("Submission not found with Conference ID: " + conferenceId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ConferenceSubmissionForm> forms = submissionFormRepository.findByConferenceId(conferenceId, pageable);
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
        entity.setDefinitionJson(dto.getDefinitionJson());
    }

    private ConferenceSubmissionFormResponseDTO mapEntityToResponse(ConferenceSubmissionForm entity) {
        ConferenceSubmissionFormResponseDTO response = new ConferenceSubmissionFormResponseDTO();
        response.setId(entity.getId());
        response.setConference(entity.getConference());
        response.setDefinitionJson(entity.getDefinitionJson());
        return response;
    }
}