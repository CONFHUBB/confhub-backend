package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.CheckTrackFitRequest;
import com.capstone.confhub.dto.request.CheckWritingRequest;
import com.capstone.confhub.dto.request.SuggestKeywordsRequest;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.integration.GeminiApiClient;
import com.capstone.confhub.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Assistant Service — provides 7 AI-powered features:
 * Author: Keyword Suggestion, Track Fit, Writing Check
 * Reviewer: Paper Summary, Strength & Weakness
 * Chair: Review Consensus
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIAssistantService {

    private final GeminiApiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final PaperRepository paperRepo;
    private final ConferenceTrackRepository trackRepo;
    private final SubjectAreaRepository subjectAreaRepo;
    private final ReviewRepository reviewRepo;
    private final ReviewAnswerRepository reviewAnswerRepo;
    private final PaperFileRepository paperFileRepo;

    // ═══════════════════════════════════════════════════════
    //  FEATURE 1: AI Keyword Extractor
    // ═══════════════════════════════════════════════════════

    public SuggestKeywordsResponse suggestKeywords(SuggestKeywordsRequest request) {
        String systemPrompt = """
                You are an academic keyword extraction specialist for scientific conferences.
                Given a paper abstract, extract 5-8 highly relevant academic keywords.
                These keywords should be suitable for conference paper indexing and reviewer matching.
                Respond with ONLY valid JSON, no other text: {"keywords": ["keyword1", "keyword2", ...]}
                """;

        String userMsg = "Extract academic keywords from this abstract:\n\n" + request.getAbstractText();
        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", userMsg));

        String aiReply = geminiClient.generateContent(systemPrompt, messages);
        log.info("[AIAssistant] Keywords raw response: {}", aiReply);
        return parseKeywordsResponse(aiReply);
    }

    private SuggestKeywordsResponse parseKeywordsResponse(String aiReply) {
        try {
            String json = extractJson(aiReply);
            JsonNode root = objectMapper.readTree(json);
            List<String> keywords = new ArrayList<>();
            root.path("keywords").forEach(n -> keywords.add(n.asText()));
            return SuggestKeywordsResponse.builder().keywords(keywords).build();
        } catch (Exception e) {
            log.error("[AIAssistant] Failed to parse keywords response: {}", e.getMessage());
            return SuggestKeywordsResponse.builder().keywords(List.of()).build();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  FEATURE 2: AI Track Matcher
    // ═══════════════════════════════════════════════════════

    public TrackFitResponse checkTrackFit(CheckTrackFitRequest request) {
        ConferenceTrack track = trackRepo.findById(request.getTrackId())
                .orElseThrow(() -> new ResourceNotFoundException("Track not found"));

        List<SubjectArea> subjectAreas = subjectAreaRepo.findByTrackId(track.getId());
        String saNames = subjectAreas.stream().map(SubjectArea::getName).collect(Collectors.joining(", "));

        // Also get other tracks in same conference for suggesting better match
        List<ConferenceTrack> allTracks = trackRepo.findByConferenceId(track.getConference().getId());
        StringBuilder otherTracksInfo = new StringBuilder();
        for (ConferenceTrack t : allTracks) {
            if (!t.getId().equals(track.getId())) {
                List<SubjectArea> tSa = subjectAreaRepo.findByTrackId(t.getId());
                String tSaNames = tSa.stream().map(SubjectArea::getName).collect(Collectors.joining(", "));
                otherTracksInfo.append(String.format("- %s: %s (Subject areas: %s)\n", t.getName(), t.getDescription(), tSaNames));
            }
        }

        String systemPrompt = """
                You are a conference track matching expert. Compare a paper's abstract and keywords
                against a track's description and subject areas to determine compatibility.
                Also consider if another track in the same conference might be a better fit.
                Respond with ONLY valid JSON:
                {"matchScore": 0-100, "explanation": "brief reason", "suggestedTrack": "name or null if current is best"}
                """;

        String userMsg = String.format("""
                Paper Abstract: %s
                Paper Keywords: %s
                
                Selected Track: %s
                Track Description: %s
                Track Subject Areas: %s
                
                Other available tracks:
                %s
                """, request.getAbstractText(),
                request.getKeywords() != null ? request.getKeywords() : "not provided",
                track.getName(), track.getDescription(), saNames,
                otherTracksInfo.length() > 0 ? otherTracksInfo.toString() : "No other tracks");

        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", userMsg));
        String aiReply = geminiClient.generateContent(systemPrompt, messages);
        return parseTrackFitResponse(aiReply);
    }

    private TrackFitResponse parseTrackFitResponse(String aiReply) {
        try {
            String json = extractJson(aiReply);
            JsonNode root = objectMapper.readTree(json);
            return TrackFitResponse.builder()
                    .matchScore(root.path("matchScore").asInt(50))
                    .explanation(root.path("explanation").asText("Unable to determine match"))
                    .suggestedTrack(root.path("suggestedTrack").isNull() ? null : root.path("suggestedTrack").asText())
                    .build();
        } catch (Exception e) {
            log.error("[AIAssistant] Failed to parse track fit response: {}", e.getMessage());
            return TrackFitResponse.builder().matchScore(50).explanation("Could not analyze track fit").build();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  FEATURE 3: Academic Tone & Grammar Checker
    // ═══════════════════════════════════════════════════════

    public WritingCheckResponse checkAcademicWriting(CheckWritingRequest request) {
        String systemPrompt = """
                You are an academic writing reviewer for scientific conferences.
                Check the provided title and abstract for:
                1. Spelling mistakes (typos, misspelled words)
                2. Grammar errors (subject-verb agreement, tense, articles, punctuation)
                3. Non-academic tone (informal, colloquial, or casual language)
                4. Clarity issues (vague, ambiguous, or unclear phrasing)
                
                Classify each issue with the correct type:
                - SPELLING: for misspelled words and typos
                - GRAMMAR: for grammatical errors (not spelling)
                - TONE: for informal or non-academic language
                - CLARITY: for vague or unclear expressions
                
                Return ONLY valid JSON:
                {
                  "suggestions": [
                    {"original": "exact text with issue", "suggested": "corrected version", "reason": "brief explanation", "type": "SPELLING|GRAMMAR|TONE|CLARITY"}
                  ],
                  "overallAssessment": "brief overall quality assessment"
                }
                Maximum 10 suggestions. If writing is excellent, return empty suggestions array.
                """;

        String userMsg = String.format("Title: %s\n\nAbstract: %s",
                request.getTitle() != null ? request.getTitle() : "",
                request.getAbstractText());

        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", userMsg));
        try {
            String aiReply = geminiClient.generateContent(systemPrompt, messages);
            log.info("[AIAssistant] Writing check raw response (first 500 chars): {}",
                    aiReply.length() > 500 ? aiReply.substring(0, 500) : aiReply);
            return parseWritingCheckResponse(aiReply);
        } catch (RuntimeException e) {
            log.error("[AIAssistant] Gemini API error for writing check: {}", e.getMessage());
            return WritingCheckResponse.builder()
                    .suggestions(List.of())
                    .overallAssessment("AI service error: " + e.getMessage())
                    .build();
        }
    }

    private WritingCheckResponse parseWritingCheckResponse(String aiReply) {
        try {
            String json = extractJson(aiReply);
            if (json.equals(aiReply.trim()) && !json.startsWith("{")) {
                // extractJson couldn't find JSON — AI returned plain text
                log.warn("[AIAssistant] Writing check: AI returned non-JSON: {}", aiReply);
                throw new RuntimeException("AI did not return valid JSON");
            }
            JsonNode root = objectMapper.readTree(json);

            List<WritingCheckResponse.WritingSuggestion> suggestions = new ArrayList<>();
            root.path("suggestions").forEach(n -> suggestions.add(
                    WritingCheckResponse.WritingSuggestion.builder()
                            .original(n.path("original").asText(""))
                            .suggested(n.path("suggested").asText(""))
                            .reason(n.path("reason").asText(""))
                            .type(n.path("type").asText("GRAMMAR"))
                            .build()
            ));

            return WritingCheckResponse.builder()
                    .suggestions(suggestions)
                    .overallAssessment(root.path("overallAssessment").asText("Analysis complete"))
                    .build();
        } catch (Exception e) {
            log.error("[AIAssistant] Failed to parse writing check response. Raw reply: {}", aiReply, e);
            throw new BadRequestException("AI analysis failed. The AI returned an unexpected response. Please try again.");
        }
    }

    // ═══════════════════════════════════════════════════════
    //  FEATURE 5: Paper Quick Summarizer (Reviewer)
    // ═══════════════════════════════════════════════════════

    public PaperSummaryResponse summarizePaper(Integer paperId) {
        Paper paper = paperRepo.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found"));

        String systemPrompt = """
                You are an academic paper summarizer helping peer reviewers.
                Based on the paper's abstract and any provided content, create a structured summary.
                DO NOT include any author information — this is a double-blind review.
                Respond with ONLY valid JSON:
                {
                  "summary": "200-word summary of the paper",
                  "keyContributions": ["contribution 1", "contribution 2", ...],
                  "methodology": "Brief methodology description"
                }
                """;

        String content = "Paper Title: " + paper.getTitle() + "\n\nAbstract: " + paper.getAbstractField();
        if (paper.getKeywordsJson() != null) {
            content += "\n\nKeywords: " + paper.getKeywordsJson();
        }

        String pdfText = getManuscriptContent(paperId);
        if (pdfText != null && !pdfText.isBlank()) {
            content += "\n\n--- Full Manuscript Text ---\n" + pdfText;
        }

        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", content));
        String aiReply = geminiClient.generateContent(systemPrompt, messages);
        return parsePaperSummaryResponse(aiReply);
    }

    private PaperSummaryResponse parsePaperSummaryResponse(String aiReply) {
        try {
            String json = extractJson(aiReply);
            JsonNode root = objectMapper.readTree(json);

            List<String> contributions = new ArrayList<>();
            root.path("keyContributions").forEach(n -> contributions.add(n.asText()));

            return PaperSummaryResponse.builder()
                    .summary(root.path("summary").asText("No summary available"))
                    .keyContributions(contributions)
                    .methodology(root.path("methodology").asText("Not specified"))
                    .build();
        } catch (Exception e) {
            log.error("[AIAssistant] Failed to parse paper summary: {}", e.getMessage());
            return PaperSummaryResponse.builder()
                    .summary("Could not generate summary")
                    .keyContributions(List.of())
                    .methodology("Unknown")
                    .build();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  FEATURE 6: Strength & Weakness Highlighter (Reviewer)
    // ═══════════════════════════════════════════════════════

    public StrengthWeaknessResponse analyzeStrengthsWeaknesses(Integer paperId) {
        Paper paper = paperRepo.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found"));

        String systemPrompt = """
                You are a critical academic reviewer analyzing a research paper.
                Identify 3-5 strengths and 3-5 weaknesses of the paper from a scholarly perspective.
                Focus on: methodology, novelty, clarity, evidence quality, contribution significance.
                DO NOT include any author information — this is a double-blind review.
                Be balanced and constructive.
                Respond with ONLY valid JSON:
                {"strengths": ["strength 1", "strength 2", ...], "weaknesses": ["weakness 1", "weakness 2", ...]}
                """;

        String content = "Paper Title: " + paper.getTitle() + "\n\nAbstract: " + paper.getAbstractField();
        if (paper.getKeywordsJson() != null) {
            content += "\n\nKeywords: " + paper.getKeywordsJson();
        }

        String pdfText = getManuscriptContent(paperId);
        if (pdfText != null && !pdfText.isBlank()) {
            content += "\n\n--- Full Manuscript Text ---\n" + pdfText;
        }

        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", content));
        String aiReply = geminiClient.generateContent(systemPrompt, messages);
        return parseStrengthWeaknessResponse(aiReply);
    }

    private StrengthWeaknessResponse parseStrengthWeaknessResponse(String aiReply) {
        try {
            String json = extractJson(aiReply);
            JsonNode root = objectMapper.readTree(json);

            List<String> strengths = new ArrayList<>();
            root.path("strengths").forEach(n -> strengths.add(n.asText()));

            List<String> weaknesses = new ArrayList<>();
            root.path("weaknesses").forEach(n -> weaknesses.add(n.asText()));

            return StrengthWeaknessResponse.builder()
                    .strengths(strengths)
                    .weaknesses(weaknesses)
                    .build();
        } catch (Exception e) {
            log.error("[AIAssistant] Failed to parse S&W response: {}", e.getMessage());
            return StrengthWeaknessResponse.builder()
                    .strengths(List.of("Could not analyze"))
                    .weaknesses(List.of("Could not analyze"))
                    .build();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  FEATURE 7: Review Disagreement Summarizer (Chair)
    // ═══════════════════════════════════════════════════════

    public ConsensusResponse analyzeReviewConsensus(Integer paperId) {
        Paper paper = paperRepo.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found"));

        List<Review> reviews = reviewRepo.findByPaper_Id(paperId);
        if (reviews.isEmpty()) {
            throw new BadRequestException("No reviews found for this paper");
        }

        // Build structured review data for AI
        StringBuilder reviewData = new StringBuilder();
        reviewData.append("Paper: ").append(paper.getTitle()).append("\n\n");

        for (int i = 0; i < reviews.size(); i++) {
            Review review = reviews.get(i);
            reviewData.append(String.format("=== Reviewer %d (Score: %s) ===\n",
                    i + 1, review.getTotalScore()));

            List<ReviewAnswer> answers = reviewAnswerRepo.findByReview_Id(review.getId());
            for (ReviewAnswer answer : answers) {
                if (answer.getQuestion() != null && answer.getAnswerValue() != null) {
                    reviewData.append(String.format("Q: %s\nA: %s\n\n",
                            answer.getQuestion().getText(),
                            answer.getAnswerValue()));
                }
            }
            reviewData.append("\n");
        }

        String systemPrompt = """
                You are a meta-reviewer analyzing multiple peer reviews for a single academic paper.
                Given the review scores and answers, identify:
                1. Points of agreement among reviewers (what they all agree on)
                2. Points of disagreement (where reviewers differ significantly)
                3. Overall consensus level (0-100, where 100 = total agreement)
                4. A recommendation for the Chair
                
                Respond with ONLY valid JSON:
                {
                  "agreementScore": 0-100,
                  "recommendation": "brief recommendation",
                  "agreements": ["point 1", "point 2"],
                  "disagreements": ["point 1", "point 2"]
                }
                """;

        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", reviewData.toString()));
        String aiReply = geminiClient.generateContent(systemPrompt, messages);
        return parseConsensusResponse(aiReply);
    }

    private ConsensusResponse parseConsensusResponse(String aiReply) {
        try {
            String json = extractJson(aiReply);
            JsonNode root = objectMapper.readTree(json);

            List<String> agreements = new ArrayList<>();
            root.path("agreements").forEach(n -> agreements.add(n.asText()));

            List<String> disagreements = new ArrayList<>();
            root.path("disagreements").forEach(n -> disagreements.add(n.asText()));

            return ConsensusResponse.builder()
                    .agreementScore(root.path("agreementScore").asInt(50))
                    .recommendation(root.path("recommendation").asText("No recommendation"))
                    .agreements(agreements)
                    .disagreements(disagreements)
                    .build();
        } catch (Exception e) {
            log.error("[AIAssistant] Failed to parse consensus response: {}", e.getMessage());
            return ConsensusResponse.builder()
                    .agreementScore(0)
                    .recommendation("Could not analyze consensus")
                    .agreements(List.of())
                    .disagreements(List.of())
                    .build();
        }
    }

    // ═══════════════════════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════════════════════

    /**
     * Attempts to fetch and parse the latest active manuscript PDF
     * to inject into AI context.
     */
    private String getManuscriptContent(Integer paperId) {
        Optional<PaperFile> manuscriptOpt = paperFileRepo.findByPaper_Id(paperId).stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()) && !Boolean.TRUE.equals(f.getIsCameraReady()) && f.getUrl() != null)
                .max(Comparator.comparing(PaperFile::getId));

        if (manuscriptOpt.isPresent()) {
            String urlStr = manuscriptOpt.get().getUrl();
            try {
                java.net.URI uri = java.net.URI.create(urlStr);
                try (InputStream is = uri.toURL().openStream()) {
                    byte[] pdfBytes = is.readAllBytes();
                    try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        String text = stripper.getText(document);
                        
                        // Limit text size to ~40k chars to keep latency low & avoid hitting arbitrary limits
                        if (text.length() > 40000) {
                            return text.substring(0, 40000) + "\n... [TRUNCATED FOR LENGTH]";
                        }
                        return text;
                    }
                }
            } catch (Exception e) {
                log.warn("[AIAssistant] Could not extract PDF from '{}': {}", urlStr, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Extract JSON object from AI response that may contain markdown fences or extra text.
     */
    private String extractJson(String aiReply) {
        String text = aiReply.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
