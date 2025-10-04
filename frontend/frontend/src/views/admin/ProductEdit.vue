<template>
  <div class="product-edit-container">
    <div class="header">
      <h2>編輯商品</h2>
      <el-button
        data-cy="stock-adjustment-btn"
        @click="showStockDialog = true"
      >
        調整庫存
      </el-button>
    </div>
    <ProductForm />

    <!-- Stock Adjustment Dialog -->
    <el-dialog
      v-model="showStockDialog"
      title="調整庫存"
      width="500px"
      data-cy="stock-adjustment-modal"
    >
      <el-form v-if="product">
        <el-form-item label="當前庫存">
          <span>{{ product.stock }}</span>
        </el-form-item>
        <el-form-item label="調整類型">
          <el-select
            v-model="stockAdjustment.type"
            data-cy="adjustment-type"
          >
            <el-option label="增加" value="increase" />
            <el-option label="減少" value="decrease" />
          </el-select>
        </el-form-item>
        <el-form-item label="調整數量">
          <el-input
            v-model.number="stockAdjustment.quantity"
            type="number"
            min="1"
            data-cy="adjustment-quantity"
          />
        </el-form-item>
        <el-form-item label="調整原因">
          <el-input
            v-model="stockAdjustment.reason"
            type="textarea"
            data-cy="adjustment-reason"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showStockDialog = false">取消</el-button>
        <el-button
          type="primary"
          data-cy="confirm-adjustment-btn"
          @click="confirmStockAdjustment"
        >
          確認調整
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import ProductForm from '@/components/ProductForm.vue'
import { useProductsStore, type Product, type StockAdjustment } from '@/stores/products'
import { showSuccessMessage, showErrorMessage } from '@/utils/message'

const route = useRoute()
const productsStore = useProductsStore()

const product = ref<Product | null>(null)
const showStockDialog = ref(false)
const stockAdjustment = ref<StockAdjustment>({
  type: 'increase',
  quantity: 1,
  reason: ''
})

onMounted(async () => {
  const productId = route.params.id
  if (productId) {
    product.value = productsStore.getProductById(Number(productId)) || null
  }
})

const confirmStockAdjustment = async () => {
  if (!product.value) return

  const result = await productsStore.adjustStock(product.value.id, stockAdjustment.value)

  if (result.success) {
    ElMessage.success(result.message)
    showSuccessMessage(result.message)
    showStockDialog.value = false
    // Reload product data
    product.value = productsStore.getProductById(product.value.id) || null
  } else {
    ElMessage.error(result.message)
    showErrorMessage(result.message)
  }
}
</script>

<style scoped>
.product-edit-container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

h2 {
  margin: 0;
}
</style>
