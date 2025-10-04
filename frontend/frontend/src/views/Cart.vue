<template>
  <div class="cart-container">
    <div class="header">
      <h2>購物車</h2>
      <el-button @click="goToProducts" data-cy="continue-shopping-btn">
        <el-icon><ArrowLeft /></el-icon>
        繼續購物
      </el-button>
    </div>

    <!-- 空購物車 -->
    <div v-if="cartStore.items.length === 0" class="empty-cart">
      <el-empty data-cy="cart-empty-message" description="購物車是空的">
        <el-button type="primary" @click="goToProducts">前往商品頁面</el-button>
      </el-empty>
      <div class="empty-cart-actions">
        <el-button
          type="primary"
          size="large"
          data-cy="checkout-btn"
          disabled
        >
          前往結帳
        </el-button>
      </div>
    </div>

    <!-- 購物車商品列表 -->
    <div v-else class="cart-content">
      <div class="cart-items" data-cy="cart-items">
        <div
          v-for="item in cartStore.items"
          :key="item.product.id"
          class="cart-item"
          data-cy="cart-item"
        >
          <div class="item-image">
            <img :src="item.product.image" :alt="item.product.name" />
          </div>

          <div class="item-info">
            <h3 data-cy="item-name">{{ item.product.name }}</h3>
            <p class="item-price" data-cy="item-price">${{ item.product.price }}</p>
            <p class="item-stock">庫存: {{ item.product.stock }}</p>
          </div>

          <div class="item-quantity">
            <el-button
              size="small"
              :icon="Minus"
              data-cy="quantity-decrease-btn"
              @click="decreaseQuantity(item.product.id)"
              :disabled="item.quantity <= 1"
            />
            <span class="quantity-display" data-cy="item-quantity">{{ item.quantity }}</span>
            <el-button
              size="small"
              :icon="Plus"
              data-cy="quantity-increase-btn"
              @click="increaseQuantity(item.product.id, item.product.stock)"
              :disabled="item.quantity >= item.product.stock"
            />
          </div>

          <div class="item-subtotal">
            <p data-cy="item-subtotal">${{ (item.product.price * item.quantity).toFixed(2) }}</p>
          </div>

          <div class="item-actions">
            <el-button
              type="danger"
              size="small"
              :icon="Delete"
              data-cy="remove-item-btn"
              @click="showRemoveDialog(item.product.id, item.product.name)"
            >
              移除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 購物車總計 -->
      <div class="cart-summary">
        <div class="summary-row">
          <span>商品小計：</span>
          <span data-cy="items-subtotal">${{ cartStore.totalPrice.toFixed(2) }}</span>
        </div>
        <div class="summary-row total">
          <span>總計：</span>
          <span data-cy="cart-total">${{ cartStore.totalPrice.toFixed(2) }}</span>
        </div>
        <el-button
          type="primary"
          size="large"
          data-cy="checkout-btn"
          :disabled="cartStore.items.length === 0"
          @click="goToCheckout"
        >
          前往結帳
        </el-button>
      </div>
    </div>

    <!-- 確認移除對話框 -->
    <el-dialog
      v-model="showConfirmRemove"
      title="確認移除"
      width="400px"
      data-cy="confirm-dialog"
    >
      <p>確定要移除「{{ removeItemName }}」嗎？</p>
      <template #footer>
        <el-button @click="showConfirmRemove = false">取消</el-button>
        <el-button type="danger" data-cy="confirm-btn" @click="confirmRemove">
          確認移除
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus, Minus, Delete } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { showSuccessMessage } from '@/utils/message'

const router = useRouter()
const cartStore = useCartStore()

const showConfirmRemove = ref(false)
const removeItemId = ref<number | null>(null)
const removeItemName = ref('')

const goToProducts = () => {
  router.push('/products')
}

const goToCheckout = () => {
  if (cartStore.items.length === 0) {
    ElMessage.warning('購物車不能為空')
    return
  }
  router.push('/checkout')
}

const increaseQuantity = (productId: number, maxStock: number) => {
  const item = cartStore.getCartItem(productId)
  if (item && item.quantity < maxStock) {
    try {
      cartStore.updateQuantity(productId, item.quantity + 1)
      // 更新總價會自動透過 computed 重新計算
    } catch (error: any) {
      ElMessage.error(error.message || '更新失敗')
    }
  }
}

const decreaseQuantity = (productId: number) => {
  const item = cartStore.getCartItem(productId)
  if (item && item.quantity > 1) {
    cartStore.updateQuantity(productId, item.quantity - 1)
  }
}

const showRemoveDialog = (productId: number, productName: string) => {
  removeItemId.value = productId
  removeItemName.value = productName
  showConfirmRemove.value = true
}

const confirmRemove = () => {
  if (removeItemId.value !== null) {
    cartStore.removeFromCart(removeItemId.value)
    ElMessage.success('已移除商品')
    showSuccessMessage('已移除商品')
    showConfirmRemove.value = false
    removeItemId.value = null
    removeItemName.value = ''
  }
}
</script>

<style scoped>
.cart-container {
  max-width: 1200px;
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

.empty-cart {
  text-align: center;
  padding: 80px 20px;
}

.empty-cart-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.cart-content {
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
}

.cart-items {
  flex: 1;
  min-width: 600px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 15px;
  background: white;
}

.item-image {
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.item-info {
  flex: 1;
}

.item-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
}

.item-price {
  font-size: 18px;
  font-weight: bold;
  color: #e74c3c;
  margin: 5px 0;
}

.item-stock {
  font-size: 14px;
  color: #666;
  margin: 5px 0;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quantity-display {
  min-width: 40px;
  text-align: center;
  font-size: 16px;
  font-weight: bold;
}

.item-subtotal {
  min-width: 100px;
  text-align: right;
}

.item-subtotal p {
  font-size: 20px;
  font-weight: bold;
  color: #2c3e50;
  margin: 0;
}

.item-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cart-summary {
  min-width: 300px;
  max-width: 400px;
  padding: 25px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  align-self: flex-start;
  position: sticky;
  top: 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 16px;
}

.summary-row.total {
  font-size: 20px;
  font-weight: bold;
  padding-top: 15px;
  border-top: 2px solid #e0e0e0;
  margin-top: 10px;
}

.cart-summary .el-button {
  width: 100%;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .cart-content {
    flex-direction: column;
  }

  .cart-items {
    min-width: 100%;
  }

  .cart-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .item-quantity {
    width: 100%;
    justify-content: center;
  }

  .item-subtotal {
    width: 100%;
    text-align: center;
  }

  .item-actions {
    width: 100%;
  }

  .item-actions .el-button {
    width: 100%;
  }

  .cart-summary {
    max-width: 100%;
    position: static;
  }
}
</style>
