/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import cartApi from '@/api/cart'
import type { Order } from '@/types'

export const useCartStore = defineStore('cart', () => {
  const cart = ref<Order | null>(null)
  const loading = ref(false)

  const items = computed(() => cart.value?.items || [])
  const totalItems = computed(() => {
    return items.value.reduce((sum, item) => sum + item.quantity, 0)
  })
  const totalAmount = computed(() => cart.value?.totalAmount || 0)

  async function loadCart() {
    try {
      loading.value = true
      cart.value = await cartApi.getCart()
    } catch (error) {
      console.error('Failed to load cart:', error)
      cart.value = null
    } finally {
      loading.value = false
    }
  }

  async function addToCart(productId: number, quantity: number) {
    try {
      cart.value = await cartApi.addToCart(productId, quantity)
    } catch (error) {
      console.error('Failed to add to cart:', error)
      throw error
    }
  }

  async function updateCartItem(itemId: number, quantity: number) {
    try {
      cart.value = await cartApi.updateCartItem(itemId, quantity)
    } catch (error) {
      console.error('Failed to update cart item:', error)
      throw error
    }
  }

  async function removeCartItem(itemId: number) {
    try {
      await cartApi.removeCartItem(itemId)
      await loadCart() // Reload cart after removal
    } catch (error) {
      console.error('Failed to remove cart item:', error)
      throw error
    }
  }

  async function checkout() {
    try {
      const order = await cartApi.checkout()
      cart.value = null // Clear cart after checkout
      return order
    } catch (error) {
      console.error('Failed to checkout:', error)
      throw error
    }
  }

  function clearCart() {
    cart.value = null
  }

  return {
    cart,
    items,
    totalItems,
    totalAmount,
    loading,
    loadCart,
    addToCart,
    updateCartItem,
    removeCartItem,
    checkout,
    clearCart
  }
})
