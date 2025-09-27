<template>
  <div class="global-message-container">
    <div
      v-if="successMessage"
      class="message success-message"
      data-cy="success-message"
    >
      {{ successMessage }}
    </div>
    <div
      v-if="errorMessage"
      class="message error-message"
      data-cy="error-message"
    >
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const successMessage = ref('')
const errorMessage = ref('')

let successTimer: NodeJS.Timeout | null = null
let errorTimer: NodeJS.Timeout | null = null

// Listen for custom events to show messages
const handleSuccessMessage = (event: CustomEvent) => {
  successMessage.value = event.detail.message
  if (successTimer) clearTimeout(successTimer)
  successTimer = setTimeout(() => {
    successMessage.value = ''
  }, 3000)
}

const handleErrorMessage = (event: CustomEvent) => {
  errorMessage.value = event.detail.message
  if (errorTimer) clearTimeout(errorTimer)
  errorTimer = setTimeout(() => {
    errorMessage.value = ''
  }, 3000)
}

onMounted(() => {
  window.addEventListener('show-success-message', handleSuccessMessage as EventListener)
  window.addEventListener('show-error-message', handleErrorMessage as EventListener)
})

onUnmounted(() => {
  window.removeEventListener('show-success-message', handleSuccessMessage as EventListener)
  window.removeEventListener('show-error-message', handleErrorMessage as EventListener)
  if (successTimer) clearTimeout(successTimer)
  if (errorTimer) clearTimeout(errorTimer)
})
</script>

<style scoped>
.global-message-container {
  position: fixed;
  top: 80px;
  right: 20px;
  z-index: 9999;
  pointer-events: none;
}

.message {
  padding: 12px 20px;
  border-radius: 4px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  animation: slideIn 0.3s ease-out;
  pointer-events: auto;
}

.success-message {
  background-color: #f0f9ff;
  border: 1px solid #67c23a;
  color: #67c23a;
}

.error-message {
  background-color: #fef2f2;
  border: 1px solid #f56565;
  color: #f56565;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>