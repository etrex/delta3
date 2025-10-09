<template>
  <div class="shipping-container">
      <h1>出貨管理</h1>

      <!-- 搜尋與篩選 -->
      <div class="search-filters" data-cy="search-filters">
        <el-select
          v-model="searchType"
          placeholder="搜尋類型"
          data-cy="search-type-select"
          class="search-type-select"
        >
          <el-option label="訂單編號" value="orderId" />
          <el-option label="客戶名稱" value="customerName" />
        </el-select>

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
          v-model="selectedShippingStatus"
          placeholder="出貨狀態"
          data-cy="shipping-status-filter"
          clearable
          class="status-filter"
        >
          <el-option label="全部" value="" />
          <el-option label="已建立" value="CREATED" />
          <el-option label="待出貨" value="APPROVED" />
          <el-option label="已出貨" value="SHIPPED" />
          <el-option label="已送達" value="DELIVERED" />
        </el-select>

        <el-select
          v-model="selectedPaymentStatus"
          placeholder="付款狀態"
          data-cy="payment-status-filter"
          clearable
          class="status-filter"
        >
          <el-option label="全部" value="" />
          <el-option label="已付款" value="SUCCESS" />
          <el-option label="已退款" value="REFUNDED" />
        </el-select>

        <el-input
          v-model="dateFrom"
          type="date"
          placeholder="開始日期"
          data-cy="date-from"
          class="date-input"
        />

        <el-input
          v-model="dateTo"
          type="date"
          placeholder="結束日期"
          data-cy="date-to"
          class="date-input"
        />

        <el-button data-cy="apply-filter-btn" type="primary" @click="applyFilters">套用篩選</el-button>
        <el-button data-cy="clear-search-btn" @click="clearFilters">清除</el-button>
      </div>

      <!-- 批量操作控制 -->
      <div v-if="selectedOrders.length > 0" class="bulk-actions" data-cy="bulk-shipping-actions">
        <span data-cy="selected-count">已選擇 {{ selectedOrders.length }} 個訂單</span>
        <el-button
          type="primary"
          size="small"
          data-cy="bulk-approve-btn"
          @click="showBulkApproveDialog"
        >
          批量標記為待出貨
        </el-button>
        <el-button
          type="success"
          size="small"
          data-cy="bulk-print-labels-btn"
          @click="showPrintOptionsDialog"
        >
          批量列印標籤
        </el-button>
      </div>

      <!-- 訂單列表 -->
      <div v-if="orders.length === 0" class="no-results" data-cy="no-results-message">
        未找到符合條件的訂單
      </div>

      <div v-else class="order-list" data-cy="shipping-order-list">
        <el-table
          :data="orders"
          style="width: 100%"
          class="order-table"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55">
            <template #header>
              <el-checkbox
                v-model="selectAll"
                data-cy="select-all-checkbox"
                @change="handleSelectAll"
              />
            </template>
            <template #default="scope">
              <el-checkbox
                :model-value="isSelected(scope.row)"
                data-cy="select-checkbox"
                @change="handleSelectOrder(scope.row, $event)"
              />
            </template>
          </el-table-column>

          <el-table-column label="訂單資訊" min-width="400">
            <template #default="scope">
              <div class="order-info" data-cy="order-row" @click="navigateToOrderDetail(scope.row)">
                <div class="order-main-info">
                  <span class="order-id" data-cy="order-id">{{ scope.row.orderNo }}</span>
                  <span class="customer-name" data-cy="customer-name">{{ scope.row.customerName || '未知客戶' }}</span>
                </div>
                <div class="order-meta">
                  <span data-cy="order-date">{{ formatDateTime(scope.row.createdAt) }}</span>
                  <span data-cy="total-amount" class="amount">NT$ {{ scope.row.totalAmount?.toFixed(0) }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="付款狀態" width="120">
            <template #default="scope">
              <el-tag
                :type="getPaymentStatusType(getPaymentStatus(scope.row))"
                :class="'status-' + (getPaymentStatus(scope.row) || 'pending').toLowerCase()"
                data-cy="payment-status"
              >
                {{ getPaymentStatusLabel(getPaymentStatus(scope.row)) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="出貨狀態" width="120">
            <template #default="scope">
              <el-tag
                :type="getShippingStatusType(getShippingStatus(scope.row))"
                :class="'shipping-' + (getShippingStatus(scope.row) || 'pending').toLowerCase()"
                data-cy="shipping-status"
              >
                {{ getShippingStatusLabel(getShippingStatus(scope.row)) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button
                v-if="scope.row.status !== 'SHIPPED' && scope.row.status !== 'CANCELLED'"
                size="small"
                type="primary"
                data-cy="next-status-btn"
                @click.stop="updateShippingStatusDirectly(scope.row)"
              >
                → {{ getNextStatusLabel(scope.row.status) }}
              </el-button>
              <span v-else-if="scope.row.status === 'SHIPPED'" class="completed-text">已出貨</span>
              <span v-else class="completed-text">{{ scope.row.status }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

    <!-- 出貨操作對話框 -->
    <el-dialog
      v-model="shippingActionDialogVisible"
      title="出貨操作"
      width="600px"
      data-cy="shipping-action-modal"
    >
      <div v-if="currentOrder">
        <div class="order-summary">
          <p><strong>訂單編號：</strong>{{ currentOrder.orderNo }}</p>
          <p><strong>客戶名稱：</strong>{{ currentOrder.customerName }}</p>
          <p><strong>當前狀態：</strong>{{ getShippingStatusLabel(currentOrder.shippingStatus) }}</p>
        </div>

        <div class="action-buttons-group">
          <el-button
            :type="currentOrder.shippingStatus === 'CREATED' ? 'primary' : 'default'"
            :disabled="currentOrder.shippingStatus !== 'CREATED'"
            data-cy="action-approve"
            @click="selectAction('APPROVE')"
          >
            標記為待出貨
          </el-button>
          <el-button
            :type="currentOrder.shippingStatus === 'APPROVED' ? 'primary' : 'default'"
            :disabled="currentOrder.shippingStatus !== 'APPROVED'"
            data-cy="action-ship"
            @click="selectAction('SHIP')"
          >
            標記為已出貨
          </el-button>
          <el-button
            :type="currentOrder.shippingStatus === 'SHIPPED' ? 'primary' : 'default'"
            :disabled="currentOrder.shippingStatus !== 'SHIPPED'"
            data-cy="action-deliver"
            @click="selectAction('DELIVER')"
          >
            標記為已送達
          </el-button>
        </div>

        <!-- 待出貨確認 -->
        <div v-if="selectedAction === 'APPROVE'" class="action-form">
          <p>確定要將此訂單標記為待出貨嗎？</p>
          <el-button type="primary" data-cy="confirm-btn" @click="confirmApprove">確認</el-button>
        </div>

        <!-- 已出貨表單 -->
        <div v-if="selectedAction === 'SHIP'" class="action-form">
          <el-form :model="shippingForm" ref="shippingFormRef" label-width="120px">
            <el-form-item label="追蹤號碼" required>
              <el-input
                v-model="shippingForm.trackingNumber"
                data-cy="tracking-number"
                placeholder="請輸入追蹤號碼"
              />
              <div v-if="formErrors.trackingNumber" class="error-message" data-cy="tracking-number-error">
                {{ formErrors.trackingNumber }}
              </div>
            </el-form-item>
            <el-form-item label="物流商" required>
              <el-select
                v-model="shippingForm.carrier"
                data-cy="carrier-select"
                placeholder="請選擇物流商"
                style="width: 100%"
              >
                <el-option label="順豐速運" value="順豐速運" />
                <el-option label="宅急便" value="宅急便" />
                <el-option label="黑貓宅急便" value="黑貓宅急便" />
                <el-option label="郵局" value="郵局" />
              </el-select>
              <div v-if="formErrors.carrier" class="error-message" data-cy="carrier-error">
                {{ formErrors.carrier }}
              </div>
            </el-form-item>
            <el-form-item label="預計送達日期">
              <el-input
                v-model="shippingForm.estimatedDelivery"
                type="date"
                data-cy="estimated-delivery"
              />
            </el-form-item>
            <el-form-item label="備註">
              <el-input
                v-model="shippingForm.notes"
                type="textarea"
                :rows="3"
                data-cy="shipping-notes"
                placeholder="請輸入備註"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" data-cy="confirm-ship-btn" @click="confirmShip">確認出貨</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 已送達表單 -->
        <div v-if="selectedAction === 'DELIVER'" class="action-form" data-cy="delivery-confirmation">
          <el-form :model="deliveryForm" label-width="120px">
            <el-form-item label="送達日期">
              <el-input
                v-model="deliveryForm.deliveredDate"
                type="date"
                data-cy="delivered-date"
              />
            </el-form-item>
            <el-form-item label="送達備註">
              <el-input
                v-model="deliveryForm.notes"
                type="textarea"
                :rows="3"
                data-cy="delivery-notes"
                placeholder="請輸入送達備註"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" data-cy="confirm-delivery-btn" @click="confirmDeliver">確認送達</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-dialog>


    <!-- 批量標記為待出貨確認對話框 -->
    <el-dialog
      v-model="bulkApproveDialogVisible"
      title="批量操作確認"
      width="400px"
      data-cy="confirm-bulk-action"
    >
      <p>確定要將選中的 {{ selectedOrders.length }} 個訂單標記為待出貨嗎？</p>
      <template #footer>
        <el-button @click="bulkApproveDialogVisible = false">取消</el-button>
        <el-button type="primary" data-cy="confirm-btn" @click="confirmBulkApprove">確認</el-button>
      </template>
    </el-dialog>

    <!-- 列印選項對話框 -->
    <el-dialog
      v-model="printOptionsDialogVisible"
      title="列印出貨標籤"
      width="400px"
      data-cy="print-options-modal"
    >
      <el-form label-width="120px">
        <el-form-item label="標籤格式">
          <el-select v-model="printLabelFormat" data-cy="label-format-select" style="width: 100%">
            <el-option label="A4" value="A4" />
            <el-option label="A5" value="A5" />
            <el-option label="熱感紙" value="thermal" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="printOptionsDialogVisible = false">取消</el-button>
        <el-button type="primary" data-cy="confirm-print-btn" @click="confirmPrintLabels">確認列印</el-button>
      </template>
    </el-dialog>

    <!-- 成功訊息 -->
    <div v-if="successMessage" class="success-message" data-cy="success-message">
      {{ successMessage }}
    </div>

    <!-- 通知訊息 -->
    <div v-if="notificationMessage" class="notification-toast" data-cy="notification-toast">
      {{ notificationMessage }}
    </div>

    <!-- 訪問拒絕訊息 -->
    <div v-if="accessDenied" class="access-denied" data-cy="access-denied">
      您沒有權限訪問此頁面
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import ordersApi from '@/api/orders'

const router = useRouter()
const authStore = useAuthStore()

// 數據狀態
const orders = ref<any[]>([])
const selectedOrders = ref<any[]>([])
const selectAll = ref(false)
const currentOrder = ref<any>(null)

// 搜尋與篩選
const searchType = ref('orderId')
const searchKeyword = ref('')
const selectedShippingStatus = ref('')
const selectedPaymentStatus = ref('')
const dateFrom = ref('')
const dateTo = ref('')

// 對話框狀態
const shippingActionDialogVisible = ref(false)
const bulkApproveDialogVisible = ref(false)
const printOptionsDialogVisible = ref(false)

// 表單狀態
const selectedAction = ref('')
const shippingForm = ref({
  trackingNumber: '',
  carrier: '',
  estimatedDelivery: '',
  notes: ''
})
const deliveryForm = ref({
  deliveredDate: '',
  notes: ''
})
const formErrors = ref({
  trackingNumber: '',
  carrier: ''
})

const printLabelFormat = ref('A4')

// 訊息狀態
const successMessage = ref('')
const notificationMessage = ref('')
const accessDenied = ref(false)

const shippingFormRef = ref()

// 生命週期
onMounted(async () => {
  // 檢查權限
  if (!authStore.isAdmin) {
    accessDenied.value = true
    setTimeout(() => {
      router.push('/login')
    }, 2000)
    return
  }

  await loadOrders()

  // 監聽即時更新事件
  window.addEventListener('shipping-status-updated', handleShippingStatusUpdate)
})

onUnmounted(() => {
  window.removeEventListener('shipping-status-updated', handleShippingStatusUpdate)
})

// API 調用
async function loadOrders() {
  try {
    // 調用真實的後端 API 載入訂單
    const params: any = {
      page: 0,
      size: 100
    }

    const response = await ordersApi.getOrders(params)
    let loadedOrders = response.content || response || []

    // 應用前端篩選
    let filteredOrders = loadedOrders

    if (selectedShippingStatus.value) {
      filteredOrders = filteredOrders.filter((order: any) => order.shippingStatus === selectedShippingStatus.value)
    }

    if (selectedPaymentStatus.value) {
      filteredOrders = filteredOrders.filter((order: any) => order.paymentStatus === selectedPaymentStatus.value)
    }

    if (searchKeyword.value) {
      filteredOrders = filteredOrders.filter((order: any) => {
        if (searchType.value === 'orderId') {
          return order.orderNo?.toLowerCase().includes(searchKeyword.value.toLowerCase())
        } else {
          return order.customerName?.toLowerCase().includes(searchKeyword.value.toLowerCase())
        }
      })
    }

    orders.value = filteredOrders
  } catch (error) {
    console.error('Failed to load orders:', error)
    ElMessage.error('載入訂單失敗')
  }
}

// 事件處理
function handleSearch() {
  loadOrders()
}

function applyFilters() {
  loadOrders()
}

function clearFilters() {
  searchType.value = 'orderId'
  searchKeyword.value = ''
  selectedShippingStatus.value = ''
  selectedPaymentStatus.value = ''
  dateFrom.value = ''
  dateTo.value = ''
  loadOrders()
}

function handleSelectAll(checked: boolean) {
  if (checked) {
    selectedOrders.value = [...orders.value]
  } else {
    selectedOrders.value = []
  }
}

function handleSelectionChange(selection: any[]) {
  selectedOrders.value = selection
  selectAll.value = selection.length === orders.value.length
}

function handleSelectOrder(order: any, checked: boolean) {
  if (checked) {
    if (!selectedOrders.value.find(o => o.id === order.id)) {
      selectedOrders.value.push(order)
    }
  } else {
    selectedOrders.value = selectedOrders.value.filter(o => o.id !== order.id)
  }
  selectAll.value = selectedOrders.value.length === orders.value.length
}

function isSelected(order: any): boolean {
  return selectedOrders.value.some(o => o.id === order.id)
}

function showShippingActionModal(order: any) {
  currentOrder.value = order
  selectedAction.value = ''
  resetForms()
  shippingActionDialogVisible.value = true
}

function navigateToOrderDetail(order: any) {
  router.push(`/admin/orders/${order.id}`)
}

function selectAction(action: string) {
  selectedAction.value = action
  resetForms()
}

function resetForms() {
  shippingForm.value = {
    trackingNumber: '',
    carrier: '',
    estimatedDelivery: '',
    notes: ''
  }
  deliveryForm.value = {
    deliveredDate: '',
    notes: ''
  }
  formErrors.value = {
    trackingNumber: '',
    carrier: ''
  }
}

async function confirmApprove() {
  try {
    // 模擬 API 調用
    if (currentOrder.value) {
      currentOrder.value.shippingStatus = 'APPROVED'
      currentOrder.value.shippingHistory.push({
        timestamp: new Date().toISOString(),
        status: '待出貨',
        description: '訂單已標記為待出貨'
      })
    }
    showSuccessMessage('訂單已標記為待出貨')
    shippingActionDialogVisible.value = false
    await loadOrders()
  } catch (error) {
    console.error('Failed to approve order:', error)
    ElMessage.error('操作失敗')
  }
}

async function confirmShip() {
  // 驗證表單
  formErrors.value = {
    trackingNumber: '',
    carrier: ''
  }

  if (!shippingForm.value.trackingNumber) {
    formErrors.value.trackingNumber = '請輸入追蹤號碼'
  }
  if (!shippingForm.value.carrier) {
    formErrors.value.carrier = '請選擇物流商'
  }

  if (formErrors.value.trackingNumber || formErrors.value.carrier) {
    return
  }

  try {
    // 模擬 API 調用
    if (currentOrder.value) {
      currentOrder.value.shippingStatus = 'SHIPPED'
      currentOrder.value.trackingNumber = shippingForm.value.trackingNumber
      currentOrder.value.carrier = shippingForm.value.carrier
      currentOrder.value.shippingHistory.push({
        timestamp: new Date().toISOString(),
        status: '已出貨',
        description: `訂單已出貨，追蹤號碼：${shippingForm.value.trackingNumber}，物流商：${shippingForm.value.carrier}`
      })
    }
    showSuccessMessage('訂單已標記為已出貨')
    shippingActionDialogVisible.value = false
    await loadOrders()
  } catch (error) {
    console.error('Failed to ship order:', error)
    ElMessage.error('操作失敗')
  }
}

async function updateShippingStatusDirectly(order: any) {
  try {
    const currentStatus = order.status || 'CREATED'
    let message = ''

    console.log('Updating order:', order)
    console.log('Current status:', currentStatus)

    // 根據當前狀態決定下一個狀態並調用相應的 API
    switch (currentStatus) {
      case 'CREATED':
        // CREATED -> APPROVED (待出貨)
        await ordersApi.approveOrder(order.orderNo)
        message = '訂單已標記為待出貨'
        break
      case 'PAID':
        // PAID -> APPROVED (待出貨)
        await ordersApi.approveOrder(order.orderNo)
        message = '訂單已標記為待出貨'
        break
      case 'APPROVED':
        // APPROVED -> SHIPPED (已出貨)
        await ordersApi.shipOrder(order.id)
        message = '訂單已標記為已出貨'
        break
      case 'SHIPPED':
        // SHIPPED -> DELIVERED (已送達)
        // 目前後端沒有 deliver API，先跳過
        ElMessage.warning('已送達狀態需要手動更新')
        return
      default:
        ElMessage.warning('無法從當前狀態進行更新')
        return
    }

    showSuccessMessage(message)
    await loadOrders()
  } catch (error: any) {
    console.error('Failed to update shipping status:', error)
    ElMessage.error(error.response?.data?.message || '更新出貨狀態失敗')
  }
}

async function confirmDeliver() {
  try {
    // 模擬 API 調用
    if (currentOrder.value) {
      currentOrder.value.shippingStatus = 'DELIVERED'
      currentOrder.value.shippingHistory.push({
        timestamp: new Date().toISOString(),
        status: '已送達',
        description: deliveryForm.value.notes || '訂單已送達'
      })
    }
    showSuccessMessage('訂單已標記為已送達')
    shippingActionDialogVisible.value = false
    await loadOrders()
  } catch (error) {
    console.error('Failed to deliver order:', error)
    ElMessage.error('操作失敗')
  }
}

function showBulkApproveDialog() {
  bulkApproveDialogVisible.value = true
}

async function confirmBulkApprove() {
  try {
    // 模擬批量操作
    selectedOrders.value.forEach(order => {
      if (order.shippingStatus === 'CREATED') {
        order.shippingStatus = 'APPROVED'
        order.shippingHistory.push({
          timestamp: new Date().toISOString(),
          status: '待出貨',
          description: '訂單已標記為待出貨（批量操作）'
        })
      }
    })
    showSuccessMessage('批量操作完成')
    bulkApproveDialogVisible.value = false
    selectedOrders.value = []
    selectAll.value = false
    await loadOrders()
  } catch (error) {
    console.error('Failed to bulk approve:', error)
    ElMessage.error('批量操作失敗')
  }
}

function showPrintOptionsDialog() {
  printOptionsDialogVisible.value = true
}

async function confirmPrintLabels() {
  try {
    // 模擬列印標籤
    showSuccessMessage('標籤已準備列印')
    printOptionsDialogVisible.value = false
  } catch (error) {
    console.error('Failed to print labels:', error)
    ElMessage.error('列印失敗')
  }
}

function handleShippingStatusUpdate(event: any) {
  const { orderId, status } = event.detail
  notificationMessage.value = '訂單狀態已由其他管理員更新'
  setTimeout(() => {
    notificationMessage.value = ''
  }, 3000)
  loadOrders()
}

function showSuccessMessage(message: string) {
  successMessage.value = message
  setTimeout(() => {
    successMessage.value = ''
  }, 3000)
}

// 工具函數
function formatDateTime(date: string | undefined): string {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleString('zh-TW')
}

function getPaymentStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    'PENDING': '待付款',
    'SUCCESS': '已付款',
    'FAILED': '付款失敗',
    'REFUNDED': '已退款'
  }
  const result = labels[status] || status
  console.log(`getPaymentStatusLabel(${status}) => ${result}`)
  return result
}

function getPaymentStatusType(status: string): string {
  const types: Record<string, string> = {
    'PENDING': 'warning',
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'REFUNDED': 'info'
  }
  return types[status] || 'info'
}

function getShippingStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    'CREATED': '已建立',
    'APPROVED': '待出貨',
    'SHIPPED': '已出貨',
    'DELIVERED': '已送達',
    'PENDING': '待處理',
    'PROCESSING': '處理中'
  }
  return labels[status] || status
}

function getShippingStatusType(status: string): string {
  const types: Record<string, string> = {
    'CREATED': 'info',
    'APPROVED': 'warning',
    'SHIPPED': 'primary',
    'DELIVERED': 'success',
    'PENDING': 'warning',
    'PROCESSING': 'primary'
  }
  return types[status] || 'info'
}

function getNextStatusLabel(currentStatus: string): string {
  const nextStatusMap: Record<string, string> = {
    'CREATED': '批准',
    'PAID': '批准',
    'APPROVED': '出貨',
    'SHIPPED': '送達'
  }
  return nextStatusMap[currentStatus] || '下一步'
}

function getPaymentStatus(order: any): string {
  if (!order) return 'PENDING'

  // 從 payments 陣列取得最新的付款狀態
  if (order.payments && order.payments.length > 0) {
    return order.payments[order.payments.length - 1].status || 'PENDING'
  }
  // 或者根據訂單狀態推斷
  if (order.status === 'PAID' || order.status === 'APPROVED' || order.status === 'SHIPPED') {
    return 'SUCCESS'
  }
  return 'PENDING'
}

function getShippingStatus(order: any): string {
  if (!order) return 'CREATED'
  // 優先根據訂單狀態推斷（因為後端更新訂單狀態）
  if (order.status === 'SHIPPED') {
    return 'SHIPPED'
  } else if (order.status === 'APPROVED') {
    return 'APPROVED'
  } else if (order.status === 'PAID') {
    // 已付款但未批准，顯示已建立
    return 'CREATED'
  }
  // 從 shipping 物件取得出貨狀態（作為備選）
  if (order.shipping && order.shipping.status) {
    return order.shipping.status
  }
  return 'CREATED'
}
</script>

<style scoped>
.shipping-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.completed-text {
  color: #67C23A;
  font-size: 14px;
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

.search-type-select {
  min-width: 120px;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.status-filter {
  min-width: 150px;
}

.date-input {
  min-width: 150px;
}

.bulk-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background-color: #e6f7ff;
  border-radius: 4px;
}

.no-results {
  text-align: center;
  padding: 60px;
  color: #999;
  font-size: 18px;
}

.order-list {
  background-color: white;
  border-radius: 4px;
  overflow: hidden;
}

.order-table {
  width: 100%;
}

.order-info {
  cursor: pointer;
  padding: 8px;
}

.order-info:hover {
  background-color: #f5f7fa;
}

.order-main-info {
  display: flex;
  gap: 15px;
  margin-bottom: 8px;
}

.order-id {
  font-weight: bold;
  color: #409eff;
}

.customer-name {
  color: #666;
}

.order-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #999;
}

.amount {
  font-weight: bold;
  color: #333;
}

.order-summary {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 20px;
}

.action-buttons-group {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.action-form {
  margin-top: 20px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.error-message {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 5px;
}

.details-section {
  margin-bottom: 30px;
}

.details-section h3 {
  margin-bottom: 15px;
  color: #333;
  font-size: 16px;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.success-message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 15px 20px;
  background-color: #67c23a;
  color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  animation: slideIn 0.3s ease-out;
}

.notification-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 15px 20px;
  background-color: #409eff;
  color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  animation: slideIn 0.3s ease-out;
}

.access-denied {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 30px 50px;
  background-color: #f56c6c;
  color: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  font-size: 18px;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
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

.status-pending {
  background-color: #e6a23c;
  color: white;
}

.status-success {
  background-color: #67c23a;
  color: white;
}

.status-failed {
  background-color: #f56c6c;
  color: white;
}

.status-refunded {
  background-color: #909399;
  color: white;
}

.shipping-pending {
  background-color: #e6a23c;
  color: white;
}

.shipping-processing {
  background-color: #409eff;
  color: white;
}

.shipping-shipped {
  background-color: #409eff;
  color: white;
}

.shipping-delivered {
  background-color: #67c23a;
  color: white;
}
</style>
