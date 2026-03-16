package com.capstone.confms.controller;

import com.capstone.confms.service.ConferenceUserTrackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ConferenceUserTrackControllerTest {

    @Mock
    private ConferenceUserTrackService conferenceUserTrackService;

    @InjectMocks
    private ConferenceUserTrackController conferenceUserTrackController;

    @Test
    void shouldCreateController() {
        assertNotNull(conferenceUserTrackController);
    }
}

