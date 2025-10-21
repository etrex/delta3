# AI 輔助客服系統 - 實作計畫

> 基於 [AI-ASSISTED-CUSTOMER-SERVICE-SYSTEM.md](./AI-ASSISTED-CUSTOMER-SERVICE-SYSTEM.md) 設計文件

**目標**：升級 `POST /api/chat` 為 AI 輔助客服架構（保持 `POST /api/chat/admin` 不變）

---

## 工作項目總覽

### 📊 統計
- **新增檔案**：20 個
- **修改檔案**：3 個
- **資料庫遷移**：1 個

### ⚠️ 現有實作已完成的功能
以下功能在現有程式碼中已實作，**不需要重新開發**：
- ✅ ChatHistoryService.saveMessage()
- ✅ ChatHistoryService.saveAction()
- ✅ ChatHistoryService.getRecentHistory()
- ✅ ChatHistoryService.buildConversationContext()
- ✅ ChatHistoryService.track()
- ✅ ChatRequest（含 actionType, pageContext）
- ✅ ChatHistory Entity（含 action 相關欄位）
- ✅ ChatHistoryRepository（含 action 查詢）
- ✅ System Prompt（已包含操作記錄說明和導航指令）

---

## 第一階段：資料庫層

### 1. 資料庫遷移檔案

#### 📝 新增：`backend/src/main/resources/db/migration/V{version}__create_chat_ai_response.sql`

**職責**：建立 `chat_ai_response` 表

**內容**：
```sql
CREATE TABLE chat_ai_response (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    user_message_id BIGINT NOT NULL,
    suggested_response TEXT NOT NULL,
    confidence_score DECIMAL(3,2) NOT NULL,
    tool_calls_json TEXT,
    status VARCHAR(20) NOT NULL,
    actual_response TEXT,
    response_message_id BIGINT,
    reviewed_by_admin_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,

    -- 評價欄位（NULLABLE）
    feedback_type VARCHAR(20),
    feedback_reason TEXT,
    feedback_by_admin_id BIGINT,
    feedback_at TIMESTAMP,

    -- 外鍵
    CONSTRAINT fk_user_message FOREIGN KEY (user_message_id)
        REFERENCES chat_history(id) ON DELETE CASCADE,
    CONSTRAINT fk_response_message FOREIGN KEY (response_message_id)
        REFERENCES chat_history(id) ON DELETE SET NULL,
    CONSTRAINT fk_reviewed_by_admin FOREIGN KEY (reviewed_by_admin_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_feedback_by_admin FOREIGN KEY (feedback_by_admin_id)
        REFERENCES users(id) ON DELETE SET NULL
);

-- 索引
CREATE INDEX idx_chat_ai_response_session_id ON chat_ai_response(session_id);
CREATE INDEX idx_chat_ai_response_status ON chat_ai_response(status);
CREATE INDEX idx_chat_ai_response_confidence ON chat_ai_response(confidence_score);
CREATE INDEX idx_chat_ai_response_created_at ON chat_ai_response(created_at);
CREATE INDEX idx_chat_ai_response_feedback_type ON chat_ai_response(feedback_type);
```

---

## 第二階段：Entity 層

### 2. Entity 類

#### 📝 新增：`backend/src/main/java/com/etrex/oms/entity/ChatAiResponse.java`

**職責**：chat_ai_response 表的 JPA Entity

**主要內容**：
- 所有資料庫欄位對應的屬性
- @Entity, @Table 註解
- Getter/Setter (或使用 Lombok)
- 關聯到 ChatHistory 和 User 的外鍵

**關鍵欄位**：
```java
@Entity
@Table(name = "chat_ai_response")
public class ChatAiResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;

    @ManyToOne
    @JoinColumn(name = "user_message_id")
    private ChatHistory userMessage;

    private String suggestedResponse;
    private BigDecimal confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String toolCallsJson;

    @Enumerated(EnumType.STRING)
    private AiResponseStatus status;

    private String actualResponse;

    @ManyToOne
    @JoinColumn(name = "response_message_id")
    private ChatHistory responseMessage;

    @ManyToOne
    @JoinColumn(name = "reviewed_by_admin_id")
    private User reviewedByAdmin;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    // 評價欄位
    @Enumerated(EnumType.STRING)
    private FeedbackType feedbackType;
    private String feedbackReason;

    @ManyToOne
    @JoinColumn(name = "feedback_by_admin_id")
    private User feedbackByAdmin;
    private LocalDateTime feedbackAt;
}
```

---

### 3. Enum 類

#### 📝 新增：`backend/src/main/java/com/etrex/oms/entity/AiResponseStatus.java`

**職責**：定義 AI 回應狀態的枚舉

**內容**：
```java
public enum AiResponseStatus {
    AUTO_SENT,      // 自動發送（信心度 ≥80%）
    PENDING,        // 等待管理員決定（信心度 40-80%）
    APPROVED,       // 管理員採用原建議
    MODIFIED,       // 管理員修改後發送
    REJECTED,       // 管理員拒絕，自己寫
    IGNORED         // 管理員沒處理（超時或忽略）
}
```

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/entity/FeedbackType.java`

**職責**：定義評價類型的枚舉

**內容**：
```java
public enum FeedbackType {
    POSITIVE,       // 正面評價
    NEGATIVE        // 負面評價
}
```

---

## 第三階段：Repository 層

### 4. Repository 介面

#### 📝 新增：`backend/src/main/java/com/etrex/oms/repository/ChatAiResponseRepository.java`

**職責**：chat_ai_response 表的資料存取層

**主要方法**：
```java
@Repository
public interface ChatAiResponseRepository extends JpaRepository<ChatAiResponse, Long> {

    // 根據 session_id 查詢
    List<ChatAiResponse> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    // 根據狀態查詢
    List<ChatAiResponse> findByStatus(AiResponseStatus status);

    // 查詢待處理的建議（PENDING 且未過期）
    @Query("SELECT r FROM ChatAiResponse r WHERE r.status = 'PENDING' AND r.createdAt > :cutoffTime")
    List<ChatAiResponse> findPendingSuggestions(@Param("cutoffTime") LocalDateTime cutoffTime);

    // 根據 user_message_id 查詢
    Optional<ChatAiResponse> findByUserMessageId(Long userMessageId);

    // 查詢有負面評價的案例
    List<ChatAiResponse> findByFeedbackType(FeedbackType feedbackType);
}
```

---

## 第四階段：DTO 層

### 5. DTO 類

#### 📝 新增：`backend/src/main/java/com/etrex/oms/dto/AiSuggestionDto.java`

**職責**：傳遞 AI 建議給管理員的 DTO

**內容**：
```java
public class AiSuggestionDto {
    private Long aiResponseId;
    private String sessionId;
    private Long userId;
    private String suggestedText;
    private Double confidence;
    private List<ToolCallDto> toolCalls;
    private LocalDateTime createdAt;
}
```

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/dto/ToolCallDto.java`

**職責**：工具呼叫記錄的 DTO

**內容**：
```java
public class ToolCallDto {
    private String toolName;
    private String arguments;
    private String result;
    private Integer executionTime;  // ms
}
```

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/dto/ws/NewMessageNotification.java`

**職責**：用戶發送新訊息的 WebSocket 通知

**內容**：
```java
public class NewMessageNotification {
    private Long userId;
    private String userName;        // 用戶名稱（用於顯示）
    private String sessionId;
    private String message;
    private Long messageId;         // chat_history 的 ID
    private Long timestamp;         // Unix timestamp (ms)
}
```

**前端顯示需求**：
- Session 列表：更新最後訊息、顯示紅點
- 對話視窗（如果打開）：新增用戶訊息氣泡

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/dto/ws/UserMessageNotification.java`

**職責**：推送給客戶的訊息通知

**內容**：
```java
public class UserMessageNotification {
    private String content;
    private Long timestamp;
    private String messageType;     // "ai_auto" / "admin" / "ai_approved"
    private Long messageId;         // chat_history 的 ID
}
```

