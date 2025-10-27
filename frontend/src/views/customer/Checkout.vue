<template>
  <div class="checkout-container">
    <h1>訂單確認</h1>

    <div v-if="errorMessage" class="error-message" data-cy="error-message">
      {{ errorMessage }}
      <div v-if="errorDetails" data-cy="error-details">{{ errorDetails }}</div>
    </div>

    <div v-if="successMessage" class="success-message" data-cy="success-message">
      {{ successMessage }}
    </div>

    <div class="checkout-content">
      <div class="order-summary" data-cy="order-summary">
        <h2>訂單摘要</h2>

        <div class="customer-info" data-cy="customer-info">
          <p><strong>客戶ID:</strong> <span data-cy="customer-id">{{ authStore.user?.username }}</span></p>
        </div>

        <div class="order-items" data-cy="order-items">
          <h3>商品明細</h3>
          <div
            v-for="item in items"
            :key="item.id"
            class="order-item"
            :class="{ 'stock-insufficient': isStockInsufficient(item) }"
            data-cy="order-item"
          >
            <div class="item-info">
              <div class="item-main-row">
                <span class="item-name" data-cy="item-name">{{ item.productName }}</span>
                <div class="quantity-controls">
                  <el-button
                    size="small"
                    data-cy="decrease-quantity-btn"
                    @click="decreaseQuantity(item.id!)"
                    :disabled="item.quantity <= 1"
                  >-</el-button>
                  <el-input-number
                    :model-value="item.quantity"
                    :min="1"
                    :max="getAvailableStock(item.productId)"
                    data-cy="quantity-input"
                    size="small"
                    @change="(value) => handleQuantityChange(item.id!, value, item.quantity)"
                    :controls="false"
                  />
                  <el-button
                    size="small"
                    data-cy="increase-quantity-btn"
                    @click="increaseQuantity(item.id!)"
                    :disabled="item.quantity >= getAvailableStock(item.productId)"
                  >+</el-button>
                </div>
              </div>
              <div v-if="isStockInsufficient(item)" class="stock-warning" data-cy="stock-warning">
                ⚠️ 庫存不足（目前庫存：{{ getAvailableStock(item.productId) }}）
              </div>
            </div>
            <div class="item-prices">
              <span class="item-price" data-cy="item-price">${{ getLatestPrice(item.productId).toFixed(0) }}</span>
              <span class="item-subtotal" data-cy="item-subtotal">${{ (getLatestPrice(item.productId) * item.quantity).toFixed(0) }}</span>
              <el-button
                data-cy="remove-item-btn"
                size="small"
                type="danger"
                @click="removeItem(item.id!)"
              >移除</el-button>
            </div>
          </div>
        </div>

        <div class="order-total-section">
          <div class="total-row">
            <span>商品總計</span>
            <span data-cy="item-total">${{ totalAmount.toFixed(0) }}</span>
          </div>
          <div class="total-row total">
            <span>訂單總額</span>
            <span data-cy="total-amount">${{ totalAmount.toFixed(0) }}</span>
          </div>
        </div>

        <div v-if="hasStockIssues" class="checkout-warning" data-cy="checkout-warning">
          ⚠️ 請調整商品數量至庫存範圍內才能結帳
        </div>

        <div class="action-buttons">
          <el-button data-cy="back-to-cart-btn" @click="goBackToCart">返回購物車</el-button>
          <el-button
            type="primary"
            data-cy="confirm-order-btn"
            :loading="isCreatingOrder"
            :disabled="hasStockIssues"
            @click="confirmOrder"
          >確認結帳</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import productsApi from '@/api/products'
import type { Product } from '@/types'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const isCreatingOrder = ref(false)
const errorMessage = ref('')
const errorDetails = ref('')
const successMessage = ref('')
const productStocks = ref<Record<number, number>>({})
const productPrices = ref<Record<number, number>>({})

const items = computed(() => cartStore.items)
const totalAmount = computed(() => cartStore.totalAmount)

const hasStockIssues = computed(() => {
  return items.value.some(item => isStockInsufficient(item))
})

onMounted(async () => {
  await cartStore.loadCart({ tracking: true, context: 'checkout' }) // Track entering checkout page

  // 如果購物車是空的，重導向到商品頁面
  if (items.value.length === 0) {
    errorMessage.value = '購物車不能為空'
    setTimeout(() => {
      router.push('/products')
    }, 1500)
    return
  }

  // 載入商品庫存資訊
  await loadProductStocks()
})

async function loadProductStocks() {
  try {
    // Load products without tracking (for stock and price validation)
    const response = await productsApi.getProducts({ tracking: false })
    console.log('Products API response:', response)
    // axios 攔截器已經自動提取 response.data，所以直接使用 response.content
    const products = response.content || response || []
    console.log('Products:', products)

    // 建立 productId -> stock 和 productId -> price 的對應
    productStocks.value = {}
    productPrices.value = {}
    products.forEach((product: Product) => {
      if (product.id) {
        productStocks.value[product.id] = product.stock
        productPrices.value[product.id] = product.price
      }
    })
    console.log('Product stocks loaded:', productStocks.value)
    console.log('Product prices loaded:', productPrices.value)
  } catch (error) {
    console.error('Failed to load product stocks:', error)
  }
}

function getAvailableStock(productId: number): number {
  return productStocks.value[productId] || 999
}

function getLatestPrice(productId: number): number {
  // Return latest product price if available, otherwise fall back to item price
  return productPrices.value[productId] || 0
}

