<!--
  Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="chat-management">
    <h1>AI 客服管理</h1>

    <el-row :gutter="20">
      <!-- Left: Sessions List -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <h3>對話列表</h3>
              <el-badge :value="pendingSuggestionsCount" :hidden="pendingSuggestionsCount === 0">
                <el-button size="small" @click="loadSessions">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </el-badge>
            </div>
          </template>

          <el-input
            v-model="searchQuery"
            placeholder="搜尋會話..."
            :prefix-icon="Search"
            clearable
            class="session-search"
          />

          <div class="sessions-list">
            <div
              v-for="session in filteredSessions"
              :key="session.sessionId"
              class="session-item"
              :class="{
                active: currentSession?.sessionId === session.sessionId,
                'has-suggestion': session.hasPendingSuggestion
              }"
              @click="selectSession(session)"
            >
              <div class="session-header">
                <strong>用戶 #{{ session.userId }}</strong>
                <el-tag v-if="session.hasPendingSuggestion" type="warning" size="small">
                  待處理
                </el-tag>
              </div>
              <div class="session-message">{{ session.lastMessage }}</div>
              <div class="session-time">{{ formatTime(session.lastMessageTime) }}</div>
            </div>

            <el-empty v-if="sessions.length === 0" description="暫無對話" />
          </div>
        </el-card>
      </el-col>

      <!-- Middle: Chat History -->
      <el-col :span="10">
        <el-card v-if="currentSession" class="chat-card">
          <template #header>
            <h3>對話歷史 - 用戶 #{{ currentSession.userId }}</h3>
          </template>

          <div class="chat-container">
            <div class="chat-history" ref="chatHistoryRef">
              <div
                v-for="msg in chatHistory"
                :key="msg.id"
                class="chat-message"
                :class="msg.role.toLowerCase()"
              >
                <div class="message-header">
                  <el-tag :type="msg.role === 'USER' ? 'info' : 'success'" size="small">
                    {{ msg.role === 'USER' ? '客戶' : 'AI 助手' }}
                  </el-tag>
                  <span class="message-time">{{ formatDateTime(msg.createdAt) }}</span>
                </div>
                <div class="message-content">{{ msg.content }}</div>
                <div v-if="msg.actionType" class="message-action">
                  <el-icon><Position /></el-icon>
                  {{ msg.actionType }}: {{ msg.actionTarget }}
                </div>
              </div>

              <!-- AI Generating Status -->
              <div v-if="aiGeneratingStatus" class="chat-message assistant ai-generating">
                <div class="message-header">
                  <el-tag type="warning" size="small">
                    <el-icon class="loading-icon"><Loading /></el-icon>
                    AI 生成中
                  </el-tag>
                </div>
                <div class="message-content generating-text">AI 正在生成回覆中...</div>
              </div>

              <el-empty v-if="chatHistory.length === 0" description="暫無對話記錄" />
            </div>

            <div class="chat-input-area">
              <el-input
                v-model="manualMessage"
                type="textarea"
                :rows="3"
                placeholder="輸入訊息直接發送給客戶..."
                @keydown.ctrl.enter="sendManualMessage"
              />
              <el-button
                type="primary"
                :disabled="!manualMessage.trim()"
                @click="sendManualMessage"
                class="send-button"
              >
                <el-icon><Promotion /></el-icon>
                發送 (Ctrl+Enter)
              </el-button>
            </div>
          </div>
        </el-card>

        <el-card v-else class="empty-state">
          <el-empty description="請選擇一個對話" />
        </el-card>
      </el-col>

      <!-- Right: Context & Suggestions -->
      <el-col :span="6">
        <el-card>
          <template #header>
            <el-tabs v-model="activeTab" class="context-tabs">
              <el-tab-pane label="AI 建議" name="suggestions"></el-tab-pane>
              <el-tab-pane label="用戶上下文" name="context"></el-tab-pane>
            </el-tabs>
          </template>

          <!-- AI Suggestions Tab -->
          <div v-show="activeTab === 'suggestions'" class="suggestions-list">
            <div
              v-for="suggestion in currentSuggestions"
              :key="suggestion.aiResponseId"
              class="suggestion-item"
            >
              <div class="suggestion-header">
                <el-tag type="warning" size="small">
                  置信度: {{ (suggestion.confidence * 100).toFixed(0) }}%
                </el-tag>
                <span class="suggestion-time">{{ formatDateTime(suggestion.createdAt) }}</span>
              </div>

              <div class="suggestion-text">{{ suggestion.suggestedText }}</div>

              <div v-if="suggestion.toolCalls && suggestion.toolCalls.length > 0" class="tool-calls">
                <el-divider>工具調用</el-divider>
                <div v-for="(tool, idx) in suggestion.toolCalls" :key="idx" class="tool-call">
                  <strong>{{ tool.toolName }}</strong>
                  <div class="tool-result">{{ tool.result }}</div>
                </div>
              </div>

              <div class="suggestion-actions">
                <el-button
                  type="success"
                  size="small"
                  @click="approveSuggestion(suggestion)"
                >
                  批准發送
                </el-button>
                <el-button
                  type="primary"
                  size="small"
                  @click="showModifyDialog(suggestion)"
                >
                  修改
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="showRejectDialog(suggestion)"
                >
                  拒絕
                </el-button>
              </div>
            </div>

            <el-empty v-if="currentSuggestions.length === 0" description="暫無 AI 建議" />
          </div>

          <!-- User Context Tab -->
          <div v-show="activeTab === 'context'" class="context-panel">
            <!-- General Page Context -->
            <div v-if="userContext && !userContext.productId && !userContext.orderId" class="general-context">
              <div class="context-header">
                <small class="context-badge">正在查看</small>
                <small class="last-update">{{ formatTime(userContext.lastUpdate) }}</small>
              </div>

              <div class="page-info-display">
                <el-icon class="page-icon"><Document /></el-icon>
                <h3>{{ userContext.currentPage || '未知頁面' }}</h3>
              </div>
            </div>

            <!-- Product Context -->
            <div v-else-if="userContext?.productId && productInfo" class="product-context">
              <div class="context-header">
                <small class="context-badge">正在查看</small>
                <small class="last-update">{{ formatTime(userContext.lastUpdate) }}</small>
              </div>

              <h2 class="product-name">{{ productInfo.name }}</h2>

              <div class="product-price-display">
                <span class="currency">NT$</span>
                <span class="price-value">{{ productInfo.price?.toFixed(0) || 0 }}</span>
              </div>

              <el-divider />

              <div class="product-stock-info">
                <span class="stock-label">庫存狀態：</span>
                <span :class="['stock-status', productInfo.stockQuantity > 0 ? 'in-stock' : 'out-of-stock']">
                  {{ productInfo.stockQuantity > 0 ? `有貨 (${productInfo.stockQuantity} 件)` : '缺貨' }}
                </span>
              </div>

              <div class="product-description">
                <h4>商品描述</h4>
                <p>{{ productInfo.description || '無描述' }}</p>
              </div>

              <el-divider />

              <div class="product-meta-info">
                <div class="meta-row">
                  <span class="meta-label">商品 ID：</span>
                  <span class="meta-value">{{ productInfo.id }}</span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">狀態：</span>
                  <el-tag size="small" :type="productInfo.status === 'ACTIVE' ? 'success' : 'info'">
                    {{ productInfo.status === 'ACTIVE' ? '上架中' : '未上架' }}
                  </el-tag>
                </div>
              </div>

              <a :href="`/products/${productInfo.id}`" target="_blank" class="full-detail-link">
                <el-button type="primary" size="small" plain>
                  開啟完整商品頁面 →
                </el-button>
              </a>
            </div>

            <!-- Order Context -->
            <div v-else-if="userContext?.orderId && orderInfo" class="order-context">
              <div class="context-header">
                <small class="context-badge">正在查看</small>
                <small class="last-update">{{ formatTime(userContext.lastUpdate) }}</small>
              </div>

              <h2 class="order-title">訂單 #{{ orderInfo.id }}</h2>

              <div class="order-status-display">
                <el-tag size="large" :type="getOrderStatusType(orderInfo.status)">
                  {{ getOrderStatusText(orderInfo.status) }}
                </el-tag>
              </div>

              <el-divider />

              <div class="order-amount">
                <span class="amount-label">訂單金額</span>
                <div class="amount-value">
                  <span class="currency">NT$</span>
                  <span class="price">{{ orderInfo.totalAmount?.toFixed(0) || 0 }}</span>
                </div>
              </div>

              <div class="order-items-summary">
                <h4>訂購商品</h4>
                <div v-if="orderInfo.orderItems && orderInfo.orderItems.length > 0">
                  <div v-for="item in orderInfo.orderItems" :key="item.id" class="order-item">
                    <span class="item-name">{{ item.productName }}</span>
                    <span class="item-qty">x{{ item.quantity }}</span>
                    <span class="item-price">NT$ {{ item.price?.toFixed(0) }}</span>
                  </div>
                </div>
                <p v-else>無商品資訊</p>
              </div>

              <el-divider />

              <div class="order-meta-info">
                <div class="meta-row">
                  <span class="meta-label">訂單編號：</span>
                  <span class="meta-value">{{ orderInfo.id }}</span>
                </div>
                <div class="meta-row" v-if="orderInfo.createdAt">
                  <span class="meta-label">下單時間：</span>
                  <span class="meta-value">{{ formatDateTime(orderInfo.createdAt) }}</span>
                </div>
              </div>

              <a :href="`/orders/${orderInfo.id}`" target="_blank" class="full-detail-link">
                <el-button type="primary" size="small" plain>
                  開啟完整訂單頁面 →
                </el-button>
              </a>
            </div>

            <!-- Loading State -->
            <div v-else-if="userContext && (userContext.productId || userContext.orderId)" class="loading-state">
              <el-icon class="loading-icon"><Loading /></el-icon>
              <p>載入中...</p>
            </div>

            <!-- Empty State -->
            <el-empty v-else description="暫無用戶上下文資訊" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Modify Dialog -->
    <el-dialog v-model="modifyDialogVisible" title="修改 AI 建議" width="600px">
      <el-input
        v-model="modifiedText"
        type="textarea"
        :rows="6"
        placeholder="修改建議內容..."
      />
      <template #footer>
        <el-button @click="modifyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmModify">確認發送</el-button>
      </template>
    </el-dialog>

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectDialogVisible" title="拒絕建議並手動回覆" width="600px">
      <el-input
        v-model="manualReply"
        type="textarea"
        :rows="6"
        placeholder="輸入手動回覆內容..."
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">確認發送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Position, Promotion, Loading, Document } from '@element-plus/icons-vue'
import chatApi, { SessionDto, ChatHistory, AiSuggestionDto } from '@/api/chat'
import { useChatWebSocket, type ChatMessage as WSMessage } from '@/composables/useChatWebSocket'

