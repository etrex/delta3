<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <el-card class="events-card" data-cy="order-events">
    <template #header>
      <h3>訂單歷程</h3>
    </template>
    <el-timeline v-if="events.length > 0">
      <el-timeline-item
        v-for="event in events"
        :key="event.id"
        :timestamp="formatDateTime(event.createdAt)"
        placement="top"
        data-cy="event-item"
      >
        <div class="event-type" data-cy="event-type">{{ getEventTypeLabel(event.eventType) }}</div>
        <div class="event-message" data-cy="event-message">{{ event.message }}</div>
        <div v-if="event.modifiedByUsername" class="event-user" data-cy="event-user">
          操作者: {{ event.modifiedByUsername }}
        </div>
      </el-timeline-item>
    </el-timeline>
    <div v-else class="no-events">尚無訂單歷程記錄</div>
  </el-card>
</template>

<script setup lang="ts">
import type { OrderEvent } from '@/types'

interface Props {
  events: OrderEvent[]
}

defineProps<Props>()

function formatDateTime(date: string | undefined): string {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-TW')
}

function getEventTypeLabel(eventType: string): string {
  const labels: Record<string, string> = {
    'CREATED': '訂單建立',
    'PAID': '付款完成',
    'PAYMENT_FAILED': '付款失敗',
    'APPROVED': '訂單批准',
    'SHIPPED': '訂單出貨',
    'DELIVERED': '訂單送達',
    'CANCELLED': '訂單取消',
    'REFUNDED': '訂單退款'
  }
  return labels[eventType] || eventType
}
</script>

<style scoped>
.events-card {
  margin-bottom: 20px;
}

.event-type {
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 4px;
}

.event-message {
  color: #666;
  font-size: 14px;
}

.event-user {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
  font-style: italic;
}

.no-events {
  text-align: center;
  color: #999;
  padding: 20px;
}
</style>
