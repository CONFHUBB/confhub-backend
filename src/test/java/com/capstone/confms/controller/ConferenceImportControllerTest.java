package com.capstone.confms.controller;

import com.capstone.confms.service.ConferenceImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ConferenceImportControllerTest {

    @Mock
    private ConferenceImportService conferenceImportService;

    @InjectMocks
    private ConferenceImportController conferenceImportController;

    @Test
    void shouldCreateController() {
        assertNotNull(conferenceImportController);
    }
}