const { subscribeToSessionUpdates, subscribeToAdminNewMessages, subscribeToAdminSuggestions, subscribeToAdminUserActions } = useChatWebSocket()
const sessions = ref<SessionDto[]>([])
const currentSession = ref<SessionDto | null>(null)
const chatHistory = ref<ChatHistory[]>([])
const pendingSuggestions = ref<AiSuggestionDto[]>([])
const searchQuery = ref('')
const chatHistoryRef = ref<HTMLElement>()
const manualMessage = ref('')
let currentSubscription: any = null

// AI generating status
const aiGeneratingStatus = ref(false)

// Tab and Context states
const activeTab = ref('suggestions')
const userContext = ref<{
  currentPage?: string
  productId?: number
  orderId?: number
  lastUpdate: number
} | null>(null)
const productInfo = ref<any>(null)
const orderInfo = ref<any>(null)

// Dialog states
const modifyDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const modifiedText = ref('')
const manualReply = ref('')
const currentSuggestion = ref<AiSuggestionDto | null>(null)

// Computed
const filteredSessions = computed(() => {
  if (!searchQuery.value) return sessions.value
  const query = searchQuery.value.toLowerCase()
  return sessions.value.filter(s =>
    s.sessionId.toLowerCase().includes(query) ||
    s.lastMessage.toLowerCase().includes(query) ||
    s.userId.toString().includes(query)
  )
})

