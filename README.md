# 智能訂單管理系統 (Intelligent Order Management System)

完全地端部署的智能訂單管理系統，整合 AI 客服功能。

## 功能簡介

- **商品管理**: 商品 CRUD、庫存管理、搜尋與分頁
- **訂單系統**: 訂單建立、付款、出貨狀態追蹤
- **AI 智能客服**: 自然語言對話、商品查詢推薦、訂單狀態追蹤
- **用戶管理**: JWT 認證、Admin/Customer 角色分離

## 技術棧

- **前端**: Vue 3 + TypeScript + Element Plus
- **後端**: Spring Boot 3 + Java 17 + H2 Database
- **AI**: Ollama + Qwen 2.5 7B + LangChain4j

## 本地運行

### 1. 安裝 AI 模型
```bash
./setup-ollama.sh
```

### 2. 啟動系統
```bash
./dev.sh
```

### 3. 訪問應用
- 🌐 前端: http://localhost:5173
- 📚 API 文件: http://localhost:8080/swagger-ui.html
- 🗄️ 資料庫: http://localhost:8080/h2-console

## 測試帳號

| 身份 | 帳號 | 密碼 |
|------|------|------|
| 管理員 | `admin` | `password123` |
| 顧客 | `customer1` | `password123` |

## H2 資料庫連線

訪問 http://localhost:8080/h2-console 時，請填入以下資訊：

- **JDBC URL**: `jdbc:h2:mem:omsdb`
- **使用者名稱**: `sa`
- **密碼**: `password`

## 測試

在 `backend` 目錄下執行：

```bash
mvn test
```
