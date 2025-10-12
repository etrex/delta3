/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
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
            // Note: ChatModelListener support in Ollama 0.35.0 may cause issues
            // Temporarily disabled until stable
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
                        "請根據這些操作記錄來理解用戶的意圖和上下文。\n\n" +
                        "## 頁面導航功能\n" +
                        "你可以在回應中加入特殊指令來協助用戶跳轉頁面：\n" +
                        "格式：[NAVIGATE:/path/to/page]\n\n" +
                        "可用的導航路徑（管理員）：\n" +
                        "- /admin/dashboard - 管理員儀表板\n" +
                        "- /admin/products - 商品管理列表\n" +
                        "- /admin/products/new - 新增商品\n" +
                        "- /admin/products/:id - 商品詳情（例如：/admin/products/1）\n" +
                        "- /admin/products/:id/edit - 編輯商品（例如：/admin/products/1/edit）\n" +
                        "- /admin/orders - 訂單管理列表\n" +
                        "- /admin/orders/:id - 訂單詳情（例如：/admin/orders/1）\n" +
                        "- /admin/shipping - 出貨管理\n" +
                        "- /admin/shipping/reports - 出貨報表\n\n" +
                        "使用範例：\n" +
                        "- 「好的，我為您導航到商品管理頁面 [NAVIGATE:/admin/products]」\n" +
                        "- 「這是訂單詳情 [NAVIGATE:/admin/orders/123]」\n" +
                        "- 「請前往出貨管理頁面處理 [NAVIGATE:/admin/shipping]」\n\n" +
                        "注意：導航指令會從顯示內容中移除，只執行跳轉動作。")
                    .build();
        }
        return assistant;
    }
}
