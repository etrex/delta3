<template>
  <div class="dashboard-container">
    <h1>歡迎回來, {{ authStore.user?.username }}!</h1>

    <div class="quick-links">
      <el-card v-if="authStore.isCustomer" class="quick-link-card" @click="goTo('/products')">
        <h3>瀏覽商品</h3>
        <p>查看最新商品</p>
      </el-card>

      <el-card v-if="authStore.isCustomer" class="quick-link-card" @click="goTo('/orders')">
        <h3>我的訂單</h3>
        <p>查看訂單狀態</p>
      </el-card>

      <el-card v-if="authStore.isAdmin" class="quick-link-card" @click="goTo('/admin/orders')">
        <h3>訂單管理</h3>
        <p>管理所有訂單</p>
      </el-card>

      <el-card v-if="authStore.isAdmin" class="quick-link-card" @click="goTo('/admin/shipping')">
        <h3>出貨管理</h3>
        <p>處理訂單出貨</p>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function goTo(path: string) {
  router.push(path)
}
</script>

<style scoped>
.dashboard-container {
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 40px;
  color: #333;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.quick-link-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.quick-link-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.quick-link-card h3 {
  margin: 0 0 12px 0;
  color: #409EFF;
}

.quick-link-card p {
  margin: 0;
  color: #909399;
}
</style>
