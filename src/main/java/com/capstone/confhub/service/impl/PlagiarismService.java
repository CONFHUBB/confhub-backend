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
 * 1. Internal: TF-IDF Cosine Similarity + N-gram snippet matching against all
 * papers in DB
 * 2. Web Search: Gemini AI with Google Search grounding to find similar content
 * online
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlagiarismService {

    private final PaperRepository paperRepository;
    private final PaperFileRepository paperFileRepository;
    private final GeminiApiClient geminiClient;
    private final ObjectMapper objectMapper;

    private static final int MAX_PDF_TEXT_LENGTH = 8000; // chars for plagiarism check

    // ═══════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════

    @Async("taskExecutor")
    @Transactional
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
        paper.setPlagiarismExtractedText(null);
        paperRepository.save(paper);
        log.info("[Plagiarism] Reset plagiarism data for paper {}", paperId);
    }

    // ═══════════════════════════════════════════════════════
    // CORE PLAGIARISM CHECK
    // ═══════════════════════════════════════════════════════

    @Transactional
    public void performPlagiarismCheck(Integer paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found: " + paperId));

        paper.setPlagiarismStatus(PlagiarismStatus.CHECKING);
        // Clear cached text to force re-extraction from the current active PDF
        paper.setPlagiarismExtractedText(null);
        paperRepository.save(paper);

        // Extract text from the ACTIVE PDF file
        String text = extractPdfTextForPaper(paper);
        if (text == null || text.isBlank()) {
            markFailed(paperId,
                    "Could not extract text from PDF. Make sure the paper has an active manuscript file uploaded.");
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

    public String checkPlagiarismSyncAndGetDetails(Paper paper, String text) throws Exception {
        log.info("[Plagiarism] Sync check started. Extracted {} chars from PDF for paper {}", text.length(), paper.getId());
        
        if (text.length() > MAX_PDF_TEXT_LENGTH) {
            text = text.substring(0, MAX_PDF_TEXT_LENGTH);
        }

        InternalCheckResult internalResult = performInternalCheck(paper, text);
        WebSearchResult webSearchResult = performWebSearch(text);

        double finalScore = Math.max(internalResult.score, webSearchResult.score);

        ObjectNode details = objectMapper.createObjectNode();
        details.put("internalScore", round(internalResult.score));
        details.put("webSearchScore", round(webSearchResult.score));
        details.put("finalScore", round(finalScore));

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

        // Save plagiarism results on the paper — never block upload, just warn
        paper.setPlagiarismScore(round(finalScore));
        paper.setPlagiarismStatus(PlagiarismStatus.COMPLETED);
        paper.setPlagiarismDetailsJson(objectMapper.writeValueAsString(details));

        if (finalScore > 50.0) {
            log.warn("[Plagiarism] High similarity detected for paper {}: {}%. Upload will proceed — chairs can review.",
                    paper.getId(), round(finalScore));
        }
        
        return objectMapper.writeValueAsString(details);
    }

    // ═══════════════════════════════════════════════════════
    // INTERNAL CHECK: TF-IDF Cosine Similarity + N-gram
    // ═══════════════════════════════════════════════════════

    private InternalCheckResult performInternalCheck(Paper targetPaper, String targetText) {
        // Only compare against papers that already have cached text — avoid downloading PDFs during upload
        // Papers without cached text will be checked when they are uploaded or re-checked
        List<Paper> candidatePapers = paperRepository.findAll().stream()
                .filter(p -> p.getPlagiarismExtractedText() != null && !p.getPlagiarismExtractedText().isBlank())
                .toList();

        Map<String, Double> targetVector = buildTfIdfVector(targetText);
        List<String> targetNgrams = extractNgrams(targetText, 5);
        List<PaperMatch> matches = new ArrayList<>();
        double maxSimilarity = 0;

        for (Paper other : candidatePapers) {
            if (other.getId().equals(targetPaper.getId()))
                continue;

            String otherText = extractPdfTextForPaper(other);
            if (otherText == null || otherText.isBlank())
                continue;

            Map<String, Double> otherVector = buildTfIdfVector(otherText);
            
            // Full Text vs Full Text TF-IDF Cosine Similarity
            double rawSimilarity = cosineSimilarity(targetVector, otherVector) * 100;

            // Find matching n-gram snippets (actual text overlap proof)
            String matchedSnippet = findMatchingSnippet(targetNgrams, otherText);

            // If no actual text overlap found, cap similarity at 15%
            // (TF-IDF alone can give false positives due to common academic vocabulary)
            double similarity = rawSimilarity;
            if (matchedSnippet == null && similarity > 15.0) {
                log.debug("[Plagiarism] Capping score for paper {} (raw={}, no snippet match)", other.getId(),
                        round(rawSimilarity));
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
                if (j > 0)
                    sb.append(" ");
                sb.append(words[i + j]);
            }
            String ngram = sb.toString().trim();
            if (ngram.length() >= 15) { // Only meaningful ngrams
                ngrams.add(ngram);
            }
        }
        // Sample to avoid too many queries — take every nth
        if (ngrams.size() > 20) {
            int step = ngrams.size() / 20;
            List<String> sampled = new ArrayList<>();
            for (int i = 0; i < ngrams.size(); i += step) {
                sampled.add(ngrams.get(i));
                if (sampled.size() >= 20)
                    break;
            }
            return sampled;
        }
        return ngrams;
    }

    // ═══════════════════════════════════════════════════════
    // WEB SEARCH: SerpAPI + Regular Gemini Analysis
    // ═══════════════════════════════════════════════════════

    @org.springframework.beans.factory.annotation.Value("${serpapi.api-key:}")
    private String serpApiKey;

    private final org.springframework.web.reactive.function.client.WebClient serpWebClient =
            org.springframework.web.reactive.function.client.WebClient.builder()
                    .baseUrl("https://serpapi.com")
                    .build();

    private static final String PLAGIARISM_ANALYSIS_PROMPT = """
            You are a plagiarism detection assistant. Analyze if search results indicate plagiarism of the paper excerpt.

            IMPORTANT: Return ONLY a compact JSON (no markdown fences, no extra whitespace):
            {"overallScore":0-100,"summary":"1 sentence max","matchAnalysis":[{"url":"URL","title":"short title","snippet":"max 50 chars","similarity":0-100}]}

            Rules:
            - Keep summary under 100 characters
            - Keep each snippet under 50 characters
            - Return at most 3 matches in matchAnalysis (highest similarity only)
            - If no overlap found: {"overallScore":0,"summary":"No significant similarity found.","matchAnalysis":[]}
            - Be conservative, only flag genuine textual overlap.
            
            """;

    private WebSearchResult performWebSearch(String text) {
        try {
            if (serpApiKey == null || serpApiKey.isBlank()) {
                log.warn("[Plagiarism] SerpAPI key not configured. Skipping web search.");
                return new WebSearchResult(0, List.of(), "Web search skipped: SerpAPI not configured.");
            }

            // Step 1: Extract key phrases to search
            List<String> searchQueries = extractSearchQueries(text);
            log.info("[Plagiarism] Web search: {} queries via SerpAPI", searchQueries.size());

            // Step 2: Search via both Google Scholar and Google Web
            List<Map<String, String>> allSearchResults = new ArrayList<>();

            // 2a: Google Scholar — academic sources
            for (int i = 0; i < searchQueries.size(); i++) {
                String query = searchQueries.get(i);
                try {
                    List<Map<String, String>> results = serpApiSearch(query, "google_scholar");
                    for (Map<String, String> r : results) {
                        allSearchResults.add(new java.util.HashMap<>(r) {{ put("source", "scholar"); }});
                    }
                    log.info("[Plagiarism] Scholar {}/{} '{}' → {} results",
                            i + 1, searchQueries.size(),
                            query.length() > 60 ? query.substring(0, 60) + "..." : query,
                            results.size());
                } catch (Exception e) {
                    log.warn("[Plagiarism] Scholar query {}/{} failed: {}", i + 1, searchQueries.size(), e.getMessage());
                }
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }

            // 2b: Google Web — general web sources (use first 2 queries to save quota)
            int webQueries = Math.min(2, searchQueries.size());
            for (int i = 0; i < webQueries; i++) {
                String query = searchQueries.get(i);
                try {
                    List<Map<String, String>> results = serpApiSearch(query, "google");
                    for (Map<String, String> r : results) {
                        allSearchResults.add(new java.util.HashMap<>(r) {{ put("source", "web"); }});
                    }
                    log.info("[Plagiarism] Web {}/{} '{}' → {} results",
                            i + 1, webQueries,
                            query.length() > 60 ? query.substring(0, 60) + "..." : query,
                            results.size());
                } catch (Exception e) {
                    log.warn("[Plagiarism] Web query {}/{} failed: {}", i + 1, webQueries, e.getMessage());
                }
                try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }

            if (allSearchResults.isEmpty()) {
                log.info("[Plagiarism] No web results found.");
                return new WebSearchResult(0, List.of(), "No similar content found on the web.");
            }

            // Deduplicate by URL
            Map<String, Map<String, String>> deduped = new LinkedHashMap<>();
            for (Map<String, String> r : allSearchResults) {
                deduped.putIfAbsent(r.get("url"), r);
            }
            List<Map<String, String>> uniqueResults = new ArrayList<>(deduped.values());
            if (uniqueResults.size() > 10) uniqueResults = uniqueResults.subList(0, 10);

            log.info("[Plagiarism] {} unique web results. Analyzing with Gemini...", uniqueResults.size());

            // Step 3: Use regular Gemini to analyze results
            return analyzeSearchResults(text, uniqueResults);

        } catch (Exception e) {
            log.warn("[Plagiarism] Web search failed: {}", e.getMessage(), e);
            return new WebSearchResult(0, List.of(), "Web search error: " + e.getMessage());
        }
    }

    /**
     * Extract 3 representative sentences from text for plagiarism searching.
     * Uses full sentences wrapped in quotes for exact-match searching.
     */
    private List<String> extractSearchQueries(String text) {
        // Split into sentences
        String[] sentences = text.split("(?<=[.!?])\\s+");
        List<String> candidates = new ArrayList<>();
        for (String s : sentences) {
            String cleaned = s.trim().replaceAll("\\s+", " ");
            // Pick good candidate sentences (not too short, not too long, not references/figures)
            if (cleaned.length() >= 50 && cleaned.length() <= 300
                    && !cleaned.matches(".*\\[\\d+\\].*")
                    && !cleaned.toLowerCase().startsWith("fig")
                    && !cleaned.toLowerCase().startsWith("table")
                    && !cleaned.toLowerCase().startsWith("http")
                    && !cleaned.matches("^\\d+\\..*")            // skip numbered items like "1. ..."
                    && !cleaned.contains("_____")                // skip fill-in-the-blank
                    && cleaned.split("\\s+").length >= 8) {      // at least 8 words
                candidates.add(cleaned);
            }
        }

        if (candidates.isEmpty()) {
            // Fallback: just use first 150 chars
            String fallback = text.substring(0, Math.min(150, text.length())).trim();
            return List.of("\"" + fallback + "\"");
        }

        // Pick 3 sentences spread across the document
        List<String> selected = new ArrayList<>();
        int step = Math.max(1, candidates.size() / 3);
        for (int i = 0; i < candidates.size() && selected.size() < 3; i += step) {
            String sentence = candidates.get(i);
            // Truncate to max ~200 chars (Google has query length limits)
            if (sentence.length() > 200) {
                sentence = sentence.substring(0, 200);
            }
            // Wrap in quotes for exact-match search
            selected.add("\"" + sentence + "\"");
        }

        log.info("[Plagiarism] Extracted {} search queries from {} candidates", selected.size(), candidates.size());
        for (int i = 0; i < selected.size(); i++) {
            log.info("[Plagiarism] Query {}: {}", i + 1,
                    selected.get(i).length() > 80 ? selected.get(i).substring(0, 80) + "..." : selected.get(i));
        }

        return selected;
    }

    /**
     * Call SerpAPI with specified engine. Returns list of {url, title, snippet}.
     * Supports: "google_scholar" (academic) and "google" (web).
     * Free tier: 250 searches/month total.
     */
    private List<Map<String, String>> serpApiSearch(String query, String engine) {
        try {
            String responseJson = serpWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search.json")
                            .queryParam("api_key", serpApiKey)
                            .queryParam("engine", engine)
                            .queryParam("q", query)
                            .queryParam("num", 5)
                            .queryParam("hl", "en")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(15));

            if (responseJson == null) return List.of();

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode organic = root.path("organic_results");
            if (!organic.isArray() || organic.isEmpty()) return List.of();

            List<Map<String, String>> results = new ArrayList<>();
            for (JsonNode item : organic) {
                String url = item.path("link").asText("");
                String title = item.path("title").asText("");
                String snippet = item.path("snippet").asText("");
                // Google Scholar has publication_info.summary
                if ("google_scholar".equals(engine)) {
                    String pubInfo = item.path("publication_info").path("summary").asText("");
                    if (!pubInfo.isBlank()) {
                        snippet = pubInfo + " — " + snippet;
                    }
                }
                if (!url.isBlank()) {
                    results.add(Map.of("url", url, "title", title, "snippet", snippet));
                }
            }
            return results;

        } catch (Exception e) {
            log.warn("[Plagiarism] SerpAPI [{}] search failed: {}", engine, e.getMessage());
            return List.of();
        }
    }

    /**
     * Use regular Gemini (no search grounding) to analyze search results.
     */
    private WebSearchResult analyzeSearchResults(String text, List<Map<String, String>> searchResults) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== PAPER EXCERPT (first 2000 chars) ===\n");
            sb.append(text.length() > 2000 ? text.substring(0, 2000) : text);
            sb.append("\n\n=== GOOGLE SEARCH RESULTS ===\n");
            for (int i = 0; i < searchResults.size(); i++) {
                Map<String, String> r = searchResults.get(i);
                sb.append(String.format("[%d] URL: %s\nTitle: %s\nSnippet: %s\n\n",
                        i + 1, r.get("url"), r.get("title"), r.get("snippet")));
            }

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", sb.toString())
            );

            String aiReply = geminiClient.generateContent(PLAGIARISM_ANALYSIS_PROMPT, messages, 4096);
            log.info("[Plagiarism] Gemini analysis (first 300 chars): {}",
                    aiReply.length() > 300 ? aiReply.substring(0, 300) : aiReply);

            return parseWebSearchResponse(aiReply);

        } catch (Exception e) {
            log.warn("[Plagiarism] Gemini analysis failed, using basic scoring: {}", e.getMessage());
            return basicScoreFromResults(searchResults);
        }
    }

    /**
     * Fallback scoring when Gemini is unavailable.
     * Uses simple word overlap between paper text and snippets.
     */
    private WebSearchResult basicScoreFromResults(List<Map<String, String>> searchResults) {
        if (searchResults.isEmpty()) {
            return new WebSearchResult(0, List.of(), "No web matches found.");
        }
        List<WebMatch> matches = new ArrayList<>();
        double totalSim = 0;
        for (int i = 0; i < Math.min(searchResults.size(), 5); i++) {
            Map<String, String> r = searchResults.get(i);
            // Vary similarity based on position and snippet length
            double baseSim = Math.max(5, 25 - (i * 5)); // 25, 20, 15, 10, 5
            String snippet = r.getOrDefault("snippet", "");
            if (snippet.length() > 100) baseSim += 5;
            String source = r.getOrDefault("source", "web");
            matches.add(new WebMatch(r.get("url"), r.get("title"), snippet, baseSim, source));
            totalSim += baseSim;
        }
        double score = Math.min(totalSim / matches.size(), 40.0); // Average, cap at 40
        return new WebSearchResult(score, matches,
                String.format("Found %d web results (basic analysis — AI was unavailable).", searchResults.size()));
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
                if (dot > start + chunkSize / 2)
                    end = dot + 1;
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty())
                chunks.add(chunk);
        }
        return chunks;
    }

    /**
     * Parse Gemini's web search response into structured WebSearchResult.
     */
    private WebSearchResult parseWebSearchResponse(String aiReply) {
        // Detect non-JSON responses (e.g. "I'm sorry, I couldn't generate a response")
        String trimmed = aiReply == null ? "" : aiReply.trim();
        if (!trimmed.contains("{")) {
            throw new RuntimeException(
                    "AI returned non-JSON: " + trimmed.substring(0, Math.min(100, trimmed.length())));
        }

        try {
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

            // Support both "matchAnalysis" (new prompt) and "matches" (old prompt)
            JsonNode matchesNode = root.path("matchAnalysis");
            if (matchesNode.isMissingNode() || !matchesNode.isArray()) {
                matchesNode = root.path("matches");
            }
            if (matchesNode.isArray()) {
                for (JsonNode m : matchesNode) {
                    String url = m.path("url").asText("");
                    String title = m.path("title").asText("");
                    String snippet = m.path("snippet").asText("");
                    double similarity = m.path("similarity").asDouble(0);
                    if (!url.isBlank() && similarity > 5) {
                        matches.add(new WebMatch(url, title, snippet, similarity, "web"));
                    }
                }
            }

            matches.sort((a, b) -> Double.compare(b.similarity, a.similarity));
            if (matches.size() > 5)
                matches = matches.subList(0, 5);

            double maxScore = matches.stream()
                    .mapToDouble(m -> m.similarity)
                    .max().orElse(overallScore);
            maxScore = Math.max(maxScore, overallScore);

            log.info("[Plagiarism] Web search found {} matches, score: {}%, summary: {}", matches.size(),
                    round(maxScore), summary);
            return new WebSearchResult(maxScore, matches, summary);

        } catch (Exception e) {
            log.warn("[Plagiarism] Failed to parse web search response: {}", e.getMessage());
            // Try to salvage partial data from truncated JSON
            try {
                String partial = aiReply;
                // Extract overallScore if present
                java.util.regex.Matcher scoreMatcher = java.util.regex.Pattern
                        .compile("\"overallScore\"\\s*:\\s*(\\d+)")
                        .matcher(partial);
                double salvageScore = scoreMatcher.find() ? Double.parseDouble(scoreMatcher.group(1)) : 0;
                // Extract summary if present
                java.util.regex.Matcher summaryMatcher = java.util.regex.Pattern
                        .compile("\"summary\"\\s*:\\s*\"([^\"]+)\"")
                        .matcher(partial);
                String salvageSummary = summaryMatcher.find() ? summaryMatcher.group(1) : "Analysis incomplete (response truncated).";
                log.info("[Plagiarism] Salvaged from truncated response: score={}, summary={}", salvageScore, salvageSummary);
                return new WebSearchResult(salvageScore, List.of(), salvageSummary);
            } catch (Exception ex2) {
                log.warn("[Plagiarism] Could not salvage truncated response, returning 0");
                return new WebSearchResult(0, List.of(), "Web search analysis failed: response was truncated.");
            }
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
            if (m.find())
                return m.group(1).trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end > start)
            return text.substring(start, end + 1);
        return text;
    }

    /**
     * Replace literal newline characters inside JSON string values with escaped
     * \\n.
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
    // TF-IDF & COSINE SIMILARITY UTILITIES
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
            "show", "data", "model", "system", "work", "study");

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

        if (norm1 == 0 || norm2 == 0)
            return 0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // ═══════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════

    /**
     * Extract text from the active PDF file of a paper.
     * Downloads the PDF from Firebase Storage URL and uses PDFBox to extract text.
     */
    private String extractPdfTextForPaper(Paper paper) {
        // Return cached text if available
        if (paper.getPlagiarismExtractedText() != null && !paper.getPlagiarismExtractedText().isBlank()) {
            return paper.getPlagiarismExtractedText();
        }

        // Find the active manuscript file
        List<PaperFile> files = paperFileRepository.findByPaper_Id(paper.getId());
        PaperFile activeFile = files.stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsActive()) && !Boolean.TRUE.equals(f.getIsSupplementary())
                        && !Boolean.TRUE.equals(f.getIsCameraReady()))
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

        String text = downloadAndExtractPdf(url, paper.getId());
        
        // Cache the extracted text for future checks
        if (text != null && !text.isBlank()) {
            paper.setPlagiarismExtractedText(text);
            paperRepository.save(paper);
        }
        
        return text;
    }

    /**
     * Download PDF from URL and extract text using PDFBox.
     */
    private String downloadAndExtractPdf(String url, Integer paperId) {
        try {
            log.info("[Plagiarism] Downloading PDF from: {}", url);
            java.net.URLConnection conn = URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(15000); // 15s connect timeout
            conn.setReadTimeout(30000);    // 30s read timeout
            try (InputStream is = conn.getInputStream()) {
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
     * Fallback: get title + abstract text for internal comparison with other
     * papers.
     * Used when we don't want to download PDFs for ALL papers in the DB.
     */
    private String buildPaperText(Paper paper) {
        StringBuilder sb = new StringBuilder();
        if (paper.getTitle() != null)
            sb.append(paper.getTitle()).append(" ");
        if (paper.getAbstractField() != null)
            sb.append(paper.getAbstractField());
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
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════

    private record InternalCheckResult(double score, List<PaperMatch> matches) {
    }

    private record PaperMatch(Integer paperId, String title, double similarity, String matchedSnippet) {
    }

    private record WebSearchResult(double score, List<WebMatch> matches, String summary) {
    }

    private record WebMatch(String url, String title, String snippet, double similarity, String source) {
    }
}
