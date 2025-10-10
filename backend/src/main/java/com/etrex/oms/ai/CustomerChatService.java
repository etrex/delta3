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
public class CustomerChatService {
    private final OrderTools orderTools;

    @Value("${langchain4j.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.model-name}")
    private String modelName;

    @Value("${langchain4j.ollama.timeout}")
    private Duration timeout;

    private ChatLanguageModel chatModel;
    private CustomerAssistant assistant;

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

    public CustomerAssistant getAssistant() {
        if (assistant == null) {
            assistant = AiServices.builder(CustomerAssistant.class)
                    .chatLanguageModel(getChatModel())
                    .tools(orderTools)
                    .systemMessageProvider(chatMemoryId ->
                        "你是一個專業的訂單管理系統智能客服助手。請用繁體中文回應。" +
                        "使用者是顧客，可以協助查詢商品、建立訂單、查詢訂單狀態等。")
                    .build();
        }
        return assistant;
    }
}
