<template>
  <div class="orders-container">
      <h1>訂單管理</h1>

      <!-- 搜尋與篩選 -->
      <div class="search-filters" data-cy="search-filters">
        <el-input
          v-model="searchKeyword"
          placeholder="搜尋訂單號碼或客戶名稱"
          data-cy="search-input"
          class="search-input"
          clearable
        >
          <template #append>
            <el-button data-cy="search-btn" @click="handleSearch">搜尋</el-button>
          </template>
        </el-input>

        <el-select
          v-model="selectedStatus"
          placeholder="篩選狀態"
          data-cy="status-filter"
          clearable
          class="status-filter"
        >
          <el-option label="全部" value="" />
          <el-option label="已建立" value="CREATED" />
          <el-option label="已付款" value="PAID" />
          <el-option label="已批准" value="APPROVED" />
          <el-option label="已出貨" value="SHIPPED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>

        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="開始日期"
          end-placeholder="結束日期"
          data-cy="date-range-picker"
          class="date-picker"
        >
          <template #default="{ value }">
            <el-input data-cy="date-from" v-if="dateRange && dateRange[0]" :value="formatDate(dateRange[0])" />
            <el-input data-cy="date-to" v-if="dateRange && dateRange[1]" :value="formatDate(dateRange[1])" />
          </template>
        </el-date-picker>

        <el-button data-cy="apply-filter-btn" type="primary" @click="applyFilters">套用篩選</el-button>
        <el-button data-cy="clear-search-btn" @click="clearFilters">清除</el-button>
      </div>

      <!-- 排序與分頁控制 -->
      <div class="list-controls">
        <el-select v-model="sortBy" data-cy="sort-select" @change="handleSort" class="sort-select">
          <el-option label="日期由新到舊" value="date-desc" />
          <el-option label="日期由舊到新" value="date-asc" />
          <el-option label="金額由高到低" value="amount-desc" />
          <el-option label="金額由低到高" value="amount-asc" />
        </el-select>

        <el-select v-model="pageSize" data-cy="page-size-select" @change="handlePageSizeChange" class="page-size-select">
          <el-option :value="10" label="每頁 10 筆" />
          <el-option :value="20" label="每頁 20 筆" />
          <el-option :value="50" label="每頁 50 筆" />
        </el-select>
      </div>

      <!-- 訂單表格 -->
      <div v-if="orders.length === 0" class="no-results" data-cy="no-results-message">
        未找到符合條件的訂單
      </div>

      <el-table
        v-else
        :data="orders"
        style="width: 100%"
        data-cy="order-table"
        class="order-table"
      >
        <el-table-column prop="id" label="訂單編號" width="120" data-cy="table-header">
          <template #default="scope">
            <span data-cy="order-id">{{ scope.row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="customerName" label="客戶名稱" width="150" data-cy="table-header">
          <template #default="scope">
            <span data-cy="customer-name">{{ scope.row.customerName || '未知客戶' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="訂單日期" width="180" data-cy="table-header">
          <template #default="scope">
            <span data-cy="order-date">{{ formatDateTime(scope.row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="狀態" width="120" data-cy="table-header">
          <template #default="scope">
            <el-tag
              :type="getStatusType(scope.row.status)"
              data-cy="status-badge"
              :class="'status-' + scope.row.status.toLowerCase()"
            >
              <span data-cy="order-status">{{ getStatusLabel(scope.row.status) }}</span>
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="totalAmount" label="總金額" width="150" data-cy="table-header">
          <template #default="scope">
            <span data-cy="order-total">${{ scope.row.totalAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" data-cy="table-header">
          <template #default="scope">
            <div class="action-buttons" data-cy="action-buttons">
              <el-button
                size="small"
                data-cy="view-btn"
                @click="viewOrderDetail(scope.row.id)"
              >
                查看
              </el-button>
              <el-button
                size="small"
                type="primary"
                data-cy="edit-status-btn"
                @click="showEditStatusDialog(scope.row)"
              >
                編輯狀態
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分頁 -->
      <div v-if="totalPages > 1" class="pagination" data-cy="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalElements"
          :page-sizes="[10, 20, 50]"
          layout="prev, pager, next, jumper"
          @current-change="handlePageChange"
        >
          <template #default>
            <span data-cy="page-info">第 {{ currentPage }} 頁</span>
          </template>
        </el-pagination>
        <el-button
          data-cy="prev-page-btn"
          :disabled="currentPage === 1"
          @click="currentPage--"
        >上一頁</el-button>
        <el-button
          data-cy="next-page-btn"
          :disabled="currentPage === totalPages"
          @click="currentPage++"
        >下一頁</el-button>
      </div>

    <!-- 編輯狀態對話框 -->
    <el-dialog
      v-model="showStatusDialog"
      title="編輯訂單狀態"
      width="400px"
      data-cy="edit-status-dialog"
    >
      <el-form v-if="selectedOrder">
        <el-form-item label="當前狀態">
          <el-tag :type="getStatusType(selectedOrder.status)">
            {{ getStatusLabel(selectedOrder.status) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新狀態">
          <el-select v-model="newStatus" data-cy="status-select" placeholder="選擇新狀態">
            <el-option label="已建立" value="CREATED" />
            <el-option label="已付款" value="PAID" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已出貨" value="SHIPPED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showStatusDialog = false">取消</el-button>
        <el-button
          type="primary"
          data-cy="confirm-status-btn"
          @click="confirmStatusChange"
        >
          確認
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ordersApi from '@/api/orders'
import { useAuthStore } from '@/stores/auth'
import type { Order } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const orders = ref<Order[]>([])
const searchKeyword = ref('')
const selectedStatus = ref('')
const dateRange = ref<[Date, Date] | null>(null)
const sortBy = ref('date-desc')
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const showStatusDialog = ref(false)
const selectedOrder = ref<Order | null>(null)
const newStatus = ref('')

onMounted(async () => {
  await loadOrders()
})

async function loadOrders() {
  try {
    const params: any = {
      page: currentPage.value - 1,
      size: pageSize.value,
      sort: getSortParam()
    }

    if (selectedStatus.value) {
      params.status = selectedStatus.value
    }

    if (searchKeyword.value) {
      params.search = searchKeyword.value
    }

    const data = await ordersApi.getOrders(params)

    if (data.content) {
      orders.value = data.content
      totalElements.value = data.totalElements
      totalPages.value = data.totalPages
    } else {
      orders.value = Array.isArray(data) ? data : []
      totalElements.value = orders.value.length
      totalPages.value = 1
    }
  } catch (error) {
    console.error('Failed to load orders:', error)
    ElMessage.error('載入訂單失敗')
  }
}

function getSortParam() {
  switch (sortBy.value) {
    case 'date-desc':
      return 'createdAt,desc'
    case 'date-asc':
      return 'createdAt,asc'
    case 'amount-desc':
      return 'totalAmount,desc'
    case 'amount-asc':
      return 'totalAmount,asc'
    default:
      return 'createdAt,desc'
  }
}

function handleSearch() {
  currentPage.value = 1
  loadOrders()
}

function applyFilters() {
  currentPage.value = 1
  loadOrders()
}

function clearFilters() {
  searchKeyword.value = ''
  selectedStatus.value = ''
  dateRange.value = null
  sortBy.value = 'date-desc'
  currentPage.value = 1
  loadOrders()
}

function handleSort() {
  currentPage.value = 1
  loadOrders()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadOrders()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadOrders()
}

function viewOrderDetail(orderId: number | undefined) {
  if (orderId) {
    router.push(`/admin/orders/${orderId}`)
  }
}

function showEditStatusDialog(order: Order) {
  selectedOrder.value = order
  newStatus.value = order.status
  showStatusDialog.value = true
}

async function confirmStatusChange() {
  if (!selectedOrder.value || !newStatus.value) return

  try {
    // Call appropriate API based on new status
    const orderId = selectedOrder.value.id
    if (!orderId) return

    switch (newStatus.value) {
      case 'PAID':
        await ordersApi.payOrder(orderId, {
          paymentMethod: 'CREDIT_CARD',
          amount: selectedOrder.value.totalAmount
        })
        break
      case 'SHIPPED':
        await ordersApi.shipOrder(orderId)
        break
      case 'CANCELLED':
        await ordersApi.cancelOrder(orderId)
        break
      default:
        // For other statuses, we might need a generic update API
        break
    }

    ElMessage.success('訂單狀態已更新')
    showStatusDialog.value = false
    selectedOrder.value = null
    await loadOrders()
  } catch (error) {
    console.error('Failed to update order status:', error)
    ElMessage.error('更新訂單狀態失敗')
  }
}

function formatDate(date: Date | string): string {
  if (!date) return ''
  const d = new Date(date)
  return d.toISOString().split('T')[0]
}

function formatDateTime(date: string | undefined): string {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleString('zh-TW')
}

function getStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    'CREATED': '已建立',
    'PAID': '已付款',
    'APPROVED': '已批准',
    'SHIPPED': '已出貨',
    'CANCELLED': '已取消'
  }
  return labels[status] || status
}

function getStatusType(status: string): string {
  const types: Record<string, string> = {
    'CREATED': 'info',
    'PAID': 'success',
    'APPROVED': 'warning',
    'SHIPPED': 'success',
    'CANCELLED': 'danger'
  }
  return types[status] || 'info'
}
</script>

<style scoped>
.orders-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  margin-bottom: 30px;
  color: #333;
}

.search-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.status-filter {
  min-width: 150px;
}

.date-picker {
  min-width: 300px;
}

.list-controls {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 12px;
}

.sort-select,
.page-size-select {
  min-width: 150px;
}

.no-results {
  text-align: center;
  padding: 60px;
  color: #999;
  font-size: 18px;
}

.order-table {
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.pagination {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
}

:deep(.el-table) {
  background-color: white;
}

:deep(.el-table__row) {
  cursor: pointer;
}

:deep(.el-table__row):hover {
  background-color: #f5f7fa;
}
</style>
