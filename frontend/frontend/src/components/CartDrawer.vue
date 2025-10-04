<template>
  <el-drawer
    v-model="visible"
    title="購物車"
    direction="rtl"
    size="400px"
    data-cy="cart-drawer"
  >
    <!-- 空購物車 -->
    <div v-if="cartStore.items.length === 0" class="empty-cart">
      <el-empty data-cy="cart-empty-message" description="購物車是空的">
        <el-button type="primary" @click="goToProducts">前往商品頁面</el-button>
      </el-empty>
    </div>

    <!-- 購物車商品列表 -->
    <div v-else class="cart-drawer-content">
      <div class="cart-items">
        <div
          v-for="item in cartStore.items"
          :key="item.product.id"
          class="cart-item"
          data-cy="cart-item"
        >
          <div class="item-image">
            <img :src="item.product.image" :alt="item.product.name" />
          </div>

          <div class="item-details">
            <h4 data-cy="item-name">{{ item.product.name }}</h4>
            <p class="item-price">${{ item.product.price }}</p>

            <div class="item-quantity-controls">
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

            <p class="item-subtotal" data-cy="item-subtotal">${{ (item.product.price * item.quantity).toFixed(2) }}</p>
          </div>

          <el-button
            type="danger"
            size="small"
            :icon="Delete"
            circle
            data-cy="remove-item-btn"
            @click="showRemoveDialog(item.product.id, item.product.name)"
          />
        </div>
      </div>

      <!-- 購物車總計 -->
      <div class="cart-summary">
        <div class="summary-row">
          <span>總計：</span>
          <span data-cy="cart-total">${{ cartStore.totalPrice.toFixed(2) }}</span>
        </div>
        <el-button
          type="primary"
          size="large"
          data-cy="view-cart-btn"
          @click="goToCart"
          style="width: 100%; margin-bottom: 10px;"
        >
          查看購物車
        </el-button>
        <el-button
          type="success"
          size="large"
          data-cy="checkout-btn"
          @click="goToCheckout"
          style="width: 100%;"
        >
          前往結帳
        </el-button>
      </div>
    </div>

    <!-- 確認移除對話框 -->
    <el-dialog
      v-model="showConfirmRemove"
      title="確認移除"
      width="300px"
      data-cy="confirm-dialog"
      append-to-body
    >
      <p>確定要移除「{{ removeItemName }}」嗎？</p>
      <template #footer>
        <el-button @click="showConfirmRemove = false">取消</el-button>
        <el-button type="danger" data-cy="confirm-btn" @click="confirmRemove">
          確認移除
        </el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Minus, Delete } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { showSuccessMessage } from '@/utils/message'

interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const cartStore = useCartStore()

const visible = ref(props.modelValue)
const showConfirmRemove = ref(false)
const removeItemId = ref<number | null>(null)
const removeItemName = ref('')

// Sync with parent
const updateVisible = (value: boolean) => {
  visible.value = value
  emit('update:modelValue', value)
}

// Watch for prop changes
import { watch } from 'vue'
watch(() => props.modelValue, (newValue) => {
  visible.value = newValue
})

watch(visible, (newValue) => {
  emit('update:modelValue', newValue)
})

const goToProducts = () => {
  updateVisible(false)
  router.push('/products')
}

const goToCart = () => {
  updateVisible(false)
  router.push('/cart')
}

const goToCheckout = () => {
  if (cartStore.items.length === 0) {
    ElMessage.warning('購物車不能為空')
    return
  }
  updateVisible(false)
  router.push('/checkout')
}

const increaseQuantity = (productId: number, maxStock: number) => {
  const item = cartStore.getCartItem(productId)
  if (item && item.quantity < maxStock) {
    try {
      cartStore.updateQuantity(productId, item.quantity + 1)
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
    showSuccessMessage('已移除商品')
    showConfirmRemove.value = false
    removeItemId.value = null
    removeItemName.value = ''
  }
}
</script>

<style scoped>
.empty-cart {
  padding: 40px 20px;
  text-align: center;
}

.cart-drawer-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.cart-items {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 20px;
}

.cart-item {
  display: flex;
  gap: 12px;
  padding: 15px 0;
  border-bottom: 1px solid #e0e0e0;
  position: relative;
}

.item-image {
  width: 60px;
  height: 60px;
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
  min-width: 0;
}

.item-details h4 {
  margin: 0 0 5px 0;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  font-size: 14px;
  font-weight: bold;
  color: #e74c3c;
  margin: 5px 0;
}

.item-quantity-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
}

.quantity-display {
  min-width: 30px;
  text-align: center;
  font-size: 14px;
  font-weight: bold;
}

.item-subtotal {
  font-size: 14px;
  font-weight: bold;
  color: #2c3e50;
  margin: 5px 0 0 0;
}

.cart-summary {
  border-top: 2px solid #e0e0e0;
  padding-top: 15px;
  margin-top: auto;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 18px;
  font-weight: bold;
}
</style>
