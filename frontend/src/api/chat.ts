/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import axios from './axios'

export interface PageContext {
  path?: string
  title?: string
  pageType?: string
  data?: any
}

export default {
  // Customer chat with page context
  sendMessage(message: string, pageContext?: PageContext) {
    return axios.post('/chat', {
      message,
      pageContext: pageContext || {}
    })
  },

  // Admin chat with page context
  sendAdminMessage(message: string, pageContext?: PageContext) {
    return axios.post('/chat/admin', {
      message,
      pageContext: pageContext || {}
    })
  },

  // Get chat history
  getHistory(sessionId: string) {
    return axios.get('/chat/history', { params: { sessionId } })
  }
}