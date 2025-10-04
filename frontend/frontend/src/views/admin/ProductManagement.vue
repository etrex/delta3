<template>
  <div class="admin-layout">
    <!-- Header with logout button -->
    <el-header class="header-bar">
      <div class="header-left">
        <h2>智能訂單管理系統 - Admin</h2>
      </div>
      <div class="header-right">
        <el-space>
          <span data-cy="user-role">Admin</span>
          <span data-cy="username-display">{{ authStore.user?.username }}</span>
          <el-button
            type="danger"
            plain
            data-cy="logout-btn"
            @click="handleLogout"
          >
            登出
          </el-button>
        </el-space>
      </div>
    </el-header>

    <div class="product-management-container" data-cy="product-list">
      <div class="header">
        <h2>商品管理</h2>
        <div class="actions">
          <el-button
            type="primary"
            @click="router.push('/admin/products/new')"
          >
            新增商品
          </el-button>
          <el-button
            data-cy="export-btn"
            @click="showExportDialog = true"
          >
            匯出商品清單
          </el-button>
        </div>
      </div>

    <!-- Low Stock Warning -->
    <el-alert
      v-if="productsStore.lowStockProducts.length > 0"
      type="warning"
      :closable="false"
      data-cy="low-stock-warning"
      class="low-stock-alert"
    >
      <template #title>
        <span data-cy="low-stock-badge">
          有 {{ productsStore.lowStockProducts.length }} 個商品庫存不足
        </span>
      </template>
    </el-alert>

    <!-- Filters -->
    <div class="filters">
      <el-checkbox
        v-model="showInactive"
        data-cy="show-inactive-toggle"
        @change="handleFilterChange"
      >
        顯示已下架商品
      </el-checkbox>
      <el-checkbox
        data-cy="select-all-checkbox"
        :model-value="selectAllChecked"
        @change="handleSelectAll"
        style="margin-left: 20px"
      >
        全選
      </el-checkbox>
    </div>

    <!-- Batch Actions -->
    <div v-if="selectedIds.length > 0" class="batch-actions">
      <span>已選擇 {{ selectedIds.length }} 個商品</span>
      <el-select
        v-model="batchAction"
        placeholder="批量操作"
        data-cy="bulk-actions-menu"
        @change="handleBatchAction"
      >
        <el-option label="批量上架" value="activate" />
        <el-option label="批量下架" value="deactivate" />
      </el-select>
      <el-button
        v-if="batchAction"
        type="primary"
        data-cy="confirm-bulk-action-btn"
        @click="confirmBatchAction"
      >
        執行
      </el-button>
    </div>

    <!-- Products Table (using cards for testing compatibility) -->
    <div class="products-table" data-cy="product-table">
      <div
        v-for="product in productsStore.paginatedProducts"
        :key="product.id"
        class="product-card"
        data-cy="product-card"
      >
        <div class="product-row" data-cy="product-row">
          <el-checkbox
            :model-value="selectedIds.includes(product.id)"
            @change="handleProductSelect(product.id, $event)"
          />
          <span data-cy="product-id" class="product-field">{{ product.id }}</span>
          <div class="product-name-field">
            <span data-cy="product-name">{{ product.name }}</span>
            <el-tag
              v-if="product.stock <= (product.stockThreshold || 5)"
              type="warning"
              size="small"
              data-cy="low-stock-indicator"
              style="margin-left: 10px"
            >
              庫存不足
            </el-tag>
          </div>
          <span class="product-field">{{ product.category }}</span>
          <span data-cy="product-price" class="product-field">${{ product.price }}</span>
          <span data-cy="product-stock" class="product-field">{{ product.stock }}</span>
          <el-tag
            :type="product.status === 'ACTIVE' ? 'success' : 'info'"
            :data-cy="product.status === 'ACTIVE' ? 'product-status-active' : 'product-status-inactive'"
            class="product-field"
          >
            <span data-cy="product-status">
              {{ product.status === 'ACTIVE' ? '上架' : '下架' }}
            </span>
          </el-tag>
          <span data-cy="product-created-at" class="product-field">
            {{ product.createdAt ? new Date(product.createdAt).toLocaleString('zh-TW') : '-' }}
          </span>
          <div class="product-actions">
            <el-button
              size="small"
              data-cy="edit-product-btn"
              @click="handleEdit(product)"
            >
              編輯
            </el-button>
            <el-button
              size="small"
              data-cy="stock-adjustment-btn"
              @click="handleStockAdjustment(product)"
            >
              調整庫存
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
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        :total="productsStore.pagination.totalItems"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- Toggle Status Dialog -->
    <el-dialog
      v-model="showToggleDialog"
      title="確認操作"
      width="400px"
      data-cy="confirm-dialog"
    >
      <p data-cy="confirm-message">
        {{ confirmMessage }}
      </p>
      <template #footer>
        <el-button @click="showToggleDialog = false">取消</el-button>
        <el-button
          type="primary"
          data-cy="confirm-btn"
          @click="confirmToggleStatus"
        >
          確認
        </el-button>
      </template>
    </el-dialog>

    <!-- Stock Adjustment Dialog -->
    <el-dialog
      v-model="showStockDialog"
      title="調整庫存"
      width="500px"
      data-cy="stock-adjustment-modal"
    >
      <el-form v-if="selectedProduct">
        <el-form-item label="當前庫存">
          <span>{{ selectedProduct.stock }}</span>
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
          <el-input-number
            v-model="stockAdjustment.quantity"
            :min="1"
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

    <!-- Export Dialog -->
    <el-dialog
      v-model="showExportDialog"
      title="匯出商品清單"
      width="400px"
    >
      <el-form>
        <el-form-item label="匯出格式">
          <el-select
            v-model="exportFormat"
            data-cy="export-format-select"
          >
            <el-option label="CSV" value="CSV" />
            <el-option label="JSON" value="JSON" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showExportDialog = false">取消</el-button>
        <el-button
          type="primary"
          data-cy="confirm-export-btn"
          @click="confirmExport"
        >
          匯出
        </el-button>
      </template>
    </el-dialog>

    <!-- Batch Confirm Dialog -->
    <el-dialog
      v-model="showBatchConfirmDialog"
      title="確認批量操作"
      width="400px"
      data-cy="confirm-dialog"
    >
      <p>確定要批量{{ batchAction === 'activate' ? '上架' : '下架' }}選中的 {{ selectedIds.length }} 個商品嗎？</p>
      <template #footer>
        <el-button @click="showBatchConfirmDialog = false">取消</el-button>
        <el-button
          type="primary"
          data-cy="confirm-btn"
          @click="executeBatchAction"
        >
          確認
        </el-button>
      </template>
    </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useProductsStore, type Product, type StockAdjustment } from '@/stores/products'
