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

    <div class="reports-container">
      <div class="page-header">
        <h1>出貨報表與統計</h1>
        <el-button
          type="primary"
          data-cy="export-report-btn"
          @click="showExportDialog = true"
        >
          匯出報表
        </el-button>
      </div>

      <!-- 出貨統計數據 -->
      <div class="shipping-stats" data-cy="shipping-stats">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content" data-cy="total-orders">
                <div class="stat-icon total-orders">
                  <el-icon :size="32"><Document /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">總訂單數</div>
                  <div class="stat-value">{{ statistics.totalOrders }}</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content" data-cy="pending-shipments">
                <div class="stat-icon pending-shipments">
                  <el-icon :size="32"><Clock /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">待出貨訂單</div>
                  <div class="stat-value">{{ statistics.pendingShipments }}</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content" data-cy="shipped-today">
                <div class="stat-icon shipped-today">
                  <el-icon :size="32"><Van /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">今日已出貨</div>
                  <div class="stat-value">{{ statistics.shippedToday }}</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content" data-cy="delivery-rate">
                <div class="stat-icon delivery-rate">
                  <el-icon :size="32"><CircleCheck /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">準時送達率</div>
                  <div class="stat-value">{{ statistics.deliveryRate }}%</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 出貨趨勢圖表 -->
      <el-card shadow="hover" class="chart-card">
        <template #header>
          <div class="card-header">
            <h3>出貨趨勢圖</h3>
          </div>
        </template>
        <div class="chart-container" data-cy="shipping-trend-chart">
          <div class="chart-placeholder">
            <svg viewBox="0 0 800 300" class="trend-chart">
              <!-- X軸 -->
              <line x1="50" y1="250" x2="750" y2="250" stroke="#e4e7ed" stroke-width="2" />
              <!-- Y軸 -->
              <line x1="50" y1="50" x2="50" y2="250" stroke="#e4e7ed" stroke-width="2" />

              <!-- 趨勢線 -->
              <polyline
                points="50,200 150,180 250,160 350,140 450,120 550,110 650,100 750,90"
                fill="none"
                stroke="#409eff"
                stroke-width="3"
              />

              <!-- 數據點 -->
              <circle cx="50" cy="200" r="5" fill="#409eff" />
              <circle cx="150" cy="180" r="5" fill="#409eff" />
              <circle cx="250" cy="160" r="5" fill="#409eff" />
              <circle cx="350" cy="140" r="5" fill="#409eff" />
              <circle cx="450" cy="120" r="5" fill="#409eff" />
              <circle cx="550" cy="110" r="5" fill="#409eff" />
              <circle cx="650" cy="100" r="5" fill="#409eff" />
              <circle cx="750" cy="90" r="5" fill="#409eff" />

              <!-- X軸標籤 -->
              <text x="50" y="270" text-anchor="middle" font-size="12" fill="#606266">週一</text>
              <text x="150" y="270" text-anchor="middle" font-size="12" fill="#606266">週二</text>
              <text x="250" y="270" text-anchor="middle" font-size="12" fill="#606266">週三</text>
              <text x="350" y="270" text-anchor="middle" font-size="12" fill="#606266">週四</text>
              <text x="450" y="270" text-anchor="middle" font-size="12" fill="#606266">週五</text>
              <text x="550" y="270" text-anchor="middle" font-size="12" fill="#606266">週六</text>
              <text x="650" y="270" text-anchor="middle" font-size="12" fill="#606266">週日</text>
              <text x="750" y="270" text-anchor="middle" font-size="12" fill="#606266">今日</text>

              <!-- Y軸標籤 -->
              <text x="35" y="250" text-anchor="end" font-size="12" fill="#606266">0</text>
              <text x="35" y="200" text-anchor="end" font-size="12" fill="#606266">25</text>
              <text x="35" y="150" text-anchor="end" font-size="12" fill="#606266">50</text>
              <text x="35" y="100" text-anchor="end" font-size="12" fill="#606266">75</text>
              <text x="35" y="50" text-anchor="end" font-size="12" fill="#606266">100</text>
            </svg>
          </div>
          <div class="chart-legend" data-cy="chart-legend">
            <div class="legend-item">
              <span class="legend-color" style="background-color: #409eff;"></span>
              <span class="legend-label">每日出貨量</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 配送異常警告 -->
      <el-card shadow="hover" class="alerts-card">
        <template #header>
          <div class="card-header">
            <h3>配送異常警告</h3>
          </div>
        </template>
        <div class="shipping-alerts" data-cy="shipping-alerts">
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="alert-section" data-cy="overdue-shipments">
                <div class="alert-header">
                  <el-icon :size="20" color="#f56c6c"><WarningFilled /></el-icon>
                  <h4>逾期未出貨訂單</h4>
                </div>
                <div class="alert-list">
                  <div
                    v-for="item in overdueShipments"
                    :key="item.id"
                    class="alert-item overdue"
                  >
                    <div class="alert-item-info">
                      <span class="order-number">{{ item.orderNumber }}</span>
                      <span class="customer-name">{{ item.customerName }}</span>
                    </div>
                    <div class="alert-item-time">
                      逾期 {{ item.daysOverdue }} 天
                    </div>
                  </div>
                  <div v-if="overdueShipments.length === 0" class="no-alerts">
                    目前無逾期訂單
                  </div>
                </div>
              </div>
            </el-col>

            <el-col :span="12">
              <div class="alert-section" data-cy="delayed-deliveries">
                <div class="alert-header">
                  <el-icon :size="20" color="#e6a23c"><WarnTriangleFilled /></el-icon>
                  <h4>配送延遲訂單</h4>
                </div>
                <div class="alert-list">
                  <div
                    v-for="item in delayedDeliveries"
                    :key="item.id"
                    class="alert-item delayed"
                  >
                    <div class="alert-item-info">
                      <span class="order-number">{{ item.orderNumber }}</span>
                      <span class="customer-name">{{ item.customerName }}</span>
                    </div>
                    <div class="alert-item-time">
                      延遲 {{ item.daysDelayed }} 天
                    </div>
                  </div>
                  <div v-if="delayedDeliveries.length === 0" class="no-alerts">
                    目前無延遲訂單
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-card>
    </div>

    <!-- 匯出報表對話框 -->
    <el-dialog
      v-model="showExportDialog"
      title="匯出出貨報表"
      width="500px"
      data-cy="export-options"
    >
      <el-form label-width="100px">
        <el-form-item label="匯出格式">
          <el-select
            v-model="exportFormat"
            data-cy="export-format-select"
            placeholder="選擇格式"
            style="width: 100%;"
          >
            <el-option label="Excel" value="Excel" />
            <el-option label="PDF" value="PDF" />
            <el-option label="CSV" value="CSV" />
          </el-select>
        </el-form-item>
        <el-form-item label="時間範圍">
          <el-select
            v-model="exportDateRange"
            data-cy="date-range-select"
            placeholder="選擇時間範圍"
            style="width: 100%;"
          >
            <el-option label="今日" value="今日" />
            <el-option label="本週" value="本週" />
            <el-option label="本月" value="本月" />
            <el-option label="本季" value="本季" />
            <el-option label="本年" value="本年" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showExportDialog = false">取消</el-button>
        <el-button
          type="primary"
          data-cy="confirm-export-btn"
          @click="handleExportReport"
        >
          確認匯出
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  Document,
  Clock,
  Van,
  CircleCheck,
  WarningFilled,
  WarnTriangleFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

