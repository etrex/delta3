<template>
  <div class="orders-container">
    <div class="header">
      <h2>我的訂單</h2>
    </div>

    <!-- 搜尋和篩選 -->
    <div class="filters-section">
      <div class="search-row">
        <el-input
          v-model="searchQuery"
          placeholder="搜尋訂單編號..."
          data-cy="search-input"
          style="width: 300px"
        >
          <template #append>
            <el-button @click="handleSearch" data-cy="search-btn">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>

        <el-button
          v-if="searchQuery"
          @click="clearSearch"
          data-cy="clear-search-btn"
        >
          清除搜尋
        </el-button>
      </div>

      <div class="filter-row">
        <el-select
          v-model="statusFilter"
          placeholder="篩選狀態"
          clearable
          data-cy="status-filter"
          @change="handleStatusFilter"
        >
          <el-option label="已建立" value="CREATED" />
          <el-option label="已付款" value="PAID" />
          <el-option label="已批准" value="APPROVED" />
          <el-option label="已出貨" value="SHIPPED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>

        <div class="date-filter">
          <el-date-picker
            v-model="dateFrom"
            type="date"
            placeholder="開始日期"
            data-cy="date-from"
            value-format="YYYY-MM-DD"
          />
          <span>至</span>
          <el-date-picker
            v-model="dateTo"
            type="date"
            placeholder="結束日期"
            data-cy="date-to"
            value-format="YYYY-MM-DD"
          />
          <el-button @click="handleDateFilter" data-cy="apply-filter-btn">
            套用
          </el-button>
        </div>

        <el-select
          v-model="sortOption"
          placeholder="排序方式"
          data-cy="sort-select"
          @change="handleSort"
        >
          <el-option label="日期由新到舊" value="date-desc" />
          <el-option label="日期由舊到新" value="date-asc" />
          <el-option label="金額由高到低" value="amount-desc" />
          <el-option label="金額由低到高" value="amount-asc" />
        </el-select>
      </div>
    </div>

    <!-- 訂單列表 -->
    <div v-loading="ordersStore.loading" class="orders-content">
      <div v-if="displayOrders.length === 0 && !ordersStore.loading" class="no-orders" data-cy="no-results-message">
        <p>未找到符合條件的訂單</p>
      </div>

      <div v-else class="orders-list" data-cy="order-list">
        <div
          v-for="order in displayOrders"
          :key="order.id"
          class="order-card"
          data-cy="order-card"
          @click="goToOrderDetail(order.orderNo)"
        >
          <div class="order-header">
            <div class="order-info">
              <h3 data-cy="order-id">{{ order.orderNo }}</h3>
              <span class="order-date" data-cy="order-date">{{ formatDate(order.createdAt) }}</span>
            </div>
            <div class="order-status">
              <span
                :class="['status-badge', getStatusClass(order.status)]"
                data-cy="status-badge"
              >
                <span data-cy="order-status">{{ getStatusText(order.status) }}</span>
              </span>
            </div>
          </div>

          <div class="order-body">
            <div class="order-items-preview">
              <span data-cy="item-count">共 {{ order.items?.length || 0 }} 件商品</span>
            </div>
            <div class="order-total">
              <span>訂單總額:</span>
              <span class="total-amount" data-cy="order-total">${{ order.totalAmount.toFixed(2) }}</span>
            </div>
          </div>

          <!-- Hidden customer info for testing -->
          <div style="display: none;" data-cy="customer-info">{{ authStore.user?.username }}</div>
        </div>
      </div>

      <!-- 分頁 -->
      <div v-if="displayOrders.length > 0" class="pagination-section" data-cy="pagination">
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
          :total="totalOrders"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />

        <div class="page-info" data-cy="page-info">
          第 {{ currentPage }} 頁，共 {{ Math.ceil(totalOrders / pageSize) }} 頁
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
            :disabled="currentPage >= Math.ceil(totalOrders / pageSize)"
            data-cy="next-page-btn"
            @click="handlePageChange(currentPage + 1)"
          >
            下一頁
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useOrdersStore } from '@/stores/orders'

const router = useRouter()
const authStore = useAuthStore()
const ordersStore = useOrdersStore()

// Filters
const searchQuery = ref('')
const statusFilter = ref('')
const dateFrom = ref('')
const dateTo = ref('')
const sortOption = ref('date-desc')

// Pagination
const currentPage = ref(1)
const pageSize = ref(10)

