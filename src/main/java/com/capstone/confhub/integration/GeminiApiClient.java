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

import java.util.List;
import java.util.Map;

/**
 * HTTP client for Google Gemini REST API.
 * Uses generateContent endpoint with system instructions and multi-turn conversation.
 */
@Component
@Slf4j
public class GeminiApiClient {

    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;

    public GeminiApiClient(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-2.0-flash}") String model,
            @Value("${gemini.max-output-tokens:2048}") int maxOutputTokens
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.mapper = new ObjectMapper();
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    /**
     * Send a chat request to Gemini with system prompt and conversation history.
     *
     * @param systemPrompt The system instruction text
     * @param messages     List of {role, content} maps. role = "user" or "model"
     * @return The model's text response
     */
    public String generateContent(String systemPrompt, List<Map<String, String>> messages) {
        try {
            ObjectNode requestBody = buildRequestBody(systemPrompt, messages);

            String responseJson = webClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractTextFromResponse(responseJson);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("[GeminiAPI] Server returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            if (e.getStatusCode().value() == 429) {
                throw new RuntimeException("Gemini AI is currently busy (Rate Limit Exceeded). Please wait a few seconds and try again.", e);
            }
            throw new RuntimeException("AI Provider Error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("[GeminiAPI] Error calling Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Internal Error calling Google: " + e.getMessage(), e);
        }
    }

    /**
     * Build the JSON request body for Gemini API.
     *
     * Request format:
     * {
     *   "system_instruction": { "parts": [{ "text": "..." }] },
     *   "contents": [
     *     { "role": "user", "parts": [{ "text": "..." }] },
     *     { "role": "model", "parts": [{ "text": "..." }] }
     *   ],
     *   "generationConfig": { "maxOutputTokens": 2048 }
     * }
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
     * Extract the text response from Gemini API JSON response.
     * Response format: { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
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
