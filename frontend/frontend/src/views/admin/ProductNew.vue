<template>
  <div class="product-new-container">
    <div class="header">
      <h2>新增商品</h2>
    </div>

    <div v-loading="loading" class="form-container">
      <ProductForm
        @submit="handleSubmit"
        @cancel="handleCancel"
      />
    </div>

    <!-- Confirm Cancel Dialog -->
    <el-dialog
      v-model="showConfirmDialog"
      title="確認取消"
      width="400px"
      data-cy="confirm-dialog"
    >
      <p>確定要取消新增商品嗎？未儲存的資料將會遺失。</p>
      <template #footer>
        <el-button @click="showConfirmDialog = false">繼續編輯</el-button>
        <el-button type="primary" data-cy="confirm-cancel-btn" @click="confirmCancel">
          確認取消
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProductForm, { type ProductFormData } from '@/components/ProductForm.vue'
import { useProductsStore } from '@/stores/products'

const router = useRouter()
const productsStore = useProductsStore()

const loading = ref(false)
const showConfirmDialog = ref(false)

const handleSubmit = async (data: ProductFormData) => {
  loading.value = true
  try {
    await productsStore.createProduct({
      name: data.name,
      price: data.price!,
      description: data.description,
      stock: data.stock!,
      status: data.status as 'ACTIVE' | 'INACTIVE',
      category: '測試類別',
      stockThreshold: data.stockThreshold || 5
    })

    ElMessage.success('商品已成功創建')
    // Add data-cy attribute to success message
    setTimeout(() => {
      const msgEl = document.querySelector('.el-message--success:not([data-cy])')
      if (msgEl) msgEl.setAttribute('data-cy', 'success-message')
    }, 10)

    router.push('/admin/products')
  } catch (error) {
    ElMessage.error('創建失敗')
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  showConfirmDialog.value = true
}

const confirmCancel = () => {
  showConfirmDialog.value = false
  router.push('/admin/products')
}
</script>

<style scoped>
.product-new-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.header {
  margin-bottom: 20px;
}

.form-container {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
</style>
