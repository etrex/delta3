# E2E Tests

這是訂單管理系統的端到端測試專案，使用 Cypress 測試框架。

## 專案結構

```
cypress/
├── e2e/              # 測試檔案
│   └── acceptance/   # 驗收測試
│       ├── auth/
│       ├── orders/
│       ├── products/
│       └── chatbot/
├── support/          # 支援檔案
│   ├── commands.ts   # 自定義命令
│   └── e2e.ts        # 測試設定
├── cypress.config.ts # Cypress 配置
├── package.json      # 依賴管理
└── tsconfig.json     # TypeScript 配置
```

## 安裝依賴

```bash
npm install
```

## 執行測試

### 開啟 Cypress UI（互動模式）
```bash
npm run test:open
```

### 執行所有測試（無頭模式）
```bash
npm test
```

### 執行測試（顯示瀏覽器）
```bash
npm run test:headed
```

### 使用 Chrome 執行測試
```bash
npm run test:chrome
```

## 前置條件

執行測試前，確保以下服務正在運行：

1. **Backend** (http://localhost:8080)
   ```bash
   cd ../backend
   mvn spring-boot:run
   ```

2. **Frontend** (http://localhost:5173)
   ```bash
   cd ../frontend
   npm run dev
   ```

## 自定義命令

專案提供以下自定義 Cypress 命令：

- `cy.loginAsCustomer()` - 以客戶身份登入
- `cy.loginAsAdmin()` - 以管理員身份登入
- `cy.logout()` - 登出
- `cy.addProductToCart(productName, quantity)` - 加入商品到購物車
- `cy.createPaidOrder()` - 建立已付款訂單
- `cy.createShippedOrder()` - 建立已出貨訂單

## 測試資料管理

使用 Cypress task 管理測試資料：

```javascript
// 重置資料庫
cy.task('db:reset')

// 初始化預設用戶
cy.task('db:init:users')

// 建立測試商品
cy.task('db:seed:products', [
  {
    name: '測試商品',
    price: 100.00,
    stock: 50,
    status: 'ACTIVE'
  }
])
```

## 測試範例

```javascript
describe('訂單建立測試', () => {
  beforeEach(() => {
    cy.loginAsCustomer()
    cy.task('db:seed:products', [...])
  })

  it('應該能成功建立訂單', () => {
    cy.addProductToCart('測試商品', 2)
    cy.visit('/cart')
    cy.get('[data-cy=checkout-btn]').click()
    cy.get('[data-cy=confirm-order-btn]').click()
    cy.url().should('match', /\/orders\/\d+/)
  })
})
```
