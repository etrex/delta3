<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-content">
        <h2 class="site-title" @click="goToHome">卡米購 - 管理系統</h2>

        <!-- Desktop Navigation -->
        <div class="header-actions desktop-nav">
          <router-link to="/admin/dashboard" class="nav-link">
            總覽
          </router-link>
          <router-link to="/admin/products" class="nav-link" data-cy="menu-product-management">
            商品管理
          </router-link>
          <router-link to="/admin/orders" class="nav-link" data-cy="menu-all-orders">
            訂單管理
          </router-link>
          <router-link to="/admin/shipping" class="nav-link" data-cy="menu-shipping-management">
            出貨管理
          </router-link>
          <router-link to="/admin/chat" class="nav-link">
            客服管理
          </router-link>
          <router-link to="/admin/faqs" class="nav-link">
            FAQ 管理
          </router-link>
          <router-link to="/products" class="nav-link" data-cy="menu-products">
            商品列表
          </router-link>

          <span class="username" data-cy="username-display">{{ authStore.user?.username }}</span>
          <span class="user-role" data-cy="user-role">Admin</span>
          <el-button data-cy="logout-btn" @click="handleLogout" size="small">登出</el-button>
        </div>

        <!-- Mobile Navigation -->
        <div class="mobile-nav">
          <el-button class="menu-button" @click="menuDrawerVisible = true" text>
            <el-icon :size="24"><Menu /></el-icon>
          </el-button>
        </div>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>

    <!-- 手機版選單 drawer -->
    <el-drawer
      v-model="menuDrawerVisible"
      title="管理選單"
      direction="ltr"
      size="280px"
    >
      <div class="mobile-menu">
        <div class="mobile-menu-user">
          <div class="mobile-username">{{ authStore.user?.username }}</div>
          <div class="mobile-user-role">Admin</div>
        </div>

        <div class="mobile-menu-items">
          <router-link to="/admin/dashboard" class="mobile-menu-item" @click="menuDrawerVisible = false">
            <el-icon><DataAnalysis /></el-icon>
            <span>總覽</span>
          </router-link>
          <router-link to="/admin/products" class="mobile-menu-item" @click="menuDrawerVisible = false">
            <el-icon><Box /></el-icon>
            <span>商品管理</span>
          </router-link>
          <router-link to="/admin/orders" class="mobile-menu-item" @click="menuDrawerVisible = false">
            <el-icon><DocumentCopy /></el-icon>
            <span>訂單管理</span>
          </router-link>
          <router-link to="/admin/shipping" class="mobile-menu-item" @click="menuDrawerVisible = false">
            <el-icon><Van /></el-icon>
            <span>出貨管理</span>
          </router-link>
          <router-link to="/admin/chat" class="mobile-menu-item" @click="menuDrawerVisible = false">
            <el-icon><ChatLineRound /></el-icon>
            <span>客服管理</span>
          </router-link>
          <router-link to="/admin/faqs" class="mobile-menu-item" @click="menuDrawerVisible = false">
            <el-icon><QuestionFilled /></el-icon>
            <span>FAQ 管理</span>
          </router-link>
          <router-link to="/products" class="mobile-menu-item" @click="menuDrawerVisible = false">
            <el-icon><ShoppingBag /></el-icon>
            <span>商品列表</span>
          </router-link>
        </div>

        <div class="mobile-menu-footer">
          <el-button @click="handleLogout" type="danger" size="large" style="width: 100%">
            <el-icon><SwitchButton /></el-icon>
            <span>登出</span>
          </el-button>
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  Menu,
  DataAnalysis,
  Box,
  DocumentCopy,
  Van,
  ChatLineRound,
  QuestionFilled,
  ShoppingBag,
  SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const menuDrawerVisible = ref(false)

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function goToHome() {
  router.push('/admin/dashboard')
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

/* Mobile responsive styles */
.mobile-nav {
  display: none;
  align-items: center;
}

.menu-button {
  color: white;
  padding: 8px;
}

.mobile-menu {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.mobile-menu-user {
  padding: 20px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  margin: -20px -20px 20px -20px;
}

.mobile-username {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 4px;
}

.mobile-user-role {
  font-size: 14px;
  opacity: 0.9;
  background-color: rgba(255, 255, 255, 0.3);
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
}

.mobile-menu-items {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mobile-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  color: #333;
  text-decoration: none;
  border-radius: 8px;
  transition: background-color 0.3s;
}

.mobile-menu-item:hover {
  background-color: #f0f0f0;
}

.mobile-menu-item .el-icon {
  font-size: 20px;
  color: #f5576c;
}

.mobile-menu-footer {
  padding: 16px 0;
  border-top: 1px solid #eee;
}

.user-role {
  font-size: 12px;
  padding: 2px 8px;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

@media (max-width: 768px) {
  .desktop-nav {
    display: none !important;
  }

  .mobile-nav {
    display: flex !important;
  }

  .header {
    padding: 0 16px;
    height: 56px;
  }

  .site-title {
    font-size: 16px;
    margin: 0;
  }

  .main-content {
    padding: 12px;
  }
}
</style>
