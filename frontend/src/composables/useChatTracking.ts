/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import { useAuthStore } from '@/stores/auth'

/**
 * Track user navigation and actions for AI context
 * Note: All tracking is now done automatically by backend interceptor
 */
export function useChatTracking() {
  const authStore = useAuthStore()

  // These functions are kept for backwards compatibility but do nothing
  // Backend interceptor automatically tracks all operations
  const recordClick = (buttonId: string) => {
    // No-op: backend tracks automatically
  }

  const recordSubmit = (formId: string) => {
    // No-op: backend tracks automatically
  }

  return {
    recordClick,
    recordSubmit
  }
}
