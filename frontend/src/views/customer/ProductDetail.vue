<!--
 Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="product-detail-container">
    <el-button @click="goBack" class="back-btn">
      <el-icon><ArrowLeft /></el-icon>
      返回商品列表
    </el-button>

    <el-card v-if="product" class="product-detail-card" data-cy="product-detail-card">
      <div class="product-detail">
        <!-- 商品圖片區域 (預留) -->
        <div class="product-image-section">
          <div class="product-image-placeholder" data-cy="product-image">
            <el-icon :size="80"><Picture /></el-icon>
            <p>{{ product.name }}</p>
          </div>
        </div>

        <!-- 商品資訊區域 -->
        <div class="product-info-section">
          <h1 class="product-title" data-cy="product-name">{{ product.name }}</h1>

          <div class="product-price-section">
            <span class="product-price" data-cy="product-price">${{ product.price.toFixed(0) }}</span>
          </div>

          <el-divider />

          <div class="product-description-section">
            <h3>商品描述</h3>
            <p class="product-description" data-cy="product-description">{{ product.description }}</p>
          </div>

          <el-divider />

          <div class="product-stock-section">
            <span class="stock-label">庫存狀態：</span>
            <span
              :class="['stock-status', product.stock > 0 ? 'in-stock' : 'out-of-stock']"
              data-cy="product-stock"
            >
              {{ product.stock > 0 ? `有貨 (${product.stock} 件)` : '缺貨' }}
            </span>
          </div>

          <el-divider />

          <!-- 購買區域 -->
          <div class="purchase-section">
            <div class="quantity-selector">
              <span class="quantity-label">數量：</span>
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="product.stock"
                :disabled="product.stock === 0"
                data-cy="quantity-input"
              />
            </div>

            <div class="purchase-actions">
              <el-button
                type="primary"
                size="large"
                :disabled="product.stock === 0"
                @click="addToCart"
                :loading="isAdding"
                class="add-to-cart-btn"
                data-cy="add-to-cart-btn"
              >
                <el-icon><ShoppingCart /></el-icon>
                加入購物車
              </el-button>
            </div>

            <div class="total-price">
              <span>小計：</span>
              <span class="total-amount">${{ (product.price * quantity).toFixed(0) }}</span>
            </div>
          </div>

          <!-- 商品詳細資訊 -->
          <el-divider />
          <div class="product-meta">
            <div class="meta-item">
              <span class="meta-label">商品 ID：</span>
              <span>{{ product.id }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">狀態：</span>
              <el-tag :type="product.status === 'ACTIVE' ? 'success' : 'info'">
                {{ product.status === 'ACTIVE' ? '上架中' : '未上架' }}
              </el-tag>
            </div>
            <div class="meta-item" v-if="product.createdAt">
              <span class="meta-label">上架時間：</span>
              <span>{{ formatDate(product.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-empty v-else-if="!loading" description="找不到商品" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Picture, ShoppingCart } from '@element-plus/icons-vue'
import productsApi from '@/api/products'
import { useCartStore } from '@/stores/cart'
import type { Product } from '@/types'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const product = ref<Product | null>(null)
const quantity = ref(1)
const loading = ref(true)
const isAdding = ref(false)

onMounted(async () => {
  await loadProduct()
})

async function loadProduct() {
  try {
    loading.value = true
    const productId = Number(route.params.id)
    product.value = await productsApi.getProduct(productId)
  } catch (error) {
    console.error('Failed to load product:', error)
    ElMessage.error('載入商品失敗')
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  if (!product.value) return

  try {
    isAdding.value = true
    await cartStore.addToCart(product.value.id!, quantity.value)
    ElMessage.success(`已將 ${quantity.value} 件商品加入購物車`)
    quantity.value = 1
  } catch (error: any) {
    console.error('Failed to add to cart:', error)
    ElMessage.error(error.response?.data?.message || '加入購物車失敗')
  } finally {
    isAdding.value = false
  }
}

function goBack() {
  router.push('/products')
}

function formatDate(dateString: string) {
  return new Date(dateString).toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}
</script>

<style scoped>
.product-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.back-btn {
  margin-bottom: 20px;
}

.product-detail-card {
  margin-top: 20px;
}

.product-detail {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 40px;
}

/* 商品圖片區域 */
.product-image-section {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.product-image-placeholder {
  width: 100%;
  aspect-ratio: 1;
  background: #f5f7fa;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
}

.product-image-placeholder p {
  margin-top: 10px;
  font-weight: 500;
}

/* 商品資訊區域 */
.product-info-section {
  display: flex;
  flex-direction: column;
}

.product-title {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
}

.product-price-section {
  margin-bottom: 10px;
}

.product-price {
  font-size: 36px;
  font-weight: bold;
  color: #f56c6c;
}

.product-description-section h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}

.product-description {
  font-size: 15px;
  line-height: 1.8;
  color: #606266;
  margin: 0;
}

.product-stock-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stock-label {
  font-size: 16px;
  color: #606266;
  font-weight: 500;
}

.stock-status {
  font-size: 16px;
  font-weight: 600;
}

.stock-status.in-stock {
  color: #67c23a;
}

.stock-status.out-of-stock {
  color: #f56c6c;
}

/* 購買區域 */
.purchase-section {
  background: #f5f7fa;
  padding: 24px;
  border-radius: 8px;
}

.quantity-selector {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.quantity-label {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.purchase-actions {
  margin-bottom: 20px;
}

.add-to-cart-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

.total-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #dcdfe6;
}

.total-price span:first-child {
  font-size: 16px;
  color: #606266;
}

.total-amount {
  font-size: 28px;
  font-weight: bold;
  color: #f56c6c;
}

/* 商品詳細資訊 */
.product-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-label {
  font-weight: 500;
  color: #909399;
  min-width: 100px;
}

/* 響應式設計 */
@media (max-width: 768px) {
  .product-detail {
    grid-template-columns: 1fr;
  }

  .product-title {
    font-size: 24px;
  }

  .product-price {
    font-size: 28px;
  }
}
</style>
