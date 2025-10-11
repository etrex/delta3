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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI Chatbot APIs")
public class ChatController {
    private final CustomerChatService customerChatService;
    private final AdminChatService adminChatService;
    private final ChatHistoryService chatHistoryService;

    @PostMapping
    @Operation(summary = "Customer chat", description = "Customer chat with AI assistant (tool calling enabled)")
    public ResponseEntity<ChatResponse> customerChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        // Use user ID as session ID
        String sessionId = String.valueOf(user.getId());
        Long userId = user.getId();

        // Get recent actions to provide context
        List<String> recentActions = chatHistoryService.getRecentActionsFormatted(sessionId, 3);
        String messageWithContext = request.getMessage();
        if (!recentActions.isEmpty()) {
            messageWithContext = String.join("\n", recentActions) + "\n" + request.getMessage();
        }

        // Save user message
        chatHistoryService.saveMessage(sessionId, userId, ChatHistory.Role.USER.name(), request.getMessage());

        // Get AI response
        String response = customerChatService.getAssistant().chat(messageWithContext);

        // Save AI response
        chatHistoryService.saveMessage(sessionId, userId, ChatHistory.Role.ASSISTANT.name(), response);

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setResponse(response);
        chatResponse.setSessionId(sessionId);

        return ResponseEntity.ok(chatResponse);
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin chat", description = "Admin chat with AI assistant (tool calling enabled)")
    public ResponseEntity<ChatResponse> adminChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        // Use user ID as session ID
        String sessionId = String.valueOf(user.getId());
        Long userId = user.getId();

        // Get recent actions to provide context
        List<String> recentActions = chatHistoryService.getRecentActionsFormatted(sessionId, 3);
        String messageWithContext = request.getMessage();
        if (!recentActions.isEmpty()) {
            messageWithContext = String.join("\n", recentActions) + "\n" + request.getMessage();
        }

        // Save user message
        chatHistoryService.saveMessage(sessionId, userId, ChatHistory.Role.USER.name(), request.getMessage());

        // Get AI response
        String response = adminChatService.getAssistant().chat(messageWithContext);

        // Save AI response
        chatHistoryService.saveMessage(sessionId, userId, ChatHistory.Role.ASSISTANT.name(), response);

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setResponse(response);
        chatResponse.setSessionId(sessionId);

        return ResponseEntity.ok(chatResponse);
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