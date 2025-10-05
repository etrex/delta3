<template>
  <div class="checkout-container">
    <div class="header">
      <h2>結帳</h2>
      <el-button
        @click="backToCart"
        data-cy="back-to-cart-btn"
      >
        <el-icon><ArrowLeft /></el-icon>
        返回購物車
      </el-button>
    </div>

    <div v-loading="ordersStore.loading" class="checkout-content">
      <!-- 訂單摘要 -->
      <div class="order-summary" data-cy="order-summary">
        <h3>訂單摘要</h3>

        <!-- 客戶資訊 -->
        <div class="customer-section" data-cy="customer-info">
          <h4>客戶資訊</h4>
          <p><strong>客戶ID:</strong> <span data-cy="customer-id">{{ authStore.user?.id }}</span></p>
          <p><strong>客戶名稱:</strong> <span>{{ authStore.user?.username }}</span></p>
        </div>

        <!-- 訂單商品列表 -->
        <div class="order-items-section" data-cy="order-items">
          <h4>商品明細</h4>
          <div
            v-for="item in cartStore.items"
            :key="item.product.id"
            class="order-item"
            data-cy="order-item"
          >
            <div class="item-image">
              <img :src="item.product.image" :alt="item.product.name" data-cy="product-image" />
            </div>
            <div class="item-details">
              <h5 data-cy="product-name">{{ item.product.name }}</h5>
              <p class="item-price">
                單價: <span data-cy="item-price">${{ item.product.price.toFixed(2) }}</span>
              </p>
              <div class="item-quantity">
                數量: <span data-cy="item-quantity">{{ item.quantity }}</span>
                <el-button
                  size="small"
                  data-cy="edit-quantity-btn"
                  @click="showEditQuantity(item)"
                >
                  修改
                </el-button>
              </div>
              <p class="item-subtotal">
                小計: <span data-cy="item-subtotal">${{ (item.product.price * item.quantity).toFixed(2) }}</span>
              </p>
            </div>
          </div>
        </div>

        <!-- 訂單總計 -->
        <div class="order-total-section">
          <div class="total-row" data-cy="item-total">
            <span>商品總計:</span>
            <span>${{ cartStore.totalPrice.toFixed(2) }}</span>
          </div>
          <div class="total-row shipping-fee" data-cy="shipping-fee">
            <span>運費:</span>
            <span>$0.00</span>
          </div>
          <div class="total-row grand-total">
            <strong>訂單總額:</strong>
            <strong data-cy="total-amount">${{ cartStore.totalPrice.toFixed(2) }}</strong>
          </div>
        </div>

        <!-- 訂單狀態 -->
        <div class="order-status-section">
          <p>訂單狀態: <span class="status-badge created" data-cy="order-status">CREATED</span></p>
        </div>

        <!-- 確認訂單按鈕 -->
        <div class="actions">
          <el-button
            type="primary"
            size="large"
            :loading="ordersStore.loading"
            data-cy="confirm-order-btn"
            @click="handleConfirmOrder"
          >
            確認訂單
          </el-button>
        </div>
      </div>
    </div>

    <!-- 修改數量對話框 -->
    <el-dialog
      v-model="showQuantityDialog"
      title="修改數量"
      width="400px"
      data-cy="edit-quantity-dialog"
    >
      <div v-if="editingItem">
        <p>商品: {{ editingItem.product.name }}</p>
        <p>價格: ${{ editingItem.product.price }}</p>
        <p>可用庫存: {{ editingItem.product.stock }}</p>
        <div class="quantity-input">
          <label>數量:</label>
          <el-input-number
            v-model="newQuantity"
            :min="1"
            :max="editingItem.product.stock"
            data-cy="quantity-input"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="showQuantityDialog = false">取消</el-button>
        <el-button
          type="primary"
          data-cy="update-quantity-btn"
          @click="handleUpdateQuantity"
        >
          更新
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore, type CartItem } from '@/stores/cart'
import { useOrdersStore } from '@/stores/orders'
import { showErrorMessage, showSuccessMessage } from '@/utils/message'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()
const ordersStore = useOrdersStore()

