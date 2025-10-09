<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <el-card class="summary-card" data-cy="order-summary">
    <template #header>
      <h3>訂單摘要</h3>
    </template>
    <div class="summary-row">
      <span>商品總計</span>
      <span data-cy="subtotal">${{ totalAmount?.toFixed(0) }}</span>
    </div>
    <div class="summary-row">
      <span>運費</span>
      <span data-cy="shipping-fee">$0</span>
    </div>
    <div class="summary-row total">
      <span>訂單總額</span>
      <span data-cy="total-amount">${{ totalAmount?.toFixed(0) }}</span>
    </div>
  </el-card>
</template>

<script setup lang="ts">
interface Props {
  totalAmount?: number
}

defineProps<Props>()
</script>

<style scoped>
.summary-card {
  margin-bottom: 20px;
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
</style>
