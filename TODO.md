# 智能訂單管理系統 - 工作清單

> 最後更新：2025-10-07

## 📊 總體進度

- **前端 - Customer 端：** 9/9 完成 (100%) ✅
- **前端 - Admin 端：** 4/7 完成 (57%)
- **後端：** 22/25 完成 (88%)
- **AI：** 0/17 完成 (0%)
- **總計：** 35/58 完成 (60%)

---

## 📋 前端任務 (13/16 完成)

### 👤 Customer 端功能 (9/9 完成)

#### 1. 登入功能
- [x] 可切換身分(Customer/Admin)

#### 2. 商品瀏覽與購物
- [x] 查看商品列表
- [x] 查看商品詳情
- [x] 加入購物車（含庫存檢查）
- [x] 購物車管理（增減數量、移除商品）

#### 3. 訂單管理
- [x] 查看我的訂單列表
- [x] 訂單關鍵字搜尋
- [x] 訂單分頁與排序
- [x] 查看訂單詳情（出貨、付款狀態）

#### 4. 結帳與付款
- [x] 結帳頁面（含庫存驗證）
- [x] 付款功能

---

### 🔧 Admin 端功能 (4/7 完成)

#### 1. 商品管理
- [x] 新增商品
- [x] 編輯商品
- [x] 查看商品詳細資訊
- [x] 商品列表顯示（編號、建立時間、狀態、庫存）
- [x] 商品下架/重新上架

#### 2. 訂單管理
- [x] 查看所有訂單列表
- [x] 訂單搜尋、分頁、排序

#### 3. 出貨管理
- [x] 顯示訂單清單（出貨、付款狀態）
- [x] 標記訂單出貨狀態（待出貨→已出貨→已送達）

---

## 🔧 後端任務 (22/25 完成)

### 資料表設計 (6/6)
- [x] Users (Customer/Admin)
- [x] Orders
- [x] OrderItems
- [x] Products
- [x] OrderEvents (事件溯源)
- [x] Payments

### 基礎設施 (3/3)
- [x] Flyway 資料庫版控機制
- [x] Spring JPA + DTO/Service/Repository/Controller 分層架構
- [x] Swagger API (OpenAPI 3.0)

### API 實作 (11/12)
- [x] `GET /api/product` - 商品列表
- [x] `GET /api/orders` - 訂單列表(分頁、篩選、排序)
- [x] `POST /api/orders` - 建立訂單(驗證庫存、計算金額)
- [x] `GET /api/orders/{orderNo}` - 訂單詳情
- [x] `POST /api/orders/{orderNo}/pay` - 發起付款
- [ ] `POST /api/payments/webhook` - 付款回調 *(Optional)*
- [x] `POST /api/orders/{orderNo}/cancel` - 取消訂單(釋放庫存、退款)
- [x] `POST /api/orders/{orderNo}/ship` - 標記出貨(ADMIN)
- [x] `POST /api/product` - 新增商品(ADMIN)
- [x] `PUT /api/product/{id}` - 更新/重新上架商品(ADMIN)
- [x] `DELETE /api/product/{id}` - 下架商品(ADMIN)

### 安全性與效能 (2/3)
- [x] Spring Security + JWT + 角色權限(ADMIN/Customer)
- [x] `@Valid` + Bean Validation
- [ ] `@Cacheable` 商品查詢快取

### 測試 (2/2)
- [x] 整合測試(SpringBootTest + MockMvc) - Acceptance Tests
- [x] 單元測試(Service層 + Mockito) - Service & Controller Tests

---

## 🤖 AI Chatbot 任務 (0/17 完成)

### 基礎整合 (0/4)
- [ ] 整合 LangChain4j
- [ ] 整合 AgenticRAG
- [ ] 整合 MCP (Model Context Protocol)
- [ ] 前端 Chatbot 懸浮圖示與對話視窗

### 基礎客服功能 (0/3)
- [ ] 自動問答 FAQ 機器人(知識庫)
- [ ] 多語言支援(中、英、日)
- [ ] 智能歡迎語與主動提示

### 購物輔助功能 (0/3)
- [ ] 商品搜索與推薦
- [ ] 下單與支付引導
- [ ] 報價通知與庫存提醒

### 售後服務功能 (0/3)
- [ ] 訂單狀態查詢
- [ ] 退換貨/退款流程指引
- [ ] 售後問題處理與工單追蹤

### MCP 工具調用 (0/2)
- [ ] Tool Calling：查詢訂單、商品庫存、建立訂單等
- [ ] 根據使用者身分限制可用工具範圍

### 創新功能 - 選做 (0/2)
- [ ] 情緒感知與人性化回應
- [ ] 智能知識擴充與學習
- [ ] 數據分析與回饋機制

---

## 🐛 已知問題

### 已解決
- [x] ~~購物車數量驗證~~ - 已實作前端驗證 (Products.vue) ✅
- [x] ~~結帳頁面庫存驗證~~ - 已實作前端驗證 (Checkout.vue) + 後端最終驗證 (OrderService) ✅

### 當前問題
無

---

## 📝 實作備註

### 已完成的核心功能
1. **購物車系統** - 使用 Pinia store + 後端 API，支援即時更新
2. **訂單流程** - CART → CREATED → PAID → APPROVED → SHIPPED
3. **付款系統** - 支援模擬付款，記錄付款狀態
4. **出貨管理** - Admin 可標記訂單出貨狀態
5. **庫存驗證** - 結帳時前端驗證 + 後端最終驗證
6. **錯誤處理** - 使用 BusinessException 提供友善錯誤訊息

### 技術棧
- **前端：** Vue 3 + Vite + TypeScript + Element Plus + Pinia
- **後端：** Java 17 + Spring Boot 3 + Spring JPA + H2 Database
- **安全：** Spring Security + JWT
- **資料庫版控：** Flyway
- **API 文件：** Swagger (OpenAPI 3.0)

### 專案結構
```
delta3/
├── backend/          # Spring Boot 後端
├── frontend/         # Vue 3 前端
└── INPUT_PROMPT.md   # 需求規格書
```

---

## 🎯 下一步行動

### 立即處理
1. ~~實作商品管理 API (POST/PUT/DELETE)~~ ✅
2. ~~完成商品上下架功能~~ ✅
3. ~~實作取消訂單功能~~ ✅
4. ~~撰寫整合測試與單元測試~~ ✅

### 短期目標
5. ~~加入 OrderEvents 事件溯源~~ ✅
6. 實作商品查詢快取
7. 實作付款回調 Webhook (Optional)

### 中期目標
8. 開始 AI Chatbot 整合
9. 完善前端 Admin 端功能

---

*本文件由系統自動生成，請勿手動編輯進度百分比*
