/*
 * Copyright (c) 2025 Etrex Kuo. All rights reserved.
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    // Customer routes
    {
      path: '/',
      component: () => import('@/components/CustomerLayout.vue'),
      redirect: '/products',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/customer/Dashboard.vue')
        },
        {
          path: 'products',
          name: 'Products',
          component: () => import('@/views/customer/Products.vue')
        },
        {
          path: 'checkout',
          name: 'Checkout',
          component: () => import('@/views/customer/Checkout.vue')
        },
        {
          path: 'orders',
          name: 'Orders',
          component: () => import('@/views/customer/Orders.vue')
        },
        {
          path: 'orders/:id',
          name: 'OrderDetail',
          component: () => import('@/views/customer/OrderDetail.vue')
        }
      ]
    },
    // Admin routes
    {
      path: '/admin',
      component: () => import('@/components/AdminLayout.vue'),
      redirect: '/admin/dashboard',
      meta: { requiresAdmin: true },
      children: [
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('@/views/admin/Dashboard.vue'),
          meta: { requiresAdmin: true }
        },
        {
          path: 'products',
          name: 'ProductManagement',
          component: () => import('@/views/admin/ProductManagement.vue'),
          meta: { requiresAdmin: true }
        },
        {
          path: 'orders',
          name: 'AdminOrders',
          component: () => import('@/views/admin/Orders.vue'),
          meta: { requiresAdmin: true }
        },
        {
          path: 'orders/:id',
          name: 'AdminOrderDetail',
          component: () => import('@/views/admin/OrderDetail.vue'),
          meta: { requiresAdmin: true }
        },
        {
          path: 'shipping',
          name: 'Shipping',
          component: () => import('@/views/admin/Shipping.vue'),
          meta: { requiresAdmin: true }
        },
        {
          path: 'shipping/reports',
          name: 'ShippingReports',
          component: () => import('@/views/admin/ShippingReports.vue'),
          meta: { requiresAdmin: true }
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.path !== '/login' && !authStore.isAuthenticated) {
    next('/login')
  } else if (to.meta.requiresAdmin && authStore.user?.role !== 'ADMIN') {
    next('/products')
  } else {
    next()
  }
})

export default router