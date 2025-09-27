<template>
  <div class="dashboard-container">
    <!-- 導航欄 -->
    <el-header class="header">
      <div class="header-left">
        <h2>智能訂單管理系統 - Customer Dashboard</h2>
      </div>
      <div class="header-right">
        <el-space>
          <span data-cy="user-role" class="user-info">Customer</span>
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
          default-active="dashboard"
          class="sidebar-menu"
          router
          :default-openeds="['orders']"
        >
          <el-menu-item index="/dashboard" data-cy="menu-dashboard">
            <el-icon><House /></el-icon>
            <span>儀表板</span>
          </el-menu-item>

          <el-menu-item index="/products" data-cy="menu-products">
            <el-icon><Goods /></el-icon>
            <span>商品瀏覽</span>
          </el-menu-item>

          <el-menu-item index="/cart" data-cy="menu-cart">
            <el-icon><ShoppingCart /></el-icon>
            <span>購物車</span>
            <el-badge :value="cartStore.cartCount" :hidden="cartStore.cartCount === 0" class="cart-badge" data-cy="cart-count" />
          </el-menu-item>

          <el-sub-menu index="orders" data-cy="menu-orders-group">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>訂單管理</span>
            </template>
            <el-menu-item index="/orders" data-cy="menu-orders">
              <el-icon><List /></el-icon>
              <span>我的訂單</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <!-- 主要內容 -->
      <el-main class="main-content">
        <div class="dashboard-content">
          <el-row :gutter="20">
            <!-- 歡迎區塊 -->
            <el-col :span="24">
              <el-card class="welcome-card">
                <h3>歡迎回來，{{ authStore.user?.username }}！</h3>
                <p>您可以在這裡管理您的訂單、瀏覽商品和查看購物車。</p>
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="20" style="margin-top: 20px;">
            <!-- 快速統計 -->
            <el-col :span="8">
              <el-card class="stat-card">
                <div class="stat-content">
                  <el-icon class="stat-icon" color="#409eff"><Document /></el-icon>
                  <div class="stat-info">
                    <div class="stat-number">5</div>
                    <div class="stat-label">總訂單數</div>
                  </div>
                </div>
              </el-card>
            </el-col>

            <el-col :span="8">
              <el-card class="stat-card">
                <div class="stat-content">
                  <el-icon class="stat-icon" color="#67c23a"><ShoppingCart /></el-icon>
                  <div class="stat-info">
                    <div class="stat-number">{{ cartStore.cartCount }}</div>
                    <div class="stat-label">購物車商品</div>
                  </div>
                </div>
              </el-card>
            </el-col>

            <el-col :span="8">
              <el-card class="stat-card">
                <div class="stat-content">
                  <el-icon class="stat-icon" color="#e6a23c"><Clock /></el-icon>
                  <div class="stat-info">
                    <div class="stat-number">1</div>
                    <div class="stat-label">待付款訂單</div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="20" style="margin-top: 20px;">
            <!-- 最近訂單 -->
            <el-col :span="12">
              <el-card>
                <template #header>
                  <span>最近訂單</span>
                </template>
                <el-table :data="recentOrders" stripe>
                  <el-table-column prop="id" label="訂單編號" width="120" />
                  <el-table-column prop="status" label="狀態">
                    <template #default="{ row }">
                      <el-tag :type="getStatusType(row.status)">
                        {{ getStatusText(row.status) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="total" label="總金額">
                    <template #default="{ row }">
                      ${{ row.total }}
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </el-col>

            <!-- 快速操作 -->
            <el-col :span="12">
              <el-card>
                <template #header>
                  <span>快速操作</span>
                </template>
                <div class="quick-actions">
                  <el-button
                    type="primary"
                    @click="$router.push('/products')"
                    class="action-btn"
                  >
                    <el-icon><Goods /></el-icon>
                    瀏覽商品
                  </el-button>
                  <el-button
                    type="success"
                    @click="$router.push('/cart')"
                    class="action-btn"
                  >
                    <el-icon><ShoppingCart /></el-icon>
                    查看購物車
                  </el-button>
                  <el-button
                    type="info"
                    @click="$router.push('/orders')"
                    class="action-btn"
                  >
                    <el-icon><Document /></el-icon>
                    我的訂單
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
  ShoppingCart,
  Document,
  List,
  Clock
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

// 模擬最近訂單數據
const recentOrders = ref([
  { id: 'ORD-001', status: 'PAID', total: 299.99 },
  { id: 'ORD-002', status: 'SHIPPED', total: 599.50 },
  { id: 'ORD-003', status: 'CREATED', total: 199.00 }
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
      return '已建立'
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
  if (authStore.user?.role !== 'CUSTOMER') {
    ElMessage.error('您沒有權限訪問此頁面')
    router.push('/')
  }
})
</script>

<style scoped>
.dashboard-container {
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
  color: #67c23a;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
  font-size: 32px;
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

.cart-badge :deep(.el-badge__content) {
  margin-left: 10px;
}
</style>