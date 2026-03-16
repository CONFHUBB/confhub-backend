package com.capstone.confms.controller;

import com.capstone.confms.dto.PaperDTO;
import com.capstone.confms.service.PaperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperControllerTest {

    @Mock
    private PaperService paperService;

    private PaperController paperController;

    @BeforeEach
    void setUp() {
        paperController = new PaperController(paperService);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(paperController);
    }

    @Test
    void createPaperShouldReturnCreated() {
        PaperDTO paperDTO = new PaperDTO();
        var response = mock(com.capstone.confms.dto.response.PaperResponseDTO.class);
        when(paperService.createPaper(paperDTO)).thenReturn(response);

        var result = paperController.createPaper(paperDTO);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}
