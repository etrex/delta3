<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <el-card class="shipping-card" data-cy="shipping-info">
    <template #header>
      <h3>出貨資訊</h3>
    </template>
    <div class="shipping-status" data-cy="shipping-status">
      狀態: {{ getShippingStatus(orderStatus) }}
    </div>
    <div v-if="orderStatus === 'SHIPPED'" class="tracking-info" data-cy="tracking-info">
      <div data-cy="shipped-date">出貨日期: {{ formatDateTime(updatedAt) }}</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
interface Props {
  orderStatus: string
  updatedAt?: string
}

defineProps<Props>()

function formatDateTime(date: string | undefined): string {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-TW')
}

function getShippingStatus(status: string): string {
  if (status === 'SHIPPED') return '已出貨'
  if (status === 'APPROVED') return '待出貨'
  return '待出貨'
}
</script>

<style scoped>
.shipping-card {
  margin-bottom: 20px;
}

.shipping-status {
  margin-bottom: 16px;
  font-size: 16px;
}

.tracking-info {
  padding: 12px;
  background-color: #f0f9ff;
  border-left: 3px solid #409EFF;
  border-radius: 4px;
}
</style>
