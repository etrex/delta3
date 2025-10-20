# Cypress E2E 測試修復計畫

## 📊 測試執行總結

**執行時間**: 2025-10-21
**測試檔案**: 13 個測試檔案
**總測試數**: 241 個測試
**結果**: ❌ **13 of 13 failed (100%)**

| 狀態 | 數量 | 百分比 |
|------|------|--------|
| ✅ Passing | 1 | 0.4% |
| ❌ Failing | 59 | 24.5% |
| ⏭️ Skipped | 181 | 75.1% |

---

## 🔴 核心問題分析

### 問題 1: 登入頁面缺少 `data-cy` 屬性（影響 100% 測試）

**錯誤訊息**:
```
AssertionError: Timed out retrying after 4000ms:
Expected to find element: `[data-cy=role-selector]`, but never found it.
```

**影響範圍**: 所有 241 個測試
**失敗位置**: `support/commands.ts` 的登入 helper functions

**根本原因**:
1. 測試檔案期望登入頁面有 `data-cy` 測試選擇器
2. 前端登入頁面實作沒有加上這些選擇器
3. 導致所有測試在登入步驟就失敗

**需要修正的元素** (位於 `frontend/src/views/Login.vue`):
- `[data-cy=role-selector]` - 角色選擇器 (Customer/Admin)
- `[data-cy=username]` - 用戶名輸入框
- `[data-cy=password]` - 密碼輸入框
- `[data-cy=login-btn]` - 登入按鈕

---

## 📋 失敗測試詳細列表

### 1. auth/login.cy.ts
**狀態**: 12 失敗 / 1 通過
**通過率**: 7.7%

| 測試項目 | 狀態 | 錯誤原因 |
|----------|------|----------|
| 應顯示身份選擇器（Customer/Admin） | ❌ | 找不到 `[data-cy=role-selector]` |
| 應預設選擇 Customer 身份 | ❌ | 找不到 `[data-cy=role-selector]` |
| 應可以切換到 Admin 身份 | ❌ | 找不到 `[data-cy=role-selector]` |
| 應可以使用 Customer 帳號成功登入 | ❌ | 找不到 `[data-cy=username]` |
| Customer 登入後應看到適當的功能選單 | ❌ | 找不到 `[data-cy=role-selector]` |
| 應處理 Customer 登入失敗的情況 | ❌ | 找不到 `[data-cy=username]` |
| 應可以使用 Admin 帳號成功登入 | ❌ | 找不到 `[data-cy=role-selector]` |
| Admin 登入後應看到管理功能選單 | ❌ | 找不到 `[data-cy=role-selector]` |
| 應在重新整理後保持登入狀態 | ❌ | 找不到 `[data-cy=role-selector]` |
| 應可以成功登出 | ❌ | 找不到 `[data-cy=role-selector]` |
| **未登入時訪問受保護頁面應重導向至登入頁** | ✅ | **唯一通過的測試** |
| 應驗證必填欄位 | ❌ | 找不到 `[data-cy=login-btn]` |
| 應顯示載入狀態 | ❌ | 找不到 `[data-cy=username]` |

---

### 2. chatbot/admin-chat-realtime.cy.ts
**狀態**: 15 失敗 / 6 跳過
**測試組**: 管理員客服即時訊息

**失敗原因**: 所有測試都在 `beforeEach` 登入步驟失敗

**測試類別**:
- WebSocket 連線與即時訊息 (3 個測試) - 全部失敗
- 管理員客服管理介面 (6 個測試) - 全部跳過
- AI 建議審核功能 (4 個測試) - 全部失敗
- 錯誤處理與邊界情況 (4 個測試) - 全部失敗
- 效能與響應 (3 個測試) - 全部失敗

---

### 3. chatbot/chatbot-advanced.cy.ts
**狀態**: 1 失敗 / 23 跳過
**測試組**: 智能客服進階功能

**失敗測試**:
- 情緒感知與人性化回應 - `beforeEach` 登入失敗，導致整個 suite 跳過

---

### 4. chatbot/chatbot-tools.cy.ts
**狀態**: 1 失敗 / 24 跳過
**測試組**: 智能客服工具調用功能

**失敗測試**:
- 工具調用權限控制 - `beforeEach` 登入失敗，導致整個 suite 跳過

---

### 5. chatbot/chatbot-ui.cy.ts
**狀態**: 13 失敗 / 16 跳過
**測試組**: 智能客服對話視窗

**失敗測試分類**:
- Chatbot 基本介面 (1 失敗) - beforeEach 失敗
- 智能歡迎語與主動提示 (4 失敗) - 登入失敗
- 基本對話功能 - 整個 suite 跳過
- 多語言支援 - 整個 suite 跳過
- 用戶身分自動調整功能 (3 失敗)
- 聊天記錄管理 - 整個 suite 跳過
- 聊天視窗狀態管理 - 整個 suite 跳過
- 錯誤處理 - 整個 suite 跳過

---

