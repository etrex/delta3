<template>
  <el-dialog
    :model-value="visible"
    :title="product?.name"
    width="600px"
    data-cy="product-detail-modal"
    @update:model-value="handleClose"
    @close="handleClose"
  >
    <div v-if="product" class="product-detail">
      <div class="product-image">
        <img
          :src="product.image"
          :alt="product.name"
          data-cy="product-image"
          @error="handleImageError"
        />
      </div>

      <div class="product-info">
        <h3 data-cy="product-name">{{ product.name }}</h3>
        <p class="price" data-cy="product-price">${{ product.price }}</p>
        <p class="description" data-cy="product-description">{{ product.description }}</p>
        <p class="stock" data-cy="product-stock">庫存: {{ product.stock }}</p>

        <div class="quantity-section">
          <label>數量:</label>
          <el-input-number
            v-model="quantity"
            :min="1"
            :max="product.stock"
            data-cy="quantity-input"
          />
        </div>

        <div class="actions">
          <el-button
            type="primary"
            :disabled="product.stock === 0 || loading"
            :loading="loading"
            data-cy="add-to-cart-btn"
            @click="handleAddToCart"
          >
            {{ product.stock === 0 ? '缺貨' : '加入購物車' }}
          </el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import type { Product } from '@/stores/products'
import { showSuccessMessage, showErrorMessage } from '@/utils/message'

interface Props {
  visible: boolean
  product: Product | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
  'update:visible': [value: boolean]
}>()

const cartStore = useCartStore()
const quantity = ref(1)
const loading = ref(false)

// Reset quantity when product changes
watch(() => props.product, () => {
  quantity.value = 1
})

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.src = '/images/placeholder.jpg'
}

const handleClose = () => {
  emit('close')
}

const handleAddToCart = async () => {
  if (!props.product) return

  loading.value = true
  try {
    const result = await cartStore.addToCart(props.product.id, quantity.value)

    if (result.success) {
      ElMessage.success(result.message)
      showSuccessMessage(result.message)
      emit('close')
    } else {
      ElMessage.error(result.message)
      showErrorMessage(result.message)

      // 如果庫存不足且有可用庫存，提供智能調整
      if (result.errorType === 'INSUFFICIENT_STOCK' && result.availableStock && result.availableStock > 0) {
        quantity.value = result.availableStock
        ElMessage.info(`已自動調整為最大可用數量: ${result.availableStock}`)
      }
      // 保持對話框開啟，讓用戶可以調整數量
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.product-detail {
  display: flex;
  gap: 20px;
}

.product-image {
  flex: 1;
}

.product-image img {
  width: 100%;
  height: 300px;
  object-fit: cover;
  border-radius: 8px;
}

.product-info {
  flex: 1;
}

.product-info h3 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #333;
}

.price {
  font-size: 20px;
  font-weight: bold;
  color: #e74c3c;
  margin: 10px 0;
}

.description {
  color: #666;
  line-height: 1.6;
  margin: 15px 0;
}

.stock {
  color: #27ae60;
  font-weight: 500;
  margin: 10px 0;
}

.quantity-section {
  margin: 20px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.quantity-section label {
  font-weight: 500;
}

.actions {
  margin-top: 20px;
}

.actions .el-button {
  width: 100%;
}

@media (max-width: 768px) {
  .product-detail {
    flex-direction: column;
  }

  .product-image img {
    height: 200px;
  }
}
</style>