/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.ai.ChatService;
import com.etrex.oms.dto.ChatRequest;
import com.etrex.oms.dto.ChatResponse;
import com.etrex.oms.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI Chatbot APIs")
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/message")
    @Operation(summary = "Send message to AI chatbot", description = "Send a message to the AI assistant")
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        String response = chatService.chat(request.getMessage(), user.getRole().name());

        return ResponseEntity.ok(new ChatResponse(response, System.currentTimeMillis()));
    }

    @PostMapping("/assistant")
    @Operation(summary = "Chat with order assistant", description = "Chat with AI assistant using tool calling")
    public ResponseEntity<ChatResponse> chatWithAssistant(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        String response = chatService.getOrderAssistant().chat(request.getMessage());

        return ResponseEntity.ok(new ChatResponse(response, System.currentTimeMillis()));
    }
}