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
            v-for="(item, index) in cartStore.items"
            :key="item.product.id"
            class="order-item"
            data-cy="order-item"
          >
            <div class="item-info">
              <span class="item-name" data-cy="item-name">{{ item.product.name }}</span>
              <span v-if="editingIndex !== index" class="item-quantity" data-cy="item-quantity">x {{ item.quantity }}</span>
              <div v-else class="quantity-edit">
                <el-input-number
                  v-model="editQuantity"
                  :min="1"
                  data-cy="quantity-input"
                  size="small"
                />
                <el-button
                  data-cy="update-quantity-btn"
                  size="small"
                  type="primary"
                  @click="updateQuantity(item.product.id!)"
                >更新</el-button>
              </div>
            </div>
            <div class="item-prices">
              <span class="item-price" data-cy="item-price">${{ item.product.price.toFixed(2) }}</span>
              <span class="item-subtotal" data-cy="item-subtotal">${{ (item.product.price * item.quantity).toFixed(2) }}</span>
              <el-button
                v-if="editingIndex !== index"
                data-cy="edit-quantity-btn"
                size="small"
                @click="startEdit(index, item.quantity)"
              >修改數量</el-button>
            </div>
          </div>
        </div>

        <div class="order-total-section">
          <div class="total-row">
            <span>商品總計</span>
            <span data-cy="item-total">${{ cartStore.totalAmount.toFixed(2) }}</span>
          </div>
          <div class="total-row total">
            <span>訂單總額</span>
            <span data-cy="total-amount">${{ cartStore.totalAmount.toFixed(2) }}</span>
          </div>
          <div class="order-status-info">
            <span>訂單狀態：</span>
            <span data-cy="order-status">CREATED</span>
          </div>
        </div>

        <div class="action-buttons">
          <el-button data-cy="back-to-cart-btn" @click="goBackToCart">返回購物車</el-button>
          <el-button
            type="primary"
            data-cy="confirm-order-btn"
            :loading="isCreatingOrder"
            @click="confirmOrder"
          >確認訂單</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import ordersApi from '@/api/orders'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const isCreatingOrder = ref(false)
const errorMessage = ref('')
const errorDetails = ref('')
const successMessage = ref('')
const editingIndex = ref<number | null>(null)
const editQuantity = ref(1)

onMounted(() => {
  // 如果購物車是空的，重導向到購物車頁面
  if (cartStore.items.length === 0) {
    errorMessage.value = '購物車不能為空'
    setTimeout(() => {
      router.push('/cart')
    }, 1500)
  }
})

async function confirmOrder() {
  if (cartStore.items.length === 0) {
    errorMessage.value = '購物車不能為空'
    return
  }

  isCreatingOrder.value = true
  errorMessage.value = ''
  errorDetails.value = ''
  successMessage.value = ''

  try {
    // 準備訂單資料
    const orderData = {
      customerId: 1, // TODO: 從 authStore 獲取實際的 customer ID
      items: cartStore.items.map(item => ({
        productId: item.product.id!,
        productName: item.product.name,
        quantity: item.quantity,
        price: item.product.price
      }))
    }

    const response = await ordersApi.createOrder(orderData)
    const order = response.data

    // 清空購物車
    cartStore.clearCart()

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
  router.push('/cart')
}

function startEdit(index: number, quantity: number) {
  editingIndex.value = index
  editQuantity.value = quantity
}

function updateQuantity(productId: number) {
  cartStore.updateQuantity(productId, editQuantity.value)
  editingIndex.value = null
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

.item-info {
  display: flex;
  gap: 16px;
  align-items: center;
}

.item-name {
  font-weight: 500;
}

.item-quantity {
  color: #666;
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
