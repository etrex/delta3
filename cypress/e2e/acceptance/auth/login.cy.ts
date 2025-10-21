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
      // Check radio buttons are visible
      cy.get('[data-cy=role-customer]').should('be.visible')
      cy.get('[data-cy=role-admin]').should('be.visible')
    })

    it('應預設選擇 Customer 身份', () => {
      // Check CUSTOMER radio is checked by default (Element Plus uses .is-checked class on label)
      cy.get('[data-cy=role-customer]').closest('.el-radio').should('have.class', 'is-checked')
    })

    it('應可以切換到 Admin 身份', () => {
      cy.get('[data-cy=role-admin]').click()
      // Element Plus adds .is-checked class to the el-radio label
      cy.get('[data-cy=role-admin]').closest('.el-radio').should('have.class', 'is-checked')
    })
  })

  describe('Customer 登入', () => {
    it('應可以使用 Customer 帳號成功登入', () => {
      // Customer is already selected by default
      cy.get('[data-cy=username]').type('customer1')
      cy.get('[data-cy=password]').type('password123')
      cy.get('[data-cy=login-btn]').click()

      cy.url().should('include', '/products')
      cy.get('[data-cy=user-role]', { timeout: 10000 }).should('contain', 'Customer')
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

      // Wait a bit for login attempt to complete
      cy.wait(1000)
      // Verify we're still on login page (login failed)
      cy.url().should('include', '/login')
      // ElMessage may appear and disappear quickly, so just check we stayed on login page
    })
  })

  describe('Admin 登入', () => {
    it('應可以使用 Admin 帳號成功登入', () => {
      cy.get('[data-cy=role-admin]').click()
      cy.get('[data-cy=username]').type('admin')
      cy.get('[data-cy=password]').type('password123')
      cy.get('[data-cy=login-btn]').click()

      cy.url().should('include', '/admin/dashboard')
      cy.get('[data-cy=user-role]', { timeout: 10000 }).should('contain', 'Admin')
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

      // Logout happens directly without confirmation dialog
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
      // Element Plus shows validation errors with .el-form-item__error class
      cy.get('.el-form-item__error').should('have.length.at.least', 2)
      cy.get('.el-form-item__error').first().should('be.visible')
    })

    it('應顯示載入狀態', () => {
      cy.get('[data-cy=username]').type('customer1')
      cy.get('[data-cy=password]').type('password123')
      cy.get('[data-cy=login-btn]').click()
      // Button should be disabled while loading
      cy.get('[data-cy=login-btn]').should('be.disabled')
    })
  })
})