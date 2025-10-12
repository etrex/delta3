/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.service;

import com.etrex.oms.entity.ChatHistory;
import com.etrex.oms.entity.User;
import com.etrex.oms.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {
    private final ChatHistoryRepository chatHistoryRepository;

    /**
     * Save a message to chat history
     */
    @Transactional
    public ChatHistory saveMessage(String sessionId, Long userId, String role, String content) {
        ChatHistory history = new ChatHistory();
        history.setSessionId(sessionId);
        history.setUserId(userId);
        history.setRole(role);
        history.setMessageType(ChatHistory.MessageType.MESSAGE.name());
        history.setContent(content);
        return chatHistoryRepository.save(history);
    }

    /**
     * Save a user action to chat history
     */
    @Transactional
    public ChatHistory saveAction(String sessionId, Long userId, String actionType, String actionTarget) {
        ChatHistory history = new ChatHistory();
        history.setSessionId(sessionId);
        history.setUserId(userId);
        history.setRole(ChatHistory.Role.USER.name());
        history.setMessageType(ChatHistory.MessageType.ACTION.name());
        history.setContent(formatAction(actionType, actionTarget));
        history.setActionType(actionType);
        history.setActionTarget(actionTarget);
        return chatHistoryRepository.save(history);
    }

    /**
     * Format action as readable text for AI
     */
    private String formatAction(String actionType, String actionTarget) {
        return switch (actionType.toUpperCase()) {
            case "NAVIGATE" -> String.format("(開啟頁面 %s)", actionTarget);
            case "CLICK" -> String.format("(點擊按鈕 %s)", actionTarget);
            case "SUBMIT" -> String.format("(提交表單 %s)", actionTarget);
            case "OPEN_MODAL" -> String.format("(開啟彈窗 %s)", actionTarget);
            case "CLOSE_MODAL" -> String.format("(關閉彈窗 %s)", actionTarget);
            case "API_CALL" -> String.format("(執行: %s)", actionTarget);
            default -> String.format("(執行操作 %s: %s)", actionType, actionTarget);
        };
    }

    /**
     * Get full chat history for a session
     */
    public List<ChatHistory> getHistory(String sessionId) {
        return chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * Get recent N records for a session
     */
    public List<ChatHistory> getRecentHistory(String sessionId, int limit) {
        List<ChatHistory> history = chatHistoryRepository.findRecentBySessionId(sessionId, limit);
        Collections.reverse(history); // Reverse to get chronological order
        return history;
    }

    /**
     * Get recent actions formatted for AI context
     */
    public List<String> getRecentActionsFormatted(String sessionId, int limit) {
        List<ChatHistory> actions = chatHistoryRepository.findRecentActionsBySessionId(sessionId, limit);
        Collections.reverse(actions);
        return actions.stream()
                .map(ChatHistory::getContent)
                .collect(Collectors.toList());
    }

    /**
     * Build conversation context for AI (messages + actions)
     */
    public String buildConversationContext(String sessionId, int limit) {
        List<ChatHistory> history = getRecentHistory(sessionId, limit);
        StringBuilder context = new StringBuilder();

        for (ChatHistory h : history) {
            if (h.getMessageType().equals(ChatHistory.MessageType.ACTION.name())) {
                context.append(h.getContent()).append("\n");
            } else {
                String prefix = h.getRole().equals(ChatHistory.Role.USER.name()) ? "用戶" : "助手";
                context.append(String.format("%s: %s\n", prefix, h.getContent()));
            }
        }

        return context.toString();
    }

    /**
     * Get history by user ID
     */
    public List<ChatHistory> getHistoryByUserId(Long userId) {
        return chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Track user operation - simple one-line interface for controllers
     * Automatically gets current user from security context
     *
     * @param description Human-readable description of what user did
     */
    @Transactional
    public void track(String description) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                String sessionId = String.valueOf(user.getId());
                saveAction(sessionId, user.getId(), "api_call", description);
                log.debug("Tracked: {} for user {}", description, user.getUsername());
            }
        } catch (Exception e) {
            log.error("Failed to track operation: {}", description, e);
            // Don't fail the request if tracking fails
        }
    }

    /**
     * Track user operation with explicit user
     *
     * @param user User who performed the action
     * @param description Human-readable description of what user did
     */
    @Transactional
    public void track(User user, String description) {
        try {
            String sessionId = String.valueOf(user.getId());
            saveAction(sessionId, user.getId(), "api_call", description);
            log.debug("Tracked: {} for user {}", description, user.getUsername());
        } catch (Exception e) {
            log.error("Failed to track operation: {}", description, e);
            // Don't fail the request if tracking fails
        }
    }
}
