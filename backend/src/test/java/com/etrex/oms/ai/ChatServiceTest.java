/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private OrderTools orderTools;

    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatService, "ollamaBaseUrl", "http://localhost:11434");
        ReflectionTestUtils.setField(chatService, "modelName", "llama2");
        ReflectionTestUtils.setField(chatService, "timeout", Duration.ofSeconds(60));
    }

    @Test
    void chat_ReturnsErrorMessage_WhenOllamaUnavailable() {
        String result = chatService.chat("Hello", "CUSTOMER");

        assertNotNull(result);
        assertTrue(result.contains("抱歉") || result.contains("無法使用"));
    }

    @Test
    void chat_WithAdminRole() {
        String result = chatService.chat("查詢訂單", "ADMIN");

        assertNotNull(result);
        // Since Ollama is not available in test, should return error message
        assertTrue(result.contains("抱歉") || result.contains("無法使用"));
    }

    @Test
    void chat_WithCustomerRole() {
        String result = chatService.chat("我要查詢訂單", "CUSTOMER");

        assertNotNull(result);
        assertTrue(result.contains("抱歉") || result.contains("無法使用"));
    }

    @Test
    void getOrderAssistant_ShouldReturnSameInstance() {
        OrderAssistant assistant1 = chatService.getOrderAssistant();
        OrderAssistant assistant2 = chatService.getOrderAssistant();

        assertNotNull(assistant1);
        assertNotNull(assistant2);
        assertSame(assistant1, assistant2);
    }

    @Test
    void chat_HandlesNullMessage() {
        String result = chatService.chat(null, "CUSTOMER");

        assertNotNull(result);
        assertTrue(result.contains("抱歉") || result.contains("無法使用"));
    }

    @Test
    void chat_HandlesEmptyMessage() {
        String result = chatService.chat("", "CUSTOMER");

        assertNotNull(result);
        assertTrue(result.contains("抱歉") || result.contains("無法使用"));
    }
}
