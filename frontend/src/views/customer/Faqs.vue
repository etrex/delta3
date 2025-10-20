<!--
 Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="faqs-container">
    <h1>常見問題</h1>

    <!-- Search Box -->
    <div class="search-box">
      <el-input
        v-model="searchKeyword"
        placeholder="搜尋問題或答案..."
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- Category Tabs -->
    <el-tabs v-model="currentCategory" @tab-click="handleCategoryChange">
      <el-tab-pane label="全部" name="全部">
        <template #label>
          <span>全部 <el-tag size="small" type="info">{{ totalCount }}</el-tag></span>
        </template>
      </el-tab-pane>
      <el-tab-pane
        v-for="category in categories"
        :key="category"
        :label="category"
        :name="category"
      >
        <template #label>
          <span>{{ category }} <el-tag size="small">{{ getCategoryCount(category) }}</el-tag></span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- FAQ List -->
    <div class="faq-list">
      <el-collapse v-model="activeItems" accordion>
        <el-collapse-item
          v-for="faq in filteredFaqs"
          :key="faq.id"
          :name="faq.id"
          class="faq-item"
        >
          <template #title>
            <div class="faq-title">
              <el-icon class="question-icon"><QuestionFilled /></el-icon>
              <span v-html="highlightKeyword(faq.question)"></span>
            </div>
          </template>
          <div class="faq-content">
            <p v-html="highlightKeyword(faq.answer)"></p>
            <div class="faq-meta">
              <el-tag size="small" type="success">{{ faq.category }}</el-tag>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>

      <!-- Empty State -->
      <el-empty v-if="filteredFaqs.length === 0" description="找不到相關問題">
        <el-button type="primary" @click="clearSearch">清除搜尋</el-button>
      </el-empty>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-overlay">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search, QuestionFilled, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import faqsApi, { type Faq } from '@/api/faqs'

const faqs = ref<Faq[]>([])
const categories = ref<string[]>([])
const currentCategory = ref('全部')
const searchKeyword = ref('')
const activeItems = ref<number[]>([])
const loading = ref(false)

// Computed
const filteredFaqs = computed(() => {
  let result = faqs.value

  // Filter by category
  if (currentCategory.value !== '全部') {
    result = result.filter(faq => faq.category === currentCategory.value)
  }

  // Filter by search keyword
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(faq =>
      faq.question.toLowerCase().includes(keyword) ||
      faq.answer.toLowerCase().includes(keyword)
    )
  }

  return result
})

const totalCount = computed(() => faqs.value.length)

function getCategoryCount(category: string): number {
  return faqs.value.filter(faq => faq.category === category).length
}

// Methods
async function loadFaqs() {
  loading.value = true
  try {
    faqs.value = await faqsApi.getFaqs()
    console.log(`Loaded ${faqs.value.length} FAQs`)
  } catch (error) {
    console.error('Failed to load FAQs:', error)
    ElMessage.error('載入常見問題失敗')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await faqsApi.getCategories()
    console.log(`Loaded ${categories.value.length} categories`)
  } catch (error) {
    console.error('Failed to load categories:', error)
  }
}

function handleCategoryChange() {
  // Reset search when category changes
  searchKeyword.value = ''
  activeItems.value = []
}

function handleSearch() {
  // Reset active items when searching
  activeItems.value = []
}

function clearSearch() {
  searchKeyword.value = ''
  currentCategory.value = '全部'
  activeItems.value = []
}

function highlightKeyword(text: string): string {
  if (!searchKeyword.value.trim()) {
    return text
  }

  const keyword = searchKeyword.value.trim()
  const regex = new RegExp(`(${keyword})`, 'gi')
  return text.replace(regex, '<mark>$1</mark>')
}

// Lifecycle
onMounted(async () => {
  await Promise.all([loadFaqs(), loadCategories()])
})
</script>

<style scoped>
.faqs-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

h1 {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 30px;
  color: #303133;
}

.search-box {
  margin-bottom: 30px;
}

.search-box :deep(.el-input) {
  max-width: 600px;
}

.faq-list {
  margin-top: 20px;
  position: relative;
}

.faq-item {
  margin-bottom: 10px;
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  transition: all 0.3s;
}

.faq-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
}

.faq-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.question-icon {
  color: #409EFF;
  font-size: 20px;
  flex-shrink: 0;
}

.faq-content {
  padding: 20px;
  background-color: #F5F7FA;
  border-radius: 4px;
  line-height: 1.8;
}

.faq-content p {
  margin: 0 0 15px 0;
  color: #606266;
  white-space: pre-wrap;
}

.faq-meta {
  display: flex;
  justify-content: flex-end;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #E4E7ED;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: #409EFF;
}

/* Highlight search keyword */
:deep(mark) {
  background-color: #FFF566;
  padding: 2px 4px;
  border-radius: 2px;
  font-weight: bold;
}

/* Responsive */
@media (max-width: 768px) {
  .faqs-container {
    padding: 20px 15px;
  }

  h1 {
    font-size: 24px;
    margin-bottom: 20px;
  }

  .search-box :deep(.el-input) {
    max-width: 100%;
  }

  .faq-title {
    font-size: 14px;
  }

  .faq-content {
    padding: 15px;
  }
}
</style>
