<template>
  <div class="admin-dashboard-container">
    <!-- 導航欄 -->
    <el-header class="header">
      <div class="header-left">
        <h2>智能訂單管理系統 - Admin Dashboard</h2>
      </div>
      <div class="header-right">
        <el-space>
          <span data-cy="user-role" class="user-info">Admin</span>
          <span data-cy="username-display" class="username">{{ authStore.user?.username }}</span>
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

    <!-- 主要內容區域 -->
    <el-container>
      <!-- 側邊欄 -->
      <el-aside width="250px" class="sidebar">
        <el-menu
          default-active="admin-dashboard"
          class="sidebar-menu"
          router
          :default-openeds="['orders', 'products']"
        >
          <el-menu-item index="/admin/dashboard" data-cy="menu-admin-dashboard">
            <el-icon><House /></el-icon>
            <span>管理儀表板</span>
          </el-menu-item>

          <!-- 商品管理 -->
          <el-sub-menu index="products" data-cy="menu-product-management-group">
            <template #title>
              <el-icon><Goods /></el-icon>
              <span>商品管理</span>
            </template>
            <el-menu-item index="/admin/products" data-cy="menu-product-management">
              <el-icon><Edit /></el-icon>
              <span>商品管理</span>
            </el-menu-item>
            <el-menu-item index="/products" data-cy="menu-products">
              <el-icon><View /></el-icon>
              <span>商品瀏覽</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 訂單管理 -->
          <el-sub-menu index="orders" data-cy="menu-orders-group">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>訂單管理</span>
            </template>
            <el-menu-item index="/admin/orders" data-cy="menu-all-orders">
              <el-icon><List /></el-icon>
              <span>所有訂單</span>
            </el-menu-item>
            <el-menu-item index="/admin/shipping" data-cy="menu-shipping-management">
              <el-icon><Van /></el-icon>
              <span>出貨管理</span>
            </el-menu-item>
          </el-sub-menu>

          <!-- 系統管理 -->
          <el-sub-menu index="system">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系統管理</span>
            </template>
            <el-menu-item index="/admin/users">
              <el-icon><User /></el-icon>
              <span>用戶管理</span>
            </el-menu-item>
            <el-menu-item index="/admin/settings">
              <el-icon><Tools /></el-icon>
              <span>系統設定</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <!-- 主要內容 -->
      <el-main class="main-content">
        <div class="admin-dashboard-content">
          <el-row :gutter="20">
            <!-- 歡迎區塊 -->
            <el-col :span="24">
              <el-card class="welcome-card">
                <h3>歡迎，管理員 {{ authStore.user?.username }}！</h3>
                <p>您可以在這裡管理訂單、商品、出貨和系統設定。</p>
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="20" style="margin-top: 20px;">
            <!-- 核心統計 -->
            <el-col :span="6">
              <el-card class="stat-card orders">
                <div class="stat-content">
                  <el-icon class="stat-icon" color="#409eff"><Document /></el-icon>
                  <div class="stat-info">
                    <div class="stat-number">24</div>
                    <div class="stat-label">總訂單數</div>
                  </div>
                </div>
              </el-card>
            </el-col>

            <el-col :span="6">
              <el-card class="stat-card products">
                <div class="stat-content">
                  <el-icon class="stat-icon" color="#67c23a"><Goods /></el-icon>
                  <div class="stat-info">
                    <div class="stat-number">156</div>
                    <div class="stat-label">商品總數</div>
                  </div>
                </div>
              </el-card>
            </el-col>

            <el-col :span="6">
              <el-card class="stat-card shipping">
                <div class="stat-content">
                  <el-icon class="stat-icon" color="#e6a23c"><Van /></el-icon>
                  <div class="stat-info">
                    <div class="stat-number">8</div>
                    <div class="stat-label">待出貨訂單</div>
                  </div>
                </div>
              </el-card>
            </el-col>

            <el-col :span="6">
              <el-card class="stat-card revenue">
                <div class="stat-content">
                  <el-icon class="stat-icon" color="#f56c6c"><Money /></el-icon>
                  <div class="stat-info">
                    <div class="stat-number">$15,240</div>
                    <div class="stat-label">今日營收</div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="20" style="margin-top: 20px;">
            <!-- 待處理訂單 -->
            <el-col :span="16">
              <el-card>
                <template #header>
                  <div style="display: flex; align-items: center; justify-content: space-between;">
                    <span>待處理訂單</span>
                    <el-button type="primary" size="small" @click="$router.push('/admin/orders')">
                      查看全部
                    </el-button>
                  </div>
                </template>
                <el-table :data="pendingOrders" stripe>
                  <el-table-column prop="id" label="訂單編號" width="120" />
                  <el-table-column prop="customer" label="客戶" width="100" />
                  <el-table-column prop="status" label="狀態" width="100">
                    <template #default="{ row }">
                      <el-tag :type="getStatusType(row.status)">
                        {{ getStatusText(row.status) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="total" label="總金額" width="100">
                    <template #default="{ row }">
                      ${{ row.total }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="createdAt" label="建立時間" />
                  <el-table-column label="操作" width="120">
                    <template #default="{ row }">
                      <el-button
                        type="primary"
                        size="small"
                        @click="handleProcessOrder(row)"
                      >
                        處理
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </el-col>

            <!-- 快速操作 -->
            <el-col :span="8">
              <el-card>
                <template #header>
                  <span>快速操作</span>
                </template>
                <div class="quick-actions">
                  <el-button
                    type="primary"
                    @click="$router.push('/admin/products')"
                    class="action-btn"
                  >
                    <el-icon><Goods /></el-icon>
                    商品管理
                  </el-button>
                  <el-button
                    type="success"
                    @click="$router.push('/admin/orders')"
                    class="action-btn"
                  >
                    <el-icon><Document /></el-icon>
                    訂單管理
                  </el-button>
                  <el-button
                    type="warning"
                    @click="$router.push('/admin/shipping')"
                    class="action-btn"
                  >
                    <el-icon><Van /></el-icon>
                    出貨管理
                  </el-button>
                  <el-button
                    type="info"
                    @click="$router.push('/admin/settings')"
                    class="action-btn"
                  >
                    <el-icon><Setting /></el-icon>
                    系統設定
                  </el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  House,
  Goods,
  Document,
  List,
  Van,
  Setting,
  User,
  Tools,
  Edit,
  View,
  Money
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// 模擬待處理訂單數據
const pendingOrders = ref([
  { id: 'ORD-001', customer: 'customer1', status: 'CREATED', total: 299.99, createdAt: '2024-01-15 10:30' },
  { id: 'ORD-002', customer: 'customer2', status: 'PAID', total: 599.50, createdAt: '2024-01-15 11:15' },
  { id: 'ORD-003', customer: 'customer1', status: 'PAID', total: 199.00, createdAt: '2024-01-15 14:20' },
  { id: 'ORD-004', customer: 'customer3', status: 'CREATED', total: 399.99, createdAt: '2024-01-15 16:45' }
])

// 處理登出
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '確定要登出嗎？',
      '確認',
      {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    authStore.logout()
    ElMessage.success('已成功登出')
    router.push('/login')
  } catch {
    // 用戶取消
  }
}

// 處理訂單
const handleProcessOrder = (order: any) => {
  ElMessage.info(`正在處理訂單 ${order.id}`)
  // 這裡可以添加具體的訂單處理邏輯
}

// 獲取狀態類型
const getStatusType = (status: string) => {
  switch (status) {
    case 'PAID':
      return 'success'
    case 'SHIPPED':
      return 'info'
    case 'CREATED':
      return 'warning'
    case 'CANCELLED':
      return 'danger'
    default:
      return ''
  }
}

// 獲取狀態文字
const getStatusText = (status: string) => {
  switch (status) {
    case 'CREATED':
      return '待付款'
    case 'PAID':
      return '已付款'
    case 'SHIPPED':
      return '已出貨'
    case 'CANCELLED':
      return '已取消'
    default:
      return status
  }
}

onMounted(() => {
  // 檢查用戶權限
  if (authStore.user?.role !== 'ADMIN') {
    ElMessage.error('您沒有權限訪問此頁面')
    router.push('/')
  }
})
</script>

<style scoped>
.admin-dashboard-container {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  background: white;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-left h2 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  color: #f56c6c;
  font-weight: 600;
}

.username {
  color: #303133;
  font-weight: 500;
}

.sidebar {
  background: white;
  border-right: 1px solid #e4e7ed;
}

.sidebar-menu {
  border-right: none;
  height: calc(100vh - 61px);
}

.main-content {
  padding: 20px;
  background-color: #f5f5f5;
}

.welcome-card {
  text-align: center;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.welcome-card :deep(.el-card__body) {
  background: transparent;
}

.welcome-card h3 {
  margin: 0 0 10px 0;
  font-size: 24px;
}

.welcome-card p {
  margin: 0;
  opacity: 0.9;
}

.stat-card {
  height: 100px;
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.stat-icon {
  font-size: 40px;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.action-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
}

:deep(.el-menu-item.is-active) {
  color: #409eff;
  background-color: #ecf5ff;
}

:deep(.el-card) {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #fafafa;
}
</style>