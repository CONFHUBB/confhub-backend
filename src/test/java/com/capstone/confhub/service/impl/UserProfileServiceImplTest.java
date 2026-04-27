package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.UserProfileRequest;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.entity.UserProfile;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.UserProfileRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.utils.enums.UserStatus;
import com.capstone.confhub.utils.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

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
        user.setStatus(UserStatus.AVAILABLE);

        profile = new UserProfile();
        profile.setId(10);
        profile.setUser(user);
        profile.setUserType(UserType.STUDENT);
        profile.setJobTitle("Professor");
        profile.setDepartment("Computer Science");
        profile.setInstitution("Capstone University");
        profile.setInstitutionCountry("VN");
        profile.setInstitutionUrl("https://capstone.edu");
        profile.setSecondaryInstitution("Secondary U");
        profile.setSecondaryCountry("US");
        profile.setPhoneOffice("111");
        profile.setPhoneMobile("222");
        profile.setAvatarUrl("https://img/avatar.png");
        profile.setBiography("Researcher");
        profile.setWebsiteUrl("https://site.me");
        profile.setDblpId("dblp-1");
        profile.setGoogleScholarLink("https://scholar.google/abc");
        profile.setOrcid("0000-0000-0000-0001");
        profile.setSemanticScholarId("ss-1");
        profile.setCreatedAt(LocalDateTime.now().minusDays(5));
        profile.setUpdatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void shouldCreateService() {
        assertNotNull(userProfileService);
    }

    @Test
    void getProfileByUserIdShouldReturnFullMappedResponse() {
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));

        var result = userProfileService.getProfileByUserId(1);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(1, result.getUserId());
        assertEquals("STUDENT", result.getUserType());
        assertEquals("Professor", result.getJobTitle());
        assertEquals("Computer Science", result.getDepartment());
        assertEquals("Capstone University", result.getInstitution());
        assertEquals("VN", result.getInstitutionCountry());
        assertEquals("https://capstone.edu", result.getInstitutionUrl());
        assertEquals("Secondary U", result.getSecondaryInstitution());
        assertEquals("US", result.getSecondaryCountry());
        assertEquals("111", result.getPhoneOffice());
        assertEquals("222", result.getPhoneMobile());
        assertEquals("https://img/avatar.png", result.getAvatarUrl());
        assertEquals("Researcher", result.getBiography());
        assertEquals("https://site.me", result.getWebsiteUrl());
        assertEquals("dblp-1", result.getDblpId());
        assertEquals("https://scholar.google/abc", result.getGoogleScholarLink());
        assertEquals("0000-0000-0000-0001", result.getOrcid());
        assertEquals("ss-1", result.getSemanticScholarId());
    }

    @Test
    void getProfileByUserIdShouldThrowWhenProfileMissing() {
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userProfileService.getProfileByUserId(1));
    }

    @Test
    void getProfileByUserIdShouldReturnNullUserTypeWhenEntityTypeNull() {
        profile.setUserType(null);
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));

        var result = userProfileService.getProfileByUserId(1);

        assertNull(result.getUserType());
    }

    @Test
    void getProfileByUserIdShouldMapCreatedAndUpdatedAt() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(10);
        LocalDateTime updatedAt = LocalDateTime.now().minusHours(3);
        profile.setCreatedAt(createdAt);
        profile.setUpdatedAt(updatedAt);
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));

        var result = userProfileService.getProfileByUserId(1);

        assertEquals(createdAt, result.getCreatedAt());
        assertEquals(updatedAt, result.getUpdatedAt());
    }

    @Test
    void createOrUpdateProfileShouldThrowWhenUserMissing() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProfileService.createOrUpdateProfile(1, UserProfileRequest.builder().build()));
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

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertEquals(user, captor.getValue().getUser());
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

    @Test
    void createOrUpdateProfileShouldKeepExistingUserReferenceWhenUpdating() {
        UserProfileRequest request = UserProfileRequest.builder().jobTitle("Dean").build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userProfileService.createOrUpdateProfile(1, request);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertEquals(user, captor.getValue().getUser());
    }

    @Test
    void createOrUpdateProfileShouldNormalizeUserTypeToUppercase() {
        UserProfileRequest request = UserProfileRequest.builder().userType("student").build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals("STUDENT", result.getUserType());
    }

    @Test
    void createOrUpdateProfileShouldIgnoreBlankUserType() {
        profile.setUserType(UserType.STUDENT);
        UserProfileRequest request = UserProfileRequest.builder().userType("  ").build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals("STUDENT", result.getUserType());
    }

    @Test
    void createOrUpdateProfileShouldThrowWhenUserTypeContainsWhitespaceAroundValue() {
        UserProfileRequest request = UserProfileRequest.builder().userType(" student ").build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class, () -> userProfileService.createOrUpdateProfile(1, request));
    }

    @Test
    void createOrUpdateProfileShouldThrowWhenUserTypeInvalid() {
        UserProfileRequest request = UserProfileRequest.builder().userType("not-a-type").build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));

        assertThrows(IllegalArgumentException.class, () -> userProfileService.createOrUpdateProfile(1, request));
    }

    @Test
    void createOrUpdateProfileShouldPreserveExistingValuesWhenRequestFieldsNull() {
        UserProfileRequest request = UserProfileRequest.builder()
                .jobTitle(null)
                .department(null)
                .institution(null)
                .biography(null)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals("Professor", result.getJobTitle());
        assertEquals("Computer Science", result.getDepartment());
        assertEquals("Capstone University", result.getInstitution());
        assertEquals("Researcher", result.getBiography());
    }

    @Test
    void createOrUpdateProfileShouldAllowOverwritingFieldsWithBlankStrings() {
        UserProfileRequest request = UserProfileRequest.builder()
                .jobTitle("")
                .department("  ")
                .institution("")
                .biography(" ")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals("", result.getJobTitle());
        assertEquals("  ", result.getDepartment());
        assertEquals("", result.getInstitution());
        assertEquals(" ", result.getBiography());
    }

    @Test
    void createOrUpdateProfileShouldUpdateAllFields() {
        UserProfileRequest request = UserProfileRequest.builder()
                .userType("INDUSTRY")
                .jobTitle("Principal Engineer")
                .department("AI Lab")
                .institution("Tech Corp")
                .institutionCountry("SG")
                .institutionUrl("https://tech.example")
                .secondaryInstitution("Visiting Org")
                .secondaryCountry("JP")
                .phoneOffice("333")
                .phoneMobile("444")
                .avatarUrl("https://img/new.png")
                .biography("Updated bio")
                .websiteUrl("https://me.example")
                .dblpId("dblp-new")
                .googleScholarLink("https://scholar/new")
                .orcid("0000-0000-0000-9999")
                .semanticScholarId("ss-new")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals("INDUSTRY", result.getUserType());
        assertEquals("Principal Engineer", result.getJobTitle());
        assertEquals("AI Lab", result.getDepartment());
        assertEquals("Tech Corp", result.getInstitution());
        assertEquals("SG", result.getInstitutionCountry());
        assertEquals("https://tech.example", result.getInstitutionUrl());
        assertEquals("Visiting Org", result.getSecondaryInstitution());
        assertEquals("JP", result.getSecondaryCountry());
        assertEquals("333", result.getPhoneOffice());
        assertEquals("444", result.getPhoneMobile());
        assertEquals("https://img/new.png", result.getAvatarUrl());
        assertEquals("Updated bio", result.getBiography());
        assertEquals("https://me.example", result.getWebsiteUrl());
        assertEquals("dblp-new", result.getDblpId());
        assertEquals("https://scholar/new", result.getGoogleScholarLink());
        assertEquals("0000-0000-0000-9999", result.getOrcid());
        assertEquals("ss-new", result.getSemanticScholarId());
    }

    @Test
    void createOrUpdateProfileShouldCreateWithMostlyNullFields() {
        UserProfileRequest request = UserProfileRequest.builder().build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile saved = invocation.getArgument(0);
            saved.setId(33);
            return saved;
        });

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals(33, result.getId());
        assertNull(result.getUserType());
        assertNull(result.getJobTitle());
        assertNull(result.getDepartment());
        assertNull(result.getInstitution());
    }

    @Test
    void createOrUpdateProfileShouldCreateAndMapEveryOptionalField() {
        UserProfileRequest request = UserProfileRequest.builder()
                .userType("ACADEMIA")
                .jobTitle("Lecturer")
                .department("SE")
                .institution("Uni A")
                .institutionCountry("VN")
                .institutionUrl("https://unia.example")
                .secondaryInstitution("Uni B")
                .secondaryCountry("TH")
                .phoneOffice("100")
                .phoneMobile("200")
                .avatarUrl("https://img.example/u.png")
                .biography("bio")
                .websiteUrl("https://user.example")
                .dblpId("dblp-x")
                .googleScholarLink("https://scholar.example")
                .orcid("0000-1111-2222-3333")
                .semanticScholarId("ss-99")
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile saved = invocation.getArgument(0);
            saved.setId(55);
            return saved;
        });

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals(55, result.getId());
        assertEquals("ACADEMIA", result.getUserType());
        assertEquals("Lecturer", result.getJobTitle());
        assertEquals("SE", result.getDepartment());
        assertEquals("Uni A", result.getInstitution());
        assertEquals("VN", result.getInstitutionCountry());
        assertEquals("https://unia.example", result.getInstitutionUrl());
        assertEquals("Uni B", result.getSecondaryInstitution());
        assertEquals("TH", result.getSecondaryCountry());
        assertEquals("100", result.getPhoneOffice());
        assertEquals("200", result.getPhoneMobile());
        assertEquals("https://img.example/u.png", result.getAvatarUrl());
        assertEquals("bio", result.getBiography());
        assertEquals("https://user.example", result.getWebsiteUrl());
        assertEquals("dblp-x", result.getDblpId());
        assertEquals("https://scholar.example", result.getGoogleScholarLink());
        assertEquals("0000-1111-2222-3333", result.getOrcid());
        assertEquals("ss-99", result.getSemanticScholarId());
    }

    @Test
    void createOrUpdateProfileShouldUpdateOnlyProvidedSingleField() {
        UserProfileRequest request = UserProfileRequest.builder().websiteUrl("https://updated.example").build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals("https://updated.example", result.getWebsiteUrl());
        assertEquals("Professor", result.getJobTitle());
        assertEquals("Capstone University", result.getInstitution());
    }

    @Test
    void createOrUpdateProfileShouldAcceptUppercaseUserTypeWithoutConversionIssues() {
        UserProfileRequest request = UserProfileRequest.builder().userType("INDUSTRY").build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals("INDUSTRY", result.getUserType());
    }

    @Test
    void createOrUpdateProfileShouldReuseExistingEntityInstance() {
        UserProfileRequest request = UserProfileRequest.builder().jobTitle("Dean").build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userProfileService.createOrUpdateProfile(1, request);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertEquals(profile, captor.getValue());
    }

    @Test
    void createOrUpdateProfileShouldSetStatusUntilWhenStatusIsNotAvailable() {
        LocalDateTime future = LocalDateTime.now().plusHours(1);
        UserProfileRequest request = UserProfileRequest.builder()
                .userStatus(UserStatus.BUSY)
                .userStatusUntil(future)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals(UserStatus.BUSY, result.getUserStatus());
        assertEquals(future, result.getUserStatusUntil());
    }

    @Test
    void createOrUpdateProfileShouldClearStatusUntilWhenStatusIsAvailable() {
        user.setStatus(UserStatus.BUSY);
        user.setStatusUntil(LocalDateTime.now().plusHours(2));

        UserProfileRequest request = UserProfileRequest.builder()
                .userStatus(UserStatus.AVAILABLE)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = userProfileService.createOrUpdateProfile(1, request);

        assertEquals(UserStatus.AVAILABLE, result.getUserStatus());
        assertNull(result.getUserStatusUntil());
    }

    @Test
    void createOrUpdateProfileShouldThrowWhenStatusIsNotAvailableAndDurationMissing() {
        UserProfileRequest request = UserProfileRequest.builder()
                .userStatus(UserStatus.VACATION)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(1)).thenReturn(Optional.of(profile));

        assertThrows(com.capstone.confhub.exception.BadRequestException.class,
                () -> userProfileService.createOrUpdateProfile(1, request));
    }
}