const currentSuggestions = computed(() => {
  if (!currentSession.value) return []
  return pendingSuggestions.value.filter(
    s => s.sessionId === currentSession.value?.sessionId
  )
})

const pendingSuggestionsCount = computed(() => pendingSuggestions.value.length)

// Lifecycle
onMounted(async () => {
  loadSessions()
  loadPendingSuggestions()

  try {
    // Subscribe to global admin notifications for new messages from customers
    await subscribeToAdminNewMessages(async (notification) => {
      console.log('New message notification:', notification)
      // Reload sessions list to show the new message
      await loadSessions()
    })

    // Subscribe to global admin notifications for new AI suggestions
    await subscribeToAdminSuggestions(async (suggestion) => {
      console.log('New AI suggestion:', suggestion)
      // Reload pending suggestions
      await loadPendingSuggestions()
      // Also reload sessions to update the badge
      await loadSessions()
    })

    // Subscribe to global admin notifications for user actions
    await subscribeToAdminUserActions(async (action) => {
      console.log('User action notification:', action)

      // Update context panel if viewing this user's session
      if (currentSession.value && currentSession.value.sessionId === action.sessionId) {
        parseUserAction(action.actionTarget)
        await loadChatHistory(currentSession.value.sessionId)
      }

      // Also reload sessions to update last action time
      await loadSessions()
    })
  } catch (error) {
    console.error('Failed to subscribe to admin notifications:', error)
  }

  // Auto refresh every 30 seconds (as fallback, less frequent since we have WebSocket)
  setInterval(() => {
    loadSessions()
    loadPendingSuggestions()
  }, 30000)
})

