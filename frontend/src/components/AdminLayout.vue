<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-content">
        <h2 class="site-title" @click="goToHome">電商購物平台 - 管理系統</h2>
        <div class="header-actions">
          <router-link to="/admin/products" class="nav-link">
            商品管理
          </router-link>
          <router-link to="/admin/orders" class="nav-link">
            訂單管理
          </router-link>
          <router-link to="/admin/shipping" class="nav-link">
            出貨管理
          </router-link>

          <span class="username">{{ authStore.user?.username }}</span>
          <el-button @click="handleLogout" size="small">登出</el-button>
        </div>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function goToHome() {
  router.push('/admin/products')
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.header {
  background-color: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.nav-link {
  color: white;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.nav-link:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.site-title {
  cursor: pointer;
  transition: opacity 0.3s;
}

.site-title:hover {
  opacity: 0.8;
}

.username {
  font-size: 14px;
}

.main-content {
  padding: 20px;
  background-color: #f5f5f5;
}
</style>
