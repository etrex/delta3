/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'

export const useChatStore = defineStore('chat', () => {
  const authStore = useAuthStore()

  // Generate session ID once per app session
  const sessionId = ref(`${authStore.user?.id || 'guest'}-${Date.now()}`)

  const getSessionId = () => {
    return sessionId.value
  }

  const resetSessionId = () => {
    sessionId.value = `${authStore.user?.id || 'guest'}-${Date.now()}`
  }

  return {
    sessionId,
    getSessionId,
    resetSessionId
  }
})
