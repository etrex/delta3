/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.config;

import com.etrex.oms.service.ToolCallCollector;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ChatModelListener
 * Spring Boot will automatically register all ChatModelListener beans
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChatModelListenerConfig {

    private final ToolCallCollector toolCallCollector;

    @Bean
    public ChatModelListener loggingChatModelListener() {
        return new ChatModelListener() {
            @SuppressWarnings("unused")
            public void onRequest(ChatModelRequest request) {
                log.info("╔════════════════════════════════════════════════════════════════");
                log.info("║ 🔵 LLM REQUEST");
                log.info("╠════════════════════════════════════════════════════════════════");
                log.info("║ Model: {}", request.model());
                log.info("║ Message Count: {}", request.messages().size());
                log.info("╠════════════════════════════════════════════════════════════════");
                log.info("║ MESSAGES:");
                request.messages().forEach(msg -> {
                    log.info("║ ---");
                    log.info("║ Type: {}", msg.type());

                    // Get content based on message type (text() is deprecated)
                    String content = null;
                    if (msg instanceof UserMessage) {
                        content = ((UserMessage) msg).singleText();
                        log.info("║ Content: {}", content);
                    } else if (msg instanceof AiMessage) {
                        content = ((AiMessage) msg).text();
                        log.info("║ Content: {}", content);
                    } else if (msg instanceof SystemMessage) {
                        content = ((SystemMessage) msg).text();
                        log.info("║ Content: {}", content);
                    } else if (msg instanceof ToolExecutionResultMessage) {
                        ToolExecutionResultMessage resultMsg = (ToolExecutionResultMessage) msg;
                        log.info("║ ✅ Tool Result: {}", resultMsg.toolName());
                        log.info("║    Result: {}", resultMsg.text());
                    }
                });
                log.info("╚════════════════════════════════════════════════════════════════");
            }

            @SuppressWarnings("unused")
            public void onResponse(ChatModelResponse response) {
                log.info("╔════════════════════════════════════════════════════════════════");
                log.info("║ 🟢 LLM RESPONSE");
                log.info("╠════════════════════════════════════════════════════════════════");

                // Check for tool calls and record them
                if (response.aiMessage().hasToolExecutionRequests()) {
                    log.info("║ 🔧 TOOL CALLS:");
                    response.aiMessage().toolExecutionRequests().forEach(tool -> {
                        log.info("║   - Tool: {}", tool.name());
                        log.info("║     Arguments: {}", tool.arguments());

                        // Record tool call request (result will be recorded later when executed)
                        // For now, record with placeholder result
                        toolCallCollector.addToolCall(
                            tool.name(),
                            tool.arguments(),
                            "pending",  // Will be updated when result is available
                            0
                        );
                    });
                    log.info("╠════════════════════════════════════════════════════════════════");
                }

                // Show text response if exists
                if (response.aiMessage().text() != null && !response.aiMessage().text().isEmpty()) {
                    log.info("║ AI Response:");
                    log.info("║ {}", response.aiMessage().text());
                    log.info("╠════════════════════════════════════════════════════════════════");
                }

                log.info("║ Token Usage:");
                if (response.tokenUsage() != null) {
                    log.info("║   Input Tokens: {}", response.tokenUsage().inputTokenCount());
                    log.info("║   Output Tokens: {}", response.tokenUsage().outputTokenCount());
                    log.info("║   Total Tokens: {}", response.tokenUsage().totalTokenCount());
                }
                log.info("╚════════════════════════════════════════════════════════════════");
            }

            @SuppressWarnings("unused")
            public void onError(Throwable error) {
                log.error("╔════════════════════════════════════════════════════════════════");
                log.error("║ 🔴 LLM ERROR");
                log.error("╠════════════════════════════════════════════════════════════════");
                log.error("║ Error: ", error);
                log.error("╚════════════════════════════════════════════════════════════════");
            }
        };
    }
}
