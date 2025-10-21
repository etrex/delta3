<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="product-management">
    <div class="header">
      <h1>商品管理</h1>
      <el-button type="primary" @click="addProduct">新增商品</el-button>
    </div>

    <!-- 商品列表 -->
    <el-table :data="products" stripe style="width: 100%" data-cy="product-list">
      <el-table-column prop="id" label="商品編號" width="100">
        <template #default="{ row }">
          <span data-cy="product-id">{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名稱">
        <template #default="{ row }">
          <span data-cy="product-name">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="價格" width="120">
        <template #default="{ row }">
          <span data-cy="product-price">${{ row.price.toFixed(0) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="庫存" width="100">
        <template #default="{ row }">
          <span data-cy="product-stock">{{ row.stock }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="狀態" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" data-cy="product-status">
            {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="viewProduct(row)">查看</el-button>
          <el-button size="small" type="primary" @click="editProduct(row)" data-cy="edit-product-btn">編輯</el-button>
          <el-button
            size="small"
            type="primary"
            @click="toggleStatus(row)"
            data-cy="toggle-status-btn"
          >
            {{ row.status === 'ACTIVE' ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import productsApi from '@/api/products'
import type { Product } from '@/types'

const router = useRouter()
const products = ref<Product[]>([])

onMounted(async () => {
  await loadProducts()
})

async function loadProducts() {
  try {
    // Admin can see all products including INACTIVE ones
    // Don't pass status parameter to get all products
    const response = await productsApi.getProducts({})
    products.value = response.content || response || []
  } catch (error) {
    console.error('Failed to load products:', error)
    ElMessage.error('載入商品失敗')
  }
}

function addProduct() {
  router.push('/admin/products/new')
}

function viewProduct(product: Product) {
  router.push(`/admin/products/${product.id}`)
}

function editProduct(product: Product) {
  router.push(`/admin/products/${product.id}/edit`)
}

async function toggleStatus(product: Product) {
  const action = product.status === 'ACTIVE' ? '下架' : '上架'

  try {
    await ElMessageBox.confirm(
      `確定要${action}此商品嗎？`,
      '確認',
      {
        confirmButtonText: '確認',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'confirm-dialog',
        confirmButtonClass: 'confirm-btn',
        cancelButtonClass: 'cancel-btn'
      }
    )

    const newStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'

    await productsApi.updateProduct(product.id!, {
      ...product,
      status: newStatus
    })

    ElMessage({
      type: 'success',
      message: `商品已${action}`,
      customClass: 'success-message',
      grouping: true
    })
    await loadProducts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to toggle status:', error)
      ElMessage.error(error.response?.data?.message || `${action}失敗`)
    }
  }
}
</script>

<style scoped>
.product-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

h1 {
  margin: 0;
  color: #333;
}
</style>
