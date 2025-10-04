<template>
  <div class="product-new-container">
    <h2>新增商品</h2>
    <ProductForm />
  </div>
</template>

<script setup lang="ts">
import ProductForm from '@/components/ProductForm.vue'
</script>

<style scoped>
.product-new-container {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
}
</style>
