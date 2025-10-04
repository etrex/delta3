<template>
  <div class="checkout-container">
    <div class="header">
      <h2>結帳</h2>
    </div>

    <div class="checkout-content" data-cy="checkout-page">
      <p>結帳頁面（待實作）</p>
      <div class="cart-items" data-cy="cart-items">
        <div
          v-for="item in cartStore.items"
          :key="item.product.id"
          class="cart-item"
        >
          <p>{{ item.product.name }} x {{ item.quantity }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { showErrorMessage } from '@/utils/message'

const router = useRouter()
const cartStore = useCartStore()

onMounted(() => {
  // 檢查購物車是否為空
  if (cartStore.items.length === 0) {
    showErrorMessage('購物車不能為空')
    router.push('/cart')
  }
})
</script>

<style scoped>
.checkout-container {
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

.checkout-content {
  background: white;
  padding: 30px;
  border-radius: 8px;
}
</style>
