package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.integration.GeminiApiClient;
import com.capstone.confhub.integration.GoogleSearchClient;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.utils.enums.PlagiarismStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Plagiarism checking service with triple-layer approach:
 * 1. Internal: TF-IDF Cosine Similarity + N-gram snippet matching against all papers in DB
 * 2. Web Search: Google Custom Search API for finding similar content online
 * 3. External AI: Gemini AI analysis for originality assessment with detailed reasoning
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlagiarismService {

    private final PaperRepository paperRepository;
    private final GeminiApiClient geminiClient;
    private final GoogleSearchClient googleSearchClient;
    private final ObjectMapper objectMapper;

    // ═══════════════════════════════════════════════════════
    //  PUBLIC API
    // ═══════════════════════════════════════════════════════

    @Async
    public void checkPlagiarismAsync(Integer paperId) {
        try {
            log.info("[Plagiarism] Starting async check for paper {}", paperId);
            performPlagiarismCheck(paperId);
        } catch (Exception e) {
            log.error("[Plagiarism] Async check failed for paper {}: {}", paperId, e.getMessage(), e);
            markFailed(paperId, e.getMessage());
        }
    }

    @Transactional
    public void recheckPlagiarism(Integer paperId) {
        performPlagiarismCheck(paperId);
    }

    public Map<String, Object> getPlagiarismResult(Integer paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found: " + paperId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paperId", paperId);
        result.put("score", paper.getPlagiarismScore());
        result.put("status", paper.getPlagiarismStatus());

        if (paper.getPlagiarismDetailsJson() != null) {
            try {
                result.put("details", objectMapper.readTree(paper.getPlagiarismDetailsJson()));
            } catch (Exception e) {
                result.put("details", null);
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    //  CORE PLAGIARISM CHECK
    // ═══════════════════════════════════════════════════════

    @Transactional
    protected void performPlagiarismCheck(Integer paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found: " + paperId));

        paper.setPlagiarismStatus(PlagiarismStatus.CHECKING);
        paperRepository.save(paper);

        String text = buildPaperText(paper);
        if (text.isBlank()) {
            markFailed(paperId, "Paper has no title or abstract");
            return;
        }

        try {
            // Phase 1: Internal check (Cosine Similarity + N-gram snippets)
            InternalCheckResult internalResult = performInternalCheck(paper, text);

            // Phase 2: Web Search check (Google Custom Search API)
            WebSearchResult webSearchResult = performWebSearch(text);

            // Phase 3: External AI check (Gemini with detailed analysis)
            ExternalCheckResult externalResult = performExternalCheck(paper, text, webSearchResult);

            // Calculate final score = weighted combination
            double finalScore = Math.max(internalResult.score,
                    Math.max(webSearchResult.score, externalResult.score));

            // Build details JSON
            ObjectNode details = objectMapper.createObjectNode();
            details.put("internalScore", round(internalResult.score));
            details.put("externalScore", round(externalResult.score));
            details.put("webSearchScore", round(webSearchResult.score));
            details.put("finalScore", round(finalScore));
            details.put("checkedAt", java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()));

            // Internal matches with snippets
            ArrayNode matchesArray = objectMapper.createArrayNode();
            for (PaperMatch match : internalResult.matches) {
                ObjectNode matchNode = objectMapper.createObjectNode();
                matchNode.put("paperId", match.paperId);
                matchNode.put("title", match.title);
                matchNode.put("similarity", round(match.similarity));
                if (match.matchedSnippet != null && !match.matchedSnippet.isBlank()) {
                    matchNode.put("matchedSnippet", match.matchedSnippet);
                }
                matchesArray.add(matchNode);
            }
            details.set("internalMatches", matchesArray);

            // Web search matches
            ArrayNode webMatchesArray = objectMapper.createArrayNode();
            for (WebMatch wm : webSearchResult.matches) {
                ObjectNode wmNode = objectMapper.createObjectNode();
                wmNode.put("url", wm.url);
                wmNode.put("title", wm.title);
                wmNode.put("snippet", wm.snippet);
                wmNode.put("similarity", round(wm.similarity));
                webMatchesArray.add(wmNode);
            }
            details.set("webSearchMatches", webMatchesArray);

            // External AI analysis
            ObjectNode extNode = objectMapper.createObjectNode();
            extNode.put("score", round(externalResult.score));
            extNode.put("summary", externalResult.summary);
            ArrayNode flaggedArr = objectMapper.createArrayNode();
            for (FlaggedSection fs : externalResult.flaggedSections) {
                ObjectNode fsNode = objectMapper.createObjectNode();
                fsNode.put("text", fs.text);
                fsNode.put("reason", fs.reason);
                fsNode.put("source", fs.source);
                fsNode.put("confidence", fs.confidence);
                flaggedArr.add(fsNode);
            }
            extNode.set("flaggedSections", flaggedArr);
            details.set("externalAnalysis", extNode);

            details.put("checkedAt", Instant.now().toString());

            // Save result
            paper.setPlagiarismScore(round(finalScore));
            paper.setPlagiarismStatus(PlagiarismStatus.COMPLETED);
            paper.setPlagiarismDetailsJson(objectMapper.writeValueAsString(details));
            paperRepository.save(paper);

            log.info("[Plagiarism] Check completed for paper {}. Score: {}%", paperId, round(finalScore));

        } catch (Exception e) {
            log.error("[Plagiarism] Check failed for paper {}: {}", paperId, e.getMessage(), e);
            markFailed(paperId, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  INTERNAL CHECK: TF-IDF Cosine Similarity + N-gram
    // ═══════════════════════════════════════════════════════

    private InternalCheckResult performInternalCheck(Paper targetPaper, String targetText) {
        List<Paper> allPapers = paperRepository.findAll();

        Map<String, Double> targetVector = buildTfIdfVector(targetText);
        List<String> targetNgrams = extractNgrams(targetText, 5);
        List<PaperMatch> matches = new ArrayList<>();
        double maxSimilarity = 0;

        for (Paper other : allPapers) {
            if (other.getId().equals(targetPaper.getId())) continue;

            String otherText = buildPaperText(other);
            if (otherText.isBlank()) continue;

            Map<String, Double> otherVector = buildTfIdfVector(otherText);
            double similarity = cosineSimilarity(targetVector, otherVector) * 100;

            // Also find matching n-gram snippets
            String matchedSnippet = findMatchingSnippet(targetNgrams, otherText);

            if (similarity > 10.0) {
                matches.add(new PaperMatch(other.getId(), other.getTitle(), similarity, matchedSnippet));
            }
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }

        matches.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        if (matches.size() > 5) {
            matches = matches.subList(0, 5);
        }

        return new InternalCheckResult(maxSimilarity, matches);
    }

    /**
     * Find the best matching n-gram snippet between target and other paper.
     */
    private String findMatchingSnippet(List<String> targetNgrams, String otherText) {
        String otherLower = otherText.toLowerCase();
        String bestMatch = "";
        for (String ngram : targetNgrams) {
            if (otherLower.contains(ngram.toLowerCase())) {
                if (ngram.length() > bestMatch.length()) {
                    bestMatch = ngram;
                }
            }
        }
        return bestMatch.isBlank() ? null : "\"..." + bestMatch + "...\"";
    }

    /**
     * Extract n-grams (word-level) from text for snippet matching.
     */
    private List<String> extractNgrams(String text, int n) {
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        List<String> ngrams = new ArrayList<>();
        for (int i = 0; i <= words.length - n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) sb.append(" ");
                sb.append(words[i + j]);
            }
            String ngram = sb.toString().trim();
            if (ngram.length() >= 15) {  // Only meaningful ngrams
                ngrams.add(ngram);
            }
        }
        // Sample to avoid too many queries — take every nth
        if (ngrams.size() > 20) {
            int step = ngrams.size() / 20;
            List<String> sampled = new ArrayList<>();
            for (int i = 0; i < ngrams.size(); i += step) {
                sampled.add(ngrams.get(i));
                if (sampled.size() >= 20) break;
            }
            return sampled;
        }
        return ngrams;
    }

    // ═══════════════════════════════════════════════════════
    //  WEB SEARCH CHECK: Google Custom Search API
    // ═══════════════════════════════════════════════════════

    private WebSearchResult performWebSearch(String text) {
        if (!googleSearchClient.isConfigured()) {
            log.info("[Plagiarism] Google Search not configured, skipping web search check.");
            return new WebSearchResult(0, List.of());
        }

        try {
            // Extract key sentences for search queries
            List<String> queries = extractSearchQueries(text);
            List<WebMatch> allMatches = new ArrayList<>();

            for (String query : queries) {
                List<GoogleSearchClient.SearchResult> results = googleSearchClient.search(
                        "\"" + query + "\"", 5);  // Exact phrase search

                for (GoogleSearchClient.SearchResult sr : results) {
                    double similarity = calculateSnippetSimilarity(query, sr.snippet());
                    if (similarity > 30.0) {
                        allMatches.add(new WebMatch(sr.link(), sr.title(), sr.snippet(), similarity));
                    }
                }
            }

            // Deduplicate by URL, keep highest similarity
            Map<String, WebMatch> deduped = new LinkedHashMap<>();
            for (WebMatch wm : allMatches) {
                deduped.merge(wm.url, wm, (a, b) -> a.similarity >= b.similarity ? a : b);
            }
            List<WebMatch> finalMatches = new ArrayList<>(deduped.values());
            finalMatches.sort((a, b) -> Double.compare(b.similarity, a.similarity));
            if (finalMatches.size() > 5) finalMatches = finalMatches.subList(0, 5);

            double maxScore = finalMatches.stream()
                    .mapToDouble(m -> m.similarity)
                    .max().orElse(0);

            return new WebSearchResult(maxScore, finalMatches);

        } catch (Exception e) {
            log.warn("[Plagiarism] Web search failed: {}", e.getMessage());
            return new WebSearchResult(0, List.of());
        }
    }

    /**
     * Extract meaningful search queries from text (key sentences / phrases).
     */
    private List<String> extractSearchQueries(String text) {
        // Split into sentences and pick the most meaningful ones
        String[] sentences = text.split("[.!?]+");
        List<String> queries = new ArrayList<>();
        for (String s : sentences) {
            String trimmed = s.trim();
            // Only use sentences that are meaningful (7-15 words)
            long wordCount = trimmed.split("\\s+").length;
            if (wordCount >= 7 && wordCount <= 20 && trimmed.length() >= 30) {
                queries.add(trimmed);
            }
        }
        // Limit to 3 queries to avoid rate limiting
        if (queries.size() > 3) {
            queries = queries.subList(0, 3);
        }
        return queries;
    }

    /**
     * Calculate similarity between a query and a search snippet using word overlap.
     */
    private double calculateSnippetSimilarity(String query, String snippet) {
        Set<String> queryWords = new HashSet<>(Arrays.asList(
                query.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+")));
        Set<String> snippetWords = new HashSet<>(Arrays.asList(
                snippet.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+")));

        // Remove common stop words
        Set<String> stopWords = Set.of("the", "a", "an", "is", "are", "was", "were", "of", "in",
                "to", "for", "and", "or", "but", "on", "at", "by", "with", "from", "that", "this");
        queryWords.removeAll(stopWords);
        snippetWords.removeAll(stopWords);

        if (queryWords.isEmpty()) return 0;

        Set<String> intersection = new HashSet<>(queryWords);
        intersection.retainAll(snippetWords);

        return (double) intersection.size() / queryWords.size() * 100;
    }

    // ═══════════════════════════════════════════════════════
    //  EXTERNAL CHECK: Gemini AI (Enhanced with web context)
    // ═══════════════════════════════════════════════════════

    private ExternalCheckResult performExternalCheck(Paper paper, String text, WebSearchResult webContext) {
        try {
            // Build web context string from search results
            StringBuilder webContextStr = new StringBuilder();
            if (!webContext.matches.isEmpty()) {
                webContextStr.append("\n\nWEB SEARCH RESULTS (similar content found online):\n");
                for (WebMatch wm : webContext.matches) {
                    webContextStr.append("- Source: ").append(wm.title).append(" (").append(wm.url).append(")\n");
                    webContextStr.append("  Snippet: ").append(wm.snippet).append("\n");
                }
            }

            String systemPrompt = """
                    You are an academic plagiarism detection expert. Analyze the following paper title and abstract
                    for originality. Check if the content appears to be copied or closely paraphrased from known
                    published academic works, common textbook content, or widely available online sources.
                    
                    You will also be provided with web search results that show similar content found online.
                    Use these to help identify potential sources of plagiarism.
                    
                    Rate the plagiarism percentage from 0 to 100:
                    - 0 = completely original
                    - 100 = entirely plagiarized
                    
                    For each flagged section, provide:
                    - The exact text that appears plagiarized
                    - The reason why it is flagged
                    - The likely source (paper name, URL, or "common knowledge")
                    - A confidence level from 0 to 100
                    
                    Respond with ONLY valid JSON, no markdown, no other text:
                    {
                      "plagiarismScore": <number 0-100>,
                      "summary": "<detailed assessment explaining WHY this score was given, what parts are original vs suspicious>",
                      "flaggedSections": [
                        {
                          "text": "<exact text that appears plagiarized>",
                          "reason": "<why this is flagged - e.g. 'closely paraphrases known work', 'common phrasing from textbooks'>",
                          "source": "<likely source - paper title, URL, or 'common academic phrasing'>",
                          "confidence": <number 0-100>
                        }
                      ]
                    }
                    """;

            String userMsg = "Title: " + paper.getTitle() + "\n\nAbstract: " + paper.getAbstractField()
                    + webContextStr;
            List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", userMsg));

            String aiReply = geminiClient.generateContent(systemPrompt, messages);
            log.info("[Plagiarism] Gemini raw response: {}", aiReply);

            return parseExternalResult(aiReply);

        } catch (Exception e) {
            log.warn("[Plagiarism] Gemini analysis failed: {}. Using 0 for external score.", e.getMessage());
            return new ExternalCheckResult(0, "Gemini analysis unavailable: " + e.getMessage(), List.of());
        }
    }

    private ExternalCheckResult parseExternalResult(String aiReply) {
        try {
            String json = extractJson(aiReply);
            JsonNode root = objectMapper.readTree(json);

            double score = root.path("plagiarismScore").asDouble(0);
            String summary = root.path("summary").asText("No summary available");

            List<FlaggedSection> flagged = new ArrayList<>();
            JsonNode flaggedNode = root.path("flaggedSections");
            if (flaggedNode.isArray()) {
                for (JsonNode fs : flaggedNode) {
                    flagged.add(new FlaggedSection(
                            fs.path("text").asText(""),
                            fs.path("reason").asText(""),
                            fs.path("source").asText("unknown"),
                            fs.path("confidence").asInt(50)
                    ));
                }
            }

            return new ExternalCheckResult(score, summary, flagged);
        } catch (Exception e) {
            log.error("[Plagiarism] Failed to parse Gemini response: {}", e.getMessage());
            return new ExternalCheckResult(0, "Failed to parse AI response", List.of());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  TF-IDF & COSINE SIMILARITY UTILITIES
    // ═══════════════════════════════════════════════════════

    private Map<String, Double> buildTfIdfVector(String text) {
        String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        Map<String, Integer> termFreq = new HashMap<>();
        int totalTokens = 0;

        for (String token : tokens) {
            if (token.length() >= 3) {
                termFreq.merge(token, 1, Integer::sum);
                totalTokens++;
            }
        }

        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            vector.put(entry.getKey(), (double) entry.getValue() / Math.max(totalTokens, 1));
        }
        return vector;
    }

    private double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(v1.keySet());
        allTerms.addAll(v2.keySet());

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (String term : allTerms) {
            double a = v1.getOrDefault(term, 0.0);
            double b = v2.getOrDefault(term, 0.0);
            dotProduct += a * b;
            norm1 += a * a;
            norm2 += b * b;
        }

        if (norm1 == 0 || norm2 == 0) return 0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // ═══════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════

    private String buildPaperText(Paper paper) {
        StringBuilder sb = new StringBuilder();
        if (paper.getTitle() != null) sb.append(paper.getTitle()).append(" ");
        if (paper.getAbstractField() != null) sb.append(paper.getAbstractField());
        return sb.toString().trim();
    }

    private void markFailed(Integer paperId, String reason) {
        try {
            Paper paper = paperRepository.findById(paperId).orElse(null);
            if (paper != null) {
                paper.setPlagiarismStatus(PlagiarismStatus.FAILED);
                ObjectNode details = objectMapper.createObjectNode();
                details.put("error", reason);
                details.put("checkedAt", Instant.now().toString());
                paper.setPlagiarismDetailsJson(details.toString());
                paperRepository.save(paper);
            }
        } catch (Exception e) {
            log.error("[Plagiarism] Failed to mark paper {} as failed: {}", paperId, e.getMessage());
        }
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        int start = text.indexOf("```json");
        if (start >= 0) {
            start = text.indexOf("\n", start) + 1;
            int end = text.indexOf("```", start);
            if (end > start) return text.substring(start, end).trim();
        }
        start = text.indexOf("```");
        if (start >= 0) {
            start = text.indexOf("\n", start) + 1;
            int end = text.indexOf("```", start);
            if (end > start) return text.substring(start, end).trim();
        }
        start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text.trim();
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    // ═══════════════════════════════════════════════════════
    //  INNER CLASSES
    // ═══════════════════════════════════════════════════════

    private record InternalCheckResult(double score, List<PaperMatch> matches) {}
    private record ExternalCheckResult(double score, String summary, List<FlaggedSection> flaggedSections) {}
    private record PaperMatch(Integer paperId, String title, double similarity, String matchedSnippet) {}
    private record FlaggedSection(String text, String reason, String source, int confidence) {}
    private record WebSearchResult(double score, List<WebMatch> matches) {}
    private record WebMatch(String url, String title, String snippet, double similarity) {}
}