**前端顯示需求**：
- 客戶聊天介面：新增客服回覆氣泡
- 顯示時間
- 用戶無需知道是 AI 還是真人

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/dto/ws/SessionUpdateNotification.java`

**職責**：特定 session 的更新通知（給管理員）

**內容**：
```java
public class SessionUpdateNotification {
    private String type;            // "ai_auto_reply" / "admin_reply" / "new_message"
    private String content;         // 訊息內容
    private Long timestamp;
    private Long messageId;         // chat_history 的 ID

    // AI 自動回覆專用欄位
    private Double confidence;      // 信心度（僅 ai_auto_reply）
    private Long aiResponseId;      // chat_ai_response 的 ID（僅 ai_auto_reply）
    private List<ToolCallDto> toolCalls; // 工具呼叫（僅 ai_auto_reply）
}
```

**前端顯示需求**：
- 對話視窗：新增訊息氣泡
- AI 自動回覆：顯示信心度標記、👍👎 按鈕
- 工具呼叫側邊欄：顯示工具記錄

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/dto/ws/UserActionNotification.java`

**職責**：用戶操作網頁的 WebSocket 通知

**內容**：
```java
public class UserActionNotification {
    private Long userId;
    private String userName;
    private String sessionId;
    private String actionType;      // "navigate" / "click" / "submit" / "open_modal"
    private String actionTarget;    // "/products/123" / "buy_button" / "checkout_form"
    private Long timestamp;
}
```

**前端顯示需求**：
- 對話視窗：顯示用戶操作（灰色系統訊息）
  - 「用戶開啟了商品頁面 /products/123」
  - 「用戶點擊了購買按鈕」
  - 「用戶提交了結帳表單」
- 幫助管理員理解用戶的操作脈絡

**使用範例**：
```java
// 用戶開啟商品頁面
UserActionNotification action = UserActionNotification.builder()
    .userId(123L)
    .userName("張三")
    .sessionId("123")
    .actionType("navigate")
    .actionTarget("/products/456")
    .timestamp(System.currentTimeMillis())
    .build();

chatNotificationService.notifyAdminsUserAction(action);
// 推送到: /topic/admin/user-actions
```

**對應的 chat_history 記錄**：
```
role: USER
message_type: ACTION
action_type: navigate
action_target: /products/456
content: (開啟頁面 /products/456)
```

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/dto/FeedbackRequest.java`

**職責**：管理員提交評價的請求 DTO

**內容**：
```java
public class FeedbackRequest {
    private Long aiResponseId;
    private FeedbackType feedbackType;  // POSITIVE / NEGATIVE
    private String reason;              // 可選
}
```

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/dto/AdminSendRequest.java`

**職責**：管理員發送回覆的請求 DTO（三種操作共用）

**內容**：
```java
public class AdminSendRequest {
    private String sessionId;
    private Long userId;
    private Long aiResponseId;      // 關聯的 AI 建議（可選）
    private String text;             // 要發送的內容
    private String originalSuggestion; // 原始建議（用於 MODIFIED）
}
```

---

## 第五階段：Service 層

### 6. Service 類

#### 📝 新增：`backend/src/main/java/com/etrex/oms/service/ChatAiResponseService.java`

**職責**：管理 AI 回應記錄的業務邏輯

**主要方法**：
```java
@Service
public class ChatAiResponseService {

    // 儲存 AI 回應
    ChatAiResponse saveAiResponse(
        String sessionId,
        Long userMessageId,
        String suggestedResponse,
        Double confidenceScore,
        String toolCallsJson,
        AiResponseStatus status
    );

    // 更新狀態為 AUTO_SENT
    void markAsAutoSent(Long aiResponseId, Long responseMessageId);

    // 更新狀態為 APPROVED
    void markAsApproved(Long aiResponseId, Long responseMessageId, Long adminId);

    // 更新狀態為 MODIFIED
    void markAsModified(Long aiResponseId, String actualResponse, Long responseMessageId, Long adminId);

    // 更新狀態為 REJECTED
    void markAsRejected(Long aiResponseId, String actualResponse, Long responseMessageId, Long adminId);

    // 更新評價
    void updateFeedback(Long aiResponseId, FeedbackType feedbackType, String reason, Long adminId);

    // 查詢待處理建議
    List<ChatAiResponse> getPendingSuggestions();

    // 根據 session 查詢
    List<ChatAiResponse> getBySessionId(String sessionId);
}
```

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/service/ConfidenceEvaluator.java`

**職責**：計算 AI 回應的信心度（5 個 LLM 評估問題）

**主要方法**：
```java
@Service
public class ConfidenceEvaluator {

    private final ChatLanguageModel chatLanguageModel;

    // 主要方法：計算信心度
    public double evaluateConfidence(
        String userQuestion,
        String aiResponse,
        String toolCallsJson,
        List<ChatHistory> conversationHistory
    );

    // 內部方法：並行執行 5 個評估問題
    private int evaluateCompleteness(String userQuestion, String aiResponse);
    private int evaluateCertainty(String aiResponse);
    private int evaluateSensitivity(String userQuestion, String aiResponse);
    private int evaluateToolCallSuccess(String toolCallsJson);
    private int evaluateNeedsHumanReview(String userQuestion, String aiResponse, String conversationHistory);

    // 工具方法：格式化對話歷史
    private String formatConversationHistory(List<ChatHistory> history);
}
```

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/service/ToolCallCollector.java`

**職責**：使用 ThreadLocal 暫存工具呼叫記錄

**主要方法**：
```java
@Service
public class ToolCallCollector {

    private static final ThreadLocal<List<ToolCallDto>> toolCalls = ThreadLocal.withInitial(ArrayList::new);

    // 新增工具呼叫記錄
    public void addToolCall(String toolName, String arguments, String result, int executionTime);

    // 獲取當前線程的工具呼叫記錄
    public List<ToolCallDto> getToolCalls();

    // 清空當前線程的記錄
    public void clear();

    // 將工具呼叫記錄轉為 JSON
    public String toJson();
}
```

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/service/ChatNotificationService.java`

**職責**：WebSocket 推送服務

**主要方法**：
```java
@Service
public class ChatNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送新訊息給所有管理員
     * 頻道: /topic/admin/new-messages
     * 用途: 更新 Session 列表，顯示紅點
     */
    public void notifyAdminsNewMessage(NewMessageNotification notification);

    /**
     * 推送 AI 建議給所有管理員
     * 頻道: /topic/admin/suggestions
     * 用途: 顯示 Quick Reply 區，讓管理員決定是否採用
     */
    public void notifyAdminsSuggestion(AiSuggestionDto suggestion);

    /**
     * 推送 AI 自動回覆通知給管理員
     * 頻道: /topic/session/{sessionId}/updates
     * 用途: 更新對話視窗，顯示 AI 已自動回覆（帶信心度和工具記錄）
     */
    public void notifySessionUpdate(String sessionId, SessionUpdateNotification notification);

    /**
     * 推送訊息給特定客戶
     * 頻道: /user/{userId}/queue/messages
     * 用途: 客戶收到客服回覆
     */
    public void notifyUser(Long userId, UserMessageNotification notification);

    /**
     * 推送用戶操作給所有管理員
     * 頻道: /topic/admin/user-actions
     * 用途: 讓管理員看到用戶的操作行為（開啟頁面、點擊按鈕等）
     */
    public void notifyAdminsUserAction(UserActionNotification notification);
}
```

**推送內容範例**：

1. **用戶發送新訊息**
```java
NewMessageNotification notification = NewMessageNotification.builder()
    .userId(123L)
    .userName("張三")
    .sessionId("123")
    .message("我要退款")
    .messageId(456L)
    .timestamp(System.currentTimeMillis())
    .build();

notifyAdminsNewMessage(notification);
// 推送到: /topic/admin/new-messages
```

2. **AI 生成建議**
```java
AiSuggestionDto suggestion = AiSuggestionDto.builder()
    .aiResponseId(789L)
    .sessionId("123")
    .userId(123L)
    .suggestedText("請提供您的訂單編號...")
    .confidence(0.65)
    .toolCalls(toolCallsList)
    .createdAt(LocalDateTime.now())
    .build();

