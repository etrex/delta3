/// <reference types="cypress" />
/// <reference types="chai" />

/**
 * 驗收測試：商品列表
 * 需求來源：INPUT_PROMPT.md - 前端畫面需求 2.訂單清單 - 顯示商品列表
 */

describe('商品列表檢視', () => {
  describe('Customer 檢視商品', () => {
    beforeEach(() => {
      cy.loginAsCustomer()

      // 為這個測試套件創建需要的商品
      cy.task('db:seed:products', [
        {
          name: 'MacBook Pro',
          description: '高性能筆記型電腦',
          price: 1999.99,
          stock: 10,
          stockThreshold: 5,
          status: 'ACTIVE'
        },
        {
          name: 'iPhone 15',
          description: '最新款智慧型手機',
          price: 999.99,
          stock: 50,
          stockThreshold: 10,
          status: 'ACTIVE'
        },
        {
          name: 'AirPods Pro',
          description: '降噪耳機',
          price: 249.99,
          stock: 5,
          stockThreshold: 5,
          status: 'ACTIVE'
        }
      ])

      cy.visit('/products')
    })

    it('應顯示商品列表', () => {
      cy.get('[data-cy=product-list]').should('be.visible')
      cy.get('[data-cy=product-card]').should('have.length.at.least', 1)
    })

    it('每個商品應顯示必要資訊', () => {
      cy.get('[data-cy=product-card]').first().within(() => {
        cy.get('[data-cy=product-name]').should('be.visible')
        cy.get('[data-cy=product-price]').should('be.visible')
        cy.get('[data-cy=product-description]').should('be.visible')
        cy.get('[data-cy=product-stock]').should('be.visible')
        cy.get('[data-cy=product-image]').should('be.visible')
      })
    })

    it('Customer 只能看到上架（ACTIVE）的商品', () => {
      cy.get('[data-cy=product-card]').each(($card) => {
        cy.wrap($card).find('[data-cy=product-status]').should('not.exist')
      })
      // 確認沒有顯示下架商品
      cy.get('[data-cy=product-list]').should('not.contain', '下架商品')
    })

    it('點擊商品可查看詳細資訊', () => {
      cy.get('[data-cy=product-card]').first().click()
      cy.get('[data-cy=product-detail-card]').should('be.visible')

      cy.get('[data-cy=product-detail-card]').within(() => {
        cy.get('[data-cy=product-name]').should('be.visible')
        cy.get('[data-cy=product-price]').should('be.visible')
        cy.get('[data-cy=product-description]').should('be.visible')
        cy.get('[data-cy=product-stock]').should('be.visible')
        cy.get('[data-cy=add-to-cart-btn]').should('be.visible')
      })
    })

    it('應可以將商品加入購物車', () => {
      // Click product card to enter detail page
      cy.get('[data-cy=product-card]').first().click()

      // Wait for detail page
      cy.get('[data-cy=product-detail-card]').should('be.visible')

      // Fill quantity and add to cart (el-input-number needs to find input element)
      cy.get('[data-cy=quantity-input]').find('input').clear().type('2')
      cy.get('[data-cy=add-to-cart-btn]').click()

      // Verify success message
      cy.contains('已將').should('be.visible')
    })

    it('應將超過庫存的數量自動修正為最大值', () => {
      // 確保商品列表已載入
      cy.get('[data-cy=product-list]').should('be.visible')
      cy.get('[data-cy=product-card]').should('have.length.at.least', 1)

      // 找到庫存只有 5 的商品（AirPods Pro）並點擊進入商品詳情頁面
      cy.get('[data-cy=product-card]').contains('AirPods Pro').click()

      // 等待商品詳情頁面載入
      cy.url().should('match', /\/products\/\d+/)
      cy.get('[data-cy=product-detail-card]').should('be.visible')
      cy.get('[data-cy=product-stock]').should('contain', '5')

      // 嘗試輸入超過庫存的數量（15）
      cy.get('[data-cy=quantity-input]').find('input').clear().type('15')

      // 失焦後，input-number 會自動將值修正為最大值（5）
      cy.get('[data-cy=quantity-input]').find('input').blur()

      // 驗證數量已被自動修正為 5
      cy.get('[data-cy=quantity-input]').find('input').should('have.value', '5')

      // 驗證可以成功加入購物車（因為數量已被修正為合法範圍）
      cy.get('[data-cy=add-to-cart-btn]').click()
      cy.contains('已將').should('be.visible')
    })
  })
})