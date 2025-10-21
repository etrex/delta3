<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="product-edit-container">
    <div class="header">
      <h1>{{ isEditMode ? '編輯商品' : '新增商品' }}</h1>
      <el-button @click="goBack">返回</el-button>
    </div>

    <el-card>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" data-cy="product-form">
        <el-form-item label="商品名稱" prop="name">
          <el-input v-model="form.name" placeholder="請輸入商品名稱" data-cy="product-name-input" />
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="請輸入商品描述"
            data-cy="product-description-input"
          />
        </el-form-item>

        <el-form-item label="價格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="0"
            :step="1"
            placeholder="請輸入價格"
            data-cy="product-price-input"
          />
        </el-form-item>

        <el-form-item label="庫存" prop="stock">
          <el-input-number
            v-model="form.stock"
            :min="0"
            placeholder="請輸入庫存"
            data-cy="product-stock-input"
          />
        </el-form-item>

        <el-form-item label="狀態" prop="status">
          <el-select v-model="form.status" placeholder="請選擇狀態" data-cy="product-status-select">
            <el-option label="上架" value="ACTIVE" />
            <el-option label="下架" value="INACTIVE" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button @click="goBack" data-cy="cancel-btn">取消</el-button>
          <el-button type="primary" :loading="isSubmitting" @click="handleSubmit" data-cy="save-product-btn">
            確認
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import productsApi from '@/api/products'
import type { Product } from '@/types'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const isSubmitting = ref(false)

const isEditMode = computed(() => !!route.params.id)

const form = reactive({
  name: '',
  description: '',
  price: 0,
  stock: 0,
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
  status: [
    { required: true, message: '請選擇狀態', trigger: 'change' }
  ]
}

onMounted(async () => {
  if (isEditMode.value) {
    await loadProduct()
  }
})

async function loadProduct() {
  try {
    const productId = Number(route.params.id)
    const product = await productsApi.getProduct(productId)

    form.name = product.name
    form.description = product.description || ''
    form.price = product.price
    form.stock = product.stock
    form.status = product.status
  } catch (error) {
    console.error('Failed to load product:', error)
    ElMessage.error('載入商品失敗')
    router.push('/admin/products')
  }
}

function goBack() {
  router.back()
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
      status: form.status
    }

    if (isEditMode.value) {
      const productId = Number(route.params.id)
      await productsApi.updateProduct(productId, productData)
      ElMessage.success('商品更新成功')
      // 編輯模式保存後返回上一頁
      router.back()
    } else {
      await productsApi.createProduct(productData)
      ElMessage({
        type: 'success',
        message: '商品已成功創建',
        customClass: 'success-message',
        grouping: true
      })
      // 新增模式保存後跳回商品列表
      router.push('/admin/products')
    }
  } catch (error: any) {
    console.error('Failed to save product:', error)
    ElMessage.error(error.response?.data?.message || '操作失敗')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.product-edit-container {
  max-width: 800px;
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
</style>
