package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.CheckTrackFitRequest;
import com.capstone.confhub.dto.request.CheckWritingRequest;
import com.capstone.confhub.dto.request.SuggestKeywordsRequest;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.integration.GeminiApiClient;
import com.capstone.confhub.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIAssistantServiceTest {

    @Mock private GeminiApiClient geminiClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private PaperRepository paperRepo;
    @Mock private ConferenceTrackRepository trackRepo;
    @Mock private SubjectAreaRepository subjectAreaRepo;
    @Mock private ReviewRepository reviewRepo;
    @Mock private ReviewAnswerRepository reviewAnswerRepo;
    @Mock private PaperFileRepository paperFileRepo;

    @InjectMocks
    private AIAssistantService aiAssistantService;

    private Conference conference;
    private ConferenceTrack track;
    private Paper paper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiAssistantService, "objectMapper", new ObjectMapper());

        conference = new Conference();
        conference.setId(1);
        conference.setName("ConfHub 2026");

        track = new ConferenceTrack();
        track.setId(10);
        track.setName("AI");
        track.setDescription("Artificial intelligence");
        track.setConference(conference);

        paper = new Paper();
        paper.setId(100);
        paper.setTitle("Transformer Study");
        paper.setAbstractField("This paper explores transformer models.");
        paper.setKeywordsJson("nlp, transformer");
    }

    @Test
    void suggestKeywordsShouldParseValidJson() {
        SuggestKeywordsRequest req = new SuggestKeywordsRequest();
        req.setAbstractText("text");

        when(geminiClient.generateContent(anyString(), anyList()))
                .thenReturn("{\"keywords\":[\"nlp\",\"transformer\"]}");

        var result = aiAssistantService.suggestKeywords(req);

        assertEquals(2, result.getKeywords().size());
        assertTrue(result.getKeywords().contains("nlp"));
    }

    @Test
    void suggestKeywordsShouldReturnEmptyListOnInvalidJson() {
        SuggestKeywordsRequest req = new SuggestKeywordsRequest();
        req.setAbstractText("text");

        when(geminiClient.generateContent(anyString(), anyList()))
                .thenReturn("not-json-response");

        var result = aiAssistantService.suggestKeywords(req);

        assertNotNull(result.getKeywords());
        assertEquals(0, result.getKeywords().size());
    }

    @Test
    void checkTrackFitShouldThrowWhenTrackNotFound() {
        CheckTrackFitRequest req = new CheckTrackFitRequest();
        req.setTrackId(999);
        req.setAbstractText("abstract");
        req.setKeywords("k1");

        when(trackRepo.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> aiAssistantService.checkTrackFit(req));
    }

    @Test
    void checkTrackFitShouldReturnParsedResponse() {
        CheckTrackFitRequest req = new CheckTrackFitRequest();
        req.setTrackId(10);
        req.setAbstractText("abstract");
        req.setKeywords("k1");

        SubjectArea sa = new SubjectArea();
        sa.setName("ML");

        when(trackRepo.findById(10)).thenReturn(Optional.of(track));
        when(subjectAreaRepo.findByTrackId(10)).thenReturn(List.of(sa));
        when(trackRepo.findByConferenceId(1)).thenReturn(List.of(track));
        when(geminiClient.generateContent(anyString(), anyList()))
                .thenReturn("{\"matchScore\":87,\"explanation\":\"good\",\"suggestedTrack\":null}");

        var result = aiAssistantService.checkTrackFit(req);

        assertEquals(87, result.getMatchScore());
        assertEquals("good", result.getExplanation());
    }

    @Test
    void checkTrackFitShouldFallbackOnInvalidJson() {
        CheckTrackFitRequest req = new CheckTrackFitRequest();
        req.setTrackId(10);
        req.setAbstractText("abstract");
        req.setKeywords("k1");

        when(trackRepo.findById(10)).thenReturn(Optional.of(track));
        when(subjectAreaRepo.findByTrackId(10)).thenReturn(List.of());
        when(trackRepo.findByConferenceId(1)).thenReturn(List.of(track));
        when(geminiClient.generateContent(anyString(), anyList()))
                .thenReturn("oops");

        var result = aiAssistantService.checkTrackFit(req);

        assertEquals(50, result.getMatchScore());
    }

    @Test
    void checkAcademicWritingShouldParseSuggestions() {
        CheckWritingRequest req = new CheckWritingRequest();
        req.setTitle("Bad title");
        req.setAbstractText("This are test");

        when(geminiClient.generateContent(anyString(), anyList()))
                .thenReturn("{\"suggestions\":[{\"original\":\"This are\",\"suggested\":\"This is\",\"reason\":\"grammar\",\"type\":\"GRAMMAR\"}],\"overallAssessment\":\"Need improvements\"}");

        var result = aiAssistantService.checkAcademicWriting(req);

        assertEquals(1, result.getSuggestions().size());
        assertEquals("Need improvements", result.getOverallAssessment());
        assertEquals("GRAMMAR", result.getSuggestions().get(0).getType());
    }

    @Test
    void checkAcademicWritingShouldRetryAndFailAfterThreeAttempts() {
        CheckWritingRequest req = new CheckWritingRequest();
        req.setTitle("Title");
        req.setAbstractText("Abstract");

        when(geminiClient.generateContent(anyString(), anyList()))
                .thenThrow(new RuntimeException("network"))
                .thenThrow(new RuntimeException("network"))
                .thenThrow(new RuntimeException("network"));

        assertThrows(BadRequestException.class, () -> aiAssistantService.checkAcademicWriting(req));
        verify(geminiClient, times(3)).generateContent(anyString(), anyList());
    }

    @Test
    void summarizePaperShouldThrowWhenPaperMissing() {
        when(paperRepo.findById(100)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> aiAssistantService.summarizePaper(100));
    }

    @Test
    void summarizePaperShouldParseJsonResponse() {
        when(paperRepo.findById(100)).thenReturn(Optional.of(paper));
        when(paperFileRepo.findByPaper_Id(100)).thenReturn(List.of());
        when(geminiClient.generateContent(anyString(), anyList()))
                .thenReturn("{\"summary\":\"Short summary\",\"keyContributions\":[\"c1\",\"c2\"],\"methodology\":\"exp\"}");

        var result = aiAssistantService.summarizePaper(100);

        assertEquals("Short summary", result.getSummary());
        assertEquals(2, result.getKeyContributions().size());
        assertEquals("exp", result.getMethodology());
    }

    @Test
    void summarizePaperShouldFallbackOnInvalidJson() {
        when(paperRepo.findById(100)).thenReturn(Optional.of(paper));
        when(paperFileRepo.findByPaper_Id(100)).thenReturn(List.of());
        when(geminiClient.generateContent(anyString(), anyList())).thenReturn("not-json");

        var result = aiAssistantService.summarizePaper(100);

        assertEquals("Could not generate summary", result.getSummary());
        assertEquals(0, result.getKeyContributions().size());
    }

    @Test
    void analyzeStrengthsWeaknessesShouldParseJson() {
        when(paperRepo.findById(100)).thenReturn(Optional.of(paper));
        when(paperFileRepo.findByPaper_Id(100)).thenReturn(List.of());
        when(geminiClient.generateContent(anyString(), anyList()))
                .thenReturn("{\"strengths\":[\"s1\",\"s2\"],\"weaknesses\":[\"w1\"]}");

        var result = aiAssistantService.analyzeStrengthsWeaknesses(100);

        assertEquals(2, result.getStrengths().size());
        assertEquals(1, result.getWeaknesses().size());
    }

    @Test
    void analyzeStrengthsWeaknessesShouldFallbackOnInvalidJson() {
        when(paperRepo.findById(100)).thenReturn(Optional.of(paper));
        when(paperFileRepo.findByPaper_Id(100)).thenReturn(List.of());
        when(geminiClient.generateContent(anyString(), anyList())).thenReturn("oops");

        var result = aiAssistantService.analyzeStrengthsWeaknesses(100);

        assertFalse(result.getStrengths().isEmpty());
        assertFalse(result.getWeaknesses().isEmpty());
    }

    @Test
    void analyzeReviewConsensusShouldThrowWhenNoReviews() {
        when(paperRepo.findById(100)).thenReturn(Optional.of(paper));
        when(reviewRepo.findByPaper_Id(100)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> aiAssistantService.analyzeReviewConsensus(100));
    }

    @Test
    void analyzeReviewConsensusShouldParseResponse() {
        Review review = new Review();
        review.setId(1);
        review.setTotalScore(BigDecimal.valueOf(8.5));

        ReviewQuestion question = new ReviewQuestion();
        question.setText("Is method clear?");

        ReviewAnswer answer = new ReviewAnswer();
        answer.setQuestion(question);
        answer.setAnswerValue("Yes");

        when(paperRepo.findById(100)).thenReturn(Optional.of(paper));
        when(reviewRepo.findByPaper_Id(100)).thenReturn(List.of(review));
        when(reviewAnswerRepo.findByReview_Id(1)).thenReturn(List.of(answer));
        when(geminiClient.generateContent(anyString(), anyList()))
                .thenReturn("{\"agreementScore\":90,\"recommendation\":\"Accept\",\"agreements\":[\"clear\"],\"disagreements\":[]}");

        var result = aiAssistantService.analyzeReviewConsensus(100);

        assertEquals(90, result.getAgreementScore());
        assertEquals("Accept", result.getRecommendation());
        assertEquals(1, result.getAgreements().size());
    }

    @Test
    void analyzeReviewConsensusShouldFallbackOnInvalidJson() {
        Review review = new Review();
        review.setId(1);
        review.setTotalScore(BigDecimal.valueOf(8.5));

        when(paperRepo.findById(100)).thenReturn(Optional.of(paper));
        when(reviewRepo.findByPaper_Id(100)).thenReturn(List.of(review));
        when(reviewAnswerRepo.findByReview_Id(1)).thenReturn(List.of());
        when(geminiClient.generateContent(anyString(), anyList())).thenReturn("invalid");

        var result = aiAssistantService.analyzeReviewConsensus(100);

        assertEquals(0, result.getAgreementScore());
        assertEquals("Could not analyze consensus", result.getRecommendation());
    }
}


