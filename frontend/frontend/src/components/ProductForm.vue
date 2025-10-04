<template>
  <div class="product-form" data-cy="product-form">
    <div v-if="formLoading" data-cy="form-loading">載入中...</div>
    <el-form
      v-else
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item label="商品名稱" prop="name">
        <el-input
          v-model="formData.name"
          data-cy="product-name-input"
          placeholder="請輸入商品名稱"
        />
      </el-form-item>

      <el-form-item label="商品描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="4"
          data-cy="product-description-input"
          placeholder="請輸入商品描述"
        />
      </el-form-item>

      <el-form-item label="價格" prop="price">
        <el-input
          v-model="formData.price"
          data-cy="product-price-input"
          placeholder="請輸入價格"
        />
      </el-form-item>

      <el-form-item label="庫存數量" prop="stock">
        <el-input
          v-model="formData.stock"
          data-cy="product-stock-input"
          placeholder="請輸入庫存數量"
        />
      </el-form-item>

      <el-form-item label="類別" prop="category">
        <el-select
          v-model="formData.category"
          data-cy="product-category-select"
          placeholder="請選擇類別"
        >
          <el-option label="電子產品" value="電子產品" />
          <el-option label="食品" value="食品" />
          <el-option label="奢侈品" value="奢侈品" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>

      <el-form-item label="商品狀態" prop="status">
        <el-select
          v-model="formData.status"
          data-cy="product-status-select"
        >
          <el-option label="上架" value="ACTIVE" />
          <el-option label="下架" value="INACTIVE" />
        </el-select>
      </el-form-item>

      <el-form-item label="庫存警告門檻">
        <el-input
          v-model.number="formData.stockThreshold"
          type="number"
          min="0"
          data-cy="stock-threshold-input"
          placeholder="低於此數量時警告"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          :loading="loading"
          data-cy="save-product-btn"
          @click="handleSubmit"
        >
          {{ isEdit ? '更新商品' : '建立商品' }}
        </el-button>
        <el-button
          data-cy="cancel-btn"
          @click="handleCancel"
        >
          取消
        </el-button>
      </el-form-item>
    </el-form>

    <!-- Cancel Confirmation Dialog -->
    <el-dialog
      v-model="showCancelDialog"
      title="確認取消"
      width="400px"
      data-cy="confirm-dialog"
    >
      <p>確定要取消嗎？未保存的變更將會遺失。</p>
      <template #footer>
        <el-button @click="showCancelDialog = false">繼續編輯</el-button>
        <el-button
          type="primary"
          data-cy="confirm-cancel-btn"
          @click="confirmCancel"
        >
          確認取消
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useProductsStore, type ProductFormData } from '@/stores/products'
import { showSuccessMessage, showErrorMessage } from '@/utils/message'

const router = useRouter()
const route = useRoute()
const productsStore = useProductsStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const formLoading = ref(!!route.params.id) // Start loading if editing
const showCancelDialog = ref(false)
const isEdit = ref(!!route.params.id)

const formData = reactive<ProductFormData>({
  name: '',
  price: '' as any,
  description: '',
  stock: '' as any,
  status: 'ACTIVE',
  category: '電子產品',
  stockThreshold: 5
})

const clearError = (errorId: string) => {
  const el = document.querySelector(`[data-cy=${errorId}]`)
  if (el) el.remove()
}

const showError = (inputSelector: string, errorId: string, message: string) => {
  clearError(errorId)
  const errorEl = document.createElement('div')
  errorEl.setAttribute('data-cy', errorId)
  errorEl.textContent = message
  errorEl.style.color = '#f56c6c'
  errorEl.style.fontSize = '12px'
  errorEl.style.marginTop = '4px'
  const inputEl = document.querySelector(`[data-cy=${inputSelector}]`)
  if (inputEl?.parentElement) {
    inputEl.parentElement.appendChild(errorEl)
  }
}

const rules: FormRules = {
  name: [
    {
      required: true,
      message: '商品名稱為必填',
      trigger: 'blur',
      validator: (rule, value, callback) => {
        clearError('name-error')
        if (!value) {
          showError('product-name-input', 'name-error', '商品名稱為必填')
          callback(new Error('商品名稱為必填'))
        } else {
          callback()
        }
      }
    }
  ],
  price: [
    {
      required: true,
      trigger: 'blur',
      validator: (rule, value, callback) => {
        clearError('price-error')

        if (value === '' || value === null || value === undefined) {
          showError('product-price-input', 'price-error', '價格為必填')
          callback(new Error('價格為必填'))
          return
        }

        const numValue = typeof value === 'string' ? parseFloat(value) : value
        if (isNaN(numValue)) {
          showError('product-price-input', 'price-error', '請輸入有效的價格')
          callback(new Error('請輸入有效的價格'))
          return
        }
        if (numValue <= 0) {
          showError('product-price-input', 'price-error', '價格必須大於0')
          callback(new Error('價格必須大於0'))
          return
        }
        callback()
      }
    }
  ],
  stock: [
    {
      required: true,
      trigger: 'blur',
      validator: (rule, value, callback) => {
        clearError('stock-error')

        if (value === '' || value === null || value === undefined) {
          showError('product-stock-input', 'stock-error', '庫存數量為必填')
          callback(new Error('庫存數量為必填'))
          return
        }

        const numValue = typeof value === 'string' ? parseInt(value) : value
        if (numValue < 0) {
          showError('product-stock-input', 'stock-error', '庫存不能為負數')
          callback(new Error('庫存不能為負數'))
          return
        }
        callback()
      }
    }
  ]
}

onMounted(async () => {
  const productId = route.params.id
  if (productId) {
    try {
      // Ensure products are loaded first
      await productsStore.loadProducts()
      const product = productsStore.getProductById(Number(productId))
      if (product) {
        // Explicitly set each field to ensure reactivity
        formData.name = product.name
        formData.price = product.price
        formData.description = product.description
        formData.stock = product.stock
        formData.status = product.status
        formData.category = product.category
        formData.stockThreshold = product.stockThreshold || 5

        // Wait for DOM to update before showing form
        await nextTick()
      }
    } finally {
      formLoading.value = false
    }
  }
})

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // Convert string values to numbers
        const productData = {
          ...formData,
          price: typeof formData.price === 'string' ? parseFloat(formData.price) : formData.price,
          stock: typeof formData.stock === 'string' ? parseInt(formData.stock) : formData.stock
        }

        let result
        if (isEdit.value) {
          result = await productsStore.updateProduct(Number(route.params.id), productData)
        } else {
          result = await productsStore.createProduct(productData)
        }

        if (result.success) {
          ElMessage.success(result.message)
          showSuccessMessage(result.message)
          // Reset pagination to page 1
          productsStore.setPagination(1)
          // Wait for the store to be fully updated
          await new Promise(resolve => setTimeout(resolve, 100))
          // Navigate to list page
          await router.push('/admin/products')
        } else {
          ElMessage.error(result.message)
          showErrorMessage(result.message)
        }
      } finally {
        loading.value = false
      }
    }
  })
}

const handleCancel = () => {
  showCancelDialog.value = true
}

const confirmCancel = () => {
  showCancelDialog.value = false
  router.push('/admin/products')
}
</script>

<style scoped>
.product-form {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}
</style>
