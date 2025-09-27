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
  user: User
  token: string
  message: string
}

export interface ApiError {
  message: string
  status: number
}