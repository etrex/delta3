/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */

export interface User {
  username: string
  role: 'ADMIN' | 'CUSTOMER'
  email: string
}

export interface AuthResponse {
  token: string
  username: string
  role: string
  email: string
}

export interface Product {
  id?: number
  name: string
  description: string
  price: number
  stock: number
  status: 'ACTIVE' | 'INACTIVE'
  createdAt?: string
}

export interface OrderItem {
  id?: number
  productId: number
  productName?: string
  quantity: number
  price: number
}

export interface Order {
  id?: number
  orderNo?: string
  customerId?: number
  customerName?: string
  totalAmount: number
  status: 'CART' | 'CREATED' | 'PAID' | 'APPROVED' | 'SHIPPED' | 'CANCELLED'
  createdAt?: string
  updatedAt?: string
  items?: OrderItem[]
  payments?: Payment[]
}

export interface Payment {
  id?: number
  orderId: number
  paymentMethod: 'CREDIT_CARD' | 'BANK_TRANSFER' | 'PAYPAL' | 'CASH'
  amount: number
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED'
  transactionId?: string
  paidAt?: string
  createdAt?: string
}

export interface CreateOrderRequest {
  customerId: number
  items: OrderItem[]
}