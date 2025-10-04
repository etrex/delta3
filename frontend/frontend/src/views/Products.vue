<template>
  <div class="products-container">
    <div class="header">
      <h2>商品瀏覽</h2>
      <div class="cart-info" v-if="authStore.user?.role === 'CUSTOMER'">
        <el-badge :value="cartStore.cartCount" type="primary" data-cy="cart-count">
          <el-button @click="goToCart" data-cy="cart-icon">
            <el-icon><ShoppingCart /></el-icon>
            購物車
          </el-button>
        </el-badge>
      </div>
    </div>

    <!-- Search and Filter Section -->
    <div class="filters-section">
      <div class="search-row">
        <el-input
          v-model="searchQuery"
          placeholder="搜尋商品..."
          data-cy="search-input"
          style="width: 300px"
        >
          <template #append>
            <el-button @click="handleSearch" data-cy="search-btn">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>

        <!-- Admin only: Show inactive toggle -->
        <el-switch
          v-if="authStore.user?.role === 'ADMIN'"
          v-model="showInactive"
          active-text="顯示下架商品"
          data-cy="show-inactive-toggle"
          @change="handleShowInactiveChange"
        />
      </div>

      <div class="filter-row">
        <el-select
          v-model="selectedCategory"
          placeholder="選擇類別"
          clearable
          data-cy="category-filter"
          @change="handleCategoryChange"
        >
          <el-option
            v-for="category in productsStore.categories"
            :key="category"
            :label="category"
            :value="category"
          />
        </el-select>

        <div class="price-filter">
          <el-input
            v-model.number="priceMin"
            placeholder="最低價格"
            type="number"
            data-cy="price-min"
          />
          <span>-</span>
          <el-input
            v-model.number="priceMax"
            placeholder="最高價格"
            type="number"
            data-cy="price-max"
          />
          <el-button @click="handlePriceFilter" data-cy="apply-filter-btn">
            套用
          </el-button>
        </div>

        <el-select
          v-model="sortOption"
          placeholder="排序方式"
          data-cy="sort-select"
          @change="handleSort"
        >
          <el-option label="價格由低到高" value="price-asc" />
          <el-option label="價格由高到低" value="price-desc" />
          <el-option label="名稱 A-Z" value="name-asc" />
          <el-option label="名稱 Z-A" value="name-desc" />
        </el-select>
      </div>
    </div>

    <!-- Products List -->
    <div v-loading="productsStore.loading" class="products-content">
      <div v-if="productsStore.paginatedProducts.length === 0" class="no-products">
        <p>沒有找到符合條件的商品</p>
      </div>

      <div class="products-grid" data-cy="product-list">
        <div
          v-for="product in productsStore.paginatedProducts"
          :key="product.id"
          class="product-card"
          data-cy="product-card"
          @click="showProductDetail(product)"
        >
          <div class="product-image">
            <img
              :src="product.image"
              :alt="product.name"
              data-cy="product-image"
              @error="handleImageError"
            />
            <div v-if="product.status === 'INACTIVE'" class="status-badge inactive" data-cy="product-status-inactive">
              已下架
            </div>
          </div>

          <div class="product-info">
            <h3 data-cy="product-name">{{ product.name }}</h3>
            <p class="price" data-cy="product-price">${{ product.price }}</p>
            <p class="description" data-cy="product-description">{{ product.description }}</p>
            <p class="stock" data-cy="product-stock">庫存: {{ product.stock }}</p>

            <!-- Customer actions -->
            <div v-if="authStore.user?.role === 'CUSTOMER'" class="customer-actions" @click.stop>
              <el-button
                type="primary"
                size="small"
                :disabled="product.stock === 0 || product.status === 'INACTIVE'"
                data-cy="add-to-cart-btn"
                @click="showAddToCartDialog(product)"
              >
                {{ product.stock === 0 ? '缺貨' : '加入購物車' }}
              </el-button>
            </div>

            <!-- Admin actions -->
            <div v-if="authStore.user?.role === 'ADMIN'" class="admin-actions" @click.stop>
              <el-button size="small" data-cy="edit-product-btn">
                編輯
              </el-button>
              <el-button
                size="small"
                :type="product.status === 'ACTIVE' ? 'warning' : 'success'"
                data-cy="toggle-status-btn"
                @click="handleToggleStatus(product)"
              >
                {{ product.status === 'ACTIVE' ? '下架' : '上架' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="pagination-section" data-cy="pagination">
        <div class="page-size-selector">
          <span>每頁顯示:</span>
          <el-select
            v-model="pageSize"
            data-cy="page-size-select"
            @change="handlePageSizeChange"
          >
            <el-option label="10" :value="10" />
            <el-option label="20" :value="20" />
            <el-option label="50" :value="50" />
          </el-select>
        </div>

        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="productsStore.pagination.totalItems"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />

        <div class="page-info" data-cy="page-info">
          第 {{ currentPage }} 頁，共 {{ productsStore.pagination.totalPages }} 頁
        </div>

        <div class="pagination-buttons">
          <el-button
            :disabled="currentPage <= 1"
            data-cy="prev-page-btn"
            @click="handlePageChange(currentPage - 1)"
          >
            上一頁
          </el-button>
          <el-button
            :disabled="currentPage >= productsStore.pagination.totalPages"
            data-cy="next-page-btn"
            @click="handlePageChange(currentPage + 1)"
          >
            下一頁
          </el-button>
        </div>
      </div>
    </div>

    <!-- Product Detail Modal -->
    <ProductDetailModal
      :visible="showDetailModal"
      :product="selectedProduct"
      @close="closeDetailModal"
    />

    <!-- Quick Add to Cart Dialog -->
    <el-dialog
      v-model="showAddDialog"
      title="加入購物車"
      width="400px"
      data-cy="quantity-modal"
    >
      <div v-if="selectedProduct">
        <p>商品: {{ selectedProduct.name }}</p>
        <p>價格: ${{ selectedProduct.price }}</p>
        <div class="quantity-input">
          <label>數量:</label>
          <el-input-number
            v-model="addQuantity"
            :min="1"
            :max="selectedProduct.stock"
            data-cy="quantity-input"
          />
        </div>
        <div v-if="cartStore.loading === false && lastAddResult" class="add-result">
          <p v-if="lastAddResult.errorType === 'INSUFFICIENT_STOCK'" class="error-message" data-cy="error-message">
            {{ lastAddResult.message }}
          </p>
        </div>
      </div>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="cartStore.loading"
          data-cy="confirm-add-btn"
          @click="handleConfirmAdd"
        >
          確認加入
        </el-button>
      </template>
    </el-dialog>

    <!-- Confirm Status Change Dialog -->
    <el-dialog
      v-model="showConfirmDialog"
      title="確認操作"
      width="400px"
      data-cy="confirm-dialog"
    >
      <p>確定要{{ confirmAction }}這個商品嗎？</p>
      <template #footer>
        <el-button @click="showConfirmDialog = false">取消</el-button>
        <el-button type="primary" data-cy="confirm-btn" @click="handleConfirmStatusChange">
          確認
        </el-button>
      </template>
    </el-dialog>

    <!-- Cart Drawer -->
    <CartDrawer v-model="showCartDrawer" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useProductsStore, type Product } from '@/stores/products'
import { useCartStore } from '@/stores/cart'
import ProductDetailModal from '@/components/ProductDetailModal.vue'
import CartDrawer from '@/components/CartDrawer.vue'
import { showSuccessMessage, showErrorMessage } from '@/utils/message'

const router = useRouter()
const authStore = useAuthStore()
const productsStore = useProductsStore()
const cartStore = useCartStore()

// UI state
const showDetailModal = ref(false)
const showAddDialog = ref(false)
const showConfirmDialog = ref(false)
const showCartDrawer = ref(false)
const selectedProduct = ref<Product | null>(null)
const confirmAction = ref('')
const confirmProductId = ref<number | null>(null)

// Filter state
const searchQuery = ref('')
const selectedCategory = ref('')
const priceMin = ref<number | null>(null)
const priceMax = ref<number | null>(null)
const showInactive = ref(false)
const sortOption = ref('')

// Pagination state
const currentPage = ref(1)
const pageSize = ref(10)

// Add to cart state
const addQuantity = ref(1)
const lastAddResult = ref<any>(null)

// Initialize
onMounted(async () => {
  // Ensure showInactive is false for customer view
  productsStore.setFilter({ showInactive: false })
  await productsStore.loadProducts()
  productsStore.updatePagination()
})

// Watchers
watch([currentPage, pageSize], () => {
  productsStore.setPagination(currentPage.value, pageSize.value)
}, { immediate: false })

// Methods
const handleSearch = () => {
  productsStore.setFilter({ search: searchQuery.value })
  currentPage.value = 1
}

const handleShowInactiveChange = () => {
  productsStore.setFilter({ showInactive: showInactive.value })
  currentPage.value = 1
}

const handleCategoryChange = () => {
  productsStore.setFilter({ category: selectedCategory.value })
  currentPage.value = 1
}

const handlePriceFilter = () => {
  productsStore.setFilter({
    priceMin: priceMin.value,
    priceMax: priceMax.value
  })
  currentPage.value = 1
}

const handleSort = () => {
  const [field, order] = sortOption.value.split('-')
  productsStore.sortProducts(field as 'name' | 'price', order as 'asc' | 'desc')
}

const handlePageChange = (page: number) => {
  currentPage.value = page
}

const handlePageSizeChange = () => {
  currentPage.value = 1
  productsStore.setPagination(1, pageSize.value)
}

const showProductDetail = (product: Product) => {
  selectedProduct.value = product
  showDetailModal.value = true
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedProduct.value = null
}

const showAddToCartDialog = (product: Product) => {
  selectedProduct.value = product
  addQuantity.value = 1
  showAddDialog.value = true
}

const handleConfirmAdd = async () => {
  if (!selectedProduct.value) return

  lastAddResult.value = null
  const result = await cartStore.addToCart(selectedProduct.value.id, addQuantity.value)
  lastAddResult.value = result

  if (result.success) {
    ElMessage.success(result.message)
    showSuccessMessage(result.message)
    showAddDialog.value = false
    lastAddResult.value = null
  } else {
    ElMessage.error(result.message)
    showErrorMessage(result.message)

    // 如果庫存不足且有可用庫存，提供智能調整
    if (result.errorType === 'INSUFFICIENT_STOCK' && result.availableStock && result.availableStock > 0) {
      // 自動調整為最大可用數量
      addQuantity.value = result.availableStock
      ElMessage.info(`已自動調整為最大可用數量: ${result.availableStock}`)
    }
    // 保持對話框開啟，讓用戶可以調整數量
  }
}

const handleToggleStatus = (product: Product) => {
  selectedProduct.value = product
  confirmAction.value = product.status === 'ACTIVE' ? '下架' : '上架'
  confirmProductId.value = product.id
  showConfirmDialog.value = true
}

const handleConfirmStatusChange = async () => {
  if (confirmProductId.value) {
    try {
      await productsStore.toggleProductStatus(confirmProductId.value)
      ElMessage.success('商品狀態已更新')
      showSuccessMessage('商品狀態已更新')
      showConfirmDialog.value = false
    } catch (error) {
      ElMessage.error('更新失敗')
      showErrorMessage('更新失敗')
    }
  }
}

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.src = '/images/placeholder.jpg'
}

