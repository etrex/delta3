/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ChatService {
    @Value("${langchain4j.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.model-name}")
    private String modelName;

    @Value("${langchain4j.ollama.timeout}")
    private Duration timeout;

    private ChatLanguageModel chatModel;
    private OrderAssistant orderAssistant;

    private ChatLanguageModel getChatModel() {
        if (chatModel == null) {
            chatModel = OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(modelName)
                    .timeout(timeout)
                    .build();
        }
        return chatModel;
    }

    public OrderAssistant getOrderAssistant() {
        if (orderAssistant == null) {
            orderAssistant = AiServices.builder(OrderAssistant.class)
                    .chatLanguageModel(getChatModel())
                    .tools(new OrderTools())
                    .build();
        }
        return orderAssistant;
    }

    public String chat(String message, String userRole) {
        try {
            String systemMessage = buildSystemMessage(userRole);
            return getChatModel().generate(systemMessage + "\n\nUser: " + message);
        } catch (Exception e) {
            return "抱歉，AI 服務暫時無法使用。請稍後再試。";
        }
    }

    private String buildSystemMessage(String userRole) {
        String baseMessage = "你是一個專業的訂單管理系統智能客服助手。請用繁體中文回應。";

        if ("ADMIN".equals(userRole)) {
            return baseMessage + "使用者是管理員，可以協助處理所有商品和訂單管理相關問題。";
        } else {
            return baseMessage + "使用者是顧客，可以協助查詢商品、建立訂單、查詢訂單狀態等。";
        }
    }
}