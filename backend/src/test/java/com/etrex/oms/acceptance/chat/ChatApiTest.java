package com.etrex.oms.acceptance.chat;

import com.etrex.oms.acceptance.BaseAcceptanceTest;
import com.etrex.oms.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ChatApiTest extends BaseAcceptanceTest {

    // 14. 測試聊天 API - POST /api/chat (AI 整合)
    @Test
    void testChatAPI() throws Exception {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setMessage("我想查詢訂單狀態");
        chatRequest.setSessionId("test-session-123");

        mockMvc.perform(post("/api/chat")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").exists())
                .andExpect(jsonPath("$.sessionId").value("test-session-123"));
    }
}