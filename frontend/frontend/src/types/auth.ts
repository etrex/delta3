export interface User {
  id: number
  username: string
  role: 'CUSTOMER' | 'ADMIN'
}

export interface LoginCredentials {
  username: string
  password: string
  role: 'CUSTOMER' | 'ADMIN'
}

export interface LoginResponse {
  token: string
  username: string
  role: 'CUSTOMER' | 'ADMIN'
  email: string
}

export interface ApiError {
  message: string
  status: number
}