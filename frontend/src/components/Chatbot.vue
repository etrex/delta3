<!--
 Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="chatbot-container">
    <!-- Chatbot trigger button -->
    <el-button
      v-if="!isOpen"
      class="chatbot-trigger"
      type="primary"
      size="large"
      circle
      @click="toggleChat"
    >
      <el-icon><ChatRound /></el-icon>
    </el-button>

    <!-- Chatbot window -->
    <el-card v-if="isOpen" class="chatbot-window" shadow="always">
      <template #header>
        <div class="chatbot-header">
          <span>智能客服</span>
          <el-button @click="toggleChat" size="small" circle>
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </template>

      <!-- Messages area -->
      <div class="messages-container" ref="messagesContainer">
        <div
          v-for="(message, index) in messages"
          :key="index"
          :class="['message', message.type]"
        >
          <div class="message-content">
            {{ message.content }}
          </div>
          <div class="message-time">
            {{ formatTime(message.timestamp) }}
          </div>
        </div>

        <div v-if="isLoading" class="message bot">
          <div class="message-content">
            <el-icon class="loading-icon"><Loading /></el-icon>
            正在思考中...
          </div>
        </div>
      </div>

      <!-- Input area -->
      <template #footer>
        <div class="input-area">
          <el-input
            v-model="currentMessage"
            placeholder="請輸入您的問題..."
            @keyup.enter="sendMessage"
            :disabled="isLoading"
          >
            <template #append>
              <el-button @click="sendMessage" :loading="isLoading" type="primary">
                發送
              </el-button>
            </template>
          </el-input>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ChatRound, Close, Loading } from '@element-plus/icons-vue'
import chatApi from '@/api/chat'

interface Message {
  content: string
  type: 'user' | 'bot'
  timestamp: number
}

const isOpen = ref(false)
const currentMessage = ref('')
const messages = ref<Message[]>([])
const isLoading = ref(false)
const messagesContainer = ref<HTMLElement>()

const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value && messages.value.length === 0) {
    // Welcome message
    messages.value.push({
      content: '您好！我是智能客服助手，可以幫您查詢商品、訂單狀態等。請問有什麼需要協助的嗎？',
      type: 'bot',
      timestamp: Date.now()
    })
  }
}

const sendMessage = async () => {
  if (!currentMessage.value.trim() || isLoading.value) return

  // Add user message
  messages.value.push({
    content: currentMessage.value,
    type: 'user',
    timestamp: Date.now()
  })

  const userMessage = currentMessage.value
  currentMessage.value = ''
  isLoading.value = true

  try {
    const response = await chatApi.sendMessage(userMessage)

    // Add bot response
    messages.value.push({
      content: response.message,
      type: 'bot',
      timestamp: response.timestamp
    })
  } catch (error) {
    console.error('Chat error:', error)
    messages.value.push({
      content: '抱歉，AI 服務暫時無法使用，請稍後再試。',
      type: 'bot',
      timestamp: Date.now()
    })
  } finally {
    isLoading.value = false
    await nextTick()
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleTimeString('zh-TW', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  // Auto-scroll when new messages are added
  nextTick(() => {
    scrollToBottom()
  })
})
</script>

<style scoped>
.chatbot-container {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
}

.chatbot-trigger {
  width: 60px;
  height: 60px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.chatbot-window {
  width: 400px;
  height: 500px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.chatbot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.messages-container {
  height: 350px;
  overflow-y: auto;
  padding: 10px;
  background-color: #f8f9fa;
}

.message {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
}

.message.user {
  align-items: flex-end;
}

.message.bot {
  align-items: flex-start;
}

.message-content {
  max-width: 80%;
  padding: 10px 15px;
  border-radius: 18px;
  word-wrap: break-word;
  line-height: 1.4;
}

.message.user .message-content {
  background-color: #409eff;
  color: white;
}

.message.bot .message-content {
  background-color: white;
  color: #333;
  border: 1px solid #e4e7ed;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
  margin-left: 10px;
  margin-right: 10px;
}

.input-area {
  margin-top: 10px;
}

.loading-icon {
  animation: spin 1s linear infinite;
  margin-right: 5px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>