<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="order-detail-container" v-if="order">
    <h1>訂單詳情</h1>

    <!-- 訂單標頭 -->
    <el-card class="order-header-card">
      <div class="header-content">
        <div class="order-id">訂單編號: {{ order.id }}</div>
        <el-tag :type="getStatusType(order.status)" size="large">
          {{ getStatusLabel(order.status) }}
        </el-tag>
      </div>
      <div class="order-meta">
        <div class="customer-info">客戶ID: {{ order.customerId }}</div>
        <div class="order-date">下單時間: {{ formatDateTime(order.createdAt) }}</div>
        <div class="order-total">訂單金額: ${{ order.totalAmount?.toFixed(0) }}</div>
      </div>
    </el-card>

    <!-- 訂單詳情 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <OrderItems :items="order.items" />
        <OrderEvents :events="events" />
      </el-col>

      <el-col :span="8">
        <OrderSummary :total-amount="order.totalAmount" />
        <PaymentInfo
          :payments="order.payments"
          :order-status="order.status"
          :show-confirm-button="true"
          @confirm-payment="confirmPayment"
        />
        <ShippingInfo :order-status="order.status" :updated-at="order.updatedAt" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ordersApi from '@/api/orders'
import type { Order, OrderEvent } from '@/types'
import OrderItems from '@/components/order/OrderItems.vue'
import OrderEvents from '@/components/order/OrderEvents.vue'
import OrderSummary from '@/components/order/OrderSummary.vue'
import PaymentInfo from '@/components/order/PaymentInfo.vue'
import ShippingInfo from '@/components/order/ShippingInfo.vue'

const route = useRoute()
const order = ref<Order | null>(null)
const events = ref<OrderEvent[]>([])

onMounted(async () => {
  await loadOrder()
})

async function loadOrder() {
  try {
    const orderId = Number(route.params.id)
    order.value = await ordersApi.getOrder(orderId)
    await loadOrderEvents(orderId)
  } catch (error) {
    console.error('Failed to load order:', error)
    ElMessage.error('載入訂單失敗')
  }
}

async function loadOrderEvents(orderId: number) {
  try {
    events.value = await ordersApi.getOrderEvents(orderId)
  } catch (error) {
    console.error('Failed to load order events:', error)
  }
}

async function confirmPayment(paymentId: number) {
  if (!order.value) return

  console.log('confirmPayment called with:', {
    orderId: order.value.id,
    paymentId: paymentId,
    payments: order.value.payments
  })

  try {
    await ElMessageBox.confirm(
      '確認已收到客戶的銀行轉帳款項？確認後訂單將標記為已付款。',
      '確認收款',
      {
        confirmButtonText: '確認',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    console.log('Calling API with orderId:', order.value.id, 'paymentId:', paymentId)
    await ordersApi.confirmBankTransfer(order.value.id!, paymentId)
    ElMessage.success('已確認收款')

    // 重新載入訂單以更新狀態
    await loadOrder()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('Failed to confirm payment:', error)
      console.error('Error details:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status
      })

      // 根據錯誤狀態顯示更詳細的訊息
      let errorMessage = '確認收款失敗'
      if (error.response?.status === 403) {
        errorMessage = '權限不足：需要管理員權限'
      } else if (error.response?.status === 404) {
        errorMessage = '付款記錄不存在'
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message
      }

      ElMessage.error(errorMessage)
    }
  }
}

function formatDateTime(date: string | undefined): string {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-TW')
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
.order-detail-container {
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 30px;
  color: #333;
}

.order-header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.order-id {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
}

.order-meta {
  display: flex;
  gap: 30px;
  color: #606266;
}

.customer-info {
  font-weight: 500;
}

.order-total {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}
</style>
