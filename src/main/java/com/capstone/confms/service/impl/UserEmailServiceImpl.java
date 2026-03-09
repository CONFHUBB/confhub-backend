package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.UserEmailRequest;
import com.capstone.confms.dto.response.UserEmailResponseDTO;
import com.capstone.confms.entity.User;
import com.capstone.confms.entity.UserEmail;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.UserEmailRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.UserEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserEmailServiceImpl implements UserEmailService {

    private final UserEmailRepository userEmailRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserEmailResponseDTO> getEmailsByUserId(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id " + userId);
        }
        return userEmailRepository.findByUserId(userId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserEmailResponseDTO addEmail(Integer userId, UserEmailRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        if (userEmailRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email " + request.getEmail() + " is already in use");
        }

        UserEmail email = new UserEmail();
        email.setUser(user);
        email.setEmail(request.getEmail());
        email.setIsPrimary(false);
        email.setIsVerified(false);

        return mapToResponseDTO(userEmailRepository.save(email));
    }

    @Override
    @Transactional
    public void deleteEmail(Integer emailId) {
        UserEmail email = userEmailRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("UserEmail not found with id " + emailId));

        if (Boolean.TRUE.equals(email.getIsPrimary())) {
            throw new BadRequestException("Cannot delete the primary email address");
        }

        userEmailRepository.deleteById(emailId);
    }

    @Override
    @Transactional
    public UserEmailResponseDTO setPrimaryEmail(Integer emailId) {
        UserEmail email = userEmailRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("UserEmail not found with id " + emailId));

        // Unset all other primary emails for this user
        List<UserEmail> userEmails = userEmailRepository.findByUserId(email.getUser().getId());
        for (UserEmail e : userEmails) {
            if (Boolean.TRUE.equals(e.getIsPrimary())) {
                e.setIsPrimary(false);
                userEmailRepository.save(e);
            }
        }

        email.setIsPrimary(true);
        return mapToResponseDTO(userEmailRepository.save(email));
    }

    private UserEmailResponseDTO mapToResponseDTO(UserEmail entity) {
        return UserEmailResponseDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .email(entity.getEmail())
                .isPrimary(entity.getIsPrimary())
                .isVerified(entity.getIsVerified())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
