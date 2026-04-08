package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.AIChatRequest;
import com.capstone.confhub.entity.ChatMessage;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.integration.GeminiApiClient;
import com.capstone.confhub.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIChatServiceTest {

    @Mock private GeminiApiClient geminiClient;
    @Mock private ChatMessageRepository chatMessageRepo;
    @Mock private UserRepository userRepo;
    @Mock private ConferenceRepository conferenceRepo;
    @Mock private ConferenceTrackRepository trackRepo;
    @Mock private ConferenceActivityRepository activityRepo;
    @Mock private ConferenceUserTrackRepository userTrackRepo;
    @Mock private SubjectAreaRepository subjectAreaRepo;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private AIChatService aiChatService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@demo.com");

        ReflectionTestUtils.setField(aiChatService, "maxChatPerMinute", 10);
        ReflectionTestUtils.setField(aiChatService, "maxAnalysisPerDay", 20);

        lenient().when(userTrackRepo.findByUser_Id(anyInt())).thenReturn(List.of());
        lenient().when(conferenceRepo.findAll()).thenReturn(List.of());
    }

    @Test
    void chatShouldReturnReplyAndPersistMessages() {
        AIChatRequest req = new AIChatRequest();
        req.setMessage("Hello AI");
        req.setSessionId("s1");

        when(chatMessageRepo.countUserMessagesSince(eq(1), any(LocalDateTime.class))).thenReturn(0L);
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(chatMessageRepo.findTop20ByUser_IdAndSessionIdOrderByCreatedAtDesc(1, "s1")).thenReturn(List.of());
        when(geminiClient.generateContent(anyString(), anyList())).thenReturn("AI reply");

        var result = aiChatService.chat(1, req);

        assertEquals("AI reply", result.getReply());
        assertEquals("s1", result.getSessionId());
        assertNotNull(result.getIntent());
        verify(chatMessageRepo, times(2)).save(any(ChatMessage.class));
    }

    @Test
    void chatShouldThrowWhenRateLimitExceeded() {
        AIChatRequest req = new AIChatRequest();
        req.setMessage("Hello");
        req.setSessionId("s1");

        ReflectionTestUtils.setField(aiChatService, "maxChatPerMinute", 1);
        when(chatMessageRepo.countUserMessagesSince(eq(1), any(LocalDateTime.class))).thenReturn(1L);

        assertThrows(BadRequestException.class, () -> aiChatService.chat(1, req));
    }

    @Test
    void chatShouldThrowWhenUserNotFound() {
        AIChatRequest req = new AIChatRequest();
        req.setMessage("Hello");
        req.setSessionId("s1");

        when(chatMessageRepo.countUserMessagesSince(eq(1), any(LocalDateTime.class))).thenReturn(0L);
        when(userRepo.findById(1)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> aiChatService.chat(1, req));
    }

    @Test
    void chatShouldMergeHistoryAndCurrentMessage() {
        AIChatRequest req = new AIChatRequest();
        req.setMessage("Current message");
        req.setSessionId("session-1");

        ChatMessage oldUser = ChatMessage.builder()
                .user(user)
                .sessionId("session-1")
                .role("user")
                .content("old user msg")
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();
        ChatMessage oldModel = ChatMessage.builder()
                .user(user)
                .sessionId("session-1")
                .role("model")
                .content("old model msg")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(chatMessageRepo.countUserMessagesSince(eq(1), any(LocalDateTime.class))).thenReturn(0L);
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(chatMessageRepo.findTop20ByUser_IdAndSessionIdOrderByCreatedAtDesc(1, "session-1"))
                .thenReturn(new java.util.ArrayList<>(List.of(oldModel, oldUser)));
        when(geminiClient.generateContent(anyString(), anyList())).thenReturn("response");

        aiChatService.chat(1, req);

        verify(geminiClient).generateContent(anyString(), anyList());
    }

    @Test
    void chatShouldSaveUserAndModelMessages() {
        AIChatRequest req = new AIChatRequest();
        req.setMessage("What next?");
        req.setSessionId("s2");

        when(chatMessageRepo.countUserMessagesSince(eq(1), any(LocalDateTime.class))).thenReturn(0L);
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(chatMessageRepo.findTop20ByUser_IdAndSessionIdOrderByCreatedAtDesc(1, "s2")).thenReturn(List.of());
        when(geminiClient.generateContent(anyString(), anyList())).thenReturn("Try step 2");

        aiChatService.chat(1, req);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepo, times(2)).save(captor.capture());
        List<ChatMessage> saved = captor.getAllValues();
        assertEquals("user", saved.get(0).getRole());
        assertEquals("model", saved.get(1).getRole());
    }

    @Test
    void analyzeManuscriptShouldRejectNullFile() {
        when(chatMessageRepo.countAnalysisSince(eq(1), any(LocalDateTime.class))).thenReturn(0L);

        assertThrows(BadRequestException.class,
                () -> aiChatService.analyzeManuscript(1, null, "s1"));
    }

    @Test
    void analyzeManuscriptShouldRejectEmptyFile() {
        when(chatMessageRepo.countAnalysisSince(eq(1), any(LocalDateTime.class))).thenReturn(0L);

        MockMultipartFile file = new MockMultipartFile("file", "paper.pdf", "application/pdf", new byte[]{});
        assertThrows(BadRequestException.class,
                () -> aiChatService.analyzeManuscript(1, file, "s1"));
    }

    @Test
    void analyzeManuscriptShouldRejectNonPdfFile() {
        when(chatMessageRepo.countAnalysisSince(eq(1), any(LocalDateTime.class))).thenReturn(0L);

        MockMultipartFile file = new MockMultipartFile("file", "paper.txt", "text/plain", "demo".getBytes());
        assertThrows(BadRequestException.class,
                () -> aiChatService.analyzeManuscript(1, file, "s1"));
    }

    @Test
    void analyzeManuscriptShouldRejectWhenRateLimitExceeded() {
        ReflectionTestUtils.setField(aiChatService, "maxAnalysisPerDay", 1);
        when(chatMessageRepo.countAnalysisSince(eq(1), any(LocalDateTime.class))).thenReturn(1L);

        MockMultipartFile file = new MockMultipartFile("file", "paper.pdf", "application/pdf", "abc".getBytes());
        assertThrows(BadRequestException.class,
                () -> aiChatService.analyzeManuscript(1, file, "s1"));
    }

    @Test
    void getChatHistoryShouldFilterByOwner() {
        User user2 = new User();
        user2.setId(2);

        ChatMessage ownMsg = ChatMessage.builder()
                .user(user)
                .role("user")
                .content("mine")
                .intent("GENERAL")
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage otherMsg = ChatMessage.builder()
                .user(user2)
                .role("user")
                .content("not mine")
                .intent("GENERAL")
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageRepo.findBySessionIdOrderByCreatedAtAsc("s1")).thenReturn(List.of(ownMsg, otherMsg));

        List<Map<String, Object>> result = aiChatService.getChatHistory(1, "s1");

        assertEquals(1, result.size());
        assertEquals("mine", result.get(0).get("content"));
    }

    @Test
    void getChatHistoryShouldMapFields() {
        ChatMessage msg = ChatMessage.builder()
                .user(user)
                .role("model")
                .content("response")
                .intent("GUIDE")
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageRepo.findBySessionIdOrderByCreatedAtAsc("session-a")).thenReturn(List.of(msg));

        List<Map<String, Object>> result = aiChatService.getChatHistory(1, "session-a");

        assertEquals("model", result.get(0).get("role"));
        assertEquals("response", result.get(0).get("content"));
        assertEquals("GUIDE", result.get(0).get("intent"));
        assertNotNull(result.get(0).get("createdAt"));
    }

    @Test
    void getChatHistoryShouldReturnEmptyForNoMessages() {
        when(chatMessageRepo.findBySessionIdOrderByCreatedAtAsc("s-empty")).thenReturn(List.of());

        List<Map<String, Object>> result = aiChatService.getChatHistory(1, "s-empty");

        assertTrue(result.isEmpty());
    }
}


