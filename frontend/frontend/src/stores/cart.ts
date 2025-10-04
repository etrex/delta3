import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useProductsStore, type Product } from './products'

export interface CartItem {
  product: Product
  quantity: number
}

export interface CartResult {
  success: boolean
  message: string
  errorType?: 'PRODUCT_NOT_FOUND' | 'INSUFFICIENT_STOCK' | 'SYSTEM_ERROR'
  availableStock?: number
  cartCount?: number
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
  const addToCart = async (productId: number, quantity: number = 1): Promise<CartResult> => {
    const productsStore = useProductsStore()
    const product = productsStore.getProductById(productId)

    // 驗證商品存在
    if (!product) {
      return {
        success: false,
        errorType: 'PRODUCT_NOT_FOUND',
        message: '商品不存在'
      }
    }

    // 驗證庫存（正規業務邏輯）
    if (product.stock < quantity) {
      return {
        success: false,
        errorType: 'INSUFFICIENT_STOCK' as const,
        message: `庫存不足，目前庫存只有 ${product.stock} 個`,
        availableStock: product.stock
      }
    }

    loading.value = true
    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 300))

      const existingItem = items.value.find(item => item.product.id === productId)

      if (existingItem) {
        const totalQuantity = existingItem.quantity + quantity
        if (product.stock < totalQuantity) {
          return {
            success: false,
            errorType: 'INSUFFICIENT_STOCK',
            message: `庫存不足，您購物車已有 ${existingItem.quantity} 個，最多只能再加 ${product.stock - existingItem.quantity} 個`,
            availableStock: product.stock - existingItem.quantity
          }
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
        message: '已加入購物車',
        cartCount: totalItems.value
      }
    } catch (error) {
      // 真正的系統錯誤才在這裡處理
      return {
        success: false,
        errorType: 'SYSTEM_ERROR',
        message: '系統錯誤，請稍後再試'
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