import { useAuthStore } from '@/stores/auth'
import { showSuccessMessage, showErrorMessage } from '@/utils/message'

const router = useRouter()
const productsStore = useProductsStore()
const authStore = useAuthStore()

const handleLogout = async () => {
  await authStore.logout()
  router.push('/login')
}

const loading = ref(false)
const showInactive = ref(false)
const selectedIds = ref<number[]>([])
const batchAction = ref('')
const showToggleDialog = ref(false)
const showStockDialog = ref(false)
const showExportDialog = ref(false)
const showBatchConfirmDialog = ref(false)
const selectedProduct = ref<Product | null>(null)
const confirmMessage = ref('')
const exportFormat = ref<'CSV' | 'JSON'>('CSV')
const currentPage = ref(1)
const pageSize = ref(10)

const stockAdjustment = ref<StockAdjustment>({
  type: 'increase',
  quantity: 1,
  reason: ''
})

onMounted(async () => {
  // Always reload products when this page is mounted to get fresh data
  loading.value = true
  try {
    await productsStore.loadProducts()
    // Ensure pagination is updated
    await new Promise(resolve => setTimeout(resolve, 50))
    productsStore.updatePagination()
  } finally {
    loading.value = false
  }
})

// Watch for products changes to update pagination
watch(() => productsStore.products.length, () => {
  productsStore.updatePagination()
}, { immediate: true })

watch([currentPage, pageSize], () => {
  productsStore.setPagination(currentPage.value, pageSize.value)
})

const handleFilterChange = () => {
  productsStore.setFilter({ showInactive: showInactive.value })
}

