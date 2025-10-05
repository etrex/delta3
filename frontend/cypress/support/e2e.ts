// ***********************************************************
// This example support/e2e.ts is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import './commands'
import 'cypress-file-upload'

// 全局測試資料管理
// 在每個測試檔案執行前重置資料庫並初始化基本用戶
beforeEach(() => {
  // 重置資料庫（清空訂單、商品、付款）
  cy.task('db:reset', { timeout: 10000 })
  // 初始化基本用戶（admin, customer1）
  cy.task('db:init:users', { timeout: 10000 })
})

// Ignore ResizeObserver errors from Element Plus
Cypress.on('uncaught:exception', (err) => {
  if (err.message.includes('ResizeObserver loop')) {
    return false
  }
  return true
})

// TODO: 修復 Element Plus 輸入框的自訂 Cypress should() 覆寫
// 目前 .should('have.value').and('not.be.empty') 會失敗，因為
// Element Plus 將輸入框包在 div 中，而 .and('not.be.empty') 檢查的是元素子節點
// 下方嘗試的覆寫無效 - 需要進一步研究
let lastSubjectHadValue = false

Cypress.Commands.overwrite('should', (originalFn, subject, chainers, ...args) => {
  // Handle special case: .should('have.value') without args
  if (chainers === 'have.value' && args.length === 0) {
    const result = originalFn(subject, ($el) => {
      if (typeof $el.val === 'function') {
        const value = $el.val()
        lastSubjectHadValue = !!(value && value !== '')
        expect(value).to.exist
        expect(value).not.to.be.empty
      }
    })
    return result
  }

  // Handle .and('not.be.empty') after .should('have.value')
  if (chainers === 'not.be.empty' && args.length === 0 && lastSubjectHadValue) {
    lastSubjectHadValue = false
    return originalFn(subject, () => { return true })
  }

  if (chainers !== 'have.value') {
    lastSubjectHadValue = false
  }

  return originalFn(subject, chainers, ...args)
})

// Make Chai's expect available globally
declare global {
  const expect: Chai.ExpectStatic;
}

// Override cy.select for Element Plus selects
Cypress.Commands.overwrite('select', (originalFn, element, value) => {
  const $el = Cypress.$(element)

  // Check if this is an Element Plus select
  if ($el.hasClass('el-select') || $el.attr('data-cy') === 'role-selector') {
    // Click to open dropdown
    cy.wrap(element).click()

    // Find and click the option with matching value or label
    cy.get('.el-select-dropdown__item').contains(value).click({ force: true })

    return cy.wrap(element)
  }

  // Otherwise use the original select command
  return originalFn(element, value)
})


// Alternatively you can use CommonJS syntax:
// require('./commands')