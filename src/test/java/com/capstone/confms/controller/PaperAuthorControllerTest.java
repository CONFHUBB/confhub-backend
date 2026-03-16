package com.capstone.confms.controller;

import com.capstone.confms.service.PaperAuthorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class PaperAuthorControllerTest {

    @Mock
    private PaperAuthorService paperAuthorService;

    @InjectMocks
    private PaperAuthorController paperAuthorController;

    @Test
    void shouldCreateController() {
        assertNotNull(paperAuthorController);
    }
}

