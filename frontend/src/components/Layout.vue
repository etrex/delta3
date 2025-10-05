<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-content">
        <h2>訂單管理系統</h2>
        <div class="header-actions">
          <router-link v-if="authStore.isCustomer" to="/products" class="nav-link">
            商品列表
          </router-link>
          <router-link v-if="authStore.isCustomer" to="/orders" class="nav-link">
            我的訂單
          </router-link>
          <router-link v-if="authStore.isAdmin" to="/admin/orders" class="nav-link">
            訂單管理
          </router-link>
          <router-link v-if="authStore.isAdmin" to="/admin/shipping" class="nav-link">
            出貨管理
          </router-link>

          <div v-if="authStore.isCustomer" class="cart-icon" data-cy="cart-icon" @click="showCartDrawer">
            <el-badge :value="cartStore.totalItems" :hidden="cartStore.totalItems === 0">
              <el-icon :size="24"><ShoppingCart /></el-icon>
            </el-badge>
            <span data-cy="cart-count" class="cart-count-text">{{ cartStore.totalItems }}</span>
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
      <div v-if="cartStore.items.length === 0" class="empty-cart" data-cy="cart-empty-message">
        購物車是空的
      </div>
      <div v-else>
        <div v-for="item in cartStore.items" :key="item.product.id" class="cart-item" data-cy="cart-item">
          <div class="item-info">
            <div class="item-name" data-cy="item-name">{{ item.product.name }}</div>
            <div class="item-quantity">
              <el-button
                data-cy="quantity-decrease-btn"
                size="small"
                @click="decreaseQuantity(item.product.id!)"
                :disabled="item.quantity <= 1"
              >-</el-button>
              <span data-cy="item-quantity" class="quantity-value">{{ item.quantity }}</span>
              <el-button
                data-cy="quantity-increase-btn"
                size="small"
                @click="increaseQuantity(item.product.id!)"
              >+</el-button>
            </div>
            <div class="item-subtotal" data-cy="item-subtotal">
              ${{ (item.product.price * item.quantity).toFixed(2) }}
            </div>
            <el-button
              data-cy="remove-item-btn"
              size="small"
              type="danger"
              @click="confirmRemove(item.product.id!)"
            >移除</el-button>
          </div>
        </div>

        <div class="cart-total" data-cy="cart-total">
          總計: ${{ cartStore.totalAmount.toFixed(2) }}
        </div>

        <el-button
          type="primary"
          data-cy="checkout-from-drawer-btn"
          class="checkout-btn"
          @click="goToCart"
        >前往結帳</el-button>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { ShoppingCart } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const cartDrawerVisible = ref(false)
const confirmDialogVisible = ref(false)
const itemToRemove = ref<number | null>(null)

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function showCartDrawer() {
  cartDrawerVisible.value = true
}

function increaseQuantity(productId: number) {
  const item = cartStore.items.find(i => i.product.id === productId)
  if (item) {
    cartStore.updateQuantity(productId, item.quantity + 1)
  }
}

function decreaseQuantity(productId: number) {
  const item = cartStore.items.find(i => i.product.id === productId)
  if (item && item.quantity > 1) {
    cartStore.updateQuantity(productId, item.quantity - 1)
  }
}

function confirmRemove(productId: number) {
  itemToRemove.value = productId
  confirmDialogVisible.value = true
}

function removeItem() {
  if (itemToRemove.value !== null) {
    cartStore.removeFromCart(itemToRemove.value)
    itemToRemove.value = null
  }
  confirmDialogVisible.value = false
}

function goToCart() {
  cartDrawerVisible.value = false
  router.push('/cart')
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
