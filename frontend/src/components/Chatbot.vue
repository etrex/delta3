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
          v-for="(message, index) in filteredMessages"
          :key="index"
          :class="['message', message.type]"
        >
          <div v-if="message.type === 'action'" class="action-message">
            <el-icon><Operation /></el-icon>
            <span>{{ message.content }}</span>
          </div>
          <div v-else-if="message.type === 'navigation'" class="navigation-message">
            <el-icon class="navigation-icon"><Position /></el-icon>
            <div class="navigation-content">
              <div class="navigation-text">{{ message.content }}</div>
              <div class="navigation-path">正在跳轉至：{{ message.navigationPath }}</div>
            </div>
          </div>
          <div v-else-if="message.type === 'tool_execution'" class="tool-execution-message">
            <el-icon class="tool-icon spinning"><Tools /></el-icon>
            <span>{{ message.content }}</span>
          </div>
          <div v-else-if="message.type === 'tool_result'" class="tool-result-message">
            <el-icon class="tool-icon"><CircleCheck /></el-icon>
            <span>{{ message.content }}</span>
          </div>
          <div v-else>
            <div class="message-content" :class="{ 'markdown-content': message.type === 'bot' }">
              <div v-if="message.type === 'bot'" v-html="renderMarkdown(message.content)"></div>
              <div v-else>{{ message.content }}</div>
            </div>
            <div class="message-time">
              {{ formatTime(message.timestamp) }}
            </div>
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
import { ref, nextTick, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ChatRound, Close, Operation, Position, Tools, CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import chatApi from '@/api/chat'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useChatTracking } from '@/composables/useChatTracking'
import { useChatWebSocket, type ChatMessage as WSMessage } from '@/composables/useChatWebSocket'

// Configure marked to open links in new tab and add security
marked.setOptions({
  breaks: true,
  gfm: true
})

interface Message {
  content: string
  type: 'user' | 'bot' | 'action' | 'navigation' | 'tool_execution' | 'tool_result'
  timestamp: number
  navigationPath?: string
}

const authStore = useAuthStore()
const chatStore = useChatStore()
const router = useRouter()
const { subscribeToUserMessages } = useChatWebSocket()
const isOpen = ref(false)
const currentMessage = ref('')
const messages = ref<Message[]>([])
const isLoading = ref(false)
const messagesContainer = ref<HTMLElement>()

// Use session ID from chat store (user ID)
const sessionId = computed(() => String(authStore.user?.id || 'guest'))

// Enable chat tracking (backend auto-tracks all operations)
useChatTracking()

/**
 * Filter messages to hide tool_execution messages when corresponding tool_result exists
 * For example, if there's "✅ 完成：搜尋商品", hide "🔧 正在執行：搜尋商品"
 */
const filteredMessages = computed(() => {
  // First pass: collect all tool names that have completion messages
  const completedTools = new Set<string>()

  for (const msg of messages.value) {
    if (msg.type === 'tool_result') {
      // Extract tool name from "✅ 完成：搜尋商品"
      const toolName = msg.content.replace('✅ 完成：', '').trim()
      completedTools.add(toolName)
    }
  }

  // Second pass: filter out execution messages that have corresponding completions
  return messages.value.filter(msg => {
    if (msg.type === 'tool_execution') {
      // Extract tool name from "🔧 正在執行：搜尋商品"
      const toolName = msg.content.replace('🔧 正在執行：', '').trim()
      // Only show if no completion exists
      return !completedTools.has(toolName)
    }
    // Show all other messages
    return true
  })
})

const toggleChat = async () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    // Always reload history when opening chat
    messages.value = []
    await loadChatHistory()

    // If no history, show welcome message
    if (messages.value.length === 0) {
      messages.value.push({
        content: '您好！我是智能客服助手，可以幫您查詢商品、訂單狀態等。請問有什麼需要協助的嗎？',
        type: 'bot',
        timestamp: Date.now()
      })
    }
  }
}

const loadChatHistory = async () => {
  try {
    const history = await chatApi.getHistory(sessionId.value)

    // Convert backend history to messages
    for (const item of history) {
      if (item.messageType === 'ACTION') {
        messages.value.push({
          content: item.content.replace(/^\(|\)$/g, ''), // Remove parentheses
          type: 'action',
          timestamp: new Date(item.createdAt).getTime()
        })
      } else if (item.role === 'USER') {
        messages.value.push({
          content: item.content,
          type: 'user',
          timestamp: new Date(item.createdAt).getTime()
        })
      } else if (item.role === 'TOOL') {
        // Tool execution result - extract tool name from metadata
        let toolDisplayName = '未知工具'
        if (item.metadata) {
          try {
            const metadata = JSON.parse(item.metadata)
            toolDisplayName = formatToolNameFromMetadata(metadata.toolName)
          } catch (e) {
            console.error('Failed to parse tool metadata:', e)
          }
        }
        messages.value.push({
          content: '✅ 完成：' + toolDisplayName,
          type: 'tool_result',
          timestamp: new Date(item.createdAt).getTime()
        })
      } else if (item.role === 'ASSISTANT') {
        // Check if this is a tool execution request
        if (item.content.startsWith('🔧 正在執行：')) {
          messages.value.push({
            content: item.content,
            type: 'tool_execution',
            timestamp: new Date(item.createdAt).getTime()
          })
        } else {
          // Parse and remove navigation commands from history
          const { cleanContent } = parseNavigationCommand(item.content)
          if (cleanContent) {
            messages.value.push({
              content: cleanContent,
              type: 'bot',
              timestamp: new Date(item.createdAt).getTime()
            })
          }
        }
      }
    }

    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('Failed to load chat history:', error)
  }
}

