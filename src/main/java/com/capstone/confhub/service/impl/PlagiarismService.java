package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperFile;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.integration.GeminiApiClient;
import com.capstone.confhub.repository.PaperFileRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.utils.enums.PlagiarismStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Plagiarism checking service with dual-layer approach:
 * 1. Internal: TF-IDF Cosine Similarity + N-gram snippet matching against all papers in DB
 * 2. Web Search: Gemini AI with Google Search grounding to find similar content online
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlagiarismService {

    private final PaperRepository paperRepository;
    private final PaperFileRepository paperFileRepository;
    private final GeminiApiClient geminiClient;
    private final ObjectMapper objectMapper;

    private static final int MAX_PDF_TEXT_LENGTH = 15000; // chars for plagiarism check

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

    /**
     * Mark as CHECKING immediately, then run check in background.
     * Called by the recheck endpoint so the response returns fast.
     */
    public void recheckPlagiarismAsync(Integer paperId) {
        // Set status to CHECKING synchronously so the immediate GET returns correct status
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found: " + paperId));
        paper.setPlagiarismStatus(PlagiarismStatus.CHECKING);
        paperRepository.save(paper);
        // Kick off async
        checkPlagiarismAsync(paperId);
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
        result.put("detailsJson", paper.getPlagiarismDetailsJson()); // raw JSON string

        return result;
    }

    @Transactional
    public void resetPlagiarism(Integer paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found: " + paperId));
        paper.setPlagiarismScore(null);
        paper.setPlagiarismStatus(null);
        paper.setPlagiarismDetailsJson(null);
        paperRepository.save(paper);
        log.info("[Plagiarism] Reset plagiarism data for paper {}", paperId);
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

        // Extract text from the ACTIVE PDF file
        String text = extractPdfTextForPaper(paper);
        if (text == null || text.isBlank()) {
            markFailed(paperId, "Could not extract text from PDF. Make sure the paper has an active manuscript file uploaded.");
            return;
        }

        log.info("[Plagiarism] Extracted {} chars from PDF for paper {}", text.length(), paperId);

        try {
            // Phase 1: Internal check (Cosine Similarity + N-gram snippets)
            InternalCheckResult internalResult = performInternalCheck(paper, text);

            // Phase 2: Web Search check (Gemini + Google Search grounding)
            WebSearchResult webSearchResult = performWebSearch(text);

            // Calculate final score = max of internal and web
            double finalScore = Math.max(internalResult.score, webSearchResult.score);

            // Build details JSON
            ObjectNode details = objectMapper.createObjectNode();
            details.put("internalScore", round(internalResult.score));
            details.put("webSearchScore", round(webSearchResult.score));
            details.put("finalScore", round(finalScore));

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
            if (webSearchResult.summary != null && !webSearchResult.summary.isBlank()) {
                details.put("webSearchSummary", webSearchResult.summary);
            }

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
            double rawSimilarity = cosineSimilarity(targetVector, otherVector) * 100;

            // Find matching n-gram snippets (actual text overlap proof)
            String matchedSnippet = findMatchingSnippet(targetNgrams, otherText);

            // If no actual text overlap found, cap similarity at 15%
            // (TF-IDF alone can give false positives due to common academic vocabulary)
            double similarity = rawSimilarity;
            if (matchedSnippet == null && similarity > 15.0) {
                log.debug("[Plagiarism] Capping score for paper {} (raw={}, no snippet match)", other.getId(), round(rawSimilarity));
                similarity = 15.0;
            }

            if (similarity > 3.0) {
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

    private static final String WEB_SEARCH_SYSTEM_PROMPT = """
            You are a plagiarism detection assistant. Given an academic text excerpt,
            use Google Search to find if any similar content exists online.
            Search for key phrases and sentences from the text.
            
            IMPORTANT: Return ONLY the raw JSON object below. Do NOT wrap it in markdown code fences.
            {
              "matches": [
                {
                  "url": "the URL where similar content was found",
                  "title": "page title",
                  "snippet": "the matching text snippet found",
                  "similarity": 0-100 (how similar the found content is to the input)
                }
              ],
              "overallScore": 0-100 (overall plagiarism likelihood based on web matches),
              "summary": "brief explanation of findings"
            }
            If no similar content is found online, return {"matches": [], "overallScore": 0, "summary": "No similar content found on the web."}.
            Be honest and accurate. Only report genuine matches with real URLs.
            """;

    private WebSearchResult performWebSearch(String text) {
        try {
            // Split text into up to 3 chunks for broader coverage
            List<String> chunks = splitIntoChunks(text, 3, 1500);
            log.info("[Plagiarism] Web search: {} chunks to process", chunks.size());

            List<WebMatch> allMatches = new ArrayList<>();
            double maxScore = 0;
            List<String> summaries = new ArrayList<>();

            for (int ci = 0; ci < chunks.size(); ci++) {
                String chunk = chunks.get(ci);
                WebSearchResult chunkResult = searchChunkWithRetry(chunk, ci + 1, chunks.size());
                allMatches.addAll(chunkResult.matches());
                maxScore = Math.max(maxScore, chunkResult.score());
                if (chunkResult.summary() != null && !chunkResult.summary().isBlank()) {
                    summaries.add(chunkResult.summary());
                }
            }

            // Deduplicate by URL, keep highest similarity
            Map<String, WebMatch> deduped = new LinkedHashMap<>();
            for (WebMatch wm : allMatches) {
                deduped.merge(wm.url(), wm, (a, b) -> a.similarity() >= b.similarity() ? a : b);
            }
            List<WebMatch> finalMatches = new ArrayList<>(deduped.values());
            finalMatches.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));
            if (finalMatches.size() > 5) finalMatches = finalMatches.subList(0, 5);

            String combinedSummary = summaries.isEmpty() ? "Web search completed." : String.join(" ", summaries);

            log.info("[Plagiarism] Web search total: {} unique matches, max score: {}%", finalMatches.size(), round(maxScore));
            return new WebSearchResult(maxScore, finalMatches, combinedSummary);

        } catch (Exception e) {
            log.warn("[Plagiarism] Web search failed: {}", e.getMessage(), e);
            return new WebSearchResult(0, List.of(), "Web search encountered an error: " + e.getMessage());
        }
    }

    /**
     * Search a single chunk with retry (up to 3 attempts).
     * Each retry rotates to a different API key automatically.
     */
    private WebSearchResult searchChunkWithRetry(String chunk, int chunkNum, int totalChunks) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String userMsg = "Check this text excerpt (part " + chunkNum + "/" + totalChunks
                        + ") for potential plagiarism by searching the web:\n\n" + chunk;
                List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", userMsg));

                log.info("[Plagiarism] Chunk {}/{} attempt {}/{}", chunkNum, totalChunks, attempt, maxAttempts);
                String aiReply = geminiClient.generateContentWithSearch(WEB_SEARCH_SYSTEM_PROMPT, messages);
                log.info("[Plagiarism] Chunk {}/{} response (first 300 chars): {}",
                        chunkNum, totalChunks,
                        aiReply.length() > 300 ? aiReply.substring(0, 300) : aiReply);

                return parseWebSearchResponse(aiReply);

            } catch (Exception e) {
                log.warn("[Plagiarism] Chunk {}/{} attempt {}/{} failed: {}",
                        chunkNum, totalChunks, attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
        log.error("[Plagiarism] Chunk {}/{} failed after {} attempts", chunkNum, totalChunks, maxAttempts);
        return new WebSearchResult(0, List.of(), "Chunk " + chunkNum + " search failed after retries.");
    }

    /**
     * Split text into N roughly equal chunks for parallel/sequential search.
     */
    private List<String> splitIntoChunks(String text, int maxChunks, int maxCharsPerChunk) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= maxCharsPerChunk) {
            chunks.add(text);
            return chunks;
        }

        int totalLen = Math.min(text.length(), maxCharsPerChunk * maxChunks);
        int chunkSize = totalLen / maxChunks;

        for (int i = 0; i < maxChunks && i * chunkSize < totalLen; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, totalLen);
            // Try to break at a sentence boundary
            if (end < totalLen) {
                int dot = text.lastIndexOf('.', end);
                if (dot > start + chunkSize / 2) end = dot + 1;
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * Parse Gemini's web search response into structured WebSearchResult.
     */
    private WebSearchResult parseWebSearchResponse(String aiReply) {
        try {
            // Detect non-JSON responses (e.g. "I'm sorry, I couldn't generate a response")
            String trimmed = aiReply.trim();
            if (!trimmed.contains("{")) {
                throw new RuntimeException("AI returned non-JSON: " + trimmed.substring(0, Math.min(100, trimmed.length())));
            }

            String json = extractJsonFromReply(aiReply);

            // Sanitize: replace unescaped newlines/tabs inside JSON string values
            json = json.replace("\t", "\\t");
            // Replace newlines that are inside JSON strings (not structural newlines)
            json = sanitizeJsonNewlines(json);

            // Use lenient mapper for control characters
            com.fasterxml.jackson.core.JsonFactory factory = new com.fasterxml.jackson.core.JsonFactory();
            factory.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
            ObjectMapper lenientMapper = new ObjectMapper(factory);
            JsonNode root = lenientMapper.readTree(json);

            double overallScore = root.path("overallScore").asDouble(0);
            String summary = root.path("summary").asText("Web search completed.");
            List<WebMatch> matches = new ArrayList<>();

            JsonNode matchesNode = root.path("matches");
            if (matchesNode.isArray()) {
                for (JsonNode m : matchesNode) {
                    String url = m.path("url").asText("");
                    String title = m.path("title").asText("");
                    String snippet = m.path("snippet").asText("");
                    double similarity = m.path("similarity").asDouble(0);
                    if (!url.isBlank() && similarity > 10) {
                        matches.add(new WebMatch(url, title, snippet, similarity));
                    }
                }
            }

            matches.sort((a, b) -> Double.compare(b.similarity, a.similarity));
            if (matches.size() > 5) matches = matches.subList(0, 5);

            double maxScore = matches.stream()
                    .mapToDouble(m -> m.similarity)
                    .max().orElse(overallScore);
            maxScore = Math.max(maxScore, overallScore);

            log.info("[Plagiarism] Web search found {} matches, score: {}%, summary: {}", matches.size(), round(maxScore), summary);
            return new WebSearchResult(maxScore, matches, summary);

        } catch (Exception e) {
            log.warn("[Plagiarism] Failed to parse web search response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse web search results: " + e.getMessage(), e);
        }
    }

    /**
     * Extract JSON object from AI response (handles markdown fences).
     */
    private String extractJsonFromReply(String aiReply) {
        String text = aiReply.trim();
        // Strip markdown code fences
        if (text.contains("```")) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("```(?:json)?\\s*\\n?(\\{.*?})\\s*\\n?```", java.util.regex.Pattern.DOTALL)
                    .matcher(text);
            if (m.find()) return m.group(1).trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end > start) return text.substring(start, end + 1);
        return text;
    }

    /**
     * Replace literal newline characters inside JSON string values with escaped \\n.
     * Gemini sometimes returns multi-line strings in JSON without proper escaping.
     */
    private String sanitizeJsonNewlines(String json) {
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                sb.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                sb.append(c);
                continue;
            }
            if (inString && (c == '\n' || c == '\r')) {
                sb.append("\\n");
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }


    // ═══════════════════════════════════════════════════════
    //  TF-IDF & COSINE SIMILARITY UTILITIES
    // ═══════════════════════════════════════════════════════

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "shall", "can", "need", "must",
            "of", "in", "to", "for", "with", "on", "at", "by", "from", "as",
            "into", "through", "during", "before", "after", "above", "below",
            "between", "out", "off", "over", "under", "again", "further",
            "then", "once", "here", "there", "when", "where", "why", "how",
            "all", "each", "every", "both", "few", "more", "most", "other",
            "some", "such", "no", "nor", "not", "only", "own", "same",
            "so", "than", "too", "very", "just", "because", "but", "and",
            "or", "if", "while", "about", "up", "down", "also", "that",
            "this", "these", "those", "what", "which", "who", "whom",
            "its", "his", "her", "their", "our", "your", "my", "it",
            "he", "she", "they", "we", "you", "me", "him", "them", "us",
            "any", "much", "many", "well", "back", "even", "still",
            "however", "therefore", "thus", "hence", "paper", "using",
            "used", "based", "approach", "proposed", "results", "method",
            "show", "data", "model", "system", "work", "study"
    );

    private Map<String, Double> buildTfIdfVector(String text) {
        String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        Map<String, Integer> termFreq = new HashMap<>();
        int totalTokens = 0;

        for (String token : tokens) {
            // Only meaningful words: >= 4 chars, not a stop word, not pure numbers
            if (token.length() >= 4 && !STOP_WORDS.contains(token) && !token.matches("\\d+")) {
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

    /**
     * Extract text from the active PDF file of a paper.
     * Downloads the PDF from Firebase Storage URL and uses PDFBox to extract text.
     */
    private String extractPdfTextForPaper(Paper paper) {
        // Find the active manuscript file
        List<PaperFile> files = paperFileRepository.findByPaper_Id(paper.getId());
        PaperFile activeFile = files.stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()) && !Boolean.TRUE.equals(f.getIsSupplementary()) && !Boolean.TRUE.equals(f.getIsCameraReady()))
                .findFirst()
                .orElse(null);

        if (activeFile == null) {
            log.warn("[Plagiarism] No active manuscript file found for paper {}", paper.getId());
            return null;
        }

        String url = activeFile.getUrl();
        if (url == null || url.isBlank()) {
            log.warn("[Plagiarism] Active file has no URL for paper {}", paper.getId());
            return null;
        }

        return downloadAndExtractPdf(url, paper.getId());
    }

    /**
     * Download PDF from URL and extract text using PDFBox.
     */
    private String downloadAndExtractPdf(String url, Integer paperId) {
        try {
            log.info("[Plagiarism] Downloading PDF from: {}", url);
            try (InputStream is = URI.create(url).toURL().openStream()) {
                byte[] pdfBytes = is.readAllBytes();
                PDDocument doc = Loader.loadPDF(pdfBytes);
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);
                doc.close();

                // Clean up: remove excessive whitespace
                text = text.replaceAll("\\s{3,}", "\n\n").trim();

                // Truncate if too long
                if (text.length() > MAX_PDF_TEXT_LENGTH) {
                    text = text.substring(0, MAX_PDF_TEXT_LENGTH);
                }

                log.info("[Plagiarism] Extracted {} chars from PDF for paper {}", text.length(), paperId);
                return text;
            }
        } catch (Exception e) {
            log.error("[Plagiarism] Failed to download/extract PDF for paper {}: {}", paperId, e.getMessage());
            return null;
        }
    }

    /**
     * Fallback: get title + abstract text for internal comparison with other papers.
     * Used when we don't want to download PDFs for ALL papers in the DB.
     */
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


    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    // ═══════════════════════════════════════════════════════
    //  INNER CLASSES
    // ═══════════════════════════════════════════════════════

    private record InternalCheckResult(double score, List<PaperMatch> matches) {}
    private record PaperMatch(Integer paperId, String title, double similarity, String matchedSnippet) {}
    private record WebSearchResult(double score, List<WebMatch> matches, String summary) {}
    private record WebMatch(String url, String title, String snippet, double similarity) {}
}
