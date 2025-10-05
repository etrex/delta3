import axios from 'axios'

export interface CreateOrderRequest {
  customerId: number
  items: Array<{
    productId: number
    quantity: number
  }>
}

export interface Order {
  id: number
  orderNo: string
  customerId: number
  customerName?: string
  totalAmount: number
  status: 'CREATED' | 'PAID' | 'APPROVED' | 'SHIPPED' | 'CANCELLED'
  createdAt: string
  updatedAt: string
  items: OrderItem[]
  payment?: Payment
  shipping?: Shipping
}

export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  productImage?: string
  quantity: number
  price: number
}

export interface Payment {
  id: number
  orderId: number
  paymentMethod: 'CREDIT_CARD' | 'BANK_TRANSFER' | 'PAYPAL' | 'CASH'
  amount: number
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED'
  transactionId?: string
  paidAt?: string
  createdAt: string
}

export interface Shipping {
  id: number
  orderId: number
  status: 'PENDING' | 'PROCESSING' | 'APPROVED' | 'SHIPPED' | 'DELIVERED'
  trackingNumber?: string
  carrier?: string
  estimatedDelivery?: string
  shippedAt?: string
  deliveredAt?: string
}

export interface PaymentRequest {
  paymentMethod: 'CREDIT_CARD' | 'BANK_TRANSFER' | 'PAYPAL' | 'CASH'
  cardNumber?: string
  cardExpiry?: string
  cardCvv?: string
  cardName?: string
}

// API Base URL
const API_BASE = '/api'

export const ordersApi = {
  // 獲取訂單列表
  async getOrders(params?: {
    page?: number
    size?: number
    status?: string
    search?: string
    dateFrom?: string
    dateTo?: string
  }): Promise<{ orders: Order[]; total: number; page: number; totalPages: number }> {
    const response = await axios.get(`${API_BASE}/orders`, { params })
    return response.data
  },

  // 獲取單個訂單
  async getOrder(orderNo: string): Promise<Order> {
    const response = await axios.get(`${API_BASE}/orders/${orderNo}`)
    return response.data
  },

  // 建立訂單
  async createOrder(data: CreateOrderRequest): Promise<Order> {
    const response = await axios.post(`${API_BASE}/orders`, data)
    return response.data
  },

  // 付款
  async pay(orderNo: string, paymentData: PaymentRequest): Promise<{ success: boolean; payment: Payment; order: Order }> {
    const response = await axios.post(`${API_BASE}/orders/${orderNo}/pay`, paymentData)
    return response.data
  },

  // 取消訂單
  async cancelOrder(orderNo: string): Promise<Order> {
    const response = await axios.post(`${API_BASE}/orders/${orderNo}/cancel`)
    return response.data
  },

  // Admin: 標記出貨
  async shipOrder(orderNo: string, shippingData: {
    trackingNumber: string
    carrier: string
    estimatedDelivery?: string
    notes?: string
  }): Promise<Order> {
    const response = await axios.post(`${API_BASE}/orders/${orderNo}/ship`, shippingData)
    return response.data
  },

  // Admin: 批准訂單(待出貨)
  async approveOrder(orderNo: string): Promise<Order> {
    const response = await axios.post(`${API_BASE}/orders/${orderNo}/approve`)
    return response.data
  },

  // Admin: 標記已送達
  async deliverOrder(orderNo: string, deliveryData: {
    deliveredDate: string
    notes?: string
  }): Promise<Order> {
    const response = await axios.post(`${API_BASE}/orders/${orderNo}/deliver`, deliveryData)
    return response.data
  }
}