### 6. products/product-list.cy.ts
**狀態**: 4 失敗 / 12 跳過
**測試組**: 商品列表檢視

**失敗測試**:
- Customer 檢視商品 - beforeEach 失敗
- Admin 檢視商品 - beforeEach 失敗
- 商品搜尋與篩選 - beforeEach 失敗
- 分頁功能 - beforeEach 失敗

---

### 7. products/product-management.cy.ts
**狀態**: 1 失敗 / 16 跳過
**測試組**: 商品管理功能（Admin）

**失敗測試**:
- 商品列表檢視 - beforeEach 登入失敗

---

### 8. orders/manual-flow-test.cy.ts
**狀態**: 1 失敗
**測試組**: 手動測試流程

**失敗測試**:
- 完整訂單流程（手動步驟） - 登入失敗

---

### 9. orders/order-creation-example.cy.ts
**狀態**: 2 失敗 / 1 跳過
**測試組**: 訂單建立範例

**失敗測試**:
- 完整購物流程 - 登入失敗
- 應處理購物車為空的情況 - 登入失敗

---

### 10. orders/order-creation.cy.ts
**狀態**: 1 失敗 / 16 跳過
**測試組**: 訂單建立流程

**失敗測試**:
- 訂單建立流程 - beforeEach 登入失敗

---

### 11. orders/order-list.cy.ts
**狀態**: 6 失敗 / 16 跳過
**測試組**: 訂單列表功能

**失敗測試**:
- Customer 檢視自己的訂單 (2 失敗)
- Admin 檢視所有訂單 (1 失敗)
- 訂單篩選功能 (1 失敗)
- 訂單搜尋功能 - 整個 suite 跳過
- 訂單詳情檢視 (2 失敗)

---

### 12. orders/payment.cy.ts
**狀態**: 1 失敗 / 21 跳過
**測試組**: 付款功能

**失敗測試**:
- 付款功能 - beforeEach 登入失敗

---

### 13. orders/shipping-management.cy.ts
**狀態**: 1 失敗 / 30 跳過
**測試組**: 出貨管理功能（Admin）

**失敗測試**:
- 出貨管理檢視 - beforeEach 登入失敗

---

## 🔧 修復計畫

### 階段 1: 修復登入頁面 (優先級: 🔴 極高)

**影響**: 修復後將解鎖所有 241 個測試

#### 1.1 修改 `frontend/src/views/Login.vue`

**需要加入的 `data-cy` 屬性**:

```vue
<template>
  <div class="login-container">
    <!-- 角色選擇器 -->
    <el-radio-group
      v-model="loginForm.role"
      data-cy="role-selector"
      class="role-selector">
      <el-radio-button label="customer" data-cy="role-customer">
        Customer
      </el-radio-button>
      <el-radio-button label="admin" data-cy="role-admin">
        Admin
      </el-radio-button>
    </el-radio-group>

    <!-- 用戶名輸入 -->
    <el-input
      v-model="loginForm.username"
      data-cy="username"
      placeholder="用戶名"
    />

    <!-- 密碼輸入 -->
    <el-input
      v-model="loginForm.password"
      type="password"
      data-cy="password"
      placeholder="密碼"
    />

    <!-- 登入按鈕 -->
    <el-button
      type="primary"
      data-cy="login-btn"
      :loading="loading"
      @click="handleLogin"
    >
      登入
    </el-button>

    <!-- 登出按鈕 (如果已登入) -->
    <el-button
      data-cy="logout-btn"
      @click="handleLogout"
    >
      登出
    </el-button>
  </div>
</template>
```

#### 1.2 確認 Cypress Commands

檢查 `cypress/support/commands.ts` 中的登入 helper:

```typescript
// Customer 登入
Cypress.Commands.add('loginAsCustomer', () => {
  cy.visit('/')
  cy.get('[data-cy=role-selector]').should('be.visible')
  cy.get('[data-cy=role-customer]').click()
  cy.get('[data-cy=username]').type('customer1')
  cy.get('[data-cy=password]').type('password123')
  cy.get('[data-cy=login-btn]').click()
  cy.url().should('not.include', '/login')
})

// Admin 登入
Cypress.Commands.add('loginAsAdmin', () => {
  cy.visit('/')
  cy.get('[data-cy=role-selector]').should('be.visible')
  cy.get('[data-cy=role-admin]').click()
  cy.get('[data-cy=username]').type('admin')
  cy.get('[data-cy=password]').type('password123')
  cy.get('[data-cy=login-btn]').click()
  cy.url().should('not.include', '/login')
})
```

**預期成果**: 修復後將有 ~180 個測試從 skipped 變為可執行

---

### 階段 2: 修復其他頁面的 data-cy 屬性 (優先級: 🟡 中)

完成階段 1 後，某些測試可能還會因為其他頁面缺少 `data-cy` 而失敗。

#### 2.1 需要檢查的頁面

