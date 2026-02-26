package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ReviewTypeDTO;
import com.capstone.confms.dto.response.ReviewTypeResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ReviewType;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ReviewTypeRepository;
import com.capstone.confms.service.ReviewTypeService;
import com.capstone.confms.utils.PaginationUtils;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewTypeServiceImpl implements ReviewTypeService {

    private final ReviewTypeRepository reviewTypeRepository;
    private final ConferenceRepository conferenceRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewTypeResponseDTO> getAllReviewTypes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewType> reviewTypes = reviewTypeRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(reviewTypes, this::mapToReviewTypeResponseDTO);
    }

    @Override
    @Transactional
    public ReviewTypeResponseDTO createReviewType(ReviewTypeDTO dto) {
        ReviewType entity = new ReviewType();
        mapDtoToReviewTypeEntity(dto, entity);
        return mapToReviewTypeResponseDTO(reviewTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public ReviewTypeResponseDTO updateReviewType(Integer id, ReviewTypeDTO dto) {
        ReviewType entity = reviewTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewOption not found with id " + id));
        mapDtoToReviewTypeEntity(dto, entity);
        return mapToReviewTypeResponseDTO(reviewTypeRepository.save(entity));
    }

    @Override
    public ReviewTypeResponseDTO getReviewTypeById(Integer id) {
        return reviewTypeRepository.findById(id)
                .map(this::mapToReviewTypeResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewOption not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReviewType(Integer id) {
        if (!reviewTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. ReviewOption not found with id " + id);
        }
        reviewTypeRepository.deleteById(id);
    }

    private void mapDtoToReviewTypeEntity(ReviewTypeDTO dto, ReviewType entity) {
        Conference conference = conferenceRepository.findById(dto.getConferenceId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Conference not found with ID: " + dto.getConferenceId()));

        entity.setConference(conference);
        entity.setReviewOption(dto.getReviewOption());
        entity.setIsRebuttal(dto.getIsRebuttal());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewTypeResponseDTO mapToReviewTypeResponseDTO(ReviewType entity) {
        return ReviewTypeResponseDTO.builder()
                .id(entity.getId())
                .conference(entity.getConference())
                .reviewOption(entity.getReviewOption())
                .isRebuttal(entity.getIsRebuttal())
                .build();
    }
}