const selectAllChecked = computed(() => {
  return productsStore.paginatedProducts.length > 0 &&
    selectedIds.value.length === productsStore.paginatedProducts.length
})

const handleSelectAll = (checked: boolean) => {
  if (checked) {
    selectedIds.value = productsStore.paginatedProducts.map(p => p.id)
  } else {
    selectedIds.value = []
  }
}

const handleProductSelect = (productId: number, checked: boolean) => {
  if (checked) {
    if (!selectedIds.value.includes(productId)) {
      selectedIds.value.push(productId)
    }
  } else {
    selectedIds.value = selectedIds.value.filter(id => id !== productId)
  }
}

const handleSelectionChange = (selection: Product[]) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleEdit = (product: Product) => {
  router.push(`/admin/products/${product.id}/edit`)
}

const handleToggleStatus = (product: Product) => {
  selectedProduct.value = product
  confirmMessage.value = `確定要${product.status === 'ACTIVE' ? '下架' : '上架'}此商品嗎？`
  showToggleDialog.value = true
}

const confirmToggleStatus = async () => {
  if (!selectedProduct.value) return

  // Determine message before toggling
  const message = selectedProduct.value.status === 'ACTIVE' ? '商品已下架' : '商品已上架'

  await productsStore.toggleProductStatus(selectedProduct.value.id)

  ElMessage.success(message)
  showSuccessMessage(message)

  showToggleDialog.value = false
  selectedProduct.value = null

  // Reload products to refresh the list
  await productsStore.loadProducts()
}

const handleStockAdjustment = (product: Product) => {
  selectedProduct.value = product
  stockAdjustment.value = {
    type: 'increase',
    quantity: 1,
    reason: ''
  }
  showStockDialog.value = true
}

const confirmStockAdjustment = async () => {
  if (!selectedProduct.value) return

  const result = await productsStore.adjustStock(selectedProduct.value.id, stockAdjustment.value)

  if (result.success) {
    ElMessage.success(result.message)
    showSuccessMessage(result.message)
    showStockDialog.value = false
  } else {
    ElMessage.error(result.message)
    showErrorMessage(result.message)
  }
}

const handleBatchAction = () => {
  // Action will be confirmed when clicking the confirm button
}

const confirmBatchAction = () => {
  showBatchConfirmDialog.value = true
}

const executeBatchAction = async () => {
  const status = batchAction.value === 'activate' ? 'ACTIVE' : 'INACTIVE'
  const result = await productsStore.batchUpdateStatus(selectedIds.value, status)

  if (result.success) {
    ElMessage.success(result.message)
    showSuccessMessage(result.message)
    selectedIds.value = []
    batchAction.value = ''
    await productsStore.loadProducts()
  } else {
    ElMessage.error(result.message)
    showErrorMessage(result.message)
  }

  showBatchConfirmDialog.value = false
}

const confirmExport = async () => {
  const result = await productsStore.exportProducts(exportFormat.value)

  if (result.success) {
    ElMessage.success(result.message)
    showSuccessMessage(result.message)
  } else {
    ElMessage.error(result.message)
    showErrorMessage(result.message)
  }

  showExportDialog.value = false
}

const handlePageChange = (page: number) => {
  currentPage.value = page
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
}

// Expose stock adjustment handler for edit button
defineExpose({
  handleStockAdjustment
})
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header-bar {
  background-color: #409eff;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.product-management-container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.actions {
  display: flex;
  gap: 10px;
}

.low-stock-alert {
  margin-bottom: 20px;
}

.filters {
  margin-bottom: 20px;
}

.batch-actions {
  margin-bottom: 20px;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.products-table {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.product-card {
  border-bottom: 1px solid #ebeef5;
  transition: background-color 0.2s;
}

.product-card:last-child {
  border-bottom: none;
}

.product-card:hover {
  background-color: #f5f7fa;
}

.product-row {
  display: grid;
  grid-template-columns: 50px 100px 2fr 120px 150px 100px 100px 180px 300px;
  align-items: center;
  padding: 12px;
  gap: 10px;
}

.product-field {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-name-field {
  display: flex;
  align-items: center;
  overflow: hidden;
}

.product-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>
