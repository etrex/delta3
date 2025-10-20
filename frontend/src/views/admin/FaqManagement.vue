<!--
 Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="faq-management">
    <div class="header-section">
      <h1>FAQ 管理</h1>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        新增 FAQ
      </el-button>
    </div>

    <!-- Search and Filter -->
    <div class="filter-section">
      <el-input
        v-model="searchKeyword"
        placeholder="搜尋問題或答案..."
        clearable
        style="width: 400px; margin-right: 15px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select
        v-model="filterCategory"
        placeholder="篩選分類"
        clearable
        style="width: 200px"
      >
        <el-option label="全部分類" value=""></el-option>
        <el-option
          v-for="category in categories"
          :key="category"
          :label="category"
          :value="category"
        ></el-option>
      </el-select>
    </div>

    <!-- FAQ Table -->
    <el-table
      :data="filteredFaqs"
      stripe
      style="width: 100%"
      v-loading="loading"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="category" label="分類" width="120">
        <template #default="scope">
          <el-tag size="small">{{ scope.row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="question" label="問題" min-width="300">
        <template #default="scope">
          <div class="question-preview">{{ scope.row.question }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="answer" label="答案" min-width="400">
        <template #default="scope">
          <div class="answer-preview">{{ scope.row.answer }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="建立時間" width="180">
        <template #default="scope">
          {{ formatDateTime(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            @click="showEditDialog(scope.row)"
          >
            編輯
          </el-button>
          <el-button
            type="danger"
            size="small"
            @click="handleDelete(scope.row)"
          >
            刪除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '編輯 FAQ' : '新增 FAQ'"
      width="700px"
    >
      <el-form
        :model="formData"
        :rules="formRules"
        ref="formRef"
        label-width="100px"
      >
        <el-form-item label="分類" prop="category">
          <el-select
            v-model="formData.category"
            placeholder="選擇分類"
            allow-create
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="category in categories"
              :key="category"
              :label="category"
              :value="category"
            ></el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="問題" prop="question">
          <el-input
            v-model="formData.question"
            type="textarea"
            :rows="3"
            placeholder="輸入問題..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="答案" prop="answer">
          <el-input
            v-model="formData.answer"
            type="textarea"
            :rows="8"
            placeholder="輸入答案..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          確認
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import faqsApi, { type Faq } from '@/api/faqs'

const faqs = ref<Faq[]>([])
const categories = ref<string[]>([])
const searchKeyword = ref('')
const filterCategory = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = ref({
  id: 0,
  question: '',
  answer: '',
  category: ''
})

const formRules: FormRules = {
  category: [
    { required: true, message: '請選擇或輸入分類', trigger: 'blur' }
  ],
  question: [
    { required: true, message: '請輸入問題', trigger: 'blur' },
    { min: 5, max: 500, message: '問題長度需在 5-500 字之間', trigger: 'blur' }
  ],
  answer: [
    { required: true, message: '請輸入答案', trigger: 'blur' },
    { min: 10, message: '答案至少需要 10 個字', trigger: 'blur' }
  ]
}

// Computed
const filteredFaqs = computed(() => {
  let result = faqs.value

  // Filter by category
  if (filterCategory.value) {
    result = result.filter(faq => faq.category === filterCategory.value)
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

// Methods
async function loadFaqs() {
  loading.value = true
  try {
    faqs.value = await faqsApi.getFaqs()
    console.log(`Loaded ${faqs.value.length} FAQs`)
  } catch (error) {
    console.error('Failed to load FAQs:', error)
    ElMessage.error('載入 FAQ 失敗')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await faqsApi.getCategories()
  } catch (error) {
    console.error('Failed to load categories:', error)
  }
}

function showCreateDialog() {
  isEdit.value = false
  formData.value = {
    id: 0,
    question: '',
    answer: '',
    category: ''
  }
  dialogVisible.value = true
}

function showEditDialog(faq: Faq) {
  isEdit.value = true
  formData.value = {
    id: faq.id,
    question: faq.question,
    answer: faq.answer,
    category: faq.category
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        await faqsApi.updateFaq(formData.value.id, {
          question: formData.value.question,
          answer: formData.value.answer,
          category: formData.value.category
        })
        ElMessage.success('FAQ 更新成功')
      } else {
        await faqsApi.createFaq({
          question: formData.value.question,
          answer: formData.value.answer,
          category: formData.value.category
        })
        ElMessage.success('FAQ 建立成功')
      }

      dialogVisible.value = false
      await Promise.all([loadFaqs(), loadCategories()])
    } catch (error) {
      console.error('Failed to save FAQ:', error)
      ElMessage.error('儲存失敗')
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(faq: Faq) {
  try {
    await ElMessageBox.confirm(
      `確定要刪除這個 FAQ 嗎？\n問題：${faq.question}`,
      '刪除確認',
      {
        confirmButtonText: '確定刪除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await faqsApi.deleteFaq(faq.id)
    ElMessage.success('FAQ 已刪除')
    await Promise.all([loadFaqs(), loadCategories()])
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete FAQ:', error)
      ElMessage.error('刪除失敗')
    }
  }
}

function formatDateTime(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Lifecycle
onMounted(async () => {
  await Promise.all([loadFaqs(), loadCategories()])
})
</script>

<style scoped>
.faq-management {
  padding: 20px;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

h1 {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

.filter-section {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.question-preview,
.answer-preview {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}

.answer-preview {
  color: #606266;
  font-size: 14px;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-dialog__body) {
  padding: 20px;
}
</style>
