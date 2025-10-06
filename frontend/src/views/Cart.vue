<template>
  <div class="cart-container">
    <h1>購物車</h1>

    <div v-if="items.length === 0" class="empty-cart">
      <p data-cy="cart-empty-message">購物車是空的</p>
      <el-button
        data-cy="continue-shopping-btn"
        type="primary"
        @click="goToProducts"
      >繼續購物</el-button>
      <el-button
        data-cy="checkout-btn"
        disabled
      >結帳</el-button>
    </div>

    <div v-else class="cart-content">
      <div class="cart-items" data-cy="cart-items">
        <div
          v-for="item in items"
          :key="item.id"
          class="cart-item"
          data-cy="cart-item"
        >
          <div class="item-details">
            <h3 class="item-name" data-cy="item-name">{{ item.productName }}</h3>
            <p class="item-price" data-cy="item-price">${{ item.price.toFixed(2) }}</p>
          </div>

          <div class="item-quantity">
            <el-button
              data-cy="quantity-decrease-btn"
              size="small"
              @click="decreaseQuantity(item.id!)"
              :disabled="item.quantity <= 1"
            >-</el-button>
            <span data-cy="item-quantity" class="quantity-value">{{ item.quantity }}</span>
            <el-button
              data-cy="quantity-increase-btn"
              size="small"
              @click="increaseQuantity(item.id!)"
            >+</el-button>
          </div>

          <div class="item-subtotal" data-cy="item-subtotal">
            ${{ (item.price * item.quantity).toFixed(2) }}
          </div>

          <el-button
            data-cy="remove-item-btn"
            type="danger"
            size="small"
            @click="confirmRemove(item.id!)"
          >移除</el-button>
        </div>
      </div>

      <div class="cart-summary">
        <div class="cart-total" data-cy="cart-total">
          總計: ${{ totalAmount.toFixed(2) }}
        </div>
        <el-button
          type="primary"
          data-cy="checkout-btn"
          class="checkout-btn"
          @click="goToCheckout"
        >結帳</el-button>
      </div>
    </div>

    <!-- 確認移除對話框 -->
    <el-dialog
      v-model="confirmDialogVisible"
      title="確認移除"
      width="400px"
      data-cy="confirm-dialog"
    >
      <span>確定要移除此商品嗎？</span>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button data-cy="confirm-btn" type="danger" @click="removeItem">確定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import cartApi from '@/api/cart'
import type { Order } from '@/types'

const router = useRouter()
const cart = ref<Order | null>(null)
const confirmDialogVisible = ref(false)
const itemToRemove = ref<number | null>(null)

const items = computed(() => cart.value?.items || [])
const totalAmount = computed(() => cart.value?.totalAmount || 0)

onMounted(async () => {
  await loadCart()
})

async function loadCart() {
  try {
    cart.value = await cartApi.getCart()
  } catch (error) {
    console.error('Failed to load cart:', error)
    ElMessage.error('載入購物車失敗')
  }
}

async function increaseQuantity(itemId: number) {
  const item = items.value.find(i => i.id === itemId)
  if (item) {
    try {
      cart.value = await cartApi.updateCartItem(itemId, item.quantity + 1)
    } catch (error) {
      console.error('Failed to update quantity:', error)
      ElMessage.error('更新數量失敗')
    }
  }
}

async function decreaseQuantity(itemId: number) {
  const item = items.value.find(i => i.id === itemId)
  if (item && item.quantity > 1) {
    try {
      cart.value = await cartApi.updateCartItem(itemId, item.quantity - 1)
    } catch (error) {
      console.error('Failed to update quantity:', error)
      ElMessage.error('更新數量失敗')
    }
  }
}

function confirmRemove(itemId: number) {
  itemToRemove.value = itemId
  confirmDialogVisible.value = true
}

async function removeItem() {
  if (itemToRemove.value !== null) {
    try {
      await cartApi.removeCartItem(itemToRemove.value)
      await loadCart()
      ElMessage.success('已移除商品')
    } catch (error) {
      console.error('Failed to remove item:', error)
      ElMessage.error('移除商品失敗')
    }
    itemToRemove.value = null
  }
  confirmDialogVisible.value = false
}

function goToProducts() {
  router.push('/products')
}

function goToCheckout() {
  router.push('/checkout')
}
</script>

<style scoped>
.cart-container {
  max-width: 900px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 30px;
  color: #333;
}

.empty-cart {
  text-align: center;
  padding: 60px 20px;
}

.empty-cart p {
  font-size: 18px;
  color: #999;
  margin-bottom: 20px;
}

.cart-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cart-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.item-details {
  flex: 1;
}

.item-name {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #333;
}

.item-price {
  margin: 0;
  color: #666;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 12px;
}

.quantity-value {
  min-width: 40px;
  text-align: center;
  font-size: 16px;
  font-weight: bold;
}

.item-subtotal {
  min-width: 100px;
  text-align: right;
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
}

.cart-summary {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.cart-total {
  font-size: 24px;
  font-weight: bold;
  text-align: right;
  margin-bottom: 20px;
  color: #333;
}

.checkout-btn {
  width: 100%;
  height: 50px;
  font-size: 18px;
}
</style>
