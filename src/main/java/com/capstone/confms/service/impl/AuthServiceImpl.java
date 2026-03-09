package com.capstone.confms.service.impl;

import com.capstone.confms.dto.UserDTO;
import com.capstone.confms.dto.request.ChangePasswordRequest;
import com.capstone.confms.dto.request.ForgotPasswordRequest;
import com.capstone.confms.dto.request.LoginRequest;
import com.capstone.confms.dto.request.ResetPasswordRequest;
import com.capstone.confms.dto.response.JwtResponse;
import com.capstone.confms.dto.response.MessageResponse;
import com.capstone.confms.entity.Role;
import com.capstone.confms.entity.User;
import com.capstone.confms.entity.UserProfile;
import com.capstone.confms.entity.UserRole;
import com.capstone.confms.repository.RoleRepository;
import com.capstone.confms.repository.UserProfileRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.repository.UserRoleRepository;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.security.jwt.JwtTokenProvider;
import com.capstone.confms.security.services.UserDetailsImpl;
import com.capstone.confms.service.AuthService;
import com.capstone.confms.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    private static final int OTP_LENGTH = 6;
    private static final long OTP_TTL_MINUTES = 5;

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                roles);
    }

    @Override
    @Transactional
    public MessageResponse registerUser(UserDTO signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }

        // Create new user's account
        User user = new User();
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setCountry(signUpRequest.getCountry());
        user.setIsActive(true);

        userRepository.save(user);

        // Assign roles from database
        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            // Default role is AUTHOR
            Role authorRole = roleRepository.findByName("AUTHOR")
                    .orElseThrow(() -> new BadRequestException("Role AUTHOR is not found"));
            roles.add(authorRole);
        } else {
            strRoles.forEach(roleName -> {
                Role foundRole = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new BadRequestException("Role " + roleName + " is not found"));
                roles.add(foundRole);
            });
        }

        // Save UserRole entries
        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }

        // Create empty UserProfile
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        userProfileRepository.save(profile);

        return new MessageResponse("User registered successfully!");
    }

    @Override
    public MessageResponse requestOtp() {
        User user = getCurrentAuthenticatedUser();
        sendOtpEmail(user, "Change Password OTP", "Your OTP for changing password is: ");

        return new MessageResponse("OTP sent to your email!");
    }

    @Override
    public MessageResponse changePassword(ChangePasswordRequest changePasswordRequest) {
        User user = getCurrentAuthenticatedUser();
        validateOtp(user, changePasswordRequest.getOtp());

        // Verify current password
        if (!encoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Update password
        user.setPassword(encoder.encode(changePasswordRequest.getNewPassword()));
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        return new MessageResponse("Password changed successfully!");
    }

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found with email: " + request.getEmail()));

        sendOtpEmail(user, "Password Reset OTP", "Your OTP for password reset is: ");

        return new MessageResponse("OTP sent to your email!");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found with email: " + request.getEmail()));

        validateOtp(user, request.getOtp());

        // Update Password
        user.setPassword(encoder.encode(request.getNewPassword()));
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        return new MessageResponse("Password reset successfully!");
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            throw new BadRequestException("No authenticated user found");
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    private String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        int otpValue = secureRandom.nextInt(bound - min) + min;
        return String.valueOf(otpValue);
    }

    private void assignOtpToUser(User user, String otp) {
        user.setOtpCode(otp);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        userRepository.save(user);
    }

    private void sendOtpEmail(User user, String subject, String messagePrefix) {
        String otp = generateOtp();
        assignOtpToUser(user, otp);
        emailService.sendSimpleMessage(
                user.getEmail(),
                subject,
                messagePrefix + otp + "\nThis OTP is valid for " + OTP_TTL_MINUTES + " minutes.");
    }

    private void validateOtp(User user, String otp) {
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }
        if (user.getOtpExpiration() == null || user.getOtpExpiration().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired");
        }
    }
}
