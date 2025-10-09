<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <el-card class="items-card" data-cy="order-details">
    <template #header>
      <h3>商品明細</h3>
    </template>
    <div class="order-items" data-cy="order-items">
      <div
        v-for="item in items"
        :key="item.id"
        class="order-item"
        data-cy="order-item"
      >
        <div class="item-image" data-cy="product-image">
          <div class="image-placeholder"></div>
        </div>
        <div class="item-details">
          <div class="product-name" data-cy="product-name">{{ item.productName }}</div>
          <div class="item-meta">
            <span class="item-price" data-cy="item-price">${{ item.price?.toFixed(0) }}</span>
            <span class="item-quantity" data-cy="item-quantity">x {{ item.quantity }}</span>
            <span class="item-subtotal" data-cy="item-subtotal">
              ${{ ((item.price || 0) * item.quantity).toFixed(0) }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { OrderItem } from '@/types'

interface Props {
  items?: OrderItem[]
}

defineProps<Props>()
</script>

<style scoped>
.items-card {
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
</style>
