package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.UserEmailRequest;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.entity.UserEmail;
import com.capstone.confhub.repository.UserEmailRepository;
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
public class UserEmailServiceImplTest {

    @Mock
    private UserEmailRepository userEmailRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserEmailServiceImpl userEmailService;

    private User user;
    private UserEmail userEmail;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);

        userEmail = new UserEmail();
        userEmail.setId(10);
        userEmail.setUser(user);
        userEmail.setEmail("user@example.com");
        userEmail.setIsPrimary(false);
        userEmail.setIsVerified(false);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(userEmailService);
    }

    @Test
    void getEmailsByUserIdShouldReturnList() {
        when(userRepository.existsById(1)).thenReturn(true);
        when(userEmailRepository.findByUserId(1)).thenReturn(List.of(userEmail));

        var result = userEmailService.getEmailsByUserId(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void addEmailShouldReturnResponse() {
        UserEmailRequest request = UserEmailRequest.builder().email("user@example.com").build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userEmailRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userEmailRepository.save(any(UserEmail.class))).thenReturn(userEmail);

        var result = userEmailService.addEmail(1, request);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void deleteEmailShouldDelete() {
        when(userEmailRepository.findById(10)).thenReturn(Optional.of(userEmail));

        userEmailService.deleteEmail(10);

        verify(userEmailRepository).deleteById(10);
    }

    @Test
    void setPrimaryEmailShouldReturnResponse() {
        UserEmail oldPrimary = new UserEmail();
        oldPrimary.setId(11);
        oldPrimary.setUser(user);
        oldPrimary.setEmail("old@example.com");
        oldPrimary.setIsPrimary(true);
        oldPrimary.setIsVerified(true);

        when(userEmailRepository.findById(10)).thenReturn(Optional.of(userEmail));
        when(userEmailRepository.findByUserId(1)).thenReturn(List.of(oldPrimary, userEmail));
        when(userEmailRepository.save(any(UserEmail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userEmailService.setPrimaryEmail(10);

        assertNotNull(result);
        assertEquals(true, result.getIsPrimary());
    }
}




