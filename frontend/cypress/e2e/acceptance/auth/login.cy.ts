/// <reference types="cypress" />

/**
 * 驗收測試：登入功能
 * 需求來源：INPUT_PROMPT.md - 前端畫面需求 1.登入功能
 */

describe('登入功能', () => {
  beforeEach(() => {
    cy.visit('/login')
  })

  describe('身份選擇', () => {
    it('應顯示身份選擇器（Customer/Admin）', () => {
      cy.get('[data-cy=role-selector]').should('be.visible')
      // Click to open dropdown
      cy.get('[data-cy=role-selector]').click()
      // Check options are visible
      cy.get('.el-select-dropdown__item').contains('Customer').should('be.visible')
      cy.get('.el-select-dropdown__item').contains('Admin').should('be.visible')
      // Close dropdown
      cy.get('[data-cy=role-selector]').click()
    })

    it('應預設選擇 Customer 身份', () => {
      // Check the data-value attribute or text content
      cy.get('[data-cy=role-selector]').find('.el-select__wrapper').should('exist')
      cy.get('[data-cy=role-selector]').should('have.attr', 'data-value', 'CUSTOMER')
    })

    it('應可以切換到 Admin 身份', () => {
      cy.get('[data-cy=role-selector]').click()
      cy.get('.el-select-dropdown__item').contains('Admin').click()
      cy.get('[data-cy=role-selector]').should('have.attr', 'data-value', 'ADMIN')
    })
  })

  describe('Customer 登入', () => {
    it('應可以使用 Customer 帳號成功登入', () => {
      // Customer is already selected by default
      cy.get('[data-cy=username]').type('customer1')
      cy.get('[data-cy=password]').type('password123')
      cy.get('[data-cy=login-btn]').click()

      cy.url().should('include', '/dashboard')
      cy.get('[data-cy=user-role]').should('contain', 'Customer')
      cy.get('[data-cy=username-display]').should('contain', 'customer1')
    })

    it('Customer 登入後應看到適當的功能選單', () => {
      cy.loginAsCustomer()

      // Customer 可見的功能
      cy.get('[data-cy=menu-products]').should('be.visible')
      cy.get('[data-cy=menu-orders]').should('be.visible')
      cy.get('[data-cy=menu-cart]').should('be.visible')

      // Customer 不可見的功能
      cy.get('[data-cy=menu-product-management]').should('not.exist')
      cy.get('[data-cy=menu-shipping-management]').should('not.exist')
    })

    it('應處理 Customer 登入失敗的情況', () => {
      // Customer is already selected by default
      cy.get('[data-cy=username]').type('customer1')
      cy.get('[data-cy=password]').type('wrongpassword')
      cy.get('[data-cy=login-btn]').click()

      // Wait for error to appear
      cy.get('[data-cy=error-message]', { timeout: 5000 }).should('be.visible')
      cy.url().should('include', '/login')
    })
  })

  describe('Admin 登入', () => {
    it('應可以使用 Admin 帳號成功登入', () => {
      cy.get('[data-cy=role-selector]').click()
      cy.get('.el-select-dropdown__item').contains('Admin').click()
      cy.get('[data-cy=username]').type('admin')
      cy.get('[data-cy=password]').type('password123')
      cy.get('[data-cy=login-btn]').click()

      cy.url().should('include', '/admin/dashboard')
      cy.get('[data-cy=user-role]').should('contain', 'Admin')
      cy.get('[data-cy=username-display]').should('contain', 'admin')
    })

    it('Admin 登入後應看到管理功能選單', () => {
      cy.loginAsAdmin()

      // Admin 專屬功能
      cy.get('[data-cy=menu-product-management]').should('be.visible')
      cy.get('[data-cy=menu-shipping-management]').should('be.visible')
      cy.get('[data-cy=menu-all-orders]').should('be.visible')

      // Admin 也可見的一般功能
      cy.get('[data-cy=menu-products]').should('be.visible')
    })
  })

  describe('登入狀態管理', () => {
    it('應在重新整理後保持登入狀態', () => {
      cy.loginAsCustomer()
      cy.reload()
      cy.get('[data-cy=username-display]').should('contain', 'customer1')
    })

    it('應可以成功登出', () => {
      cy.loginAsCustomer()
      cy.get('[data-cy=logout-btn]').click()

      // Confirm logout in message box
      cy.get('.el-message-box').should('be.visible')
      cy.get('.el-message-box__btns .el-button--primary').click()

      cy.url().should('include', '/login')
      cy.get('[data-cy=login-form]').should('be.visible')
    })

    it('未登入時訪問受保護頁面應重導向至登入頁', () => {
      cy.visit('/dashboard')
      cy.url().should('include', '/login')
    })
  })

  describe('表單驗證', () => {
    it('應驗證必填欄位', () => {
      cy.get('[data-cy=login-btn]').click()
      cy.get('[data-cy=username-error]').should('contain', '請輸入用戶名')
      cy.get('[data-cy=password-error]').should('contain', '請輸入密碼')
    })

    it('應顯示載入狀態', () => {
      cy.get('[data-cy=username]').type('customer1')
      cy.get('[data-cy=password]').type('password123')
      cy.get('[data-cy=login-btn]').click()
      cy.get('[data-cy=login-btn]').should('be.disabled')
      cy.get('[data-cy=loading-spinner]').should('be.visible')
    })
  })
})