/// <reference types="cypress" />

/**
 * 驗收測試：智能客服進階功能
 * 需求來源：INPUT_PROMPT.md - Agentic Chatbot 需求 - 創新功能建議
 */

describe('智能客服進階功能', () => {
  beforeEach(() => {
    cy.loginAsCustomer()
    cy.visit('/dashboard')
    cy.get('[data-cy=chatbot-float-icon]').click()
  })

  describe('情緒感知與人性化回應', () => {
    it('應識別用戶的不滿情緒並給予安撫', () => {
      cy.get('[data-cy=message-input]').type('我很生氣！這個商品品質太差了！')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=emotion-indicator]').should('have.class', 'emotion-angry')
        cy.get('[data-cy=empathy-response]').should('be.visible')
      })

      cy.get('[data-cy=bot-message]').last().should('contain', '理解您的困擾')
      cy.get('[data-cy=bot-message]').last().should('contain', '協助您解決')
    })

    it('應識別用戶的開心情緒並給予積極回應', () => {
      cy.get('[data-cy=message-input]').type('太棒了！我很滿意這次的購物體驗')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=emotion-indicator]').should('have.class', 'emotion-happy')
        cy.get('[data-cy=positive-response]').should('be.visible')
      })

      cy.get('[data-cy=bot-message]').last().should('contain', '很高興聽到')
    })

    it('應識別用戶的困惑情緒並提供詳細說明', () => {
      cy.get('[data-cy=message-input]').type('我不太懂這個退貨流程要怎麼操作...')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=emotion-indicator]').should('have.class', 'emotion-confused')
        cy.get('[data-cy=detailed-explanation]').should('be.visible')
        cy.get('[data-cy=step-by-step-guide]').should('be.visible')
      })
    })

    it('應根據情緒調整回應語調', () => {
      // 測試正式語調（處理投訴）
      cy.get('[data-cy=message-input]').type('我要投訴這個服務！')
      cy.get('[data-cy=send-button]').click()
      cy.get('[data-cy=bot-message]').last().should('have.class', 'tone-formal')

      // 測試友好語調（一般詢問）
      cy.get('[data-cy=message-input]').type('請問有什麼推薦的商品嗎？')
      cy.get('[data-cy=send-button]').click()
      cy.get('[data-cy=bot-message]').last().should('have.class', 'tone-friendly')
    })
  })

  describe('智能知識擴充', () => {
    beforeEach(() => {
      cy.logout()
      cy.loginAsAdmin()
      cy.visit('/admin/chatbot/knowledge')
    })

    it('應可以新增知識庫條目', () => {
      cy.get('[data-cy=add-knowledge-btn]').click()
      cy.get('[data-cy=knowledge-form]').should('be.visible')

      cy.get('[data-cy=question-input]').type('如何申請延長保固？')
      cy.get('[data-cy=answer-input]').type('您可以在購買後30天內申請延長保固...')
      cy.get('[data-cy=category-select]').select('售後服務')
      cy.get('[data-cy=tags-input]').type('保固,延長,申請')

      cy.get('[data-cy=save-knowledge-btn]').click()
      cy.get('[data-cy=success-message]').should('contain', '知識已新增')
    })

    it('應可以編輯現有知識', () => {
      cy.get('[data-cy=knowledge-item]').first().within(() => {
        cy.get('[data-cy=edit-btn]').click()
      })

      cy.get('[data-cy=knowledge-form]').within(() => {
        cy.get('[data-cy=answer-input]').clear()
        cy.get('[data-cy=answer-input]').type('更新後的回答內容...')
        cy.get('[data-cy=save-btn]').click()
      })

      cy.get('[data-cy=success-message]').should('contain', '知識已更新')
    })

    it('應可以從對話中學習新問題', () => {
      // 切換回客戶身分測試
      cy.logout()
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=message-input]').type('什麼是無接觸配送？')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().should('contain', '抱歉，我還不知道')
      cy.get('[data-cy=learn-suggestion]').should('be.visible')

      // 管理員應收到學習建議
      cy.logout()
      cy.loginAsAdmin()
      cy.visit('/admin/chatbot/learning')

      cy.get('[data-cy=learning-suggestion]').should('contain', '什麼是無接觸配送')
      cy.get('[data-cy=add-to-knowledge-btn]').click()

      cy.get('[data-cy=answer-input]').type('無接觸配送是指配送員將商品放置在指定地點...')
      cy.get('[data-cy=save-btn]').click()
    })

    it('應支援批量匯入知識庫', () => {
      cy.get('[data-cy=bulk-import-btn]').click()
      cy.get('[data-cy=file-upload]').attachFile('knowledge-base.csv')

      cy.get('[data-cy=import-preview]').should('be.visible')
      cy.get('[data-cy=import-item]').should('have.length.at.least', 5)

      cy.get('[data-cy=confirm-import-btn]').click()
      cy.get('[data-cy=import-progress]').should('be.visible')
      cy.get('[data-cy=success-message]').should('contain', '批量匯入完成')
    })
  })

  describe('數據分析與回饋機制', () => {
    beforeEach(() => {
      cy.logout()
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()
    })

    it('應可以對回應進行評分', () => {
      cy.get('[data-cy=message-input]').type('如何查詢訂單狀態？')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=feedback-section]').should('be.visible')
        cy.get('[data-cy=thumbs-up]').should('be.visible')
        cy.get('[data-cy=thumbs-down]').should('be.visible')
        cy.get('[data-cy=rating-stars]').should('be.visible')
      })

      cy.get('[data-cy=rating-star-4]').click()
      cy.get('[data-cy=feedback-submitted]').should('be.visible')
    })

    it('應可以提供詳細回饋', () => {
      cy.get('[data-cy=message-input]').type('測試問題')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=thumbs-down]').click()
      })

      cy.get('[data-cy=feedback-modal]').should('be.visible')
      cy.get('[data-cy=feedback-category]').select('回答不準確')
      cy.get('[data-cy=feedback-detail]').type('回答與問題不相符')
      cy.get('[data-cy=submit-feedback-btn]').click()

      cy.get('[data-cy=success-message]').should('contain', '感謝您的回饋')
    })

    it('管理員應可以查看回饋統計', () => {
      cy.logout()
      cy.loginAsAdmin()
      cy.visit('/admin/chatbot/analytics')

      cy.get('[data-cy=feedback-dashboard]').should('be.visible')
      cy.get('[data-cy=satisfaction-score]').should('be.visible')
      cy.get('[data-cy=response-accuracy]').should('be.visible')
      cy.get('[data-cy=user-engagement]').should('be.visible')

      cy.get('[data-cy=feedback-chart]').should('be.visible')
      cy.get('[data-cy=trend-analysis]').should('be.visible')
    })

    it('應顯示回饋改善建議', () => {
      cy.logout()
      cy.loginAsAdmin()
      cy.visit('/admin/chatbot/analytics')

      cy.get('[data-cy=improvement-suggestions]').should('be.visible')
      cy.get('[data-cy=suggestion-item]').should('have.length.at.least', 1)

      cy.get('[data-cy=suggestion-item]').first().within(() => {
        cy.get('[data-cy=suggestion-text]').should('be.visible')
        cy.get('[data-cy=priority-level]').should('be.visible')
        cy.get('[data-cy=implement-btn]').should('be.visible')
      })
    })
  })

  describe('對話品質監控', () => {
    it('應記錄對話品質指標', () => {
      cy.get('[data-cy=message-input]').type('你好')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=message-input]').type('我想查詢訂單')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=message-input]').type('謝謝')
      cy.get('[data-cy=send-button]').click()

      // 對話應被標記為成功完成
      cy.get('[data-cy=conversation-quality]').should('have.class', 'quality-good')
      cy.get('[data-cy=resolution-indicator]').should('contain', '問題已解決')
    })

    it('應識別無效對話並提供改善', () => {
      cy.get('[data-cy=message-input]').type('asdfasdf')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=clarification-request]').should('be.visible')
        cy.get('[data-cy=suggested-questions]').should('be.visible')
      })

      cy.get('[data-cy=bot-message]').last().should('contain', '我沒有理解您的問題')
    })

    it('應追蹤對話完成率', () => {
      cy.logout()
      cy.loginAsAdmin()
      cy.visit('/admin/chatbot/metrics')

      cy.get('[data-cy=completion-rate]').should('be.visible')
      cy.get('[data-cy=resolution-rate]').should('be.visible')
      cy.get('[data-cy=escalation-rate]').should('be.visible')

      cy.get('[data-cy=metrics-chart]').should('be.visible')
    })
  })

  describe('個性化體驗', () => {
    beforeEach(() => {
      cy.logout()
      cy.loginAsCustomer()
      cy.visit('/dashboard')
      cy.get('[data-cy=chatbot-float-icon]').click()
    })

    it('應記住用戶偏好', () => {
      cy.get('[data-cy=message-input]').type('我喜歡蘋果的產品')
      cy.get('[data-cy=send-button]').click()

      // 再次對話時應考慮偏好
      cy.get('[data-cy=message-input]').type('推薦一些手機給我')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().should('contain', 'iPhone')
      cy.get('[data-cy=personalized-recommendations]').should('be.visible')
    })

    it('應根據購買歷史提供建議', () => {
      // 模擬有購買歷史的用戶
      cy.createPaidOrder()

      cy.get('[data-cy=message-input]').type('有什麼新商品推薦嗎？')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=purchase-based-recommendations]').should('be.visible')
        cy.get('[data-cy=recommendation-reason]').should('contain', '根據您的購買歷史')
      })
    })

    it('應調整對話風格', () => {
      // 設定偏好的對話風格
      cy.get('[data-cy=chat-settings]').click()
      cy.get('[data-cy=conversation-style]').select('正式')
      cy.get('[data-cy=save-settings-btn]').click()

      cy.get('[data-cy=message-input]').type('你好')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().should('have.class', 'style-formal')
    })
  })

  describe('多模態交互', () => {
    it('應支援圖片上傳並分析', () => {
      cy.get('[data-cy=attachment-btn]').click()
      cy.get('[data-cy=image-upload]').attachFile('product-image.jpg')

      cy.get('[data-cy=message-input]').type('這個商品有什麼問題？')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=image-analysis]').should('be.visible')
        cy.get('[data-cy=visual-feedback]').should('be.visible')
      })
    })

    it('應支援語音輸入', () => {
      cy.get('[data-cy=voice-input-btn]').click()
      cy.get('[data-cy=voice-recording]').should('be.visible')

      // 模擬語音輸入完成
      cy.window().then((win) => {
        win.dispatchEvent(new CustomEvent('voice-input-complete', {
          detail: { text: '我想查詢訂單狀態' }
        }))
      })

      cy.get('[data-cy=message-input]').should('have.value', '我想查詢訂單狀態')
      cy.get('[data-cy=voice-indicator]').should('be.visible')
    })

    it('應支援語音回應', () => {
      cy.get('[data-cy=chat-settings]').click()
      cy.get('[data-cy=enable-voice-response]').check()
      cy.get('[data-cy=save-settings-btn]').click()

      cy.get('[data-cy=message-input]').type('你好')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().within(() => {
        cy.get('[data-cy=voice-response-btn]').should('be.visible')
        cy.get('[data-cy=audio-player]').should('be.visible')
      })
    })
  })

  describe('效能監控', () => {
    it('應監控回應時間', () => {
      const startTime = Date.now()

      cy.get('[data-cy=message-input]').type('測試回應時間')
      cy.get('[data-cy=send-button]').click()

      cy.get('[data-cy=bot-message]').last().should('be.visible').then(() => {
        const responseTime = Date.now() - startTime
        // 驗證回應時間在 5 秒內
        if (responseTime >= 5000) {
          throw new Error(`Response time ${responseTime}ms exceeded 5000ms limit`)
        }
      })

      cy.get('[data-cy=response-time-indicator]').should('be.visible')
    })

    it('應顯示系統負載狀態', () => {
      cy.logout()
      cy.loginAsAdmin()
      cy.visit('/admin/chatbot/performance')

      cy.get('[data-cy=system-status]').should('be.visible')
      cy.get('[data-cy=load-indicator]').should('be.visible')
      cy.get('[data-cy=response-time-chart]').should('be.visible')
      cy.get('[data-cy=concurrent-users]').should('be.visible')
    })

    it('高負載時應顯示等待提示', () => {
      // 模擬高負載情況
      cy.window().then((win) => {
        win.localStorage.setItem('chatbot-high-load', 'true')
      })

      cy.reload()
      cy.get('[data-cy=chatbot-float-icon]').click()

      cy.get('[data-cy=high-load-warning]').should('be.visible')
      cy.get('[data-cy=estimated-wait-time]').should('be.visible')
    })
  })
})