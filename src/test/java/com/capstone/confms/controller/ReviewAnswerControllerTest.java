package com.capstone.confms.controller;

import com.capstone.confms.service.ReviewAnswerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ReviewAnswerControllerTest {

    @Mock
    private ReviewAnswerService reviewAnswerService;

    @InjectMocks
    private ReviewAnswerController reviewAnswerController;

    @Test
    void shouldCreateController() {
        assertNotNull(reviewAnswerController);
    }
}