// 統計數據
const statistics = ref({
  totalOrders: 1247,
  pendingShipments: 38,
  shippedToday: 156,
  deliveryRate: 96.8
})

// 逾期未出貨訂單
const overdueShipments = ref([
  {
    id: 1,
    orderNumber: 'ORD-2024-001',
    customerName: '王小明',
    daysOverdue: 3
  },
  {
    id: 2,
    orderNumber: 'ORD-2024-015',
    customerName: '李大華',
    daysOverdue: 2
  },
  {
    id: 3,
    orderNumber: 'ORD-2024-028',
    customerName: '陳美玲',
    daysOverdue: 1
  }
])

// 配送延遲訂單
const delayedDeliveries = ref([
  {
    id: 1,
    orderNumber: 'ORD-2024-045',
    customerName: '張志明',
    daysDelayed: 2
  },
  {
    id: 2,
    orderNumber: 'ORD-2024-067',
    customerName: '林淑芬',
    daysDelayed: 1
  }
])

// 匯出相關
const showExportDialog = ref(false)
const exportFormat = ref('Excel')
const exportDateRange = ref('本月')

const handleLogout = async () => {
  await authStore.logout()
  router.push('/login')
}

const handleExportReport = () => {
  ElMessage.success('報表匯出成功')
  showExportDialog.value = false
}
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

.reports-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0;
  color: #333;
}

/* 統計卡片 */
.shipping-stats {
  margin-bottom: 30px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.total-orders {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.pending-shipments {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.shipped-today {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.delivery-rate {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

/* 圖表卡片 */
.chart-card {
  margin-bottom: 30px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.chart-container {
  padding: 20px 0;
}

.chart-placeholder {
  width: 100%;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafafa;
  border-radius: 8px;
}

.trend-chart {
  width: 100%;
  height: 100%;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-top: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-color {
  width: 20px;
  height: 4px;
  border-radius: 2px;
}

.legend-label {
  font-size: 14px;
  color: #606266;
}

/* 警告卡片 */
.alerts-card {
  margin-bottom: 30px;
}

.shipping-alerts {
  padding: 10px 0;
}

.alert-section {
  background-color: #fafafa;
  border-radius: 8px;
  padding: 20px;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
}

.alert-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: white;
  border-radius: 6px;
  border-left: 4px solid;
}

.alert-item.overdue {
  border-left-color: #f56c6c;
}

.alert-item.delayed {
  border-left-color: #e6a23c;
}

.alert-item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-number {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.customer-name {
  font-size: 13px;
  color: #909399;
}

.alert-item-time {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.no-alerts {
  text-align: center;
  padding: 30px;
  color: #909399;
  font-size: 14px;
}

:deep(.el-card__header) {
  padding: 18px 20px;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>
