/// <reference types="cypress" />

// ***********************************************
// This example commands.ts shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

import 'cypress-file-upload'

declare global {
  namespace Cypress {
    interface Clock {
      tick(milliseconds: number): Clock
      restore(): void
    }

    interface Chainable {
      /**
       * Custom command to login as customer
       * @example cy.loginAsCustomer()
       */
      loginAsCustomer(): Chainable

      /**
       * Custom command to login as admin
       * @example cy.loginAsAdmin()
       */
      loginAsAdmin(): Chainable

      /**
       * Custom command to add product to cart
       * @example cy.addProductToCart('商品名稱', 2)
       */
      addProductToCart(productName: string, quantity: number): Chainable

      /**
       * Custom command to logout
       * @example cy.logout()
       */
      logout(): Chainable

      /**
       * Custom command to create a paid order for testing
       * @example cy.createPaidOrder()
       */
      createPaidOrder(): Chainable

      /**
       * Attach file command from cypress-file-upload
       * @example cy.get('input').attachFile('file.csv')
       */
      attachFile(fileName: string): Chainable

      /**
       * Custom command to create a shipped order for testing
       * @example cy.createShippedOrder()
       */
      createShippedOrder(): Chainable

      /**
       * Custom command to create a return request for testing
       * @example cy.createReturnRequest(returnId => { ... })
       */
      createReturnRequest(): Chainable

      /**
       * Custom command to create an unpaid order for testing
       * @example cy.createUnpaidOrder()
       */
      createUnpaidOrder(): Chainable

      /**
       * Custom command to fill credit card form
       * @example cy.fillCreditCardForm()
       */
      fillCreditCardForm(): Chainable

      /**
       * Custom command to create a failed payment order
       * @example cy.createFailedPaymentOrder()
       */
      createFailedPaymentOrder(): Chainable

      /**
       * Intercept network requests (built-in Cypress command)
       * @example cy.intercept('GET', '/api/**', { fixture: 'data.json' })
       */
      intercept(method: string, url: string, response?: any): Chainable

      /**
       * Clock control for time manipulation
       * @example cy.clock()
       */
      clock(timestamp?: number): Chainable

      /**
       * Tick the clock forward
       * @example cy.tick(1000)
       */
      tick(milliseconds: number): Chainable
    }
  }
}

// @ts-ignore
Cypress.Commands.add('loginAsCustomer', () => {
  cy.visit('/login')
  cy.get('[data-cy=role-selector]').click()
  cy.get('.el-select-dropdown__item').contains('Customer').click()
  cy.get('[data-cy=username]').type('customer1')
  cy.get('[data-cy=password]').type('password123')
  cy.get('[data-cy=login-btn]').click()

  // Wait for successful redirect
  cy.url({ timeout: 15000 }).should('satisfy', (url) => {
    return url.includes('/dashboard') || url.includes('/products')
  })
})

// @ts-ignore
Cypress.Commands.add('loginAsAdmin', () => {
  cy.visit('/login')
  cy.get('[data-cy=role-selector]').click()
  cy.get('.el-select-dropdown__item').contains('Admin').click()
  cy.get('[data-cy=username]').type('admin')
  cy.get('[data-cy=password]').type('password123')
  cy.get('[data-cy=login-btn]').click()

  // Wait for successful redirect
  cy.url({ timeout: 15000 }).should('satisfy', (url) => {
    return url.includes('/admin/dashboard') || url.includes('/admin')
  })
})

// @ts-ignore
Cypress.Commands.add('addProductToCart', (productName: string, quantity: number) => {
  cy.visit('/products')

  // Find the product card containing the product name and click add to cart
  cy.get('[data-cy=product-card]')
    .filter(`:contains("${productName}")`)
    .first()
    .within(() => {
      cy.get('[data-cy=add-to-cart-btn]').click()
    })

  // Fill in quantity and confirm
  cy.get('[data-cy=quantity-modal]').should('be.visible')
  cy.get('[data-cy=quantity-input]').clear().type(quantity.toString())
  cy.get('[data-cy=confirm-add-btn]').click()

  // Wait for success message to appear
  cy.get('[data-cy=success-message]').should('be.visible').should('contain', '已加入購物車')

  // Wait for modal to close
  cy.get('[data-cy=quantity-modal]').should('not.be.visible')
})