notifyAdminsSuggestion(suggestion);
// 推送到: /topic/admin/suggestions
```

3. **AI 自動回覆**
```java
SessionUpdateNotification update = SessionUpdateNotification.builder()
    .type("ai_auto_reply")
    .content("請提供您的訂單編號...")
    .timestamp(System.currentTimeMillis())
    .messageId(460L)
    .confidence(0.85)
    .aiResponseId(790L)
    .toolCalls(toolCallsList)
    .build();

notifySessionUpdate("123", update);
// 推送到: /topic/session/123/updates

// 同時推送給客戶
UserMessageNotification userMsg = UserMessageNotification.builder()
    .content("請提供您的訂單編號...")
    .timestamp(System.currentTimeMillis())
    .messageType("ai_auto")
    .messageId(460L)
    .build();

notifyUser(123L, userMsg);
// 推送到: /user/123/queue/messages
```

4. **管理員發送回覆**
```java
// 推送給客戶
UserMessageNotification userMsg = UserMessageNotification.builder()
    .content("非常抱歉！請提供您的訂單編號...")
    .timestamp(System.currentTimeMillis())
    .messageType("admin")
    .messageId(461L)
    .build();

notifyUser(123L, userMsg);

// 推送給其他管理員（更新對話視窗）
SessionUpdateNotification update = SessionUpdateNotification.builder()
    .type("admin_reply")
    .content("非常抱歉！請提供您的訂單編號...")
    .timestamp(System.currentTimeMillis())
    .messageId(461L)
    .build();

notifySessionUpdate("123", update);
```

5. **用戶操作網頁**
```java
UserActionNotification action = UserActionNotification.builder()
    .userId(123L)
    .userName("張三")
    .sessionId("123")
    .actionType("navigate")
    .actionTarget("/products/456")
    .timestamp(System.currentTimeMillis())
    .build();

chatNotificationService.notifyAdminsUserAction(action);
// 推送到: /topic/admin/user-actions
```

---

#### ✏️ 修改：`backend/src/main/java/com/etrex/oms/service/ChatHistoryService.java`

**職責**：新增「取得對話歷史供 AI 使用」的方法

**現有方法（已實作）**：
```java
// ✅ 已實作
public ChatHistory saveMessage(String sessionId, Long userId, String role, String content);
public ChatHistory saveAction(String sessionId, Long userId, String actionType, String actionTarget);
public List<ChatHistory> getHistory(String sessionId);
public List<ChatHistory> getRecentHistory(String sessionId, int limit);
public String buildConversationContext(String sessionId, int limit);
public void track(String description);  // 簡易追蹤介面
```

**需要新增的方法**：
```java
// 轉換為 langchain4j 的 ChatMessage 格式（用於 memory）
public List<ChatMessage> getHistoryAsChatMessages(String sessionId, int limit);
```

**getHistoryAsChatMessages 實作範例**：
```java
public List<ChatMessage> getHistoryAsChatMessages(String sessionId, int limit) {
    List<ChatHistory> history = getRecentHistory(sessionId, limit);
    List<ChatMessage> messages = new ArrayList<>();

    for (ChatHistory h : history) {
        if (h.getMessageType().equals("ACTION")) {
            // 操作記錄作為用戶訊息
            messages.add(new UserMessage(h.getContent()));
        } else if (h.getRole().equals("USER")) {
            messages.add(new UserMessage(h.getContent()));
        } else if (h.getRole().equals("ASSISTANT")) {
            messages.add(new AiMessage(h.getContent()));
        }
    }

    return messages;
}
```

**注意**：
- `saveAction()` 方法**已在現有實作中完成**，支援多種操作類型
- `buildConversationContext()` 可以將對話歷史格式化為文字，但不適用於需要 ChatMemory 的場景

---

## 第六階段：Controller 層

#### ✏️ 修改：`backend/src/main/java/com/etrex/oms/controller/ChatController.java`

**職責**：升級 `customerChat` 方法，加入信心度評估和路由決策

**修改內容**：
```java
@PostMapping
public ResponseEntity<ChatResponse> customerChat(
        @RequestBody ChatRequest request,
        @AuthenticationPrincipal User user) {

    String sessionId = String.valueOf(user.getId());
    Long userId = user.getId();

    try {
        // 0. 記錄用戶操作（如果有）
        if (request.getActionType() != null) {
            ChatHistory action = chatHistoryService.saveAction(
                sessionId,
                userId,
                request.getActionType(),
                request.getActionTarget()
            );

            // 推送用戶操作給管理員（讓管理員看到用戶在做什麼）
            UserActionNotification actionNotification = UserActionNotification.builder()
                .userId(userId)
                .userName(user.getUsername())
                .sessionId(sessionId)
                .actionType(request.getActionType())
                .actionTarget(request.getActionTarget())
                .timestamp(System.currentTimeMillis())
                .build();

            chatNotificationService.notifyAdminsUserAction(actionNotification);
        }

        // 1. 保存用戶訊息（如果有文字訊息）
        Long userMessageId = null;
        if (request.getMessage() != null && !request.getMessage().isEmpty()) {
            ChatHistory userMsg = chatHistoryService.saveMessage(
                sessionId, userId, "USER", request.getMessage()
            );
            userMessageId = userMsg.getId();

            // 推送新訊息給管理員
            NewMessageNotification msgNotification = NewMessageNotification.builder()
                .userId(userId)
                .userName(user.getUsername())
                .sessionId(sessionId)
                .message(request.getMessage())
                .messageId(userMessageId)
                .timestamp(System.currentTimeMillis())
                .build();

            chatNotificationService.notifyAdminsNewMessage(msgNotification);
        }

        // 如果沒有文字訊息，只是操作記錄，不需要 AI 回應
        if (request.getMessage() == null || request.getMessage().isEmpty()) {
            return ResponseEntity.ok(ChatResponse.builder()
                .sessionId(sessionId)
                .response(null)
                .build());
        }

        // 2. 獲取對話歷史（包含操作記錄）
        List<ChatHistory> history = chatHistoryService.getHistoryForAI(sessionId, 20);

        // 3. 建構動態上下文
        String dynamicContext = chatContextService.buildDynamicContext(user, request.getPageContext());
        String messageWithContext = request.getMessage() + dynamicContext;

        // 4. 清空 ToolCallCollector
        toolCallCollector.clear();

        // 5. AI 生成回覆
        String aiResponse = customerChatService.getAssistant().chat(messageWithContext);

        // 6. 獲取工具呼叫記錄
        String toolCallsJson = toolCallCollector.toJson();
        List<ToolCallDto> toolCalls = toolCallCollector.getToolCalls();

        // 7. 計算信心度
        double confidence = confidenceEvaluator.evaluateConfidence(
            request.getMessage(), aiResponse, toolCallsJson, history
        );

        // 8. 根據信心度決策
        if (confidence >= 0.80) {
            return handleAutoReply(sessionId, userId, user.getUsername(), userMessageId,
                                   aiResponse, confidence, toolCallsJson, toolCalls);
        } else if (confidence >= 0.40) {
            return handleSuggestionMode(sessionId, userId, user.getUsername(), userMessageId,
                                       aiResponse, confidence, toolCallsJson, toolCalls);
        } else {
            return handleLowConfidence(sessionId, userId, userMessageId,
                                      aiResponse, confidence, toolCallsJson);
        }

    } catch (Exception e) {
        log.error("Customer chat error for user {}", userId, e);
        return handleChatError(e, sessionId, userId);
    }
}

private ResponseEntity<ChatResponse> handleAutoReply(...) {
    // 1. 儲存 AI 回覆到 chat_history
    // 2. 記錄到 chat_ai_response (status=AUTO_SENT)
    // 3. WebSocket 推送給客戶
    // 4. WebSocket 推送給管理員
}