const showQuantityDialog = ref(false)
const editingItem = ref<CartItem | null>(null)
const newQuantity = ref(1)

onMounted(() => {
  // 檢查購物車是否為空
  if (cartStore.items.length === 0) {
    showErrorMessage('購物車不能為空')
    router.push('/cart')
  }
})

const backToCart = () => {
  router.push('/cart')
}

const showEditQuantity = (item: CartItem) => {
  editingItem.value = item
  newQuantity.value = item.quantity
  showQuantityDialog.value = true
}

const handleUpdateQuantity = () => {
  if (editingItem.value) {
    try {
      cartStore.updateQuantity(editingItem.value.product.id, newQuantity.value)
      ElMessage.success('數量已更新')
      showQuantityDialog.value = false
      editingItem.value = null
    } catch (error: any) {
      ElMessage.error(error.message || '更新失敗')
    }
  }
}

const handleConfirmOrder = async () => {
  try {
    const order = await ordersStore.createOrder()

    showSuccessMessage('訂單已成功建立')

    // 顯示訂單號碼
    const orderNumberEl = document.createElement('div')
    orderNumberEl.setAttribute('data-cy', 'order-number')
    orderNumberEl.textContent = order.orderNo
    orderNumberEl.style.display = 'none'
    document.body.appendChild(orderNumberEl)

    // 導向訂單詳情頁面
    router.push(`/orders/${order.orderNo}`)
  } catch (error: any) {
    const errorMsg = error.response?.data?.message || error.message || '建立訂單失敗'

    // 檢查是否為庫存不足錯誤
    if (errorMsg.includes('Insufficient stock') || errorMsg.includes('庫存不足')) {
      showErrorMessage('庫存不足')

      // 顯示詳細錯誤訊息
      const errorDetailsEl = document.createElement('div')
      errorDetailsEl.setAttribute('data-cy', 'error-details')
      errorDetailsEl.textContent = errorMsg
      errorDetailsEl.style.display = 'none'
      document.body.appendChild(errorDetailsEl)
    } else {
      showErrorMessage(errorMsg)
    }
  }
}
</script>

<style scoped>
.checkout-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h2 {
  margin: 0;
}

.checkout-content {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.order-summary h3 {
  margin-top: 0;
  margin-bottom: 25px;
  font-size: 24px;
  color: #333;
}

.customer-section {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 25px;
}

.customer-section h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 16px;
}

.customer-section p {
  margin: 5px 0;
  font-size: 14px;
}

.order-items-section {
  margin-bottom: 25px;
}

.order-items-section h4 {
  margin-bottom: 15px;
  font-size: 18px;
}

.order-item {
  display: flex;
  gap: 15px;
  padding: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  margin-bottom: 15px;
}

.item-image {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.item-details {
  flex: 1;
}

.item-details h5 {
  margin: 0 0 8px 0;
  font-size: 16px;
}

.item-price {
  margin: 5px 0;
  font-size: 14px;
  color: #666;
}

.item-quantity {
  margin: 8px 0;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-subtotal {
  margin: 8px 0 0 0;
  font-size: 16px;
  font-weight: bold;
  color: #e74c3c;
}

.order-total-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.total-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 16px;
}

.total-row.shipping-fee {
  color: #666;
  font-size: 14px;
}

.total-row.grand-total {
  font-size: 20px;
  padding-top: 15px;
  border-top: 2px solid #ddd;
  margin-top: 10px;
  margin-bottom: 0;
}

.order-status-section {
  margin-bottom: 20px;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
}

.status-badge.created {
  background-color: #3498db;
  color: white;
}

.actions {
  text-align: center;
}

.actions .el-button {
  min-width: 200px;
}

.quantity-input {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 15px 0;
}

@media (max-width: 768px) {
  .checkout-container {
    padding: 10px;
  }

  .checkout-content {
    padding: 20px;
  }

  .order-item {
    flex-direction: column;
  }

  .item-image {
    width: 100%;
    height: 150px;
  }
}
</style>