const goToCart = () => {
  showCartDrawer.value = true
}
</script>

<style scoped>
.products-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.cart-info .el-badge {
  margin-right: 10px;
}

.filters-section {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.search-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.filter-row {
  display: flex;
  gap: 15px;
  align-items: center;
  flex-wrap: wrap;
}

.price-filter {
  display: flex;
  align-items: center;
  gap: 10px;
}

.price-filter .el-input {
  width: 120px;
}

.products-content {
  min-height: 400px;
}

.no-products {
  text-align: center;
  padding: 50px;
  color: #999;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.product-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
  background: white;
}

.product-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.product-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 8px;
  border-radius: 4px;
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.status-badge.inactive {
  background-color: #f56565;
}

.product-info {
  padding: 15px;
}

.product-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #333;
}

.price {
  font-size: 18px;
  font-weight: bold;
  color: #e74c3c;
  margin: 5px 0;
}

.description {
  color: #666;
  font-size: 14px;
  margin: 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.stock {
  color: #27ae60;
  font-size: 14px;
  margin: 8px 0;
}

.customer-actions,
.admin-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 30px;
  padding: 20px 0;
  border-top: 1px solid #e0e0e0;
  flex-wrap: wrap;
  gap: 20px;
}

.page-size-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-info {
  font-size: 14px;
  color: #666;
}

.pagination-buttons {
  display: flex;
  gap: 10px;
}

.quantity-input {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 15px 0;
}

@media (max-width: 768px) {
  .products-container {
    padding: 10px;
  }

  .header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .search-row,
  .filter-row {
    flex-direction: column;
    gap: 10px;
  }

  .products-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 15px;
  }

  .pagination-section {
    flex-direction: column;
    text-align: center;
  }
}
</style>