// Watch for session changes to subscribe to session-specific updates
watch(currentSession, async (newSession, oldSession) => {
  // Unsubscribe from old session
  if (currentSubscription) {
    currentSubscription.unsubscribe()
    currentSubscription = null
  }

  // Reset AI generating status when switching sessions
  aiGeneratingStatus.value = false

  // Subscribe to new session updates
  if (newSession) {
    try {
      currentSubscription = await subscribeToSessionUpdates(newSession.sessionId, async (message: WSMessage) => {
        console.log('Received session update:', message)

        // Handle AI generating status
        if (message.type === 'ai_generating') {
          aiGeneratingStatus.value = true
        } else {
          // Clear generating status when any actual message arrives
          aiGeneratingStatus.value = false
        }

        // Reload chat history when there's any update (user message, admin reply, AI auto reply, etc.)
        if (currentSession.value) {
          await loadChatHistory(currentSession.value.sessionId)
        }

        // Also reload sessions list to update last message
        await loadSessions()
      })
    } catch (error) {
      console.error('Failed to subscribe to session updates:', error)
    }
  }
})

// Methods
async function loadSessions() {
  try {
    sessions.value = await chatApi.getSessions()
  } catch (error) {
    console.error('Failed to load sessions:', error)
    ElMessage.error('載入對話列表失敗')
  }
}

async function loadPendingSuggestions() {
  try {
    pendingSuggestions.value = await chatApi.getPendingSuggestions()
  } catch (error) {
    console.error('Failed to load suggestions:', error)
  }
}

async function selectSession(session: SessionDto) {
  currentSession.value = session
  await loadChatHistory(session.sessionId)
}

async function loadChatHistory(sessionId: string) {
  try {
    chatHistory.value = await chatApi.getSessionHistory(sessionId)

    // Load initial context from chat history (find most recent user action with product/order)
    loadInitialContextFromHistory()

    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('Failed to load chat history:', error)
    ElMessage.error('載入對話歷史失敗')
  }
}

function scrollToBottom() {
  if (chatHistoryRef.value) {
    chatHistoryRef.value.scrollTop = chatHistoryRef.value.scrollHeight
  }
}

