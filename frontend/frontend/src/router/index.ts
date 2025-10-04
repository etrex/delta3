import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/views/Dashboard.vue'),
      meta: { requiresAuth: true, roles: ['CUSTOMER'] }
    },
    {
      path: '/admin/dashboard',
      name: 'AdminDashboard',
      component: () => import('@/views/admin/AdminDashboard.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/products',
      name: 'Products',
      component: () => import('@/views/Products.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/orders',
      name: 'Orders',
      component: () => import('@/views/Orders.vue'),
      meta: { requiresAuth: true, roles: ['CUSTOMER'] }
    },
    {
      path: '/cart',
      name: 'Cart',
      component: () => import('@/views/Cart.vue'),
      meta: { requiresAuth: true, roles: ['CUSTOMER'] }
    },
    {
      path: '/checkout',
      name: 'Checkout',
      component: () => import('@/views/Checkout.vue'),
      meta: { requiresAuth: true, roles: ['CUSTOMER'] }
    },
    {
      path: '/admin/orders',
      name: 'AdminOrders',
      component: () => import('@/views/admin/AdminOrders.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/admin/products',
      name: 'ProductManagement',
      component: () => import('@/views/admin/ProductManagement.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/admin/products/new',
      name: 'ProductNew',
      component: () => import('@/views/admin/ProductNew.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/admin/products/:id/edit',
      name: 'ProductEdit',
      component: () => import('@/views/admin/ProductEdit.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/admin/shipping',
      name: 'ShippingManagement',
      component: () => import('@/views/admin/ShippingManagement.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] }
    },
    {
      path: '/',
      redirect: to => {
        const authStore = useAuthStore()
        if (!authStore.isAuthenticated) {
          return '/login'
        }
        return authStore.user?.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'
      }
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
})

// 路由守衛
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 檢查是否需要登入
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }

  // 檢查角色權限
  if (to.meta.roles && authStore.user) {
    const userRole = authStore.user.role
    const allowedRoles = to.meta.roles as string[]

    if (!allowedRoles.includes(userRole)) {
      // 根據角色重導向到適當的頁面
      const redirectPath = userRole === 'ADMIN' ? '/admin/dashboard' : '/dashboard'
      next(redirectPath)
      return
    }
  }

  // 已登入使用者訪問登入頁面時重導向
  if (to.path === '/login' && authStore.isAuthenticated) {
    const redirectPath = authStore.user?.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'
    next(redirectPath)
    return
  }

  next()
})

export default router