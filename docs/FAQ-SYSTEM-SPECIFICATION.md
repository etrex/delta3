# FAQ 系統規格文件

## 目錄
1. [系統概述](#系統概述)
2. [開發進度](#開發進度)
3. [資料庫設計](#資料庫設計)
4. [RAG 系統整合](#rag-系統整合)
5. [API 設計](#api-設計)
6. [前端設計](#前端設計)
7. [實作細節](#實作細節)

---

## 系統概述

### 設計理念

FAQ（Frequently Asked Questions）系統是智能客服系統的重要組成部分，提供兩種訪問方式：

1. **RAG 驅動的 AI 自動查詢**（已完成）
   - AI 聊天機器人自動搜尋 FAQ 資料庫
   - 使用語義相似度匹配，而非關鍵字搜尋
   - 無縫整合到客服對話流程中

2. **獨立的 FAQ 瀏覽頁面**（待開發）
   - 客戶可直接瀏覽所有 FAQ
   - 支援分類篩選和關鍵字搜尋
   - 提供自助服務入口

### 系統目標

1. **知識共享**：將常見問題的標準答案集中管理
2. **減輕客服負擔**：客戶可自助查找答案
3. **提升 AI 效能**：為 AI 提供高品質的知識庫
4. **持續優化**：根據客戶詢問頻率優化 FAQ 內容

---

## 開發進度

### ✅ 已完成部分 (~70%)

#### 1. 資料庫層 (100%)
- ✅ FAQ 資料表設計與建立
- ✅ 測試資料準備（15 筆涵蓋 6 大分類）
- ✅ Repository 層實作（CRUD + 搜尋）

#### 2. RAG 系統整合 (100%)
- ✅ 向量嵌入服務（Embedding Service）
- ✅ 自動載入 FAQ 到向量資料庫
- ✅ RAG 搜尋工具整合到 AI 助手
- ✅ 語義搜尋功能

#### 3. AI 助手整合 (100%)
- ✅ AI 可自動調用 `searchKnowledgeBase()` 工具
- ✅ 搜尋結果格式化供 LLM 使用
- ✅ 自動判斷何時需要搜尋知識庫

### ❌ 待開發部分 (~30%)

#### 1. REST API 層 (0%)
- ❌ FAQ CRUD API 端點
- ❌ 分類查詢 API
- ❌ 關鍵字搜尋 API
- ❌ 管理員權限控制

#### 2. 前端客戶端頁面 (0%)
- ❌ FAQ 列表頁面
- ❌ 分類篩選功能
- ❌ 搜尋框實作
- ❌ 展開/收合效果
- ❌ 響應式設計

#### 3. 前端管理端頁面 (0%)
- ❌ FAQ 管理介面
- ❌ 新增/編輯/刪除功能
- ❌ 分類管理
- ❌ 批次操作

---

## 資料庫設計

### FAQ 資料表結構

**表名**：`faqs`

| 欄位名 | 類型 | 說明 | 約束 |
|--------|------|------|------|
| id | BIGINT | 主鍵 | PRIMARY KEY, AUTO_INCREMENT |
| question | VARCHAR(500) | 問題內容 | NOT NULL |
| answer | TEXT | 答案內容 | NOT NULL |
| category | VARCHAR(100) | 分類 | NOT NULL, INDEX |
| created_at | TIMESTAMP | 建立時間 | DEFAULT CURRENT_TIMESTAMP |

**索引**：
- PRIMARY KEY: `id`
- INDEX: `idx_faqs_category` on `category`

### 測試資料分類

系統預設包含以下 6 大分類，共 15 筆 FAQ：

1. **商品技術** (4筆)
   - RAM 選擇建議
   - OLED vs LCD 螢幕
   - 主動降噪原理
   - SSD vs HDD 差異

2. **購物流程** (3筆)
   - 訂單修改流程
   - 超商取貨說明
   - 發票開立方式

3. **售後服務** (3筆)
   - 7天鑑賞期規定
   - 保固內故障處理
   - 配件退換貨規則

4. **會員權益** (2筆)
   - 會員註冊說明
   - 紅利點數使用

5. **支付方式** (2筆)
   - 付款方式說明
   - 分期付款手續費

6. **運送相關** (1筆)
   - 運費計算規則

### 資料庫遷移檔案

**位置**：
- `backend/src/main/resources/db/migration/V9__Create_faq_table.sql`
- `backend/src/main/resources/db/migration/V11__Insert_test_faqs.sql`

---

## RAG 系統整合

### 架構說明

FAQ 系統採用 RAG（Retrieval-Augmented Generation）架構，讓 AI 能夠語義化搜尋 FAQ：

```
用戶提問
    ↓
AI 判斷需要搜尋知識庫
    ↓
調用 searchKnowledgeBase(query)
    ↓
EmbeddingService 進行向量搜尋
    ↓
返回前 3 名最相似的 FAQ
    ↓
AI 基於搜尋結果生成回答
    ↓
返回給用戶
```

### 核心組件

#### 1. EmbeddingService
**位置**：`backend/src/main/java/com/etrex/oms/embedding/EmbeddingService.java`

**功能**：
- 將文本轉換為向量（Embedding）
- 向量相似度搜尋
- 批次處理文檔

**主要方法**：
```java
// 添加單一文檔
String addDocument(String text, String metadata)

// 批次添加文檔
List<String> addDocuments(List<String> texts)

// 語義搜尋
List<EmbeddingMatch<TextSegment>> search(String query, int maxResults, double minScore)

// 搜尋並格式化結果
String searchAndFormat(String query, int maxResults)
```

#### 2. EmbeddingInitializer
**位置**：`backend/src/main/java/com/etrex/oms/embedding/EmbeddingInitializer.java`

**功能**：
- 應用啟動時自動載入所有 FAQ
- 將 FAQ 轉換為向量並存入向量資料庫
- 支援產品描述的向量化（未來擴展）

**初始化流程**：
```java
@PostConstruct
public void init() {
    // 1. 從資料庫載入所有 FAQ
    List<Faq> faqs = faqRepository.findAll();

    // 2. 格式化為文本
    List<String> faqTexts = faqs.stream()
        .map(faq -> formatFaqForEmbedding(faq))
        .collect(Collectors.toList());

    // 3. 批次添加到向量資料庫
    embeddingService.addDocuments(faqTexts);
}
```

**FAQ 格式化範例**：
```
問題：筆電的 RAM 要選 16GB 還是 32GB？
答案：如果您是一般文書、上網、看影片使用，16GB RAM 已經非常足夠...
分類：商品技術
```

#### 3. RagSearchTool
**位置**：`backend/src/main/java/com/etrex/oms/ai/RagSearchTool.java`

**功能**：
- 作為 LangChain4j Tool 供 AI 調用
- 封裝 EmbeddingService 的搜尋功能
- 提供友善的錯誤處理

**Tool 定義**：
```java
@Tool("Search knowledge base using semantic similarity. " +
      "Use this when user asks about product features, " +
      "technical specs, FAQs, shopping policies, or any " +
      "question that requires domain knowledge.")
public String searchKnowledgeBase(String query)
```

**使用場景**：
- 用戶問：「DDR4 和 DDR5 有什麼差別？」
- 用戶問：「7天鑑賞期怎麼算？」
- 用戶問：「可以超商取貨嗎？」

### 向量資料庫配置

**使用技術**：LangChain4j In-Memory Embedding Store

**配置位置**：`backend/src/main/java/com/etrex/oms/config/LangChain4jConfig.java`

**特性**：
- 內存存儲（適合開發環境）
- 快速搜尋
- 應用重啟需重新載入

**生產環境建議**：
- 改用 PostgreSQL + pgvector
- 或使用 Pinecone、Weaviate 等專業向量資料庫

### 搜尋參數

**預設配置**：
- **maxResults**: 3（返回前 3 名最相似結果）
- **minScore**: 0.6（最低相似度閾值 60%）

**相似度分數說明**：
- 0.9 - 1.0：幾乎完全匹配
- 0.8 - 0.9：高度相關
- 0.7 - 0.8：相關
- 0.6 - 0.7：部分相關
- < 0.6：不相關（被過濾）

---

## API 設計

### 客戶端 API

#### 1. 取得所有 FAQ
```
GET /api/faqs
```

**查詢參數**：
- `category` (optional): 按分類篩選

**回應範例**：
```json
[
  {
    "id": 1,
    "question": "筆電的 RAM 要選 16GB 還是 32GB？",
    "answer": "如果您是一般文書、上網、看影片使用...",
    "category": "商品技術",
    "createdAt": "2025-10-19T12:00:00"
  }
]
```

#### 2. 搜尋 FAQ
```
GET /api/faqs/search?keyword={keyword}
```

**查詢參數**：
- `keyword` (required): 搜尋關鍵字

**搜尋範圍**：問題 + 答案

**回應**：同上

#### 3. 取得分類列表
```
GET /api/faqs/categories
```

**回應範例**：
```json
[
  "商品技術",
  "購物流程",
  "售後服務",
  "會員權益",
  "支付方式",
  "運送相關"
]
```

### 管理端 API

#### 1. 新增 FAQ
```
POST /api/admin/faqs
Authorization: Bearer {admin_token}
```

**請求 Body**：
```json
{
  "question": "新問題",
  "answer": "新答案",
  "category": "分類名稱"
}
```

#### 2. 更新 FAQ
```
PUT /api/admin/faqs/{id}
Authorization: Bearer {admin_token}
```

#### 3. 刪除 FAQ
```
DELETE /api/admin/faqs/{id}
Authorization: Bearer {admin_token}
```

#### 4. 重新載入向量資料庫
```
POST /api/admin/faqs/reload-embeddings
Authorization: Bearer {admin_token}
```

**說明**：新增/修改 FAQ 後需調用此 API 更新向量資料庫

---

## 前端設計

### 客戶端頁面

#### 路由
```
/faqs
```

#### 頁面佈局

```
┌─────────────────────────────────────────┐
│  常見問題                                │
├─────────────────────────────────────────┤
│  搜尋：[___________________] [搜尋]     │
├─────────────────────────────────────────┤
│  分類篩選：                              │
│  [全部] [商品技術] [購物流程] [售後]... │
├─────────────────────────────────────────┤
│  ▼ 筆電的 RAM 要選 16GB 還是 32GB？    │
│     如果您是一般文書、上網、看影片...    │
│                                          │
│  ▼ 什麼是 OLED 螢幕？跟 LCD 有什麼差別？│
│     OLED（有機發光二極體）螢幕...       │
│                                          │
│  [顯示更多]                              │
└─────────────────────────────────────────┘
```

#### 功能需求

1. **分類篩選**
   - Tab 切換不同分類
   - 預設顯示「全部」
   - 顯示每個分類的 FAQ 數量

2. **搜尋功能**
   - 即時搜尋（輸入時自動過濾）
   - 高亮顯示匹配關鍵字
   - 無結果時顯示提示訊息

3. **展開/收合**
   - 預設收合，只顯示問題
   - 點擊展開顯示完整答案
   - 支援手風琴效果（展開一個自動收合其他）

4. **響應式設計**
   - 桌面：2 欄佈局
   - 平板：1 欄佈局
   - 手機：堆疊佈局

#### 技術實作

**組件結構**：
```
src/views/customer/
  └── Faqs.vue          # FAQ 列表頁面

src/components/
  └── FaqItem.vue       # 單一 FAQ 項目
```

**狀態管理**：
```typescript
// Faqs.vue
const faqs = ref<Faq[]>([])
const categories = ref<string[]>([])
const currentCategory = ref('全部')
const searchKeyword = ref('')

// 計算屬性：過濾後的 FAQ
const filteredFaqs = computed(() => {
  return faqs.value.filter(faq => {
    // 分類篩選
    if (currentCategory.value !== '全部' &&
        faq.category !== currentCategory.value) {
      return false
    }

    // 關鍵字搜尋
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      return faq.question.toLowerCase().includes(keyword) ||
             faq.answer.toLowerCase().includes(keyword)
    }

    return true
  })
})
```

### 管理端頁面

#### 路由
```
/admin/faqs
```

#### 頁面佈局

```
┌─────────────────────────────────────────┐
│  FAQ 管理               [+ 新增 FAQ]    │
├─────────────────────────────────────────┤
│  搜尋：[___________________] [搜尋]     │
│  分類：[全部 ▼]                          │
├──────┬──────────────────────┬───────────┤
│ ID   │ 問題                  │ 操作      │
├──────┼──────────────────────┼───────────┤
│ 1    │ 筆電的 RAM 要選...   │ [編輯][刪除]│
│ 2    │ 什麼是 OLED 螢幕... │ [編輯][刪除]│
└──────┴──────────────────────┴───────────┘
```

#### 功能需求

1. **列表顯示**
   - 表格形式顯示
   - 分頁功能（每頁 20 筆）
   - 排序功能（ID、分類、建立時間）

2. **CRUD 操作**
   - 新增：彈出對話框輸入問題、答案、分類
   - 編輯：彈出對話框修改內容
   - 刪除：確認對話框

3. **批次操作**
   - 多選 FAQ
   - 批次刪除
   - 批次修改分類

4. **向量資料庫管理**
   - 「重新載入向量資料庫」按鈕
   - 顯示最後載入時間
   - 載入進度顯示

---

## 實作細節

### 後端實體類別

**位置**：`backend/src/main/java/com/etrex/oms/entity/Faq.java`

```java
@Entity
@Table(name = "faqs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Faq {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

### 後端 Repository

**位置**：`backend/src/main/java/com/etrex/oms/repository/FaqRepository.java`

```java
@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
    // 按分類查詢
    List<Faq> findByCategory(String category);

    // 關鍵字搜尋（問題 + 答案）
    @Query("SELECT f FROM Faq f WHERE " +
           "LOWER(f.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.answer) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Faq> searchByKeyword(String keyword);

    // 按建立時間排序
    List<Faq> findAllByOrderByCreatedAtDesc();
}
```

### AI 工具調用範例

**用戶問題**：「7天鑑賞期怎麼算？」

**AI 判斷邏輯**：
1. 分析問題：這是關於購物政策的問題
2. 決定調用 `searchKnowledgeBase("7天鑑賞期")`

**RAG 搜尋結果**：
```
搜尋結果：

問題：7 天鑑賞期怎麼算？
答案：依消費者保護法規定，商品到貨日起算 7 天內（含例假日）...
分類：售後服務

（其他相關結果...）
```

**AI 最終回覆**：
```
根據消費者保護法規定，7天鑑賞期是從商品到貨日開始計算，
包含例假日。在這期間如果商品有問題或不滿意，您可以申請退貨。

需要特別注意的是：
1. 商品需保持全新狀態
2. 配件要完整
3. 包裝要完好

鑑賞期不等於試用期，如果商品有使用痕跡，可能無法退貨。

您還有其他問題嗎？
```

---

## 測試計劃

### RAG 系統測試

**測試檔案**：`backend/src/test/java/com/etrex/oms/embedding/EmbeddingServiceTest.java`

**測試案例**：
1. 測試 FAQ 載入
2. 測試語義搜尋
3. 測試相似度閾值
4. 測試搜尋結果格式化

### API 測試

**測試工具**：Postman / REST Client

**測試案例**：
1. 取得所有 FAQ
2. 分類篩選
3. 關鍵字搜尋
4. 新增 FAQ（管理員）
5. 更新 FAQ（管理員）
6. 刪除 FAQ（管理員）
7. 權限驗證

### E2E 測試

**測試工具**：Cypress

**測試案例**：
1. 客戶瀏覽 FAQ
2. 分類切換
3. 搜尋功能
4. 展開/收合
5. 管理員 CRUD 操作

---

## 未來擴展

### 1. 多語言支援
- 為每個 FAQ 增加語言欄位
- 支援中文、英文、日文

### 2. FAQ 使用統計
- 記錄每個 FAQ 的瀏覽次數
- 記錄 AI 引用次數
- 產生熱門 FAQ 排行

### 3. FAQ 推薦
- 根據用戶瀏覽記錄推薦相關 FAQ
- 在商品頁面顯示相關 FAQ

### 4. FAQ 評分
- 讓用戶對 FAQ 評分（有幫助/無幫助）
- 收集改進建議

### 5. 富文本編輯
- 支援圖片、影片
- 支援代碼區塊
- 支援連結

### 6. FAQ 版本控制
- 記錄修改歷史
- 支援回滾
- 比對差異

---

## 參考資料

### 相關文件
- [AI 輔助客服系統設計文件](./AI-ASSISTED-CUSTOMER-SERVICE-SYSTEM.md)
- [AI 輔助客服系統實作計劃](./AI-ASSISTED-CUSTOMER-SERVICE-IMPLEMENTATION-PLAN.md)

### 相關程式碼
- 資料庫遷移：`backend/src/main/resources/db/migration/V9__Create_faq_table.sql`
- 測試資料：`backend/src/main/resources/db/migration/V11__Insert_test_faqs.sql`
- FAQ 實體：`backend/src/main/java/com/etrex/oms/entity/Faq.java`
- FAQ Repository：`backend/src/main/java/com/etrex/oms/repository/FaqRepository.java`
- Embedding 服務：`backend/src/main/java/com/etrex/oms/embedding/EmbeddingService.java`
- RAG 工具：`backend/src/main/java/com/etrex/oms/ai/RagSearchTool.java`

### 技術文件
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [RAG 架構最佳實踐](https://www.pinecone.io/learn/retrieval-augmented-generation/)
- [向量資料庫選擇指南](https://www.pinecone.io/learn/vector-database/)

---

*最後更新：2025-10-19*
*文件版本：1.0*
