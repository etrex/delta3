# 🚀 智能訂單管理系統 - 設置說明

**Copyright (c) 2025 Etrex Kuo. All rights reserved.**

## 🎯 快速啟動指南

### 1. 安裝 AI 模型（首次執行）

```bash
# 安裝 Ollama 和 Qwen 2.5 7B 模型
./setup-ollama.sh
```

**注意**：模型下載約需 5-10 分鐘，下載完成後會自動測試。

### 2. 啟動完整系統

```bash
# 一鍵啟動後端和前端
./startup.sh
```

### 3. 訪問系統

| 服務 | URL | 說明 |
|------|-----|------|
| **前端應用** | http://localhost:5173 | 主要使用者介面 |
| **API 文件** | http://localhost:8080/api/swagger-ui.html | 完整 REST API 文件 |
| **H2 資料庫** | http://localhost:8080/api/h2-console | 資料庫管理介面 |
| **AI 模型** | http://localhost:11434 | Ollama LLM 服務 |

## 🔐 測試帳號

| 角色 | 帳號 | 密碼 | 功能權限 |
|------|------|------|----------|
| **管理員** | `admin` | `password` | 全部功能 + 商品管理 |
| **顧客** | `customer1` | `password` | 查看商品、建立訂單 |
| **顧客** | `customer2` | `password` | 查看商品、建立訂單 |

## 🤖 AI 功能測試

1. **登入系統後**，右下角會出現藍色聊天圖示
2. **點擊開啟聊天窗**，可測試以下功能：
   - "幫我查詢所有商品"
   - "MacBook 的價格是多少？"
   - "查詢訂單編號 1 的狀態"
   - "iPhone 還有庫存嗎？"

## 📊 資料庫連接

**H2 Console 設定**：
- JDBC URL: `jdbc:h2:mem:omsdb`
- User Name: `sa`
- Password: `password`

## 🛠️ 手動操作指南

### 啟動後端
```bash
cd backend
mvn spring-boot:run
```

### 啟動前端
```bash
cd frontend
npm run dev
```

### 啟動 AI 服務
```bash
ollama serve
```

## 🔧 故障排除

### 問題 1：Ollama 無法啟動
```bash
# 檢查是否安裝
ollama --version

# 重新安裝
brew reinstall ollama
```

### 問題 2：模型下載失敗
```bash
# 手動下載模型
ollama pull qwen2.5:7b

# 測試模型
ollama run qwen2.5:7b "你好"
```

### 問題 3：前端無法連接後端
```bash
# 檢查後端是否運行
curl http://localhost:8080/api/products

# 檢查 CORS 設定
curl -H "Origin: http://localhost:5173" http://localhost:8080/api/auth/login
```

### 問題 4：JWT Token 錯誤
- 重新登入獲取新 Token
- 檢查瀏覽器本地儲存
- 確認 application.yml 中的 JWT 設定

## 📁 專案結構

```
delta3/
├── backend/                 # Spring Boot 後端
│   ├── src/main/java/      # Java 源碼
│   ├── src/main/resources/ # 配置和遷移文件
│   └── pom.xml             # Maven 依賴
├── frontend/               # Vue 3 前端
│   ├── src/                # Vue 源碼
│   ├── package.json        # NPM 依賴
│   └── vite.config.ts      # Vite 配置
├── setup-ollama.sh         # AI 模型安裝腳本
├── startup.sh              # 系統啟動腳本
└── PROJECT_GUIDE.md        # 詳細專案說明
```

## 🎯 核心功能驗證

### 1. 身份管理
- ✅ 登入/登出
- ✅ JWT Token 驗證
- ✅ 角色權限控制

### 2. 商品管理
- ✅ 商品列表（分頁/搜尋）
- ✅ 商品詳情查看
- ✅ 商品 CRUD（管理員）

### 3. 訂單系統
- ✅ 建立訂單
- ✅ 訂單查詢
- ✅ 付款流程
- ✅ 出貨管理

### 4. 智能客服
- ✅ 自然語言對話
- ✅ 商品查詢工具
- ✅ 訂單狀態查詢
- ✅ 庫存檢查功能

## 📞 技術支援

系統完全地端運行，如有問題請參考：
1. **日誌檢查**：Spring Boot 控制台輸出
2. **API 測試**：使用 Swagger UI
3. **資料庫檢查**：H2 Console
4. **AI 測試**：直接與 Ollama 對話

---

🎉 **恭喜！您的智能訂單管理系統已準備就緒！**