/**
 * Parse navigation commands from AI response
 * Format: [NAVIGATE:/path/to/page]
 * Example: [NAVIGATE:/products] or [NAVIGATE:/orders/123]
 */
const parseNavigationCommand = (content: string): { path: string | null; cleanContent: string } => {
  const navRegex = /\[NAVIGATE:([^\]]+)\]/g
  let path: string | null = null
  const cleanContent = content.replace(navRegex, (match, capturedPath) => {
    path = capturedPath
    return '' // Remove the command from display
  }).trim()

  return { path, cleanContent }
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
    // Build page context
    const pageContext = {
      path: router.currentRoute.value.path,
      title: router.currentRoute.value.meta?.title as string || router.currentRoute.value.name as string || undefined,
      pageType: router.currentRoute.value.meta?.pageType as string || undefined
    }

    // Use different API based on user role
    const response = authStore.user?.role === 'ADMIN'
      ? await chatApi.sendAdminMessage(userMessage, pageContext)
      : await chatApi.sendMessage(userMessage, pageContext)

    console.log('Chat response:', response)

    const botResponse = response.response || response.message || ''

    // If response is empty, it means the message will be delivered via WebSocket
    // (e.g., AI auto-reply with high confidence). Skip adding to avoid duplication.
    if (!botResponse || botResponse.trim() === '') {
      console.log('Empty response - message will be delivered via WebSocket')
      return
    }

    // Parse navigation command if present
    const { path, cleanContent } = parseNavigationCommand(botResponse)

    // If navigation command found, show special navigation message
    if (path) {
      messages.value.push({
        content: cleanContent || '正在為您導航',
        type: 'navigation',
        timestamp: Date.now(),
        navigationPath: path
      })

      await nextTick()
      scrollToBottom()

      // Navigate after a short delay (keep chat open)
      setTimeout(() => {
        router.push(path)
      }, 1000)
    } else if (cleanContent) {
      // Add normal bot response if no navigation
      messages.value.push({
        content: cleanContent,
        type: 'bot',
        timestamp: Date.now()
      })
    }
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

/**
 * Render markdown content to HTML
 * Links will open in new tab and be styled
 */
const renderMarkdown = (content: string): string => {
  const html = marked.parse(content) as string

  // Add target="_blank" and rel="noopener noreferrer" to all links for security
  return html.replace(
    /<a href=/g,
    '<a target="_blank" rel="noopener noreferrer" href='
  )
}

const formatToolNameFromMetadata = (toolName: string): string => {
  const toolNameMap: Record<string, string> = {
    'searchProducts': '搜尋商品',
    'getProductDetails': '查詢商品詳情',
    'getMyOrders': '查詢訂單列表',
    'getOrderDetails': '查詢訂單詳情',
    'checkStock': '檢查庫存',
    'addToCart': '加入購物車',
    'checkoutCart': '結帳',
    'cancelOrder': '取消訂單',
    'searchFAQ': '搜尋常見問題'
  }
  return toolNameMap[toolName] || toolName
}