1. **商品列表** (`frontend/src/views/customer/Products.vue`)
   - `[data-cy=product-list]`
   - `[data-cy=product-item]`
   - `[data-cy=search-input]`
   - `[data-cy=filter-select]`

2. **訂單列表** (`frontend/src/views/customer/Orders.vue`)
   - `[data-cy=order-list]`
   - `[data-cy=order-item]`
   - `[data-cy=order-status]`

3. **Chatbot** (`frontend/src/components/Chatbot.vue`)
   - `[data-cy=chatbot-trigger]`
   - `[data-cy=chatbot-window]`
   - `[data-cy=message-input]`
   - `[data-cy=send-btn]`

4. **管理員客服管理** (`frontend/src/views/admin/ChatManagement.vue`)
   - `[data-cy=session-list]`
   - `[data-cy=session-item]`
   - `[data-cy=chat-messages]`
   - `[data-cy=reply-input]`

#### 2.2 系統化的做法

創建一個標準化的 `data-cy` 命名規範：

```
元件類型-元素功能-索引(可選)

例如:
- product-list-item-1
- order-status-pending
- btn-submit-order
- input-search-keyword
```

---

### 階段 3: 驗證與優化 (優先級: 🟢 低)

#### 3.1 分批執行測試

```bash
# 先測試登入功能
npx cypress run --spec "cypress/e2e/acceptance/auth/login.cy.ts"

# 再測試商品功能
npx cypress run --spec "cypress/e2e/acceptance/products/**/*.cy.ts"

# 最後測試訂單功能
npx cypress run --spec "cypress/e2e/acceptance/orders/**/*.cy.ts"
```

#### 3.2 修復特定的業務邏輯測試

某些測試可能因為業務邏輯變更而需要更新：

- AI 聊天功能相關測試（需要 Ollama 服務）
- WebSocket 即時訊息測試
- 多語言支援測試

---

## 📈 預期成果

### 修復階段 1 後
- ✅ Passing: 1 → **~60** (60 個原本 failing 的測試)
- ❌ Failing: 59 → **~30** (部分測試仍需修正 data-cy)
- ⏭️ Skipped: 181 → **~10** (大部分測試解鎖)

### 修復階段 2 後
- ✅ Passing: ~60 → **~200+**
- ❌ Failing: ~30 → **~10** (只剩業務邏輯問題)
- ⏭️ Skipped: ~10 → **~5**

### 修復階段 3 後
- ✅ Passing: ~200 → **~230+**
- ❌ Failing: ~10 → **<5**
- ⏭️ Skipped: ~5 → **<5**

**目標通過率**: 從 **0.4%** 提升至 **95%+**

---

## 🎯 行動項目

### 立即執行（本週）

1. ✅ **執行 Cypress 測試並記錄失敗原因**
2. ⏳ **修改 Login.vue 加入所有必要的 data-cy 屬性**
3. ⏳ **重新執行測試驗證修復效果**

### 短期執行（下週）

4. ⏳ **為其他主要頁面加入 data-cy 屬性**
5. ⏳ **更新測試文件與命名規範**
6. ⏳ **設置 CI/CD 自動執行 Cypress 測試**

### 中期執行（下個月）

7. ⏳ **修復業務邏輯相關的測試失敗**
8. ⏳ **加入視覺回歸測試**
9. ⏳ **優化測試執行速度**

---

## 📚 參考資料

### Cypress Best Practices

1. **使用 data-cy 而非 class 或 id**
   - 理由：避免樣式更改影響測試
   - 範例：`<button data-cy="submit-btn">` 而非 `<button class="btn-primary">`

2. **命名規範**
   - 使用語義化的名稱
   - 避免使用索引（除非必要）
   - 保持一致性

3. **測試隔離**
   - 每個測試都應該獨立運行
   - 使用 `beforeEach` 重置狀態
   - 避免測試之間的依賴

### 測試文件位置

- 測試檔案：`/cypress/e2e/acceptance/`
- Helper commands：`/cypress/support/commands.ts`
- 配置檔：`/cypress/cypress.config.ts`

---

## 📊 測試覆蓋率目標

| 功能模組 | 當前通過率 | 目標通過率 |
|----------|-----------|-----------|
| 登入功能 | 7.7% | 100% |
| 商品管理 | 0% | 90%+ |
| 訂單管理 | 0% | 90%+ |
| 聊天機器人 | 0% | 85%+ |
| 管理員功能 | 0% | 85%+ |
| **整體** | **0.4%** | **95%+** |

---

## 🔄 持續改進

### 每次 Sprint 檢查
- [ ] 新功能是否有對應的 E2E 測試？
- [ ] 所有測試是否通過？
- [ ] 測試覆蓋率是否達標？

### 每月檢討
- [ ] 測試執行時間是否可接受？
- [ ] 是否有 flaky tests？
- [ ] 測試文件是否需要更新？

---

**文件版本**: 1.0
**最後更新**: 2025-10-21
**負責人**: Development Team
**審核人**: QA Team
