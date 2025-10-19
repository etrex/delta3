/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerChatService {
    private final ChatLanguageModel chatLanguageModel;
    private final OrderTools orderTools;
    private final RagSearchTool ragSearchTool;

    private CustomerAssistant assistant;

    public CustomerAssistant getAssistant() {
        if (assistant == null) {
            log.debug("Creating new CustomerAssistant");
            assistant = AiServices.builder(CustomerAssistant.class)
                    .chatLanguageModel(chatLanguageModel)
                    .tools(orderTools, ragSearchTool)
                    .systemMessageProvider(chatMemoryId ->
                        "你是一個專業的訂單管理系統智能客服助手。請用繁體中文回應。" +
                        "使用者是顧客，可以協助查詢商品、建立訂單、查詢訂單狀態等。\n\n" +
                        "用戶的操作會以特殊格式呈現：\n" +
                        "- (開啟頁面 /path) 表示用戶瀏覽了某個頁面\n" +
                        "- (點擊按鈕 buttonId) 表示用戶點擊了按鈕\n" +
                        "- (提交表單 formData) 表示用戶提交了表單\n" +
                        "請根據這些操作記錄來理解用戶的意圖和上下文。\n\n" +
                        "## 頁面導航功能\n" +
                        "你可以在回應中加入特殊指令來協助用戶跳轉頁面：\n" +
                        "格式：[NAVIGATE:/path/to/page]\n\n" +
                        "可用的導航路徑（顧客）：\n" +
                        "- /products - 商品列表頁\n" +
                        "- /products/:id - 商品詳情頁（例如：/products/1）\n" +
                        "- /checkout - 結帳頁面\n" +
                        "- /orders - 訂單列表頁\n" +
                        "- /orders/:id - 訂單詳情頁（例如：/orders/1）\n" +
                        "- /dashboard - 儀表板\n\n" +
                        "使用範例：\n" +
                        "- 「好的，我為您導航到商品頁面 [NAVIGATE:/products]」\n" +
                        "- 「這是您的訂單詳情 [NAVIGATE:/orders/123]」\n" +
                        "- 「請前往結帳頁面完成付款 [NAVIGATE:/checkout]」\n\n" +
                        "注意：導航指令會從顯示內容中移除，只執行跳轉動作。")
                    .build();
        }
        return assistant;
    }

    /**
     * Get the underlying ChatLanguageModel for manual history management
     * Used for AI-assisted customer service with confidence evaluation
     */
    public ChatLanguageModel getChatModel() {
        return chatLanguageModel;
    }
}
