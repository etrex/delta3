/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.acceptance.chat;

import com.etrex.oms.dto.AiSuggestionDto;
import com.etrex.oms.service.ChatNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * WebSocket Notification Service Tests
 * Tests ChatNotificationService notification methods (Happy Path)
 * Note: These tests verify the service methods work correctly.
 * Full end-to-end WebSocket testing requires a connected client.
 */
@SpringBootTest
@Transactional
@Rollback
@DisplayName("WebSocket Notification Service Tests")
public class WebSocketIntegrationTest {

    @Autowired
    private ChatNotificationService chatNotificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("notifyAdminsNewMessage should execute without errors")
    void testNotifyAdminsNewMessage() {
        // Verify that the notification method executes successfully
        assertThatCode(() -> {
            chatNotificationService.notifyAdminsNewMessage(
                    "session-123",
                    1L,
                    "customer1",
                    "Hello, I need help",
                    100L
            );
        }).doesNotThrowAnyException();

        // Verify messaging template is properly configured
        assertThat(messagingTemplate).isNotNull();
    }

    @Test
    @DisplayName("notifyUser should execute without errors")
    void testNotifyUser() {
        // Verify that the notification method executes successfully
        assertThatCode(() -> {
            chatNotificationService.notifyUser(
                    1L,
                    "ai_auto",
                    "Thank you for your inquiry. How can I help you?",
                    200L
            );
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notifyAdminsSuggestion should execute without errors")
    void testNotifyAdminsSuggestion() {
        // Create test AI suggestion
        AiSuggestionDto suggestion = AiSuggestionDto.builder()
                .aiResponseId(1L)
                .sessionId("session-456")
                .userId(2L)
                .suggestedText("Suggested response text")
                .confidence(0.75)
                .createdAt(LocalDateTime.now())
                .build();

        // Verify that the notification method executes successfully
        assertThatCode(() -> {
            chatNotificationService.notifyAdminsSuggestion(suggestion);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notifySessionUpdate should execute without errors")
    void testNotifySessionUpdate() {
        // Verify that the notification method executes successfully
        assertThatCode(() -> {
            chatNotificationService.notifySessionUpdate(
                    "session-789",
                    "new_message",
                    "Customer sent a new message",
                    400L
            );
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("notifyAdminsUserAction should execute without errors")
    void testNotifyAdminsUserAction() {
        // Verify that the notification method executes successfully
        assertThatCode(() -> {
            chatNotificationService.notifyAdminsUserAction(
                    "session-999",
                    3L,
                    "customer3",
                    "NAVIGATE",
                    "/products"
            );
        }).doesNotThrowAnyException();
    }
}
