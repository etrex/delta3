/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.controller;

import com.etrex.oms.ai.CustomerChatService;
import com.etrex.oms.ai.AdminChatService;
import com.etrex.oms.annotation.RateLimit;
import com.etrex.oms.dto.AiSuggestionDto;
import com.etrex.oms.dto.ChatRequest;
import com.etrex.oms.dto.ChatResponse;
import com.etrex.oms.entity.AiResponseStatus;
import com.etrex.oms.entity.ChatAiResponse;
import com.etrex.oms.entity.ChatHistory;
import com.etrex.oms.entity.User;
import com.etrex.oms.service.ChatAiResponseService;
import com.etrex.oms.service.ChatHistoryService;
import com.etrex.oms.service.ChatContextService;
import com.etrex.oms.service.ChatNotificationService;
import com.etrex.oms.service.ConfidenceEvaluator;
import com.etrex.oms.service.ToolCallCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ChatAiResponseService chatAiResponseService;
    private final ChatNotificationService chatNotificationService;
    private final ConfidenceEvaluator confidenceEvaluator;
    private final ToolCallCollector toolCallCollector;
    private final ObjectMapper objectMapper;

    @PostMapping
    @RateLimit(requests = 5, duration = 60)  // 每 60 秒最多 5 次 (AI 成本高)
    @Operation(summary = "Customer chat", description = "Customer chat with AI assistant (AI-assisted with confidence evaluation)")
    public ResponseEntity<ChatResponse> customerChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        String sessionId = String.valueOf(user.getId());
        Long userId = user.getId();
        String userName = user.getUsername();

        log.debug("Customer chat request from user {}", userId);

        try {
            // 1. Get user message
            String userMessage = request.getMessage();

            // 2. Build dynamic context (cart + current page) - append at the END
            String dynamicContext = chatContextService.buildDynamicContext(user, request.getPageContext());
            String messageWithContext = userMessage + dynamicContext;

            // 3. Save user message (without dynamic context to keep history clean)
            ChatHistory userMsgHistory = chatHistoryService.saveMessage(
                    sessionId, userId, ChatHistory.Role.USER.name(), userMessage);

            // 4. Notify admins of new user message
            chatNotificationService.notifyAdminsNewMessage(
                    sessionId, userId, userName, userMessage, userMsgHistory.getId());

            // 5. Notify admins monitoring this specific session
            chatNotificationService.notifySessionUpdate(
                    sessionId, "user_message", userMessage, userMsgHistory.getId());

            // 5.5. Create initial AI response record with GENERATING status
            ChatAiResponse aiResponseRecord = chatAiResponseService.createInitialResponse(
                    sessionId,
                    userMsgHistory.getId()
            );

            // 5.6. Notify admins that AI is generating response
            chatNotificationService.notifySessionUpdate(
                    sessionId, "ai_generating", "AI 正在生成回覆中...", null);

            // 6. Clear and initialize ToolCallCollector for this request
            toolCallCollector.clear();

            // 7. Get conversation history for confidence evaluation
            List<ChatHistory> historyRecords = chatHistoryService.getRecentHistory(sessionId, 20);

            // 8. Sync conversation history to ChatMemory before calling assistant
            customerChatService.syncHistoryToMemory(sessionId, historyRecords);

            // 8.5. Record message count before assistant call
            int messageCountBefore = customerChatService.getMessageCount(sessionId);

            // 9. Get AI response using CustomerAssistant (with tools and proper memory)
            String aiResponse = customerChatService.getAssistant().chat(sessionId, messageWithContext);

            // 9.5. Get all new messages added during assistant call (including tool executions)
            List<ChatMessage> newMessages = customerChatService.getNewMessages(sessionId, messageCountBefore);

            // 9.6. Save all new messages to database (except the last one which is the final AI response)
            saveNewMessagesToHistory(sessionId, userId, newMessages, aiResponse);

            // 10. Collect tool calls from ThreadLocal
            String toolCallsJson = toolCallCollector.toJson();

            // 11. Evaluate confidence
            double confidence = confidenceEvaluator.evaluateConfidence(
                    userMessage,
                    aiResponse,
                    toolCallsJson,
                    historyRecords
            );

            log.info("AI response confidence: {}", confidence);

            // 12. Determine final status and update AI response record
            AiResponseStatus finalStatus;
            if (confidence >= 0.8) {
                finalStatus = AiResponseStatus.AUTO_SENT;
            } else {
                finalStatus = AiResponseStatus.PENDING;
            }

            chatAiResponseService.updateGeneratedResponse(
                    aiResponseRecord.getId(),
                    aiResponse,
                    confidence,
                    toolCallsJson,
                    finalStatus
            );

            // 13. Confidence-based routing
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setSessionId(sessionId);

            if (confidence >= 0.8) {
                // ✅ AUTO-SEND (confidence >= 80%)
                log.info("Auto-sending AI response (confidence: {})", confidence);

                // Save assistant message
                ChatHistory assistantMsg = chatHistoryService.saveMessage(
                        sessionId, userId, ChatHistory.Role.ASSISTANT.name(), aiResponse);

                // Mark as auto-sent
                chatAiResponseService.markAsAutoSent(aiResponseRecord.getId(), assistantMsg.getId());

                // Notify user via WebSocket
                chatNotificationService.notifyUser(userId, "ai_auto", aiResponse, assistantMsg.getId());

                // Notify admins monitoring this session
                chatNotificationService.notifySessionUpdateWithAiInfo(
                        sessionId, aiResponse, assistantMsg.getId(), confidence, aiResponseRecord.getId());

                // Return empty response (message will be delivered via WebSocket to avoid duplication)
                chatResponse.setResponse("");

            } else if (confidence >= 0.4) {
                // 📋 SUGGEST TO ADMIN (40% <= confidence < 80%)
                log.info("Suggesting AI response to admin for review (confidence: {})", confidence);

                // Create AI suggestion DTO
                AiSuggestionDto suggestion = AiSuggestionDto.builder()
                        .aiResponseId(aiResponseRecord.getId())
                        .sessionId(sessionId)
                        .userId(userId)
                        .suggestedText(aiResponse)
                        .confidence(confidence)
                        .createdAt(aiResponseRecord.getCreatedAt())
                        .build();

                // Notify admins of suggestion
                chatNotificationService.notifyAdminsSuggestion(suggestion);

                // Return empty response (message will be delivered via WebSocket when admin approves)
                chatResponse.setResponse("");

            } else {
                // ⏳ WAIT FOR MANUAL HANDLING (confidence < 40%)
                log.info("AI confidence too low, waiting for manual handling (confidence: {})", confidence);

                // Return empty response (admin will handle manually)
                chatResponse.setResponse("");
            }

            return ResponseEntity.ok(chatResponse);

        } catch (Exception e) {
            log.error("Customer chat error for user {}", userId, e);
            return handleChatError(e, sessionId, userId);
        } finally {
            // 14. Clean up ToolCallCollector
            toolCallCollector.remove();
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
    @RateLimit(requests = 10, duration = 60)  // 每 60 秒最多 10 次 (管理員額度較高)
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

    @PostMapping("/action")
    @Operation(summary = "Record user action", description = "Record user action to chat history for AI context")
    public ResponseEntity<Void> recordAction(
            @RequestBody ActionRequest request,
            @AuthenticationPrincipal User user) {

        String sessionId = String.valueOf(user.getId());
        Long userId = user.getId();

        // Convert string to enum
        ChatHistory.ActionType actionType;
        try {
            actionType = ChatHistory.ActionType.valueOf(request.getActionType());
        } catch (IllegalArgumentException e) {
            log.error("Invalid action type: {}", request.getActionType());
            return ResponseEntity.badRequest().build();
        }

        // Save action to chat history
        ChatHistory action = chatHistoryService.saveAction(
                sessionId,
                userId,
                actionType,
                request.getActionTarget()
        );

        // Notify user themselves via WebSocket
        chatNotificationService.notifyUser(
                userId,
                "user_action",
                action.getContent(),
                action.getId()
        );

        // Notify admins monitoring this session
        chatNotificationService.notifySessionUpdate(
                sessionId,
                "user_action",
                action.getContent(),
                action.getId()
        );

        // Notify all admins of user action
        chatNotificationService.notifyAdminsUserAction(
                sessionId,
                userId,
                user.getUsername(),
                request.getActionType(),
                request.getActionTarget()
        );

        log.debug("Recorded user action: userId={}, type={}, target={}",
                userId, request.getActionType(), request.getActionTarget());

        return ResponseEntity.ok().build();
    }

    /**
     * Format tool name to user-friendly Chinese text
     */
    private String formatToolName(String toolName) {
        return switch (toolName) {
            case "searchProducts" -> "搜尋商品";
            case "getProductDetails" -> "查詢商品詳情";
            case "getMyOrders" -> "查詢訂單列表";
            case "getOrderDetails" -> "查詢訂單詳情";
            case "checkStock" -> "檢查庫存";
            case "addToCart" -> "加入購物車";
            case "checkoutCart" -> "結帳";
            case "cancelOrder" -> "取消訂單";
            case "searchFAQ" -> "搜尋常見問題";
            default -> toolName; // Fallback to original name
        };
    }

    /**
     * Save new messages from ChatMemory to database
     * This includes tool execution requests, tool results, and intermediate AI messages
     */
    private void saveNewMessagesToHistory(String sessionId, Long userId, List<ChatMessage> newMessages, String finalAiResponse) {
        try {
            for (int i = 0; i < newMessages.size(); i++) {
                ChatMessage message = newMessages.get(i);

                // Skip the last message if it's an AiMessage matching the final response
                // (we'll save it separately with confidence info)
                boolean isLastMessage = (i == newMessages.size() - 1);
                if (isLastMessage && message instanceof AiMessage) {
                    AiMessage aiMsg = (AiMessage) message;
                    if (finalAiResponse.equals(aiMsg.text())) {
                        log.debug("Skipping final AI response (will be saved separately)");
                        continue;
                    }
                }

                if (message instanceof dev.langchain4j.data.message.UserMessage) {
                    // This is the user's message (already saved before assistant call)
                    log.debug("Skipping UserMessage (already saved)");

                } else if (message instanceof AiMessage) {
                    AiMessage aiMsg = (AiMessage) message;

                    // Check if this AI message contains tool execution requests
                    if (aiMsg.hasToolExecutionRequests()) {
                        // Build user-friendly text for tool execution
                        StringBuilder toolExecutionText = new StringBuilder();

                        // Convert tool requests to Map format for JSON serialization
                        List<Map<String, Object>> toolRequestsData = new ArrayList<>();
                        for (var toolRequest : aiMsg.toolExecutionRequests()) {
                            if (toolExecutionText.length() > 0) {
                                toolExecutionText.append("、");
                            }
                            toolExecutionText.append(formatToolName(toolRequest.name()));

                            Map<String, Object> requestData = new HashMap<>();
                            requestData.put("id", toolRequest.id());
                            requestData.put("name", toolRequest.name());
                            requestData.put("arguments", toolRequest.arguments());
                            toolRequestsData.add(requestData);
                        }

                        String metadata = objectMapper.writeValueAsString(toolRequestsData);
                        String content = "🔧 正在執行：" + toolExecutionText.toString();

                        // Save to DB with metadata
                        ChatHistory savedMsg = chatHistoryService.saveMessageWithMetadata(
                                sessionId, userId, ChatHistory.Role.ASSISTANT.name(), content, metadata);

                        // Also push via WebSocket for real-time display
                        chatNotificationService.notifyUser(
                                userId, "tool_execution", content, savedMsg.getId());

                        log.debug("Saved and notified tool execution start for {} tools",
                                aiMsg.toolExecutionRequests().size());
                    } else {
                        // Regular AI message without tool requests
                        chatHistoryService.saveMessage(
                                sessionId, userId, ChatHistory.Role.ASSISTANT.name(), aiMsg.text());

                        log.debug("Saved intermediate AI message");
                    }

                } else if (message instanceof ToolExecutionResultMessage) {
                    ToolExecutionResultMessage toolMsg = (ToolExecutionResultMessage) message;

                    // Save tool execution result with metadata
                    String metadata = objectMapper.writeValueAsString(Map.of(
                            "toolName", toolMsg.toolName(),
                            "id", toolMsg.id() != null ? toolMsg.id() : ""
                    ));

                    // Create user-friendly completion message
                    String toolCompletionText = "✅ 完成：" + formatToolName(toolMsg.toolName());

                    ChatHistory savedMsg = chatHistoryService.saveMessageWithMetadata(
                            sessionId, userId, ChatHistory.Role.TOOL.name(), toolMsg.text(), metadata);

                    // Push tool completion notification to user via WebSocket
                    chatNotificationService.notifyUser(
                            userId, "tool_result", toolCompletionText, savedMsg.getId());

                    log.debug("Saved and notified tool execution result for tool: {}", toolMsg.toolName());
                }
            }

            log.debug("Saved {} new messages to history for session {}", newMessages.size(), sessionId);

        } catch (Exception e) {
            log.error("Failed to save new messages to history", e);
            // Don't fail the request if history saving fails
        }
    }

    // DTO for action recording
    public static class ActionRequest {
        private String actionType;
        private String actionTarget;

        public String getActionType() {
            return actionType;
        }

        public void setActionType(String actionType) {
            this.actionType = actionType;
        }

        public String getActionTarget() {
            return actionTarget;
        }

        public void setActionTarget(String actionTarget) {
            this.actionTarget = actionTarget;
        }
    }
}