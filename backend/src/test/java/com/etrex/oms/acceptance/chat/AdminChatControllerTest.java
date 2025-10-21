/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.acceptance.chat;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import com.etrex.oms.dto.AdminSendRequest;
import com.etrex.oms.dto.FeedbackRequest;
import com.etrex.oms.entity.*;
import com.etrex.oms.repository.ChatAiResponseRepository;
import com.etrex.oms.repository.ChatHistoryRepository;
import com.etrex.oms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminChatController Integration Tests
 * Tests admin chat management APIs (Happy Path)
 */
@DisplayName("Admin Chat Controller Tests")
public class AdminChatControllerTest extends BaseAcceptanceTest {

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private ChatAiResponseRepository chatAiResponseRepository;

    @Autowired
    private UserRepository userRepository;

    private Long customerId;
    private String sessionId;
    private ChatHistory userMessage;
    private ChatAiResponse aiSuggestion;

    @BeforeEach
    void setUp() {
        // Get customer user
        User customer = userRepository.findByUsername("customer1").orElseThrow();
        customerId = customer.getId();
        sessionId = String.valueOf(customerId);

        // Create test chat history
        userMessage = new ChatHistory();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(customerId);
        userMessage.setRole(ChatHistory.Role.USER.name());
        userMessage.setMessageType(ChatHistory.MessageType.MESSAGE.name());
        userMessage.setContent("我想查詢訂單狀態");
        userMessage.setCreatedAt(LocalDateTime.now());
        userMessage = chatHistoryRepository.save(userMessage);

        // Create test AI suggestion
        aiSuggestion = ChatAiResponse.builder()
                .sessionId(sessionId)
                .userMessage(userMessage)
                .suggestedResponse("您好，請提供您的訂單編號，我會幫您查詢訂單狀態。")
                .confidenceScore(BigDecimal.valueOf(0.65))
                .toolCallsJson("[]")
                .status(AiResponseStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        aiSuggestion = chatAiResponseRepository.save(aiSuggestion);
    }

    @Test
    @DisplayName("GET /api/admin/chat/sessions - should return session list")
    void testGetSessions() throws Exception {
        mockMvc.perform(get("/api/admin/chat/sessions")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.sessionId=='" + sessionId + "')]").exists())
                .andExpect(jsonPath("$[?(@.sessionId=='" + sessionId + "')].userId").value(customerId.intValue()))
                .andExpect(jsonPath("$[?(@.sessionId=='" + sessionId + "')].lastMessage").value("我想查詢訂單狀態"))
                .andExpect(jsonPath("$[?(@.sessionId=='" + sessionId + "')].hasPendingSuggestion").value(true));
    }

    @Test
    @DisplayName("GET /api/admin/chat/sessions/{sessionId}/history - should return chat history")
    void testGetSessionHistory() throws Exception {
        mockMvc.perform(get("/api/admin/chat/sessions/" + sessionId + "/history")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$[0].userId").value(customerId.intValue()))
                .andExpect(jsonPath("$[0].content").value("我想查詢訂單狀態"));
    }

    @Test
    @DisplayName("GET /api/admin/chat/suggestions - should return pending AI suggestions")
    void testGetPendingSuggestions() throws Exception {
        mockMvc.perform(get("/api/admin/chat/suggestions")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].aiResponseId").value(aiSuggestion.getId().intValue()))
                .andExpect(jsonPath("$[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$[0].userId").value(customerId.intValue()))
                .andExpect(jsonPath("$[0].suggestedText").value("您好，請提供您的訂單編號，我會幫您查詢訂單狀態。"))
                .andExpect(jsonPath("$[0].confidence").value(0.65));
    }

    @Test
    @DisplayName("POST /api/admin/chat/approve - should approve and send AI suggestion")
    void testApproveSuggestion() throws Exception {
        AdminSendRequest request = AdminSendRequest.builder()
                .sessionId(sessionId)
                .userId(customerId)
                .aiResponseId(aiSuggestion.getId())
                .text("您好，請提供您的訂單編號，我會幫您查詢訂單狀態。")
                .build();

        mockMvc.perform(post("/api/admin/chat/approve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Verify AI response status updated to APPROVED
        ChatAiResponse updated = chatAiResponseRepository.findById(aiSuggestion.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AiResponseStatus.APPROVED);
        assertThat(updated.getResponseMessage()).isNotNull();
        assertThat(updated.getReviewedByAdmin()).isNotNull();

        // Verify chat history saved
        List<ChatHistory> history = chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getContent()).isEqualTo("您好，請提供您的訂單編號，我會幫您查詢訂單狀態。");
        assertThat(history.get(1).getRole()).isEqualTo(ChatHistory.Role.ASSISTANT.name());
    }

    @Test
    @DisplayName("POST /api/admin/chat/modify - should modify and send AI suggestion")
    void testModifySuggestion() throws Exception {
        String modifiedText = "您好！我是客服人員。請提供訂單編號，我立即為您查詢。";

        AdminSendRequest request = AdminSendRequest.builder()
                .sessionId(sessionId)
                .userId(customerId)
                .aiResponseId(aiSuggestion.getId())
                .text(modifiedText)
                .build();

        mockMvc.perform(post("/api/admin/chat/modify")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Verify AI response status updated to MODIFIED
        ChatAiResponse updated = chatAiResponseRepository.findById(aiSuggestion.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AiResponseStatus.MODIFIED);
        assertThat(updated.getActualResponse()).isEqualTo(modifiedText);
        assertThat(updated.getResponseMessage()).isNotNull();
        assertThat(updated.getReviewedByAdmin()).isNotNull();

        // Verify chat history saved
        List<ChatHistory> history = chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getContent()).isEqualTo(modifiedText);
    }

    @Test
    @DisplayName("POST /api/admin/chat/reject - should reject and send manual reply")
    void testRejectSuggestion() throws Exception {
        String manualReply = "抱歉讓您久等了。我是真人客服，請問您的訂單編號是多少？";

        AdminSendRequest request = AdminSendRequest.builder()
                .sessionId(sessionId)
                .userId(customerId)
                .aiResponseId(aiSuggestion.getId())
                .text(manualReply)
                .build();

        mockMvc.perform(post("/api/admin/chat/reject")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Verify AI response status updated to REJECTED
        ChatAiResponse updated = chatAiResponseRepository.findById(aiSuggestion.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AiResponseStatus.REJECTED);
        assertThat(updated.getActualResponse()).isEqualTo(manualReply);
        assertThat(updated.getResponseMessage()).isNotNull();
        assertThat(updated.getReviewedByAdmin()).isNotNull();

        // Verify chat history saved
        List<ChatHistory> history = chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getContent()).isEqualTo(manualReply);
    }

    @Test
    @DisplayName("POST /api/admin/chat/feedback - should provide feedback on AI response")
    void testProvideFeedback() throws Exception {
        FeedbackRequest request = FeedbackRequest.builder()
                .aiResponseId(aiSuggestion.getId())
                .feedbackType(FeedbackType.POSITIVE)
                .reason("回應準確且專業")
                .build();

        mockMvc.perform(post("/api/admin/chat/feedback")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // Verify feedback saved
        ChatAiResponse updated = chatAiResponseRepository.findById(aiSuggestion.getId()).orElseThrow();
        assertThat(updated.getFeedbackType()).isEqualTo(FeedbackType.POSITIVE);
        assertThat(updated.getFeedbackReason()).isEqualTo("回應準確且專業");
        assertThat(updated.getFeedbackByAdmin()).isNotNull();
        assertThat(updated.getFeedbackAt()).isNotNull();
    }
}
