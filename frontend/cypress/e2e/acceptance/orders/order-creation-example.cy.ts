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
    
    cy.get('[data-cy=product-card]').first().within(() => {
      cy.get('[data-cy=product-name]').should('contain', '測試商品1')
      cy.get('[data-cy=add-to-cart-btn]').click()
    })

    cy.get('[data-cy=quantity-modal]').should('be.visible')
    cy.get('[data-cy=quantity-input]').clear().type('2')
    cy.get('[data-cy=confirm-add-btn]').click()

    cy.get('[data-cy=success-message]').should('contain', '已加入購物車')
    cy.get('[data-cy=cart-count]').should('contain', '2')
  })

  it('應檢查庫存限制', () => {
    cy.visit('/products')
    
    // 找到庫存只有 5 的商品
    cy.get('[data-cy=product-card]').contains('庫存少的商品').parent().within(() => {
      cy.get('[data-cy=product-stock]').should('contain', '5')
      cy.get('[data-cy=add-to-cart-btn]').click()
    })

    cy.get('[data-cy=quantity-input]').clear().type('10')
    cy.get('[data-cy=confirm-add-btn]').click()

    cy.get('[data-cy=error-message]').should('contain', '超過可用庫存')
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
