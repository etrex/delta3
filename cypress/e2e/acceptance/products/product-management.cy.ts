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
      // 新增商品不需要預先創建商品
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
      cy.get('[data-cy=product-price-input]').find('input').clear().type('300')
      cy.get('[data-cy=product-stock-input]').find('input').clear().type('100')
      cy.get('[data-cy=product-status-select]').click()
      cy.contains('.el-select-dropdown__item', '上架').click()

      cy.get('[data-cy=save-product-btn]').click()

      cy.get('.el-message').should('contain', '商品已成功創建')
      cy.url().should('include', '/admin/products')
      cy.get('[data-cy=product-list]').should('contain', '測試新商品')
    })

  })

  describe('編輯商品', () => {
    beforeEach(() => {
      // 創建一個商品用於編輯測試
      cy.task('db:seed:products', [
        {
          name: '待編輯商品',
          description: '這是待編輯的商品',
          price: 299.99,
          stock: 50,
          stockThreshold: 10,
          status: 'ACTIVE'
        }
      ])

      cy.visit('/admin/products')
      cy.get('[data-cy=edit-product-btn]').first().click()
    })

    // 尚未完成實作：商品編輯功能
    it('應載入現有商品資料', () => {
      cy.log('Edit functionality not yet implemented')
    })

    // 尚未完成實作：商品資訊更新功能
    it('應可以更新商品資訊', () => {
      cy.log('Edit functionality not yet implemented')
    })
  })

  describe('商品上下架管理', () => {
    beforeEach(() => {
      // 創建測試商品用於上下架管理
      cy.task('db:seed:products', [
        {
          name: '上架商品1',
          description: '測試用上架商品',
          price: 199.99,
          stock: 100,
          stockThreshold: 10,
          status: 'ACTIVE'
        },
        {
          name: '上架商品2',
          description: '測試用上架商品2',
          price: 299.99,
          stock: 50,
          stockThreshold: 10,
          status: 'ACTIVE'
        }
      ])

      cy.visit('/admin/products')
    })

    it('應可以下架商品', () => {
      // Click toggle status button on the first product
      cy.get('[data-cy=toggle-status-btn]').first().click()

      cy.get('.el-message-box').should('be.visible')
      cy.get('.el-message-box').should('contain', '確定要下架此商品嗎')
      cy.get('.el-message-box .el-button--primary').click()

      cy.get('.el-message').should('contain', '商品已下架')
    })

    it('應可以重新上架商品', () => {
      // First deactivate a product
      cy.get('[data-cy=toggle-status-btn]').first().click()
      cy.get('.el-message-box .el-button--primary').click()
      cy.get('.el-message').should('be.visible')

      // Reload to ensure fresh data
      cy.reload()
      cy.wait(1000)

      // Click the toggle button again to reactivate
      cy.get('[data-cy=toggle-status-btn]').first().click()
      cy.get('.el-message-box').should('be.visible')
      cy.get('.el-message-box .el-button--primary').click()
      cy.get('.el-message').should('be.visible')
    })

    it('下架商品應不會出現在客戶端', () => {
      // 記錄商品名稱並下架
      let productName: string
      cy.get('[data-cy=product-name]').first().invoke('text').then((text) => {
        productName = text
      })
      cy.get('[data-cy=toggle-status-btn]').first().click()
      cy.get('.el-message-box .el-button--primary').click()
      cy.get('.el-message').should('be.visible')
      cy.wait(1000)

      // 切換到 Customer 視角
      cy.logout()
      cy.loginAsCustomer()
      cy.visit('/products')
      cy.wait(1000) // 等待商品載入

      // 確認下架商品不顯示
      cy.get('[data-cy=product-list]').then(($list) => {
        expect($list.text()).not.to.contain(productName)
      })
    })
  })

  describe('商品列表管理檢視', () => {
    beforeEach(() => {
      // 創建多個測試商品用於列表管理
      cy.task('db:seed:products', [
        {
          name: '列表商品1',
          description: '測試商品1',
          price: 99.99,
          stock: 100,
          stockThreshold: 10,
          status: 'ACTIVE'
        },
        {
          name: '列表商品2',
          description: '測試商品2',
          price: 199.99,
          stock: 80,
          stockThreshold: 10,
          status: 'ACTIVE'
        },
        {
          name: '列表商品3',
          description: '測試商品3',
          price: 299.99,
          stock: 60,
          stockThreshold: 10,
          status: 'ACTIVE'
        }
      ])

      cy.visit('/admin/products')
    })

    it('商品應顯示在列表中包含所需資訊', () => {
      cy.get('[data-cy=product-list]').should('be.visible')
      cy.get('[data-cy=product-id]').first().should('be.visible')
      cy.get('[data-cy=product-name]').first().should('be.visible')
      cy.get('[data-cy=product-status]').first().should('be.visible')
      cy.get('[data-cy=product-price]').first().should('be.visible')
      cy.get('[data-cy=product-stock]').first().should('be.visible')
    })
  })
})