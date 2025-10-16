/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.ai.CustomerChatService;
import com.etrex.oms.ai.AdminChatService;
import com.etrex.oms.dto.ChatRequest;
import com.etrex.oms.dto.ChatResponse;
import com.etrex.oms.entity.ChatHistory;
import com.etrex.oms.entity.User;
import com.etrex.oms.service.ChatHistoryService;
import com.etrex.oms.service.ChatContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI Chatbot APIs")
public class ChatController {
    private final CustomerChatService customerChatService;
    private final AdminChatService adminChatService;
    private final ChatHistoryService chatHistoryService;
    private final ChatContextService chatContextService;

    @PostMapping
    @Operation(summary = "Customer chat", description = "Customer chat with AI assistant (tool calling enabled)")
    public ResponseEntity<ChatResponse> customerChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        String sessionId = String.valueOf(user.getId());
        Long userId = user.getId();

        log.debug("Customer chat request from user {}", userId);

        try {
            // Get user message
            String userMessage = request.getMessage();

            // Build dynamic context (cart + current page) - append at the END
            String dynamicContext = chatContextService.buildDynamicContext(user, request.getPageContext());
            String messageWithContext = userMessage + dynamicContext;

            // Save user message (without dynamic context to keep history clean)
            chatHistoryService.saveMessage(sessionId, userId, ChatHistory.Role.USER.name(), userMessage);

            // Get AI response (may throw exception)
            String response = customerChatService.getAssistant().chat(messageWithContext);

            // Save AI response
            chatHistoryService.saveMessage(sessionId, userId, ChatHistory.Role.ASSISTANT.name(), response);

            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setResponse(response);
            chatResponse.setSessionId(sessionId);

            return ResponseEntity.ok(chatResponse);

        } catch (Exception e) {
            log.error("Customer chat error for user {}", userId, e);
            return handleChatError(e, sessionId, userId);
        }
    }

    private ResponseEntity<ChatResponse> handleChatError(Exception e, String sessionId, Long userId) {
        // Log full error with stack trace
        log.error("AI chat error for user {}", userId, e);

        // Create error details
        String errorType = e.getClass().getSimpleName();
        String errorMsg = e.getMessage() != null ? e.getMessage() : "未知錯誤";

        // Truncate if too long (max 80 chars)
        if (errorMsg.length() > 80) {
            errorMsg = errorMsg.substring(0, 80) + "...";
        }

        // Format user-friendly error
        String userFriendlyError = String.format(
            "❌ AI 服務暫時無法使用\n" +
            "錯誤類型: %s\n" +
            "錯誤訊息: %s\n" +
            "請稍後再試或聯繫系統管理員",
            errorType,
            errorMsg
        );

        // Save error to chat history
        chatHistoryService.saveMessage(sessionId, userId, ChatHistory.Role.ASSISTANT.name(), userFriendlyError);

        // Return error response
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setResponse(userFriendlyError);
        chatResponse.setSessionId(sessionId);

        return ResponseEntity.ok(chatResponse);
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin chat", description = "Admin chat with AI assistant (tool calling enabled)")
    public ResponseEntity<ChatResponse> adminChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        String sessionId = String.valueOf(user.getId());
        Long userId = user.getId();

        try {
            // Get user message
            String userMessage = request.getMessage();

            // Build dynamic context (cart + current page) - append at the END
            String dynamicContext = chatContextService.buildDynamicContext(user, request.getPageContext());
            String messageWithContext = userMessage + dynamicContext;

            // Save user message (without dynamic context to keep history clean)
            chatHistoryService.saveMessage(sessionId, userId, ChatHistory.Role.USER.name(), userMessage);

            // Get AI response (may throw exception)
            String response = adminChatService.getAssistant().chat(messageWithContext);

            // Save AI response
            chatHistoryService.saveMessage(sessionId, userId, ChatHistory.Role.ASSISTANT.name(), response);

            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setResponse(response);
            chatResponse.setSessionId(sessionId);

            return ResponseEntity.ok(chatResponse);

        } catch (Exception e) {
            return handleChatError(e, sessionId, userId);
        }
    }


    @GetMapping("/history")
    @Operation(summary = "Get chat history", description = "Get chat history for a session")
    public ResponseEntity<List<ChatHistory>> getHistory(
            @RequestParam String sessionId,
            @AuthenticationPrincipal User user) {

        List<ChatHistory> history = chatHistoryService.getHistory(sessionId);
        return ResponseEntity.ok(history);
    }
}