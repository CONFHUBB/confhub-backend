package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.UserDTO;
import com.capstone.confhub.dto.request.ChangePasswordRequest;
import com.capstone.confhub.dto.request.ForgotPasswordRequest;
import com.capstone.confhub.dto.request.LoginRequest;
import com.capstone.confhub.dto.request.ResetPasswordRequest;
import com.capstone.confhub.dto.response.JwtResponse;
import com.capstone.confhub.dto.response.MessageResponse;
import com.capstone.confhub.entity.Role;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.RoleRepository;
import com.capstone.confhub.repository.UserProfileRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.repository.UserRoleRepository;
import com.capstone.confhub.security.jwt.JwtTokenProvider;
import com.capstone.confhub.security.services.UserDetailsImpl;
import com.capstone.confhub.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail("john@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setCountry("VN");
        user.setPassword("encoded-old");
        user.setIsActive(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateService() {
        assertNotNull(authService);
    }

    @Test
    void authenticateUserShouldReturnJwtResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password");

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        UserDetailsImpl principal = new UserDetailsImpl(
                1,
                "john@example.com",
                "John",
                "Doe",
                "VN",
                "encoded-old",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_AUTHOR"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtTokenProvider.generateJwtToken(authentication)).thenReturn("jwt-token");

        JwtResponse response = authService.authenticateUser(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(1, response.getId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(1, response.getRoles().size());
        assertEquals("ROLE_AUTHOR", response.getRoles().get(0));
    }

    @Test
    void registerUserShouldReturnSuccessMessage() {
        UserDTO request = UserDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("plaintext")
                .country("VN")
                .build();

        Role authorRole = new Role();
        authorRole.setId(2);
        authorRole.setName("AUTHOR");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(encoder.encode("plaintext")).thenReturn("encoded-new");
        when(roleRepository.findByName("AUTHOR")).thenReturn(Optional.of(authorRole));

        MessageResponse response = authService.registerUser(request);

        assertNotNull(response);
        assertEquals("User registered successfully!", response.getMessage());
        verify(userRepository).save(any(User.class));
        verify(userRoleRepository).save(any());
        verify(userProfileRepository).save(any());
    }

    @Test
    void requestOtpShouldReturnSuccessMessage() {
        setAuthenticatedUser(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = authService.requestOtp();

        assertNotNull(response);
        assertEquals("OTP sent to your email!", response.getMessage());
        verify(emailService).sendSimpleMessage(eq("john@example.com"), eq("Change Password OTP"), any(String.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePasswordShouldReturnSuccessMessage() {
        setAuthenticatedUser(user.getId());
        user.setOtpCode("123456");
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(3));

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-plain");
        request.setNewPassword("new-plain");
        request.setOtp("123456");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(encoder.matches("old-plain", "encoded-old")).thenReturn(true);
        when(encoder.encode("new-plain")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = authService.changePassword(request);

        assertNotNull(response);
        assertEquals("Password changed successfully!", response.getMessage());
        assertEquals("encoded-new", user.getPassword());
        assertNull(user.getOtpCode());
        assertNull(user.getOtpExpiration());
    }

    @Test
    void forgotPasswordShouldReturnSuccessMessage() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("john@example.com");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertEquals("OTP sent to your email!", response.getMessage());
        verify(emailService).sendSimpleMessage(eq("john@example.com"), eq("Password Reset OTP"), any(String.class));
    }

    @Test
    void resetPasswordShouldReturnSuccessMessage() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");
        request.setNewPassword("new-password");

        user.setOtpCode("123456");
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(3));

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(encoder.encode("new-password")).thenReturn("encoded-reset");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = authService.resetPassword(request);

        assertNotNull(response);
        assertEquals("Password reset successfully!", response.getMessage());
        assertEquals("encoded-reset", user.getPassword());
        assertNull(user.getOtpCode());
        assertNull(user.getOtpExpiration());
    }

    private void setAuthenticatedUser(Integer userId) {
        UserDetailsImpl principal = new UserDetailsImpl(
                userId,
                "john@example.com",
                "John",
                "Doe",
                "VN",
                "encoded-old",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_AUTHOR"))
        );

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
