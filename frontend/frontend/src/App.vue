<script setup lang="ts">
import GlobalMessage from '@/components/GlobalMessage.vue'
</script>

<template>
  <div id="app">
    <router-view />
    <GlobalMessage />
  </div>
</template>

<style>
#app {
  min-height: 100vh;
  background-color: #f5f5f5;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}
</style>