private ResponseEntity<ChatResponse> handleSuggestionMode(...) {
    // 1. 記錄到 chat_ai_response (status=PENDING)
    // 2. WebSocket 推送建議給管理員
    // 3. 返回「等待處理」給客戶
}

private ResponseEntity<ChatResponse> handleLowConfidence(...) {
    // 1. 記錄到 chat_ai_response (status=PENDING, 但不顯示建議)
    // 2. WebSocket 推送「需要人工」給管理員
    // 3. 返回「等待處理」給客戶
}
```

---

#### 📝 新增：`backend/src/main/java/com/etrex/oms/controller/AdminChatController.java`

**職責**：管理員操作 API（獨立的 Controller）

**主要方法**：
```java
@RestController
@RequestMapping("/api/admin/chat")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatController {

    // 取得所有活躍會話
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionDto>> getSessions();

    // 取得特定會話的歷史記錄
    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<List<ChatHistory>> getSessionHistory(@PathVariable String sessionId);

    // 發送 AI 建議（未修改）
    @PostMapping("/sessions/{sessionId}/send-suggestion")
    public ResponseEntity<?> sendSuggestion(@RequestBody AdminSendRequest request);

    // 發送修改後的回覆
    @PostMapping("/sessions/{sessionId}/send-modified")
    public ResponseEntity<?> sendModified(@RequestBody AdminSendRequest request);

    // 發送完全手動的回覆
    @PostMapping("/sessions/{sessionId}/send-manual")
    public ResponseEntity<?> sendManual(@RequestBody AdminSendRequest request);

    // 管理員對 AI 回覆進行評價
    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody FeedbackRequest request);
}
```

---

## 第七階段：WebSocket 配置

#### 📝 新增：`backend/src/main/java/com/etrex/oms/config/WebSocketConfig.java`

**職責**：配置 WebSocket (STOMP over SockJS)

**內容**：
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

---

## 第八階段：ChatModelListener 修改

#### 📝 修改：`backend/src/main/java/com/etrex/oms/config/ChatModelListenerConfig.java`

**職責**：在 ChatModelListener 中攔截工具呼叫，記錄到 ToolCallCollector

**修改內容**：
```java
@Bean
public ChatModelListener loggingChatModelListener() {
    return new ChatModelListener() {
        public void onResponse(ChatModelResponse response) {
            // 原有的日誌記錄...

            // 新增：記錄工具呼叫到 ToolCallCollector
            if (response.aiMessage().hasToolExecutionRequests()) {
                response.aiMessage().toolExecutionRequests().forEach(tool -> {
                    toolCallCollector.addToolCall(
                        tool.name(),
                        tool.arguments(),
                        "success", // 需要實際的 result
                        0  // 需要實際的執行時間
                    );
                });
            }
        }
    };
}
```

**問題**：ChatModelListener 目前無法獲取工具執行結果和執行時間，可能需要其他方式攔截。

---

## 第九階段：其他支援類

#### 📝 新增：`backend/src/main/java/com/etrex/oms/dto/SessionDto.java`

**職責**：會話列表的 DTO

**內容**：
```java
public class SessionDto {
    private String sessionId;
    private Long userId;
    private String lastMessage;
    private Long lastMessageTime;
    private Boolean hasUnread;
    private Boolean hasPendingSuggestion;
}
```

---

## 實作順序建議

### Phase 1: 資料庫與基礎設施（第 1-3 天）
1. ✅ 資料庫遷移檔案
2. ✅ Entity 類 (ChatAiResponse, Enum)
3. ✅ Repository 類

### Phase 2: 信心度評估（第 4-5 天）
4. ✅ ConfidenceEvaluator
5. ✅ ToolCallCollector
6. ✅ 測試信心度計算

### Phase 3: AI 回應管理（第 6-7 天）
7. ✅ ChatAiResponseService
8. ✅ DTO 類
9. ✅ 修改 ChatController (customerChat)

### Phase 4: 管理員功能（第 8-10 天）
10. ✅ AdminChatController
11. ✅ WebSocket 配置
12. ✅ ChatNotificationService

### Phase 5: 整合與測試（第 11-14 天）
13. ✅ 整合測試
14. ✅ 前端整合（如需要）
15. ✅ 修復問題

---

## 風險與注意事項

### ⚠️ 技術風險

1. **ToolCallCollector 的攔截點**
   - ChatModelListener 可能無法獲取工具執行結果
   - 可能需要在 OrderTools, RagSearchTool 等工具類中手動記錄

2. **信心度計算成本**
   - 5 個 LLM 評估問題會增加回應時間
   - 建議並行執行，或考慮快取策略

3. **WebSocket 連線管理**
   - 需要處理斷線重連
   - 需要認證機制

4. **並發問題**
   - 多個管理員同時處理同一個建議
   - 需要樂觀鎖或悲觀鎖

### 📋 待確認事項

1. **信心度閾值**：80% / 40% 是否需要可配置？
2. **建議超時**：PENDING 狀態的建議多久後自動過期？
3. **對話歷史長度**：傳遞給 AI 的歷史訊息數量？
4. **工具呼叫記錄**：如何準確獲取執行結果和執行時間？
5. **前端需求**：是否需要同步開發管理員介面？

### ✅ 已確認決策

1. **ChatMemory 策略**：❌ **不使用** langchain4j 的 ChatMemory
   - 理由：需要精細控制對話歷史的載入和管理
   - 實作方式：使用 ChatHistoryService.getHistoryAsChatMessages() 手動從資料庫載入並轉換為 ChatMessage 格式

2. **導航指令處理**：✅ **前端已完整實作**（無需後端額外處理）
   - 前端使用正則表達式解析 `[NAVIGATE:/path]` 指令（`Chatbot.vue:179-188`）
   - 自動從回應中移除指令，只顯示純文字
   - 顯示特殊的導航訊息（紫色漸層背景 + 位置圖示）
   - 延遲 1 秒後自動跳轉至目標頁面
   - **後端只需確保 AI 回應包含 `[NAVIGATE:/path]` 指令即可**（System Prompt 已包含）

### 🔍 現有實作中的重要功能（已有但未在計畫中說明）

#### 1. 導航指令功能

**後端實作**：CustomerChatService 和 AdminChatService 的 System Prompt 已包含導航指令

```
你可以在回應中加入特殊指令來協助用戶跳轉頁面：
格式：[NAVIGATE:/path/to/page]

使用範例：
- 「好的，我為您導航到商品頁面 [NAVIGATE:/products]」
- 「這是您的訂單詳情 [NAVIGATE:/orders/123]」
```

**前端實作**：✅ **已完整實作**（`frontend/src/components/Chatbot.vue:179-188`）

前端使用正則表達式自動解析和處理導航指令：

```typescript
const parseNavigationCommand = (content: string): { path: string | null; cleanContent: string } => {
  const navRegex = /\[NAVIGATE:([^\]]+)\]/g
  let path: string | null = null
  const cleanContent = content.replace(navRegex, (match, capturedPath) => {
    path = capturedPath
    return '' // Remove the command from display
  }).trim()

  return { path, cleanContent }
}
```

**前端行為**：
1. 自動從 AI 回應中解析 `[NAVIGATE:/path]` 指令
2. 移除指令文字，只顯示純文字內容
3. 顯示特殊的導航訊息（紫色漸層背景 + 位置圖示 + 動畫效果）
4. 延遲 1 秒後使用 `router.push(path)` 自動跳轉
5. 聊天視窗保持開啟

**結論**：
- ❌ **不需要**在後端新增 `navigateTo` 欄位到 ChatResponse
- ❌ **不需要**在後端解析導航指令
- ✅ **只需要**確保 AI 回應包含 `[NAVIGATE:/path]` 指令（System Prompt 已處理）
- ✅ 前端會自動處理所有導航邏輯

---

#### 2. track() 簡易追蹤介面

**現有實作**：ChatHistoryService 已提供簡化的追蹤方法

```java
// 在任何 Controller 中直接調用
@PostMapping("/products/{id}")
public ResponseEntity<?> getProduct(@PathVariable Long id) {
    chatHistoryService.track("查看商品 #" + id);
    // ... 業務邏輯
}

