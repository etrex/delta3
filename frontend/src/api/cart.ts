import axios from './axios'
import type { Order } from '@/types'

export default {
  // Get current user's cart
  getCart() {
    return axios.get<Order>('/orders/cart')
  },

  // Add item to cart
  addToCart(productId: number, quantity: number) {
    return axios.post<Order>('/orders/cart/items', {
      productId,
      quantity
    })
  },

  // Update cart item quantity
  updateCartItem(itemId: number, quantity: number) {
    return axios.put<Order>(`/orders/cart/items/${itemId}`, {
      quantity
    })
  },

  // Remove item from cart
  removeCartItem(itemId: number) {
    return axios.delete(`/orders/cart/items/${itemId}`)
  },

  // Checkout cart
  checkout() {
    return axios.post<Order>('/orders/cart/checkout')
  }
}
