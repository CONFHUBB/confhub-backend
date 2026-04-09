package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.AIChatRequest;
import com.capstone.confhub.dto.response.AIChatResponse;
import com.capstone.confhub.dto.response.ManuscriptAnalysisResponse;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.integration.GeminiApiClient;
import com.capstone.confhub.repository.*;
import com.capstone.confhub.utils.enums.ActivityType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatService {

    private final GeminiApiClient geminiClient;
    private final ChatMessageRepository chatMessageRepo;
    private final UserRepository userRepo;
    private final ConferenceRepository conferenceRepo;
    private final ConferenceTrackRepository trackRepo;
    private final ConferenceActivityRepository activityRepo;
    private final ConferenceUserTrackRepository userTrackRepo;
    private final SubjectAreaRepository subjectAreaRepo;
    private final ObjectMapper objectMapper;

    @Value("${ai.rate-limit.max-chat-per-minute:10}")
    private int maxChatPerMinute;

    @Value("${ai.rate-limit.max-analysis-per-day:20}")
    private int maxAnalysisPerDay;

    private static final int MAX_PDF_TEXT_LENGTH = 6000;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ═══════════════════════════════════════════════════════
    // CHAT
    // ═══════════════════════════════════════════════════════

    @Transactional
    public AIChatResponse chat(Integer userId, AIChatRequest request) {
        // 1. Rate limit check
        enforceRateLimit(userId, "CHAT");

        // 2. Load user
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // 3. Build system prompt with context
        String systemPrompt = buildSystemPrompt(user, request.getConferenceId());

        // 4. Load conversation history from DB
        List<ChatMessage> history = chatMessageRepo
                .findTop20ByUser_IdAndSessionIdOrderByCreatedAtDesc(userId, request.getSessionId());
        Collections.reverse(history); // oldest first

        // 5. Build messages list: history + current message
        List<Map<String, String>> rawMessages = new ArrayList<>();
        for (ChatMessage h : history) {
            rawMessages.add(Map.of("role", h.getRole(), "content", h.getContent()));
        }
        rawMessages.add(Map.of("role", "user", "content", request.getMessage()));
        
        List<Map<String, String>> messages = cleanMessagesForGemini(rawMessages);

        // 6. Call Gemini
        String aiReply = geminiClient.generateContent(systemPrompt, messages);

        // 7. Detect intent
        String intent = detectIntent(request.getMessage(), aiReply);

        // 8. Generate suggested actions
        List<AIChatResponse.ActionSuggestion> actions = generateSuggestedActions(intent, request.getConferenceId(), aiReply);

        // 9. Save messages to DB
        saveMessage(user, request.getSessionId(), "user", request.getMessage(), intent, request.getConferenceId());
        saveMessage(user, request.getSessionId(), "model", aiReply, intent, request.getConferenceId());

        // 10. Return response
        return AIChatResponse.builder()
                .reply(aiReply)
                .intent(intent)
                .sessionId(request.getSessionId())
                .suggestedActions(actions)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // MANUSCRIPT ANALYSIS
    // ═══════════════════════════════════════════════════════

    @Transactional
    public ManuscriptAnalysisResponse analyzeManuscript(Integer userId, MultipartFile file, String sessionId) {
        // 1. Rate limit check
        enforceRateLimit(userId, "ANALYZE");

        // 2. Validate file
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please upload a PDF file");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are accepted");
        }

        // 3. Extract text from PDF
        String pdfText = extractTextFromPdf(file);
        if (pdfText.isBlank()) {
            throw new BadRequestException(
                    "Could not extract text from the PDF. The file may be image-based or corrupted.");
        }

        // 4. Query all conferences accepting submissions
        List<Map<String, Object>> conferenceCtx = buildConferenceListForMatching();

        if (conferenceCtx.isEmpty()) {
            // Still analyze the manuscript even if no conferences are accepting
            String analysisOnlyPrompt = buildManuscriptAnalysisPrompt(pdfText, List.of());
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", analysisOnlyPrompt));
            String aiReply = geminiClient.generateContent(buildManuscriptSystemPrompt(), messages);
            ManuscriptAnalysisResponse response = parseManuscriptResponse(aiReply);
            // Override recommendations with empty + clear message
            response = ManuscriptAnalysisResponse.builder()
                    .summary(response.getSummary())
                    .detectedKeywords(response.getDetectedKeywords())
                    .detectedArea(response.getDetectedArea())
                    .recommendations(List.of())
                    .build();

            User user = userRepo.findById(userId).orElse(null);
            if (user != null && sessionId != null) {
                saveMessage(user, sessionId, "user",
                        "[Uploaded manuscript: " + filename + "]", "ANALYZE", null);
                saveMessage(user, sessionId, "model",
                        "\uD83D\uDCC4 **Manuscript Analysis Complete**\n\n" + response.getSummary()
                                + "\n\n⚠️ *No conferences are currently accepting paper submissions. Check back later or create your own conference.*",
                        "ANALYZE", null);
            }
            return response;
        }

        // 5. Build analysis prompt
        String analysisPrompt = buildManuscriptAnalysisPrompt(pdfText, conferenceCtx);

        // 6. Call Gemini
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", analysisPrompt));
        String aiReply = geminiClient.generateContent(buildManuscriptSystemPrompt(), messages);

        // 7. Parse structured response
        ManuscriptAnalysisResponse response = parseManuscriptResponse(aiReply);

        // 8. Save to chat history
        User user = userRepo.findById(userId).orElse(null);
        if (user != null && sessionId != null) {
            saveMessage(user, sessionId, "user",
                    "[Uploaded manuscript: " + filename + "]", "ANALYZE", null);
            saveMessage(user, sessionId, "model",
                    "📄 **Manuscript Analysis Complete**\n\n" + response.getSummary(), "ANALYZE", null);
        }

        return response;
    }

    // ═══════════════════════════════════════════════════════
    // CHAT HISTORY
    // ═══════════════════════════════════════════════════════

    public List<Map<String, Object>> getChatHistory(Integer userId, String sessionId) {
        List<ChatMessage> messages = chatMessageRepo
                .findBySessionIdOrderByCreatedAtAsc(sessionId);
        // Verify ownership
        return messages.stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    map.put("intent", m.getIntent());
                    map.put("createdAt", m.getCreatedAt().format(DT_FMT));
                    return map;
                })
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════

    /**
     * Build the system prompt with user + conference context injection.
     */
    private String buildSystemPrompt(User user, Integer conferenceId) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                        You are ConfHub AI, an intelligent assistant for the ConfHub Scientific Conference Management System.

                        ## Your Capabilities:
                        1. Guide users through their conference workflow step-by-step
                        2. Explain system features, processes, and business rules
                        3. Help with paper submission, review, and management tasks
                        4. Provide actionable recommendations based on current context

                        ## Conference Lifecycle (8 phases in strict order):
                        1. **Paper Submission** — Authors submit manuscripts with title, abstract, keywords, PDF
                        2. **Reviewer Bidding** — Reviewers select subject areas, then bid on papers (Eager/Willing/In a pinch/Not willing)
                        3. **Review Submission** — Assigned reviewers write structured reviews with scores
                        4. **Review Discussion** — Reviewers discuss, post meta-reviews, finalize assessments
                        5. **Author Notification** — Chair sends accept/reject decisions
                        6. **Camera-Ready Submission** — Accepted authors upload final versions
                        7. **Registration** — Attendees register and purchase tickets (VNPay)
                        8. **Event Day** — Conference sessions run according to program schedule

                        ## User Roles:
                        - **Conference Chair** — Full control: create conference, manage tracks, assign roles, configure timeline
                        - **Program Chair** — Manage reviews: assign reviewers, monitor review progress, notify authors
                        - **Reviewer** — Bid on papers, write reviews, participate in discussions
                        - **Author** — Submit papers, upload camera-ready, track paper status
                        - **Attendee** — Register, purchase tickets, attend sessions

                        ## Response Rules:
                        1. Always respond in the SAME LANGUAGE as the user's message
                        2. Use Markdown formatting (headers, bullets, bold, code blocks)
                        3. Always provide specific, actionable next steps
                        4. Be concise but thorough — aim for clear, helpful answers
                        5. When unsure, say so and suggest where to find the answer
                        """);

        // Inject user context
        sb.append("\n## Current User:\n");
        sb.append("- Name: ").append(user.getFirstName()).append(" ").append(user.getLastName()).append("\n");
        sb.append("- Email: ").append(user.getEmail()).append("\n");

        // Inject user's conference memberships
        try {
            var memberships = userTrackRepo.findByUser_Id(user.getId());
            if (!memberships.isEmpty()) {
                sb.append("\n## User's Conference Memberships:\n");
                Map<Integer, List<String>> confRoles = new LinkedHashMap<>();
                for (var m : memberships) {
                    int cId = m.getConference().getId();
                    String cName = m.getConference().getName();
                    String role = m.getAssignedRole().name();
                    confRoles.computeIfAbsent(cId, k -> new ArrayList<>()).add(cName + " → " + role);
                }
                confRoles.forEach((cId, roles) -> {
                    sb.append("- Conference #").append(cId).append(": ").append(String.join(", ", roles)).append("\n");
                });
            }
        } catch (Exception e) {
            log.warn("Could not load user memberships: {}", e.getMessage());
        }

        // Inject specific conference context if provided
        if (conferenceId != null) {
            try {
                var conf = conferenceRepo.findById(conferenceId).orElse(null);
                if (conf != null) {
                    sb.append("\n## Current Conference Context:\n");
                    sb.append("- Name: ").append(conf.getName()).append(" (").append(conf.getAcronym()).append(")\n");
                    sb.append("- Status: ").append(conf.getStatus()).append("\n");
                    sb.append("- Location: ").append(conf.getLocation()).append("\n");
                    sb.append("- Dates: ").append(conf.getStartDate().format(DT_FMT))
                            .append(" → ").append(conf.getEndDate().format(DT_FMT)).append("\n");

                    // Tracks
                    var tracks = trackRepo.findByConferenceId(conferenceId);
                    if (!tracks.isEmpty()) {
                        sb.append("- Tracks: ");
                        sb.append(tracks.stream().map(ConferenceTrack::getName).collect(Collectors.joining(", ")));
                        sb.append("\n");
                    }

                    // Activities / Timeline
                    var activities = activityRepo.findByConferenceId(conferenceId);
                    if (!activities.isEmpty()) {
                        sb.append("- Activity Timeline:\n");
                        for (var act : activities) {
                            String status = act.getIsEnabled() ? "✅ ENABLED" : "⬜ DISABLED";
                            String deadline = act.getDeadline() != null ? act.getDeadline().format(DT_FMT)
                                    : "No deadline";
                            sb.append("  - ").append(act.getActivityType()).append(": ")
                                    .append(status).append(" | Deadline: ").append(deadline).append("\n");
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not load conference context: {}", e.getMessage());
            }
        }

        // Inject ALL conferences and their current phase
        try {
            List<Conference> allConfs = conferenceRepo.findAll();
            if (!allConfs.isEmpty()) {
                sb.append("\n## All Conferences on ConfHub:\n");
                for (Conference conf : allConfs) {
                    sb.append("- **").append(conf.getName());
                    if (conf.getAcronym() != null && !conf.getAcronym().isBlank()) {
                        sb.append(" (").append(conf.getAcronym()).append(")");
                    }
                    sb.append("** [ID: ").append(conf.getId()).append("]\n");
                    sb.append("  Status: ").append(conf.getStatus()).append("\n");

                    var activities = activityRepo.findByConferenceId(conf.getId());
                    var currentPhase = activities.stream()
                            .filter(ConferenceActivity::getIsEnabled)
                            .map(a -> a.getActivityType().name())
                            .collect(Collectors.joining(", "));
                    sb.append("  Active Phase(s): ").append(currentPhase.isEmpty() ? "None" : currentPhase).append("\n");

                    var subActivity = activities.stream()
                            .filter(a -> a.getActivityType() == ActivityType.PAPER_SUBMISSION && a.getIsEnabled())
                            .findFirst().orElse(null);
                    if (subActivity != null) {
                        sb.append("  📌 ACCEPTING SUBMISSIONS");
                        if (subActivity.getDeadline() != null) {
                            sb.append(" (Deadline: ").append(subActivity.getDeadline().format(DT_FMT)).append(")");
                        }
                        sb.append("\n");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not load conference list: {}", e.getMessage());
        }

        return sb.toString();
    }

    /**
     * System prompt specifically for manuscript analysis.
     */
    private String buildManuscriptSystemPrompt() {
        return """
                You are ConfHub AI, specialized in analyzing academic manuscripts and matching them with suitable conferences.

                Your task is to analyze a manuscript's content and match it with available conferences.

                You MUST respond with ONLY valid JSON (no markdown fences, no explanation outside JSON) in this exact format:
                {
                  "summary": "Brief 2-3 sentence summary of the manuscript",
                  "detectedKeywords": ["keyword1", "keyword2", "keyword3"],
                  "detectedArea": "Primary research area (e.g., Machine Learning, Computer Vision, NLP)",
                  "recommendations": [
                    {
                      "conferenceId": 1,
                      "matchScore": 0.85,
                      "matchReason": "Why this conference is a good match in 1-2 sentences",
                      "matchingTracks": ["Track Name 1"]
                    }
                  ]
                }

                Rules:
                1. matchScore must be between 0.0 and 1.0
                2. Sort recommendations by matchScore descending (best match first)
                3. Only include conferences with matchScore >= 0.3
                4. Maximum 5 recommendations
                5. Be specific in matchReason — reference the manuscript's topic vs conference focus
                6. detectedKeywords should have 3-8 items
                """;
    }

    /**
     * Build the user prompt for manuscript analysis.
     */
    private String buildManuscriptAnalysisPrompt(String pdfText, List<Map<String, Object>> conferences) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Manuscript Content (extracted from PDF):\n\n");
        sb.append(pdfText);
        sb.append("\n\n## Available Conferences Accepting Submissions:\n\n");

        try {
            sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(conferences));
        } catch (Exception e) {
            sb.append(conferences.toString());
        }

        sb.append(
                "\n\nAnalyze the manuscript above and match it with the most suitable conferences. Respond with JSON only.");
        return sb.toString();
    }

    /**
     * Query all conferences that currently have PAPER_SUBMISSION enabled.
     */
    private List<Map<String, Object>> buildConferenceListForMatching() {
        List<Map<String, Object>> result = new ArrayList<>();

        List<Conference> allConferences = conferenceRepo.findAll();
        for (Conference conf : allConferences) {
            // Check if PAPER_SUBMISSION activity is enabled
            var activities = activityRepo.findByConferenceId(conf.getId());
            var submissionActivity = activities.stream()
                    .filter(a -> a.getActivityType() == ActivityType.PAPER_SUBMISSION)
                    .findFirst().orElse(null);

            if (submissionActivity == null || !submissionActivity.getIsEnabled())
                continue;

            Map<String, Object> cMap = new LinkedHashMap<>();
            cMap.put("conferenceId", conf.getId());
            cMap.put("name", conf.getName());
            cMap.put("acronym", conf.getAcronym());
            cMap.put("description", conf.getDescription());
            cMap.put("area", conf.getArea());
            cMap.put("location", conf.getLocation());
            cMap.put("deadline", submissionActivity.getDeadline() != null
                    ? submissionActivity.getDeadline().format(DT_FMT)
                    : "No deadline set");
            cMap.put("status", "OPEN");

            // Add tracks & subject areas
            var tracks = trackRepo.findByConferenceId(conf.getId());
            List<Map<String, Object>> trackList = new ArrayList<>();
            for (var t : tracks) {
                Map<String, Object> tMap = new LinkedHashMap<>();
                tMap.put("trackName", t.getName());
                var sas = subjectAreaRepo.findByTrackId(t.getId());
                tMap.put("subjectAreas", sas.stream().map(SubjectArea::getName).collect(Collectors.toList()));
                trackList.add(tMap);
            }
            cMap.put("tracks", trackList);

            result.add(cMap);
        }
        return result;
    }

    /**
     * Extract text from uploaded PDF using Apache PDFBox.
     * Truncates to MAX_PDF_TEXT_LENGTH to stay within token limits.
     */
    private String extractTextFromPdf(MultipartFile file) {
        try {
            PDDocument doc = Loader.loadPDF(file.getBytes());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            doc.close();

            // Clean up: remove excessive whitespace
            text = text.replaceAll("\\s{3,}", "\n\n").trim();

            // Truncate if too long, keeping the beginning (abstract + intro)
            if (text.length() > MAX_PDF_TEXT_LENGTH) {
                text = text.substring(0, MAX_PDF_TEXT_LENGTH) + "\n\n[... truncated ...]";
            }

            return text;
        } catch (Exception e) {
            log.error("[PDF] Failed to extract text: {}", e.getMessage());
            throw new BadRequestException("Could not read the PDF file: " + e.getMessage());
        }
    }

    /**
     * Parse the AI's JSON response into ManuscriptAnalysisResponse.
     */
    private ManuscriptAnalysisResponse parseManuscriptResponse(String aiReply) {
        try {
            // Extract the actual JSON block in case there is surrounding Markdown or text
            String json = aiReply.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start != -1 && end != -1 && end > start) {
                json = json.substring(start, end + 1);
            }

            JsonNode root = objectMapper.readTree(json);

            String summary = root.path("summary").asText("No summary available");
            List<String> keywords = new ArrayList<>();
            root.path("detectedKeywords").forEach(n -> keywords.add(n.asText()));
            String area = root.path("detectedArea").asText("General");

            List<ManuscriptAnalysisResponse.ConferenceMatch> recommendations = new ArrayList<>();
            for (JsonNode rec : root.path("recommendations")) {
                int confId = rec.path("conferenceId").asInt();
                // Enrich with actual conference data
                var conf = conferenceRepo.findById(confId).orElse(null);

                List<String> matchingTracks = new ArrayList<>();
                rec.path("matchingTracks").forEach(n -> matchingTracks.add(n.asText()));

                // Get deadline
                String deadline = "N/A";
                if (conf != null) {
                    var acts = activityRepo.findByConferenceId(confId);
                    var subAct = acts.stream()
                            .filter(a -> a.getActivityType() == ActivityType.PAPER_SUBMISSION)
                            .findFirst().orElse(null);
                    if (subAct != null && subAct.getDeadline() != null) {
                        deadline = subAct.getDeadline().format(DT_FMT);
                    }
                }

                recommendations.add(ManuscriptAnalysisResponse.ConferenceMatch.builder()
                        .conferenceId(confId)
                        .conferenceName(conf != null ? conf.getName() : "Unknown")
                        .acronym(conf != null ? conf.getAcronym() : "")
                        .matchScore(rec.path("matchScore").asDouble(0))
                        .matchReason(rec.path("matchReason").asText(""))
                        .matchingTracks(matchingTracks)
                        .deadline(deadline)
                        .status("OPEN")
                        .build());
            }

            return ManuscriptAnalysisResponse.builder()
                    .summary(summary)
                    .detectedKeywords(keywords)
                    .detectedArea(area)
                    .recommendations(recommendations)
                    .build();

        } catch (Exception e) {
            log.error("[AI] Failed to parse manuscript analysis response: {}", e.getMessage());
            log.debug("[AI] Raw response: {}", aiReply);
            // Fallback: return the raw text as summary
            return ManuscriptAnalysisResponse.builder()
                    .summary(aiReply)
                    .detectedKeywords(List.of())
                    .detectedArea("Unknown")
                    .recommendations(List.of())
                    .build();
        }
    }

    /**
     * Simple intent detection from user message.
     */
    private String detectIntent(String userMessage, String aiReply) {
        String lower = userMessage.toLowerCase();
        if (lower.contains("manuscript") || lower.contains("upload") || lower.contains("analyze")
                || lower.contains("phân tích"))
            return "ANALYZE";
        if (lower.contains("step") || lower.contains("bước") || lower.contains("guide") || lower.contains("hướng dẫn")
                || lower.contains("what should") || lower.contains("làm gì") || lower.contains("tiếp theo") || lower.contains("tasks") || lower.contains("nhiệm vụ"))
            return "GUIDE";
        if (lower.contains("submit") || lower.contains("nộp") || lower.contains("conference")
                || lower.contains("hội nghị") || lower.contains("recommend"))
            return "RECOMMEND";
        return "GENERAL";
    }

    /**
     * Generate contextual quick-action buttons for the frontend.
     */
    private List<AIChatResponse.ActionSuggestion> generateSuggestedActions(String intent, Integer conferenceId, String aiReply) {
        List<AIChatResponse.ActionSuggestion> actions = new ArrayList<>();
        Set<String> addedLabels = new HashSet<>();

        switch (intent) {
            case "GUIDE" -> {
                if (conferenceId != null) {
                    actions.add(createAction(addedLabels, "📋 What should I do next?", "CHAT", "Based on my role and current phase of this conference, what should I do next?"));
                } else {
                    actions.add(createAction(addedLabels, "📋 View upcoming tasks", "CHAT", "Across all my tracked conferences, what are my pending roles and next steps?"));
                }
            }
            case "RECOMMEND" -> {
                actions.add(createAction(addedLabels, "📄 Analyze my manuscript", "UPLOAD", "analyze-manuscript"));
            }
        }

        // Always suggest viewing the current conference if in context
        if (conferenceId != null) {
            actions.add(createAction(addedLabels, "🔍 Chi tiết hội nghị hiện tại", "NAVIGATE", "/conference/" + conferenceId));
        }

        // Dynamically parse aiReply for mentioned conferences
        if (aiReply != null) {
            List<Conference> allConfs = conferenceRepo.findAll();
            for (Conference c : allConfs) {
                // If AI mentioned the conference name or acronym
                if (aiReply.contains(c.getName()) || 
                   (c.getAcronym() != null && !c.getAcronym().isBlank() && aiReply.contains(c.getAcronym()))) {
                    
                    String acronym = c.getAcronym() != null && !c.getAcronym().isBlank() ? c.getAcronym() : "Hội nghị";

                    // Gather conference metadata for enriched cards
                    var activities = activityRepo.findByConferenceId(c.getId());
                    var subActivity = activities.stream()
                            .filter(a -> a.getActivityType() == ActivityType.PAPER_SUBMISSION && a.getIsEnabled())
                            .findFirst().orElse(null);
                    String deadline = subActivity != null && subActivity.getDeadline() != null
                            ? subActivity.getDeadline().format(DT_FMT) : null;
                    String dates = c.getStartDate().format(DT_FMT) + " → " + c.getEndDate().format(DT_FMT);

                    // Add "View" button with metadata
                    actions.add(createActionWithMeta(addedLabels, "🔍 Xem: " + acronym, "NAVIGATE",
                            "/conference/" + c.getId(), c, deadline, dates));

                    // If submission is open, add "Submit" button
                    boolean canSubmit = subActivity != null;
                    
                    if (canSubmit) {
                        actions.add(createActionWithMeta(addedLabels, "📄 Nộp bài: " + acronym, "NAVIGATE",
                                "/conference/" + c.getId(), c, deadline, dates));
                    }
                    // If registration is open, add "Register" button
                    boolean canRegister = activities.stream()
                            .anyMatch(a -> a.getActivityType() == ActivityType.REGISTRATION && a.getIsEnabled());
                    if (canRegister) {
                        actions.add(createActionWithMeta(addedLabels, "🎟 Đăng ký: " + acronym, "NAVIGATE",
                                "/conference/" + c.getId(), c, deadline, dates));
                    }
                }
            }
        }

        // Always add a browse conferences action
        if (actions.size() < 4) {
            actions.add(createAction(addedLabels, "🔍 Xem tất cả hội nghị", "NAVIGATE", "/conference"));
        }

        return actions;
    }

    private AIChatResponse.ActionSuggestion createAction(Set<String> set, String label, String action, String val) {
        if (!set.contains(label)) set.add(label);
        return AIChatResponse.ActionSuggestion.builder().label(label).action(action).value(val).build();
    }

    /**
     * Create an action enriched with conference metadata for rich card display.
     */
    private AIChatResponse.ActionSuggestion createActionWithMeta(
            Set<String> set, String label, String action, String val,
            Conference conf, String deadline, String dates) {
        if (!set.contains(label)) set.add(label);
        return AIChatResponse.ActionSuggestion.builder()
                .label(label)
                .action(action)
                .value(val)
                .conferenceId(conf.getId())
                .location(conf.getLocation())
                .area(conf.getArea())
                .deadline(deadline)
                .dates(dates)
                .build();
    }

    /**
     * Clean messages for Gemini to strictly alternate roles.
     * Gemini fails with 400 if two consecutive messages have the same role.
     */
    private List<Map<String, String>> cleanMessagesForGemini(List<Map<String, String>> raw) {
        if (raw == null || raw.isEmpty()) return raw;
        List<Map<String, String>> clean = new ArrayList<>();
        Map<String, String> lastMsg = null;
        for (Map<String, String> msg : raw) {
            if (lastMsg == null) {
                lastMsg = new LinkedHashMap<>(msg);
                clean.add(lastMsg);
            } else {
                if (lastMsg.get("role").equals(msg.get("role"))) {
                    // merge content to avoid 400 error
                    lastMsg.put("content", lastMsg.get("content") + "\n\n" + msg.get("content"));
                } else {
                    lastMsg = new LinkedHashMap<>(msg);
                    clean.add(lastMsg);
                }
            }
        }
        // Gemini MUST start with 'user'
        if (!clean.isEmpty() && !clean.get(0).get("role").equals("user")) {
            clean.remove(0);
        }
        return clean;
    }

    /**
     * Enforce rate limiting per user.
     */
    private void enforceRateLimit(Integer userId, String type) {
        if ("CHAT".equals(type)) {
            long recentChats = chatMessageRepo.countUserMessagesSince(
                    userId, LocalDateTime.now().minusMinutes(1));
            if (recentChats >= maxChatPerMinute) {
                throw new BadRequestException(
                        "Rate limit exceeded. Maximum " + maxChatPerMinute + " messages per minute. Please wait.");
            }
        } else if ("ANALYZE".equals(type)) {
            long recentAnalysis = chatMessageRepo.countAnalysisSince(
                    userId, LocalDateTime.now().minusDays(1));
            if (recentAnalysis >= maxAnalysisPerDay) {
                throw new BadRequestException(
                        "Analysis limit reached. Maximum " + maxAnalysisPerDay + " manuscript analyses per day.");
            }
        }
    }

    /**
     * Persist a chat message to the database.
     */
    private void saveMessage(User user, String sessionId, String role, String content, String intent,
            Integer conferenceId) {
        ChatMessage msg = ChatMessage.builder()
                .user(user)
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .intent(intent)
                .conferenceId(conferenceId)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepo.save(msg);
    }
}
