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
import com.capstone.confms.entity.UserRole;
import com.capstone.confms.repository.RoleRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.repository.UserRoleRepository;
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
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

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
                userDetails.getFullName(),
                roles);
    }

    @Override
    @Transactional
    public MessageResponse registerUser(UserDTO signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        // Create new user's account
        User user = new User();
        user.setFullName(signUpRequest.getFullName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setCountry(signUpRequest.getCountry());
        user.setIsActive(true);

        userRepository.save(user);

        // Assign roles from database
        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            // Default role is AUTHOR
            Role authorRole = roleRepository.findByName("AUTHOR")
                    .orElseThrow(() -> new RuntimeException("Error: Role AUTHOR is not found."));
            roles.add(authorRole);
        } else {
            strRoles.forEach(roleName -> {
                Role foundRole = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new RuntimeException("Error: Role " + roleName + " is not found."));
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

        return new MessageResponse("User registered successfully!");
    }

    @Override
    public MessageResponse requestOtp() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: User not found."));

        // Generate OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        user.setOtpCode(otp);
        user.setOtpExpiration(java.time.LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // Send Email
        emailService.sendSimpleMessage(
                user.getEmail(),
                "Change Password OTP",
                "Your OTP for changing password is: " + otp + "\nThis OTP is valid for 5 minutes.");

        return new MessageResponse("OTP sent to your email!");
    }

    @Override
    public MessageResponse changePassword(ChangePasswordRequest changePasswordRequest) {
        // Get current user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: User not found."));

        // Verify OTP
        if (user.getOtpCode() == null || !user.getOtpCode().equals(changePasswordRequest.getOtp())) {
            throw new RuntimeException("Error: Invalid OTP!");
        }

        if (user.getOtpExpiration().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Error: OTP has expired!");
        }

        // Verify current password
        if (!encoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Error: Current password is incorrect!");
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
                .orElseThrow(() -> new RuntimeException("Error: User not found with email: " + request.getEmail()));

        // Generate OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        user.setOtpCode(otp);
        user.setOtpExpiration(java.time.LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // Send Email
        emailService.sendSimpleMessage(
                user.getEmail(),
                "Password Reset OTP",
                "Your OTP for password reset is: " + otp + "\nThis OTP is valid for 5 minutes.");

        return new MessageResponse("OTP sent to your email!");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: User not found with email: " + request.getEmail()));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.getOtp())) {
            throw new RuntimeException("Error: Invalid OTP!");
        }

        if (user.getOtpExpiration().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Error: OTP has expired!");
        }

        // Update Password
        user.setPassword(encoder.encode(request.getNewPassword()));
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        return new MessageResponse("Password reset successfully!");
    }
}