// 或傳入 user
chatHistoryService.track(user, "完成結帳");
```

**優勢**：
- 自動從 SecurityContext 獲取當前用戶
- 失敗不影響主流程
- 統一格式記錄為 ACTION 類型

**建議**：
- 在實作計畫中說明這個功能
- 建議在關鍵業務操作中使用（如：下單、付款、出貨）

---

#### 3. buildConversationContext() 方法

**現有實作**：已提供格式化對話歷史的方法

```java
String context = chatHistoryService.buildConversationContext(sessionId, 10);
// 輸出：
// (開啟頁面 /products/123)
// 用戶: 這個商品多少錢？
// 助手: 這個商品是 NT$ 15000
```

**用途**：
- 可用於信心度評估時傳入對話歷史
- 可用於日誌記錄和調試

**注意**：
- 這個方法返回純文字，不是 ChatMessage 格式
- 如果需要使用 langchain4j 的 ChatMemory，還需要 `getHistoryAsChatMessages()`

---

## 檔案清單總結

### 新增檔案 (20 個)

#### 資料庫
1. `db/migration/V{version}__create_chat_ai_response.sql`

#### Entity
2. `entity/ChatAiResponse.java`
3. `entity/AiResponseStatus.java`
4. `entity/FeedbackType.java`

#### Repository
5. `repository/ChatAiResponseRepository.java`

#### DTO
6. `dto/AiSuggestionDto.java`
7. `dto/ToolCallDto.java`
8. `dto/FeedbackRequest.java`
9. `dto/AdminSendRequest.java`
10. `dto/SessionDto.java`
11. `dto/ws/NewMessageNotification.java`
12. `dto/ws/UserMessageNotification.java`
13. `dto/ws/SessionUpdateNotification.java`
14. `dto/ws/UserActionNotification.java`

#### Service
15. `service/ChatAiResponseService.java`
16. `service/ConfidenceEvaluator.java`
17. `service/ToolCallCollector.java`
18. `service/ChatNotificationService.java`

#### Controller
19. `controller/AdminChatController.java`

#### Config
20. `config/WebSocketConfig.java`

### 修改檔案 (3 個)

1. `controller/ChatController.java` - 升級 customerChat 方法，加入信心度評估和路由決策
2. `service/ChatHistoryService.java` - 新增 getHistoryAsChatMessages 方法（手動載入對話歷史）
3. `config/ChatModelListenerConfig.java` - 攔截工具呼叫，記錄到 ToolCallCollector

---

**文件版本**：v1.1
**建立日期**：2025-01-15
**最後更新**：2025-01-15
**預估工時**：14 天（1 位後端工程師）

**v1.1 更新說明**：
- ✅ 明確記錄「不使用 langchain4j ChatMemory」決策
- ✅ 補充前端導航指令處理的完整實作細節（Chatbot.vue）
- ✅ 確認後端無需新增 navigateTo 欄位或解析導航指令
- ✅ 修正修改檔案數量為 3 個（移除不需要的 ChatResponse.java 修改）
- ✅ 新增「未來升級機制」章節：結構化前端操作指令系統

---

## 🚀 未來升級機制

### 前端操作指令系統（Frontend Action System）

**現況**：
- 目前使用文字嵌入式的 `[NAVIGATE:/path]` 指令
- 前端使用正則表達式解析文字內容
- 只支援單一導航操作

**升級目標**：
將前端操作指令從文字解析升級為結構化的資料格式，類似後端的 Tool Call 機制，讓 AI 可以驅動前端執行各種操作。

---

### 升級後的資料結構

#### 新增 DTO：`FrontendAction.java`

```java
package com.etrex.oms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * 前端操作指令 DTO
 * 讓 AI 可以驅動前端執行各種操作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrontendAction {
    /**
     * 操作類型
     * - navigate: 頁面導航
     * - open_modal: 開啟 Modal
     * - scroll_to: 滾動到指定位置
     * - highlight_element: 高亮顯示元素
     * - fill_form: 自動填寫表單
     * - show_notification: 顯示通知
     */
    private String actionType;

    /**
     * 操作參數（依 actionType 而異）
     */
    private Map<String, Object> params;

    /**
     * 延遲執行時間（毫秒），預設 0
     */
    private Integer delay;

    /**
     * 是否需要用戶確認（預設 false）
     * true: 在對話窗中顯示確認訊息，等待用戶同意
     * false: 直接執行
     */
    private Boolean requireConfirmation;

    /**
     * 確認提示訊息（當 requireConfirmation=true 時使用）
     * 例如：「AI 建議為您導航到訂單頁面，是否同意？」
     */
    private String confirmationMessage;

    /**
     * 操作的顯示名稱（用於確認 UI）
     * 例如：「導航到訂單頁面」、「自動填寫表單」
     */
    private String displayName;
}
```

#### 修改 DTO：`ChatResponse.java`

```java
package com.etrex.oms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String response;
    private String sessionId;

    /**
     * 前端操作指令列表（升級機制）
     * 前端會按順序執行這些操作
     */
    private List<FrontendAction> frontendActions;
}
```

---

### 操作類型範例

#### 1. 頁面導航（navigate）

**用途**：導航到指定頁面

```java
FrontendAction.builder()
    .actionType("navigate")
    .params(Map.of(
        "path", "/products/123",
        "openInNewTab", false
    ))
    .delay(1000)
    .build()
```

**前端執行**：
```typescript
if (action.actionType === 'navigate') {
  setTimeout(() => {
    const path = action.params.path
    const openInNewTab = action.params.openInNewTab || false

    if (openInNewTab) {
      window.open(path, '_blank')
    } else {
      router.push(path)
    }
  }, action.delay || 0)
}
```

---

#### 3. 滾動到元素（scroll_to）

**用途**：滾動頁面到指定元素

```java
FrontendAction.builder()
    .actionType("scroll_to")
    .params(Map.of(
        "selector", "#order-summary",
        "behavior", "smooth"
    ))
    .build()
```

**前端執行**：
```typescript
if (action.actionType === 'scroll_to') {
  const element = document.querySelector(action.params.selector)
  if (element) {
    element.scrollIntoView({
      behavior: action.params.behavior || 'smooth'
    })
  }
}
```

---

#### 4. 高亮顯示元素（highlight_element）

**用途**：高亮顯示特定元素（引導用戶注意）

```java
FrontendAction.builder()
    .actionType("highlight_element")
    .params(Map.of(
        "selector", ".checkout-button",
        "duration", 3000,
        "color", "#ff6b6b"
    ))
    .build()
```

**前端執行**：
```typescript
if (action.actionType === 'highlight_element') {
  const element = document.querySelector(action.params.selector)
  if (element) {
    element.classList.add('ai-highlight')
    setTimeout(() => {
      element.classList.remove('ai-highlight')
    }, action.params.duration || 3000)
  }
}
```

---

#### 5. 自動填寫表單（fill_form）

**用途**：自動填寫表單欄位（需用戶確認）

```java
FrontendAction.builder()
    .actionType("fill_form")
    .params(Map.of(
        "formId", "checkout-form",
        "fields", Map.of(
            "name", "張三",
            "phone", "0912345678",
            "address", "台北市信義區..."
        ),
        "requireConfirmation", true
    ))
    .build()
```

**前端執行**：
```typescript
if (action.actionType === 'fill_form') {
  const fields = action.params.fields
  const requireConfirmation = action.params.requireConfirmation || true

  if (requireConfirmation) {
    ElMessageBox.confirm('AI 建議自動填寫以下資料，是否同意？', '確認')
      .then(() => {
        Object.entries(fields).forEach(([key, value]) => {
          const input = document.querySelector(`#${key}`)
          if (input) input.value = value
        })
      })
  }
}
```

---

#### 6. 顯示通知（show_notification）

**用途**：顯示前端通知訊息

```java
FrontendAction.builder()
    .actionType("show_notification")
    .params(Map.of(
        "type", "success",  // success / warning / error / info
        "message", "您的訂單已建立成功！",
        "duration", 3000
    ))
    .build()
