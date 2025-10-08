<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="admin-dashboard">
    <h1>管理後台總覽</h1>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon orders">
            <el-icon :size="40"><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalOrders }}</div>
            <div class="stat-label">總訂單數</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon products">
            <el-icon :size="40"><Goods /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalProducts }}</div>
            <div class="stat-label">商品總數</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon revenue">
            <el-icon :size="40"><Money /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">${{ stats.totalRevenue.toFixed(2) }}</div>
            <div class="stat-label">總營收</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon pending">
            <el-icon :size="40"><Clock /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.pendingOrders }}</div>
            <div class="stat-label">待處理訂單</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="quick-actions">
      <el-col :span="24">
        <el-card>
          <template #header>
            <h3>快速操作</h3>
          </template>
          <div class="actions-grid">
            <el-button type="primary" @click="goTo('/admin/products')">
              <el-icon><Goods /></el-icon>
              商品管理
            </el-button>
            <el-button type="primary" @click="goTo('/admin/orders')">
              <el-icon><Document /></el-icon>
              訂單管理
            </el-button>
            <el-button type="primary" @click="goTo('/admin/shipping')">
              <el-icon><Van /></el-icon>
              出貨管理
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="recent-section">
      <el-col :span="12">
        <el-card>
          <template #header>
            <h3>最近訂單</h3>
          </template>
          <el-table :data="recentOrders" stripe>
            <el-table-column prop="orderNo" label="訂單號" width="120" />
            <el-table-column prop="totalAmount" label="金額" width="100">
              <template #default="{ row }">
                ${{ row.totalAmount.toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="狀態" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="goTo(`/admin/orders/${row.id}`)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <h3>低庫存商品</h3>
          </template>
          <el-table :data="lowStockProducts" stripe>
            <el-table-column prop="name" label="商品名稱" />
            <el-table-column prop="stock" label="庫存" width="80">
              <template #default="{ row }">
                <span :class="{ 'low-stock': row.stock < 10 }">{{ row.stock }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="goTo('/admin/products')">
                  補貨
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, Goods, Money, Clock, Van } from '@element-plus/icons-vue'
import ordersApi from '@/api/orders'
import productsApi from '@/api/products'

const router = useRouter()

const stats = ref({
  totalOrders: 0,
  totalProducts: 0,
  totalRevenue: 0,
  pendingOrders: 0
})

const recentOrders = ref<any[]>([])
const lowStockProducts = ref<any[]>([])

onMounted(async () => {
  await loadDashboardData()
})

async function loadDashboardData() {
  try {
    // Load orders
    const ordersResponse = await ordersApi.getOrders({ size: 5, sort: 'createdAt,desc' })
    const orders = ordersResponse.content || ordersResponse || []
    recentOrders.value = orders.slice(0, 5)

    stats.value.totalOrders = ordersResponse.totalElements || orders.length
    stats.value.pendingOrders = orders.filter((o: any) =>
      o.status === 'CREATED' || o.status === 'PAID'
    ).length

    // Calculate total revenue
    stats.value.totalRevenue = orders.reduce((sum: number, order: any) => {
      if (order.status === 'PAID' || order.status === 'APPROVED' || order.status === 'SHIPPED') {
        return sum + (order.totalAmount || 0)
      }
      return sum
    }, 0)

    // Load products
    const productsResponse = await productsApi.getProducts()
    const products = productsResponse.content || productsResponse || []
    stats.value.totalProducts = products.length

    // Find low stock products (stock < 10)
    lowStockProducts.value = products
      .filter((p: any) => p.stock < 10)
      .sort((a: any, b: any) => a.stock - b.stock)
      .slice(0, 5)

  } catch (error) {
    console.error('Failed to load dashboard data:', error)
    ElMessage.error('載入數據失敗')
  }
}

function goTo(path: string) {
  router.push(path)
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
.admin-dashboard {
  max-width: 1400px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 30px;
  color: #333;
}

.stats-row {
  margin-bottom: 30px;
}

.stat-card {
  text-align: center;
  cursor: pointer;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.orders {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.products {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.revenue {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.pending {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.quick-actions {
  margin-bottom: 30px;
}

.actions-grid {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.actions-grid .el-button {
  flex: 1;
  max-width: 200px;
  height: 80px;
  font-size: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.recent-section {
  margin-bottom: 30px;
}

.low-stock {
  color: #F56C6C;
  font-weight: bold;
}

h3 {
  margin: 0;
  color: #333;
}
</style>
