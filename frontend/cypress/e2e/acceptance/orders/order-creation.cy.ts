/// <reference types="cypress" />

/**
 * 驗收測試：建立訂單
 * 需求來源：INPUT_PROMPT.md - 前端畫面需求 2.訂單清單 - 建立訂單
 */

describe('建立訂單功能', () => {
  beforeEach(() => {
    cy.loginAsCustomer()
  })

  describe('購物車功能', () => {
    beforeEach(() => {
      cy.visit('/products')
    })

    it('應可以將商品加入購物車', () => {
      cy.get('[data-cy=product-card]').first().within(() => {
        cy.get('[data-cy=product-name]').invoke('text').as('productName')
        cy.get('[data-cy=add-to-cart-btn]').click()
      })

      cy.get('[data-cy=quantity-modal]').should('be.visible')
      cy.get('[data-cy=quantity-input]').type('2')
      cy.get('[data-cy=confirm-add-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '已加入購物車')
      cy.get('[data-cy=cart-count]').should('contain', '2')
    })

    it('應可以檢視購物車內容', () => {
      // 先加入商品
      cy.addProductToCart('測試商品1', 2)
      cy.addProductToCart('測試商品2', 1)

      cy.get('[data-cy=cart-icon]').click()
      cy.get('[data-cy=cart-drawer]').should('be.visible')

      cy.get('[data-cy=cart-item]').should('have.length', 2)
      cy.get('[data-cy=cart-item]').first().within(() => {
        cy.get('[data-cy=item-name]').should('contain', '測試商品1')
        cy.get('[data-cy=item-quantity]').should('contain', '2')
        cy.get('[data-cy=item-subtotal]').should('be.visible')
      })

      cy.get('[data-cy=cart-total]').should('be.visible')
    })

    it('應可以修改購物車數量', () => {
      cy.addProductToCart('測試商品1', 2)
      cy.get('[data-cy=cart-icon]').click()

      cy.get('[data-cy=cart-item]').first().within(() => {
        cy.get('[data-cy=quantity-increase-btn]').click()
        cy.get('[data-cy=item-quantity]').should('contain', '3')
      })

      cy.get('[data-cy=cart-total]').should('contain', '更新後的總價')
    })

    it('應可以從購物車移除商品', () => {
      cy.addProductToCart('測試商品1', 2)
      cy.get('[data-cy=cart-icon]').click()

      cy.get('[data-cy=cart-item]').first().within(() => {
        cy.get('[data-cy=remove-item-btn]').click()
      })

      cy.get('[data-cy=confirm-dialog]').should('be.visible')
      cy.get('[data-cy=confirm-btn]').click()

      cy.get('[data-cy=cart-empty-message]').should('contain', '購物車是空的')
    })

    it('應檢查庫存限制', () => {
      // 嘗試加入超過庫存的數量
      cy.get('[data-cy=product-card]').contains('[data-cy=product-stock]', '庫存: 5').parent().within(() => {
        cy.get('[data-cy=add-to-cart-btn]').click()
      })

      cy.get('[data-cy=quantity-input]').type('10')
      cy.get('[data-cy=confirm-add-btn]').click()

      cy.get('[data-cy=error-message]').should('contain', '超過可用庫存')
      cy.get('[data-cy=quantity-input]').should('have.class', 'error')
    })
  })

  describe('結帳流程', () => {
    beforeEach(() => {
      cy.addProductToCart('測試商品1', 2)
      cy.addProductToCart('測試商品2', 1)
      cy.visit('/cart')
    })

    it('應顯示結帳頁面', () => {
      cy.get('[data-cy=checkout-btn]').click()
      cy.url().should('include', '/checkout')

      cy.get('[data-cy=order-summary]').should('be.visible')
      cy.get('[data-cy=order-items]').should('be.visible')
      cy.get('[data-cy=order-total]').should('be.visible')
    })

    it('應驗證訂單資訊', () => {
      cy.get('[data-cy=checkout-btn]').click()

      cy.get('[data-cy=order-items]').within(() => {
        cy.get('[data-cy=order-item]').should('have.length', 2)
        cy.get('[data-cy=order-item]').first().should('contain', '測試商品1')
        cy.get('[data-cy=order-item]').eq(1).should('contain', '測試商品2')
      })

      cy.get('[data-cy=total-amount]').should('not.be.empty')
      cy.get('[data-cy=customer-info]').should('contain', 'customer1')
    })

    it('應可以成功建立訂單', () => {
      cy.get('[data-cy=checkout-btn]').click()
      cy.get('[data-cy=confirm-order-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '訂單已成功建立')
      cy.get('[data-cy=order-number]').should('be.visible')
      cy.url().should('include', '/orders/')

      // 驗證購物車已清空
      cy.get('[data-cy=cart-count]').should('contain', '0')
    })

    it('建立訂單後應更新商品庫存', () => {
      // 記錄建立訂單前的庫存
      cy.visit('/products')
      cy.get('[data-cy=product-card]').first().within(() => {
        cy.get('[data-cy=product-stock]').invoke('text').then((stockText) => {
          const originalStock = parseInt(stockText.match(/\d+/)[0])

          // 建立訂單
          cy.visit('/cart')
          cy.get('[data-cy=checkout-btn]').click()
          cy.get('[data-cy=confirm-order-btn]').click()

          // 檢查庫存是否已減少
          cy.visit('/products')
          cy.get('[data-cy=product-card]').first().within(() => {
            cy.get('[data-cy=product-stock]').invoke('text').should('contain', (originalStock - 2).toString())
          })
        })
      })
    })

    it('應處理庫存不足的情況', () => {
      // 模擬在結帳過程中庫存被其他用戶購買完
      cy.intercept('POST', '/api/orders', {
        statusCode: 400,
        body: { message: 'Insufficient stock for product: 測試商品1' }
      }).as('createOrderWithInsufficientStock')

      cy.get('[data-cy=checkout-btn]').click()
      cy.get('[data-cy=confirm-order-btn]').click()

      cy.wait('@createOrderWithInsufficientStock')
      cy.get('[data-cy=error-message]').should('contain', '庫存不足')
      cy.get('[data-cy=error-details]').should('contain', '測試商品1')
    })
  })

  describe('訂單資訊確認', () => {
    beforeEach(() => {
      cy.addProductToCart('測試商品1', 2)
      cy.visit('/checkout')
    })

    it('應顯示完整的訂單資訊', () => {
      cy.get('[data-cy=order-summary]').within(() => {
        cy.get('[data-cy=customer-id]').should('be.visible')
        cy.get('[data-cy=order-items]').should('be.visible')
        cy.get('[data-cy=item-total]').should('be.visible')
        cy.get('[data-cy=total-amount]').should('be.visible')
        cy.get('[data-cy=order-status]').should('contain', 'CREATED')
      })
    })

    it('應正確計算總金額', () => {
      cy.get('[data-cy=order-item]').first().within(() => {
        cy.get('[data-cy=item-price]').invoke('text').then((price) => {
          cy.get('[data-cy=item-quantity]').invoke('text').then((qty) => {
            const expectedTotal = parseFloat(price.replace('$', '')) * parseInt(qty)
            cy.get('[data-cy=item-subtotal]').should('contain', expectedTotal.toFixed(2))
          })
        })
      })
    })

    it('應可以修改數量後重新計算', () => {
      cy.get('[data-cy=edit-quantity-btn]').click()
      cy.get('[data-cy=quantity-input]').clear().type('3')
      cy.get('[data-cy=update-quantity-btn]').click()

      cy.get('[data-cy=order-item]').first().within(() => {
        cy.get('[data-cy=item-quantity]').should('contain', '3')
      })

      cy.get('[data-cy=total-amount]').should('not.contain', '原來的金額')
    })

    it('應可以返回購物車修改', () => {
      cy.get('[data-cy=back-to-cart-btn]').click()
      cy.url().should('include', '/cart')
      cy.get('[data-cy=cart-items]').should('be.visible')
    })
  })

  describe('空購物車處理', () => {
    beforeEach(() => {
      cy.visit('/cart')
    })

    it('空購物車應顯示適當訊息', () => {
      cy.get('[data-cy=cart-empty-message]').should('be.visible')
      cy.get('[data-cy=cart-empty-message]').should('contain', '購物車是空的')
      cy.get('[data-cy=checkout-btn]').should('be.disabled')
    })

    it('空購物車應提供返回商品頁面的連結', () => {
      cy.get('[data-cy=continue-shopping-btn]').click()
      cy.url().should('include', '/products')
    })

    it('直接訪問結帳頁面應重導向到購物車', () => {
      cy.visit('/checkout')
      cy.url().should('include', '/cart')
      cy.get('[data-cy=error-message]').should('contain', '購物車不能為空')
    })
  })
})