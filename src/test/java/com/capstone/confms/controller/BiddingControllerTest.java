package com.capstone.confms.controller;

import com.capstone.confms.dto.BiddingDTO;
import com.capstone.confms.service.BiddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BiddingControllerTest {

    @Mock
    private BiddingService biddingService;

    private BiddingController biddingController;

    @BeforeEach
    void setUp() {
        biddingController = new BiddingController(biddingService);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(biddingController);
    }

    @Test
    void submitOrUpdateBidShouldReturnOk() {
        BiddingDTO dto = new BiddingDTO();
        var response = mock(com.capstone.confms.dto.response.BiddingResponseDTO.class);
        when(biddingService.submitOrUpdateBid(dto)).thenReturn(response);

        var result = biddingController.submitOrUpdateBid(dto);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void getPapersForBiddingShouldReturnOk() {
        var payload = List.of(mock(com.capstone.confms.dto.response.PaperForBiddingDTO.class));
        when(biddingService.getPapersForBidding(1, 2)).thenReturn(payload);

        var result = biddingController.getPapersForBidding(1, 2);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void getBidsByReviewerAndConferenceShouldReturnOk() {
        var payload = List.of(mock(com.capstone.confms.dto.response.BiddingResponseDTO.class));
        when(biddingService.getBidsByReviewerAndConference(1, 2)).thenReturn(payload);

        var result = biddingController.getBidsByReviewerAndConference(1, 2);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void getBidsByPaperShouldReturnOk() {
        var payload = List.of(mock(com.capstone.confms.dto.response.BiddingResponseDTO.class));
        when(biddingService.getBidsByPaper(10)).thenReturn(payload);

        var result = biddingController.getBidsByPaper(10);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void getBidsSummaryShouldReturnOk() {
        var payload = mock(com.capstone.confms.dto.response.BidsSummaryDTO.class);
        when(biddingService.getBidsSummary(1, 2)).thenReturn(payload);

        var result = biddingController.getBidsSummary(1, 2);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void deleteBidShouldReturnNoContent() {
        doNothing().when(biddingService).deleteBid(3);

        var result = biddingController.deleteBid(3);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
