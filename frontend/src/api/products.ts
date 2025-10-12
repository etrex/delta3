/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import axios from './axios'
import type { Product } from '@/types'

export default {
  getProducts(params?: any, tracking: boolean = true, context?: string) {
    return axios.get('/product', { params: { ...params, tracking, context } })
  },

  getProduct(id: number) {
    return axios.get(`/product/${id}`)
  },

  createProduct(product: Product) {
    return axios.post('/product', product)
  },

  updateProduct(id: number, product: Product) {
    return axios.put(`/product/${id}`, product)
  },

  deleteProduct(id: number) {
    return axios.delete(`/product/${id}`)
  }
}