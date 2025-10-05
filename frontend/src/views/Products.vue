<template>
  <div class="products-container">
    <h1>商品列表</h1>

    <div class="products-grid">
      <el-card
        v-for="product in products"
        :key="product.id"
        class="product-card"
        data-cy="product-card"
      >
        <div class="product-info">
          <h3 class="product-name" data-cy="product-name">{{ product.name }}</h3>
          <p class="product-description">{{ product.description }}</p>
          <div class="product-price" data-cy="product-price">${{ product.price.toFixed(2) }}</div>
          <div class="product-stock" data-cy="product-stock">庫存: {{ product.stock }}</div>
          <el-button
            v-if="product.stock > 0"
            type="primary"
            data-cy="add-to-cart-btn"
            @click="showAddToCartModal(product)"
            class="add-to-cart-btn"
          >加入購物車</el-button>
          <el-button v-else disabled>缺貨</el-button>
        </div>
      </el-card>
    </div>

    <!-- 加入購物車數量選擇 Modal -->
    <el-dialog
      v-model="quantityModalVisible"
      title="選擇數量"
      width="400px"
      data-cy="quantity-modal"
    >
      <div v-if="selectedProduct">
        <p>商品：{{ selectedProduct.name }}</p>
        <p>價格：${{ selectedProduct.price.toFixed(2) }}</p>
        <p>可用庫存：{{ selectedProduct.stock }}</p>

        <el-form :model="form" :rules="rules" ref="formRef">
          <el-form-item label="數量" prop="quantity">
            <el-input-number
              v-model="form.quantity"
              :min="1"
              data-cy="quantity-input"
              class="quantity-input"
            />
          </el-form-item>
        </el-form>

        <div v-if="errorMessage" class="error-message" data-cy="error-message">
          {{ errorMessage }}
        </div>
        <div v-if="successMessage" class="success-message" data-cy="success-message">
          {{ successMessage }}
        </div>
      </div>

      <template #footer>
        <el-button @click="quantityModalVisible = false">取消</el-button>
        <el-button
          type="primary"
          data-cy="confirm-add-btn"
          @click="confirmAddToCart"
        >確認</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import productsApi from '@/api/products'
import { useCartStore } from '@/stores/cart'
import type { Product } from '@/types'

const products = ref<Product[]>([])
const quantityModalVisible = ref(false)
const selectedProduct = ref<Product | null>(null)
const errorMessage = ref('')
const successMessage = ref('')
const formRef = ref<FormInstance>()

const cartStore = useCartStore()

const form = reactive({
  quantity: 1
})

const rules = {
  quantity: [
    {
      validator: (rule: any, value: number, callback: any) => {
        if (selectedProduct.value && value > selectedProduct.value.stock) {
          callback(new Error('超過可用庫存'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

onMounted(async () => {
  await loadProducts()
})

async function loadProducts() {
  try {
    const response = await productsApi.getProducts({ status: 'ACTIVE' })
    products.value = response.data.content || response.data
  } catch (error) {
    console.error('Failed to load products:', error)
    ElMessage.error('載入商品失敗')
  }
}

function showAddToCartModal(product: Product) {
  selectedProduct.value = product
  form.quantity = 1
  errorMessage.value = ''
  successMessage.value = ''
  quantityModalVisible.value = true
}

async function confirmAddToCart() {
  if (!selectedProduct.value) return

  errorMessage.value = ''
  successMessage.value = ''

  // 驗證數量
  if (form.quantity > selectedProduct.value.stock) {
    errorMessage.value = '超過可用庫存'
    return
  }

  if (form.quantity <= 0) {
    errorMessage.value = '數量必須大於 0'
    return
  }

  try {
    await formRef.value?.validate()

    cartStore.addToCart(selectedProduct.value, form.quantity)

    successMessage.value = '已加入購物車'
    ElMessage.success('已加入購物車')

    // 延遲關閉 modal 讓測試能看到成功訊息
    setTimeout(() => {
      quantityModalVisible.value = false
      selectedProduct.value = null
      successMessage.value = ''
    }, 500)
  } catch (error) {
    console.error('Validation failed:', error)
  }
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

.add-to-cart-btn {
  width: 100%;
}

.quantity-input {
  width: 100%;
}

.error-message {
  color: #F56C6C;
  font-size: 14px;
  margin-top: 10px;
}

.success-message {
  color: #67C23A;
  font-size: 14px;
  margin-top: 10px;
}
</style>
