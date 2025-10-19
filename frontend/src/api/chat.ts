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

export interface SessionDto {
  sessionId: string
  userId: number
  lastMessage: string
  lastMessageTime: number
  hasUnread: boolean
  hasPendingSuggestion: boolean
}

export interface AiSuggestionDto {
  aiResponseId: number
  sessionId: string
  userId: number
  suggestedText: string
  confidence: number
  toolCalls?: ToolCallDto[]
  createdAt: string
}

export interface ToolCallDto {
  toolName: string
  arguments: string
  result: string
  executionTime: number
}

export interface ChatHistory {
  id: number
  sessionId: string
  userId: number
  role: string
  messageType: string
  content: string
  actionType?: string
  actionTarget?: string
  createdAt: string
}

export interface AdminSendRequest {
  sessionId: string
  userId: number
  aiResponseId: number
  text: string
  originalSuggestion?: string
}

export interface FeedbackRequest {
  aiResponseId: number
  feedbackType: 'POSITIVE' | 'NEGATIVE'
  reason?: string
}

export interface AdminDirectMessageRequest {
  sessionId: string
  userId: number
  text: string
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
  },

  // Admin API - Get all chat sessions
  getSessions(): Promise<SessionDto[]> {
    return axios.get('/admin/chat/sessions')
  },

  // Admin API - Get session history
  getSessionHistory(sessionId: string): Promise<ChatHistory[]> {
    return axios.get(`/admin/chat/sessions/${sessionId}/history`)
  },

  // Admin API - Get pending AI suggestions
  getPendingSuggestions(): Promise<AiSuggestionDto[]> {
    return axios.get('/admin/chat/suggestions')
  },

  // Admin API - Approve AI suggestion
  approveSuggestion(request: AdminSendRequest) {
    return axios.post('/admin/chat/approve', request)
  },

  // Admin API - Modify and send AI suggestion
  modifySuggestion(request: AdminSendRequest) {
    return axios.post('/admin/chat/modify', request)
  },

  // Admin API - Reject AI suggestion and send manual message
  rejectSuggestion(request: AdminSendRequest) {
    return axios.post('/admin/chat/reject', request)
  },

  // Admin API - Provide feedback on AI response
  provideFeedback(request: FeedbackRequest) {
    return axios.post('/admin/chat/feedback', request)
  },

  // Admin API - Send direct message to user
  sendDirectMessage(request: AdminDirectMessageRequest) {
    return axios.post('/admin/chat/send', request)
  },

  // Get product by ID
  getProduct(productId: number) {
    return axios.get(`/product/${productId}`)
  },

  // Get order by ID
  getOrder(orderId: number) {
    return axios.get(`/orders/${orderId}`)
  }
}