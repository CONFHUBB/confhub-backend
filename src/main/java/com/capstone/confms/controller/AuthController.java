package com.capstone.confms.controller;

import com.capstone.confms.entity.Role;
import com.capstone.confms.entity.User;
import com.capstone.confms.entity.UserRole;
import com.capstone.confms.dto.request.ChangePasswordRequest;
import com.capstone.confms.dto.request.ForgotPasswordRequest;
import com.capstone.confms.dto.request.LoginRequest;
import com.capstone.confms.dto.request.ResetPasswordRequest;
import com.capstone.confms.dto.request.SignupRequest;
import com.capstone.confms.dto.response.JwtResponse;
import com.capstone.confms.dto.response.MessageResponse;
import com.capstone.confms.repository.RoleRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.repository.UserRoleRepository;
import com.capstone.confms.security.jwt.JwtTokenProvider;
import com.capstone.confms.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getFullName(),
                roles));
    }

    @PostMapping("/signup")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
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

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @Autowired
    private com.capstone.confms.service.EmailService emailService;

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp() {
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

        return ResponseEntity.ok(new MessageResponse("OTP sent to your email!"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        // Get current user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: User not found."));

        // Verify OTP
        if (user.getOtpCode() == null || !user.getOtpCode().equals(changePasswordRequest.getOtp())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid OTP!"));
        }

        if (user.getOtpExpiration().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: OTP has expired!"));
        }

        // Verify current password
        if (!encoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Current password is incorrect!"));
        }

        // Update password
        user.setPassword(encoder.encode(changePasswordRequest.getNewPassword()));
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Password changed successfully!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
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

        return ResponseEntity.ok(new MessageResponse("OTP sent to your email!"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Error: User not found with email: " + request.getEmail()));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.getOtp())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid OTP!"));
        }

        if (user.getOtpExpiration().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: OTP has expired!"));
        }

        // Update Password
        user.setPassword(encoder.encode(request.getNewPassword()));
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Password reset successfully!"));
    }
}
