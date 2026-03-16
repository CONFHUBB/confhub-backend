package com.capstone.confms.controller;

import com.capstone.confms.service.PaperConflictService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class PaperConflictControllerTest {

    @Mock
    private PaperConflictService paperConflictService;

    @InjectMocks
    private PaperConflictController paperConflictController;

    @Test
    void shouldCreateController() {
        assertNotNull(paperConflictController);
    }
}

