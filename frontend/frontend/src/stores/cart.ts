import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useProductsStore, type Product } from './products'

export interface CartItem {
  product: Product
  quantity: number
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const loading = ref(false)

  // Computed properties
  const totalItems = computed(() => {
    return items.value.reduce((total, item) => total + item.quantity, 0)
  })

  const totalPrice = computed(() => {
    return items.value.reduce((total, item) => total + (item.product.price * item.quantity), 0)
  })

  const cartCount = computed(() => totalItems.value)

  // Actions
  const addToCart = async (productId: number, quantity: number = 1) => {
    const productsStore = useProductsStore()
    const product = productsStore.getProductById(productId)

    if (!product) {
      throw new Error('Product not found')
    }

    if (product.stock < quantity) {
      throw new Error('庫存不足')
    }

    loading.value = true
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 300))

      const existingItem = items.value.find(item => item.product.id === productId)

      if (existingItem) {
        const totalQuantity = existingItem.quantity + quantity
        if (product.stock < totalQuantity) {
          throw new Error('庫存不足')
        }
        existingItem.quantity = totalQuantity
      } else {
        items.value.push({
          product,
          quantity
        })
      }

      // Update product stock (simulate)
      product.stock -= quantity

      return {
        success: true,
        message: '已加入購物車'
      }
    } finally {
      loading.value = false
    }
  }

  const removeFromCart = (productId: number) => {
    const index = items.value.findIndex(item => item.product.id === productId)
    if (index > -1) {
      const item = items.value[index]
      // Restore stock
      item.product.stock += item.quantity
      items.value.splice(index, 1)
    }
  }

  const updateQuantity = (productId: number, quantity: number) => {
    const item = items.value.find(item => item.product.id === productId)
    if (item) {
      if (quantity <= 0) {
        removeFromCart(productId)
        return
      }

      const quantityDiff = quantity - item.quantity
      if (item.product.stock < quantityDiff) {
        throw new Error('庫存不足')
      }

      // Update stock
      item.product.stock -= quantityDiff
      item.quantity = quantity
    }
  }

  const clearCart = () => {
    // Restore all stock
    items.value.forEach(item => {
      item.product.stock += item.quantity
    })
    items.value = []
  }

  const getCartItem = (productId: number) => {
    return items.value.find(item => item.product.id === productId)
  }

  return {
    items,
    loading,
    totalItems,
    totalPrice,
    cartCount,
    addToCart,
    removeFromCart,
    updateQuantity,
    clearCart,
    getCartItem
  }
})