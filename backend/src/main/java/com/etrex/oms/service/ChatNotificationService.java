/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.dto.ws.NewMessageNotification;
import com.etrex.oms.dto.ws.SessionUpdateNotification;
import com.etrex.oms.dto.ws.UserActionNotification;
import com.etrex.oms.dto.ws.UserMessageNotification;
import com.etrex.oms.dto.AiSuggestionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Chat Notification Service
 * Handles WebSocket push notifications for real-time communication
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify all admins of a new user message
     * Destination: /topic/admin/new-messages
     */
    public void notifyAdminsNewMessage(String sessionId, Long userId, String userName, String message, Long messageId) {
        NewMessageNotification notification = NewMessageNotification.builder()
                .sessionId(sessionId)
                .userId(userId)
                .userName(userName)
                .message(message)
                .messageId(messageId)
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend("/topic/admin/new-messages", notification);
        log.debug("Notified admins of new message: sessionId={}, userId={}", sessionId, userId);
    }

    /**
     * Notify all admins of a new AI suggestion
     * Destination: /topic/admin/suggestions
     */
    public void notifyAdminsSuggestion(AiSuggestionDto suggestion) {
        messagingTemplate.convertAndSend("/topic/admin/suggestions", suggestion);
        log.debug("Notified admins of AI suggestion: aiResponseId={}, confidence={}",
                suggestion.getAiResponseId(), suggestion.getConfidence());
    }

    /**
     * Notify admins monitoring a specific session of updates
     * Destination: /topic/session/{sessionId}/updates
     */
    public void notifySessionUpdate(String sessionId, String type, String content, Long messageId) {
        SessionUpdateNotification notification = SessionUpdateNotification.builder()
                .type(type)
                .content(content)
                .messageId(messageId)
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/updates", notification);
        log.debug("Notified session update: sessionId={}, type={}", sessionId, type);
    }

    /**
     * Notify admins monitoring a specific session of AI auto-reply with confidence and tool calls
     * Destination: /topic/session/{sessionId}/updates
     */
    public void notifySessionUpdateWithAiInfo(String sessionId, String content, Long messageId,
                                              Double confidence, Long aiResponseId) {
        SessionUpdateNotification notification = SessionUpdateNotification.builder()
                .type("ai_auto_reply")
                .content(content)
                .messageId(messageId)
                .confidence(confidence)
                .aiResponseId(aiResponseId)
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend("/topic/session/" + sessionId + "/updates", notification);
        log.debug("Notified session AI auto-reply: sessionId={}, confidence={}", sessionId, confidence);
    }

    /**
     * Notify a specific user of a new message (from admin or AI)
     * Destination: /user/{userId}/queue/messages
     *
     * @param userId User ID to notify
     * @param messageType Message type: "ai_auto", "admin", "ai_approved"
     * @param content Message content
     * @param messageId Chat history message ID
     */
    public void notifyUser(Long userId, String messageType, String content, Long messageId) {
        UserMessageNotification notification = UserMessageNotification.builder()
                .messageType(messageType)
                .content(content)
                .messageId(messageId)
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/messages",
                notification
        );
        log.debug("Notified user: userId={}, messageType={}", userId, messageType);
    }

    /**
     * Notify all admins of a user action (page navigation, button click, etc.)
     * Destination: /topic/admin/user-actions
     */
    public void notifyAdminsUserAction(String sessionId, Long userId, String userName,
                                       String actionType, String actionTarget) {
        UserActionNotification notification = UserActionNotification.builder()
                .sessionId(sessionId)
                .userId(userId)
                .userName(userName)
                .actionType(actionType)
                .actionTarget(actionTarget)
                .timestamp(System.currentTimeMillis())
                .build();

        messagingTemplate.convertAndSend("/topic/admin/user-actions", notification);
        log.debug("Notified admins of user action: sessionId={}, userId={}, actionType={}",
                sessionId, userId, actionType);
    }
}
