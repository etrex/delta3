/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
package com.etrex.oms.ai;

import com.etrex.oms.entity.ChatHistory;
import com.etrex.oms.service.ChatHistoryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerChatService {
    private final ChatLanguageModel chatLanguageModel;
    private final OrderTools orderTools;
    private final RagSearchTool ragSearchTool;
    private final ChatHistoryService chatHistoryService;
    private final ObjectMapper objectMapper;

    private CustomerAssistant assistant;
    private final Map<String, ChatMemory> chatMemories = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT =
        "你是一個專業、簡潔的訂單管理系統智能客服助手。請用繁體中文回應。" +
        "使用者是客戶，當客戶提出需求時，你替客戶完成任務，任務包含查詢商品、建立訂單、查詢訂單狀態等。\n" +
        "如果客戶明確說想買什麼，你就使用工具搜尋我們商店中有沒有販售對應的商品，如果有，協助用戶跳轉到商品詳情頁，並詢問客戶要不要幫他結帳。\n\n" +
        "協助的意思是直接幫用戶操作，不要嘗試教用戶如何操作，重要操作前可以先詢問用戶是否同意，例如幫用戶結帳\n\n" +
        "你可以透過呼叫多個工具組合來處理複雜需求，例如用戶想要更改已經結帳的訂單內容，你可以先幫用戶取消舊的訂單後再重新下訂\n\n" +
        "## 重要：絕對不要重複用戶已知信息\n" +
        "嚴禁描述用戶當前狀態，例如：\n" +
        "❌ 錯誤：「看來您正在查看商品 ID 為 3 的詳情」\n" +
        "❌ 錯誤：「您好！看來您正在 XX 頁面」\n" +
        "✅ 正確：根據上下文直接提供有用建議，例如「需要幫您加入購物車嗎？」「這款有現貨」\n\n" +
        "當你真的不確定客戶想要做什麼的時候，簡單問「請問需要什麼服務嗎？」即可，不要描述當前頁面或商品\n\n" +
        "當你需要回答問題的時候，如果不是訂單或商品相關問題，你應該查詢知識庫，如果查不到資料，你直接放棄回答即可，你就說「我不知道」四個字就好，真人客服會協助回答問題\n\n" +
        "你可以在對話紀錄中看見用戶的操作，用戶操作會以特殊格式呈現：\n" +
        "- (開啟頁面 /path) 表示用戶瀏覽了某個頁面\n" +
        "- (點擊按鈕 buttonId) 表示用戶點擊了按鈕\n" +
        "- (提交表單 formData) 表示用戶提交了表單\n" +
        "請根據這些操作記錄來理解用戶的意圖和上下文。\n\n" +
        "## 回應格式\n" +
        "請使用 Markdown 格式來組織你的回應，特別是：\n" +
        "- 使用 [文字](URL) 格式來顯示鏈接，例如：[MacBook Pro M4](https://www.example.com/products/3)\n" +
        "- 使用 **粗體** 來強調重點\n" +
        "- 使用 `代碼` 格式來顯示訂單號、商品 ID 等\n" +
        "- 使用列表來呈現多個項目\n\n" +
        "## 頁面跳轉功能\n" +
        "你可以在回應中加入特殊指令來幫用戶跳轉到正確的頁面：\n" +
        "格式：[NAVIGATE:/path/to/page]\n\n" +
        "可用的跳轉路徑（客戶）：\n" +
        "- /products - 商品列表頁\n" +
        "- /products/:id - 商品詳情頁（例如：/products/1）\n" +
        "- /checkout - 結帳頁面\n" +
        "- /orders - 訂單列表頁\n" +
        "- /orders/:id - 訂單詳情頁（例如：/orders/1）\n" +
        "- /dashboard - 儀表板\n\n" +
        "使用範例：\n" +
        "- 「好的，我將為您跳轉到商品頁面 [NAVIGATE:/products]」\n" +
        "你應該在查詢到商品或完成結帳後，直接幫用戶跳轉到對應的頁面上\n" +
        "注意：結帳成功後應該跳轉到訂單詳情頁，而不是結帳頁";

    public CustomerAssistant getAssistant() {
        if (assistant == null) {
            log.debug("Creating new CustomerAssistant (manual memory control, auto tool loop)");
            assistant = AiServices.builder(CustomerAssistant.class)
                    .chatLanguageModel(chatLanguageModel)
                    .tools(orderTools, ragSearchTool)  // Tools with automatic loop handling
                    .chatMemoryProvider(memoryId -> {
                        String key = String.valueOf(memoryId);
                        return chatMemories.computeIfAbsent(key,
                            k -> MessageWindowChatMemory.withMaxMessages(50));
                    })
                    // No systemMessageProvider - we manually control message order in ChatMemory
                    .build();
        }
        return assistant;
    }

    /**
     * Sync conversation history from database to ChatMemory
     * We manually control the complete message order to ensure correct structure
     * Now includes support for tool execution messages
     */
    public void syncHistoryToMemory(String sessionId, List<ChatHistory> historyRecords) {
        ChatMemory memory = chatMemories.computeIfAbsent(sessionId,
            k -> MessageWindowChatMemory.withMaxMessages(50));

        // Clear existing messages (rebuild from scratch for complete control)
        memory.clear();

        // ✅ 1. System message FIRST (we control the order)
        memory.add(SystemMessage.from(SYSTEM_PROMPT));

        // ✅ 2. Then conversation history (including tool execution messages)
        for (ChatHistory history : historyRecords) {
            try {
                if ("USER".equals(history.getRole())) {
                    memory.add(UserMessage.from(history.getContent()));

                } else if ("ASSISTANT".equals(history.getRole())) {
                    // Check if this message has tool execution requests metadata
                    if (history.getMetadata() != null && !history.getMetadata().isEmpty()) {
                        // Restore AI message with tool execution requests from Map format
                        List<Map<String, Object>> toolRequestsData = objectMapper.readValue(
                                history.getMetadata(),
                                new TypeReference<List<Map<String, Object>>>() {}
                        );

                        // Convert Map data back to ToolExecutionRequest objects
                        List<ToolExecutionRequest> toolRequests = new ArrayList<>();
                        for (Map<String, Object> requestData : toolRequestsData) {
                            String id = (String) requestData.get("id");
                            String name = (String) requestData.get("name");
                            String arguments = (String) requestData.get("arguments");
                            toolRequests.add(ToolExecutionRequest.builder()
                                    .id(id)
                                    .name(name)
                                    .arguments(arguments)
                                    .build());
                        }

                        memory.add(AiMessage.from(toolRequests));
                        log.debug("Restored AI message with {} tool execution requests", toolRequests.size());

                    } else {
                        // Regular AI message without tools
                        memory.add(AiMessage.from(history.getContent()));
                    }

                } else if ("TOOL".equals(history.getRole())) {
                    // Restore tool execution result
                    if (history.getMetadata() != null && !history.getMetadata().isEmpty()) {
                        Map<String, String> metadata = objectMapper.readValue(
                                history.getMetadata(),
                                new TypeReference<Map<String, String>>() {}
                        );

                        String toolName = metadata.getOrDefault("toolName", "unknown");
                        String id = metadata.getOrDefault("id", "");

                        memory.add(ToolExecutionResultMessage.from(id, toolName, history.getContent()));
                        log.debug("Restored tool execution result for tool: {}", toolName);
                    }
                }
                // Skip ACTION messages as they're already part of user messages

            } catch (Exception e) {
                log.error("Failed to restore message from history: {}", history.getId(), e);
                // Continue processing other messages
            }
        }

        // ✅ 3. Current message will be added by assistant.chat() automatically
        // Final order: System -> History (with tools) -> Current User Message

        log.debug("Synced ChatMemory for session {}: 1 system + {} history messages",
            sessionId, historyRecords.size());
    }

    /**
     * Get the underlying ChatLanguageModel for manual history management
     * Used for AI-assisted customer service with confidence evaluation
     */
    public ChatLanguageModel getChatModel() {
        return chatLanguageModel;
    }

    /**
     * Get new messages added to ChatMemory after a certain index
     * This captures tool execution messages and AI responses
     *
     * @param sessionId Session ID
     * @param fromIndex Start index (exclusive) - messages after this index will be returned
     * @return List of new ChatMessage objects
     */
    public List<dev.langchain4j.data.message.ChatMessage> getNewMessages(String sessionId, int fromIndex) {
        ChatMemory memory = chatMemories.get(sessionId);
        if (memory == null) {
            return List.of();
        }

        List<dev.langchain4j.data.message.ChatMessage> allMessages = memory.messages();
        if (allMessages.size() <= fromIndex) {
            return List.of();
        }

        // Return messages after fromIndex
        return allMessages.subList(fromIndex, allMessages.size());
    }

    /**
     * Get current message count in ChatMemory
     *
     * @param sessionId Session ID
     * @return Number of messages in memory
     */
    public int getMessageCount(String sessionId) {
        ChatMemory memory = chatMemories.get(sessionId);
        if (memory == null) {
            return 0;
        }
        return memory.messages().size();
    }
}