```

**前端執行**：
```typescript
if (action.actionType === 'show_notification') {
  ElNotification({
    type: action.params.type || 'info',
    message: action.params.message,
    duration: action.params.duration || 3000
  })
}
```

---

### 後端實作範例

#### ChatController 中生成前端操作

```java
private ResponseEntity<ChatResponse> handleAutoReply(...) {
    // 1. 儲存 AI 回覆到 chat_history
    ChatHistory assistantMsg = chatHistoryService.saveMessage(
        sessionId, userId, "ASSISTANT", aiResponse
    );

    // 2. 解析 AI 回應中的前端操作指令
    List<FrontendAction> frontendActions = parseFrontendActions(aiResponse);

    // 3. 記錄到 chat_ai_response
    ChatAiResponse aiRecord = chatAiResponseService.saveAiResponse(
        sessionId, userMessageId, aiResponse, confidence,
        toolCallsJson, AiResponseStatus.AUTO_SENT
    );
    chatAiResponseService.markAsAutoSent(aiRecord.getId(), assistantMsg.getId());

    // 4. 構建回應（包含前端操作指令）
    ChatResponse response = ChatResponse.builder()
        .response(removeActionCommands(aiResponse))  // 移除指令文字
        .sessionId(sessionId)
        .frontendActions(frontendActions)  // 結構化前端操作
        .build();

    // 5. WebSocket 推送（略）

    return ResponseEntity.ok(response);
}

/**
 * 解析前端操作指令
 * 目前支援文字嵌入式的 [NAVIGATE:/path]
 * 未來可擴展為更複雜的指令解析
 */
private List<FrontendAction> parseFrontendActions(String aiResponse) {
    List<FrontendAction> actions = new ArrayList<>();

    // 解析 [NAVIGATE:/path]
    Pattern navPattern = Pattern.compile("\\[NAVIGATE:([^\\]]+)\\]");
    Matcher matcher = navPattern.matcher(aiResponse);

    while (matcher.find()) {
        String path = matcher.group(1);
        actions.add(FrontendAction.builder()
            .actionType("navigate")
            .params(Map.of("path", path))
            .delay(1000)
            .build());
    }

    // 未來可擴展解析其他指令格式
    // 例如：[OPEN_MODAL:product-detail:123]
    // 例如：[SCROLL_TO:#order-summary]

    return actions;
}

/**
 * 移除前端操作指令文字
 */
private String removeActionCommands(String text) {
    return text.replaceAll("\\[NAVIGATE:[^\\]]+\\]", "").trim();
}
```

---

### 前端執行器實作

#### 新增：`frontend/src/composables/useFrontendActions.ts`

```typescript
import { useRouter } from 'vue-router'
import { ElNotification, ElMessageBox } from 'element-plus'

export interface FrontendAction {
  actionType: string
  params: Record<string, any>
  delay?: number
}

export function useFrontendActions() {
  const router = useRouter()

  /**
   * 執行前端操作列表
   */
  const executeActions = async (actions: FrontendAction[]) => {
    for (const action of actions) {
      await executeAction(action)
    }
  }

  /**
   * 執行單一前端操作
   */
  const executeAction = async (action: FrontendAction) => {
    const delay = action.delay || 0

    await new Promise(resolve => setTimeout(resolve, delay))

    switch (action.actionType) {
      case 'navigate':
        handleNavigate(action.params)
        break

      case 'open_modal':
        handleOpenModal(action.params)
        break

      case 'scroll_to':
        handleScrollTo(action.params)
        break

      case 'highlight_element':
        handleHighlightElement(action.params)
        break

      case 'fill_form':
        await handleFillForm(action.params)
        break

      case 'show_notification':
        handleShowNotification(action.params)
        break

      default:
        console.warn('Unknown action type:', action.actionType)
    }
  }

  const handleNavigate = (params: any) => {
    const path = params.path
    const openInNewTab = params.openInNewTab || false

    if (openInNewTab) {
      window.open(path, '_blank')
    } else {
      router.push(path)
    }
  }

  const handleOpenModal = (params: any) => {
    // 使用 EventBus 或其他方式觸發 Modal
    window.dispatchEvent(new CustomEvent('open-modal', {
      detail: params
    }))
  }

  const handleScrollTo = (params: any) => {
    const element = document.querySelector(params.selector)
    if (element) {
      element.scrollIntoView({
        behavior: params.behavior || 'smooth'
      })
    }
  }

  const handleHighlightElement = (params: any) => {
    const element = document.querySelector(params.selector)
    if (element) {
      element.classList.add('ai-highlight')
      setTimeout(() => {
        element.classList.remove('ai-highlight')
      }, params.duration || 3000)
    }
  }

  const handleFillForm = async (params: any) => {
    const fields = params.fields
    const requireConfirmation = params.requireConfirmation !== false

    if (requireConfirmation) {
      try {
        await ElMessageBox.confirm(
          'AI 建議自動填寫以下資料，是否同意？',
          '確認'
        )
        fillFormFields(fields)
      } catch {
        // 用戶取消
      }
    } else {
      fillFormFields(fields)
    }
  }

  const fillFormFields = (fields: Record<string, any>) => {
    Object.entries(fields).forEach(([key, value]) => {
      const input = document.querySelector(`#${key}`) as HTMLInputElement
      if (input) {
        input.value = String(value)
        input.dispatchEvent(new Event('input', { bubbles: true }))
      }
    })
  }

  const handleShowNotification = (params: any) => {
    ElNotification({
      type: params.type || 'info',
      message: params.message,
      duration: params.duration || 3000
    })
  }

  return {
    executeActions,
    executeAction
  }
}
```

#### 修改：`frontend/src/components/Chatbot.vue`

```typescript
import { useFrontendActions } from '@/composables/useFrontendActions'

const { executeActions } = useFrontendActions()

