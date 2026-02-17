package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceSubmissionFormDTO;
import com.capstone.confms.dto.response.ConferenceSubmissionFormResponseDTO;
import com.capstone.confms.entity.ConferenceSubmissionForm;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.ConferenceTrackTopic;
import com.capstone.confms.repository.ConferenceSubmissionFormRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.ConferenceTrackTopicRepository;
import com.capstone.confms.service.ConferenceSubmissionFormService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConferenceSubmissionFormServiceImpl implements ConferenceSubmissionFormService {

    private final ConferenceSubmissionFormRepository submissionFormRepository;
    private final ConferenceTrackRepository trackRepository;
    private final ConferenceTrackTopicRepository topicRepository;

    @Override
    @Transactional
    public ConferenceSubmissionFormResponseDTO createSubmissionForm(ConferenceSubmissionFormDTO dto) {
        ConferenceTrackTopic topic = topicRepository.findById(dto.getTopicId())
                .orElseThrow(() -> new EntityNotFoundException("Topic not found with ID: " + dto.getTopicId()));

        ConferenceTrack track = topic.getTrack();

        ConferenceSubmissionForm form = new ConferenceSubmissionForm();
        form.setTrack(track);
        form.setTopic(topic);
        mapDtoToEntity(dto, form);

        ConferenceSubmissionForm savedForm = submissionFormRepository.save(form);
        return mapEntityToResponse(savedForm);
    }

    @Override
    @Transactional
    public ConferenceSubmissionFormResponseDTO updateSubmissionForm(Integer id, ConferenceSubmissionFormDTO dto) {
        ConferenceSubmissionForm form = submissionFormRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Submission Form not found with ID: " + id));

        if (dto.getTopicId() != null && !dto.getTopicId().equals(form.getTopic().getId())) {
            ConferenceTrackTopic newTopic = topicRepository.findById(dto.getTopicId())
                    .orElseThrow(() -> new EntityNotFoundException("Topic not found with ID: " + dto.getTopicId()));

            if (!newTopic.getTrack().getId().equals(form.getTrack().getId())) {
                 throw new IllegalArgumentException("The selected Topic does not belong to the selected Track.");
            }
            form.setTopic(newTopic);
        }

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
    public List<ConferenceSubmissionFormResponseDTO> getAllSubmissionForms() {
        return submissionFormRepository.findAll().stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConferenceSubmissionFormResponseDTO> getSubmissionFormsByTrackId(Integer trackId) {
        if(!trackRepository.existsById(trackId)) {
            throw new EntityNotFoundException("Submission not found with Track ID: " + trackId);
        }

        return submissionFormRepository.findByTrackId(trackId).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConferenceSubmissionFormResponseDTO> getSubmissionFormsByTopicId(Integer topicId) {
        if(!topicRepository.existsById(topicId)) {
            throw new EntityNotFoundException("Submission not found with Topic ID: " + topicId);
        }

        return submissionFormRepository.findByTopicId(topicId).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
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
        response.setTopic(entity.getTopic());
        response.setTitle(entity.getTitle());
        response.setAbstractField(entity.getAbstractField());
        response.setKeyword1(entity.getKeyword1());
        response.setKeyword2(entity.getKeyword2());
        response.setKeyword3(entity.getKeyword3());
        response.setKeyword4(entity.getKeyword4());
        return response;
    }
}