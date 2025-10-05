import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { ordersApi, type Order, type CreateOrderRequest, type PaymentRequest } from '@/api/orders'
import { useAuthStore } from './auth'
import { useCartStore } from './cart'

export const useOrdersStore = defineStore('orders', () => {
  const orders = ref<Order[]>([])
  const currentOrder = ref<Order | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Pagination
  const pagination = ref({
    page: 1,
    size: 10,
    total: 0,
    totalPages: 0
  })

  // Filters
  const filters = ref({
    status: '',
    search: '',
    dateFrom: '',
    dateTo: ''
  })

  // Computed
  const userOrders = computed(() => {
    const authStore = useAuthStore()
    if (authStore.user?.role === 'ADMIN') {
      return orders.value
    }
    return orders.value.filter(order => order.customerId === authStore.user?.id)
  })

  // Actions
  const loadOrders = async (params?: {
    page?: number
    size?: number
    status?: string
    search?: string
    dateFrom?: string
    dateTo?: string
  }) => {
    loading.value = true
    error.value = null
    try {
      const response = await ordersApi.getOrders({
        page: params?.page || pagination.value.page,
        size: params?.size || pagination.value.size,
        ...filters.value,
        ...params
      })

      orders.value = response.orders
      pagination.value = {
        page: response.page,
        size: pagination.value.size,
        total: response.total,
        totalPages: response.totalPages
      }
    } catch (err: any) {
      error.value = err.response?.data?.message || '載入訂單失敗'
      throw err
    } finally {
      loading.value = false
    }
  }

  const loadOrder = async (orderNo: string) => {
    loading.value = true
    error.value = null
    try {
      currentOrder.value = await ordersApi.getOrder(orderNo)
      return currentOrder.value
    } catch (err: any) {
      error.value = err.response?.data?.message || '載入訂單失敗'
      throw err
    } finally {
      loading.value = false
    }
  }

  const createOrder = async () => {
    const authStore = useAuthStore()
    const cartStore = useCartStore()

    if (!authStore.user) {
      throw new Error('請先登入')
    }

    if (cartStore.items.length === 0) {
      throw new Error('購物車不能為空')
    }

    loading.value = true
    error.value = null
    try {
      const orderData: CreateOrderRequest = {
        customerId: authStore.user.id,
        items: cartStore.items.map(item => ({
          productId: item.product.id,
          quantity: item.quantity
        }))
      }

      const order = await ordersApi.createOrder(orderData)

      // 清空購物車
      cartStore.clearCart()

      // 設為當前訂單
      currentOrder.value = order

      return order
    } catch (err: any) {
      error.value = err.response?.data?.message || '建立訂單失敗'
      throw err
    } finally {
      loading.value = false
    }
  }

  const payOrder = async (orderNo: string, paymentData: PaymentRequest) => {
    loading.value = true
    error.value = null
    try {
      const response = await ordersApi.pay(orderNo, paymentData)

      // 更新當前訂單
      if (currentOrder.value && currentOrder.value.orderNo === orderNo) {
        currentOrder.value = response.order
      }

      // 更新列表中的訂單
      const index = orders.value.findIndex(o => o.orderNo === orderNo)
      if (index !== -1) {
        orders.value[index] = response.order
      }

      return response
    } catch (err: any) {
      error.value = err.response?.data?.message || '付款失敗'
      throw err
    } finally {
      loading.value = false
    }
  }

  const cancelOrder = async (orderNo: string) => {
    loading.value = true
    error.value = null
    try {
      const order = await ordersApi.cancelOrder(orderNo)

      // 更新當前訂單
      if (currentOrder.value && currentOrder.value.orderNo === orderNo) {
        currentOrder.value = order
      }

      // 更新列表中的訂單
      const index = orders.value.findIndex(o => o.orderNo === orderNo)
      if (index !== -1) {
        orders.value[index] = order
      }

      return order
    } catch (err: any) {
      error.value = err.response?.data?.message || '取消訂單失敗'
      throw err
    } finally {
      loading.value = false
    }
  }

  const approveOrder = async (orderNo: string) => {
    loading.value = true
    error.value = null
    try {
      const order = await ordersApi.approveOrder(orderNo)

      // 更新當前訂單
      if (currentOrder.value && currentOrder.value.orderNo === orderNo) {
        currentOrder.value = order
      }

      // 更新列表中的訂單
      const index = orders.value.findIndex(o => o.orderNo === orderNo)
      if (index !== -1) {
        orders.value[index] = order
      }

      return order
    } catch (err: any) {
      error.value = err.response?.data?.message || '批准訂單失敗'
      throw err
    } finally {
      loading.value = false
    }
  }

  const shipOrder = async (orderNo: string, shippingData: {
    trackingNumber: string
    carrier: string
    estimatedDelivery?: string
    notes?: string
  }) => {
    loading.value = true
    error.value = null
    try {
      const order = await ordersApi.shipOrder(orderNo, shippingData)

      // 更新當前訂單
      if (currentOrder.value && currentOrder.value.orderNo === orderNo) {
        currentOrder.value = order
      }

      // 更新列表中的訂單
      const index = orders.value.findIndex(o => o.orderNo === orderNo)
      if (index !== -1) {
        orders.value[index] = order
      }

      return order
    } catch (err: any) {
      error.value = err.response?.data?.message || '標記出貨失敗'
      throw err
    } finally {
      loading.value = false
    }
  }

  const deliverOrder = async (orderNo: string, deliveryData: {
    deliveredDate: string
    notes?: string
  }) => {
    loading.value = true
    error.value = null
    try {
      const order = await ordersApi.deliverOrder(orderNo, deliveryData)

      // 更新當前訂單
      if (currentOrder.value && currentOrder.value.orderNo === orderNo) {
        currentOrder.value = order
      }

      // 更新列表中的訂單
      const index = orders.value.findIndex(o => o.orderNo === orderNo)
      if (index !== -1) {
        orders.value[index] = order
      }

      return order
    } catch (err: any) {
      error.value = err.response?.data?.message || '標記送達失敗'
      throw err
    } finally {
      loading.value = false
    }
  }

  const setFilters = (newFilters: Partial<typeof filters.value>) => {
    filters.value = { ...filters.value, ...newFilters }
  }

  const setPagination = (page: number, size?: number) => {
    pagination.value.page = page
    if (size) pagination.value.size = size
  }

  const clearCurrentOrder = () => {
    currentOrder.value = null
  }

  return {
    orders,
    currentOrder,
    loading,
    error,
    pagination,
    filters,
    userOrders,
    loadOrders,
    loadOrder,
    createOrder,
    payOrder,
    cancelOrder,
    approveOrder,
    shipOrder,
    deliverOrder,
    setFilters,
    setPagination,
    clearCurrentOrder
  }
})
