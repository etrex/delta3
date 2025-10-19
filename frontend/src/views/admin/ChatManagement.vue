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

      <!-- Right: AI Suggestions -->
      <el-col :span="6">
        <el-card>
          <template #header>
            <h3>AI 建議</h3>
          </template>

          <div class="suggestions-list">
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Position, Promotion } from '@element-plus/icons-vue'
import chatApi, { SessionDto, ChatHistory, AiSuggestionDto } from '@/api/chat'

const sessions = ref<SessionDto[]>([])
const currentSession = ref<SessionDto | null>(null)
const chatHistory = ref<ChatHistory[]>([])
const pendingSuggestions = ref<AiSuggestionDto[]>([])
const searchQuery = ref('')
const chatHistoryRef = ref<HTMLElement>()
const manualMessage = ref('')

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
onMounted(() => {
  loadSessions()
  loadPendingSuggestions()
  // Auto refresh every 10 seconds
  setInterval(() => {
    loadSessions()
    loadPendingSuggestions()
  }, 10000)
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
</style>