const sendMessage = async () => {
  // ... 現有邏輯 ...

  const response = await chatApi.sendMessage(userMessage, pageContext)

  // 執行前端操作（如果有）
  if (response.frontendActions && response.frontendActions.length > 0) {
    await executeActions(response.frontendActions)
  }

  // 顯示 AI 回應
  messages.value.push({
    content: response.response,
    type: 'bot',
    timestamp: Date.now()
  })
}
```

---

### 升級路徑

#### Phase 1：向後兼容（目前階段）
- ✅ 保持現有的 `[NAVIGATE:/path]` 文字解析
- ✅ 前端繼續使用正則表達式處理

#### Phase 2：雙軌並行
- 新增 `frontendActions` 欄位到 ChatResponse
- 後端同時支援文字解析和結構化指令
- 前端優先使用 `frontendActions`，降級到文字解析
- System Prompt 保持不變（仍使用 `[NAVIGATE:/path]`）

#### Phase 3：完全遷移（未來）
- 擴展 AI 的 System Prompt，教會 AI 使用更多前端操作
- 可能需要自訂 Tool 來生成 FrontendAction
- 移除舊的文字解析方式

---

### 優勢分析

#### 現有方式（文字嵌入）
✅ 優點：
- 簡單直觀
- 不需要修改後端 DTO
- AI 容易理解和生成

❌ 缺點：
- 只能嵌入在回應文字中
- 無法傳遞複雜參數
- 擴展性差（每種操作需要新的正則）
- 難以執行多個操作

#### 升級方式（結構化指令）
✅ 優點：
- 結構化、可擴展
- 支援複雜參數
- 可以一次執行多個操作
- 易於版本控制和調試
- 類似 Tool Call，符合 AI 輔助理念
- 可以記錄前端操作到資料庫

❌ 缺點：
- 需要修改後端 DTO
- 前端需要實作執行器
- AI 需要學習新的指令格式（或使用自訂 Tool）

---

### 實作優先級

**目前階段**：保持現有的文字解析方式
**短期目標**：完成 AI 輔助客服系統的核心功能
**中期目標**：評估前端操作指令的實際需求
**長期目標**：如有需要，再升級為結構化指令系統

**建議**：在文件中保留此升級路徑，但**暫不實作**，等核心功能穩定後再評估。

---

## 🔐 用戶確認機制（User Confirmation）

### 設計理念

某些前端操作可能涉及敏感行為或重要決策，應該在執行前取得用戶明確同意：

**需要確認的操作**：
- 導航到其他頁面（可能中斷當前操作）
- 自動填寫表單（涉及個人資料）
- 提交訂單或付款（金錢相關）
- 刪除資料（不可逆操作）

**不需要確認的操作**：
- 顯示通知訊息
- 滾動到元素
- 高亮顯示元素
- 開啟 Modal（僅顯示資訊）

---

### 確認 UI 設計

#### 在對話窗中顯示確認卡片

```vue
<!-- Chatbot.vue 中的確認卡片 -->
<template>
  <div class="chatbot-container">
    <!-- ... 現有的訊息列表 ... -->

    <div
      v-for="(message, index) in messages"
      :key="index"
      :class="['message', message.type]"
    >
      <!-- 一般訊息 -->
      <div v-if="message.type === 'bot'" class="message-content">
        {{ message.content }}
      </div>

      <!-- 確認卡片 -->
      <div v-if="message.pendingAction" class="action-confirmation-card">
        <div class="confirmation-header">
          <el-icon class="warning-icon"><Warning /></el-icon>
          <span class="action-name">{{ message.pendingAction.displayName }}</span>
        </div>

        <div class="confirmation-message">
          {{ message.pendingAction.confirmationMessage }}
        </div>

        <div class="confirmation-details" v-if="message.pendingAction.params">
          <div class="detail-item" v-for="(value, key) in getDisplayParams(message.pendingAction)" :key="key">
            <span class="detail-label">{{ formatParamLabel(key) }}:</span>
            <span class="detail-value">{{ value }}</span>
          </div>
        </div>

        <div class="confirmation-actions">
          <el-button
            type="primary"
            size="small"
            @click="confirmAction(index)"
            :loading="message.confirmationLoading"
          >
            同意執行
          </el-button>
          <el-button
            size="small"
            @click="rejectAction(index)"
            :disabled="message.confirmationLoading"
          >
            拒絕
          </el-button>
        </div>

        <!-- 確認狀態顯示 -->
        <div v-if="message.actionStatus" :class="['action-status', message.actionStatus]">
          <el-icon v-if="message.actionStatus === 'confirmed'"><CircleCheck /></el-icon>
          <el-icon v-if="message.actionStatus === 'rejected'"><CircleClose /></el-icon>
          <el-icon v-if="message.actionStatus === 'executed'"><CircleCheck /></el-icon>
          <span>{{ getStatusText(message.actionStatus) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.action-confirmation-card {
  background: linear-gradient(135deg, #fff5e6 0%, #ffe6cc 100%);
  border: 2px solid #ff9800;
  border-radius: 12px;
  padding: 16px;
  margin: 8px 0;
  max-width: 85%;
}

.confirmation-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.warning-icon {
  font-size: 20px;
  color: #ff9800;
}

.action-name {
  font-weight: 600;
  color: #e65100;
  font-size: 14px;
}

.confirmation-message {
  color: #5d4037;
  margin-bottom: 12px;
  line-height: 1.5;
  font-size: 14px;
}

.confirmation-details {
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  font-size: 13px;
}

.detail-item {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-label {
  font-weight: 600;
  color: #6d4c41;
  min-width: 60px;
}

.detail-value {
  color: #4e342e;
  font-family: monospace;
}

.confirmation-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.action-status {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.action-status.confirmed {
  background: #e8f5e9;
  color: #2e7d32;
}

.action-status.rejected {
  background: #ffebee;
  color: #c62828;
}

.action-status.executed {
  background: #e3f2fd;
  color: #1565c0;
}
</style>
```

---

### 前端確認流程實作

#### 修改：`frontend/src/composables/useFrontendActions.ts`

```typescript
export function useFrontendActions() {
  const router = useRouter()

  /**
   * 執行前端操作列表（支援確認機制）
   */
  const executeActions = async (
    actions: FrontendAction[],
    onConfirmationRequired?: (action: FrontendAction, index: number) => Promise<boolean>
  ) => {
    for (let i = 0; i < actions.length; i++) {
      const action = actions[i]

      // 檢查是否需要確認
      if (action.requireConfirmation) {
        if (onConfirmationRequired) {
          // 使用自訂確認處理（在對話窗中顯示確認卡片）
          const confirmed = await onConfirmationRequired(action, i)
          if (!confirmed) {
            console.log('User rejected action:', action.actionType)
            continue // 跳過此操作
          }
        } else {
          // 使用內建確認對話框（降級方案）
          const confirmed = await confirmWithDialog(action)
          if (!confirmed) {
            console.log('User rejected action:', action.actionType)
            continue
          }
        }
      }

      // 執行操作
      await executeAction(action)
    }
  }

  /**
   * 內建確認對話框（降級方案）
   */
  const confirmWithDialog = async (action: FrontendAction): Promise<boolean> => {
    try {
      await ElMessageBox.confirm(
        action.confirmationMessage || `確定要執行「${action.displayName}」嗎？`,
        '確認操作',
        {
          confirmButtonText: '同意',
          cancelButtonText: '拒絕',
          type: 'warning'
        }
      )
      return true
    } catch {
      return false
    }
  }

  // ... 其他方法保持不變 ...

  return {
    executeActions,
    executeAction,
    confirmWithDialog
  }
}
```

#### 修改：`frontend/src/components/Chatbot.vue`

```typescript
<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useFrontendActions } from '@/composables/useFrontendActions'

interface Message {
  content: string
  type: 'user' | 'bot' | 'action' | 'navigation'
  timestamp: number
  pendingAction?: FrontendAction
  actionStatus?: 'confirmed' | 'rejected' | 'executed'
  confirmationLoading?: boolean
}

const messages = ref<Message[]>([])
const { executeActions, executeAction } = useFrontendActions()

const sendMessage = async () => {
  // ... 現有的發送訊息邏輯 ...

  const response = await chatApi.sendMessage(userMessage, pageContext)

  // 顯示 AI 回應
  messages.value.push({
    content: response.response,
    type: 'bot',
    timestamp: Date.now()
  })

  // 處理前端操作（支援確認機制）
  if (response.frontendActions && response.frontendActions.length > 0) {
    await executeActions(response.frontendActions, handleActionConfirmation)
  }
}

/**
 * 處理前端操作確認
 * 在對話窗中顯示確認卡片，等待用戶決策
 */
const handleActionConfirmation = async (
  action: FrontendAction,
  index: number
): Promise<boolean> => {
  return new Promise((resolve) => {
    // 在對話窗中顯示確認卡片
    const confirmationMessage: Message = {
      content: '',
      type: 'bot',
      timestamp: Date.now(),
      pendingAction: action,
      actionStatus: undefined,
      confirmationLoading: false
    }

    messages.value.push(confirmationMessage)
    scrollToBottom()

    // 儲存 resolve 函數，供確認/拒絕按鈕使用
    const messageIndex = messages.value.length - 1
    confirmationResolvers.set(messageIndex, resolve)
  })
}

// 儲存確認的 Promise resolvers
const confirmationResolvers = new Map<number, (confirmed: boolean) => void>()

/**
 * 用戶同意執行操作
 */
const confirmAction = async (messageIndex: number) => {
  const message = messages.value[messageIndex]
  if (!message.pendingAction) return

  message.confirmationLoading = true

  try {
    // 執行操作
    await executeAction(message.pendingAction)

    // 更新狀態
    message.actionStatus = 'executed'
    message.confirmationLoading = false

    // 解析 Promise
    const resolver = confirmationResolvers.get(messageIndex)
    if (resolver) {
      resolver(true)
      confirmationResolvers.delete(messageIndex)
    }

    // 記錄用戶確認行為（可選）
    await recordUserConfirmation(message.pendingAction, true)
  } catch (error) {
    console.error('Action execution failed:', error)
    message.confirmationLoading = false
    ElMessage.error('操作執行失敗，請稍後再試')

    // 仍然解析 Promise（視為已確認但執行失敗）
    const resolver = confirmationResolvers.get(messageIndex)
    if (resolver) {
      resolver(true)
      confirmationResolvers.delete(messageIndex)
    }
  }
}

/**
 * 用戶拒絕執行操作
 */
const rejectAction = async (messageIndex: number) => {
  const message = messages.value[messageIndex]
  if (!message.pendingAction) return

  // 更新狀態
  message.actionStatus = 'rejected'

  // 解析 Promise
  const resolver = confirmationResolvers.get(messageIndex)
  if (resolver) {
    resolver(false)
    confirmationResolvers.delete(messageIndex)
  }

  // 記錄用戶拒絕行為（可選）
  await recordUserConfirmation(message.pendingAction, false)

  // 顯示拒絕訊息
  ElMessage.info('已取消操作')
}

/**
 * 記錄用戶確認/拒絕行為（用於 AI 學習）
 */
const recordUserConfirmation = async (
  action: FrontendAction,
  confirmed: boolean
) => {
  try {
    await chatApi.recordActionConfirmation({
      actionType: action.actionType,
      params: action.params,
      confirmed,
      timestamp: Date.now()
    })
  } catch (error) {
    console.error('Failed to record user confirmation:', error)
  }
}

/**
 * 格式化參數顯示
 */
const getDisplayParams = (action: FrontendAction) => {
  const params = action.params
  const displayParams: Record<string, any> = {}

  // 根據操作類型選擇要顯示的參數
  switch (action.actionType) {
    case 'navigate':
      displayParams['目標頁面'] = params.path
      if (params.openInNewTab) {
        displayParams['開啟方式'] = '新分頁'
      }
      break

    case 'fill_form':
      Object.entries(params.fields || {}).forEach(([key, value]) => {
        displayParams[formatParamLabel(key)] = value
      })
      break

    case 'scroll_to':
      displayParams['目標元素'] = params.selector
      break

    case 'highlight_element':
      displayParams['目標元素'] = params.selector
      displayParams['持續時間'] = `${params.duration}ms`
      break

    default:
      Object.entries(params).forEach(([key, value]) => {
        displayParams[key] = value
      })
  }

  return displayParams
}

const formatParamLabel = (key: string): string => {
  const labelMap: Record<string, string> = {
    path: '目標頁面',
    name: '姓名',
    phone: '電話',
    address: '地址',
    email: '電子郵件',
    selector: '目標元素',
    duration: '持續時間'
  }
  return labelMap[key] || key
}

const getStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    confirmed: '已同意',
    rejected: '已拒絕',
    executed: '執行成功'
  }
  return statusMap[status] || status
}
</script>
```

---

### 後端範例：帶確認的前端操作

#### 需要確認的導航操作

```java
FrontendAction.builder()
    .actionType("navigate")
    .params(Map.of("path", "/checkout"))
    .requireConfirmation(true)
    .confirmationMessage("AI 建議為您導航到結帳頁面，是否同意？")
    .displayName("導航到結帳頁面")
    .delay(0)
    .build()
```

#### 需要確認的表單填寫

```java
FrontendAction.builder()
    .actionType("fill_form")
    .params(Map.of(
        "formId", "checkout-form",
        "fields", Map.of(
            "name", "張三",
            "phone", "0912345678",
            "address", "台北市信義區信義路五段7號"
        )
    ))
    .requireConfirmation(true)
    .confirmationMessage("AI 根據您的歷史資料建議填寫以下資訊，是否同意？")
    .displayName("自動填寫收件資訊")
    .build()
```

#### 不需要確認的操作

```java
// 高亮顯示（安全操作，不需確認）
FrontendAction.builder()
    .actionType("highlight_element")
    .params(Map.of(
        "selector", ".checkout-button",
        "duration", 3000
    ))
    .requireConfirmation(false)
    .displayName("高亮顯示結帳按鈕")
    .build()

// 顯示通知（安全操作，不需確認）
FrontendAction.builder()
    .actionType("show_notification")
    .params(Map.of(
        "type", "info",
        "message", "您的訂單已建立成功！",
        "duration", 3000
    ))
    .requireConfirmation(false)
    .displayName("顯示成功通知")
    .build()
```

---

### 確認行為記錄（可選功能）

#### 新增 API：記錄用戶確認/拒絕

```java
// ChatController.java
@PostMapping("/action-confirmation")
public ResponseEntity<?> recordActionConfirmation(
    @RequestBody ActionConfirmationRequest request,
    @AuthenticationPrincipal User user
) {
    chatHistoryService.saveAction(
        String.valueOf(user.getId()),
        user.getId(),
        request.isConfirmed() ? "confirm_action" : "reject_action",
        request.getActionType() + ":" + objectMapper.writeValueAsString(request.getParams())
    );

    return ResponseEntity.ok().build();
}

// DTO
@Data
public class ActionConfirmationRequest {
    private String actionType;
    private Map<String, Object> params;
    private Boolean confirmed;
    private Long timestamp;
}
```

**用途**：
- 記錄用戶對 AI 建議操作的接受/拒絕行為
- 用於分析哪些操作容易被接受/拒絕
- 幫助改進 AI 的操作建議策略
- 可用於個人化學習（記住用戶偏好）

---

### 確認策略建議

#### 預設需要確認的操作類型

```java
public class FrontendActionService {

    private static final Set<String> CONFIRMATION_REQUIRED_ACTIONS = Set.of(
        "navigate",      // 導航（可能中斷當前流程）
        "fill_form",     // 填寫表單（涉及個人資料）
        "submit_form",   // 提交表單（不可逆）
        "delete_item"    // 刪除操作（不可逆）
    );

    /**
     * 建立前端操作，自動判斷是否需要確認
     */
    public FrontendAction createAction(
        String actionType,
        Map<String, Object> params,
        String displayName
    ) {
        boolean needsConfirmation = CONFIRMATION_REQUIRED_ACTIONS.contains(actionType);

        return FrontendAction.builder()
            .actionType(actionType)
            .params(params)
            .displayName(displayName)
            .requireConfirmation(needsConfirmation)
            .confirmationMessage(generateConfirmationMessage(actionType, params))
            .build();
    }

    private String generateConfirmationMessage(String actionType, Map<String, Object> params) {
        switch (actionType) {
            case "navigate":
                return String.format("AI 建議為您導航到「%s」，是否同意？", params.get("path"));
            case "fill_form":
                return "AI 根據您的歷史資料建議填寫表單，是否同意？";
            case "submit_form":
                return "AI 建議提交此表單，是否同意？";
            case "delete_item":
                return "AI 建議刪除此項目，是否同意？此操作無法復原。";
            default:
                return String.format("AI 建議執行「%s」操作，是否同意？", actionType);
        }
    }
}
```

---

### 安全性考量

1. **敏感操作必須確認**
   - 所有涉及資料修改、金錢、個人資訊的操作都應該要求確認
   - 即使 AI 信心度很高，仍應要求用戶確認

2. **確認訊息應該清楚明確**
   - 告訴用戶「AI 要做什麼」
   - 顯示關鍵參數（如導航目標、要填寫的資料）
   - 使用淺顯易懂的語言

3. **提供詳細資訊**
   - 在確認卡片中顯示操作的詳細參數
   - 讓用戶可以審查 AI 的建議內容

4. **記錄用戶決策**
   - 記錄用戶的確認/拒絕行為
   - 用於改進 AI 策略和個人化體驗

5. **降級方案**
   - 如果對話窗確認機制失敗，使用內建對話框
   - 確保用戶始終有機會拒絕操作
