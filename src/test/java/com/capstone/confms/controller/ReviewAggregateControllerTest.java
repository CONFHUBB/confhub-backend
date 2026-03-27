package com.capstone.confms.controller;

import com.capstone.confms.dto.response.ReviewAggregateDTO;
import com.capstone.confms.service.ReviewAggregateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewAggregateControllerTest {

    @Mock
    private ReviewAggregateService reviewAggregateService;

    private ReviewAggregateController reviewAggregateController;

    @BeforeEach
    void setUp() {
        reviewAggregateController = new ReviewAggregateController(reviewAggregateService);
    }

    @Test
    void getByConferenceShouldReturnAggregates() {
        ReviewAggregateDTO dto = ReviewAggregateDTO.builder()
                .paperId(10)
                .paperTitle("Paper A")
                .averageTotalScore(new BigDecimal("8.50"))
                .build();
        when(reviewAggregateService.getAggregatesByConference(3)).thenReturn(List.of(dto));

        var response = reviewAggregateController.getByConference(3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(10, response.getBody().get(0).getPaperId());
        verify(reviewAggregateService).getAggregatesByConference(3);
    }

    @Test
    void getByPaperShouldReturnAggregate() {
        ReviewAggregateDTO dto = ReviewAggregateDTO.builder()
                .paperId(15)
                .paperTitle("Paper B")
                .reviewCount(2)
                .build();
        when(reviewAggregateService.getAggregateByPaper(15)).thenReturn(dto);

        var response = reviewAggregateController.getByPaper(15);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(15, response.getBody().getPaperId());
        assertEquals(2, response.getBody().getReviewCount());
        verify(reviewAggregateService).getAggregateByPaper(15);
    }
}