function isStockInsufficient(item: any): boolean {
  const availableStock = getAvailableStock(item.productId)
  const result = item.quantity > availableStock
  console.log(`Stock check for ${item.productName}: quantity=${item.quantity}, stock=${availableStock}, insufficient=${result}`)
  return result
}

async function confirmOrder() {
  if (items.value.length === 0) {
    errorMessage.value = '購物車不能為空'
    return
  }

  isCreatingOrder.value = true
  errorMessage.value = ''
  errorDetails.value = ''
  successMessage.value = ''

  try {
    // Call checkout API to convert cart to order
    const order = await cartStore.checkout()

    // 顯示成功訊息
    successMessage.value = '訂單已成功建立'
    ElMessage.success('訂單已成功建立')

    // 跳轉到訂單詳情頁面
    router.push(`/orders/${order.id}`)
  } catch (error: any) {
    console.error('Failed to create order:', error)

    if (error.response?.data?.message) {
      if (error.response.data.message.includes('Insufficient stock')) {
        errorMessage.value = '庫存不足'
        errorDetails.value = error.response.data.message
      } else {
        errorMessage.value = error.response.data.message
      }
    } else {
      errorMessage.value = '建立訂單失敗，請稍後再試'
    }
  } finally {
    isCreatingOrder.value = false
  }
}

function goBackToCart() {
  router.push('/products')
}

async function handleQuantityChange(itemId: number, newValue: number | null | undefined, oldValue: number) {
  console.log('handleQuantityChange called:', { itemId, newValue, oldValue, type: typeof newValue })

  // Validate and convert quantity
  const qty = typeof newValue === 'string' ? parseInt(newValue, 10) : newValue

  if (qty === null || qty === undefined || isNaN(qty) || qty < 1) {
    console.warn('Invalid quantity after conversion:', qty, 'keeping old value:', oldValue)
    // Reload cart to reset to correct value
    await cartStore.loadCart({ tracking: false })
    return
  }

  // Only update if value actually changed
  if (qty === oldValue) {
    console.log('Quantity unchanged, skipping update')
    return
  }

  try {
    await cartStore.updateCartItem(itemId, qty)
    console.log('Cart updated successfully, new quantity:', qty)
    ElMessage.success('已更新數量')
  } catch (error) {
    console.error('Failed to update quantity:', error)
    ElMessage.error('更新數量失敗')
    // Reload cart on error to reset to correct value
    await cartStore.loadCart({ tracking: false })
  }
}

async function updateQuantity(itemId: number, quantity: number) {
  try {
    await cartStore.updateCartItem(itemId, quantity)
    ElMessage.success('已更新數量')
  } catch (error) {
    console.error('Failed to update quantity:', error)
    ElMessage.error('更新數量失敗')
  }
}

async function increaseQuantity(itemId: number) {
  const item = items.value.find(i => i.id === itemId)
  if (item) {
    const newQuantity = item.quantity + 1
    const availableStock = getAvailableStock(item.productId)
    if (newQuantity > availableStock) {
      ElMessage.warning(`庫存不足，目前庫存：${availableStock}`)
      return
    }
    await updateQuantity(itemId, newQuantity)
  }
}

async function decreaseQuantity(itemId: number) {
  const item = items.value.find(i => i.id === itemId)
  if (item && item.quantity > 1) {
    await updateQuantity(itemId, item.quantity - 1)
  }
}

async function removeItem(itemId: number) {
  try {
    await cartStore.removeCartItem(itemId)
    ElMessage.success('已移除商品')

    // 如果購物車空了，返回商品頁面
    if (items.value.length === 0) {
      errorMessage.value = '購物車已空'
      setTimeout(() => {
        router.push('/products')
      }, 1500)
    }
  } catch (error) {
    console.error('Failed to remove item:', error)
    ElMessage.error('移除商品失敗')
  }
}
</script>

<style scoped>
.checkout-container {
  max-width: 800px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 30px;
  color: #333;
}

.error-message {
  background-color: #FEF0F0;
  color: #F56C6C;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #FDE2E2;
}

.success-message {
  background-color: #F0F9FF;
  color: #67C23A;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #E1F3D8;
}

.checkout-content {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.order-summary h2 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
}

.customer-info {
  margin-bottom: 30px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.order-items h3 {
  margin-bottom: 16px;
  color: #333;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  margin-bottom: 12px;
  background-color: #fafafa;
  border-radius: 4px;
}

.order-item.stock-insufficient {
  background-color: #fef0f0;
  border: 1px solid #f56c6c;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.item-main-row {
  display: flex;
  gap: 16px;
  align-items: center;
}

.stock-warning {
  color: #f56c6c;
  font-size: 14px;
  font-weight: 500;
}

.checkout-warning {
  padding: 12px 16px;
  margin-bottom: 20px;
  background-color: #fef0f0;
  color: #f56c6c;
  border: 1px solid #f56c6c;
  border-radius: 4px;
  font-weight: 500;
}

.item-name {
  font-weight: 500;
  min-width: 120px;
}

.item-quantity {
  color: #666;
}

.quantity-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.item-prices {
  display: flex;
  gap: 20px;
  align-items: center;
}

.item-price {
  color: #909399;
}

.item-subtotal {
  font-weight: bold;
  color: #409EFF;
  min-width: 80px;
  text-align: right;
}

.order-total-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 2px solid #e4e7ed;
}

.total-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  font-size: 16px;
}

.total-row.total {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e4e7ed;
}

.order-status-info {
  margin-top: 16px;
  padding: 12px;
  background-color: #ecf5ff;
  border-radius: 4px;
  color: #409EFF;
}

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: flex-end;
  margin-top: 30px;
}

.action-buttons .el-button {
  min-width: 120px;
}
</style>
