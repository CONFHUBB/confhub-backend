package com.capstone.confms.service.impl;

import com.capstone.confms.dto.UserDTO;
import com.capstone.confms.dto.response.UserResponseDTO;
import com.capstone.confms.entity.User;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.UserService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                             .map(this::mapToResponseDTO)
                             .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponseDTO createUser(UserDTO dto) {
        log.info("Registering new user with email: {}", dto.getEmail());
        User user = new User();
        mapDtoToEntity(dto, user);
        return mapToResponseDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(Integer id, UserDTO dto) {
        User user = userRepository.findById(id)
                                  .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
        mapDtoToEntity(dto, user);
        return mapToResponseDTO(userRepository.save(user));
    }

    @Override
    public UserResponseDTO getUserById(Integer id) {
        return userRepository.findById(id)
                             .map(this::mapToResponseDTO)
                             .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + id));
    }

    @Override
    public UserResponseDTO getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                             .map(this::mapToResponseDTO)
                             .orElseThrow(() -> new ResourceNotFoundException("User not found with email " + email));
    }

    @Override
    @Transactional
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. User not found with id " + id);
        }
        userRepository.deleteById(id);
    }

    private void mapDtoToEntity(UserDTO dto, User entity) {
        entity.setFullName(dto.getFullName());
        entity.setEmail(dto.getEmail());
        entity.setPassword(dto.getPassword());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setCountry(dto.getCountry());
        if (entity.getIsActive() == null) {
            entity.setIsActive(Boolean.TRUE);
        }
        entity.setCreatedAt(LocalDateTime.now());
    }

    private UserResponseDTO mapToResponseDTO(User entity) {
        return UserResponseDTO.builder()
                              .id(entity.getId())
                              .fullName(entity.getFullName())
                              .email(entity.getEmail())
                              .phoneNumber(entity.getPhoneNumber())
                              .country(entity.getCountry())
                              .isActive(entity.getIsActive())
                              .createdAt(entity.getCreatedAt())
                              .build();
    }
}