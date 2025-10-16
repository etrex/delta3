/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.config;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ChatModelListener
 * Spring Boot will automatically register all ChatModelListener beans
 */
@Slf4j
@Configuration
public class ChatModelListenerConfig {

    @Bean
    public ChatModelListener loggingChatModelListener() {
        return new ChatModelListener() {
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
                    log.info("║ Content: {}", msg.text());
                });
                log.info("╚════════════════════════════════════════════════════════════════");
            }

            public void onResponse(ChatModelResponse response) {
                log.info("╔════════════════════════════════════════════════════════════════");
                log.info("║ 🟢 LLM RESPONSE");
                log.info("╠════════════════════════════════════════════════════════════════");
                log.info("║ AI Response:");
                log.info("║ {}", response.aiMessage().text());
                log.info("╠════════════════════════════════════════════════════════════════");
                log.info("║ Token Usage:");
                if (response.tokenUsage() != null) {
                    log.info("║   Input Tokens: {}", response.tokenUsage().inputTokenCount());
                    log.info("║   Output Tokens: {}", response.tokenUsage().outputTokenCount());
                    log.info("║   Total Tokens: {}", response.tokenUsage().totalTokenCount());
                }
                log.info("╚════════════════════════════════════════════════════════════════");
            }

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
