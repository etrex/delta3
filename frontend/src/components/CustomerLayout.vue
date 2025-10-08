<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-content">
        <h2 class="site-title" @click="goToHome">卡米購</h2>
        <div class="header-actions">
          <router-link to="/products" class="nav-link">
            商品列表
          </router-link>
          <router-link to="/orders" class="nav-link">
            我的訂單
          </router-link>

          <div class="cart-icon" data-cy="cart-icon" @click="showCartDrawer">
            <el-badge :value="totalItems" :hidden="totalItems === 0">
              <el-icon :size="24"><ShoppingCart /></el-icon>
            </el-badge>
            <span data-cy="cart-count" class="cart-count-text">{{ totalItems }}</span>
          </div>

          <span class="username">{{ authStore.user?.username }}</span>
          <el-button data-cy="logout-btn" @click="handleLogout" size="small">登出</el-button>
        </div>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>

    <!-- 購物車抽屜 -->
    <el-drawer
      v-model="cartDrawerVisible"
      title="購物車"
      direction="rtl"
      size="400px"
      data-cy="cart-drawer"
    >
      <div v-if="items.length === 0" class="empty-cart" data-cy="cart-empty-message">
        購物車是空的
      </div>
      <div v-else>
        <div v-for="item in items" :key="item.id" class="cart-item" data-cy="cart-item">
          <div class="item-info">
            <div class="item-name" data-cy="item-name">{{ item.productName }}</div>
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
              size="small"
              type="danger"
              @click="confirmRemove(item.id!)"
            >移除</el-button>
          </div>
        </div>

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
    </el-drawer>

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
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { ShoppingCart } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const cartDrawerVisible = ref(false)
const confirmDialogVisible = ref(false)
const itemToRemove = ref<number | null>(null)

const items = computed(() => cartStore.items)
const totalAmount = computed(() => cartStore.totalAmount)
const totalItems = computed(() => cartStore.totalItems)

onMounted(async () => {
  await cartStore.loadCart()
})

// Watch for cart drawer opening to reload cart
watch(cartDrawerVisible, async (newValue) => {
  if (newValue) {
    await cartStore.loadCart()
  }
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function showCartDrawer() {
  cartDrawerVisible.value = true
}

async function increaseQuantity(itemId: number) {
  const item = items.value.find(i => i.id === itemId)
  if (item) {
    try {
      await cartStore.updateCartItem(itemId, item.quantity + 1)
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
      await cartStore.updateCartItem(itemId, item.quantity - 1)
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
      await cartStore.removeCartItem(itemToRemove.value)
      ElMessage.success('已移除商品')
    } catch (error) {
      console.error('Failed to remove item:', error)
      ElMessage.error('移除商品失敗')
    }
    itemToRemove.value = null
  }
  confirmDialogVisible.value = false
}

function goToCheckout() {
  cartDrawerVisible.value = false
  router.push('/checkout')
}

function goToHome() {
  router.push('/products')
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.header {
  background-color: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-link {
  color: white;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.nav-link:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.cart-icon {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.cart-count-text {
  font-size: 14px;
}

.site-title {
  cursor: pointer;
  transition: opacity 0.3s;
}

.site-title:hover {
  opacity: 0.8;
}

.username {
  font-size: 14px;
}

.main-content {
  padding: 20px;
  background-color: #f5f5f5;
}

.empty-cart {
  text-align: center;
  padding: 40px;
  color: #999;
}

.cart-item {
  padding: 16px;
  border-bottom: 1px solid #eee;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-name {
  font-weight: bold;
  font-size: 16px;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity-value {
  min-width: 30px;
  text-align: center;
}

.item-subtotal {
  font-weight: bold;
  color: #409EFF;
}

.cart-total {
  padding: 20px;
  font-size: 18px;
  font-weight: bold;
  text-align: right;
  border-top: 2px solid #409EFF;
}

.checkout-btn {
  width: 100%;
  margin-top: 16px;
}
</style>
