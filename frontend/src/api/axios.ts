/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'

const API_BASE_URL = 'http://localhost:8080/api'

const instance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 300000, // 300 seconds for AI chat operations
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
instance.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }

    // Add chat session ID for tracking
    try {
      const chatStore = useChatStore()
      if (chatStore && chatStore.sessionId) {
        config.headers['X-Chat-Session-Id'] = chatStore.sessionId
      }
    } catch (e) {
      // Store might not be initialized yet, skip
      console.debug('Chat store not initialized yet')
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor
instance.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      if (error.response.status === 401) {
        const authStore = useAuthStore()
        authStore.logout()
        window.location.href = '/login'
      }
      // Don't show error message here, let the calling code handle it
      // to avoid duplicate error messages
    } else {
      ElMessage.error('網路錯誤')
    }
    return Promise.reject(error)
  }
)

export default instance