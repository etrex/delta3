/**
 * 驗收測試：出貨管理頁面（Admin only）
 * 需求來源：INPUT_PROMPT.md - 前端畫面需求 3.出貨管理頁面(Admin only)
 */

describe('出貨管理功能（Admin）', () => {
  beforeEach(() => {
    cy.loginAsAdmin()
    cy.visit('/admin/shipping')
  })

  describe('出貨管理檢視', () => {
    it('應顯示所有需要處理的訂單', () => {
      cy.get('[data-cy=shipping-order-list]').should('be.visible')
      cy.get('[data-cy=order-row]').should('have.length.at.least', 1)
    })

    it('應顯示訂單的出貨和付款狀態', () => {
      cy.get('[data-cy=order-row]').first().within(() => {
        cy.get('[data-cy=order-id]').should('be.visible')
        cy.get('[data-cy=customer-name]').should('be.visible')
        cy.get('[data-cy=payment-status]').should('be.visible')
        cy.get('[data-cy=shipping-status]').should('be.visible')
        cy.get('[data-cy=order-date]').should('be.visible')
        cy.get('[data-cy=total-amount]').should('be.visible')
      })
    })

    it('應顯示付款狀態標籤', () => {
      cy.get('[data-cy=payment-status]').each(($status) => {
        cy.wrap($status).should('have.class').and('match', /status-(pending|success|failed|refunded)/)
      })
    })

    it('應顯示出貨狀態標籤', () => {
      cy.get('[data-cy=shipping-status]').each(($status) => {
        cy.wrap($status).should('have.class').and('match', /shipping-(pending|processing|shipped|delivered)/)
      })
    })

    it('應只顯示已付款的訂單', () => {
      cy.get('[data-cy=order-row]').each(($row) => {
        cy.wrap($row).find('[data-cy=payment-status]').should('not.contain', 'PENDING')
      })
    })
  })

  describe('出貨狀態管理', () => {
    it('應可以標記訂單為待出貨狀態', () => {
      cy.get('[data-cy=order-row]').contains('[data-cy=shipping-status]', 'CREATED').parent().within(() => {
        cy.get('[data-cy=ship-action-btn]').click()
      })

      cy.get('[data-cy=shipping-action-modal]').should('be.visible')
      cy.get('[data-cy=action-approve]').click()
      cy.get('[data-cy=confirm-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '訂單已標記為待出貨')
    })

    it('應可以標記訂單為已出貨狀態', () => {
      cy.get('[data-cy=order-row]').contains('[data-cy=shipping-status]', 'APPROVED').parent().within(() => {
        cy.get('[data-cy=ship-action-btn]').click()
      })

      cy.get('[data-cy=shipping-action-modal]').should('be.visible')
      cy.get('[data-cy=action-ship]').click()

      // 填寫出貨資訊
      cy.get('[data-cy=tracking-number]').type('TRK123456789')
      cy.get('[data-cy=carrier-select]').select('順豐速運')
      cy.get('[data-cy=estimated-delivery]').type('2024-01-15')
      cy.get('[data-cy=shipping-notes]').type('請收件人攜帶身分證件')

      cy.get('[data-cy=confirm-ship-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '訂單已標記為已出貨')
    })

    it('應可以標記訂單為已送達狀態', () => {
      cy.get('[data-cy=order-row]').contains('[data-cy=shipping-status]', 'SHIPPED').parent().within(() => {
        cy.get('[data-cy=ship-action-btn]').click()
      })

      cy.get('[data-cy=shipping-action-modal]').should('be.visible')
      cy.get('[data-cy=action-deliver]').click()

      cy.get('[data-cy=delivery-confirmation]').should('be.visible')
      cy.get('[data-cy=delivered-date]').type('2024-01-16')
      cy.get('[data-cy=delivery-notes]').type('已送達，由本人簽收')

      cy.get('[data-cy=confirm-delivery-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '訂單已標記為已送達')
    })

    it('出貨狀態應按順序進行（待出貨 → 已出貨 → 已送達）', () => {
      // 測試不能跳過狀態
      cy.get('[data-cy=order-row]').contains('[data-cy=shipping-status]', 'CREATED').parent().within(() => {
        cy.get('[data-cy=ship-action-btn]').click()
      })

      cy.get('[data-cy=shipping-action-modal]').within(() => {
        cy.get('[data-cy=action-ship]').should('be.disabled')
        cy.get('[data-cy=action-deliver]').should('be.disabled')
        cy.get('[data-cy=action-approve]').should('not.be.disabled')
      })
    })

    it('應驗證必填的出貨資訊', () => {
      cy.get('[data-cy=order-row]').contains('[data-cy=shipping-status]', 'APPROVED').parent().within(() => {
        cy.get('[data-cy=ship-action-btn]').click()
      })

      cy.get('[data-cy=action-ship]').click()
      cy.get('[data-cy=confirm-ship-btn]').click()

      cy.get('[data-cy=tracking-number-error]').should('contain', '請輸入追蹤號碼')
      cy.get('[data-cy=carrier-error]').should('contain', '請選擇物流商')
    })
  })

  describe('批量出貨操作', () => {
    it('應可以選擇多個訂單進行批量操作', () => {
      cy.get('[data-cy=select-all-checkbox]').click()
      cy.get('[data-cy=selected-count]').should('contain', '已選擇')
      cy.get('[data-cy=bulk-shipping-actions]').should('be.visible')
    })

    it('應可以批量標記為待出貨', () => {
      // 選擇所有 CREATED 狀態的訂單
      cy.get('[data-cy=order-row]').contains('[data-cy=shipping-status]', 'CREATED').parent().within(() => {
        cy.get('[data-cy=select-checkbox]').click()
      })

      cy.get('[data-cy=bulk-approve-btn]').click()
      cy.get('[data-cy=confirm-bulk-action]').should('be.visible')
      cy.get('[data-cy=confirm-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '批量操作完成')
    })

    it('應可以批量列印出貨標籤', () => {
      cy.get('[data-cy=order-row]').contains('[data-cy=shipping-status]', 'SHIPPED').parent().within(() => {
        cy.get('[data-cy=select-checkbox]').click()
      })

      cy.get('[data-cy=bulk-print-labels-btn]').click()
      cy.get('[data-cy=print-options-modal]').should('be.visible')
      cy.get('[data-cy=label-format-select]').select('A4')
      cy.get('[data-cy=confirm-print-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '標籤已準備列印')
    })
  })

  describe('出貨篩選與搜尋', () => {
    it('應可以按出貨狀態篩選', () => {
      cy.get('[data-cy=shipping-status-filter]').select('待出貨')
      cy.get('[data-cy=apply-filter-btn]').click()

      cy.get('[data-cy=order-row]').each(($row) => {
        cy.wrap($row).find('[data-cy=shipping-status]').should('contain', 'APPROVED')
      })
    })

    it('應可以按付款狀態篩選', () => {
      cy.get('[data-cy=payment-status-filter]').select('已付款')
      cy.get('[data-cy=apply-filter-btn]').click()

      cy.get('[data-cy=order-row]').each(($row) => {
        cy.wrap($row).find('[data-cy=payment-status]').should('contain', 'SUCCESS')
      })
    })

    it('應可以按日期範圍篩選', () => {
      const today = new Date().toISOString().split('T')[0]
      const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString().split('T')[0]

      cy.get('[data-cy=date-from]').type(yesterday)
      cy.get('[data-cy=date-to]').type(today)
      cy.get('[data-cy=apply-filter-btn]').click()

      cy.get('[data-cy=order-row]').should('be.visible')
    })

    it('應可以搜尋特定訂單', () => {
      cy.get('[data-cy=search-input]').type('ORD-001')
      cy.get('[data-cy=search-btn]').click()

      cy.get('[data-cy=order-row]').should('have.length.at.most', 1)
      cy.get('[data-cy=order-row]').first().should('contain', 'ORD-001')
    })

    it('應可以按客戶名稱搜尋', () => {
      cy.get('[data-cy=search-type-select]').select('客戶名稱')
      cy.get('[data-cy=search-input]').type('customer1')
      cy.get('[data-cy=search-btn]').click()

      cy.get('[data-cy=order-row]').each(($row) => {
        cy.wrap($row).find('[data-cy=customer-name]').should('contain', 'customer1')
      })
    })
  })

  describe('出貨詳情管理', () => {
    beforeEach(() => {
      cy.get('[data-cy=order-row]').first().click()
    })

    it('應顯示完整的出貨資訊', () => {
      cy.get('[data-cy=shipping-details-modal]').should('be.visible')
      cy.get('[data-cy=shipping-details-modal]').within(() => {
        cy.get('[data-cy=order-info]').should('be.visible')
        cy.get('[data-cy=customer-info]').should('be.visible')
        cy.get('[data-cy=shipping-address]').should('be.visible')
        cy.get('[data-cy=order-items]').should('be.visible')
        cy.get('[data-cy=shipping-history]').should('be.visible')
      })
    })

    it('應顯示配送地址', () => {
      cy.get('[data-cy=shipping-address]').within(() => {
        cy.get('[data-cy=recipient-name]').should('be.visible')
        cy.get('[data-cy=recipient-phone]').should('be.visible')
        cy.get('[data-cy=delivery-address]').should('be.visible')
        cy.get('[data-cy=postal-code]').should('be.visible')
      })
    })

    it('應顯示出貨歷史記錄', () => {
      cy.get('[data-cy=shipping-history]').should('be.visible')
      cy.get('[data-cy=shipping-event]').should('have.length.at.least', 1)

      cy.get('[data-cy=shipping-event]').first().within(() => {
        cy.get('[data-cy=event-time]').should('be.visible')
        cy.get('[data-cy=event-status]').should('be.visible')
        cy.get('[data-cy=event-description]').should('be.visible')
      })
    })

    it('應可以編輯配送資訊', () => {
      cy.get('[data-cy=edit-shipping-info-btn]').click()
      cy.get('[data-cy=shipping-edit-form]').should('be.visible')

      cy.get('[data-cy=tracking-number-input]').clear().type('NEW123456')
      cy.get('[data-cy=carrier-select]').select('宅急便')
      cy.get('[data-cy=save-shipping-info-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '出貨資訊已更新')
    })

    it('應可以新增配送備註', () => {
      cy.get('[data-cy=add-shipping-note-btn]').click()
      cy.get('[data-cy=shipping-note-input]').type('客戶要求延後配送')
      cy.get('[data-cy=save-note-btn]').click()

      cy.get('[data-cy=shipping-notes]').should('contain', '客戶要求延後配送')
    })
  })

  describe('出貨報表與統計', () => {
    beforeEach(() => {
      cy.visit('/admin/shipping/reports')
    })

    it('應顯示出貨統計數據', () => {
      cy.get('[data-cy=shipping-stats]').should('be.visible')
      cy.get('[data-cy=total-orders]').should('be.visible')
      cy.get('[data-cy=pending-shipments]').should('be.visible')
      cy.get('[data-cy=shipped-today]').should('be.visible')
      cy.get('[data-cy=delivery-rate]').should('be.visible')
    })

    it('應顯示出貨趨勢圖表', () => {
      cy.get('[data-cy=shipping-trend-chart]').should('be.visible')
      cy.get('[data-cy=chart-legend]').should('be.visible')
    })

    it('應可以匯出出貨報表', () => {
      cy.get('[data-cy=export-report-btn]').click()
      cy.get('[data-cy=export-options]').should('be.visible')
      cy.get('[data-cy=export-format-select]').select('Excel')
      cy.get('[data-cy=date-range-select]').select('本月')
      cy.get('[data-cy=confirm-export-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '報表匯出成功')
    })

    it('應顯示配送異常警告', () => {
      cy.get('[data-cy=shipping-alerts]').should('be.visible')
      cy.get('[data-cy=overdue-shipments]').should('be.visible')
      cy.get('[data-cy=delayed-deliveries]').should('be.visible')
    })
  })

  describe('權限控制', () => {
    it('Customer 無法訪問出貨管理頁面', () => {
      cy.logout()
      cy.loginAsCustomer()
      cy.visit('/admin/shipping', { timeout: 10000 })

      cy.url().should('not.include', '/admin/shipping')
      cy.get('[data-cy=access-denied]').should('be.visible')
    })

    it('未登入用戶應被重導向到登入頁', () => {
      cy.logout()
      cy.visit('/admin/shipping')

      cy.url().should('include', '/login')
    })
  })

  describe('即時更新', () => {
    it('出貨狀態更新應即時反映', () => {
      cy.get('[data-cy=order-row]').first().within(() => {
        cy.get('[data-cy=shipping-status]').invoke('text').as('originalStatus')
        cy.get('[data-cy=ship-action-btn]').click()
      })

      cy.get('[data-cy=action-approve]').click()
      cy.get('[data-cy=confirm-btn]').click()

      cy.get('@originalStatus').then((originalStatus) => {
        cy.get('[data-cy=order-row]').first().within(() => {
          cy.get('[data-cy=shipping-status]').should('not.contain', originalStatus)
        })
      })
    })

    it('應顯示其他管理員的操作', () => {
      // 模擬其他管理員的操作
      cy.window().then((win) => {
        win.dispatchEvent(new CustomEvent('shipping-status-updated', {
          detail: { orderId: '123', status: 'SHIPPED' }
        }))
      })

      cy.get('[data-cy=notification-toast]').should('contain', '訂單狀態已由其他管理員更新')
    })
  })
})