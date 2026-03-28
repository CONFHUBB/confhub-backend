package com.capstone.confhub.controller;

import com.capstone.confhub.service.ReviewerInterestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ReviewerInterestControllerTest {

    @Mock
    private ReviewerInterestService reviewerInterestService;

    @InjectMocks
    private ReviewerInterestController reviewerInterestController;

    @Test
    void shouldCreateController() {
        assertNotNull(reviewerInterestController);
    }
}
