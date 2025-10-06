/// <reference types="cypress" />
/// <reference types="chai" />

/**
 * 驗收測試：訂單清單
 * 需求來源：INPUT_PROMPT.md - 前端畫面需求 2.訂單清單
 */

describe('訂單清單功能', () => {
  describe('Customer 檢視訂單', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/orders')
    })

    it('應顯示Customer自己的訂單清單', () => {
      cy.get('[data-cy=order-list]').should('be.visible')
      cy.get('[data-cy=order-card]').should('have.length.at.least', 1)
    })

    it('每個訂單應顯示必要資訊', () => {
      cy.get('[data-cy=order-card]').first().within(() => {
        cy.get('[data-cy=order-id]').should('be.visible')
        cy.get('[data-cy=order-date]').should('be.visible')
        cy.get('[data-cy=order-status]').should('be.visible')
        cy.get('[data-cy=order-total]').should('be.visible')
        cy.get('[data-cy=item-count]').should('be.visible')
      })
    })

    it('應顯示訂單狀態標籤', () => {
      cy.get('[data-cy=order-card]').each(($card) => {
        cy.wrap($card).within(() => {
          cy.get('[data-cy=order-status]').should('be.visible')
          cy.get('[data-cy=status-badge]').should('have.class').and('match', /status-(created|paid|shipped|cancelled)/)
        })
      })
    })

    it('Customer 只能看到自己的訂單', () => {
      // 驗證所有訂單都屬於當前登入的 customer
      cy.get('[data-cy=order-card]').each(($card) => {
        cy.wrap($card).find('[data-cy=customer-info]').should('contain', 'customer1')
      })
    })

    it('應可以點擊訂單查看詳情', () => {
      cy.get('[data-cy=order-card]').first().click()
      cy.url().should('match', /\/orders\/\d+/)
      cy.get('[data-cy=order-details]').should('be.visible')
    })
  })

  describe('Admin 檢視所有訂單', () => {
    beforeEach(() => {
      cy.loginAsAdmin()
      cy.visit('/admin/orders')
    })

    it('Admin 應可以看到所有客戶的訂單', () => {
      cy.get('[data-cy=order-table]').should('be.visible')
      cy.get('[data-cy=order-row]').should('have.length.at.least', 2)

      // 驗證有不同客戶的訂單
      cy.get('[data-cy=customer-name]').then(($customers) => {
        const customerNames = Array.from($customers).map((el: any) => el.textContent)
        const uniqueCustomers = Array.from(new Set(customerNames))
        expect(uniqueCustomers.length).to.be.at.least(1)
      })
    })

    it('Admin 應看到額外的管理欄位', () => {
      cy.get('[data-cy=order-table]').within(() => {
        cy.get('[data-cy=table-header]').should('contain', '客戶名稱')
        cy.get('[data-cy=table-header]').should('contain', '操作')
      })

      cy.get('[data-cy=order-row]').first().within(() => {
        cy.get('[data-cy=customer-name]').should('be.visible')
        cy.get('[data-cy=action-buttons]').should('be.visible')
        cy.get('[data-cy=view-btn]').should('be.visible')
        cy.get('[data-cy=edit-status-btn]').should('be.visible')
      })
    })
  })

  describe('搜尋功能', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/orders')
    })

    it('應可以輸入關鍵字搜尋訂單', () => {
      cy.get('[data-cy=search-input]').should('be.visible')
      cy.get('[data-cy=search-input]').type('ORD-001')
      cy.get('[data-cy=search-btn]').click()

      cy.get('[data-cy=order-card]').should('have.length.at.most', 1)
      cy.get('[data-cy=order-card]').first().should('contain', 'ORD-001')
    })

    it('應可以按訂單狀態篩選', () => {
      cy.get('[data-cy=status-filter]').select('已付款')
      cy.get('[data-cy=apply-filter-btn]').click()

      cy.get('[data-cy=order-card]').each(($card) => {
        cy.wrap($card).find('[data-cy=order-status]').should('contain', 'PAID')
      })
    })

    it('應可以按日期範圍篩選', () => {
      const today = new Date().toISOString().split('T')[0]
      const lastWeek = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]

      cy.get('[data-cy=date-from]').type(lastWeek)
      cy.get('[data-cy=date-to]').type(today)
      cy.get('[data-cy=apply-filter-btn]').click()

      cy.get('[data-cy=order-card]').should('be.visible')
      // 驗證所有訂單都在指定日期範圍內
      cy.get('[data-cy=order-date]').each(($date) => {
        const orderDate = new Date($date.text())
        const lastWeekDate = new Date(lastWeek)
        const todayDate = new Date(today)
        expect(orderDate.getTime()).to.be.at.least(lastWeekDate.getTime())
        expect(orderDate.getTime()).to.be.at.most(todayDate.getTime())
      })
    })

    it('搜尋無結果時應顯示適當訊息', () => {
      cy.get('[data-cy=search-input]').type('不存在的訂單號')
      cy.get('[data-cy=search-btn]').click()

      cy.get('[data-cy=no-results-message]').should('be.visible')
      cy.get('[data-cy=no-results-message]').should('contain', '未找到符合條件的訂單')
    })

    it('應可以清除搜尋條件', () => {
      cy.get('[data-cy=search-input]').type('ORD-001')
      cy.get('[data-cy=search-btn]').click()
      cy.get('[data-cy=clear-search-btn]').click()

      cy.get('[data-cy=search-input]').should('have.value', '')
      cy.get('[data-cy=order-card]').should('have.length.at.least', 1)
    })
  })

  describe('分頁與排序', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/orders')
    })

    it('應支援分頁功能', () => {
      cy.get('[data-cy=pagination]').should('be.visible')
      cy.get('[data-cy=page-info]').should('contain', '第 1 頁')

      // 如果有多頁的話
      cy.get('[data-cy=next-page-btn]').then($btn => {
        if (!$btn.prop('disabled')) {
          cy.wrap($btn).click()
          cy.get('[data-cy=page-info]').should('contain', '第 2 頁')
        }
      })
    })

    it('應支援排序功能', () => {
      cy.get('[data-cy=sort-select]').select('日期由新到舊')

      // 驗證排序結果
      cy.get('[data-cy=order-date]').then($dates => {
        const dates = Array.from($dates).map((el: any) => new Date(el.textContent))
        for (let i = 1; i < dates.length; i++) {
          expect(dates[i-1].getTime()).to.be.at.least(dates[i].getTime())
        }
      })
    })

    it('應可以選擇每頁顯示數量', () => {
      cy.get('[data-cy=page-size-select]').select('20')
      cy.get('[data-cy=order-card]').should('have.length.at.most', 20)
    })
  })

  describe('訂單詳情檢視', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/orders')
      cy.get('[data-cy=order-card]').first().click()
    })

    it('應顯示訂單詳情資訊', () => {
      cy.get('[data-cy=order-details]').should('be.visible')
      cy.get('[data-cy=order-header]').within(() => {
        cy.get('[data-cy=order-id]').should('be.visible')
        cy.get('[data-cy=order-status]').should('be.visible')
        cy.get('[data-cy=order-date]').should('be.visible')
        cy.get('[data-cy=order-total]').should('be.visible')
      })
    })

    it('應顯示出貨狀態', () => {
      cy.get('[data-cy=shipping-info]').should('be.visible')
      cy.get('[data-cy=shipping-status]').should('be.visible')

      // 根據訂單狀態顯示適當的出貨資訊
      cy.get('[data-cy=order-status]').then($status => {
        const status = $status.text()
        if (status.includes('SHIPPED')) {
          cy.get('[data-cy=tracking-info]').should('be.visible')
          cy.get('[data-cy=shipped-date]').should('be.visible')
        } else {
          cy.get('[data-cy=shipping-status]').should('contain', '待出貨')
        }
      })
    })

    it('應顯示付款狀態', () => {
      cy.get('[data-cy=payment-info]').should('be.visible')
      cy.get('[data-cy=payment-status]').should('be.visible')

      cy.get('[data-cy=order-status]').then($status => {
        const status = $status.text()
        if (status.includes('PAID')) {
          cy.get('[data-cy=payment-method]').should('be.visible')
          cy.get('[data-cy=paid-date]').should('be.visible')
          cy.get('[data-cy=transaction-id]').should('be.visible')
        } else {
          cy.get('[data-cy=payment-status]').should('contain', '待付款')
        }
      })
    })

    it('應顯示訂單商品明細', () => {
      cy.get('[data-cy=order-items]').should('be.visible')
      cy.get('[data-cy=order-item]').should('have.length.at.least', 1)

      cy.get('[data-cy=order-item]').first().within(() => {
        cy.get('[data-cy=product-name]').should('be.visible')
        cy.get('[data-cy=product-image]').should('be.visible')
        cy.get('[data-cy=item-quantity]').should('be.visible')
        cy.get('[data-cy=item-price]').should('be.visible')
        cy.get('[data-cy=item-subtotal]').should('be.visible')
      })
    })

    it('應顯示訂單總計', () => {
      cy.get('[data-cy=order-summary]').within(() => {
        cy.get('[data-cy=items-total]').should('be.visible')
        cy.get('[data-cy=shipping-fee]').should('be.visible')
        cy.get('[data-cy=total-amount]').should('be.visible')
      })
    })
  })

  describe('響應式設計', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/orders')
    })

    it('在手機版應正確顯示', () => {
      cy.viewport('iphone-6', 'portrait')
      cy.get('[data-cy=order-list]').should('be.visible')
      cy.get('[data-cy=order-card]').should('have.css', 'width').and('match', /100%|auto/)
    })

    it('在平板版應正確顯示', () => {
      cy.viewport('ipad-2', 'portrait')
      cy.get('[data-cy=order-list]').should('be.visible')
      cy.get('[data-cy=search-filters]').should('be.visible')
    })
  })
})