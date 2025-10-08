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
        <div class="order-total">訂單金額: ${{ order.totalAmount?.toFixed(2) }}</div>
      </div>
    </el-card>

    <!-- 訂單詳情 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <!-- 商品明細 -->
        <el-card class="items-card">
          <template #header>
            <h3>商品明細</h3>
          </template>
          <div class="order-items">
            <div v-for="item in order.items" :key="item.id" class="order-item">
              <div class="item-image">
                <div class="image-placeholder"></div>
              </div>
              <div class="item-details">
                <div class="product-name">{{ item.productName }}</div>
                <div class="item-meta">
                  <span class="item-price">${{ item.price?.toFixed(2) }}</span>
                  <span class="item-quantity">x {{ item.quantity }}</span>
                  <span class="item-subtotal">
                    ${{ ((item.price || 0) * item.quantity).toFixed(2) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 出貨資訊 -->
        <el-card class="shipping-card">
          <template #header>
            <h3>出貨資訊</h3>
          </template>
          <div class="shipping-status">
            狀態: {{ getShippingStatus(order.status) }}
          </div>
          <div v-if="order.status === 'SHIPPED'" class="tracking-info">
            <div>出貨日期: {{ formatDateTime(order.updatedAt) }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <!-- 訂單摘要 -->
        <el-card class="summary-card">
          <template #header>
            <h3>訂單摘要</h3>
          </template>
          <div class="summary-row">
            <span>商品總計</span>
            <span>${{ order.totalAmount?.toFixed(2) }}</span>
          </div>
          <div class="summary-row">
            <span>運費</span>
            <span>$0.00</span>
          </div>
          <div class="summary-row total">
            <span>訂單總額</span>
            <span>${{ order.totalAmount?.toFixed(2) }}</span>
          </div>
        </el-card>

        <!-- 付款資訊 -->
        <el-card class="payment-card">
          <template #header>
            <h3>付款資訊</h3>
          </template>

          <div class="payment-status">
            狀態: {{ getPaymentStatus(order.status) }}
          </div>

          <!-- 付款記錄 -->
          <div v-if="order.payments && order.payments.length > 0" class="payment-history">
            <h4>付款記錄</h4>
            <div v-for="payment in order.payments" :key="payment.id" class="payment-record">
              <div>付款方式: {{ getPaymentMethodLabel(payment.paymentMethod) }}</div>
              <div>金額: ${{ payment.amount?.toFixed(2) }}</div>
              <div>時間: {{ formatDateTime(payment.paidAt) }}</div>
              <div v-if="payment.transactionId">
                交易號: {{ payment.transactionId }}
              </div>
              <el-tag :type="getPaymentStatusType(payment.status)">
                {{ getPaymentStatusLabel(payment.status) }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import ordersApi from '@/api/orders'
import type { Order } from '@/types'

const route = useRoute()
const order = ref<Order | null>(null)

onMounted(async () => {
  await loadOrder()
})

async function loadOrder() {
  try {
    const orderId = Number(route.params.id)
    order.value = await ordersApi.getOrder(orderId)
  } catch (error) {
    console.error('Failed to load order:', error)
    ElMessage.error('載入訂單失敗')
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

function getShippingStatus(status: string): string {
  if (status === 'SHIPPED') return '已出貨'
  if (status === 'APPROVED') return '待出貨'
  return '待出貨'
}

function getPaymentStatus(status: string): string {
  if (status === 'PAID' || status === 'APPROVED' || status === 'SHIPPED') return '已付款'
  return '待付款'
}

function getPaymentMethodLabel(method: string): string {
  const labels: Record<string, string> = {
    'CREDIT_CARD': '信用卡',
    'BANK_TRANSFER': '銀行轉帳',
    'PAYPAL': 'PayPal',
    'CASH': '貨到付款'
  }
  return labels[method] || method
}

function getPaymentStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    'PENDING': '處理中',
    'SUCCESS': '成功',
    'FAILED': '失敗',
    'REFUNDED': '已退款'
  }
  return labels[status] || status
}

function getPaymentStatusType(status: string): string {
  const types: Record<string, string> = {
    'PENDING': 'warning',
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'REFUNDED': 'info'
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

.items-card,
.shipping-card,
.summary-card,
.payment-card {
  margin-bottom: 20px;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background-color: #fafafa;
  border-radius: 4px;
}

.item-image {
  width: 80px;
  height: 80px;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  background-color: #e4e7ed;
  border-radius: 4px;
}

.item-details {
  flex: 1;
}

.product-name {
  font-weight: bold;
  margin-bottom: 8px;
}

.item-meta {
  display: flex;
  gap: 20px;
  color: #909399;
}

.item-subtotal {
  color: #409EFF;
  font-weight: bold;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.summary-row.total {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
  border-bottom: none;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 2px solid #409EFF;
}

.payment-status {
  margin-bottom: 16px;
  font-size: 16px;
}

.payment-history {
  margin-top: 20px;
}

.payment-record {
  padding: 12px;
  background-color: #fafafa;
  border-radius: 4px;
  margin-bottom: 12px;
}
</style>
