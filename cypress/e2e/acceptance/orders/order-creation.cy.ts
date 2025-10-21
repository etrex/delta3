/// <reference types="cypress" />

/**
 * 驗收測試：建立訂單
 * 需求來源：INPUT_PROMPT.md - 前端畫面需求 2.訂單清單 - 建立訂單
 */

describe('建立訂單功能', () => {
  beforeEach(() => {
    cy.loginAsCustomer()

    // 為測試創建商品資料
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
        name: '測試商品3',
        description: '用於測試庫存限制',
        price: 199.99,
        stock: 5,
        stockThreshold: 5,
        status: 'ACTIVE'
      }
    ])
  })

  describe('購物車功能', () => {
    beforeEach(() => {
      cy.visit('/products')
    })

    it('應可以將商品加入購物車', () => {
      // Click product to enter detail page
      cy.get('[data-cy=product-card]').first().click()

      // Add to cart from product detail page
      cy.get('[data-cy=quantity-input]').find('input').clear().type('2')
      cy.get('[data-cy=add-to-cart-btn]').click()

      // Verify success message
      cy.contains('已將').should('be.visible')
    })

    it('應可以檢視購物車內容（在結帳頁）', () => {
      // Add products
      cy.addProductToCart('測試商品1', 2)
      cy.addProductToCart('測試商品2', 1)

      // Visit checkout to view cart
      cy.visit('/checkout')

      cy.get('[data-cy=order-items]').should('be.visible')
      cy.get('[data-cy=order-item]').should('have.length', 2)
      cy.get('[data-cy=order-item]').first().within(() => {
        cy.get('[data-cy=item-name]').should('contain', '測試商品1')
      })
    })

    it('應可以修改購物車數量（在結帳頁）', () => {
      cy.addProductToCart('測試商品1', 2)
      cy.visit('/checkout')

      // Record original total
      cy.get('[data-cy=total-amount]').invoke('text').then((originalTotal) => {
        // Increase quantity using + button
        cy.get('[data-cy=order-item]').first().within(() => {
          cy.get('[data-cy=increase-quantity-btn]').click()
        })

        cy.wait(500) // Wait for update

        // Verify total changed
        cy.get('[data-cy=total-amount]').invoke('text').should('not.equal', originalTotal)
      })
    })

    it('應可以從購物車移除商品（在結帳頁）', () => {
      cy.addProductToCart('測試商品1', 2)
      cy.visit('/checkout')

      cy.get('[data-cy=order-item]').first().within(() => {
        cy.get('[data-cy=remove-item-btn]').click()
      })

      // After removal, should redirect to products or show error
      cy.get('[data-cy=error-message]', { timeout: 3000 }).should('contain', '購物車')
    })

    it('應檢查庫存限制（在結帳頁）', () => {
      // Add product with limited stock
      cy.addProductToCart('測試商品3', 5) // Stock is 5
      cy.visit('/checkout')

      // When quantity equals stock, increase button should be disabled
      cy.get('[data-cy=order-item]').first().within(() => {
        cy.get('[data-cy=increase-quantity-btn]').should('be.disabled')
      })

      // Note: Stock warning (庫存不足) only shows when quantity > stock
      // Since quantity === stock here, no warning is displayed
      // This is the correct behavior as user cannot exceed stock limit
    })
  })

  describe('結帳流程', () => {
    beforeEach(() => {
      cy.addProductToCart('測試商品1', 2)
      cy.addProductToCart('測試商品2', 1)
    })

    it('應顯示結帳頁面', () => {
      cy.visit('/checkout')

      cy.get('[data-cy=order-summary]').should('be.visible')
      cy.get('[data-cy=order-items]').should('be.visible')
      cy.get('[data-cy=total-amount]').should('be.visible')
    })

    it('應驗證訂單資訊', () => {
      cy.visit('/checkout')

      cy.get('[data-cy=order-items]').within(() => {
        cy.get('[data-cy=order-item]').should('have.length', 2)
        cy.get('[data-cy=order-item]').first().should('contain', '測試商品1')
        cy.get('[data-cy=order-item]').eq(1).should('contain', '測試商品2')
      })

      cy.get('[data-cy=total-amount]').should('not.be.empty')
      cy.get('[data-cy=customer-info]').should('contain', 'customer1')
    })

    it('應可以成功建立訂單', () => {
      cy.visit('/checkout')
      cy.get('[data-cy=confirm-order-btn]').click()

      // After successful order creation, should redirect to order detail page
      // (success message is shown but immediately navigates away)
      cy.url({ timeout: 10000 }).should('include', '/orders/')
      cy.get('[data-cy=order-details]', { timeout: 5000 }).should('be.visible')
    })

    it('當庫存不足時應禁用結帳按鈕並顯示警告', () => {
      // Add product with stock=5 twice to exceed stock limit
      // First add: 3 items, Second add: 3 items (total 6 > stock 5)
      cy.addProductToCart('測試商品3', 3)
      cy.addProductToCart('測試商品3', 3)

      cy.visit('/checkout')

      // Checkout button should be disabled due to insufficient stock
      cy.get('[data-cy=confirm-order-btn]').should('be.disabled')

      // Should show stock warning message
      cy.get('[data-cy=stock-warning]').should('be.visible')
      cy.get('[data-cy=stock-warning]').should('contain', '庫存不足')

      // Should show checkout warning
      cy.get('[data-cy=checkout-warning]').should('be.visible')
      cy.get('[data-cy=checkout-warning]').should('contain', '請調整商品數量')
    })
  })

  describe('訂單資訊確認', () => {
    beforeEach(() => {
      cy.addProductToCart('測試商品1', 2)
      cy.visit('/checkout')
    })

    it('應顯示完整的訂單資訊', () => {
      cy.get('[data-cy=order-summary]').should('be.visible')
      cy.get('[data-cy=customer-info]').should('be.visible')
      cy.get('[data-cy=order-items]').should('be.visible')
      cy.get('[data-cy=item-total]').should('be.visible')
      cy.get('[data-cy=total-amount]').should('be.visible')
    })

    it('應正確計算總金額', () => {
      cy.get('[data-cy=order-item]').first().within(() => {
        cy.get('[data-cy=item-price]').invoke('text').then((price) => {
          const priceValue = parseFloat(price.replace('$', ''))
          cy.get('[data-cy=item-subtotal]').invoke('text').then((subtotal) => {
            const subtotalValue = parseFloat(subtotal.replace('$', ''))
            // Subtotal should be price * quantity (2)
            expect(subtotalValue).to.equal(priceValue * 2)
          })
        })
      })
    })

    it('應可以修改數量後重新計算', () => {
      // Record original total
      cy.get('[data-cy=total-amount]').invoke('text').then((originalTotal) => {
        // Update quantity using input number
        cy.get('[data-cy=order-item]').first().within(() => {
          cy.get('[data-cy=quantity-input]').find('input').clear().type('3')
        })

        cy.wait(500) // Wait for API update

        // Total should change
        cy.get('[data-cy=total-amount]').invoke('text').should('not.equal', originalTotal)
      })
    })

    it('應可以返回商品頁面', () => {
      cy.get('[data-cy=back-to-cart-btn]').click()
      cy.url().should('include', '/products')
    })
  })

  describe('空購物車處理', () => {
    it('直接訪問結帳頁面應顯示錯誤並重導向', () => {
      cy.visit('/checkout')

      // Should show error message
      cy.get('[data-cy=error-message]', { timeout: 3000 }).should('contain', '購物車不能為空')

      // Should redirect to products
      cy.url({ timeout: 3000 }).should('include', '/products')
    })
  })
})