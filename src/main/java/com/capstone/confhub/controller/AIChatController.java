package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.AIChatRequest;
import com.capstone.confhub.dto.response.AIChatResponse;
import com.capstone.confhub.dto.response.ManuscriptAnalysisResponse;
import com.capstone.confhub.security.services.UserDetailsImpl;
import com.capstone.confhub.service.impl.AIChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService chatService;

    /**
     * POST /api/v1/ai/chat
     * Send a chat message and receive an AI response with context.
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AIChatResponse> chat(@Valid @RequestBody AIChatRequest request) {
        Integer userId = getCurrentUserId();
        AIChatResponse response = chatService.chat(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/ai/analyze-manuscript
     * Upload a PDF manuscript for AI analysis and conference matching.
     */
    @PostMapping("/analyze-manuscript")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ManuscriptAnalysisResponse> analyzeManuscript(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sessionId", required = false, defaultValue = "default") String sessionId
    ) {
        Integer userId = getCurrentUserId();
        ManuscriptAnalysisResponse response = chatService.analyzeManuscript(userId, file, sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/ai/history?sessionId=xxx
     * Retrieve chat history for a session.
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getChatHistory(
            @RequestParam("sessionId") String sessionId
    ) {
        Integer userId = getCurrentUserId();
        List<Map<String, Object>> history = chatService.getChatHistory(userId, sessionId);
        return ResponseEntity.ok(history);
    }

    private Integer getCurrentUserId() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return userDetails.getId();
    }
}
