<template>
  <div class="product-edit-container">
    <div class="header">
      <h2>編輯商品</h2>
    </div>

    <div v-loading="loading" class="form-container">
      <ProductForm
        :initial-data="productData"
        :is-edit="true"
        @submit="handleSubmit"
        @cancel="handleCancel"
      />

      <!-- Stock Adjustment Section -->
      <div class="stock-adjustment-section">
        <h3>庫存調整</h3>
        <el-button
          type="primary"
          data-cy="stock-adjustment-btn"
          @click="showStockAdjustment = true"
        >
          調整庫存
        </el-button>
      </div>
    </div>

    <!-- Stock Adjustment Modal -->
    <el-dialog
      v-model="showStockAdjustment"
      title="庫存調整"
      width="500px"
      data-cy="stock-adjustment-modal"
    >
      <el-form :model="adjustmentForm" label-width="100px">
        <el-form-item label="調整類型">
          <el-select v-model="adjustmentForm.type" data-cy="adjustment-type">
            <el-option label="增加" value="increase" />
            <el-option label="減少" value="decrease" />
          </el-select>
        </el-form-item>
        <el-form-item label="調整數量">
          <el-input-number
            v-model="adjustmentForm.quantity"
            :min="1"
            data-cy="adjustment-quantity"
          />
        </el-form-item>
        <el-form-item label="調整原因">
          <el-input
            v-model="adjustmentForm.reason"
            type="textarea"
            data-cy="adjustment-reason"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showStockAdjustment = false">取消</el-button>
        <el-button
          type="primary"
          data-cy="confirm-adjustment-btn"
          @click="handleStockAdjustment"
        >
          確認調整
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProductForm, { type ProductFormData } from '@/components/ProductForm.vue'
import { useProductsStore } from '@/stores/products'

const route = useRoute()
const router = useRouter()
const productsStore = useProductsStore()

const loading = ref(false)
const productData = ref<ProductFormData | undefined>()
const showStockAdjustment = ref(false)
const adjustmentForm = ref({
  type: 'increase',
  quantity: 1,
  reason: ''
})

onMounted(async () => {
  const productId = parseInt(route.params.id as string)
  if (productId) {
    loading.value = true
    try {
      await productsStore.loadProducts()
      const product = productsStore.getProductById(productId)
      if (product) {
        productData.value = {
          name: product.name,
          description: product.description,
          price: product.price,
          stock: product.stock,
          stockThreshold: product.stockThreshold || 5,
          status: product.status
        }
      }
    } catch (error) {
      ElMessage.error('載入商品失敗')
    } finally {
      loading.value = false
    }
  }
})

const handleSubmit = async (data: ProductFormData) => {
  const productId = parseInt(route.params.id as string)
  loading.value = true
  try {
    await productsStore.updateProduct(productId, {
      id: productId,
      ...data,
      price: data.price!,
      stock: data.stock!,
      stockThreshold: data.stockThreshold!,
      createdAt: ''
    })
    ElMessage.success('商品已成功更新')
    router.push('/admin/products')
  } catch (error) {
    ElMessage.error('更新失敗')
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  router.push('/admin/products')
}

const handleStockAdjustment = async () => {
  const productId = parseInt(route.params.id as string)
  const adjustment = adjustmentForm.value.type === 'increase'
    ? adjustmentForm.value.quantity
    : -adjustmentForm.value.quantity

  try {
    await productsStore.adjustStock(productId, adjustment, adjustmentForm.value.reason)
    ElMessage.success('庫存已調整')
    // Add data-cy attribute to success message
    setTimeout(() => {
      const msgEl = document.querySelector('.el-message--success:not([data-cy])')
      if (msgEl) msgEl.setAttribute('data-cy', 'success-message')
    }, 10)
    showStockAdjustment.value = false
    // Reload product data
    await productsStore.loadProducts()
    const product = productsStore.getProductById(productId)
    if (product && productData.value) {
      productData.value.stock = product.stock
    }
  } catch (error) {
    ElMessage.error('調整失敗')
  }
}
</script>

<style scoped>
.product-edit-container {
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

.stock-adjustment-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.stock-adjustment-section h3 {
  margin-bottom: 15px;
}
</style>
