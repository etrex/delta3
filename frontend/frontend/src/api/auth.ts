import axios from 'axios'
import type { LoginCredentials, LoginResponse } from '@/types/auth'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

export const authApi = {
  async login(credentials: LoginCredentials): Promise<LoginResponse> {
    const response = await api.post('/api/auth/login', credentials)
    return response.data
  },

  async logout(): Promise<void> {
    await api.post('/api/auth/logout')
  },

  async validateToken(token: string): Promise<boolean> {
    try {
      await api.get('/api/auth/validate', {
        headers: {
          Authorization: `Bearer ${token}`
        }
      })
      return true
    } catch {
      return false
    }
  }
}

// 添加 request interceptor 來自動添加 token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 添加 response interceptor 來處理 401 錯誤
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Only redirect to login if user is already authenticated (has token)
    // Don't redirect on login endpoint failures
    if (error.response?.status === 401 &&
        !error.config?.url?.includes('/api/auth/login') &&
        localStorage.getItem('auth_token')) {
      // Token 過期或無效，清除登入狀態
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api