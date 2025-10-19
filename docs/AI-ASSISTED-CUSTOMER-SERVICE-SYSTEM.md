# AI 輔助客服系統設計文件

## 目錄
1. [系統概述](#系統概述)
2. [雙聊天場景說明](#雙聊天場景說明)
3. [核心概念](#核心概念)
4. [架構設計](#架構設計)
5. [資料庫設計](#資料庫設計)
6. [流程設計](#流程設計)
7. [介面設計](#介面設計)
8. [WebSocket 通訊設計](#websocket-通訊設計)
9. [信心度計算策略](#信心度計算策略)

---

## 系統概述

### 設計理念

這是一個 **AI 輔助真人客服** 的混合系統，而非傳統的「AI 或真人」二選一模式。

**核心思想**：
- 用戶視角：始終在與「客服」對話（無法分辨是 AI 還是真人）
- 實際運作：AI 根據信心度決定是否自動回覆或提供建議給管理員
- 管理員角色：AI 的監督者和協作者，而非替代品

### 系統目標

1. **效率**：簡單問題由 AI 自動處理，管理員專注於複雜問題
2. **品質**：複雜或敏感問題由真人處理，確保服務品質
3. **無縫**：用戶體驗一致，不會感受到切換
4. **持續改進**：收集數據用於優化 AI 表現

---

## 雙聊天場景說明

### 系統中的兩種聊天場景

本系統實際包含兩種不同性質的聊天場景，需要不同的處理策略：

#### 1. 客戶問 AI（需要 AI 輔助客服架構）

**端點**：`POST /api/chat`

**使用者**：一般客戶

**目的**：解決客戶問題、提供客戶服務

**背後機制**：AI + 真人客服（雙層保障）

**需要的功能**：
- ✅ 信心度評估（確保服務品質）
- ✅ 建議模式（40-80% 信心度時）
- ✅ 管理員審核介面
- ✅ WebSocket 即時推送
- ✅ chat_ai_response 記錄
- ✅ chat_ai_feedback 評價

**原因**：
- 直接影響客戶體驗和公司形象
- 需要品質保證機制
- 複雜或敏感問題必須由真人處理

---

#### 2. 管理者問 AI（純知識管理系統）

**端點**：`POST /api/chat/admin`

**使用者**：管理員/客服人員

**目的**：查詢知識、輔助決策、快速獲取資訊

**背後機制**：純 AI（知識管理系統）

**需要的功能**：
- ✅ 即時 AI 回答
- ✅ 對話記錄（chat_history）
- ❌ 不需要信心度評估（管理員自己判斷）
- ❌ 不需要建議模式
- ❌ 不需要真人審核
- ❌ 不需要 WebSocket 推送

**原因**：
- 管理員本身是專業人員，具備判斷能力
- 使用 AI 是為了快速查詢知識，不是對外服務
- 不需要「再一層管理員」審核
- 管理員可以自己判斷 AI 回答的正確性

---

### 架構相容性與實施策略

#### 架構設計：雙軌制

```
┌─────────────────────────────────────────────────────────┐
│                    ChatController                        │
├──────────────────────┬──────────────────────────────────┤
│  POST /api/chat      │  POST /api/chat/admin            │
│  (客戶聊天)           │  (管理者聊天)                     │
├──────────────────────┼──────────────────────────────────┤
│  新的 AI 輔助流程     │  保持原有簡單流程                 │
│  ├─ 保存訊息         │  ├─ 保存訊息                      │
│  ├─ AI 生成回覆      │  ├─ AI 生成回覆                   │
│  ├─ 信心度評估 🆕    │  ├─ 保存回覆                      │
│  ├─ 決策路由 🆕      │  └─ 直接返回                      │
│  │  ├─ ≥80%: 自動   │                                   │
│  │  ├─ 40-80%: 建議 │  ✓ 不需要信心度評估               │
│  │  └─ <40%: 人工   │  ✓ 不需要建議模式                 │
│  ├─ chat_ai_response │  ✓ 不需要 WebSocket               │
│  └─ WebSocket 推送   │  ✓ 向下相容，無需修改             │
└──────────────────────┴──────────────────────────────────┘
```

---

#### 資料流對比

**客戶聊天（新架構）**：
```
用戶訊息 → chat_history
    ↓
AI 生成回覆 + 信心度評估
    ↓
    ├─ ≥80%: 自動回覆 → chat_history + chat_ai_response
    ├─ 40-80%: 建議 → chat_ai_response (PENDING) + WebSocket 推送
    └─ <40%: 等待人工 → chat_ai_response (PENDING)
```

**管理者聊天（原架構，不變）**：
```
管理者訊息 → chat_history
    ↓
AI 生成回覆
    ↓
AI 回覆 → chat_history
    ↓
直接返回
```

---

#### 資料庫使用差異

| 表名 | 客戶聊天 | 管理者聊天 |
|------|---------|-----------|
| `chat_history` | ✅ 記錄對話 | ✅ 記錄對話 |
| `chat_ai_response` | ✅ 記錄 AI 表現 + 評價 | ❌ 不記錄 |

---

#### 向下相容性

✅ **完全向下相容**

- `POST /api/chat/admin` 端點保持不變
- `AdminChatService` 邏輯保持不變
- 只有 `POST /api/chat` 需要升級為新架構
- 兩個流程互不干擾

---

#### 實作優先級

**第一階段（保持穩定）**：
- ✅ `POST /api/chat/admin` - 完全不動
- ✅ 管理員知識查詢功能正常運作

**第二階段（新增功能）**：
- 🔨 升級 `POST /api/chat` 為 AI 輔助客服架構
- 🔨 新增信心度計算
- 🔨 新增建議模式
- 🔨 新增管理員審核介面
- 🔨 新增 WebSocket 推送

---

### 為什麼這樣設計？

1. **職責分離**：
   - 客戶聊天 = 對外服務（需要品質保證）
   - 管理者聊天 = 內部工具（管理員自行判斷）

2. **複雜度控制**：
   - 管理者不需要複雜的信心度評估和審核流程
   - 避免過度設計

3. **風險管理**：
   - 客戶聊天錯誤會影響公司形象 → 需要多層保護
   - 管理者聊天錯誤由管理員自己負責 → 不需要額外保護

4. **向下相容**：
   - 保護現有功能，降低開發風險
   - 分階段實施，逐步上線

---

## 核心概念

### 三種運作模式

| 模式 | 觸發條件 | 行為 | 用戶感受 |
|------|---------|------|---------|
| **自動回覆** | 信心度 ≥ 80% | AI 立即回覆，記錄到對話歷史 | 立即收到回覆 |
| **建議模式** | 信心度 40-80% | AI 生成建議，等待管理員審核 | 稍等片刻後收到回覆 |
| **完全人工** | 信心度 < 40% | 不生成建議，等待管理員處理 | 等待客服處理 |

### 關注點分離

系統將「對話記錄」和「AI 元數據」完全分離：

- **對話記錄** (`chat_history`)：只記錄實際發生的對話
- **AI 回應記錄** (`chat_ai_response`)：記錄 AI 的表現和評估

這種設計確保：
- 對話查詢效能高（不需載入 AI 元數據）
- AI 功能可獨立分析和改進
- 未來可彈性移除 AI 功能而不影響對話記錄

---

## 架構設計

### 系統架構圖

```
┌─────────────────────────────────────────────────────────────┐
│                        前端層                                  │
├──────────────────────┬──────────────────────────────────────┤
│  客戶聊天介面          │         管理員客服介面                 │
│  - 發送/接收訊息       │  - Session 列表                        │
│  - WebSocket 即時通訊 │  - 對話詳情                            │
│                       │  - Quick Reply 區（AI 建議）           │
│                       │  - Tool Calls 側邊欄（備查）           │
└──────────────────────┴──────────────────────────────────────┘
                         ↕ WebSocket + HTTP
┌─────────────────────────────────────────────────────────────┐
│                      後端架構                                  │
├─────────────────────────────────────────────────────────────┤
│  Controller 層：                                              │
│  - ChatController（客戶端 API）                               │
│  - AdminChatController（管理員 API）                          │
├─────────────────────────────────────────────────────────────┤
│  Service 層：                                                 │
│  - ConfidenceEvaluator（信心度計算）                          │
│  - ChatNotificationService（WebSocket 推送）                  │
│  - ChatHistoryService（對話記錄管理）                         │
│  - ChatAiResponseService（AI 回應記錄管理）                   │
│  - ToolCallCollector（ThreadLocal 暫存工具呼叫）              │
├─────────────────────────────────────────────────────────────┤
│  AI 層：                                                      │
│  - CustomerChatService（langchain4j wrapper）                │
│  - ChatModelListener（攔截 tool calls）                       │
└─────────────────────────────────────────────────────────────┘
                         ↕
┌─────────────────────────────────────────────────────────────┐
│  資料庫層：                                                    │
│  - chat_history（對話記錄）                                   │
│  - chat_ai_response（AI 回應記錄 + 評價）                     │
└─────────────────────────────────────────────────────────────┘
```

### 技術棧

- **後端框架**：Spring Boot
- **AI 框架**：langchain4j
- **即時通訊**：WebSocket (STOMP)
- **資料庫**：關聯式資料庫（已有 chat_history 表）
- **前端通訊**：SockJS + STOMP client

---

## 資料庫設計

### 表 1: chat_history（對話記錄表）

**職責**：記錄所有實際發送的對話

| 欄位 | 類型 | 說明 |
|------|------|------|
| id | BIGINT | 主鍵 |
| session_id | VARCHAR(255) | 會話 ID |
| user_id | BIGINT | 用戶 ID |
| role | VARCHAR(20) | USER / ASSISTANT |
| message_type | VARCHAR(50) | MESSAGE / ACTION |
| content | TEXT | 訊息內容 |
| action_type | VARCHAR(50) | 操作類型（如果是 ACTION） |
| action_target | VARCHAR(255) | 操作目標 |
| created_at | TIMESTAMP | 建立時間 |

**索引**：
- `idx_session_id` on session_id
- `idx_user_id` on user_id
- `idx_created_at` on created_at

**特點**：
- 保持簡潔，不包含任何 AI 元數據
- 用戶和管理員看到的對話記錄都來自這張表

---

### 表 2: chat_ai_response（AI 回應記錄表）

**職責**：記錄 AI 的建議、信心度、採納情況、以及管理員評價

| 欄位 | 類型 | 說明 |
|------|------|------|
| id | BIGINT | 主鍵 |
| session_id | VARCHAR(255) | 會話 ID |
| user_message_id | BIGINT | 關聯的用戶訊息 ID (FK: chat_history) |
| suggested_response | TEXT | AI 建議的回覆內容 |
| confidence_score | DECIMAL(3,2) | 信心度 (0.00-1.00) |
| tool_calls_json | TEXT | JSON 格式的工具呼叫記錄 |
| status | VARCHAR(20) | 狀態（見下表） |
| actual_response | TEXT | 實際發送的內容（如果被修改） |
| response_message_id | BIGINT | 關聯的回覆訊息 ID (FK: chat_history) |
| reviewed_by_admin_id | BIGINT | 處理的管理員 ID |
| created_at | TIMESTAMP | 建立時間 |
| reviewed_at | TIMESTAMP | 審核時間 |
| feedback_type | VARCHAR(20) | 評價類型（POSITIVE / NEGATIVE，可為 NULL） |
| feedback_reason | TEXT | 評價原因（可為 NULL） |
| feedback_by_admin_id | BIGINT | 評價的管理員 ID（可為 NULL） |
| feedback_at | TIMESTAMP | 評價時間（可為 NULL） |

**status 欄位值**：

| 值 | 說明 |
|---|------|
| AUTO_SENT | 自動發送（信心度 ≥80%） |
| PENDING | 等待管理員決定（信心度 40-80%） |
| APPROVED | 管理員採用原建議 |
| MODIFIED | 管理員修改後發送 |
| REJECTED | 管理員拒絕，自己寫 |
| IGNORED | 管理員沒處理（超時或忽略） |

**索引**：
- `idx_session_id` on session_id
- `idx_status` on status
- `idx_confidence` on confidence_score
- `idx_created_at` on created_at
- `idx_feedback_type` on feedback_type

**評價欄位說明**：
- `feedback_type` 為 NULL：表示尚未評價
- `feedback_type` 為 'POSITIVE'：管理員認為這是好的回覆
- `feedback_type` 為 'NEGATIVE'：管理員認為這是需要改進的回覆
- 管理員可以修改評價（UPDATE 操作）

**設計考量**：
- 評價欄位為 NULLABLE，因為大部分 AI 回應不會被評價（約 < 10%）
- NULL 語義清晰：「還沒評價」
- 合併設計降低認知負荷，避免複雜的 JOIN 查詢
- 儲存成本可忽略，但開發/維護成本大幅降低

---

### tool_calls_json 欄位結構

```json
[
  {
    "toolName": "getOrderById",
    "arguments": "{\"orderId\": 123}",
    "result": "{\"status\": \"SHIPPED\", \"trackingNumber\": \"ABC123\"}",
    "executionTime": 150
  },
  {
    "toolName": "searchProduct",
    "arguments": "{\"keyword\": \"iPhone\"}",
    "result": "[{\"id\": 1, \"name\": \"iPhone 15 Pro\"}]",
    "executionTime": 230
  }
]
```

---

## 流程設計

### 流程 1: 客戶發送訊息

```
用戶在前端輸入訊息
         ↓
HTTP POST /api/chat
         ↓
1. 儲存用戶訊息到 chat_history
         ↓
2. WebSocket 推送給所有管理員
   「用戶 #123 發送了新訊息」
         ↓
3. AI 生成回覆 + 計算信心度
         ↓
    ┌────┴────┐
信心度 ≥80%  信心度 40-80%
    ↓           ↓
 自動回覆模式   建議模式
```

---

### 流程 2: 自動回覆模式（信心度 ≥80%）

```
AI 生成回覆（信心度 ≥80%）
         ↓
1. 儲存 AI 回覆到 chat_history
   (role=ASSISTANT, content=AI回覆)
         ↓
2. 記錄到 chat_ai_response
   (status=AUTO_SENT, confidence=0.85)
         ↓
3. WebSocket 推送給客戶
   「收到新訊息：[AI 回覆內容]」
         ↓
4. WebSocket 推送給管理員
   「AI 已自動回覆用戶 #123」
```

**特點**：
- 用戶立即收到回覆
- 管理員看到對話更新（包含 AI 回覆）
- 對話記錄中正常顯示

---

### 流程 3: 建議模式（信心度 40-80%）

```
AI 生成建議（信心度 65%）
         ↓
1. 記錄到 chat_ai_response
   (status=PENDING, confidence=0.65)
   ⚠️ 不存入 chat_history（還未實際發送）
         ↓
2. WebSocket 推送給管理員
   「收到 AI 建議」
   {
     sessionId, userId,
     suggestedText, confidence,
     toolCalls
   }
         ↓
管理員看到 Quick Reply 區顯示建議
         ↓
    管理員選擇：
    ┌────┼────┐
直接發送 修改發送 自己寫
    ↓       ↓       ↓
 APPROVED MODIFIED REJECTED
```

#### 3a. 管理員點擊「直接發送」

```
1. 儲存到 chat_history
   (role=ASSISTANT, content=AI建議內容)
         ↓
2. 更新 chat_ai_response
   (status=APPROVED, response_message_id=xxx)
         ↓
3. WebSocket 推送給客戶
   「收到新訊息：[AI 建議內容]」
```

#### 3b. 管理員「修改後發送」

```
1. 儲存到 chat_history
   (role=ASSISTANT, content=修改後內容)
         ↓
2. 更新 chat_ai_response
   (status=MODIFIED, actual_response=修改後內容)
         ↓
3. WebSocket 推送給客戶
   「收到新訊息：[修改後內容]」
```

#### 3c. 管理員「完全自己寫」

```
1. 儲存到 chat_history
   (role=ASSISTANT, content=管理員輸入內容)
         ↓
2. 更新 chat_ai_response
   (status=REJECTED, actual_response=管理員內容)
         ↓
3. WebSocket 推送給客戶
   「收到新訊息：[管理員內容]」
```

---

### 流程 4: 低信心度模式（< 40%）

```
AI 生成回覆（信心度 < 40%）
         ↓
1. 記錄到 chat_ai_response
   (status=PENDING, confidence=0.25)
   ⚠️ 但不顯示建議（信心度太低）
         ↓
2. WebSocket 推送給管理員
   「用戶 #123 的訊息需要人工處理」
         ↓
管理員完全自己撰寫回覆
```

---

### 流程 5: 管理員快速評價 AI 回覆

```
管理員在對話視窗看到 AI 回覆
         ↓
訊息下方顯示 [👍 讚] [👎 爛] 按鈕
         ↓
管理員點擊其中一個按鈕
         ↓
    ┌────┴────┐
 點擊 👍     點擊 👎
    ↓           ↓
插入 feedback  插入 feedback
(POSITIVE)    (NEGATIVE)
    ↓           ↓
可選填原因     可選填原因
    ↓           ↓
存入 chat_ai_feedback 表
    ↓
按鈕狀態更新（顯示已評價）
```

**互動細節**：
- 點擊後彈出簡單的 modal 或 tooltip：「評價原因（可選）」
- 可以不填原因直接提交
- 已評價的訊息按鈕變成「已評 👍」或「已評 👎」
- 可以修改評價（再次點擊另一個按鈕）

**資料記錄**：
```
chat_ai_response (UPDATE):
  - id: 123
  - feedback_type: 'POSITIVE' or 'NEGATIVE'
  - feedback_reason: '回覆很準確' (optional)
  - feedback_by_admin_id: 456
  - feedback_at: timestamp
```

---

## 介面設計

### 管理員後台佈局

```
┌──────────────────────────────────────────────────────────────────────────┐
│  客服中心後台                                              [登出]          │
├────────────┬────────────────────────────────┬──────────────────────────┤
│            │  對話視窗 - 用戶 #12345          │  🔧 工具呼叫記錄（可摺疊）│
│  Session   │  ┌────────────────────────────┐│  ┌────────────────────┐ │
│  列表      │  │ 👤 用戶: 我要退款           ││  │ ✓ getOrderById(123)│ │
│            │  │    2 分鐘前                 ││  │   → { status:      │ │
│ ┌────────┐ │  └────────────────────────────┘│  │     "SHIPPED" }    │ │
│ │用戶 #123│ │  ┌────────────────────────────┐│  │   ⏱ 150ms          │ │
│ │🔴新訊息│ │  │ 💬 客服: 請提供訂單編號     ││  └────────────────────┘ │
│ │2 分鐘前│ │  │    🤖 AI自動（信心 85%）   ││  ┌────────────────────┐ │
│ └────────┘ │  │    1 分鐘前                 ││  │ ✓ searchProduct()  │ │
│            │  │    [👍 讚] [👎 爛]          ││  │   → [{ id: 1,      │ │
│ ┌────────┐ │  └────────────────────────────┘│  │      name: "..." }]│ │
│ │用戶 #456│ │                                 │  │   ⏱ 230ms          │ │
│ │5 分鐘前│ │  ┌────────────────────────────┐│  └────────────────────┘ │
│ └────────┘ │  │ ✨ AI 建議（信心度: 65%）  ││                          │
│            │  │                             ││                          │
│ ┌────────┐ │  │ 「非常抱歉造成您的困擾。   ││                          │
│ │用戶 #789│ │  │  請提供您的訂單編號，我們  ││                          │
│ │10分鐘前│ │  │  會立即為您處理退款事宜。」││                          │
│ └────────┘ │  │                             ││                          │
│            │  │ [✓直接] [✏️修改] [✗忽略]   ││                          │
│ [查看全部] │  └────────────────────────────┘│                          │
│            │                                 │                          │
│            │  ┌────────────────────────────┐│                          │
│            │  │ [輸入回覆訊息...]           ││                          │
│            │  │                  [發送] ──┐││                          │
│            │  └────────────────────────────┘│                          │
└────────────┴────────────────────────────────┴──────────────────────────┘
```

---

### 介面元素說明

#### 1. Session 列表（左側）
- 顯示所有有對話的用戶
- 紅點標記：有新訊息的 session
- 排序：最新訊息在最上面
- 顯示最後一則訊息預覽和時間

#### 2. 對話視窗（中間）
**顯示歷史記錄**，每則訊息包含：
- 角色標記：👤 用戶 / 💬 客服
- 訊息內容
- 時間戳記
- （僅管理員可見）發送方式標記：
  - `🤖 AI 自動回覆（信心度 85%）`
  - `✓ 已採用 AI 建議`
  - `✏️ 已修改 AI 建議`
  - `👤 真人客服回覆`
- **快速評價控制項**（僅 AI 回覆訊息顯示）：
  - 👍 讚（標記為正面案例）
  - 👎 爛（標記為負面案例）
  - 點擊後可選填評價原因（可選）
  - 已評價的訊息顯示當前評價狀態

#### 3. Tool Calls 側邊欄（右側，可摺疊）
顯示 AI 呼叫的工具：
- 工具名稱
- 輸入參數
- 執行結果
- 執行時間

**用途**：
- 幫助管理員快速了解 AI 做了什麼
- 輔助判斷 AI 建議是否合理
- 不佔據主要對話區域

#### 4. Quick Reply 區（輸入框上方）
**僅在信心度 40-80% 時顯示**

內容：
- AI 建議的回覆文字
- 信心度百分比
- 三個操作按鈕：
  - `✓ 直接發送`：一鍵發送 AI 建議
  - `✏️ 修改後發送`：將建議填入輸入框，可修改
  - `✗ 忽略`：關閉建議，自己寫

#### 5. 訊息輸入框（底部）
- 標準文字輸入框
- 當點擊「修改後發送」時，自動填入 AI 建議
- 發送按鈕

---

### 客戶端介面

標準聊天介面，無特殊設計：
- 訊息列表（用戶和客服）
- 輸入框
- 發送按鈕

**特點**：
- 用戶無法分辨是 AI 還是真人回覆
- 所有回覆都顯示為「客服」

---

## WebSocket 通訊設計

### WebSocket 端點

```
連線端點：/ws/chat
協議：STOMP over SockJS
```

---

### 訂閱頻道設計

#### 客戶端訂閱

| 頻道 | 用途 | 訊息格式 |
|------|------|---------|
| `/user/{userId}/queue/messages` | 接收客服回覆 | `{ content, timestamp }` |

**範例**：
```javascript
stompClient.subscribe('/user/123/queue/messages', (message) => {
  // 用戶 123 收到新訊息
});
```

---

#### 管理員訂閱（全域）

| 頻道 | 用途 | 訊息格式 |
|------|------|---------|
| `/topic/admin/new-messages` | 任何用戶發送新訊息 | `{ userId, sessionId, message, timestamp }` |
| `/topic/admin/suggestions` | AI 生成建議 | `{ sessionId, userId, suggestedText, confidence, toolCalls, timestamp }` |

**範例**：
```javascript
// 監聽所有新訊息
stompClient.subscribe('/topic/admin/new-messages', (message) => {
  // 更新 Session 列表，顯示紅點
});

// 監聽 AI 建議
stompClient.subscribe('/topic/admin/suggestions', (message) => {
  // 顯示 Quick Reply 區
});
```

---

#### 管理員訂閱（特定 session）

| 頻道 | 用途 | 訊息格式 |
|------|------|---------|
| `/topic/session/{sessionId}/updates` | 特定會話的更新 | `{ type, content, timestamp }` |

**範例**：
```javascript
// 當管理員打開用戶 #123 的對話時
stompClient.subscribe('/topic/session/123/updates', (message) => {
  if (message.type === 'auto_reply') {
    // AI 自動回覆了，更新對話視窗
  }
});
```

---

### 推送時機

| 事件 | 推送對象 | 推送頻道 | 內容 |
|------|---------|---------|------|
| 用戶發送訊息 | 所有管理員 | `/topic/admin/new-messages` | 用戶訊息 |
| AI 自動回覆（≥80%） | 客戶 | `/user/{userId}/queue/messages` | AI 回覆 |
| AI 自動回覆（≥80%） | 管理員 | `/topic/session/{sessionId}/updates` | AI 回覆通知 |
| AI 生成建議（40-80%） | 管理員 | `/topic/admin/suggestions` | 建議內容 + 信心度 |
| 管理員發送回覆 | 客戶 | `/user/{userId}/queue/messages` | 管理員回覆 |

---

## 信心度計算策略

### 核心方法：LLM 多維度自我評估

使用 LLM 針對多個獨立問題進行評分，每個問題獨立執行，最後加總得出最終信心度。

---

### 評估問題設計

每個問題返回 0-5 分，最後加總除以總分（25 分）得到信心度（0.0-1.0）。

#### 問題 1: 回答完整性（權重：5 分）
```
提示詞：
「評估以下 AI 回答的完整性（0-5 分）：

用戶問題：{用戶問題}
AI 回答：{AI 回答}

評分標準：
- 5 分：完整回答了所有問題要點
- 4 分：回答了主要問題，但遺漏次要細節
- 3 分：回答了部分問題
- 2 分：僅部分觸及問題
- 1 分：幾乎沒有回答問題
- 0 分：完全沒有回答問題

只回覆數字（0-5）。
」
```

#### 問題 2: 用詞確定性（權重：5 分）
```
提示詞：
「評估以下 AI 回答的用詞確定性（0-5 分）：

AI 回答：{AI 回答}

評分標準：
- 5 分：用詞明確果斷，沒有模糊表達
- 4 分：大部分確定，僅少量不確定詞彙（如「可能」「也許」）
- 3 分：有明顯不確定詞彙，但不影響主要訊息
- 2 分：充滿不確定詞彙（「應該」「大概」「不確定」）
- 1 分：幾乎完全不確定
- 0 分：完全不確定的回答

只回覆數字（0-5）。
」
```

#### 問題 3: 敏感度判斷（權重：5 分）
```
提示詞：
「評估以下對話的敏感程度（0-5 分）：

用戶問題：{用戶問題}
AI 回答：{AI 回答}

評分標準：
- 5 分：一般性問題，AI 可以安全處理
- 4 分：稍微敏感，但 AI 回答適當
- 3 分：涉及敏感議題（退款、投訴），建議人工確認
- 2 分：涉及法律、糾紛等敏感議題
- 1 分：高度敏感
- 0 分：極度敏感，必須人工處理

只回覆數字（0-5）。
」
```

#### 問題 4: 工具呼叫成功率（權重：5 分）
```
提示詞：
「評估以下工具呼叫的成功情況（0-5 分）：

工具呼叫記錄：{tool_calls_json}

評分標準：
- 5 分：沒有工具呼叫，或所有工具呼叫成功
- 4 分：大部分工具呼叫成功
- 3 分：部分工具呼叫失敗，但不影響主要回答
- 2 分：關鍵工具呼叫失敗
- 1 分：多數工具呼叫失敗
- 0 分：所有工具呼叫失敗或有錯誤

只回覆數字（0-5）。
」
```

#### 問題 5: 需要人工確認程度（權重：5 分）
```
提示詞：
「評估以下 AI 回答是否需要人工確認（0-5 分）：

用戶問題：{用戶問題}
AI 回答：{AI 回答}
對話歷史：{conversation_history}

評分標準：
- 5 分：完全可以自動發送，不需人工確認
- 4 分：建議人工快速審核
- 3 分：需要人工仔細審核
- 2 分：建議人工修改後發送
- 1 分：強烈建議人工重寫
- 0 分：不應自動發送，必須人工重寫

只回覆數字（0-5）。
」
```

---

### 計算流程

```
1. 並行執行 5 個評估問題
   ├─ 問題 1: 回答完整性 → 得分 A (0-5)
   ├─ 問題 2: 用詞確定性 → 得分 B (0-5)
   ├─ 問題 3: 敏感度判斷 → 得分 C (0-5)
   ├─ 問題 4: 工具呼叫成功率 → 得分 D (0-5)
   └─ 問題 5: 需要人工確認程度 → 得分 E (0-5)

2. 計算總分
   總分 = A + B + C + D + E (最高 25 分)

3. 計算信心度
   信心度 = 總分 / 25

   範例：
   - 總分 21 → 信心度 0.84 → 自動發送
   - 總分 15 → 信心度 0.60 → 建議模式
   - 總分 8 → 信心度 0.32 → 等待人工
```

---

### 優勢

1. **可解釋性**：知道哪個維度得分低，可以針對性改進
2. **可調整性**：可以調整各問題的權重
3. **一致性**：使用 LLM 評估，與回答的 LLM 一致
4. **擴展性**：可以輕鬆新增或移除評估問題

---

### 信心度閾值

| 信心度範圍 | 處理方式 | 用戶體驗 |
|-----------|---------|---------|
| ≥ 0.80 | 自動發送 | 立即收到回覆 |
| 0.40 - 0.79 | 建議模式 | 稍等片刻後收到回覆 |
| < 0.40 | 等待人工 | 等待客服處理 |

**閾值可配置**，根據實際運營調整。

---

## API 設計

### 客戶端 API

#### ✅ POST /api/chat
發送訊息（**已實現**）

**Request**:
```json
{
  "message": "我要退款",
  "pageContext": {
    "path": "/orders/123",
    "title": "訂單詳情",
    "pageType": "order_detail"
  }
}
```

**Response**:
```json
{
  "response": "請提供您的訂單編號...",
  "sessionId": "123"
}
```

#### ✅ GET /api/chat/history?sessionId={sessionId}
取得對話歷史（**已實現**）

**Response**:
```json
[
  {
    "id": 1,
    "role": "USER",
    "content": "我要退款",
    "createdAt": "2025-01-15T10:00:00"
  },
  {
    "id": 2,
    "role": "ASSISTANT",
    "content": "請提供您的訂單編號...",
    "createdAt": "2025-01-15T10:00:05"
  }
]
```

---

### 管理員 API

#### ❌ GET /api/admin/chat/sessions
取得所有活躍會話（**未實現**）

**Response**:
```json
[
  {
    "sessionId": "123",
    "userId": 123,
    "lastMessage": "我要退款",
    "lastMessageTime": 1705276800,
    "hasUnread": true
  }
]
```

#### ❌ GET /api/admin/chat/sessions/{sessionId}/history
取得特定會話的歷史記錄（**未實現**）

**Response**:（同客戶端 API）

#### ❌ POST /api/admin/chat/sessions/{sessionId}/send-suggestion
發送 AI 建議（未修改）（**未實現**）

**Request**:
```json
{
  "userId": 123,
  "suggestedText": "請提供您的訂單編號..."
}
```

#### ❌ POST /api/admin/chat/sessions/{sessionId}/send-modified
發送修改後的回覆（**未實現**）

**Request**:
```json
{
  "userId": 123,
  "originalSuggestion": "請提供您的訂單編號...",
  "modifiedText": "非常抱歉！請提供您的訂單編號，我們會立即處理。"
}
```

#### ❌ POST /api/admin/chat/sessions/{sessionId}/send-manual
發送完全手動的回覆（**未實現**）

**Request**:
```json
{
  "userId": 123,
  "text": "您好，關於您的問題..."
}
```

#### ❌ POST /api/admin/chat/feedback
管理員對 AI 回覆進行評價（**未實現**）

**Request**:
```json
{
  "aiResponseId": 123,
  "feedbackType": "POSITIVE",
  "reason": "回覆準確且有禮貌"
}
```

**Response**:
```json
{
  "success": true,
  "message": "評價已更新"
}
```

**說明**：
- `feedbackType`: "POSITIVE" 或 "NEGATIVE"
- `reason`: 可選，管理員的評價原因
- 操作會直接 UPDATE chat_ai_response 表的 feedback 欄位
- 同一個 AI 回覆可以被多次評價（覆蓋舊評價）

---

### 📊 實現狀態總結

#### 已實現 (2/8)
- ✅ POST /api/chat - 客戶端發送訊息
- ✅ GET /api/chat/history - 取得對話歷史

#### 未實現 (6/8)
- ❌ GET /api/admin/chat/sessions - 取得所有會話列表
- ❌ GET /api/admin/chat/sessions/{sessionId}/history - 取得特定會話歷史
- ❌ POST /api/admin/chat/sessions/{sessionId}/send-suggestion - 發送 AI 建議
- ❌ POST /api/admin/chat/sessions/{sessionId}/send-modified - 發送修改後的回覆
- ❌ POST /api/admin/chat/sessions/{sessionId}/send-manual - 發送手動回覆
- ❌ POST /api/admin/chat/feedback - AI 回覆評價

#### 核心缺失功能
1. **信心度計算** - 尚未實現 LLM 多維度自我評估
2. **AI 建議模式** - 信心度 40-80% 的建議流程
3. **管理員操作 API** - 所有管理員相關的 API 都未實現
4. **WebSocket 推送** - 即時通訊功能尚未實現
5. **chat_ai_response 表** - AI 回應記錄表（含評價欄位）尚未建立

#### 目前實現程度
約 **25%**（僅基礎聊天功能，核心 AI 輔助功能未實現）

---

## 數據分析

### 可分析的指標

基於 `chat_ai_response` 表，可以分析：

#### 1. AI 自動處理率
```sql
SELECT
  COUNT(CASE WHEN status = 'AUTO_SENT' THEN 1 END) * 100.0 / COUNT(*) as auto_rate
FROM chat_ai_response
```

#### 2. AI 建議採納率
```sql
SELECT
  status,
  COUNT(*) as count
FROM chat_ai_response
WHERE status IN ('APPROVED', 'MODIFIED', 'REJECTED')
GROUP BY status
```

#### 3. 平均信心度
```sql
SELECT
  status,
  AVG(confidence_score) as avg_confidence
FROM chat_ai_response
GROUP BY status
```

#### 4. 管理員工作量
```sql
SELECT
  reviewed_by_admin_id,
  COUNT(*) as handled_count
FROM chat_ai_response
WHERE reviewed_by_admin_id IS NOT NULL
GROUP BY reviewed_by_admin_id
```

#### 5. 需要改進的案例
```sql
SELECT *
FROM chat_ai_response
WHERE feedback_type = 'NEGATIVE'
```

#### 6. 評價覆蓋率
```sql
SELECT
  COUNT(CASE WHEN feedback_type IS NOT NULL THEN 1 END) * 100.0 / COUNT(*) as feedback_rate
FROM chat_ai_response
```

---

## 擴展性考慮

### 多管理員協作

**問題**：多個管理員同時處理不同客戶

**解決方案**：
- Session 列表顯示「誰正在處理」
- 當管理員打開對話時，發送「佔用」訊號
- 其他管理員看到「客服 A 正在處理中」

### 管理員主動發起對話

**場景**：管理員想主動聯繫客戶

**解決方案**：
- 在 Session 列表加入「發起新對話」功能
- 選擇用戶後，建立新的 session
- 第一則訊息由管理員發送

### 多語言支援

**考慮**：
- AI 需要偵測用戶語言
- 信心度計算的關鍵詞需要多語言版本
- 管理員介面多語言

### AI 模型更新

**策略**：
- `chat_ai_response` 加入 `model_version` 欄位
- 可以比較不同版本的 AI 表現
- A/B 測試不同模型

---

## 安全性考慮

### 1. WebSocket 認證
- 連線時需要驗證 JWT token
- 確保用戶只能訂閱自己的頻道

### 2. 資料隔離
- 用戶只能查詢自己的對話記錄
- 管理員需要 ADMIN 權限才能訪問後台 API

### 3. 敏感資訊過濾
- AI 回覆中不應包含敏感資訊（密碼、信用卡等）
- 對話記錄加密存儲（可選）

### 4. 速率限制
- 防止用戶發送過多訊息
- 防止管理員濫用 API

---

## 監控與告警

### 建議監控指標

1. **AI 回應時間**：從用戶發送到 AI 回覆的延遲
2. **信心度分布**：監控信心度是否正常分布
3. **自動回覆失敗率**：AI 生成失敗的比例
4. **管理員回應時間**：建議模式下，管理員處理的平均時間
5. **WebSocket 連線數**：監控即時通訊狀態

### 告警策略

- AI 回應時間 > 5 秒 → 告警
- 自動回覆失敗率 > 10% → 告警
- 待處理建議堆積 > 20 則 → 告警（人手不足）

---

## 未來優化方向

### 1. 智慧分流
根據問題類型自動分配給專業管理員：
- 訂單問題 → 訂單專員
- 退款問題 → 退款專員
- 技術問題 → 技術支援

### 2. 管理員績效評估
- 回覆速度
- 客戶滿意度
- AI 建議採納率

### 3. 客戶滿意度調查
對話結束後，詢問客戶滿意度，用於評估 AI 和管理員表現。

### 4. 知識庫整合
- AI 無法回答的問題自動加入「待補充」清單
- 管理員補充後更新知識庫
- AI 持續學習

---

## 總結

本系統設計的核心優勢：

1. **關注點分離**：對話記錄與 AI 元數據分離，架構清晰
2. **無縫體驗**：用戶無法分辨 AI 還是真人，體驗一致
3. **靈活控制**：管理員可以選擇採用、修改或拒絕 AI 建議
4. **持續改進**：完整記錄 AI 表現，可用於優化
5. **可擴展性**：設計支援未來功能擴展

---

**文件版本**：v1.0
**最後更新**：2025-01-15
**維護者**：開發團隊
