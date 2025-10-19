/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.acceptance.chat;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import com.etrex.oms.dto.ChatRequest;
import com.etrex.oms.dto.ChatResponse;
import com.etrex.oms.entity.AiResponseStatus;
import com.etrex.oms.entity.ChatAiResponse;
import com.etrex.oms.entity.ChatHistory;
import com.etrex.oms.repository.ChatAiResponseRepository;
import com.etrex.oms.repository.ChatHistoryRepository;
import com.etrex.oms.service.ConfidenceEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI-Assisted Chat Integration Tests
 * Tests confidence-based routing and AI response management (Happy Path)
 */
@DisplayName("AI-Assisted Chat Integration Tests")
public class AiAssistedChatIntegrationTest extends BaseAcceptanceTest {

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private ChatAiResponseRepository chatAiResponseRepository;

    @MockBean
    private ConfidenceEvaluator confidenceEvaluator;

    @Test
    @DisplayName("High confidence (>=80%) - should auto-send to customer")
    void testHighConfidenceAutoSend() throws Exception {
        // Mock high confidence score
        when(confidenceEvaluator.evaluateConfidence(anyString(), anyString(), anyString(), any()))
                .thenReturn(0.85);

        ChatRequest request = new ChatRequest();
        request.setMessage("請問有什麼商品？");

        MvcResult result = mockMvc.perform(post("/api/chat")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").exists())
                .andExpect(jsonPath("$.sessionId").exists())
                .andReturn();

        ChatResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ChatResponse.class);

        // Verify user message saved
        List<ChatHistory> history = chatHistoryRepository
                .findBySessionIdOrderByCreatedAtAsc(response.getSessionId());
        assertThat(history).isNotEmpty();

        // Find the user message we just sent
        boolean hasUserMessage = history.stream()
                .anyMatch(h -> h.getContent().equals("請問有什麼商品？") &&
                              h.getRole().equals(ChatHistory.Role.USER.name()));
        assertThat(hasUserMessage).isTrue();

        // Verify AI response record created
        List<ChatAiResponse> aiResponses = chatAiResponseRepository
                .findBySessionIdOrderByCreatedAtDesc(response.getSessionId());
        assertThat(aiResponses).isNotEmpty();
        ChatAiResponse aiResponse = aiResponses.get(0);
        assertThat(aiResponse.getConfidenceScore().doubleValue()).isEqualTo(0.85);

        // For high confidence, should be AUTO_SENT and have response message
        if (aiResponse.getConfidenceScore().doubleValue() >= 0.8) {
            assertThat(aiResponse.getStatus()).isEqualTo(AiResponseStatus.AUTO_SENT);
            assertThat(aiResponse.getResponseMessage()).isNotNull();
        }
    }

    @Test
    @DisplayName("Medium confidence (40-80%) - should suggest to admin")
    void testMediumConfidenceSuggestToAdmin() throws Exception {
        // Mock medium confidence score
        when(confidenceEvaluator.evaluateConfidence(anyString(), anyString(), anyString(), any()))
                .thenReturn(0.65);

        ChatRequest request = new ChatRequest();
        request.setMessage("我的訂單什麼時候會到？");

        MvcResult result = mockMvc.perform(post("/api/chat")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("您的問題已收到，客服人員將儘快為您服務。"))
                .andExpect(jsonPath("$.sessionId").exists())
                .andReturn();

        ChatResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ChatResponse.class);

        // Verify user message saved
        List<ChatHistory> history = chatHistoryRepository
                .findBySessionIdOrderByCreatedAtAsc(response.getSessionId());
        assertThat(history).isNotEmpty();
        ChatHistory lastUserMsg = history.stream()
                .filter(h -> h.getRole().equals(ChatHistory.Role.USER.name()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(lastUserMsg.getContent()).isEqualTo("我的訂單什麼時候會到？");

        // Verify AI response record created with PENDING status
        List<ChatAiResponse> aiResponses = chatAiResponseRepository
                .findBySessionIdOrderByCreatedAtDesc(response.getSessionId());
        assertThat(aiResponses).isNotEmpty();
        ChatAiResponse aiResponse = aiResponses.get(0);
        assertThat(aiResponse.getStatus()).isEqualTo(AiResponseStatus.PENDING);
        assertThat(aiResponse.getConfidenceScore().doubleValue()).isEqualTo(0.65);
        assertThat(aiResponse.getResponseMessage()).isNull(); // Not sent yet
    }

    @Test
    @DisplayName("Low confidence (<40%) - should wait for manual handling")
    void testLowConfidenceWaitForManual() throws Exception {
        // Mock low confidence score
        when(confidenceEvaluator.evaluateConfidence(anyString(), anyString(), anyString(), any()))
                .thenReturn(0.25);

        ChatRequest request = new ChatRequest();
        request.setMessage("你們的退貨政策是什麼？我昨天買的東西想退貨。");

        MvcResult result = mockMvc.perform(post("/api/chat")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("您的問題已收到，客服人員將儘快為您服務。"))
                .andExpect(jsonPath("$.sessionId").exists())
                .andReturn();

        ChatResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ChatResponse.class);

        // Verify user message saved
        List<ChatHistory> history = chatHistoryRepository
                .findBySessionIdOrderByCreatedAtAsc(response.getSessionId());
        assertThat(history).isNotEmpty();
        ChatHistory lastUserMsg = history.stream()
                .filter(h -> h.getRole().equals(ChatHistory.Role.USER.name()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(lastUserMsg.getContent()).isEqualTo("你們的退貨政策是什麼？我昨天買的東西想退貨。");

        // Verify AI response record created with PENDING status
        List<ChatAiResponse> aiResponses = chatAiResponseRepository
                .findBySessionIdOrderByCreatedAtDesc(response.getSessionId());
        assertThat(aiResponses).isNotEmpty();
        ChatAiResponse aiResponse = aiResponses.get(0);
        assertThat(aiResponse.getStatus()).isEqualTo(AiResponseStatus.PENDING);
        assertThat(aiResponse.getConfidenceScore().doubleValue()).isEqualTo(0.25);
        assertThat(aiResponse.getResponseMessage()).isNull(); // Not sent yet
    }

    @Test
    @DisplayName("Tool calls should be recorded in AI response")
    void testToolCallsRecorded() throws Exception {
        // Mock high confidence to auto-send
        when(confidenceEvaluator.evaluateConfidence(anyString(), anyString(), anyString(), any()))
                .thenReturn(0.9);

        ChatRequest request = new ChatRequest();
        request.setMessage("請幫我查詢訂單 1");

        MvcResult result = mockMvc.perform(post("/api/chat")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        ChatResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ChatResponse.class);

        // Verify AI response record exists
        List<ChatAiResponse> aiResponses = chatAiResponseRepository
                .findBySessionIdOrderByCreatedAtDesc(response.getSessionId());
        assertThat(aiResponses).isNotEmpty();

        // Verify tool calls JSON is present (may be empty array or contain calls)
        ChatAiResponse aiResponse = aiResponses.get(0);
        assertThat(aiResponse.getToolCallsJson()).isNotNull();
        assertThat(aiResponse.getToolCallsJson()).matches("\\[.*\\]"); // Valid JSON array
    }
}
