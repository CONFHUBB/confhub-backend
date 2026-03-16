package com.capstone.confms.controller;

import com.capstone.confms.service.PaperFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class PaperFileControllerTest {

    @Mock
    private PaperFileService paperFileService;

    @InjectMocks
    private PaperFileController paperFileController;

    @Test
    void shouldCreateController() {
        assertNotNull(paperFileController);
    }
}
