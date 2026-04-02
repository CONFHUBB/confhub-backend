package com.capstone.confhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for Google Custom Search JSON API.
 * Searches the web for n-gram queries extracted from paper text
 * and returns matching URLs with snippets for plagiarism analysis.
 */
@Component
@Slf4j
public class GoogleSearchClient {

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String searchEngineId;

    public GoogleSearchClient(
            @Value("${google.search.api-key:}") String apiKey,
            @Value("${google.search.engine-id:}") String searchEngineId
    ) {
        this.apiKey = apiKey;
        this.searchEngineId = searchEngineId;
        this.mapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl("https://www.googleapis.com/customsearch/v1")
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && searchEngineId != null && !searchEngineId.isBlank();
    }

    /**
     * Search Google for a query string.
     * Returns a list of SearchResult with title, link, and snippet.
     */
    public List<SearchResult> search(String query, int numResults) {
        if (!isConfigured()) {
            log.warn("[GoogleSearch] API key or Search Engine ID not configured. Skipping web search.");
            return List.of();
        }

        try {
            String responseJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("key", apiKey)
                            .queryParam("cx", searchEngineId)
                            .queryParam("q", query)
                            .queryParam("num", Math.min(numResults, 10))
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseSearchResults(responseJson);
        } catch (Exception e) {
            log.warn("[GoogleSearch] Search failed for query '{}': {}", truncate(query, 50), e.getMessage());
            return List.of();
        }
    }

    private List<SearchResult> parseSearchResults(String json) {
        List<SearchResult> results = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    results.add(new SearchResult(
                            item.path("title").asText(""),
                            item.path("link").asText(""),
                            item.path("snippet").asText("")
                    ));
                }
            }
        } catch (Exception e) {
            log.error("[GoogleSearch] Failed to parse response: {}", e.getMessage());
        }
        return results;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    public record SearchResult(String title, String link, String snippet) {}
}
