package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.UserConflictRequest;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.entity.UserConflict;
import com.capstone.confhub.repository.UserConflictRepository;
import com.capstone.confhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserConflictServiceImplTest {

    @Mock
    private UserConflictRepository userConflictRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserConflictServiceImpl userConflictService;

    private User user;
    private UserConflict conflict;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);

        conflict = new UserConflict();
        conflict.setId(10);
        conflict.setUser(user);
        conflict.setConflictEmail("conflict@example.com");
        conflict.setConflictName("Conflict Name");
        conflict.setReason("Reason");
        conflict.setIsActive(true);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(userConflictService);
    }

    @Test
    void getConflictsByUserIdShouldReturnList() {
        when(userRepository.existsById(1)).thenReturn(true);
        when(userConflictRepository.findByUserId(1)).thenReturn(List.of(conflict));

        var result = userConflictService.getConflictsByUserId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void addConflictShouldReturnResponse() {
        UserConflictRequest request = UserConflictRequest.builder()
                .conflictEmail("conflict@example.com")
                .conflictName("Conflict Name")
                .reason("Reason")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userConflictRepository.existsByUserIdAndConflictEmail(1, "conflict@example.com")).thenReturn(false);
        when(userConflictRepository.save(any(UserConflict.class))).thenReturn(conflict);

        var result = userConflictService.addConflict(1, request);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void deleteConflictShouldDelete() {
        when(userConflictRepository.existsById(10)).thenReturn(true);

        userConflictService.deleteConflict(10);

        verify(userConflictRepository).deleteById(10);
    }

    @Test
    void toggleConflictActiveShouldReturnResponse() {
        when(userConflictRepository.findById(10)).thenReturn(Optional.of(conflict));
        when(userConflictRepository.save(any(UserConflict.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userConflictService.toggleConflictActive(10);

        assertNotNull(result);
        assertEquals(false, result.getIsActive());
    }
}




