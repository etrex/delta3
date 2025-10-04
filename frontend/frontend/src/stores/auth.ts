import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginCredentials } from '@/types/auth'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const isAuthenticated = computed(() => !!token.value && !!user.value)

  // Actions
  const login = async (credentials: LoginCredentials) => {
    isLoading.value = true
    error.value = null

    try {
      // Use real API
      const response = await authApi.login(credentials)

      // Transform backend response to User object
      const userObj: User = {
        id: 0, // Backend doesn't return ID, use 0 as placeholder
        username: response.username,
        role: response.role
      }

      user.value = userObj
      token.value = response.token

      // 儲存到 localStorage
      localStorage.setItem('auth_token', response.token)
      localStorage.setItem('auth_user', JSON.stringify(userObj))

      return response
    } catch (err: any) {
      error.value = err.message || err.response?.data?.message || '登入失敗'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const logout = () => {
    user.value = null
    token.value = null
    error.value = null

    // 清除 localStorage
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
  }

  const initializeAuth = () => {
    const savedToken = localStorage.getItem('auth_token')
    const savedUser = localStorage.getItem('auth_user')

    if (savedToken && savedUser) {
      try {
        token.value = savedToken
        user.value = JSON.parse(savedUser)
      } catch (error) {
        // 如果解析失敗，清除損壞的資料
        logout()
      }
    }
  }

  const clearError = () => {
    error.value = null
  }

  return {
    // State
    user,
    token,
    isLoading,
    error,
    // Getters
    isAuthenticated,
    // Actions
    login,
    logout,
    initializeAuth,
    clearError
  }
})