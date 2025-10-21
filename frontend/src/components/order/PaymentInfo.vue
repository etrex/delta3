<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <el-card class="payment-card" data-cy="payment-info">
    <template #header>
      <h3>付款資訊</h3>
    </template>

    <div class="payment-status" data-cy="payment-status">
      狀態: {{ getPaymentStatus(orderStatus) }}
    </div>

    <!-- 付款記錄 -->
    <div v-if="payments && payments.length > 0" class="payment-history" data-cy="payment-history">
      <h4>付款記錄</h4>
      <div
        v-for="payment in payments"
        :key="payment.id"
        class="payment-record"
        data-cy="payment-record"
      >
        <div data-cy="payment-method">付款方式: {{ getPaymentMethodLabel(payment.paymentMethod) }}</div>
        <div data-cy="payment-amount">金額: ${{ payment.amount?.toFixed(0) }}</div>
        <div data-cy="paid-date">時間: {{ formatDateTime(payment.paidAt) }}</div>
        <div v-if="payment.transactionId" data-cy="transaction-id">
          交易號: {{ payment.transactionId }}
        </div>
        <el-tag :type="getPaymentStatusType(payment.status)" data-cy="payment-status-tag">
          {{ getPaymentStatusLabel(payment.status) }}
        </el-tag>
      </div>

      <el-tag v-if="hasSuccessfulPayment" type="success" data-cy="payment-completed-badge">
        付款已完成
      </el-tag>
    </div>

    <!-- 未付款顯示付款按鈕 (僅客戶端) -->
    <div v-if="showPaymentButton && orderStatus === 'CREATED' && !hasSuccessfulPayment && !hasFailedPayment">
      <el-button
        type="primary"
        data-cy="pay-now-btn"
        @click="$emit('pay')"
        class="pay-btn"
      >立即付款</el-button>
    </div>

    <!-- 付款失敗顯示訊息 (一個訂單僅支援一次付清，失敗後不可重試) -->
    <div v-if="showPaymentButton && hasFailedPayment">
      <el-tag type="danger" data-cy="payment-failed-badge">付款失敗</el-tag>
      <div class="failed-notice">此訂單付款已失敗，無法重新付款</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Payment } from '@/types'

interface Props {
  payments?: Payment[]
  orderStatus: string
  showPaymentButton?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showPaymentButton: false
})

defineEmits<{
  pay: []
}>()

const hasSuccessfulPayment = computed(() => {
  return props.payments?.some(p => p.status === 'SUCCESS') || false
})

const hasFailedPayment = computed(() => {
  return props.payments?.some(p => p.status === 'FAILED') || false
})

function formatDateTime(date: string | undefined): string {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-TW')
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
.payment-card {
  margin-bottom: 20px;
}

.payment-status {
  margin-bottom: 16px;
  font-size: 16px;
}

.payment-history {
  margin-top: 20px;
}

.payment-history h4 {
  margin-bottom: 10px;
  color: #666;
}

.payment-record {
  padding: 12px;
  background-color: #fafafa;
  border-radius: 4px;
  margin-bottom: 12px;
}

.pay-btn {
  margin-top: 16px;
  width: 100%;
}

.failed-notice {
  margin-top: 12px;
  padding: 12px;
  background-color: #fef0f0;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 14px;
}
</style>
