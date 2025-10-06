/// <reference types="cypress" />

/**
 * 驗收測試：智能客服工具調用功能
 * 需求來源：INPUT_PROMPT.md - Agentic Chatbot 需求 - MCP 工具調用（Tool Calling）功能
 */

describe('智能客服工具調用功能', () => {
  beforeEach(() => {
    cy.loginAsCustomer()
    cy.visit('/dashboard')
    cy.get('[data-cy=chatbot-float-icon]').click()
  })

  describe('工具調用權限控制', () => {
    it('Customer 應只能使用客戶權限內的工具', () => {
      cy.get('[data-cy=message-input]').type('幫我查詢我的訂單')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-call-indicator]').should('be.visible')
        cy.get('[data-cy=tool-name]').should('contain', 'query_customer_orders')
      })

      // Customer 不應能看到管理員工具
      cy.get('[data-cy=message-input]').type('幫我查看所有客戶的訂單')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().should('contain', '抱歉，您沒有權限執行此操作')
    })

    it('Admin 應可以使用所有管理工具', () => {
      cy.logout()
      cy.loginAsAdmin()
      cy.visit('/admin/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=message-input]').type('幫我查看今天的所有訂單')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-call-indicator]').should('be.visible')
        cy.get('[data-cy=tool-name]').should('contain', 'query_all_orders')
      })
    })
  })

  describe('訂單查詢工具', () => {
    beforeEach(() => {
      cy.createPaidOrder().as('orderId')
    })

    it('應可以查詢特定訂單詳情', () => {
      cy.get('@orderId').then((orderId) => {
        cy.get('[data-cy=message-input]').type(`幫我查詢訂單 ${orderId} 的詳細資訊`)
        cy.get('[data-cy=send-button]').click()

        cy.get('[data-cy=bot-message]').last().within(() => {
          cy.get('[data-cy=tool-result]').should('be.visible')
          cy.get('[data-cy=order-details]').should('contain', orderId)
          cy.get('[data-cy=order-status]').should('be.visible')
          cy.get('[data-cy=order-total]').should('be.visible')
        })
      })
    })

    it('應可以查詢訂單出貨狀態', () => {
      cy.get('[data-cy=message-input]').type('我的訂單出貨了嗎？')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-call-indicator]').should('be.visible')
        cy.get('[data-cy=shipping-status]').should('be.visible')
        cy.get('[data-cy=order-list]').should('be.visible')
      })
    })

    it('查詢不存在的訂單應返回適當錯誤', () => {
      cy.get('[data-cy=message-input]').type('幫我查詢訂單 999999')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().should('contain', '找不到該訂單')
      cy.get('[data-cy=error-indicator]').should('be.visible')
    })
  })

  describe('商品查詢工具', () => {
    it('應可以搜尋商品資訊', () => {
      cy.get('[data-cy=message-input]').type('請幫我找手機相關的商品')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-call-indicator]').should('be.visible')
        cy.get('[data-cy=tool-name]').should('contain', 'search_products')
        cy.get('[data-cy=product-list]').should('be.visible')
        cy.get('[data-cy=product-item]').should('have.length.at.least', 1)
      })
    })

    it('應可以查詢商品庫存', () => {
      cy.get('[data-cy=message-input]').type('iPhone 13 還有庫存嗎？')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-result]').should('be.visible')
        cy.get('[data-cy=stock-info]').should('be.visible')
      })
    })

    it('應可以推薦相關商品', () => {
      cy.get('[data-cy=message-input]').type('推薦一些3000元以下的手機')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-call-indicator]').should('be.visible')
        cy.get('[data-cy=product-recommendations]').should('be.visible')
        cy.get('[data-cy=product-item]').each(($item) => {
          cy.wrap($item).find('[data-cy=product-price]').should('be.visible')
        })
      })
    })
  })

  describe('下單輔助工具', () => {
    it('應可以引導用戶完成下單流程', () => {
      cy.get('[data-cy=message-input]').type('我想買 iPhone 13，幫我下單')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-call-indicator]').should('be.visible')
        cy.get('[data-cy=order-assistant]').should('be.visible')
        cy.get('[data-cy=product-selector]').should('be.visible')
      })

      cy.get('[data-cy=select-product-btn]').click()
      cy.get('[data-cy=quantity-input]').type('1')
      cy.get('[data-cy=add-to-cart-btn]').click()

      cy.get('[data-cy=bot-message]').last().should('contain', '已為您添加到購物車')
    })

    it('應可以檢查庫存並提醒用戶', () => {
      cy.get('[data-cy=message-input]').type('我想買 100 台 iPhone 13')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-result]').should('be.visible')
        cy.get('[data-cy=stock-warning]').should('be.visible')
      })

      cy.get('[data-cy=bot-message]').last().should('contain', '庫存不足')
    })

    it('應可以協助填寫訂單資訊', () => {
      cy.addProductToCart('iPhone 13', 1)
      cy.get('[data-cy=message-input]').type('幫我完成結帳')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=checkout-assistant]').should('be.visible')
        cy.get('[data-cy=order-summary]').should('be.visible')
        cy.get('[data-cy=proceed-checkout-btn]').should('be.visible')
      })
    })
  })

  describe('庫存提醒工具', () => {
    it('應可以設定商品到貨提醒', () => {
      cy.get('[data-cy=message-input]').type('請提醒我 iPhone 14 到貨')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-call-indicator]').should('be.visible')
        cy.get('[data-cy=reminder-setup]').should('be.visible')
        cy.get('[data-cy=product-name]').should('contain', 'iPhone 14')
      })

      cy.get('[data-cy=confirm-reminder-btn]').click()
      cy.get('[data-cy=success-message]').should('contain', '提醒設定成功')
    })

    it('應可以設定降價提醒', () => {
      cy.get('[data-cy=message-input]').type('iPhone 13 降到 25000 元時提醒我')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=price-alert-setup]').should('be.visible')
        cy.get('[data-cy=target-price]').should('contain', '25000')
      })
    })
  })

  describe('售後服務工具', () => {
    beforeEach(() => {
      cy.createShippedOrder().as('shippedOrderId')
    })

    it('應可以協助申請退貨', () => {
      cy.get('@shippedOrderId').then((orderId) => {
        cy.get('[data-cy=message-input]').type(`我想要退貨訂單 ${orderId}`)
        cy.get('[data-cy=send-button]').click()

        cy.get('[data-cy=bot-message]').last().within(() => {
          cy.get('[data-cy=tool-call-indicator]').should('be.visible')
          cy.get('[data-cy=return-assistant]').should('be.visible')
          cy.get('[data-cy=return-eligibility]').should('be.visible')
        })

        cy.get('[data-cy=proceed-return-btn]').click()
        cy.get('[data-cy=return-form]').should('be.visible')
      })
    })

    it('應可以查詢退款進度', () => {
      cy.createReturnRequest().then((returnId: any) => {
        cy.get('[data-cy=message-input]').type(`查詢退款申請 ${returnId} 的進度`)
        cy.get('[data-cy=send-button]').click()

        cy.get('[data-cy=bot-message]').last().within(() => {
          cy.get('[data-cy=return-status]').should('be.visible')
          cy.get('[data-cy=refund-timeline]').should('be.visible')
        })
      })
    })

    it('應可以建立客服工單', () => {
      cy.get('[data-cy=message-input]').type('我的商品有瑕疵，需要客服協助')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=support-ticket-form]').should('be.visible')
        cy.get('[data-cy=issue-category]').should('be.visible')
      })

      cy.get('[data-cy=issue-category]').select('商品瑕疵')
      cy.get('[data-cy=issue-description]').type('商品外觀有刮痕')
      cy.get('[data-cy=submit-ticket-btn]').click()

      cy.get('[data-cy=ticket-number]').should('be.visible')
      cy.get('[data-cy=success-message]').should('contain', '工單已建立')
    })
  })

  describe('Admin 管理工具', () => {
    beforeEach(() => {
      cy.logout()
      cy.loginAsAdmin()
      cy.visit('/admin/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()
    })

    it('應可以查看訂單統計', () => {
      cy.get('[data-cy=message-input]').type('今天的訂單統計如何？')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-call-indicator]').should('be.visible')
        cy.get('[data-cy=order-statistics]').should('be.visible')
        cy.get('[data-cy=total-orders]').should('be.visible')
        cy.get('[data-cy=total-revenue]').should('be.visible')
      })
    })

    it('應可以管理商品庫存', () => {
      cy.get('[data-cy=message-input]').type('幫我更新 iPhone 13 的庫存為 50')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-call-indicator]').should('be.visible')
        cy.get('[data-cy=stock-update-confirmation]').should('be.visible')
      })

      cy.get('[data-cy=confirm-stock-update-btn]').click()
      cy.get('[data-cy=success-message]').should('contain', '庫存已更新')
    })

    it('應可以處理出貨請求', () => {
      cy.get('[data-cy=message-input]').type('有哪些訂單需要處理出貨？')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=pending-shipments]').should('be.visible')
        cy.get('[data-cy=order-item]').should('have.length.at.least', 1)
      })

      cy.get('[data-cy=order-item]').first().within(() => {
        cy.get('[data-cy=ship-order-btn]').click()
      })

      cy.get('[data-cy=shipping-form]').should('be.visible')
    })
  })

  describe('工具調用錯誤處理', () => {
    it('應處理工具調用失敗的情況', () => {
      cy.intercept('POST', '/api/chat/tools/**', {
        statusCode: 500,
        body: { message: 'Tool execution failed' }
      }).as('toolError')

      cy.get('[data-cy=message-input]').type('幫我查詢訂單')
      cy.get('[data-cy=send-button]').click()

      cy.wait('@toolError')
      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=error-indicator]').should('be.visible')
        cy.get('[data-cy=retry-btn]').should('be.visible')
      })

      cy.get('[data-cy=bot-message]').last().should('contain', '工具調用失敗')
    })

    it('應提供工具調用重試機制', () => {
      cy.get('[data-cy=message-input]').type('幫我查詢訂單')
      cy.get('[data-cy=send-button]').click()

      // 模擬首次失敗
      cy.intercept('POST', '/api/chat/tools/query_orders', {
        statusCode: 500
      }).as('firstAttempt')

      cy.wait('@firstAttempt')
      cy.get('[data-cy=retry-btn]').should('be.visible')

      // 模擬重試成功
      cy.intercept('POST', '/api/chat/tools/query_orders', {
        fixture: 'order-query-success.json'
      }).as('retryAttempt')

      cy.get('[data-cy=retry-btn]').click()
      cy.wait('@retryAttempt')

      cy.get('[data-cy=tool-result]').should('be.visible')
    })

    it('應記錄工具調用歷史', () => {
      cy.get('[data-cy=message-input]').type('幫我查詢訂單')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=tool-call-history]').should('be.visible')
        cy.get('[data-cy=tool-execution-time]').should('be.visible')
        cy.get('[data-cy=tool-status]').should('contain', 'SUCCESS')
      })
    })
  })

  describe('工具調用追蹤與安全', () => {
    it('應記錄所有工具調用以供審計', () => {
      cy.get('[data-cy=message-input]').type('查詢我的訂單')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=chat-settings]').click()
      cy.get('[data-cy=tool-audit-log]').click()

      cy.get('[data-cy=audit-log]').within(() => {
        cy.get('[data-cy=audit-entry]').should('have.length.at.least', 1)
        cy.get('[data-cy=audit-entry]').first().within(() => {
          cy.get('[data-cy=tool-name]').should('be.visible')
          cy.get('[data-cy=execution-time]').should('be.visible')
          cy.get('[data-cy=user-id]').should('be.visible')
          cy.get('[data-cy=result-status]').should('be.visible')
        })
      })
    })

    it('應驗證工具調用權限', () => {
      // Customer 嘗試調用 Admin 工具
      cy.get('[data-cy=message-input]').type('刪除商品 ID 123')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=permission-error]').should('be.visible')
        cy.get('[data-cy=error-message]').should('contain', '權限不足')
      })

      // 確保危險操作不會被執行
      cy.get('[data-cy=tool-execution-blocked]').should('be.visible')
    })

    it('應限制工具調用頻率', () => {
      // 快速連續調用同一工具
      for (let i = 0; i < 5; i++) {
        cy.get('[data-cy=message-input]').type('查詢訂單')
        cy.get('[data-cy=send-button]').click()
        cy.wait(100)
      }

      cy.get('[data-cy=bot-message]').last().should('contain', '請稍後再試')
      cy.get('[data-cy=rate-limit-warning]').should('be.visible')
    })
  })
})