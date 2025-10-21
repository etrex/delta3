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
       * @example cy.fillCreditCardForm({ cardExpiry: '01/20' })
       */
      fillCreditCardForm(options?: {
        cardNumber?: string
        cardExpiry?: string
        cardCvv?: string
        cardName?: string
      }): Chainable

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
  // Customer is selected by default, no need to change role
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
  // Select Admin role
  cy.get('[data-cy=role-admin]').click()
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
  // Always visit products page to ensure we start fresh
  cy.visit('/products')

  // Wait for products to load with longer timeout
  cy.get('[data-cy=product-card]', { timeout: 10000 }).should('have.length.at.least', 1)

  // Find and click the product card to enter detail page
  cy.get('[data-cy=product-card]')
    .filter(`:contains("${productName}")`)
    .first()
    .click()

  // Wait for product detail page to load
  cy.get('[data-cy=product-detail-card]').should('be.visible')

  // Fill in quantity
  cy.get('[data-cy=quantity-input]').find('input').clear().type(quantity.toString())

  // Click add to cart button
  cy.get('[data-cy=add-to-cart-btn]').click()

  // Wait for success message
  cy.contains('已將').should('be.visible')

  // Go back to products page
  cy.visit('/products')
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
  cy.visit('/checkout')

  // 確認訂單
  cy.get('[data-cy=confirm-order-btn]').click()

  // 進行付款
  cy.get('[data-cy=pay-now-btn]').click()
  cy.get('[data-cy=payment-method-credit-card]').click()

  // 填寫信用卡資訊
  cy.get('[data-cy=card-number]').type('4111111111111111')
  cy.get('[data-cy=card-expiry]').type('12/25')
  cy.get('[data-cy=card-cvv]').type('123')
  cy.get('[data-cy=card-name]').type('Test User')

  cy.get('[data-cy=confirm-payment-btn]').click()

  // 驗證付款成功
  cy.get('[data-cy=success-message]', { timeout: 10000 }).should('contain', '付款成功')
  cy.get('[data-cy=order-status]').should('contain', '已付款')

  // 從 URL 取得訂單 ID
  cy.url().should('match', /\/orders\/\d+/)
  cy.url().then((url) => {
    const orderId = url.match(/\/orders\/(\d+)/)?.[1]
    return cy.wrap(orderId)
  })
})

// @ts-ignore
Cypress.Commands.add('createShippedOrder', () => {
  // 先建立已付款訂單
  cy.createPaidOrder()

  // 從 URL 取得訂單 ID
  cy.url().then((url) => {
    const orderId = url.match(/\/orders\/(\d+)/)?.[1]
    cy.wrap(orderId).as('shippedOrderId')
  })

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
    cy.get('[data-cy=order-status]').should('contain', '已出貨')

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
  cy.visit('/checkout')

  // 確認訂單但不付款
  cy.get('[data-cy=confirm-order-btn]').click()

  // 等待跳轉到訂單詳情頁
  cy.url().should('match', /\/orders\/\d+/)

  // 從 URL 取得訂單 ID
  cy.url().then((url) => {
    const orderId = url.match(/\/orders\/(\d+)/)?.[1]
    return cy.wrap(orderId)
  })
})

// @ts-ignore
Cypress.Commands.add('fillCreditCardForm', (options = {}) => {
  const {
    cardNumber = '4111111111111111',
    cardExpiry = '12/25',
    cardCvv = '123',
    cardName = 'Test User'
  } = options

  cy.get('[data-cy=card-number]').type(cardNumber)
  cy.get('[data-cy=card-expiry]').type(cardExpiry)
  cy.get('[data-cy=card-cvv]').type(cardCvv)
  cy.get('[data-cy=card-name]').type(cardName)
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
    cy.get('[data-cy=card-cvv]').type('123')
    cy.get('[data-cy=card-name]').type('Test User')

    cy.get('[data-cy=pay-btn]').click()
    cy.get('[data-cy=payment-failed]').should('be.visible')

    return cy.wrap(orderId)
  })
})

export {}