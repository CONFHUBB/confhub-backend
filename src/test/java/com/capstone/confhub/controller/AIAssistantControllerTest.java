package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.CheckTrackFitRequest;
import com.capstone.confhub.dto.request.CheckWritingRequest;
import com.capstone.confhub.dto.request.SuggestKeywordsRequest;
import com.capstone.confhub.dto.response.PaperSummaryResponse;
import com.capstone.confhub.dto.response.StrengthWeaknessResponse;
import com.capstone.confhub.dto.response.SuggestKeywordsResponse;
import com.capstone.confhub.dto.response.TrackFitResponse;
import com.capstone.confhub.dto.response.WritingCheckResponse;
import com.capstone.confhub.service.impl.AIAssistantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIAssistantControllerTest {

    @Mock
    private AIAssistantService assistantService;

    @InjectMocks
    private AIAssistantController aiAssistantController;

    @BeforeEach
    void setUp() {
        // Setup complete
    }

    @Test
    void shouldCreateController() {
        assertNotNull(aiAssistantController);
    }

    @Test
    void suggestKeywordsShouldReturnOkResponse() {
        SuggestKeywordsResponse response = mock(SuggestKeywordsResponse.class);
        when(assistantService.suggestKeywords(any())).thenReturn(response);

        var result = aiAssistantController.suggestKeywords(mock(SuggestKeywordsRequest.class));
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void checkTrackFitShouldReturnOkResponse() {
        TrackFitResponse response = mock(TrackFitResponse.class);
        when(assistantService.checkTrackFit(any())).thenReturn(response);

        var result = aiAssistantController.checkTrackFit(mock(CheckTrackFitRequest.class));
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void checkWritingShouldReturnOkResponse() {
        WritingCheckResponse response = mock(WritingCheckResponse.class);
        when(assistantService.checkAcademicWriting(any())).thenReturn(response);

        var result = aiAssistantController.checkWriting(mock(CheckWritingRequest.class));
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void summarizePaperShouldReturnOkResponse() {
        PaperSummaryResponse response = mock(PaperSummaryResponse.class);
        when(assistantService.summarizePaper(anyInt())).thenReturn(response);

        var result = aiAssistantController.summarizePaper(1);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void analyzeStrengthsWeaknessesShouldReturnOkResponse() {
        StrengthWeaknessResponse response = mock(StrengthWeaknessResponse.class);
        when(assistantService.analyzeStrengthsWeaknesses(anyInt())).thenReturn(response);

        var result = aiAssistantController.analyzeStrengthsWeaknesses(1);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void suggestKeywordsShouldCallService() {
        SuggestKeywordsResponse response = mock(SuggestKeywordsResponse.class);
        when(assistantService.suggestKeywords(any())).thenReturn(response);
        SuggestKeywordsRequest req = mock(SuggestKeywordsRequest.class);

        aiAssistantController.suggestKeywords(req);
        verify(assistantService, times(1)).suggestKeywords(req);
    }

    @Test
    void checkTrackFitShouldCallService() {
        TrackFitResponse response = mock(TrackFitResponse.class);
        when(assistantService.checkTrackFit(any())).thenReturn(response);
        CheckTrackFitRequest req = mock(CheckTrackFitRequest.class);

        aiAssistantController.checkTrackFit(req);
        verify(assistantService, times(1)).checkTrackFit(req);
    }

    @Test
    void checkWritingShouldCallService() {
        WritingCheckResponse response = mock(WritingCheckResponse.class);
        when(assistantService.checkAcademicWriting(any())).thenReturn(response);
        CheckWritingRequest req = mock(CheckWritingRequest.class);

        aiAssistantController.checkWriting(req);
        verify(assistantService, times(1)).checkAcademicWriting(req);
    }

    @Test
    void summarizePaperShouldCallServiceWithCorrectId() {
        PaperSummaryResponse response = mock(PaperSummaryResponse.class);
        when(assistantService.summarizePaper(42)).thenReturn(response);

        aiAssistantController.summarizePaper(42);
        verify(assistantService, times(1)).summarizePaper(42);
    }

    @Test
    void analyzeStrengthsWeaknessesShouldCallServiceWithCorrectId() {
        StrengthWeaknessResponse response = mock(StrengthWeaknessResponse.class);
        when(assistantService.analyzeStrengthsWeaknesses(99)).thenReturn(response);

        aiAssistantController.analyzeStrengthsWeaknesses(99);
        verify(assistantService, times(1)).analyzeStrengthsWeaknesses(99);
    }

    @Test
    void allEndpointsShouldReturnOkStatus() {
        when(assistantService.suggestKeywords(any())).thenReturn(mock(SuggestKeywordsResponse.class));
        when(assistantService.checkTrackFit(any())).thenReturn(mock(TrackFitResponse.class));
        when(assistantService.checkAcademicWriting(any())).thenReturn(mock(WritingCheckResponse.class));
        when(assistantService.summarizePaper(anyInt())).thenReturn(mock(PaperSummaryResponse.class));
        when(assistantService.analyzeStrengthsWeaknesses(anyInt())).thenReturn(mock(StrengthWeaknessResponse.class));

        assertEquals(HttpStatus.OK, aiAssistantController.suggestKeywords(mock(SuggestKeywordsRequest.class)).getStatusCode());
        assertEquals(HttpStatus.OK, aiAssistantController.checkTrackFit(mock(CheckTrackFitRequest.class)).getStatusCode());
        assertEquals(HttpStatus.OK, aiAssistantController.checkWriting(mock(CheckWritingRequest.class)).getStatusCode());
        assertEquals(HttpStatus.OK, aiAssistantController.summarizePaper(1).getStatusCode());
        assertEquals(HttpStatus.OK, aiAssistantController.analyzeStrengthsWeaknesses(1).getStatusCode());
    }

    @Test
    void suggestKeywordsResponseBodyNotNull() {
        SuggestKeywordsResponse response = mock(SuggestKeywordsResponse.class);
        when(assistantService.suggestKeywords(any())).thenReturn(response);

        var result = aiAssistantController.suggestKeywords(mock(SuggestKeywordsRequest.class));
        assertNotNull(result.getBody());
    }

    @Test
    void trackFitResponseBodyNotNull() {
        TrackFitResponse response = mock(TrackFitResponse.class);
        when(assistantService.checkTrackFit(any())).thenReturn(response);

        var result = aiAssistantController.checkTrackFit(mock(CheckTrackFitRequest.class));
        assertNotNull(result.getBody());
    }

    @Test
    void writingCheckResponseBodyNotNull() {
        WritingCheckResponse response = mock(WritingCheckResponse.class);
        when(assistantService.checkAcademicWriting(any())).thenReturn(response);

        var result = aiAssistantController.checkWriting(mock(CheckWritingRequest.class));
        assertNotNull(result.getBody());
    }

    @Test
    void summarizePaperResponseBodyNotNull() {
        PaperSummaryResponse response = mock(PaperSummaryResponse.class);
        when(assistantService.summarizePaper(anyInt())).thenReturn(response);

        var result = aiAssistantController.summarizePaper(1);
        assertNotNull(result.getBody());
    }

    @Test
    void analyzeStrengthsResponseBodyNotNull() {
        StrengthWeaknessResponse response = mock(StrengthWeaknessResponse.class);
        when(assistantService.analyzeStrengthsWeaknesses(anyInt())).thenReturn(response);

        var result = aiAssistantController.analyzeStrengthsWeaknesses(1);
        assertNotNull(result.getBody());
    }

    @Test
    void multipleCallsToSuggestKeywordsShouldReturnDifferentResults() {
        SuggestKeywordsResponse resp1 = mock(SuggestKeywordsResponse.class);
        SuggestKeywordsResponse resp2 = mock(SuggestKeywordsResponse.class);

        when(assistantService.suggestKeywords(any()))
                .thenReturn(resp1)
                .thenReturn(resp2);

        var result1 = aiAssistantController.suggestKeywords(mock(SuggestKeywordsRequest.class));
        var result2 = aiAssistantController.suggestKeywords(mock(SuggestKeywordsRequest.class));

        assertEquals(resp1, result1.getBody());
        assertEquals(resp2, result2.getBody());
    }

    @Test
    void summarizeMultiplePapers() {
        PaperSummaryResponse resp1 = mock(PaperSummaryResponse.class);
        PaperSummaryResponse resp2 = mock(PaperSummaryResponse.class);

        when(assistantService.summarizePaper(1)).thenReturn(resp1);
        when(assistantService.summarizePaper(2)).thenReturn(resp2);

        var result1 = aiAssistantController.summarizePaper(1);
        var result2 = aiAssistantController.summarizePaper(2);

        assertEquals(resp1, result1.getBody());
        assertEquals(resp2, result2.getBody());
    }

    @Test
    void analyzeMultiplePapers() {
        StrengthWeaknessResponse resp1 = mock(StrengthWeaknessResponse.class);
        StrengthWeaknessResponse resp2 = mock(StrengthWeaknessResponse.class);

        when(assistantService.analyzeStrengthsWeaknesses(1)).thenReturn(resp1);
        when(assistantService.analyzeStrengthsWeaknesses(2)).thenReturn(resp2);

        var result1 = aiAssistantController.analyzeStrengthsWeaknesses(1);
        var result2 = aiAssistantController.analyzeStrengthsWeaknesses(2);

        assertEquals(resp1, result1.getBody());
        assertEquals(resp2, result2.getBody());
    }

    @Test
    void checkTrackFitWithDifferentRequests() {
        TrackFitResponse response = mock(TrackFitResponse.class);
        when(assistantService.checkTrackFit(any())).thenReturn(response);

        CheckTrackFitRequest req1 = mock(CheckTrackFitRequest.class);
        CheckTrackFitRequest req2 = mock(CheckTrackFitRequest.class);

        var result1 = aiAssistantController.checkTrackFit(req1);
        var result2 = aiAssistantController.checkTrackFit(req2);

        assertEquals(HttpStatus.OK, result1.getStatusCode());
        assertEquals(HttpStatus.OK, result2.getStatusCode());
    }

    @Test
    void checkWritingWithDifferentRequests() {
        WritingCheckResponse response = mock(WritingCheckResponse.class);
        when(assistantService.checkAcademicWriting(any())).thenReturn(response);

        CheckWritingRequest req1 = mock(CheckWritingRequest.class);
        CheckWritingRequest req2 = mock(CheckWritingRequest.class);

        var result1 = aiAssistantController.checkWriting(req1);
        var result2 = aiAssistantController.checkWriting(req2);

        assertEquals(HttpStatus.OK, result1.getStatusCode());
        assertEquals(HttpStatus.OK, result2.getStatusCode());
    }
}

