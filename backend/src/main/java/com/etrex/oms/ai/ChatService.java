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
    private final OrderTools orderTools;
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
                    .tools(orderTools)
                    .build();
        }
        return orderAssistant;
    }

    public String chat(String message, String userRole) {
        try {
            String systemMessage = buildSystemMessage(userRole);
            String response = getChatModel().generate(systemMessage + "\n\nUser: " + message);
            return response;
        } catch (Exception e) {
            // Fallback to simple rule-based responses
            return generateFallbackResponse(message, userRole);
        }
    }

    private String generateFallbackResponse(String message, String userRole) {
        String lowerMessage = message.toLowerCase();

        // 問候語
        if (lowerMessage.contains("你好") || lowerMessage.contains("hi") || lowerMessage.contains("hello")) {
            return "您好！我是智能客服助手，可以幫您查詢商品、訂單狀態等。請問有什麼需要協助的嗎？";
        }

        // 商品相關
        if (lowerMessage.contains("商品") || lowerMessage.contains("產品")) {
            return "我可以幫您查詢商品資訊。請告訴我您想了解哪個商品，或是您可以前往商品列表頁面瀏覽。";
        }

        // 訂單相關
        if (lowerMessage.contains("訂單") || lowerMessage.contains("order")) {
            if ("ADMIN".equals(userRole)) {
                return "您可以在訂單管理頁面查看所有訂單，並進行出貨管理等操作。";
            } else {
                return "您可以在「我的訂單」頁面查看您的訂單狀態，包括付款和出貨資訊。";
            }
        }

        // 付款相關
        if (lowerMessage.contains("付款") || lowerMessage.contains("payment")) {
            return "訂單建立後，您可以在訂單詳情頁面進行付款。我們支援信用卡、銀行轉帳等多種付款方式。";
        }

        // 出貨相關
        if (lowerMessage.contains("出貨") || lowerMessage.contains("配送") || lowerMessage.contains("送達")) {
            return "訂單付款完成後，管理員會處理出貨。您可以在訂單詳情中查看出貨狀態和物流資訊。";
        }

        // 預設回應
        return "感謝您的詢問。我是智能客服助手，可以協助您：\n" +
               "- 查詢商品資訊\n" +
               "- 訂單狀態查詢\n" +
               "- 付款相關問題\n" +
               "- 出貨配送資訊\n" +
               "\n請告訴我您需要什麼協助？";
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