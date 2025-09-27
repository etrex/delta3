/// <reference types="cypress" />

/**
 * 驗收測試：智能客服對話視窗
 * 需求來源：INPUT_PROMPT.md - Agentic Chatbot 需求 - 前端介面新增智能客服對話視窗
 */

describe('智能客服對話視窗', () => {
  describe('Chatbot 基本介面', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/dashboard')
    })

    it('應在右下角顯示 Chatbot 懸浮圖示', () => {
      cy.get('[data-cy=chatbot-float-icon]').should('be.visible')
      cy.get('[data-cy=chatbot-float-icon]').should('have.css', 'position', 'fixed')
      cy.get('[data-cy=chatbot-float-icon]').should('have.css', 'bottom')
      cy.get('[data-cy=chatbot-float-icon]').should('have.css', 'right')
    })

    it('點擊懸浮圖示應展開對話視窗', () => {
      cy.get('[data-cy=chatbot-float-icon]').click()
      cy.get('[data-cy=chatbot-window]').should('be.visible')
      cy.get('[data-cy=chatbot-window]').should('have.class', 'expanded')
    })

    it('對話視窗應包含必要的元件', () => {
      cy.get('[data-cy=chatbot-float-icon]').click()
      cy.get('[data-cy=chatbot-window]').within(() => {
        cy.get('[data-cy=chatbot-header]').should('be.visible')
        cy.get('[data-cy=chat-messages]').should('be.visible')
        cy.get('[data-cy=message-input]').should('be.visible')
        cy.get('[data-cy=send-button]').should('be.visible')
        cy.get('[data-cy=close-button]').should('be.visible')
      })
    })

    it('應可以關閉對話視窗', () => {
      cy.get('[data-cy=chatbot-float-icon]').click()
      cy.get('[data-cy=close-button]').click()
      cy.get('[data-cy=chatbot-window]').should('not.have.class', 'expanded')
    })

    it('應支援跨平台響應式設計', () => {
      // 桌面版
      cy.viewport(1200, 800)
      cy.get('[data-cy=chatbot-float-icon]').click()
      cy.get('[data-cy=chatbot-window]').should('have.css', 'width').and('match', /400px|30%/)

      // 平板版
      cy.viewport('ipad-2', 'portrait')
      cy.get('[data-cy=chatbot-window]').should('have.css', 'width').and('match', /350px|40%/)

      // 手機版
      cy.viewport('iphone-6', 'portrait')
      cy.get('[data-cy=chatbot-window]').should('have.css', 'width').and('match', /90%|100%/)
    })
  })

  describe('智能歡迎語與主動提示', () => {
    it('首次開啟應顯示歡迎訊息', () => {
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=chat-messages]').within(() => {
        cy.get('[data-cy=welcome-message]').should('be.visible')
        cy.get('[data-cy=welcome-message]').should('contain', '歡迎')
        cy.get('[data-cy=suggested-questions]').should('be.visible')
      })
    })

    it('應根據時間顯示不同的歡迎語', () => {
      const now = new Date()
      const hour = now.getHours()
      let expectedGreeting = '您好'

      if (hour < 12) expectedGreeting = '早安'
      else if (hour < 18) expectedGreeting = '午安'
      else expectedGreeting = '晚安'

      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=welcome-message]').should('contain', expectedGreeting)
    })

    it('應根據用戶行為主動推送建議', () => {
      cy.loginAsCustomer()
      cy.visit('/products')
      cy.wait(5000) // 模擬停留時間

      cy.get('[data-cy=chatbot-float-icon]').should('have.class', 'has-suggestion')
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=proactive-suggestion]').should('be.visible')
      cy.get('[data-cy=proactive-suggestion]').should('contain', '需要幫助找商品嗎')
    })

    it('應顯示常見問題建議', () => {
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=suggested-questions]').should('be.visible')
      cy.get('[data-cy=suggestion-button]').should('have.length.at.least', 3)
      cy.get('[data-cy=suggestion-button]').should('contain', '如何下單')
      cy.get('[data-cy=suggestion-button]').should('contain', '查詢訂單狀態')
      cy.get('[data-cy=suggestion-button]').should('contain', '聯絡客服')
    })
  })

  describe('基本對話功能', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()
    })

    it('應可以輸入問題並發送', () => {
      cy.get('[data-cy=message-input]').type('你好，我想查詢訂單狀態')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=chat-messages]').within(() => {
        cy.get('[data-cy=user-message]').last().should('contain', '你好，我想查詢訂單狀態')
        cy.get('[data-cy=typing-indicator]').should('be.visible')
      })
    })

    it('應可以使用 Enter 鍵發送訊息', () => {
      cy.get('[data-cy=message-input]').type('測試訊息{enter}')

      cy.get('[data-cy=user-message]').last().should('contain', '測試訊息')
    })

    it('應即時獲得智能客服回應', () => {
      cy.intercept('POST', '/api/chat', {
        fixture: 'chatbot-response.json'
      }).as('chatResponse')

      cy.get('[data-cy=message-input]').type('你好')
      cy.get('[data-cy=send-button]').click()

      cy.wait('@chatResponse')
      cy.get('[data-cy=bot-message]').last().should('be.visible')
      cy.get('[data-cy=bot-message]').last().should('not.be.empty')
    })

    it('應支援上下文理解，能追蹤多輪對話脈絡', () => {
      // 第一輪對話
      cy.get('[data-cy=message-input]').type('我想買手機')
      cy.get('[data-cy=send-button]').click()
      cy.wait(1000)

      // 第二輪對話（應理解上下文）
      cy.get('[data-cy=message-input]').type('價格在3000元以下的有哪些？')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().should('contain', '手機')
      cy.get('[data-cy=bot-message]').last().should('contain', '3000')
    })

    it('點擊建議問題應自動填入', () => {
      cy.get('[data-cy=suggestion-button]').first().click()
      cy.get('[data-cy=user-message]').should('have.length.at.least', 1)
    })
  })

  describe('多語言支援', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()
    })

    it('應自動偵測用戶語言', () => {
      cy.get('[data-cy=message-input]').type('Hello, how are you?')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=language-indicator]').should('contain', 'EN')
      cy.get('[data-cy=bot-message]').last().should('match', /[a-zA-Z]/)
    })

    it('應可以手動切換語言', () => {
      cy.get('[data-cy=language-switcher]').click()
      cy.get('[data-cy=language-option-en]').click()

      cy.get('[data-cy=chatbot-header]').should('contain', 'Customer Service')
      cy.get('[data-cy=message-input]').should('have.attr', 'placeholder').and('contain', 'Type a message')
    })

    it('應支援中、英、日等多國語言', () => {
      const languages = ['zh-TW', 'en-US', 'ja-JP']

      languages.forEach((lang) => {
        cy.get('[data-cy=language-switcher]').click()
        cy.get(`[data-cy=language-option-${lang}]`).click()
        cy.get('[data-cy=language-indicator]').should('contain', lang.split('-')[0].toUpperCase())
      })
    })
  })

  describe('用戶身分自動調整功能', () => {
    it('Customer 應看到客戶相關功能', () => {
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=suggested-questions]').should('contain', '查詢我的訂單')
      cy.get('[data-cy=suggested-questions]').should('contain', '如何下單')
      cy.get('[data-cy=suggested-questions]').should('contain', '退換貨政策')
      cy.get('[data-cy=suggested-questions]').should('not.contain', '管理商品')
    })

    it('Admin 應看到管理相關功能', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=suggested-questions]').should('contain', '訂單管理')
      cy.get('[data-cy=suggested-questions]').should('contain', '商品管理')
      cy.get('[data-cy=suggested-questions]').should('contain', '出貨管理')
      cy.get('[data-cy=suggested-questions]').should('contain', '系統設定')
    })

    it('應根據身分限制可用工具範圍', () => {
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=message-input]').type('幫我查看所有客戶的訂單')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().should('contain', '抱歉，您沒有權限')
    })
  })

  describe('聊天記錄管理', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()
    })

    it('應保存對話歷史', () => {
      cy.get('[data-cy=message-input]').type('測試訊息1')
      cy.get('[data-cy=send-button]').click()
      cy.wait(1000)

      cy.get('[data-cy=close-button]').click()
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=user-message]').should('contain', '測試訊息1')
    })

    it('應可以清除對話記錄', () => {
      cy.get('[data-cy=message-input]').type('測試訊息')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=chat-settings]').click()
      cy.get('[data-cy=clear-history-btn]').click()
      cy.get('[data-cy=confirm-clear-btn]').click()

      cy.get('[data-cy=chat-messages]').should('not.contain', '測試訊息')
      cy.get('[data-cy=welcome-message]').should('be.visible')
    })

    it('應可以匯出對話記錄', () => {
      cy.get('[data-cy=message-input]').type('測試訊息')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=chat-settings]').click()
      cy.get('[data-cy=export-history-btn]').click()

      cy.get('[data-cy=export-format-select]').select('PDF')
      cy.get('[data-cy=confirm-export-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '對話記錄匯出成功')
    })
  })

  describe('聊天視窗狀態管理', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/dashboard')
    })

    it('應記住視窗展開/收合狀態', () => {
      cy.get('[data-cy=chatbot-float-icon]').click()
      cy.reload()

      cy.get('[data-cy=chatbot-window]').should('have.class', 'expanded')
    })

    it('應可以最小化到托盤', () => {
      cy.get('[data-cy=chatbot-float-icon]').click()
      cy.get('[data-cy=minimize-button]').click()

      cy.get('[data-cy=chatbot-window]').should('have.class', 'minimized')
      cy.get('[data-cy=chatbot-float-icon]').should('have.class', 'has-unread')
    })

    it('有新訊息時應顯示通知徽章', () => {
      cy.get('[data-cy=chatbot-float-icon]').click()
      cy.get('[data-cy=minimize-button]').click()

      // 模擬收到新訊息
      cy.window().then((win) => {
        win.dispatchEvent(new CustomEvent('new-chat-message', {
          detail: { message: '您有新的優惠券可以使用' }
        }))
      })

      cy.get('[data-cy=chatbot-float-icon]').should('have.class', 'has-notification')
      cy.get('[data-cy=notification-badge]').should('be.visible')
    })
  })

  describe('錯誤處理', () => {
    beforeEach(() => {
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()
    })

    it('網路錯誤時應顯示適當訊息', () => {
      cy.intercept('POST', '/api/chat', { forceNetworkError: true }).as('networkError')

      cy.get('[data-cy=message-input]').type('測試訊息')
      cy.get('[data-cy=send-button]').click()

      cy.wait('@networkError')
      cy.get('[data-cy=error-message]').should('contain', '網路連線異常')
      cy.get('[data-cy=retry-button]').should('be.visible')
    })

    it('服務器錯誤時應顯示重試選項', () => {
      cy.intercept('POST', '/api/chat', { statusCode: 500 }).as('serverError')

      cy.get('[data-cy=message-input]').type('測試訊息')
      cy.get('[data-cy=send-button]').click()

      cy.wait('@serverError')
      cy.get('[data-cy=error-message]').should('contain', '服務暫時不可用')
      cy.get('[data-cy=retry-button]').click()
    })

    it('長時間無回應應顯示超時提示', () => {
      cy.intercept('POST', '/api/chat', (req: any) => {
        req.reply((res: any) => {
          res.delay(10000) // 10秒延遲
        })
      }).as('slowResponse')

      cy.get('[data-cy=message-input]').type('測試訊息')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=timeout-warning]').should('be.visible')
      cy.get('[data-cy=cancel-request-btn]').should('be.visible')
    })
  })
})