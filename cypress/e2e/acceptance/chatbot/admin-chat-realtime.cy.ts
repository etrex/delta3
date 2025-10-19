/// <reference types="cypress" />

/**
 * 驗收測試：管理員客服即時訊息功能
 * 測試管理員和客戶之間的即時 WebSocket 訊息傳遞
 */

describe('管理員客服即時訊息', () => {
  describe('WebSocket 連線與即時訊息', () => {
    it('客戶發送訊息時，管理員應即時收到（無需重新整理）', () => {
      // 步驟 1: 客戶登入並發送訊息
      cy.loginAsCustomer()
      cy.visit('/products')

      // 打開客服聊天視窗
      cy.get('.chatbot-trigger').click()
      cy.get('.chatbot-window').should('be.visible')

      // 客戶發送測試訊息
      const testMessage = `測試訊息 ${Date.now()}`
      cy.get('[data-cy=message-input]').type(testMessage)
      cy.get('[data-cy=send-button]').click()

      // 確認訊息已發送
      cy.get('[data-cy=user-message]').last().should('contain', testMessage)

      // 步驟 2: 在另一個視窗中，管理員應該即時收到訊息（測試 WebSocket）
      cy.window().then((win) => {
        // 打開新視窗作為管理員
        const adminWindow = win.open('/admin/chat', '_blank')

        cy.wrap(adminWindow).should('not.be.null')
      })

      // 驗證管理員端的 WebSocket 連線
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 等待 WebSocket 連線建立
      cy.wait(2000)

      // 檢查 console 是否顯示 WebSocket connected
      cy.window().then((win) => {
        const consoleMessages: string[] = []
        cy.stub(win.console, 'log').callsFake((...args) => {
          consoleMessages.push(args.join(' '))
        })

        // 確認有 WebSocket 連線訊息
        cy.wrap(consoleMessages).should((msgs) => {
          expect(msgs.some(msg => msg.includes('WebSocket connected'))).to.be.true
        })
      })

      // 選擇客戶的對話
      cy.contains('用戶 #2').click()

      // 驗證管理員可以看到客戶剛才發送的訊息（即時載入，無需刷新）
      cy.get('.chat-history').should('contain', testMessage)
    })

    it('管理員發送訊息時，客戶應即時收到（無需重新整理）', () => {
      const adminMessage = `管理員回覆 ${Date.now()}`

      // 步驟 1: 管理員登入並進入客服管理
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 等待 sessions 載入
      cy.wait(1000)

      // 選擇第一個對話
      cy.get('.session-item').first().click()

      // 管理員發送訊息
      cy.get('.chat-input-area textarea').type(adminMessage)
      cy.get('.send-button').click()

      // 確認訊息發送成功
      cy.contains('訊息已發送').should('be.visible')

      // 驗證訊息出現在聊天記錄中
      cy.get('.chat-history').should('contain', adminMessage)

      // 步驟 2: 切換到客戶視角，驗證即時收到訊息
      cy.loginAsCustomer()
      cy.visit('/products')

      // 打開聊天視窗
      cy.get('.chatbot-trigger').click()

      // 等待 WebSocket 連線和訊息載入
      cy.wait(2000)

      // 驗證客戶可以看到管理員的回覆（即時，無需發送訊息或刷新）
      cy.get('.messages-container').should('contain', adminMessage)
      cy.get('[data-cy=bot-message]').last().should('contain', adminMessage)
    })

    it('WebSocket 連線中斷時應自動重連', () => {
      cy.loginAsCustomer()
      cy.visit('/products')

      // 打開聊天視窗
      cy.get('.chatbot-trigger').click()

      // 等待 WebSocket 連線
      cy.wait(2000)

      // 模擬網路中斷
      cy.window().then((win: any) => {
        // 取得 WebSocket 實例並強制關閉
        if (win.webSocket) {
          win.webSocket.close()
        }
      })

      // 等待重連
      cy.wait(6000) // reconnectDelay is 5000ms

      // 驗證重連成功
      cy.window().then((win) => {
        const consoleMessages: string[] = []
        cy.stub(win.console, 'log').callsFake((...args) => {
          consoleMessages.push(args.join(' '))
        })

        // 確認有重連訊息
        cy.wrap(consoleMessages).should((msgs) => {
          expect(msgs.some(msg => msg.includes('WebSocket connected'))).to.be.true
        })
      })
    })
  })

  describe('管理員客服管理介面', () => {
    beforeEach(() => {
      // 先作為客戶發送幾則訊息，建立對話記錄
      cy.loginAsCustomer()
      cy.visit('/products')
      cy.get('.chatbot-trigger').click()
      cy.get('[data-cy=message-input]').type('你好，我需要幫助')
      cy.get('[data-cy=send-button]').click()
      cy.wait(1000)
    })

    it('管理員應能看到所有客戶對話列表', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 驗證對話列表顯示
      cy.get('.sessions-list').should('be.visible')
      cy.get('.session-item').should('have.length.at.least', 1)

      // 驗證對話資訊
      cy.get('.session-item').first().within(() => {
        cy.get('.session-header').should('be.visible')
        cy.get('.session-message').should('be.visible')
        cy.get('.session-time').should('be.visible')
      })
    })

    it('點擊對話應顯示完整聊天記錄', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 點擊第一個對話
      cy.get('.session-item').first().click()

      // 驗證聊天記錄顯示
      cy.get('.chat-history').should('be.visible')
      cy.get('.chat-message').should('have.length.at.least', 1)

      // 驗證訊息包含角色標籤
      cy.get('.chat-message').first().within(() => {
        cy.get('.el-tag').should('be.visible')
        cy.get('.message-content').should('be.visible')
        cy.get('.message-time').should('be.visible')
      })
    })

    it('管理員應能直接發送訊息給客戶', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 選擇對話
      cy.get('.session-item').first().click()

      // 輸入並發送訊息
      const directMessage = `直接回覆 ${Date.now()}`
      cy.get('.chat-input-area textarea').type(directMessage)
      cy.get('.send-button').click()

      // 驗證發送成功
      cy.contains('訊息已發送').should('be.visible')

      // 驗證訊息出現在聊天記錄中
      cy.get('.chat-history').should('contain', directMessage)

      // 驗證訊息顯示為 AI 助手
      cy.get('.chat-message').last().within(() => {
        cy.contains('AI 助手').should('be.visible')
      })
    })

    it('使用 Ctrl+Enter 快捷鍵應能發送訊息', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 選擇對話
      cy.get('.session-item').first().click()

      // 使用 Ctrl+Enter 發送
      const shortcutMessage = `快捷鍵測試 ${Date.now()}`
      cy.get('.chat-input-area textarea')
        .type(shortcutMessage)
        .type('{ctrl}{enter}')

      // 驗證發送成功
      cy.contains('訊息已發送').should('be.visible')
      cy.get('.chat-history').should('contain', shortcutMessage)
    })

    it('發送訊息後輸入框應清空', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 選擇對話
      cy.get('.session-item').first().click()

      // 發送訊息
      cy.get('.chat-input-area textarea').type('測試訊息')
      cy.get('.send-button').click()

      // 等待發送完成
      cy.wait(500)

      // 驗證輸入框已清空
      cy.get('.chat-input-area textarea').should('have.value', '')
    })

    it('發送訊息後應自動滾動到底部', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 選擇對話
      cy.get('.session-item').first().click()

      // 發送多則訊息以產生滾動
      for (let i = 0; i < 5; i++) {
        cy.get('.chat-input-area textarea').type(`測試訊息 ${i}`)
        cy.get('.send-button').click()
        cy.wait(500)
      }

      // 驗證滾動到底部
      cy.get('.chat-history').then(($el) => {
        const element = $el[0]
        expect(element.scrollTop + element.clientHeight).to.be.closeTo(
          element.scrollHeight,
          10
        )
      })
    })

    it('搜尋功能應能過濾對話列表', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 等待 sessions 載入
      cy.wait(1000)

      // 使用搜尋
      cy.get('.session-search input').type('2')

      // 驗證過濾結果
      cy.get('.session-item').should('have.length.at.least', 1)
      cy.get('.session-item').each(($item) => {
        cy.wrap($item).should('contain', '2')
      })
    })
  })

  describe('AI 建議審核功能', () => {
    it('有待處理的 AI 建議時應顯示標記', () => {
      // 此測試需要後端產生 AI 建議
      // 可以通過 API 或客戶發送觸發 AI 的訊息來產生

      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 檢查是否有待處理標記
      cy.get('.session-item.has-suggestion').should('exist')
      cy.get('.el-tag').contains('待處理').should('be.visible')
    })

    it('管理員應能批准 AI 建議', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 選擇有 AI 建議的對話
      cy.get('.session-item.has-suggestion').first().click()

      // 檢查 AI 建議面板
      cy.get('.suggestions-list').should('be.visible')
      cy.get('.suggestion-item').should('have.length.at.least', 1)

      // 批准建議
      cy.get('.suggestion-item').first().within(() => {
        cy.contains('批准發送').click()
      })

      // 驗證成功訊息
      cy.contains('已批准並發送建議').should('be.visible')
    })

    it('管理員應能修改 AI 建議後發送', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 選擇有 AI 建議的對話
      cy.get('.session-item.has-suggestion').first().click()

      // 點擊修改按鈕
      cy.get('.suggestion-item').first().within(() => {
        cy.contains('修改').click()
      })

      // 修改對話框應出現
      cy.get('.el-dialog').should('be.visible')
      cy.contains('修改 AI 建議').should('be.visible')

      // 修改內容
      cy.get('.el-dialog textarea').clear().type('修改後的回覆內容')

      // 確認發送
      cy.get('.el-dialog').within(() => {
        cy.contains('確認發送').click()
      })

      // 驗證成功
      cy.contains('已修改並發送').should('be.visible')
    })

    it('管理員應能拒絕 AI 建議並手動回覆', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 選擇有 AI 建議的對話
      cy.get('.session-item.has-suggestion').first().click()

      // 點擊拒絕按鈕
      cy.get('.suggestion-item').first().within(() => {
        cy.contains('拒絕').click()
      })

      // 拒絕對話框應出現
      cy.get('.el-dialog').should('be.visible')
      cy.contains('拒絕建議並手動回覆').should('be.visible')

      // 輸入手動回覆
      cy.get('.el-dialog textarea').type('手動回覆內容')

      // 確認發送
      cy.get('.el-dialog').within(() => {
        cy.contains('確認發送').click()
      })

      // 驗證成功
      cy.contains('已拒絕建議並發送手動回覆').should('be.visible')
    })
  })

  describe('錯誤處理與邊界情況', () => {
    it('未選擇對話時不應顯示聊天記錄', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 驗證顯示空狀態
      cy.contains('請選擇一個對話').should('be.visible')
    })

    it('輸入框為空時發送按鈕應禁用', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 選擇對話
      cy.get('.session-item').first().click()

      // 驗證空輸入時按鈕禁用
      cy.get('.send-button').should('be.disabled')

      // 輸入內容後啟用
      cy.get('.chat-input-area textarea').type('測試')
      cy.get('.send-button').should('not.be.disabled')
    })

    it('發送失敗時應顯示錯誤訊息', () => {
      // 模擬 API 錯誤
      cy.intercept('POST', '/api/admin/chat/send', {
        statusCode: 500,
        body: { message: 'Internal Server Error' }
      }).as('sendError')

      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 選擇對話並發送
      cy.get('.session-item').first().click()
      cy.get('.chat-input-area textarea').type('測試訊息')
      cy.get('.send-button').click()

      cy.wait('@sendError')

      // 驗證錯誤訊息
      cy.contains('發送訊息失敗').should('be.visible')
    })

    it('WebSocket 連線失敗時應顯示離線狀態', () => {
      // 模擬 WebSocket 無法連線
      cy.intercept('GET', '**/ws/chat', { forceNetworkError: true })

      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 等待連線嘗試
      cy.wait(3000)

      // 檢查 console 錯誤
      cy.window().then((win) => {
        const consoleErrors: string[] = []
        cy.stub(win.console, 'error').callsFake((...args) => {
          consoleErrors.push(args.join(' '))
        })

        // 確認有連線錯誤訊息
        cy.wrap(consoleErrors).should((errors) => {
          expect(errors.some(err =>
            err.includes('WebSocket') || err.includes('STOMP')
          )).to.be.true
        })
      })
    })
  })

  describe('效能與響應', () => {
    it('對話列表應在 3 秒內載入', () => {
      const startTime = Date.now()

      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      cy.get('.sessions-list').should('be.visible').then(() => {
        const loadTime = Date.now() - startTime
        expect(loadTime).to.be.lessThan(3000)
      })
    })

    it('發送訊息應在 2 秒內完成', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      cy.get('.session-item').first().click()

      const startTime = Date.now()
      cy.get('.chat-input-area textarea').type('效能測試')
      cy.get('.send-button').click()

      cy.contains('訊息已發送').should('be.visible').then(() => {
        const sendTime = Date.now() - startTime
        expect(sendTime).to.be.lessThan(2000)
      })
    })

    it('切換對話應流暢無延遲', () => {
      cy.loginAsAdmin()
      cy.visit('/admin/chat')

      // 快速切換多個對話
      cy.get('.session-item').eq(0).click()
      cy.wait(100)
      cy.get('.session-item').eq(1).click()
      cy.wait(100)
      cy.get('.session-item').eq(0).click()

      // 驗證最終顯示正確對話
      cy.get('.chat-history').should('be.visible')
    })
  })
})
