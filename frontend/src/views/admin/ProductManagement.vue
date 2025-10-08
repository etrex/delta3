<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="product-management">
    <div class="header">
      <h1>商品管理</h1>
      <el-button type="primary" @click="showAddDialog">新增商品</el-button>
    </div>

    <!-- 商品列表 -->
    <el-table :data="products" stripe style="width: 100%">
      <el-table-column prop="id" label="商品編號" width="100" />
      <el-table-column prop="name" label="商品名稱" width="200" />
      <el-table-column prop="description" label="商品描述" />
      <el-table-column prop="price" label="價格" width="120">
        <template #default="{ row }">
          ${{ row.price.toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column prop="stock" label="庫存" width="100" />
      <el-table-column prop="status" label="狀態" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="建立時間" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showEditDialog(row)">編輯</el-button>
          <el-button
            size="small"
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/編輯商品對話框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增商品' : '編輯商品'"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="商品名稱" prop="name">
          <el-input v-model="form.name" placeholder="請輸入商品名稱" />
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="請輸入商品描述"
          />
        </el-form-item>

        <el-form-item label="價格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            :step="0.01"
            placeholder="請輸入價格"
          />
        </el-form-item>

        <el-form-item label="庫存" prop="stock">
          <el-input-number
            v-model="form.stock"
            :min="0"
            placeholder="請輸入庫存"
          />
        </el-form-item>

        <el-form-item label="庫存門檻" prop="stockThreshold">
          <el-input-number
            v-model="form.stockThreshold"
            :min="0"
            placeholder="低於此數量時提醒"
          />
        </el-form-item>

        <el-form-item label="狀態" prop="status">
          <el-select v-model="form.status" placeholder="請選擇狀態">
            <el-option label="上架" value="ACTIVE" />
            <el-option label="下架" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="isSubmitting" @click="handleSubmit">
          確認
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import productsApi from '@/api/products'
import type { Product } from '@/types'

const products = ref<Product[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const isSubmitting = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  description: '',
  price: 0,
  stock: 0,
  stockThreshold: 5,
  status: 'ACTIVE'
})

const rules = {
  name: [
    { required: true, message: '請輸入商品名稱', trigger: 'blur' },
    { max: 100, message: '商品名稱不能超過100個字元', trigger: 'blur' }
  ],
  description: [
    { max: 255, message: '商品描述不能超過255個字元', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '請輸入價格', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '價格必須大於0', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '請輸入庫存', trigger: 'blur' },
    { type: 'number', min: 0, message: '庫存不能為負數', trigger: 'blur' }
  ],
  stockThreshold: [
    { type: 'number', min: 0, message: '庫存門檻不能為負數', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '請選擇狀態', trigger: 'change' }
  ]
}

onMounted(async () => {
  await loadProducts()
})

async function loadProducts() {
  try {
    // Admin can see all products including INACTIVE ones
    const response = await productsApi.getProducts()
    products.value = response.content || response || []
  } catch (error) {
    console.error('Failed to load products:', error)
    ElMessage.error('載入商品失敗')
  }
}

function showAddDialog() {
  dialogMode.value = 'add'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function showEditDialog(product: Product) {
  dialogMode.value = 'edit'
  editingId.value = product.id!

  // Fill form with product data
  form.name = product.name
  form.description = product.description || ''
  form.price = product.price
  form.stock = product.stock
  form.stockThreshold = product.stockThreshold || 5
  form.status = product.status

  dialogVisible.value = true
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.price = 0
  form.stock = 0
  form.stockThreshold = 5
  form.status = 'ACTIVE'
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    isSubmitting.value = true

    const productData: Product = {
      name: form.name,
      description: form.description,
      price: form.price,
      stock: form.stock,
      stockThreshold: form.stockThreshold,
      status: form.status
    }

    if (dialogMode.value === 'add') {
      await productsApi.createProduct(productData)
      ElMessage.success('商品新增成功')
    } else {
      await productsApi.updateProduct(editingId.value!, productData)
      ElMessage.success('商品更新成功')
    }

    dialogVisible.value = false
    await loadProducts()
  } catch (error: any) {
    console.error('Failed to save product:', error)
    ElMessage.error(error.response?.data?.message || '操作失敗')
  } finally {
    isSubmitting.value = false
  }
}

async function toggleStatus(product: Product) {
  const action = product.status === 'ACTIVE' ? '下架' : '上架'

  try {
    await ElMessageBox.confirm(
      `確定要${action}商品「${product.name}」嗎？`,
      '確認',
      {
        confirmButtonText: '確認',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const newStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'

    await productsApi.updateProduct(product.id!, {
      ...product,
      status: newStatus
    })

    ElMessage.success(`${action}成功`)
    await loadProducts()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to toggle status:', error)
      ElMessage.error(error.response?.data?.message || `${action}失敗`)
    }
  }
}

function formatDate(dateString: string): string {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
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
