/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.ai.ChatService;
import com.etrex.oms.ai.OrderAssistant;
import com.etrex.oms.dto.ChatRequest;
import com.etrex.oms.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import(TestSecurityConfig.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private OrderAssistant orderAssistant;

    private User testUser;
    private ChatRequest chatRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.CUSTOMER);

        chatRequest = new ChatRequest();
        chatRequest.setMessage("Hello");
        chatRequest.setSessionId("session-123");
    }

    @Test
    void sendMessage_Success() throws Exception {
        // Given
        when(chatService.chat(eq("Hello"), eq("CUSTOMER")))
                .thenReturn("AI response");

        // When & Then
        mockMvc.perform(post("/api/chat")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("AI response"))
                .andExpect(jsonPath("$.sessionId").value("session-123"));
    }

    @Test
    void sendMessage_AdminUser() throws Exception {
        // Given
        testUser.setRole(User.Role.ADMIN);
        when(chatService.chat(eq("Hello"), eq("ADMIN")))
                .thenReturn("Admin AI response");

        // When & Then
        mockMvc.perform(post("/api/chat")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Admin AI response"));
    }

    @Test
    void chatWithAssistant_Success() throws Exception {
        // Given
        when(chatService.getOrderAssistant()).thenReturn(orderAssistant);
        when(orderAssistant.chat(eq("Hello"))).thenReturn("Assistant response");

        // When & Then
        mockMvc.perform(post("/api/chat/assistant")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Assistant response"))
                .andExpect(jsonPath("$.sessionId").value("session-123"));
    }

    @Test
    void sendMessage_EmptyMessage() throws Exception {
        // Given
        chatRequest.setMessage("");
        when(chatService.chat(eq(""), eq("CUSTOMER")))
                .thenReturn("請輸入訊息");

        // When & Then
        mockMvc.perform(post("/api/chat")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").exists());
    }
}
