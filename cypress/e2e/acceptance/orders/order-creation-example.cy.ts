/// <reference types="cypress" />

/**
 * 範例：展示如何使用新的測試資料管理方式
 * 
 * 重要概念：
 * 1. beforeEach 會自動重置資料庫（在 cypress/support/e2e.ts 中設定）
 * 2. 每個測試套件自己創建需要的商品資料
 * 3. 使用 cy.task('db:seed:products') 批量創建商品
 */

describe('訂單建立範例（使用新的資料管理方式）', () => {
  beforeEach(() => {
    // 登入（用戶已由全局 beforeEach 自動初始化）
    cy.loginAsCustomer()
    
    // 為這個測試套件創建需要的商品
    cy.task('db:seed:products', [
      {
        name: '測試商品1',
        description: '這是測試商品1的描述',
        price: 99.99,
        stock: 100,
        stockThreshold: 10,
        status: 'ACTIVE'
      },
      {
        name: '測試商品2',
        description: '這是測試商品2的描述',
        price: 149.99,
        stock: 50,
        stockThreshold: 5,
        status: 'ACTIVE'
      },
      {
        name: '庫存少的商品',
        description: '用於測試庫存限制',
        price: 199.99,
        stock: 5,
        stockThreshold: 5,
        status: 'ACTIVE'
      }
    ])
  })

  it('應可以將商品加入購物車', () => {
    cy.visit('/products')

    // 現在商品應該出現了！
    cy.get('[data-cy=product-card]').should('have.length', 3)

    // 點擊商品卡片進入商品詳情頁面（會導航到 /products/{id}）
    cy.get('[data-cy=product-card]').first().click()

    // 等待商品詳情頁面載入
    cy.url().should('match', /\/products\/\d+/)
    cy.get('[data-cy=product-detail-card]').should('be.visible')
    cy.get('[data-cy=product-name]').should('contain', '測試商品1')

    // 設定數量並加入購物車
    cy.get('[data-cy=quantity-input]').find('input').clear().type('2')
    cy.get('[data-cy=add-to-cart-btn]').click()

    // 驗證成功訊息
    cy.contains('已將').should('be.visible')
  })

  it('應將超過庫存的數量自動修正為最大值', () => {
    cy.visit('/products')

    // 找到庫存只有 5 的商品並點擊進入商品詳情頁面
    cy.get('[data-cy=product-card]').contains('庫存少的商品').click()

    // 等待商品詳情頁面載入
    cy.url().should('match', /\/products\/\d+/)
    cy.get('[data-cy=product-detail-card]').should('be.visible')
    cy.get('[data-cy=product-stock]').should('contain', '5')

    // 嘗試輸入超過庫存的數量（10）
    cy.get('[data-cy=quantity-input]').find('input').clear().type('10')

    // 失焦後，input-number 會自動將值修正為最大值（5）
    cy.get('[data-cy=quantity-input]').find('input').blur()

    // 驗證數量已被自動修正為 5
    cy.get('[data-cy=quantity-input]').find('input').should('have.value', '5')

    // 驗證可以成功加入購物車（因為數量已被修正為合法範圍）
    cy.get('[data-cy=add-to-cart-btn]').click()
    cy.contains('已將').should('be.visible')
  })
})

describe('不同測試套件可以有不同的測試資料', () => {
  beforeEach(() => {
    cy.loginAsCustomer()
    
    // 這個測試套件只需要一個商品
    cy.task('db:seed:products', [
      {
        name: '特殊商品',
        price: 999.99,
        stock: 1,
        status: 'ACTIVE'
      }
    ])
  })

  it('應只顯示這個測試套件的商品', () => {
    cy.visit('/products')
    cy.get('[data-cy=product-card]').should('have.length', 1)
    cy.get('[data-cy=product-name]').should('contain', '特殊商品')
  })
})