// @ts-ignore
Cypress.Commands.add('logout', () => {
  cy.get('[data-cy=logout-btn]').click()
  cy.url().should('include', '/login')
  cy.get('[data-cy=login-form]').should('be.visible')
})

// @ts-ignore
Cypress.Commands.add('createPaidOrder', () => {
  // 先添加商品到購物車
  cy.addProductToCart('測試商品1', 1)

  // 前往結帳
  cy.visit('/cart')
  cy.get('[data-cy=checkout-btn]').click()

  // 確認訂單
  cy.get('[data-cy=confirm-order-btn]').click()

  // 進行付款
  cy.get('[data-cy=pay-now-btn]').click()
  cy.get('[data-cy=payment-method]').select('CREDIT_CARD')
  cy.get('[data-cy=confirm-payment-btn]').click()

  // 驗證付款成功
  cy.get('[data-cy=payment-success]').should('be.visible')
  cy.get('[data-cy=order-status]').should('contain', 'PAID')
})

// @ts-ignore
Cypress.Commands.add('createShippedOrder', () => {
  // 先建立已付款訂單
  cy.createPaidOrder()

  // 取得訂單 ID
  cy.get('[data-cy=order-id]').invoke('text').as('shippedOrderId')

  // 切換到管理員身分
  cy.logout()
  cy.loginAsAdmin()

  // 前往出貨管理頁面
  cy.visit('/admin/shipping')

  // 標記訂單為已出貨
  cy.get('@shippedOrderId').then((orderId) => {
    cy.get(`[data-cy=order-${orderId}]`).within(() => {
      cy.get('[data-cy=ship-order-btn]').click()
    })
    cy.get('[data-cy=confirm-ship-btn]').click()
    cy.get('[data-cy=order-status]').should('contain', 'SHIPPED')

    // 返回訂單 ID
    return cy.wrap(orderId)
  })
})

// @ts-ignore
Cypress.Commands.add('createReturnRequest', () => {
  // 先建立已出貨訂單
  cy.createShippedOrder().as('returnOrderId')

  // 切換回客戶身分
  cy.logout()
  cy.loginAsCustomer()

  // 前往訂單頁面申請退貨
  cy.get('@returnOrderId').then((orderId) => {
    cy.visit(`/orders/${orderId}`)
    cy.get('[data-cy=request-return-btn]').click()

    // 填寫退貨原因
    cy.get('[data-cy=return-reason]').select('商品瑕疵')
    cy.get('[data-cy=return-description]').type('商品有瑕疵，申請退貨')
    cy.get('[data-cy=submit-return-btn]').click()

    // 取得退貨申請 ID
    cy.get('[data-cy=return-id]').invoke('text').then((returnId) => {
      return cy.wrap(returnId)
    })
  })
})

// @ts-ignore
Cypress.Commands.add('createUnpaidOrder', () => {
  // 先添加商品到購物車
  cy.addProductToCart('測試商品1', 1)

  // 前往結帳
  cy.visit('/cart')
  cy.get('[data-cy=checkout-btn]').click()

  // 確認訂單但不付款
  cy.get('[data-cy=confirm-order-btn]').click()

  // 取得訂單 ID
  cy.get('[data-cy=order-id]').invoke('text').then((orderId) => {
    return cy.wrap(orderId)
  })
})

// @ts-ignore
Cypress.Commands.add('fillCreditCardForm', () => {
  cy.get('[data-cy=card-number]').type('4111111111111111')
  cy.get('[data-cy=card-expiry]').type('12/25')
  cy.get('[data-cy=card-cvc]').type('123')
  cy.get('[data-cy=card-name]').type('Test User')
})

// @ts-ignore
Cypress.Commands.add('createFailedPaymentOrder', () => {
  // 建立未付款訂單
  cy.createUnpaidOrder().as('failedOrderId')

  // 模擬付款失敗
  cy.get('@failedOrderId').then((orderId) => {
    cy.visit(`/orders/${orderId}/payment`)
    cy.get('[data-cy=payment-method]').select('CREDIT_CARD')

    // 使用無效卡號
    cy.get('[data-cy=card-number]').type('4000000000000002') // 卡號會失敗
    cy.get('[data-cy=card-expiry]').type('12/25')
    cy.get('[data-cy=card-cvc]').type('123')
    cy.get('[data-cy=card-name]').type('Test User')

    cy.get('[data-cy=pay-btn]').click()
    cy.get('[data-cy=payment-failed]').should('be.visible')

    return cy.wrap(orderId)
  })
})

export {}