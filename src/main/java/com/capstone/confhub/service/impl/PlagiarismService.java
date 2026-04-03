package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.entity.PaperFile;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.integration.GoogleSearchClient;
import com.capstone.confhub.repository.PaperFileRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.utils.enums.PlagiarismStatus;
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
 * Plagiarism checking service with triple-layer approach:
 * 1. Internal: TF-IDF Cosine Similarity + N-gram snippet matching against all papers in DB
 * 2. Web Search: Google Custom Search API for finding similar content online
 * 2. Web Search: Google Custom Search API for finding similar content online
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlagiarismService {

    private final PaperRepository paperRepository;
    private final PaperFileRepository paperFileRepository;
    private final GoogleSearchClient googleSearchClient;
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

            // Phase 2: Web Search check (Google Custom Search API)
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
            // Extract key sentences/phrases for search queries
            List<String> queries = extractSearchQueries(text);
            log.info("[Plagiarism] Web search with {} queries", queries.size());
            List<WebMatch> allMatches = new ArrayList<>();

            for (String query : queries) {
                // Search 1: exact phrase match (wrap in quotes)
                try {
                    List<GoogleSearchClient.SearchResult> exactResults = googleSearchClient.search(
                            "\"" + query + "\"", 3);
                    for (GoogleSearchClient.SearchResult sr : exactResults) {
                        double similarity = calculateSnippetSimilarity(query, sr.snippet());
                        // Exact phrase match gets a boost
                        allMatches.add(new WebMatch(sr.link(), sr.title(), sr.snippet(), Math.max(similarity, 50.0)));
                    }
                } catch (Exception e) {
                    log.debug("[Plagiarism] Exact search failed for query: {}", e.getMessage());
                }

                // Search 2: keyword search (without quotes — broader match)
                try {
                    List<GoogleSearchClient.SearchResult> keywordResults = googleSearchClient.search(
                            query, 5);
                    for (GoogleSearchClient.SearchResult sr : keywordResults) {
                        double similarity = calculateSnippetSimilarity(query, sr.snippet());
                        if (similarity > 15.0) {
                            allMatches.add(new WebMatch(sr.link(), sr.title(), sr.snippet(), similarity));
                        }
                    }
                } catch (Exception e) {
                    log.debug("[Plagiarism] Keyword search failed for query: {}", e.getMessage());
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

            log.info("[Plagiarism] Web search found {} matches, max score: {}%", finalMatches.size(), round(maxScore));
            return new WebSearchResult(maxScore, finalMatches);

        } catch (Exception e) {
            log.warn("[Plagiarism] Web search failed: {}", e.getMessage());
            return new WebSearchResult(0, List.of());
        }
    }

    /**
     * Extract meaningful search queries from text (key sentences / phrases).
     * Improved: broader sentence filter, more queries, uses key n-word phrases.
     */
    private List<String> extractSearchQueries(String text) {
        // Clean text: normalize whitespace, remove references/numbers
        String cleanText = text.replaceAll("\\s+", " ").trim();

        // Split into sentences
        String[] sentences = cleanText.split("[.!?]+");
        List<String> queries = new ArrayList<>();

        for (String s : sentences) {
            String trimmed = s.trim();
            long wordCount = trimmed.split("\\s+").length;
            // Accept sentences from 5 to 40 words
            if (wordCount >= 5 && wordCount <= 40 && trimmed.length() >= 20) {
                // If sentence is long, take the first 15 words as query
                if (wordCount > 15) {
                    String[] words = trimmed.split("\\s+");
                    trimmed = String.join(" ", Arrays.copyOfRange(words, 0, Math.min(15, words.length)));
                }
                queries.add(trimmed);
            }
        }

        // Also extract the paper title as a query (from first line or first sentence)
        String firstLine = cleanText.split("\n")[0].trim();
        if (firstLine.length() >= 10 && firstLine.split("\\s+").length <= 20) {
            queries.add(0, firstLine); // Title as first query
        }

        // Limit to 5 queries to avoid rate limiting
        if (queries.size() > 5) {
            // Take first (title), then evenly sample from the rest
            List<String> sampled = new ArrayList<>();
            sampled.add(queries.get(0));
            int step = Math.max(1, (queries.size() - 1) / 4);
            for (int i = 1; i < queries.size() && sampled.size() < 5; i += step) {
                sampled.add(queries.get(i));
            }
            queries = sampled;
        }

        log.info("[Plagiarism] Extracted {} search queries from text", queries.size());
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
    private record WebSearchResult(double score, List<WebMatch> matches) {}
    private record WebMatch(String url, String title, String snippet, double similarity) {}
}
