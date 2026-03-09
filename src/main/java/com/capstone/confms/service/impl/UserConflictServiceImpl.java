package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.UserConflictRequest;
import com.capstone.confms.dto.response.UserConflictResponseDTO;
import com.capstone.confms.entity.User;
import com.capstone.confms.entity.UserConflict;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.UserConflictRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.UserConflictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserConflictServiceImpl implements UserConflictService {

    private final UserConflictRepository userConflictRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserConflictResponseDTO> getConflictsByUserId(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id " + userId);
        }
        return userConflictRepository.findByUserId(userId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserConflictResponseDTO addConflict(Integer userId, UserConflictRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (request.getConflictEmail() != null &&
                userConflictRepository.existsByUserIdAndConflictEmail(userId, request.getConflictEmail())) {
            throw new BadRequestException("Conflict already exists for email " + request.getConflictEmail());
        }

        UserConflict conflict = new UserConflict();
        conflict.setUser(user);
        conflict.setConflictEmail(request.getConflictEmail());
        conflict.setConflictName(request.getConflictName());
        conflict.setReason(request.getReason());
        conflict.setIsActive(true);

        return mapToResponseDTO(userConflictRepository.save(conflict));
    }

    @Override
    @Transactional
    public void deleteConflict(Integer conflictId) {
        if (!userConflictRepository.existsById(conflictId)) {
            throw new ResourceNotFoundException("UserConflict not found with id " + conflictId);
        }
        userConflictRepository.deleteById(conflictId);
    }

    @Override
    @Transactional
    public UserConflictResponseDTO toggleConflictActive(Integer conflictId) {
        UserConflict conflict = userConflictRepository.findById(conflictId)
                .orElseThrow(() -> new ResourceNotFoundException("UserConflict not found with id " + conflictId));

        conflict.setIsActive(!Boolean.TRUE.equals(conflict.getIsActive()));
        return mapToResponseDTO(userConflictRepository.save(conflict));
    }

    private UserConflictResponseDTO mapToResponseDTO(UserConflict entity) {
        return UserConflictResponseDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .conflictEmail(entity.getConflictEmail())
                .conflictName(entity.getConflictName())
                .reason(entity.getReason())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
