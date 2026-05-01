package com.capstone.confhub.controller;

import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceChat;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceChatRepository;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.service.WebSocketNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/conference-chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Conference Chat", description = "Real-time chat between TPC members (group + DM)")
public class ConferenceChatController {

    private final ConferenceChatRepository chatRepository;
    private final ConferenceRepository conferenceRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final UserRepository userRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final com.capstone.confhub.service.FirebaseStorageService firebaseStorageService;

    // ==================== GROUP CHAT ====================

    @GetMapping("/{conferenceId}/group")
    @Operation(summary = "Get group chat messages (latest 100)")
    public ResponseEntity<List<Map<String, Object>>> getGroupMessages(@PathVariable Integer conferenceId) {
        List<ConferenceChat> messages = chatRepository.findByConference_IdAndRecipientIdIsNullOrderByCreatedAtAsc(conferenceId);
        int start = Math.max(0, messages.size() - 100);
        List<Map<String, Object>> result = messages.subList(start, messages.size())
                .stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{conferenceId}/group")
    @Operation(summary = "Send a group chat message")
    public ResponseEntity<Map<String, Object>> sendGroupMessage(
            @PathVariable Integer conferenceId,
            @RequestBody Map<String, Object> body) {

        Integer userId = (Integer) body.get("userId");
        String content = (String) body.get("content");
        if (userId == null || content == null || content.isBlank()) {
            throw new BadRequestException("userId and content are required");
        }

        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long replyToId = body.get("replyToId") != null ? ((Number) body.get("replyToId")).longValue() : null;

        ConferenceChat chat = ConferenceChat.builder()
                .conference(conference)
                .user(user)
                .recipientId(null)
                .content(content.trim())
                .replyToId(replyToId)
                .createdAt(LocalDateTime.now())
                .build();

        ConferenceChat saved = chatRepository.save(chat);
        Map<String, Object> response = toMap(saved);

        // Broadcast via WebSocket to group topic
        webSocketNotificationService.broadcastChatMessage(conferenceId, response);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ==================== DIRECT MESSAGE (global, not conference-scoped) ====================

    @GetMapping("/dm/{otherUserId}")
    @Operation(summary = "Get DM messages between current user and another user (global)")
    public ResponseEntity<List<Map<String, Object>>> getDmMessages(
            @PathVariable Integer otherUserId,
            @RequestParam Integer userId) {
        List<ConferenceChat> messages = chatRepository.findDmMessagesGlobal(userId, otherUserId);
        List<Map<String, Object>> result = messages.stream().map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/dm")
    @Operation(summary = "Send a direct message to another user (global)")
    public ResponseEntity<Map<String, Object>> sendDm(
            @RequestBody Map<String, Object> body) {

        Integer userId = (Integer) body.get("userId");
        Integer recipientId = (Integer) body.get("recipientId");
        String content = (String) body.get("content");

        if (userId == null || recipientId == null || content == null || content.isBlank()) {
            throw new BadRequestException("userId, recipientId and content are required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        Long replyToId = body.get("replyToId") != null ? ((Number) body.get("replyToId")).longValue() : null;

        ConferenceChat chat = ConferenceChat.builder()
                .conference(null) // DM is not conference-scoped
                .user(user)
                .recipientId(recipientId)
                .content(content.trim())
                .replyToId(replyToId)
                .createdAt(LocalDateTime.now())
                .build();

        ConferenceChat saved = chatRepository.save(chat);
        Map<String, Object> response = toMap(saved);

        // Broadcast DM via WebSocket
        webSocketNotificationService.broadcastDm(null, userId, recipientId, response);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/dm/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Send a file via DM")
    public ResponseEntity<Map<String, Object>> sendDmFile(
            @RequestParam("userId") Integer userId,
            @RequestParam("recipientId") Integer recipientId,
            @RequestParam("file") MultipartFile file) throws java.io.IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        String fileUrl = firebaseStorageService.uploadChatFile(file);
        String fileName = file.getOriginalFilename();

        ConferenceChat chat = ConferenceChat.builder()
                .conference(null)
                .user(user)
                .recipientId(recipientId)
                .content("[File] " + fileName)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .createdAt(LocalDateTime.now())
                .build();

        ConferenceChat saved = chatRepository.save(chat);
        Map<String, Object> response = toMap(saved);
        webSocketNotificationService.broadcastDm(null, userId, recipientId, response);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/{conferenceId}/group/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Send a file in group chat")
    public ResponseEntity<Map<String, Object>> sendGroupFile(
            @PathVariable Integer conferenceId,
            @RequestParam("userId") Integer userId,
            @RequestParam("file") MultipartFile file) throws java.io.IOException {

        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String fileUrl = firebaseStorageService.uploadChatFile(file);
        String fileName = file.getOriginalFilename();

        ConferenceChat chat = ConferenceChat.builder()
                .conference(conference)
                .user(user)
                .recipientId(null)
                .content("[File] " + fileName)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .createdAt(LocalDateTime.now())
                .build();

        ConferenceChat saved = chatRepository.save(chat);
        Map<String, Object> response = toMap(saved);
        webSocketNotificationService.broadcastChatMessage(conferenceId, response);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ==================== MEMBERS LIST ====================

    @GetMapping("/{conferenceId}/members")
    @Operation(summary = "Get conference members for chat (name + email)")
    public ResponseEntity<List<Map<String, Object>>> getMembers(@PathVariable Integer conferenceId) {
        List<ConferenceUserTrack> cuts = conferenceUserTrackRepository.findByConference_Id(conferenceId);
        // Deduplicate by userId
        Map<Integer, Map<String, Object>> seen = new LinkedHashMap<>();
        for (ConferenceUserTrack cut : cuts) {
            User u = cut.getUser();
            if (!seen.containsKey(u.getId())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("userId", u.getId());
                m.put("firstName", u.getFirstName());
                m.put("lastName", u.getLastName());
                m.put("email", u.getEmail());
                m.put("role", cut.getAssignedRole().name());
                seen.put(u.getId(), m);
            }
        }
        return ResponseEntity.ok(new ArrayList<>(seen.values()));
    }

    // ==================== DM CONVERSATIONS (global) ====================

    @GetMapping("/dm-conversations")
    @Operation(summary = "Get DM conversation list for a user (global, unique partners + last message)")
    public ResponseEntity<List<Map<String, Object>>> getDmConversations(
            @RequestParam Integer userId) {

        List<ConferenceChat> allDms = chatRepository.findAllDmsForUserGlobal(userId);

        // Group by the "other" user, keep only the latest message per partner
        Map<Integer, ConferenceChat> latestPerPartner = new LinkedHashMap<>();
        for (ConferenceChat dm : allDms) {
            int otherUserId = dm.getUser().getId().equals(userId) ? dm.getRecipientId() : dm.getUser().getId();
            latestPerPartner.putIfAbsent(otherUserId, dm); // already sorted DESC, first = latest
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, ConferenceChat> entry : latestPerPartner.entrySet()) {
            User other = userRepository.findById(entry.getKey()).orElse(null);
            if (other == null) continue;
            ConferenceChat lastMsg = entry.getValue();

            Map<String, Object> conv = new LinkedHashMap<>();
            conv.put("userId", other.getId());
            conv.put("firstName", other.getFirstName());
            conv.put("lastName", other.getLastName());
            conv.put("email", other.getEmail());
            conv.put("lastMessage", lastMsg.getContent());
            conv.put("lastMessageAt", lastMsg.getCreatedAt().toString());
            conv.put("lastMessageUserId", lastMsg.getUser().getId());
            result.add(conv);
        }

        return ResponseEntity.ok(result);
    }

    // ==================== ONLINE PRESENCE ====================

    /** In-memory map: userId -> last heartbeat timestamp (epoch ms) */
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Long> PRESENCE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long ONLINE_THRESHOLD_MS = 60_000; // 60 seconds

    @PostMapping("/heartbeat")
    @Operation(summary = "Send a heartbeat to mark user as online")
    public ResponseEntity<Void> heartbeat(@RequestBody Map<String, Object> body) {
        Integer userId = (Integer) body.get("userId");
        if (userId != null) {
            PRESENCE.put(userId, System.currentTimeMillis());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{conferenceId}/online")
    @Operation(summary = "Get list of online user IDs for a conference")
    public ResponseEntity<List<Integer>> getOnlineUsers(@PathVariable Integer conferenceId) {
        long now = System.currentTimeMillis();
        // Clean up stale entries
        PRESENCE.entrySet().removeIf(e -> now - e.getValue() > ONLINE_THRESHOLD_MS * 3);

        List<ConferenceUserTrack> cuts = conferenceUserTrackRepository.findByConference_Id(conferenceId);
        Set<Integer> memberIds = cuts.stream().map(c -> c.getUser().getId()).collect(Collectors.toSet());

        List<Integer> onlineIds = PRESENCE.entrySet().stream()
                .filter(e -> now - e.getValue() <= ONLINE_THRESHOLD_MS)
                .map(Map.Entry::getKey)
                .filter(memberIds::contains)
                .collect(Collectors.toList());

        return ResponseEntity.ok(onlineIds);
    }

    // ==================== FORWARD ====================

    @PostMapping("/forward")
    @Operation(summary = "Forward a message to another conversation (DM or group)")
    public ResponseEntity<Map<String, Object>> forwardMessage(@RequestBody Map<String, Object> body) {
        Long messageId = ((Number) body.get("messageId")).longValue();
        Integer userId = (Integer) body.get("userId");
        String targetType = (String) body.get("targetType"); // "dm" or "group"
        Integer targetId = (Integer) body.get("targetId"); // recipientId or conferenceId

        ConferenceChat original = chatRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ConferenceChat.ConferenceChatBuilder builder = ConferenceChat.builder()
                .user(sender)
                .content(original.getContent())
                .fileUrl(original.getFileUrl())
                .fileName(original.getFileName())
                .forwarded(true)
                .createdAt(LocalDateTime.now());

        if ("dm".equals(targetType)) {
            builder.conference(null).recipientId(targetId);
        } else {
            Conference conference = conferenceRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conference not found"));
            builder.conference(conference).recipientId(null);
        }

        ConferenceChat saved = chatRepository.save(builder.build());
        Map<String, Object> response = toMap(saved);

        if ("dm".equals(targetType)) {
            webSocketNotificationService.broadcastDm(null, userId, targetId, response);
        } else {
            webSocketNotificationService.broadcastChatMessage(targetId, response);
        }

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ==================== REACTIONS ====================

    @PostMapping("/{messageId}/react")
    @Operation(summary = "Toggle an emoji reaction on a message")
    public ResponseEntity<Map<String, Object>> toggleReaction(
            @PathVariable Long messageId,
            @RequestBody Map<String, Object> body) {

        Integer userId = (Integer) body.get("userId");
        String emoji = (String) body.get("emoji");
        if (userId == null || emoji == null) throw new BadRequestException("userId and emoji required");

        ConferenceChat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        // Parse existing reactions JSON
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        List<Map<String, Object>> reactionsList;
        try {
            reactionsList = chat.getReactions() != null
                    ? om.readValue(chat.getReactions(), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {})
                    : new java.util.ArrayList<>();
        } catch (Exception e) { reactionsList = new java.util.ArrayList<>(); }

        // Toggle: remove if same user+emoji exists, else add
        boolean removed = reactionsList.removeIf(r ->
                Integer.valueOf(userId).equals(r.get("userId")) && emoji.equals(r.get("emoji")));
        if (!removed) {
            Map<String, Object> reaction = new LinkedHashMap<>();
            reaction.put("userId", userId);
            reaction.put("emoji", emoji);
            reactionsList.add(reaction);
        }

        try { chat.setReactions(reactionsList.isEmpty() ? null : om.writeValueAsString(reactionsList)); }
        catch (Exception e) { /* ignore */ }

        chatRepository.save(chat);
        return ResponseEntity.ok(toMap(chat));
    }

    // ==================== DELETE ====================

    @DeleteMapping("/{messageId}")
    @Operation(summary = "Soft-delete a message (only sender can delete)")
    public ResponseEntity<Map<String, Object>> deleteMessage(
            @PathVariable Long messageId,
            @RequestParam Integer userId) {

        ConferenceChat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!chat.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own messages");
        }

        chat.setDeleted(true);
        chat.setContent("This message has been deleted");
        chat.setFileUrl(null);
        chat.setFileName(null);
        chatRepository.save(chat);
        return ResponseEntity.ok(toMap(chat));
    }

    // ==================== MAPPER ====================

    private Map<String, Object> toMap(ConferenceChat chat) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", chat.getId());
        map.put("conferenceId", chat.getConference() != null ? chat.getConference().getId() : null);
        map.put("userId", chat.getUser().getId());
        map.put("userFirstName", chat.getUser().getFirstName());
        map.put("userLastName", chat.getUser().getLastName());
        map.put("userEmail", chat.getUser().getEmail());
        map.put("recipientId", chat.getRecipientId());
        map.put("content", chat.getContent());
        map.put("fileUrl", chat.getFileUrl());
        map.put("fileName", chat.getFileName());
        map.put("replyToId", chat.getReplyToId());
        map.put("deleted", Boolean.TRUE.equals(chat.getDeleted()));
        map.put("forwarded", Boolean.TRUE.equals(chat.getForwarded()));
        map.put("createdAt", chat.getCreatedAt().toString());

        // Parse reactions
        if (chat.getReactions() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                map.put("reactions", om.readValue(chat.getReactions(), List.class));
            } catch (Exception e) { map.put("reactions", List.of()); }
        } else {
            map.put("reactions", List.of());
        }

        // Reply preview
        if (chat.getReplyToId() != null) {
            chatRepository.findById(chat.getReplyToId()).ifPresent(reply -> {
                Map<String, Object> replyMap = new LinkedHashMap<>();
                replyMap.put("id", reply.getId());
                replyMap.put("content", Boolean.TRUE.equals(reply.getDeleted()) ? "Deleted message" : reply.getContent());
                replyMap.put("userFirstName", reply.getUser().getFirstName());
                replyMap.put("userLastName", reply.getUser().getLastName());
                replyMap.put("userId", reply.getUser().getId());
                map.put("replyTo", replyMap);
            });
        }

        return map;
    }
}
