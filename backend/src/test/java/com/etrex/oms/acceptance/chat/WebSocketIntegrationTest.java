/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.acceptance.chat;

import com.etrex.oms.dto.ws.NewMessageNotification;
import com.etrex.oms.dto.ws.UserMessageNotification;
import com.etrex.oms.service.ChatNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket Integration Tests
 * Tests WebSocket STOMP messaging (Happy Path)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Rollback
@DisplayName("WebSocket Integration Tests")
public class WebSocketIntegrationTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private ChatNotificationService chatNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private String wsUrl;

    @BeforeEach
    void setUp() {
        wsUrl = "ws://localhost:" + port + "/ws/chat";

        // Create WebSocket client with SockJS
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        SockJsClient sockJsClient = new SockJsClient(List.of(new WebSocketTransport(webSocketClient)));

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    @DisplayName("Admin should receive new message notifications")
    void testAdminReceivesNewMessageNotification() throws Exception {
        BlockingQueue<NewMessageNotification> messages = new LinkedBlockingQueue<>();

        // Connect to WebSocket
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        // Subscribe to admin new messages topic
        session.subscribe("/topic/admin/new-messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NewMessageNotification.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((NewMessageNotification) payload);
            }
        });

        // Wait for subscription to complete
        Thread.sleep(500);

        // Send notification
        chatNotificationService.notifyAdminsNewMessage(
                "session-123",
                1L,
                "customer1",
                "Hello, I need help",
                100L
        );

        // Verify notification received
        NewMessageNotification notification = messages.poll(5, TimeUnit.SECONDS);
        assertThat(notification).isNotNull();
        assertThat(notification.getSessionId()).isEqualTo("session-123");
        assertThat(notification.getUserId()).isEqualTo(1L);
        assertThat(notification.getUserName()).isEqualTo("customer1");
        assertThat(notification.getMessage()).isEqualTo("Hello, I need help");
        assertThat(notification.getMessageId()).isEqualTo(100L);
        assertThat(notification.getTimestamp()).isNotNull();

        session.disconnect();
    }

    @Test
    @DisplayName("User should receive message notifications")
    void testUserReceivesMessageNotification() throws Exception {
        BlockingQueue<UserMessageNotification> messages = new LinkedBlockingQueue<>();
        Long userId = 1L;

        // Connect to WebSocket
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        // Subscribe to user-specific queue
        session.subscribe("/user/" + userId + "/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return UserMessageNotification.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((UserMessageNotification) payload);
            }
        });

        // Wait for subscription to complete
        Thread.sleep(500);

        // Send notification to user
        chatNotificationService.notifyUser(
                userId,
                "ai_auto",
                "Thank you for your inquiry. How can I help you?",
                200L
        );

        // Verify notification received
        UserMessageNotification notification = messages.poll(5, TimeUnit.SECONDS);
        assertThat(notification).isNotNull();
        assertThat(notification.getMessageType()).isEqualTo("ai_auto");
        assertThat(notification.getContent()).isEqualTo("Thank you for your inquiry. How can I help you?");
        assertThat(notification.getMessageId()).isEqualTo(200L);
        assertThat(notification.getTimestamp()).isNotNull();

        session.disconnect();
    }

    @Test
    @DisplayName("Multiple subscribers should receive broadcast messages")
    void testBroadcastToMultipleSubscribers() throws Exception {
        BlockingQueue<NewMessageNotification> admin1Messages = new LinkedBlockingQueue<>();
        BlockingQueue<NewMessageNotification> admin2Messages = new LinkedBlockingQueue<>();

        // Connect first admin
        StompSession admin1Session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        admin1Session.subscribe("/topic/admin/new-messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NewMessageNotification.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                admin1Messages.add((NewMessageNotification) payload);
            }
        });

        // Connect second admin
        StompSession admin2Session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        admin2Session.subscribe("/topic/admin/new-messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return NewMessageNotification.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                admin2Messages.add((NewMessageNotification) payload);
            }
        });

        // Wait for subscriptions to complete
        Thread.sleep(500);

        // Send broadcast notification
        chatNotificationService.notifyAdminsNewMessage(
                "session-456",
                2L,
                "customer2",
                "Broadcast test message",
                300L
        );

        // Verify both admins received the message
        NewMessageNotification admin1Notification = admin1Messages.poll(5, TimeUnit.SECONDS);
        NewMessageNotification admin2Notification = admin2Messages.poll(5, TimeUnit.SECONDS);

        assertThat(admin1Notification).isNotNull();
        assertThat(admin2Notification).isNotNull();
        assertThat(admin1Notification.getSessionId()).isEqualTo("session-456");
        assertThat(admin2Notification.getSessionId()).isEqualTo("session-456");
        assertThat(admin1Notification.getMessage()).isEqualTo("Broadcast test message");
        assertThat(admin2Notification.getMessage()).isEqualTo("Broadcast test message");

        admin1Session.disconnect();
        admin2Session.disconnect();
    }

    @Test
    @DisplayName("Session-specific updates should only reach subscribed admins")
    void testSessionSpecificUpdates() throws Exception {
        BlockingQueue<Object> sessionMessages = new LinkedBlockingQueue<>();
        String sessionId = "session-789";

        // Connect to WebSocket
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        // Subscribe to specific session updates
        session.subscribe("/topic/session/" + sessionId + "/updates", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Object.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                sessionMessages.add(payload);
            }
        });

        // Wait for subscription to complete
        Thread.sleep(500);

        // Send session update
        chatNotificationService.notifySessionUpdate(
                sessionId,
                "new_message",
                "Customer sent a new message",
                400L
        );

        // Verify update received
        Object update = sessionMessages.poll(5, TimeUnit.SECONDS);
        assertThat(update).isNotNull();

        session.disconnect();
    }
}
