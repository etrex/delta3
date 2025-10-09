<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="product-detail-container" v-if="product">
    <div class="header">
      <h1>商品詳情</h1>
      <div class="actions">
        <el-button @click="goBack">返回</el-button>
        <el-button type="primary" @click="editProduct">編輯商品</el-button>
      </div>
    </div>

    <!-- 商品資訊卡片 -->
    <el-card class="product-card">
      <template #header>
        <div class="card-header">
          <h3>基本資訊</h3>
          <el-tag :type="product.status === 'ACTIVE' ? 'success' : 'info'" size="large">
            {{ product.status === 'ACTIVE' ? '上架中' : '已下架' }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="商品編號">
          {{ product.id }}
        </el-descriptions-item>
        <el-descriptions-item label="商品名稱">
          {{ product.name }}
        </el-descriptions-item>
        <el-descriptions-item label="價格" :span="2">
          <span class="price">${{ product.price.toFixed(0) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="庫存">
          <span :class="{'low-stock': product.stock < 10}">
            {{ product.stock }} 件
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="狀態">
          <el-tag :type="product.status === 'ACTIVE' ? 'success' : 'info'">
            {{ product.status === 'ACTIVE' ? '上架' : '下架' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="建立時間" :span="2">
          {{ formatDateTime(product.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="商品描述" :span="2">
          <p class="description">{{ product.description || '無描述' }}</p>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 操作按鈕 -->
    <el-card class="actions-card">
      <template #header>
        <h3>商品操作</h3>
      </template>
      <div class="action-buttons">
        <el-button type="primary" @click="editProduct">編輯商品資訊</el-button>
        <el-button
          :type="product.status === 'ACTIVE' ? 'warning' : 'success'"
          @click="toggleStatus"
        >
          {{ product.status === 'ACTIVE' ? '下架商品' : '上架商品' }}
        </el-button>
      </div>
    </el-card>
  </div>

  <div v-else class="loading">
    <el-icon class="is-loading" :size="40">
      <Loading />
    </el-icon>
    <p>載入中...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import productsApi from '@/api/products'
import type { Product } from '@/types'

const route = useRoute()
const router = useRouter()
const product = ref<Product | null>(null)

onMounted(async () => {
  await loadProduct()
})

async function loadProduct() {
  try {
    const productId = Number(route.params.id)
    product.value = await productsApi.getProduct(productId)
  } catch (error) {
    console.error('Failed to load product:', error)
    ElMessage.error('載入商品失敗')
    router.push('/admin/products')
  }
}

function goBack() {
  router.push('/admin/products')
}

function editProduct() {
  if (!product.value?.id) return
  router.push(`/admin/products/${product.value.id}/edit`)
}

async function toggleStatus() {
  if (!product.value) return

  const action = product.value.status === 'ACTIVE' ? '下架' : '上架'

  try {
    await ElMessageBox.confirm(
      `確定要${action}此商品嗎？`,
      '確認操作',
      {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const newStatus = product.value.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'

    await productsApi.updateProduct(product.value.id!, {
      ...product.value,
      status: newStatus
    })

    ElMessage.success(`${action}成功`)
    await loadProduct()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to toggle status:', error)
      ElMessage.error(error.response?.data?.message || `${action}失敗`)
    }
  }
}

function formatDateTime(date: string | undefined): string {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-TW')
}
</script>

<style scoped>
.product-detail-container {
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

.header h1 {
  margin: 0;
  color: #333;
}

.actions {
  display: flex;
  gap: 12px;
}

.product-card,
.actions-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}

.price {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.low-stock {
  color: #F56C6C;
  font-weight: bold;
}

.description {
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #909399;
}

.loading p {
  margin-top: 16px;
  font-size: 16px;
}
</style>