async function approveSuggestion(suggestion: AiSuggestionDto) {
  try {
    await chatApi.approveSuggestion({
      sessionId: suggestion.sessionId,
      userId: suggestion.userId,
      aiResponseId: suggestion.aiResponseId,
      text: suggestion.suggestedText
    })
    ElMessage.success('已批准並發送建議')
    await loadPendingSuggestions()
    if (currentSession.value) {
      await loadChatHistory(currentSession.value.sessionId)
    }
  } catch (error) {
    console.error('Failed to approve suggestion:', error)
    ElMessage.error('批准建議失敗')
  }
}

function showModifyDialog(suggestion: AiSuggestionDto) {
  currentSuggestion.value = suggestion
  modifiedText.value = suggestion.suggestedText
  modifyDialogVisible.value = true
}

async function confirmModify() {
  if (!currentSuggestion.value) return

  try {
    await chatApi.modifySuggestion({
      sessionId: currentSuggestion.value.sessionId,
      userId: currentSuggestion.value.userId,
      aiResponseId: currentSuggestion.value.aiResponseId,
      text: modifiedText.value,
      originalSuggestion: currentSuggestion.value.suggestedText
    })
    ElMessage.success('已修改並發送')
    modifyDialogVisible.value = false
    await loadPendingSuggestions()
    if (currentSession.value) {
      await loadChatHistory(currentSession.value.sessionId)
    }
  } catch (error) {
    console.error('Failed to modify suggestion:', error)
    ElMessage.error('修改建議失敗')
  }
}

function showRejectDialog(suggestion: AiSuggestionDto) {
  currentSuggestion.value = suggestion
  manualReply.value = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!currentSuggestion.value) return

  try {
    await chatApi.rejectSuggestion({
      sessionId: currentSuggestion.value.sessionId,
      userId: currentSuggestion.value.userId,
      aiResponseId: currentSuggestion.value.aiResponseId,
      text: manualReply.value
    })
    ElMessage.success('已拒絕建議並發送手動回覆')
    rejectDialogVisible.value = false
    await loadPendingSuggestions()
    if (currentSession.value) {
      await loadChatHistory(currentSession.value.sessionId)
    }
  } catch (error) {
    console.error('Failed to reject suggestion:', error)
    ElMessage.error('拒絕建議失敗')
  }
}

