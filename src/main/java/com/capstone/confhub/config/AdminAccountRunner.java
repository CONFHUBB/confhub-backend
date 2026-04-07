package com.capstone.confhub.config;

import com.capstone.confhub.entity.Role;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.entity.UserProfile;
import com.capstone.confhub.entity.UserRole;
import com.capstone.confhub.repository.RoleRepository;
import com.capstone.confhub.repository.UserProfileRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = "admin@confhub.com";

        // Ensure ADMIN role exists
        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ADMIN");
            role.setDescription("Administrator role with full access");
            return roleRepository.save(role);
        });

        // Ensure AUTHOR role exists as it might be required by other workflows
        roleRepository.findByName("AUTHOR").orElseGet(() -> {
            Role role = new Role();
            role.setName("AUTHOR");
            role.setDescription("Regular author role");
            return roleRepository.save(role);
        });

        User adminUser = userRepository.findByEmail(adminEmail).orElse(null);
        if (adminUser != null) {
            log.info("Admin account already exists. Forcing password reset to default...");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setIsActive(true);
            userRepository.save(adminUser);

            boolean hasAdminRole = userRoleRepository.findByUserId(adminUser.getId())
                    .stream()
                    .anyMatch(ur -> "ADMIN".equals(ur.getRole().getName()));

            if (!hasAdminRole) {
                UserRole userRole = new UserRole();
                userRole.setUser(adminUser);
                userRole.setRole(adminRole);
                userRoleRepository.save(userRole);
            }
            return;
        }

        log.info("Creating default admin account...");
        
        adminUser = new User();
        adminUser.setFirstName("Super");
        adminUser.setLastName("Admin");
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode("admin123")); // Default password
        adminUser.setIsActive(true);
        userRepository.save(adminUser);

        UserRole userRole = new UserRole();
        userRole.setUser(adminUser);
        userRole.setRole(adminRole);
        userRoleRepository.save(userRole);

        UserProfile profile = new UserProfile();
        profile.setUser(adminUser);
        userProfileRepository.save(profile);

        log.info("Default admin account created successfully.");
        log.info("Email: {}", adminEmail);
        log.info("Password: {}", "admin123");
    }
}
