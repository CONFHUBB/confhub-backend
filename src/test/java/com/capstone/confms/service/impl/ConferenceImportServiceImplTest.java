package com.capstone.confms.service.impl;

import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.SubjectAreaRepository;
import com.capstone.confms.repository.TrackReviewSettingRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.ConferenceActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ConferenceImportServiceImplTest {

    @Mock
    private ConferenceRepository conferenceRepository;
    @Mock
    private ConferenceTrackRepository conferenceTrackRepository;
    @Mock
    private TrackReviewSettingRepository trackReviewSettingRepository;
    @Mock
    private SubjectAreaRepository subjectAreaRepository;
    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ConferenceActivityService conferenceActivityService;

    @InjectMocks
    private ConferenceImportServiceImpl conferenceImportService;

    @Test
    void shouldCreateService() {
        assertNotNull(conferenceImportService);
    }
}