onMounted(async () => {
  // Auto-scroll when new messages are added
  nextTick(() => {
    scrollToBottom()
  })

  // Connect to WebSocket and subscribe to user messages
  if (authStore.user?.id) {
    try {
      await subscribeToUserMessages(authStore.user.id, (message: any) => {
        console.log('Received WebSocket message:', message)

        // Determine message type based on messageType from backend
        // Note: Backend sends 'messageType' field, not 'type'
        let messageType: 'user' | 'bot' | 'action' | 'navigation' | 'tool_execution' | 'tool_result' = 'bot'
        const backendMessageType = message.messageType || message.type

        if (backendMessageType === 'user_action') {
          messageType = 'action'
        } else if (backendMessageType === 'navigation') {
          messageType = 'navigation'
        } else if (backendMessageType === 'tool_execution') {
          messageType = 'tool_execution'
        } else if (backendMessageType === 'tool_result') {
          messageType = 'tool_result'
        }

        // Parse navigation command from bot messages
        let messageContent = message.content
        let navigationPath: string | undefined

        if (messageType === 'bot') {
          const { path, cleanContent } = parseNavigationCommand(message.content)
          if (path) {
            messageType = 'navigation'
            messageContent = cleanContent || '正在為您導航'
            navigationPath = path

            // Navigate after a short delay
            setTimeout(() => {
              router.push(path)
            }, 1000)
          }
        }

        // Add message to chat
        messages.value.push({
          content: messageContent,
          type: messageType,
          timestamp: Date.now(),
          navigationPath
        })

        // If it's a tool result for addToCart, refresh the cart
        if (messageType === 'tool_result' && messageContent.includes('加入購物車')) {
          console.log('Tool result for addToCart detected, refreshing cart...')
          // Refresh cart after a short delay to ensure backend has processed
          setTimeout(async () => {
            try {
              await cartStore.loadCart({ tracking: false })
              console.log('Cart refreshed successfully')
            } catch (error) {
              console.error('Failed to refresh cart:', error)
            }
          }, 500)
        }

        nextTick(() => {
          scrollToBottom()
        })

        // Show notification if chat is closed (skip for action messages)
        if (!isOpen.value && messageType !== 'action') {
          ElMessage({
            message: '您收到新的客服訊息',
            type: 'info',
            duration: 3000
          })
        }
      })
    } catch (error) {
      console.error('Failed to subscribe to user messages:', error)
    }
  }
})
</script>

<style scoped>
.chatbot-container {
  position: fixed;
  bottom: 80px;
  right: 20px;
  z-index: 9999;
  max-height: calc(100vh - 100px);
}

.chatbot-trigger {
  width: 60px;
  height: 60px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.chatbot-window {
  width: 400px;
  max-height: calc(100vh - 160px);
  height: auto;
  min-height: 400px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

/* Mobile responsive styles */
@media (max-width: 768px) {
  .chatbot-container {
    bottom: 20px;
    right: 10px;
    left: 10px;
    max-height: calc(100vh - 40px);
  }

  .chatbot-trigger {
    width: 56px;
    height: 56px;
    position: fixed;
    right: 16px;
    bottom: 16px;
  }

  .chatbot-window {
    width: 100%;
    max-width: 100%;
    max-height: calc(100vh - 40px);
    height: calc(100vh - 40px);
    min-height: unset;
    margin: 0;
    border-radius: 12px;
  }

  .messages-container {
    height: calc(100vh - 220px) !important;
  }
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

/* Markdown content styling */
.markdown-content :deep(a) {
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
  border-bottom: 1px solid #409eff;
  transition: all 0.3s;
}

.markdown-content :deep(a:hover) {
  color: #66b1ff;
  border-bottom-color: #66b1ff;
  background-color: rgba(64, 158, 255, 0.05);
}

.markdown-content :deep(p) {
  margin: 0.5em 0;
  line-height: 1.6;
}

.markdown-content :deep(p:first-child) {
  margin-top: 0;
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(code) {
  background-color: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #e6a23c;
}

.markdown-content :deep(pre) {
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 0.5em 0;
}

.markdown-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
  color: inherit;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.markdown-content :deep(li) {
  margin: 0.3em 0;
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

/* Action message styling */
.message.action {
  align-items: center;
  margin-bottom: 8px;
}

.action-message {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background-color: #f0f2f5;
  border-left: 3px solid #909399;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  font-style: italic;
  opacity: 0.8;
  max-width: 90%;
}

.action-message .el-icon {
  font-size: 14px;
  color: #909399;
}

/* Navigation message styling */
.message.navigation {
  align-items: center;
  margin-bottom: 15px;
}

.navigation-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  max-width: 90%;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  animation: slideInScale 0.3s ease-out;
}

.navigation-icon {
  font-size: 24px;
  color: white;
  animation: pulse 1.5s ease-in-out infinite;
}

.navigation-content {
  flex: 1;
  color: white;
}

.navigation-text {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 6px;
  line-height: 1.4;
}

.navigation-path {
  font-size: 12px;
  opacity: 0.9;
  padding: 4px 8px;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  display: inline-block;
  font-family: monospace;
}

@keyframes slideInScale {
  from {
    transform: translateY(10px) scale(0.95);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

/* Tool execution message styling */
.message.tool_execution,
.message.tool_result {
  align-items: center;
  margin-bottom: 10px;
}

.tool-execution-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: linear-gradient(135deg, #ffeaa7 0%, #fdcb6e 100%);
  border-radius: 8px;
  font-size: 13px;
  color: #2d3436;
  max-width: 90%;
  box-shadow: 0 2px 8px rgba(253, 203, 110, 0.3);
}

.tool-result-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: linear-gradient(135deg, #55efc4 0%, #00b894 100%);
  border-radius: 8px;
  font-size: 13px;
  color: #2d3436;
  max-width: 90%;
  box-shadow: 0 2px 8px rgba(0, 184, 148, 0.3);
}

.tool-icon {
  font-size: 18px;
  color: #2d3436;
}

.tool-icon.spinning {
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>