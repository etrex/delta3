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
    {
      path: '/',
      name: 'Layout',
      component: () => import('@/components/Layout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue')
        },
        {
          path: 'products',
          name: 'Products',
          component: () => import('@/views/Products.vue')
        },
        {
          path: 'cart',
          name: 'Cart',
          component: () => import('@/views/Cart.vue')
        },
        {
          path: 'checkout',
          name: 'Checkout',
          component: () => import('@/views/Checkout.vue')
        },
        {
          path: 'orders',
          name: 'Orders',
          component: () => import('@/views/Orders.vue')
        },
        {
          path: 'orders/:id',
          name: 'OrderDetail',
          component: () => import('@/views/OrderDetail.vue')
        },
        {
          path: 'admin/orders',
          name: 'AdminOrders',
          component: () => import('@/views/admin/Orders.vue'),
          meta: { requiresAdmin: true }
        },
        {
          path: 'admin/shipping',
          name: 'Shipping',
          component: () => import('@/views/admin/Shipping.vue'),
          meta: { requiresAdmin: true }
        },
        {
          path: 'admin/shipping/reports',
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
    next('/dashboard')
  } else {
    next()
  }
})

export default router