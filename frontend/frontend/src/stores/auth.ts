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
      // For development/testing, use mock login
      const mockLogin = async (creds: LoginCredentials) => {
        await new Promise(resolve => setTimeout(resolve, 500))

        // Check mock credentials
        if (creds.role === 'CUSTOMER' && creds.username === 'customer1' && creds.password === 'password123') {
          return {
            user: {
              id: 1,
              username: 'customer1',
              email: 'customer1@example.com',
              role: 'CUSTOMER' as const
            },
            token: 'mock-customer-token-123'
          }
        } else if (creds.role === 'ADMIN' && creds.username === 'admin' && creds.password === 'password123') {
          return {
            user: {
              id: 2,
              username: 'admin',
              email: 'admin@example.com',
              role: 'ADMIN' as const
            },
            token: 'mock-admin-token-456'
          }
        } else {
          throw new Error('登入失敗')
        }
      }

      // Try real API first, fallback to mock
      let response
      try {
        response = await authApi.login(credentials)
      } catch (apiError) {
        console.log('API login failed, using mock login')
        response = await mockLogin(credentials)
      }

      user.value = response.user
      token.value = response.token

      // 儲存到 localStorage
      localStorage.setItem('auth_token', response.token)
      localStorage.setItem('auth_user', JSON.stringify(response.user))

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