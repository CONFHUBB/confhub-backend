package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.UserEmailRequest;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.entity.UserEmail;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserEmailServiceImplTest {

    private static final int USER_ID = 1;
    private static final int EMAIL_ID = 10;

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
        user.setId(USER_ID);

        userEmail = new UserEmail();
        userEmail.setId(EMAIL_ID);
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
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userEmailRepository.findByUserId(USER_ID)).thenReturn(List.of(userEmail));

        var result = userEmailService.getEmailsByUserId(USER_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getEmailsByUserIdShouldThrowWhenUserNotFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> userEmailService.getEmailsByUserId(USER_ID));
    }

    @Test
    void addEmailShouldReturnResponse() {
        UserEmailRequest request = UserEmailRequest.builder().email("user@example.com").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userEmailRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userEmailRepository.save(any(UserEmail.class))).thenReturn(userEmail);

        var result = userEmailService.addEmail(USER_ID, request);

        assertNotNull(result);
        assertEquals(EMAIL_ID, result.getId());
    }

    @Test
    void addEmailShouldThrowWhenUserNotFound() {
        UserEmailRequest request = UserEmailRequest.builder().email("user@example.com").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userEmailService.addEmail(USER_ID, request));
    }

    @Test
    void addEmailShouldThrowWhenEmailAlreadyUsed() {
        UserEmailRequest request = UserEmailRequest.builder().email("user@example.com").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userEmailRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> userEmailService.addEmail(USER_ID, request));
    }

    @Test
    void deleteEmailShouldDelete() {
        when(userEmailRepository.findById(EMAIL_ID)).thenReturn(Optional.of(userEmail));

        userEmailService.deleteEmail(EMAIL_ID);

        verify(userEmailRepository).deleteById(EMAIL_ID);
    }

    @Test
    void deleteEmailShouldThrowWhenEmailNotFound() {
        when(userEmailRepository.findById(EMAIL_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userEmailService.deleteEmail(EMAIL_ID));
    }

    @Test
    void deleteEmailShouldThrowWhenDeletingPrimaryEmail() {
        userEmail.setIsPrimary(true);
        when(userEmailRepository.findById(EMAIL_ID)).thenReturn(Optional.of(userEmail));

        assertThrows(BadRequestException.class,
                () -> userEmailService.deleteEmail(EMAIL_ID));
    }

    @Test
    void setPrimaryEmailShouldReturnResponse() {
        UserEmail oldPrimary = new UserEmail();
        oldPrimary.setId(11);
        oldPrimary.setUser(user);
        oldPrimary.setEmail("old@example.com");
        oldPrimary.setIsPrimary(true);
        oldPrimary.setIsVerified(true);

        when(userEmailRepository.findById(EMAIL_ID)).thenReturn(Optional.of(userEmail));
        when(userEmailRepository.findByUserId(USER_ID)).thenReturn(List.of(oldPrimary, userEmail));
        when(userEmailRepository.save(any(UserEmail.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userEmailService.setPrimaryEmail(EMAIL_ID);

        assertNotNull(result);
        assertEquals(true, result.getIsPrimary());
    }

    @Test
    void setPrimaryEmailShouldThrowWhenEmailNotFound() {
        when(userEmailRepository.findById(EMAIL_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userEmailService.setPrimaryEmail(EMAIL_ID));
    }
}