function formatTime(timestamp: number): string {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '剛剛'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分鐘前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小時前`

  return date.toLocaleDateString('zh-TW')
}

function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-TW', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function sendManualMessage() {
  if (!currentSession.value || !manualMessage.value.trim()) return

  try {
    await chatApi.sendDirectMessage({
      sessionId: currentSession.value.sessionId,
      userId: currentSession.value.userId,
      text: manualMessage.value
    })

    ElMessage.success('訊息已發送')
    manualMessage.value = ''

    // Reload chat history and scroll to bottom
    await loadChatHistory(currentSession.value.sessionId)
    await loadSessions() // Refresh session list
  } catch (error) {
    console.error('Failed to send message:', error)
    ElMessage.error('發送訊息失敗')
  }
}

// Load initial context from chat history (find most recent action with product/order or general page)
function loadInitialContextFromHistory() {
  // Find most recent action record with actionTarget
  const recentActions = chatHistory.value
    .filter(msg => msg.actionType && msg.actionTarget)
    .reverse() // Most recent first

  // Get the most recent action (regardless of type)
  if (recentActions.length > 0) {
    const mostRecentAction = recentActions[0]
    const target = mostRecentAction.actionTarget || ''
    console.log('Loading initial context from history:', target)
    parseUserAction(target, false) // Don't auto-switch tab on initial load
    return
  }

  // No action found, clear context
  userContext.value = null
  productInfo.value = null
  orderInfo.value = null
}

// Parse user action to update context (only shows current viewing, not history)
function parseUserAction(actionTarget: string, autoSwitchTab = true) {
  // Extract product ID from action like "查看商品詳情 (商品 ID: 123)"
  const productMatch = actionTarget.match(/商品\s*ID[:：]\s*(\d+)/)
  if (productMatch) {
    const productId = parseInt(productMatch[1])
    // Reset entire context to only show current product
    userContext.value = {
      currentPage: actionTarget,
      productId,
      lastUpdate: Date.now()
    }
    productInfo.value = null
    orderInfo.value = null
    loadProductInfo(productId)
    if (autoSwitchTab) {
      activeTab.value = 'context'
    }
    return
  }

  // Extract order ID from action like "查看訂單詳情 (訂單 ID: 123)"
  const orderMatch = actionTarget.match(/訂單\s*ID[:：]\s*(\d+)/)
  if (orderMatch) {
    const orderId = parseInt(orderMatch[1])
    // Reset entire context to only show current order
    userContext.value = {
      currentPage: actionTarget,
      orderId,
      lastUpdate: Date.now()
    }
    productInfo.value = null
    orderInfo.value = null
    loadOrderInfo(orderId)
    if (autoSwitchTab) {
      activeTab.value = 'context'
    }
    return
  }

  // General page action - clear product/order context
  userContext.value = {
    currentPage: actionTarget,
    lastUpdate: Date.now()
  }
  productInfo.value = null
  orderInfo.value = null
}

async function loadProductInfo(productId: number) {
  try {
    productInfo.value = await chatApi.getProduct(productId)
  } catch (error) {
    console.error('Failed to load product info:', error)
    ElMessage.error('載入商品資訊失敗')
  }
}

async function loadOrderInfo(orderId: number) {
  try {
    orderInfo.value = await chatApi.getOrder(orderId)
  } catch (error) {
    console.error('Failed to load order info:', error)
    ElMessage.error('載入訂單資訊失敗')
  }
}

function getOrderStatusType(status: string): string {
  const typeMap: Record<string, string> = {
    'PENDING': 'warning',
    'CONFIRMED': 'info',
    'SHIPPED': 'primary',
    'DELIVERED': 'success',
    'CANCELLED': 'danger'
  }
  return typeMap[status] || 'info'
}

function getOrderStatusText(status: string): string {
  const textMap: Record<string, string> = {
    'PENDING': '待處理',
    'CONFIRMED': '已確認',
    'SHIPPED': '已出貨',
    'DELIVERED': '已送達',
    'CANCELLED': '已取消'
  }
  return textMap[status] || status
}
</script>

<style scoped>
.chat-management {
  max-width: 1600px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 30px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h3 {
  margin: 0;
  color: #333;
}

.session-search {
  margin-bottom: 15px;
}

.sessions-list {
  max-height: 550px;
  overflow-y: auto;
}

.session-item {
  padding: 12px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background-color 0.3s;
}

.session-item:hover {
  background-color: #f5f7fa;
}

.session-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409EFF;
}

.session-item.has-suggestion {
  border-left: 3px solid #E6A23C;
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.session-message {
  color: #666;
  font-size: 14px;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  color: #999;
  font-size: 12px;
}

.chat-card {
  height: 600px;
  display: flex;
  flex-direction: column;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 520px;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-input-area {
  border-top: 1px solid #eee;
  padding: 15px;
  background-color: #f9f9f9;
}

.send-button {
  width: 100%;
  margin-top: 10px;
}

.chat-message {
  margin-bottom: 20px;
  padding: 12px;
  border-radius: 8px;
}

.chat-message.user {
  background-color: #f0f0f0;
}

.chat-message.assistant {
  background-color: #e8f4fd;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.message-time {
  color: #999;
  font-size: 12px;
}

.message-content {
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-action {
  margin-top: 8px;
  padding: 8px;
  background-color: rgba(64, 158, 255, 0.1);
  border-radius: 4px;
  font-size: 12px;
  color: #409EFF;
  display: flex;
  align-items: center;
  gap: 5px;
}

/* AI Generating Status */
.chat-message.ai-generating {
  background: linear-gradient(90deg, #fff3cd 0%, #ffe8a1 50%, #fff3cd 100%);
  background-size: 200% 100%;
  animation: shimmer 2s infinite;
  border: 1px dashed #f0ad4e;
}

.generating-text {
  color: #856404;
  font-style: italic;
}

.loading-icon {
  animation: spin 1s linear infinite;
  margin-right: 4px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes shimmer {
  0% { background-position: 0% 0%; }
  50% { background-position: 100% 0%; }
  100% { background-position: 0% 0%; }
}

.empty-state {
  height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.suggestions-list {
  max-height: 550px;
  overflow-y: auto;
}

.suggestion-item {
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 15px;
  background-color: #fffbf0;
}

.suggestion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.suggestion-time {
  color: #999;
  font-size: 12px;
}

.suggestion-text {
  color: #333;
  line-height: 1.6;
  margin-bottom: 12px;
  padding: 10px;
  background-color: white;
  border-radius: 4px;
}

.tool-calls {
  margin-bottom: 12px;
}

.tool-call {
  padding: 8px;
  background-color: white;
  border-radius: 4px;
  margin-bottom: 8px;
}

.tool-call strong {
  color: #409EFF;
  display: block;
  margin-bottom: 5px;
}

.tool-result {
  color: #666;
  font-size: 12px;
  white-space: pre-wrap;
}

.suggestion-actions {
  display: flex;
  gap: 8px;
}

.suggestion-actions .el-button {
  flex: 1;
}

/* Context Panel Styles */
.context-tabs {
  margin: -20px -20px 0 -20px;
}

.context-panel {
  max-height: 550px;
  overflow-y: auto;
  padding: 16px;
}

/* Context Header */
.context-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.context-badge {
  background: #ecf5ff;
  color: #409EFF;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.last-update {
  color: #909399;
  font-size: 11px;
}

/* General Page Context Styles */
.general-context {
  animation: fadeIn 0.3s ease-in;
}

.page-info-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ecf0f3 100%);
  border-radius: 12px;
  text-align: center;
}

.page-icon {
  font-size: 48px;
  color: #909399;
  margin-bottom: 16px;
}

.page-info-display h3 {
  font-size: 16px;
  font-weight: 500;
  color: #606266;
  margin: 0;
  line-height: 1.6;
}

/* Product Context Styles */
.product-context {
  animation: fadeIn 0.3s ease-in;
}

.product-name {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
  line-height: 1.4;
}

.product-price-display {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}

.product-price-display .currency {
  font-size: 18px;
  color: #f56c6c;
  font-weight: 500;
}

.product-price-display .price-value {
  font-size: 32px;
  font-weight: bold;
  color: #f56c6c;
}

.product-stock-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.stock-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.stock-status {
  font-size: 14px;
  font-weight: 600;
}

.stock-status.in-stock {
  color: #67c23a;
}

.stock-status.out-of-stock {
  color: #f56c6c;
}

.product-description {
  margin-bottom: 12px;
}

.product-description h4 {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.product-description p {
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  margin: 0;
}

.product-meta-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

/* Order Context Styles */
.order-context {
  animation: fadeIn 0.3s ease-in;
}

.order-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.order-status-display {
  margin-bottom: 8px;
}

.order-amount {
  margin-bottom: 12px;
}

.order-amount .amount-label {
  display: block;
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.order-amount .amount-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.order-amount .currency {
  font-size: 18px;
  color: #f56c6c;
  font-weight: 500;
}

.order-amount .price {
  font-size: 32px;
  font-weight: bold;
  color: #f56c6c;
}

.order-items-summary {
  margin-bottom: 12px;
}

.order-items-summary h4 {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.order-item:last-child {
  border-bottom: none;
}

.order-item .item-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
}

.order-item .item-qty {
  font-size: 12px;
  color: #909399;
  margin: 0 12px;
}

.order-item .item-price {
  font-size: 13px;
  color: #f56c6c;
  font-weight: 600;
}

.order-meta-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

/* Common Meta Styles */
.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.meta-label {
  color: #909399;
  font-weight: 500;
  min-width: 80px;
}

.meta-value {
  color: #606266;
}

/* Full Detail Link */
.full-detail-link {
  display: block;
  text-decoration: none;
  margin-top: 16px;
}

.full-detail-link .el-button {
  width: 100%;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.loading-state .loading-icon {
  font-size: 32px;
  margin-bottom: 12px;
  animation: spin 1s linear infinite;
}

.loading-state p {
  font-size: 13px;
  margin: 0;
}

/* Animation */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
