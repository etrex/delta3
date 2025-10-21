/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.dto.AdminDirectMessageRequest;
import com.etrex.oms.dto.AdminSendRequest;
import com.etrex.oms.dto.AiSuggestionDto;
import com.etrex.oms.dto.FeedbackRequest;
import com.etrex.oms.dto.SessionDto;
import com.etrex.oms.entity.AiResponseStatus;
import com.etrex.oms.entity.ChatAiResponse;
import com.etrex.oms.entity.ChatHistory;
import com.etrex.oms.entity.User;
import com.etrex.oms.repository.ChatHistoryRepository;
import com.etrex.oms.service.ChatAiResponseService;
import com.etrex.oms.service.ChatHistoryService;
import com.etrex.oms.service.ChatNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Chat Controller
 * Handles admin operations for customer service chat
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
public class AdminChatController {

    private final ChatHistoryService chatHistoryService;
    private final ChatAiResponseService chatAiResponseService;
    private final ChatNotificationService chatNotificationService;
    private final ChatHistoryRepository chatHistoryRepository;

    /**
     * Get session list with last message and status
     * GET /api/admin/chat/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionDto>> getSessions() {
        // Get all sessions grouped by sessionId
        List<Object[]> rawSessions = chatHistoryRepository.findAllSessionsWithLastMessage();

        List<SessionDto> sessions = rawSessions.stream()
                .map(row -> {
                    String sessionId = (String) row[0];
                    Long userId = (Long) row[1];
                    String lastMessage = (String) row[2];
                    // Handle both Timestamp (H2) and LocalDateTime (PostgreSQL)
                    Long lastMessageTime;
                    if (row[3] instanceof java.sql.Timestamp) {
                        lastMessageTime = ((java.sql.Timestamp) row[3]).getTime();
                    } else {
                        lastMessageTime = ((java.time.LocalDateTime) row[3])
                                .toInstant(ZoneOffset.UTC).toEpochMilli();
                    }

                    // Check if has pending AI suggestion
                    List<ChatAiResponse> pendingSuggestions = chatAiResponseService.getBySessionId(sessionId)
                            .stream()
                            .filter(s -> s.getStatus() == AiResponseStatus.PENDING)
                            .toList();

                    return SessionDto.builder()
                            .sessionId(sessionId)
                            .userId(userId)
                            .lastMessage(lastMessage)
                            .lastMessageTime(lastMessageTime)
                            .hasUnread(false) // TODO: Implement unread tracking
                            .hasPendingSuggestion(!pendingSuggestions.isEmpty())
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(sessions);
    }

    /**
     * Get chat history for a specific session
     * GET /api/admin/chat/sessions/{sessionId}/history
     */
    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<List<ChatHistory>> getSessionHistory(@PathVariable String sessionId) {
        List<ChatHistory> history = chatHistoryService.getHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get pending AI suggestions
     * GET /api/admin/chat/suggestions
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<AiSuggestionDto>> getPendingSuggestions() {
        List<ChatAiResponse> suggestions = chatAiResponseService.getPendingSuggestions();

        List<AiSuggestionDto> dtos = suggestions.stream()
                .map(s -> AiSuggestionDto.builder()
                        .aiResponseId(s.getId())
                        .sessionId(s.getSessionId())
                        .userId(s.getUserMessage().getUserId())
                        .suggestedText(s.getSuggestedResponse())
                        .confidence(s.getConfidenceScore().doubleValue())
                        .createdAt(s.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Approve AI suggestion and send to user
     * POST /api/admin/chat/approve
     */
    @PostMapping("/approve")
    public ResponseEntity<Map<String, String>> approveSuggestion(
            @RequestBody AdminSendRequest request,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();

        // Get AI response to ensure data consistency
        ChatAiResponse aiResponse = chatAiResponseService.getById(request.getAiResponseId());

        // Use sessionId and userId from aiResponse to prevent data inconsistency
        String sessionId = aiResponse.getSessionId();
        Long userId = aiResponse.getUserMessage().getUserId();

        // Save assistant message to chat history
        ChatHistory responseMessage = chatHistoryService.saveMessage(
                sessionId,
                userId,
                ChatHistory.Role.ASSISTANT.name(),
                request.getText()
        );

        // Mark AI response as APPROVED
        chatAiResponseService.markAsApproved(
                request.getAiResponseId(),
                responseMessage.getId(),
                admin.getId()
        );

        // Notify user via WebSocket
        chatNotificationService.notifyUser(
                userId,
                "ai_approved",
                request.getText(),
                responseMessage.getId()
        );

        // Notify admins monitoring this session
        chatNotificationService.notifySessionUpdate(
                sessionId,
                "admin_reply",
                request.getText(),
                responseMessage.getId()
        );

        log.info("Admin {} approved AI suggestion {} and sent to user {}",
                admin.getUsername(), request.getAiResponseId(), userId);

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Modify AI suggestion and send to user
     * POST /api/admin/chat/modify
     */
    @PostMapping("/modify")
    public ResponseEntity<Map<String, String>> modifySuggestion(
            @RequestBody AdminSendRequest request,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();

        // Get AI response to ensure data consistency
        ChatAiResponse aiResponse = chatAiResponseService.getById(request.getAiResponseId());

        // Use sessionId and userId from aiResponse to prevent data inconsistency
        String sessionId = aiResponse.getSessionId();
        Long userId = aiResponse.getUserMessage().getUserId();

        // Save assistant message to chat history
        ChatHistory responseMessage = chatHistoryService.saveMessage(
                sessionId,
                userId,
                ChatHistory.Role.ASSISTANT.name(),
                request.getText()
        );

        // Mark AI response as MODIFIED
        chatAiResponseService.markAsModified(
                request.getAiResponseId(),
                request.getText(),
                responseMessage.getId(),
                admin.getId()
        );

        // Notify user via WebSocket
        chatNotificationService.notifyUser(
                userId,
                "admin",
                request.getText(),
                responseMessage.getId()
        );

        // Notify admins monitoring this session
        chatNotificationService.notifySessionUpdate(
                sessionId,
                "admin_reply",
                request.getText(),
                responseMessage.getId()
        );

        log.info("Admin {} modified AI suggestion {} and sent to user {}",
                admin.getUsername(), request.getAiResponseId(), userId);

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Reject AI suggestion and send manual reply
     * POST /api/admin/chat/reject
     */
    @PostMapping("/reject")
    public ResponseEntity<Map<String, String>> rejectSuggestion(
            @RequestBody AdminSendRequest request,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();

        // Get AI response to ensure data consistency
        ChatAiResponse aiResponse = chatAiResponseService.getById(request.getAiResponseId());

        // Use sessionId and userId from aiResponse to prevent data inconsistency
        String sessionId = aiResponse.getSessionId();
        Long userId = aiResponse.getUserMessage().getUserId();

        // Save assistant message to chat history
        ChatHistory responseMessage = chatHistoryService.saveMessage(
                sessionId,
                userId,
                ChatHistory.Role.ASSISTANT.name(),
                request.getText()
        );

        // Mark AI response as REJECTED
        chatAiResponseService.markAsRejected(
                request.getAiResponseId(),
                request.getText(),
                responseMessage.getId(),
                admin.getId()
        );

        // Notify user via WebSocket
        chatNotificationService.notifyUser(
                userId,
                "admin",
                request.getText(),
                responseMessage.getId()
        );

        // Notify admins monitoring this session
        chatNotificationService.notifySessionUpdate(
                sessionId,
                "admin_reply",
                request.getText(),
                responseMessage.getId()
        );

        log.info("Admin {} rejected AI suggestion {} and sent manual reply to user {}",
                admin.getUsername(), request.getAiResponseId(), userId);

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Provide feedback on AI response
     * POST /api/admin/chat/feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, String>> provideFeedback(
            @RequestBody FeedbackRequest request,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();

        chatAiResponseService.updateFeedback(
                request.getAiResponseId(),
                request.getFeedbackType(),
                request.getReason(),
                admin.getId()
        );

        log.info("Admin {} provided {} feedback for AI response {}",
                admin.getUsername(), request.getFeedbackType(), request.getAiResponseId());

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Send direct message to user (without AI suggestion)
     * POST /api/admin/chat/send
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendDirectMessage(
            @RequestBody AdminDirectMessageRequest request,
            Authentication authentication) {

        User admin = (User) authentication.getPrincipal();

        // Save admin message to chat history
        ChatHistory message = chatHistoryService.saveMessage(
                request.getSessionId(),
                request.getUserId(),
                ChatHistory.Role.ASSISTANT.name(),
                request.getText()
        );

        // Notify user via WebSocket
        chatNotificationService.notifyUser(
                request.getUserId(),
                "admin",
                request.getText(),
                message.getId()
        );

        // Notify other admins monitoring this session
        chatNotificationService.notifySessionUpdate(
                request.getSessionId(),
                "admin_reply",
                request.getText(),
                message.getId()
        );

        log.info("Admin {} sent direct message to user {} in session {}",
                admin.getUsername(), request.getUserId(), request.getSessionId());

        return ResponseEntity.ok(Map.of("status", "success", "messageId", message.getId().toString()));
    }

    /**
     * Check if AI is currently generating response for a session
     * GET /api/admin/chat/sessions/{sessionId}/status
     */
    @GetMapping("/sessions/{sessionId}/status")
    public ResponseEntity<Map<String, Boolean>> getSessionStatus(@PathVariable String sessionId) {
        boolean isGenerating = chatAiResponseService.isGenerating(sessionId);
        log.debug("Session {} generating status: {}", sessionId, isGenerating);
        return ResponseEntity.ok(Map.of("isGenerating", isGenerating));
    }
}
