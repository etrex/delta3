/**
 * 驗收測試：商品管理（Admin only）
 * 需求來源：INPUT_PROMPT.md - 前端畫面需求 4.建立商品(Admin only)
 */

describe('商品管理功能（Admin）', () => {
  beforeEach(() => {
    cy.loginAsAdmin()
  })

  describe('新增商品', () => {
    beforeEach(() => {
      cy.visit('/admin/products/new')
    })

    it('應顯示新增商品表單', () => {
      cy.get('[data-cy=product-form]').should('be.visible')
      cy.get('[data-cy=product-name-input]').should('be.visible')
      cy.get('[data-cy=product-description-input]').should('be.visible')
      cy.get('[data-cy=product-price-input]').should('be.visible')
      cy.get('[data-cy=product-stock-input]').should('be.visible')
      cy.get('[data-cy=product-status-select]').should('be.visible')
    })

    it('應可以成功建立新商品', () => {
      cy.get('[data-cy=product-name-input]').type('測試新商品')
      cy.get('[data-cy=product-description-input]').type('這是測試商品的描述')
      cy.get('[data-cy=product-price-input]').type('299.99')
      cy.get('[data-cy=product-stock-input]').type('100')
      cy.get('[data-cy=product-status-select]').select('上架')

      cy.get('[data-cy=save-product-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '商品已成功創建')
      cy.url().should('include', '/admin/products')
      cy.get('[data-cy=product-list]').should('contain', '測試新商品')
    })

    it('應驗證必填欄位', () => {
      cy.get('[data-cy=save-product-btn]').click()

      cy.get('[data-cy=name-error]').should('contain', '商品名稱為必填')
      cy.get('[data-cy=price-error]').should('contain', '價格為必填')
      cy.get('[data-cy=stock-error]').should('contain', '庫存數量為必填')
    })

    it('應驗證價格格式', () => {
      cy.get('[data-cy=product-price-input]').type('abc')
      cy.get('[data-cy=save-product-btn]').click()
      cy.get('[data-cy=price-error]').should('contain', '請輸入有效的價格')

      cy.get('[data-cy=product-price-input]').clear().type('-10')
      cy.get('[data-cy=save-product-btn]').click()
      cy.get('[data-cy=price-error]').should('contain', '價格必須大於0')
    })

    it('應驗證庫存數量', () => {
      cy.get('[data-cy=product-stock-input]').type('-5')
      cy.get('[data-cy=save-product-btn]').click()
      cy.get('[data-cy=stock-error]').should('contain', '庫存不能為負數')
    })

    it('應可以取消新增商品', () => {
      cy.get('[data-cy=product-name-input]').type('測試商品')
      cy.get('[data-cy=cancel-btn]').click()

      cy.get('[data-cy=confirm-dialog]').should('be.visible')
      cy.get('[data-cy=confirm-cancel-btn]').click()

      cy.url().should('include', '/admin/products')
    })
  })

  describe('編輯商品', () => {
    beforeEach(() => {
      cy.visit('/admin/products')
      cy.get('[data-cy=product-card]').first().find('[data-cy=edit-product-btn]').click()
    })

    it('應載入現有商品資料', () => {
      cy.get('[data-cy=product-name-input]').should('have.value').and('not.be.empty')
      cy.get('[data-cy=product-price-input]').should('have.value').and('not.be.empty')
      cy.get('[data-cy=product-stock-input]').should('have.value').and('not.be.empty')
    })

    it('應可以更新商品資訊', () => {
      cy.get('[data-cy=product-name-input]').clear().type('更新後的商品名稱')
      cy.get('[data-cy=product-price-input]').clear().type('399.99')
      cy.get('[data-cy=save-product-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '商品已成功更新')
      cy.get('[data-cy=product-list]').should('contain', '更新後的商品名稱')
      cy.get('[data-cy=product-list]').should('contain', '399.99')
    })

    it('應可以調整庫存', () => {
      cy.get('[data-cy=stock-adjustment-btn]').click()
      cy.get('[data-cy=stock-adjustment-modal]').should('be.visible')

      cy.get('[data-cy=adjustment-type]').select('增加')
      cy.get('[data-cy=adjustment-quantity]').type('50')
      cy.get('[data-cy=adjustment-reason]').type('進貨')
      cy.get('[data-cy=confirm-adjustment-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '庫存已調整')
    })
  })

  describe('商品上下架管理', () => {
    beforeEach(() => {
      cy.visit('/admin/products')
    })

    it('應可以下架商品', () => {
      // 找到上架中的商品
      cy.get('[data-cy=product-status-active]').first().parent().within(() => {
        cy.get('[data-cy=toggle-status-btn]').click()
      })

      cy.get('[data-cy=confirm-dialog]').should('be.visible')
      cy.get('[data-cy=confirm-message]').should('contain', '確定要下架此商品嗎')
      cy.get('[data-cy=confirm-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '商品已下架')
    })

    it('應可以重新上架商品', () => {
      // 顯示所有商品包含下架
      cy.get('[data-cy=show-inactive-toggle]').click()

      // 找到下架的商品
      cy.get('[data-cy=product-status-inactive]').first().parent().within(() => {
        cy.get('[data-cy=toggle-status-btn]').click()
      })

      cy.get('[data-cy=confirm-dialog]').should('be.visible')
      cy.get('[data-cy=confirm-message]').should('contain', '確定要上架此商品嗎')
      cy.get('[data-cy=confirm-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '商品已上架')
    })

    it('下架商品應不會出現在客戶端', () => {
      // 先下架一個商品
      cy.get('[data-cy=product-card]').first().within(() => {
        cy.get('[data-cy=product-name]').invoke('text').as('productName')
        cy.get('[data-cy=toggle-status-btn]').click()
      })
      cy.get('[data-cy=confirm-btn]').click()

      // 切換到 Customer 視角
      cy.logout()
      cy.loginAsCustomer()
      cy.visit('/products')

      // 確認下架商品不顯示
      cy.get('@productName').then((name) => {
        cy.get('[data-cy=product-list]').should('not.contain', name)
      })
    })
  })

  describe('商品列表管理檢視', () => {
    beforeEach(() => {
      cy.visit('/admin/products')
    })

    it('商品應顯示在列表中包含所需資訊', () => {
      cy.get('[data-cy=product-table]').should('be.visible')
      cy.get('[data-cy=product-row]').first().within(() => {
        cy.get('[data-cy=product-id]').should('be.visible') // 商品編號
        cy.get('[data-cy=product-name]').should('be.visible')
        cy.get('[data-cy=product-created-at]').should('be.visible') // 建立時間
        cy.get('[data-cy=product-status]').should('be.visible') // 狀態
        cy.get('[data-cy=product-price]').should('be.visible') // 總金額（價格）
        cy.get('[data-cy=product-stock]').should('be.visible')
      })
    })

    it('應可以批量操作商品', () => {
      cy.get('[data-cy=select-all-checkbox]').click()
      cy.get('[data-cy=bulk-actions-menu]').should('be.visible')
      cy.get('[data-cy=bulk-actions-menu]').select('批量下架')
      cy.get('[data-cy=confirm-bulk-action-btn]').click()

      cy.get('[data-cy=confirm-dialog]').should('be.visible')
      cy.get('[data-cy=confirm-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '已成功處理')
    })

    it('應可以匯出商品清單', () => {
      cy.get('[data-cy=export-btn]').click()
      cy.get('[data-cy=export-format-select]').select('CSV')
      cy.get('[data-cy=confirm-export-btn]').click()

      // 驗證下載開始
      cy.get('[data-cy=success-message]').should('contain', '匯出成功')
    })
  })

  describe('庫存檢查功能', () => {
    beforeEach(() => {
      cy.visit('/admin/products')
    })

    it('應顯示庫存警告', () => {
      cy.get('[data-cy=low-stock-warning]').should('be.visible')
      cy.get('[data-cy=low-stock-badge]').should('exist')
    })

    it('應可以設定庫存警告門檻', () => {
      cy.get('[data-cy=product-card]').first().find('[data-cy=edit-product-btn]').click()
      cy.get('[data-cy=stock-threshold-input]').clear().type('10')
      cy.get('[data-cy=save-product-btn]').click()

      // 當庫存低於門檻時應顯示警告
      cy.visit('/admin/products')
      cy.get('[data-cy=product-card]').first().within(() => {
        cy.get('[data-cy=low-stock-indicator]').should('be.visible')
      })
    })
  })
})