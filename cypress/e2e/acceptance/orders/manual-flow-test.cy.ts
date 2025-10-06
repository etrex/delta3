/// <reference types="cypress" />

/**
 * 手動流程測試：驗證完整的結帳→付款→出貨流程
 */

describe('完整訂單流程測試', () => {
  beforeEach(() => {
    cy.loginAsCustomer()

    // 建立測試商品
    cy.task('db:seed:products', [
      {
        name: '流程測試商品',
        description: '用於測試完整流程',
        price: 100.00,
        stock: 50,
        stockThreshold: 10,
        status: 'ACTIVE'
      }
    ])
  })

  it('完整流程：加入購物車 → 結帳 → 付款 → 管理員出貨', () => {
    // 1. 加入購物車
    cy.addProductToCart('流程測試商品', 2)

    // 2. 前往購物車
    cy.visit('/cart')

    // 驗證購物車有商品
    cy.get('[data-cy=cart-items]').should('be.visible')
    cy.get('[data-cy=cart-total]').should('contain', '200.00')

    // 3. 結帳
    cy.get('[data-cy=checkout-btn]').should('not.be.disabled')
    cy.get('[data-cy=checkout-btn]').click()

    // 4. 確認訂單
    cy.url().should('include', '/checkout')
    cy.get('[data-cy=order-summary]').should('be.visible')
    cy.get('[data-cy=confirm-order-btn]').click()

    // 5. 應該跳轉到訂單詳情頁
    cy.url().should('match', /\/orders\/\d+/)
    cy.get('[data-cy=order-details]').should('be.visible')

    // 6. 付款
    cy.get('[data-cy=pay-now-btn]').should('be.visible')
    cy.get('[data-cy=pay-now-btn]').click()

    cy.get('[data-cy=payment-modal]').should('be.visible')
    cy.get('[data-cy=payment-method-credit-card]').click()

    // 填寫信用卡資訊
    cy.get('[data-cy=card-number]').type('4111111111111111')
    cy.get('[data-cy=card-expiry]').type('12/25')
    cy.get('[data-cy=card-cvv]').type('123')
    cy.get('[data-cy=card-name]').type('TEST USER')

    cy.get('[data-cy=confirm-payment-btn]').click()

    // 7. 驗證付款成功
    cy.get('[data-cy=payment-completed-badge]', { timeout: 10000 }).should('be.visible')
    cy.get('[data-cy=order-status]').should('contain', 'PAID')

    // 8. 記錄訂單號
    cy.get('[data-cy=order-id]').invoke('text').then((orderIdText) => {
      const orderId = orderIdText.match(/\d+/)[0]

      // 9. 切換到管理員
      cy.logout()
      cy.loginAsAdmin()

      // 10. 前往出貨管理
      cy.visit('/admin/shipping')

      // 11. 找到訂單並出貨
      cy.get(`[data-cy=order-${orderId}]`).should('be.visible')
      cy.get(`[data-cy=ship-btn-${orderId}]`).click()

      // 12. 驗證出貨成功
      cy.get('[data-cy=shipping-success-message]').should('be.visible')
    })
  })
})