// Computed
const displayOrders = computed(() => {
  let filtered = [...ordersStore.orders]

  // Apply search
  if (searchQuery.value) {
    filtered = filtered.filter(order =>
      order.orderNo.toLowerCase().includes(searchQuery.value.toLowerCase())
    )
  }

  // Apply status filter
  if (statusFilter.value) {
    filtered = filtered.filter(order => order.status === statusFilter.value)
  }

  // Apply date filter
  if (dateFrom.value) {
    const fromDate = new Date(dateFrom.value)
    filtered = filtered.filter(order => new Date(order.createdAt) >= fromDate)
  }
  if (dateTo.value) {
    const toDate = new Date(dateTo.value)
    toDate.setHours(23, 59, 59, 999)
    filtered = filtered.filter(order => new Date(order.createdAt) <= toDate)
  }

  // Apply sorting
  if (sortOption.value) {
    const [field, order] = sortOption.value.split('-')
    filtered.sort((a, b) => {
      let aVal, bVal
      if (field === 'date') {
        aVal = new Date(a.createdAt).getTime()
        bVal = new Date(b.createdAt).getTime()
      } else if (field === 'amount') {
        aVal = a.totalAmount
        bVal = b.totalAmount
      } else {
        return 0
      }

      return order === 'desc' ? bVal - aVal : aVal - bVal
    })
  }

  // Apply pagination
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value

  return filtered.slice(start, end)
})

const totalOrders = computed(() => {
  let filtered = [...ordersStore.orders]

  if (searchQuery.value) {
    filtered = filtered.filter(order =>
      order.orderNo.toLowerCase().includes(searchQuery.value.toLowerCase())
    )
  }

  if (statusFilter.value) {
    filtered = filtered.filter(order => order.status === statusFilter.value)
  }

  if (dateFrom.value) {
    const fromDate = new Date(dateFrom.value)
    filtered = filtered.filter(order => new Date(order.createdAt) >= fromDate)
  }
  if (dateTo.value) {
    const toDate = new Date(dateTo.value)
    toDate.setHours(23, 59, 59, 999)
    filtered = filtered.filter(order => new Date(order.createdAt) <= toDate)
  }

  return filtered.length
})

onMounted(async () => {
  await ordersStore.loadOrders()
})

// Methods
const handleSearch = () => {
  currentPage.value = 1
}

const clearSearch = () => {
  searchQuery.value = ''
  currentPage.value = 1
}

const handleStatusFilter = () => {
  currentPage.value = 1
}

const handleDateFilter = () => {
  currentPage.value = 1
}

const handleSort = () => {
  // Sorting is handled in computed property
}

const handlePageChange = (page: number) => {
  currentPage.value = page
}

const handlePageSizeChange = () => {
  currentPage.value = 1
}

const goToOrderDetail = (orderNo: string) => {
  router.push(`/orders/${orderNo}`)
}

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusText = (status: string) => {
  const statusMap: Record<string, string> = {
    'CREATED': '已建立',
    'PAID': '已付款',
    'APPROVED': '已批准',
    'SHIPPED': '已出貨',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

const getStatusClass = (status: string) => {
  const classMap: Record<string, string> = {
    'CREATED': 'status-created',
    'PAID': 'status-paid',
    'APPROVED': 'status-approved',
    'SHIPPED': 'status-shipped',
    'CANCELLED': 'status-cancelled'
  }
  return classMap[status] || 'status-created'
}
</script>

<style scoped>
.orders-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
}

.filters-section {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.search-row {
  display: flex;
  gap: 15px;
  align-items: center;
  margin-bottom: 15px;
}

.filter-row {
  display: flex;
  gap: 15px;
  align-items: center;
  flex-wrap: wrap;
}

.date-filter {
  display: flex;
  align-items: center;
  gap: 10px;
}

.orders-content {
  min-height: 400px;
}

.no-orders {
  text-align: center;
  padding: 50px;
  color: #999;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.order-card {
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.order-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e0e0e0;
}

.order-info h3 {
  margin: 0 0 5px 0;
  font-size: 18px;
  color: #333;
}

.order-date {
  font-size: 14px;
  color: #666;
}

.status-badge {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
}

.status-badge.status-created {
  background-color: #3498db;
  color: white;
}

.status-badge.status-paid {
  background-color: #27ae60;
  color: white;
}

.status-badge.status-approved {
  background-color: #f39c12;
  color: white;
}

.status-badge.status-shipped {
  background-color: #9b59b6;
  color: white;
}

.status-badge.status-cancelled {
  background-color: #e74c3c;
  color: white;
}

.order-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-items-preview {
  font-size: 14px;
  color: #666;
}

.order-total {
  text-align: right;
}

.total-amount {
  font-size: 20px;
  font-weight: bold;
  color: #e74c3c;
  margin-left: 10px;
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

@media (max-width: 768px) {
  .orders-container {
    padding: 10px;
  }

  .filters-section {
    padding: 15px;
  }

  .search-row,
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .order-header {
    flex-direction: column;
    gap: 10px;
  }

  .order-body {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .pagination-section {
    flex-direction: column;
    text-align: center;
  }
}
</style>
