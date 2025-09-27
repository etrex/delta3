# 智能訂單管理系統 (Intelligent Order Management System)

**Copyright (c) 2025 Etrex Kuo. All rights reserved.**

## 專案概述

這是一個完整的智能訂單管理系統，採用地端方案實作，包含前端、後端和 AI 整合三個主要部分。

## 技術棧

### 後端 (Spring Boot)
- **Java 17** + **Maven** + **Spring Boot 3**
- **H2 Database** 內嵌資料庫
- **Flyway** 資料庫版控
- **Spring Security** + **JWT** 身份驗證
- **Spring JPA** 資料存取層
- **Swagger/OpenAPI 3.0** API 文件
- **LangChain4j** AI 整合框架

### 前端 (Vue 3)
- **Vue 3** + **Vite** + **TypeScript**
- **Element Plus** UI 組件庫
- **Pinia** 狀態管理
- **Vue Router** 路由管理
- **Axios** HTTP 客戶端

### AI 部分
- **Ollama** 本地 LLM 運行環境
- **Qwen 2.5 7B** 大型語言模型
- **LangChain4j** 與 **MCP** 工具調用
- **AgenticRAG** 智能檢索增強

## 快速開始

### 1. 安裝必要軟體
```bash
# 安裝 Ollama 和 AI 模型
./setup-ollama.sh
```

### 2. 安裝依賴
```bash
# 後端依賴 (自動下載)
cd backend
mvn clean install

# 前端依賴
cd ../frontend
npm install
```

### 3. 啟動系統
```bash
# 使用一鍵啟動腳本
./startup.sh

# 或手動啟動
# 後端
cd backend && mvn spring-boot:run

# 前端 (新開終端)
cd frontend && npm run dev
```

### 4. 訪問系統
- **前端頁面**: http://localhost:5173
- **後端 API**: http://localhost:8080/api
- **Swagger 文件**: http://localhost:8080/api/swagger-ui.html
- **H2 資料庫控台**: http://localhost:8080/api/h2-console
- **Ollama API**: http://localhost:11434

## 測試帳號

| 角色 | 帳號 | 密碼 | 權限 |
|------|------|------|------|
| 管理員 | admin | password | 所有功能 |
| 顧客 | customer1 | password | 查看商品、建立訂單 |
| 顧客 | customer2 | password | 查看商品、建立訂單 |

## 主要功能

### 1. 使用者身份管理
- ✅ Customer/Admin 角色切換登入
- ✅ JWT Token 身份驗證
- ✅ 角色權限控制

### 2. 商品管理
- ✅ 商品列表展示
- ✅ 關鍵字搜尋
- ✅ 分頁與排序
- ✅ 商品新增/修改/下架 (Admin)
- ✅ 庫存檢查

### 3. 訂單管理
- ✅ 訂單建立
- ✅ 訂單查詢
- ✅ 付款處理
- ✅ 出貨狀態管理 (Admin)
- ✅ 訂單取消與退款

### 4. 智能客服
- ✅ 右下角懸浮聊天窗
- ✅ 自然語言對話
- ✅ 商品查詢與推薦
- ✅ 訂單狀態查詢
- ✅ Tool Calling 功能
- ✅ 多語言支援
- ✅ 角色適應性回應

## API 規格

### 認證 API
- `POST /auth/login` - 用戶登入

### 商品 API
- `GET /products` - 取得商品列表
- `GET /products/{id}` - 取得商品詳情
- `POST /products` - 新增商品 (Admin)
- `PUT /products/{id}` - 更新商品 (Admin)
- `DELETE /products/{id}` - 下架商品 (Admin)

### 訂單 API
- `GET /orders` - 取得訂單列表
- `GET /orders/{id}` - 取得訂單詳情
- `POST /orders` - 建立訂單
- `POST /orders/{id}/pay` - 訂單付款
- `POST /orders/{id}/cancel` - 取消訂單
- `POST /orders/{id}/ship` - 標記出貨 (Admin)

### AI 聊天 API
- `POST /chat/message` - 發送訊息給 AI
- `POST /chat/assistant` - 與智能助手對話 (Tool Calling)

## 資料庫結構

### 主要資料表
1. **users** - 使用者資料
2. **products** - 商品資料
3. **orders** - 訂單主表
4. **order_items** - 訂單明細
5. **order_events** - 訂單事件記錄
6. **payments** - 付款記錄

## AI 功能特色

### 1. 地端 LLM
- 使用 Ollama + Qwen 2.5 7B 模型
- 完全離線運行，保護隱私
- 針對 MacBook Pro M4 優化

### 2. Tool Calling
- 商品搜尋工具
- 訂單查詢工具
- 庫存檢查工具
- 自動函數調用

### 3. 智能對話
- 繁體中文對話
- 上下文理解
- 角色適應 (Admin/Customer)
- 即時回應

## 開發注意事項

### 後端
- 所有 API 都有 Swagger 文件
- 使用 DTO 模式，避免直接暴露 Entity
- 實作 Repository-Service-Controller 分層
- 支援分頁、排序、搜尋

### 前端
- 響應式設計
- TypeScript 類型安全
- Element Plus 組件
- 統一錯誤處理

### AI 整合
- LangChain4j 工具鏈
- MCP 協議實作
- Tool 函數自動註冊
- 錯誤恢復機制

## 測試

```bash
# 後端測試
cd backend
mvn test

# 前端測試
cd frontend
npm run test
```

## 部署

系統設計為地端運行，所有服務都在本機執行：
1. H2 資料庫內嵌
2. Ollama 本地 LLM
3. Spring Boot 應用
4. Vue 前端應用

## 授權

本專案所有程式碼均屬 Etrex Kuo 個人版權所有。

---

**開發完成日期**: 2025年9月20日
**技術支援**: 如有問題請查看 Swagger 文件或原始碼註解