/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import axios from './axios'
import type { Product } from '@/types'

export default {
  getProducts(params?: any) {
    return axios.get('/products', { params })
  },

  getProduct(id: number) {
    return axios.get(`/products/${id}`)
  },

  createProduct(product: Product) {
    return axios.post('/products', product)
  },

  updateProduct(id: number, product: Product) {
    return axios.put(`/products/${id}`, product)
  },

  deleteProduct(id: number) {
    return axios.delete(`/products/${id}`)
  }
}