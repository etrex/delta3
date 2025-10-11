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
public class AdminChatService {
    private final OrderTools orderTools;

    @Value("${langchain4j.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.model-name}")
    private String modelName;

    @Value("${langchain4j.ollama.timeout}")
    private Duration timeout;

    private ChatLanguageModel chatModel;
    private AdminAssistant assistant;

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

    public AdminAssistant getAssistant() {
        if (assistant == null) {
            assistant = AiServices.builder(AdminAssistant.class)
                    .chatLanguageModel(getChatModel())
                    .tools(orderTools)
                    .systemMessageProvider(chatMemoryId ->
                        "你是一個專業的訂單管理系統智能客服助手。請用繁體中文回應。" +
                        "使用者是管理員，可以協助處理所有商品和訂單管理相關問題，包括出貨、庫存管理等進階功能。\n\n" +
                        "用戶的操作會以特殊格式呈現：\n" +
                        "- (開啟頁面 /path) 表示用戶瀏覽了某個頁面\n" +
                        "- (點擊按鈕 buttonId) 表示用戶點擊了按鈕\n" +
                        "- (提交表單 formData) 表示用戶提交了表單\n" +
                        "請根據這些操作記錄來理解用戶的意圖和上下文。")
                    .build();
        }
        return assistant;
    }
}
