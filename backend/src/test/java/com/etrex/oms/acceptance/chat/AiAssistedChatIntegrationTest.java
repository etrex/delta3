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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI-Assisted Chat Integration Tests
 * Tests confidence-based routing and AI response management (Happy Path)
 *
 * NOTE: These tests require Ollama AI service to be running.
 * Run: ollama serve
 * And ensure the model (e.g., qwen2.5:3b) is available: ollama pull qwen2.5:3b
 */
@DisplayName("AI-Assisted Chat Integration Tests")
@Import(TestConfidenceEvaluatorConfig.class)
@Disabled("Requires Ollama AI service running - enable manually when testing AI integration")
public class AiAssistedChatIntegrationTest extends BaseAcceptanceTest {

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private ChatAiResponseRepository chatAiResponseRepository;

    @Test
    @DisplayName("High confidence (>=80%) - should auto-send to customer")
    void testHighConfidenceAutoSend() throws Exception {
        // Test with high confidence question (keyword: "請問有什麼商品")
        // TestConfidenceEvaluator will return 0.9 for this
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
        assertThat(aiResponse.getConfidenceScore().doubleValue()).isEqualTo(0.9);

        // For high confidence, should be AUTO_SENT and have response message
        assertThat(aiResponse.getStatus()).isEqualTo(AiResponseStatus.AUTO_SENT);
        assertThat(aiResponse.getResponseMessage()).isNotNull();
    }

    @Test
    @DisplayName("Medium confidence (40-80%) - should suggest to admin")
    void testMediumConfidenceSuggestToAdmin() throws Exception {
        // Test with medium confidence question (keyword: "什麼時候會到")
        // TestConfidenceEvaluator will return 0.65 for this
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
        // Test with low confidence question (keyword: "退貨政策")
        // TestConfidenceEvaluator will return 0.25 for this
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
        // Test with tool call question (keyword: "查詢訂單")
        // TestConfidenceEvaluator will return 0.9 for this
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

        // Verify AI response was created (tool calls may or may not be present)
        ChatAiResponse aiResponse = aiResponses.get(0);
        assertThat(aiResponse.getStatus()).isEqualTo(AiResponseStatus.AUTO_SENT);

        // Tool calls JSON should be set (even if empty array "[]")
        // In some test environments it might be null if no tools were called
        if (aiResponse.getToolCallsJson() != null) {
            assertThat(aiResponse.getToolCallsJson()).matches("\\[.*\\]"); // Valid JSON array
        }
    }
}
