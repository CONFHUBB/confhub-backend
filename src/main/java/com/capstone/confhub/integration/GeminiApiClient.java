package com.capstone.confhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * HTTP client for Google Gemini REST API.
 * Supports multiple API keys with smart rotation — automatically switches
 * to the next key when the current one hits a rate limit (429) or API error.
 */
@Component
@Slf4j
public class GeminiApiClient {

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final List<String> apiKeys;
    private final String model;
    private final int maxOutputTokens;
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    public GeminiApiClient(
            @Value("${gemini.api-keys:}") String apiKeysRaw,
            @Value("${gemini.model:gemini-2.0-flash}") String model,
            @Value("${gemini.max-output-tokens:2048}") int maxOutputTokens
    ) {
        this.apiKeys = Arrays.stream(apiKeysRaw.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .collect(Collectors.toList());

        if (this.apiKeys.isEmpty()) {
            log.warn("[GeminiAPI] No API keys configured! AI features will not work.");
        } else {
            log.info("[GeminiAPI] Loaded {} API key(s) for rotation", this.apiKeys.size());
        }

        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.mapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    /**
     * Get the next API key using round-robin rotation.
     * Thread-safe via AtomicInteger.
     */
    private String getNextKey() {
        if (apiKeys.isEmpty()) {
            throw new RuntimeException("No Gemini API keys configured. Please set gemini.api-keys in application.properties.");
        }
        int idx = keyIndex.getAndUpdate(i -> (i + 1) % apiKeys.size());
        return apiKeys.get(idx);
    }

    /**
     * Mask an API key for safe logging (show first 8 chars + last 4).
     */
    private String maskKey(String key) {
        if (key == null || key.length() < 16) return "***";
        return key.substring(0, 8) + "..." + key.substring(key.length() - 4);
    }

    /**
     * Send a chat request to Gemini with system prompt and conversation history.
     * On rate limit (429) or server error, automatically rotates to the next API key.
     *
     * @param systemPrompt The system instruction text
     * @param messages     List of {role, content} maps. role = "user" or "model"
     * @return The model's text response
     */
    public String generateContent(String systemPrompt, List<Map<String, String>> messages) {
        int totalAttempts = apiKeys.size() + 1; // Try each key at most once, plus one retry on the first

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            String currentKey = getNextKey();
            try {
                ObjectNode requestBody = buildRequestBody(systemPrompt, messages);

                log.info("[GeminiAPI] Request using key [{}], model [{}] (attempt {}/{})",
                        maskKey(currentKey), model, attempt, totalAttempts);

                String responseJson = webClient.post()
                        .uri("/models/{model}:generateContent?key={key}", model, currentKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody.toString())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                return extractTextFromResponse(responseJson);

            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                int statusCode = e.getStatusCode().value();
                log.error("[GeminiAPI] Key [{}] returned HTTP {}: {}",
                        maskKey(currentKey), statusCode, e.getResponseBodyAsString());

                if (statusCode == 429 || statusCode >= 500) {
                    // Rate limited or server error → rotate to next key
                    log.warn("[GeminiAPI] Key [{}] hit {} — rotating to next key (attempt {}/{})",
                            maskKey(currentKey),
                            statusCode == 429 ? "RATE LIMIT" : "SERVER ERROR " + statusCode,
                            attempt, totalAttempts);

                    if (attempt < totalAttempts) {
                        // Small delay before trying next key (500ms)
                        try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        continue;
                    }
                    throw new RuntimeException(
                            "Gemini AI is currently busy (all API keys exhausted). Please wait a moment and try again.", e);
                }

                // Non-retryable error (400, 403, etc.)
                throw new RuntimeException("AI Provider Error: " + e.getStatusCode(), e);

            } catch (Exception e) {
                log.error("[GeminiAPI] Key [{}] error: {}", maskKey(currentKey), e.getMessage(), e);

                if (attempt < totalAttempts) {
                    log.warn("[GeminiAPI] Rotating to next key after error (attempt {}/{})", attempt, totalAttempts);
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                throw new RuntimeException("Internal Error calling Google: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Gemini API failed after trying all " + apiKeys.size() + " API keys");
    }

    /**
     * Build the JSON request body for Gemini API.
     */
    private ObjectNode buildRequestBody(String systemPrompt, List<Map<String, String>> messages) {
        ObjectNode body = mapper.createObjectNode();

        // System instruction
        ObjectNode sysInstruction = mapper.createObjectNode();
        ArrayNode sysParts = mapper.createArrayNode();
        sysParts.add(mapper.createObjectNode().put("text", systemPrompt));
        sysInstruction.set("parts", sysParts);
        body.set("systemInstruction", sysInstruction);

        // Conversation contents
        ArrayNode contents = mapper.createArrayNode();
        for (Map<String, String> msg : messages) {
            ObjectNode content = mapper.createObjectNode();
            content.put("role", msg.get("role"));
            ArrayNode parts = mapper.createArrayNode();
            parts.add(mapper.createObjectNode().put("text", msg.get("content")));
            content.set("parts", parts);
            contents.add(content);
        }
        body.set("contents", contents);

        // Generation config
        ObjectNode genConfig = mapper.createObjectNode();
        genConfig.put("maxOutputTokens", maxOutputTokens);
        genConfig.put("temperature", 0.7);
        body.set("generationConfig", genConfig);

        return body;
    }

    /**
     * Send a request to Gemini WITH Google Search grounding enabled.
     * This allows the model to search the web for similar content.
     * Used for plagiarism web search.
     */
    public String generateContentWithSearch(String systemPrompt, List<Map<String, String>> messages) {
        int totalAttempts = apiKeys.size() + 1;

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            String currentKey = getNextKey();
            try {
                ObjectNode requestBody = buildRequestBody(systemPrompt, messages);

                // Add Google Search tool
                ArrayNode tools = mapper.createArrayNode();
                ObjectNode searchTool = mapper.createObjectNode();
                searchTool.set("google_search", mapper.createObjectNode());
                tools.add(searchTool);
                requestBody.set("tools", tools);

                // Lower temperature for factual search results
                ((ObjectNode) requestBody.get("generationConfig")).put("temperature", 0.1);

                log.info("[GeminiAPI] Search-grounded request using key [{}] (attempt {}/{})",
                        maskKey(currentKey), attempt, totalAttempts);

                String responseJson = webClient.post()
                        .uri("/models/{model}:generateContent?key={key}", model, currentKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody.toString())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                return extractTextFromResponse(responseJson);

            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                int statusCode = e.getStatusCode().value();
                log.error("[GeminiAPI] Search key [{}] returned HTTP {}", maskKey(currentKey), statusCode);
                if ((statusCode == 429 || statusCode >= 500) && attempt < totalAttempts) {
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                throw new RuntimeException("AI Search Error: HTTP " + statusCode, e);
            } catch (Exception e) {
                log.error("[GeminiAPI] Search key [{}] error: {}", maskKey(currentKey), e.getMessage());
                if (attempt < totalAttempts) {
                    try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                throw new RuntimeException("AI Search Error: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Gemini Search failed after trying all keys");
    }

    /**
     * Extract the text response from Gemini API JSON response.
     */
    private String extractTextFromResponse(String responseJson) {
        try {
            JsonNode root = mapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty() || !candidates.isArray()) {
                log.warn("[GeminiAPI] No candidates in response: {}", responseJson);
                return "I'm sorry, I couldn't generate a response. Please try again.";
            }
            JsonNode firstCandidate = candidates.get(0);
            JsonNode parts = firstCandidate.path("content").path("parts");
            if (parts.isEmpty() || !parts.isArray()) {
                return "I'm sorry, I couldn't generate a response. Please try again.";
            }
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : parts) {
                // Skip "thought" parts from gemini-2.5-flash (internal reasoning)
                if (part.has("thought") && part.get("thought").asBoolean(false)) {
                    continue;
                }
                if (part.has("text")) {
                    sb.append(part.get("text").asText());
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("[GeminiAPI] Error parsing response: {}", e.getMessage());
            return "I'm sorry, there was an error processing the AI response.";
        }
    }
}
