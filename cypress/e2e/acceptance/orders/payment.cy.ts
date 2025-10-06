/// <reference types="cypress" />
/// <reference types="chai" />

/**
 * 驗收測試：付款功能
 * 需求來源：INPUT_PROMPT.md - 前端畫面需求 2.訂單清單 - 付款
 */

describe('付款功能', () => {
  beforeEach(() => {
    cy.loginAsCustomer()
    // 先建立一個未付款的訂單
    cy.createUnpaidOrder().as('orderId')
  })

  describe('付款流程', () => {
    beforeEach(() => {
      cy.get('@orderId').then((orderId) => {
        cy.visit(`/orders/${orderId}`)
      })
    })

    it('未付款訂單應顯示付款按鈕', () => {
      cy.get('[data-cy=order-status]').should('contain', 'CREATED')
      cy.get('[data-cy=pay-now-btn]').should('be.visible')
      cy.get('[data-cy=pay-now-btn]').should('not.be.disabled')
    })

    it('點擊付款應顯示付款方式選擇', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-modal]').should('be.visible')

      cy.get('[data-cy=payment-methods]').should('be.visible')
      cy.get('[data-cy=payment-method-credit-card]').should('be.visible')
      cy.get('[data-cy=payment-method-bank-transfer]').should('be.visible')
      cy.get('[data-cy=payment-method-paypal]').should('be.visible')
      cy.get('[data-cy=payment-method-cash]').should('be.visible')
    })

    it('應可以選擇信用卡付款', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-credit-card]').click()

      cy.get('[data-cy=credit-card-form]').should('be.visible')
      cy.get('[data-cy=card-number]').should('be.visible')
      cy.get('[data-cy=card-expiry]').should('be.visible')
      cy.get('[data-cy=card-cvv]').should('be.visible')
      cy.get('[data-cy=card-name]').should('be.visible')
    })

    it('應驗證信用卡資訊', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-credit-card]').click()
      cy.get('[data-cy=confirm-payment-btn]').click()

      cy.get('[data-cy=card-number-error]').should('contain', '請輸入卡號')
      cy.get('[data-cy=card-expiry-error]').should('contain', '請輸入有效期')
      cy.get('[data-cy=card-cvv-error]').should('contain', '請輸入安全碼')
    })

    it('應可以成功完成信用卡付款', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-credit-card]').click()

      cy.get('[data-cy=card-number]').type('4111111111111111')
      cy.get('[data-cy=card-expiry]').type('12/25')
      cy.get('[data-cy=card-cvv]').type('123')
      cy.get('[data-cy=card-name]').type('Test User')

      cy.get('[data-cy=confirm-payment-btn]').click()

      cy.get('[data-cy=payment-processing]').should('be.visible')
      cy.get('[data-cy=success-message]').should('contain', '付款成功')
      cy.get('[data-cy=order-status]').should('contain', 'PAID')
    })

    it('應可以選擇銀行轉帳', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-bank-transfer]').click()

      cy.get('[data-cy=bank-transfer-info]').should('be.visible')
      cy.get('[data-cy=bank-account]').should('be.visible')
      cy.get('[data-cy=bank-code]').should('be.visible')
      cy.get('[data-cy=transfer-amount]').should('be.visible')
      cy.get('[data-cy=reference-number]').should('be.visible')
    })

    it('應可以選擇 PayPal 付款', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-paypal]').click()

      cy.get('[data-cy=paypal-redirect-info]').should('be.visible')
      cy.get('[data-cy=paypal-pay-btn]').click()

      // 模擬 PayPal 重導向
      cy.get('[data-cy=paypal-processing]').should('be.visible')
    })

    it('應可以選擇現金付款', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-cash]').click()

      cy.get('[data-cy=cash-payment-info]').should('be.visible')
      cy.get('[data-cy=cash-instructions]').should('contain', '請於收貨時準備現金')
      cy.get('[data-cy=confirm-cash-payment-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '現金付款訂單已確認')
      cy.get('[data-cy=payment-status]').should('contain', 'PENDING')
    })
  })

  describe('付款狀態處理', () => {
    it('已付款訂單不應顯示付款按鈕', () => {
      cy.createPaidOrder().then((orderId: any) => {
        cy.visit(`/orders/${orderId}`)
        cy.get('[data-cy=order-status]').should('contain', 'PAID')
        cy.get('[data-cy=pay-now-btn]').should('not.exist')
        cy.get('[data-cy=payment-completed-badge]').should('be.visible')
      })
    })

    it('應顯示付款歷史記錄', () => {
      cy.createPaidOrder().then((orderId: any) => {
        cy.visit(`/orders/${orderId}`)
        cy.get('[data-cy=payment-history]').should('be.visible')

        cy.get('[data-cy=payment-record]').within(() => {
          cy.get('[data-cy=payment-method]').should('be.visible')
          cy.get('[data-cy=payment-amount]').should('be.visible')
          cy.get('[data-cy=payment-date]').should('be.visible')
          cy.get('[data-cy=transaction-id]').should('be.visible')
          cy.get('[data-cy=payment-status]').should('contain', 'SUCCESS')
        })
      })
    })

    it('付款失敗應顯示錯誤訊息', () => {
      cy.intercept('POST', '/api/orders/*/pay', {
        statusCode: 400,
        body: { message: 'Payment failed: Insufficient funds' }
      }).as('paymentFailed')

      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-credit-card]').click()
      cy.fillCreditCardForm()
      cy.get('[data-cy=confirm-payment-btn]').click()

      cy.wait('@paymentFailed')
      cy.get('[data-cy=error-message]').should('contain', '付款失敗')
      cy.get('[data-cy=error-details]').should('contain', 'Insufficient funds')
      cy.get('[data-cy=retry-payment-btn]').should('be.visible')
    })

    it('應可以重試失敗的付款', () => {
      // 先創建一個付款失敗的訂單
      cy.createFailedPaymentOrder().then((orderId: any) => {
        cy.visit(`/orders/${orderId}`)
        cy.get('[data-cy=payment-failed-badge]').should('be.visible')
        cy.get('[data-cy=retry-payment-btn]').should('be.visible')

        cy.get('[data-cy=retry-payment-btn]').click()
        cy.get('[data-cy=payment-modal]').should('be.visible')
      })
    })
  })

  describe('付款安全性', () => {
    beforeEach(() => {
      cy.get('@orderId').then((orderId) => {
        cy.visit(`/orders/${orderId}`)
      })
    })

    it('信用卡號應部分遮蔽顯示', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-credit-card]').click()
      cy.get('[data-cy=card-number]').type('4111111111111111')

      // 輸入後應顯示遮蔽的卡號
      cy.get('[data-cy=card-display]').should('contain', '**** **** **** 1111')
    })

    it('CVV 應隱藏輸入', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-credit-card]').click()
      cy.get('[data-cy=card-cvv]').should('have.attr', 'type', 'password')
    })

    it('付款表單應有 HTTPS 要求', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=security-notice]').should('contain', '安全付款')
      cy.get('[data-cy=ssl-indicator]').should('be.visible')
    })

    it('付款超時應自動取消', () => {
      cy.clock()
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-credit-card]').click()

      // 模擬付款超時
      cy.tick(300000) // 5分鐘
      cy.get('[data-cy=timeout-warning]').should('be.visible')
      cy.get('[data-cy=timeout-warning]').should('contain', '付款已超時')
    })
  })

  describe('付款金額驗證', () => {
    beforeEach(() => {
      cy.get('@orderId').then((orderId) => {
        cy.visit(`/orders/${orderId}`)
      })
    })

    it('付款金額應與訂單總額一致', () => {
      cy.get('[data-cy=order-total]').invoke('text').then((orderTotal) => {
        cy.get('[data-cy=pay-now-btn]').click()
        cy.get('[data-cy=payment-amount]').should('contain', orderTotal.trim())
      })
    })

    it('應顯示付款手續費（如有）', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-credit-card]').click()

      cy.get('[data-cy=payment-breakdown]').within(() => {
        cy.get('[data-cy=order-amount]').should('be.visible')
        cy.get('[data-cy=processing-fee]').should('be.visible')
        cy.get('[data-cy=total-payment-amount]').should('be.visible')
      })
    })

    it('不同付款方式應顯示不同手續費', () => {
      cy.get('[data-cy=pay-now-btn]').click()

      // 信用卡手續費
      cy.get('[data-cy=payment-method-credit-card]').click()
      cy.get('[data-cy=processing-fee]').invoke('text').as('creditCardFee')

      // 銀行轉帳手續費
      cy.get('[data-cy=payment-method-bank-transfer]').click()
      cy.get('[data-cy=processing-fee]').invoke('text').as('bankTransferFee')

      cy.get('@creditCardFee').then((ccFee) => {
        cy.get('@bankTransferFee').then((btFee) => {
          expect(ccFee).to.not.equal(btFee)
        })
      })
    })
  })

  describe('行動裝置付款', () => {
    beforeEach(() => {
      cy.viewport('iphone-6', 'portrait')
      cy.get('@orderId').then((orderId) => {
        cy.visit(`/orders/${orderId}`)
      })
    })

    it('行動版付款介面應適當顯示', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-modal]').should('have.class', 'mobile-optimized')
      cy.get('[data-cy=payment-methods]').should('be.visible')
    })

    it('應支援行動支付方式', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-apple-pay]').should('be.visible')
      cy.get('[data-cy=payment-method-google-pay]').should('be.visible')
    })

    it('數字鍵盤應正確顯示', () => {
      cy.get('[data-cy=pay-now-btn]').click()
      cy.get('[data-cy=payment-method-credit-card]').click()
      cy.get('[data-cy=card-number]').should('have.attr', 'inputmode', 'numeric')
      cy.get('[data-cy=card-cvv]').should('have.attr', 'inputmode', 'numeric')
    })
  })
})