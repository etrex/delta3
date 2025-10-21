/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import axios from './axios'
import type { CreateOrderRequest, Payment } from '@/types'

export default {
  getOrders(params?: any, tracking: boolean = true, context?: string) {
    return axios.get('/orders', { params: { ...params, tracking, context } })
  },

  getOrder(id: number) {
    return axios.get(`/orders/${id}`)
  },

  createOrder(order: CreateOrderRequest) {
    return axios.post('/orders', order)
  },

  payOrder(id: number, payment: Partial<Payment>) {
    return axios.post(`/orders/${id}/payments`, payment)
  },

  cancelOrder(id: number) {
    return axios.post(`/orders/${id}/cancel`)
  },

  approveOrder(orderNo: string) {
    return axios.post(`/orders/by-order-no/${orderNo}/approve`)
  },

  shipOrder(id: number) {
    return axios.post(`/orders/${id}/ship`)
  },

  getOrderEvents(id: number) {
    return axios.get(`/orders/${id}/events`)
  }
}