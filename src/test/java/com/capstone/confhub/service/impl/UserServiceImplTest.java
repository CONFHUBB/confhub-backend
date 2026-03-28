package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.UserDTO;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setTitle("Dr.");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setGender("Female");
        user.setEmail("jane@example.com");
        user.setPassword("password123");
        user.setCountry("Vietnam");
        user.setIsActive(true);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(userService);
    }

    @Test
    void getAllUsersShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
        when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = userService.getAllUsers(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createUserShouldReturnResponse() {
        UserDTO dto = UserDTO.builder()
                .title("Dr.")
                .firstName("Jane")
                .lastName("Doe")
                .gender("Female")
                .email("jane@example.com")
                .password("password123")
                .country("Vietnam")
                .build();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1);
            return saved;
        });

        var result = userService.createUser(dto);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("jane@example.com", result.getEmail());
    }

    @Test
    void updateUserShouldReturnResponse() {
        UserDTO dto = UserDTO.builder()
                .title("Prof.")
                .firstName("Janet")
                .lastName("Doe")
                .gender("Female")
                .email("janet@example.com")
                .password("password123")
                .country("Singapore")
                .build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userService.updateUser(1, dto);

        assertNotNull(result);
        assertEquals("Janet", result.getFirstName());
        assertEquals("janet@example.com", result.getEmail());
    }

    @Test
    void getUserByIdShouldReturnResponse() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        var result = userService.getUserById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void getUserByEmailShouldReturnResponse() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        var result = userService.getUserByEmail("jane@example.com");

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void deleteUserShouldDelete() {
        when(userRepository.existsById(1)).thenReturn(true);

        userService.deleteUser(1);

        verify(userRepository).deleteById(1);
    }
}



