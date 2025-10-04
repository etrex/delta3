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
      cy.get('[data-cy=product-detail-modal]').should('be.visible')

      cy.get('[data-cy=product-detail-modal]').within(() => {
        cy.get('[data-cy=product-name]').should('be.visible')
        cy.get('[data-cy=product-price]').should('be.visible')
        cy.get('[data-cy=product-description]').should('be.visible')
        cy.get('[data-cy=product-stock]').should('be.visible')
        cy.get('[data-cy=add-to-cart-btn]').should('be.visible')
      })
    })

    it('應可以將商品加入購物車', () => {
      cy.get('[data-cy=product-card]').first().within(() => {
        cy.get('[data-cy=quick-add-btn]').click()
      })

      cy.get('[data-cy=quantity-input]').find('input').clear().type('2')
      cy.get('[data-cy=confirm-add-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '已加入購物車')
      cy.get('[data-cy=cart-count]').should('contain', '2')
    })

    it('庫存不足時應顯示提示', () => {
      // 找到庫存數量少的商品（測試商品 3，庫存: 1）
      cy.get('[data-cy=product-card]').contains('[data-cy=product-name]', '測試商品 3')
        .parents('[data-cy=product-card]')
        .within(() => {
          cy.get('[data-cy=quick-add-btn]').click()
        })

      cy.get('[data-cy=quantity-input]').find('input').clear().type('5')
      cy.get('[data-cy=confirm-add-btn]').click()

      cy.get('[data-cy=error-message]', { timeout: 6000 }).should('contain', '庫存不足')
    })
  })

  describe('Admin 檢視商品', () => {
    beforeEach(() => {
      cy.loginAsAdmin()
      cy.visit('/products')
    })

    it('Admin 可以看到所有商品（包含下架）', () => {
      cy.get('[data-cy=show-inactive-toggle]').should('be.visible')
      cy.get('[data-cy=show-inactive-toggle]').click()

      cy.get('[data-cy=product-card]').should('have.length.at.least', 2)
      cy.get('[data-cy=product-status-inactive]').should('exist')
    })

    it('Admin 應看到管理按鈕', () => {
      cy.get('[data-cy=product-card]').first().within(() => {
        cy.get('[data-cy=edit-product-btn]').should('be.visible')
        cy.get('[data-cy=toggle-status-btn]').should('be.visible')
      })
    })

    it('Admin 可以快速切換商品狀態', () => {
      cy.get('[data-cy=product-card]').first().within(() => {
        cy.get('[data-cy=toggle-status-btn]').click()
      })

      cy.get('[data-cy=confirm-dialog]').should('be.visible')
      cy.get('[data-cy=confirm-btn]').click()
      cy.get('[data-cy=success-message]').should('contain', '商品狀態已更新')
    })
  })

  describe('商品搜尋與篩選', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/products')
    })

    it('應可以搜尋商品', () => {
      cy.get('[data-cy=search-input]').type('測試商品')
      cy.get('[data-cy=search-btn]').click()

      cy.get('[data-cy=product-card]').each(($card) => {
        cy.wrap($card).should('contain', '測試商品')
      })
    })

    it('應可以按價格範圍篩選', () => {
      cy.get('[data-cy=price-min]').type('100')
      cy.get('[data-cy=price-max]').type('300')
      cy.get('[data-cy=apply-filter-btn]').click()

      cy.get('[data-cy=product-card]').each(($card) => {
        cy.wrap($card).find('[data-cy=product-price]').then(($price) => {
          const price = parseFloat($price.text().replace('$', ''))
          expect(price).to.be.at.least(100)
          expect(price).to.be.at.most(300)
        })
      })
    })

    it('應可以按類別篩選', () => {
      cy.get('[data-cy=category-filter]').select('電子產品')
      cy.get('[data-cy=product-list]').should('not.contain', '食品')
    })

    it('應可以排序商品', () => {
      cy.get('[data-cy=sort-select]').select('價格由低到高')

      let previousPrice = 0
      cy.get('[data-cy=product-card]').each(($card) => {
        cy.wrap($card).find('[data-cy=product-price]').then(($price) => {
          const currentPrice = parseFloat($price.text().replace('$', ''))
          expect(currentPrice).to.be.at.least(previousPrice)
          previousPrice = currentPrice
        })
      })
    })
  })

  describe('分頁功能', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/products')
    })

    it('應顯示分頁控制項', () => {
      cy.get('[data-cy=pagination]').should('be.visible')
      cy.get('[data-cy=page-info]').should('contain', '第 1 頁')
    })

    it('應可以切換頁面', () => {
      cy.get('[data-cy=next-page-btn]').click()
      cy.get('[data-cy=page-info]').should('contain', '第 2 頁')
      cy.get('[data-cy=product-list]').should('be.visible')
    })

    it('應可以選擇每頁顯示數量', () => {
      cy.get('[data-cy=page-size-select]').select('20')
      cy.get('[data-cy=product-card]').should('have.length.at.most', 20)
    })
  })
})