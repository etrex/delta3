<template>
  <div class="products-container">
    <h1>商品列表</h1>

    <div class="products-grid">
      <el-card
        v-for="product in products"
        :key="product.id"
        class="product-card"
        data-cy="product-card"
        @click="viewProductDetail(product.id)"
      >
        <div class="product-info">
          <h3 class="product-name" data-cy="product-name">{{ product.name }}</h3>
          <p class="product-description">{{ product.description }}</p>
          <div class="product-price" data-cy="product-price">${{ product.price.toFixed(0) }}</div>
          <div class="product-stock" data-cy="product-stock">庫存: {{ product.stock }}</div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import productsApi from '@/api/products'
import type { Product } from '@/types'

const router = useRouter()

const products = ref<Product[]>([])

onMounted(async () => {
  await loadProducts()
})

async function loadProducts() {
  try {
    const data = await productsApi.getProducts({ status: 'ACTIVE' })
    products.value = data.content || data
  } catch (error) {
    console.error('Failed to load products:', error)
    ElMessage.error('載入商品失敗')
  }
}

function viewProductDetail(productId: number) {
  router.push(`/products/${productId}`)
}
</script>

<style scoped>
.products-container {
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 30px;
  color: #333;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.product-card {
  transition: transform 0.2s;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-name {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.product-description {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.product-price {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.product-stock {
  font-size: 14px;
  color: #909399;
}
</style>
