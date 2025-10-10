/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.ai.CustomerChatService;
import com.etrex.oms.ai.AdminChatService;
import com.etrex.oms.dto.ChatRequest;
import com.etrex.oms.dto.ChatResponse;
import com.etrex.oms.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI Chatbot APIs")
public class ChatController {
    private final CustomerChatService customerChatService;
    private final AdminChatService adminChatService;

    @PostMapping
    @Operation(summary = "Customer chat", description = "Customer chat with AI assistant (tool calling enabled)")
    public ResponseEntity<ChatResponse> customerChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        String response = customerChatService.getAssistant().chat(request.getMessage());

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setResponse(response);
        chatResponse.setSessionId(request.getSessionId());

        return ResponseEntity.ok(chatResponse);
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin chat", description = "Admin chat with AI assistant (tool calling enabled)")
    public ResponseEntity<ChatResponse> adminChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        String response = adminChatService.getAssistant().chat(request.getMessage());

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setResponse(response);
        chatResponse.setSessionId(request.getSessionId());

        return ResponseEntity.ok(chatResponse);
    }
}