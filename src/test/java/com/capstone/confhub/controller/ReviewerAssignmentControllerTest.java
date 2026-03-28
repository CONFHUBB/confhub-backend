package com.capstone.confhub.controller;

import com.capstone.confhub.service.ReviewerAssignmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ReviewerAssignmentControllerTest {

    @Mock
    private ReviewerAssignmentService reviewerAssignmentService;

    @InjectMocks
    private ReviewerAssignmentController reviewerAssignmentController;

    @Test
    void shouldCreateController() {
        assertNotNull(reviewerAssignmentController);
    }
}
