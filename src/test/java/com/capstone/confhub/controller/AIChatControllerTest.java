package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.AIChatRequest;
import com.capstone.confhub.dto.response.AIChatResponse;
import com.capstone.confhub.dto.response.ManuscriptAnalysisResponse;
import com.capstone.confhub.security.services.UserDetailsImpl;
import com.capstone.confhub.service.impl.AIChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIChatControllerTest {

    @Mock
    private AIChatService chatService;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private AIChatController aiChatController;

    private AIChatRequest chatRequest;
    private MultipartFile mockFile;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        chatRequest = mock(AIChatRequest.class);
        mockFile = new MockMultipartFile("file", "paper.pdf", "application/pdf", new byte[]{1, 2, 3});
        userDetails = mock(UserDetailsImpl.class);
        lenient().when(userDetails.getId()).thenReturn(123);

        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(aiChatController);
    }

    @Test
    void chatShouldReturnOkResponse() {
        AIChatResponse response = mock(AIChatResponse.class);
        when(chatService.chat(anyInt(), any(AIChatRequest.class))).thenReturn(response);
        var result = aiChatController.chat(chatRequest);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void chatShouldExtractUserId() {
        AIChatResponse response = mock(AIChatResponse.class);
        when(chatService.chat(anyInt(), any(AIChatRequest.class))).thenReturn(response);
        aiChatController.chat(chatRequest);
        verify(chatService).chat(123, chatRequest);
    }

    @Test
    void chatShouldReturnResponseBody() {
        AIChatResponse response = mock(AIChatResponse.class);
        when(chatService.chat(anyInt(), any(AIChatRequest.class))).thenReturn(response);
        var result = aiChatController.chat(chatRequest);
        assertNotNull(result.getBody());
    }

    @Test
    void chatShouldHandleMultipleRequests() {
        AIChatResponse response1 = mock(AIChatResponse.class);
        AIChatResponse response2 = mock(AIChatResponse.class);
        when(chatService.chat(anyInt(), any(AIChatRequest.class))).thenReturn(response1).thenReturn(response2);
        var result1 = aiChatController.chat(chatRequest);
        var result2 = aiChatController.chat(chatRequest);
        assertEquals(response1, result1.getBody());
        assertEquals(response2, result2.getBody());
    }

    @Test
    void analyzeManuscriptShouldReturnOk() {
        ManuscriptAnalysisResponse response = mock(ManuscriptAnalysisResponse.class);
        when(chatService.analyzeManuscript(anyInt(), any(MultipartFile.class), anyString())).thenReturn(response);
        var result = aiChatController.analyzeManuscript(mockFile, "session");
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void analyzeManuscriptShouldExtractUserId() {
        ManuscriptAnalysisResponse response = mock(ManuscriptAnalysisResponse.class);
        when(chatService.analyzeManuscript(anyInt(), any(MultipartFile.class), anyString())).thenReturn(response);
        aiChatController.analyzeManuscript(mockFile, "session");
        verify(chatService).analyzeManuscript(123, mockFile, "session");
    }

    @Test
    void analyzeManuscriptShouldReturnResponseBody() {
        ManuscriptAnalysisResponse response = mock(ManuscriptAnalysisResponse.class);
        when(chatService.analyzeManuscript(anyInt(), any(MultipartFile.class), anyString())).thenReturn(response);
        var result = aiChatController.analyzeManuscript(mockFile, "session");
        assertNotNull(result.getBody());
    }

    @Test
    void analyzeManuscriptShouldHandleMultipleFiles() {
        MultipartFile file1 = new MockMultipartFile("file", "paper1.pdf", "application/pdf", new byte[]{1});
        MultipartFile file2 = new MockMultipartFile("file", "paper2.pdf", "application/pdf", new byte[]{2});
        ManuscriptAnalysisResponse response1 = mock(ManuscriptAnalysisResponse.class);
        ManuscriptAnalysisResponse response2 = mock(ManuscriptAnalysisResponse.class);
        when(chatService.analyzeManuscript(123, file1, "session")).thenReturn(response1);
        when(chatService.analyzeManuscript(123, file2, "session")).thenReturn(response2);
        var result1 = aiChatController.analyzeManuscript(file1, "session");
        var result2 = aiChatController.analyzeManuscript(file2, "session");
        assertEquals(response1, result1.getBody());
        assertEquals(response2, result2.getBody());
    }

    @Test
    void getChatHistoryShouldReturnOk() {
        when(chatService.getChatHistory(anyInt(), anyString())).thenReturn(Arrays.asList());
        var result = aiChatController.getChatHistory("session");
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getChatHistoryShouldExtractUserId() {
        List<Map<String, Object>> history = Arrays.asList();
        when(chatService.getChatHistory(anyInt(), anyString())).thenReturn(history);
        aiChatController.getChatHistory("session");
        verify(chatService).getChatHistory(123, "session");
    }

    @Test
    void getChatHistoryShouldReturnHistoryList() {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        List<Map<String, Object>> history = Arrays.asList(msg);
        when(chatService.getChatHistory(anyInt(), anyString())).thenReturn(history);
        var result = aiChatController.getChatHistory("session");
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getChatHistoryShouldReturnEmptyList() {
        when(chatService.getChatHistory(anyInt(), anyString())).thenReturn(Arrays.asList());
        var result = aiChatController.getChatHistory("session");
        assertEquals(0, result.getBody().size());
    }

    @Test
    void getChatHistoryShouldReturnMultipleMessages() {
        List<Map<String, Object>> history = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("index", i);
            history.add(msg);
        }
        when(chatService.getChatHistory(anyInt(), anyString())).thenReturn(history);
        var result = aiChatController.getChatHistory("session");
        assertEquals(5, result.getBody().size());
    }

    @Test
    void getChatHistoryShouldHandleDifferentSessions() {
        List<Map<String, Object>> history1 = Arrays.asList(new HashMap<>(Map.of("id", "msg1")));
        List<Map<String, Object>> history2 = Arrays.asList(new HashMap<>(Map.of("id", "msg2")));
        when(chatService.getChatHistory(123, "session1")).thenReturn(history1);
        when(chatService.getChatHistory(123, "session2")).thenReturn(history2);
        var result1 = aiChatController.getChatHistory("session1");
        var result2 = aiChatController.getChatHistory("session2");
        assertNotEquals(result1.getBody(), result2.getBody());
    }

    @Test
    void allEndpointsShouldReturnOkStatus() {
        AIChatResponse chatResp = mock(AIChatResponse.class);
        ManuscriptAnalysisResponse manuscriptResp = mock(ManuscriptAnalysisResponse.class);
        when(chatService.chat(anyInt(), any(AIChatRequest.class))).thenReturn(chatResp);
        when(chatService.analyzeManuscript(anyInt(), any(MultipartFile.class), anyString())).thenReturn(manuscriptResp);
        when(chatService.getChatHistory(anyInt(), anyString())).thenReturn(Arrays.asList());
        assertEquals(HttpStatus.OK, aiChatController.chat(chatRequest).getStatusCode());
        assertEquals(HttpStatus.OK, aiChatController.analyzeManuscript(mockFile, "session").getStatusCode());
        assertEquals(HttpStatus.OK, aiChatController.getChatHistory("session").getStatusCode());
    }

    @Test
    void sequentialOperationsShouldWork() {
        AIChatResponse chatResp = mock(AIChatResponse.class);
        ManuscriptAnalysisResponse manuscriptResp = mock(ManuscriptAnalysisResponse.class);
        when(chatService.chat(anyInt(), any(AIChatRequest.class))).thenReturn(chatResp);
        when(chatService.analyzeManuscript(anyInt(), any(MultipartFile.class), anyString())).thenReturn(manuscriptResp);
        when(chatService.getChatHistory(anyInt(), anyString())).thenReturn(Arrays.asList());
        var result1 = aiChatController.chat(chatRequest);
        var result2 = aiChatController.analyzeManuscript(mockFile, "session");
        var result3 = aiChatController.getChatHistory("session");
        assertEquals(HttpStatus.OK, result1.getStatusCode());
        assertEquals(HttpStatus.OK, result2.getStatusCode());
        assertEquals(HttpStatus.OK, result3.getStatusCode());
    }
}
