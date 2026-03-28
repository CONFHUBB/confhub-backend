package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.UserProfileRequest;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.entity.UserProfile;
import com.capstone.confhub.repository.UserProfileRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.utils.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private User user;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);

        profile = new UserProfile();
        profile.setId(10);
        profile.setUser(user);
        profile.setUserType(UserType.STUDENT);
        profile.setJobTitle("Professor");
        profile.setInstitution("Capstone University");
        profile.setBiography("Researcher");
    }

    @Test
    void shouldCreateService() {
        assertNotNull(userProfileService);
    }

    @Test
    void getProfileByUserIdShouldReturnResponse() {
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));

        var result = userProfileService.getProfileByUserId(1);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(1, result.getUserId());
        assertEquals("STUDENT", result.getUserType());
    }

    @Test
    void createOrUpdateProfileShouldCreateProfileWhenMissing() {
        UserProfileRequest request = UserProfileRequest.builder()
                .userType(UserType.STUDENT.toString())
                .jobTitle("Professor")
                .institution("Capstone University")
                .biography("Researcher")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile saved = invocation.getArgument(0);
            saved.setId(10);
            return saved;
        });

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("Professor", result.getJobTitle());
        assertEquals("STUDENT", result.getUserType());
    }

    @Test
    void createOrUpdateProfileShouldUpdateExistingProfile() {
        UserProfileRequest request = UserProfileRequest.builder()
                .jobTitle("Dean")
                .institution("Updated University")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertNotNull(result);
        assertEquals("Dean", result.getJobTitle());
        assertEquals("Updated University", result.getInstitution());
    }
}




