/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Product } from '@/types'

export interface CartItem {
  product: Product
  quantity: number
}

export const useCartStore = defineStore('cart', () => {
  // Load cart from localStorage on init
  const savedCart = localStorage.getItem('cart')
  const items = ref<CartItem[]>(savedCart ? JSON.parse(savedCart) : [])

  const totalItems = computed(() => {
    return items.value.reduce((sum, item) => sum + item.quantity, 0)
  })

  const totalAmount = computed(() => {
    return items.value.reduce((sum, item) => sum + item.product.price * item.quantity, 0)
  })

  function saveToLocalStorage() {
    localStorage.setItem('cart', JSON.stringify(items.value))
  }

  function addToCart(product: Product, quantity: number) {
    const existingItem = items.value.find(item => item.product.id === product.id)

    if (existingItem) {
      existingItem.quantity += quantity
    } else {
      items.value.push({ product, quantity })
    }
    saveToLocalStorage()
  }

  function updateQuantity(productId: number, quantity: number) {
    const item = items.value.find(item => item.product.id === productId)
    if (item) {
      if (quantity <= 0) {
        removeFromCart(productId)
      } else {
        item.quantity = quantity
      }
    }
    saveToLocalStorage()
  }

  function removeFromCart(productId: number) {
    const index = items.value.findIndex(item => item.product.id === productId)
    if (index > -1) {
      items.value.splice(index, 1)
    }
    saveToLocalStorage()
  }

  function clearCart() {
    items.value = []
    saveToLocalStorage()
  }

  return {
    items,
    totalItems,
    totalAmount,
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart
  }
})
