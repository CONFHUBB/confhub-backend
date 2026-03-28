package com.capstone.confhub.controller;

import com.capstone.confhub.service.ReviewCommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ReviewCommentControllerTest {

    @Mock
    private ReviewCommentService reviewCommentService;

    @InjectMocks
    private ReviewCommentController reviewCommentController;

    @Test
    void shouldCreateController() {
        assertNotNull(reviewCommentController);
    }
}
