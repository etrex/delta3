<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-width="120px"
    data-cy="product-form"
  >
    <el-form-item label="商品名稱" prop="name" data-cy="name-error">
      <el-input
        v-model="formData.name"
        placeholder="請輸入商品名稱"
        data-cy="product-name-input"
      />
    </el-form-item>

    <el-form-item label="商品描述" prop="description">
      <el-input
        v-model="formData.description"
        type="textarea"
        :rows="3"
        placeholder="請輸入商品描述"
        data-cy="product-description-input"
      />
    </el-form-item>

    <el-form-item label="價格" prop="price" data-cy="price-error">
      <el-input
        v-model.number="formData.price"
        placeholder="請輸入價格"
        data-cy="product-price-input"
      >
        <template #prepend>$</template>
      </el-input>
    </el-form-item>

    <el-form-item label="庫存數量" prop="stock" data-cy="stock-error">
      <el-input
        v-model.number="formData.stock"
        type="number"
        placeholder="請輸入庫存數量"
        data-cy="product-stock-input"
      />
    </el-form-item>

    <el-form-item label="庫存警告門檻" prop="stockThreshold">
      <el-input
        v-model.number="formData.stockThreshold"
        type="number"
        placeholder="請輸入庫存警告門檻"
        data-cy="stock-threshold-input"
      />
      <div class="form-help">當庫存低於此值時將顯示警告</div>
    </el-form-item>

    <el-form-item label="商品狀態" prop="status">
      <el-select
        v-model="formData.status"
        placeholder="請選擇商品狀態"
        data-cy="product-status-select"
      >
        <el-option label="上架" value="ACTIVE" />
        <el-option label="下架" value="INACTIVE" />
      </el-select>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="handleSubmit" data-cy="save-product-btn">
        {{ isEdit ? '更新商品' : '創建商品' }}
      </el-button>
      <el-button @click="handleCancel" data-cy="cancel-btn">
        取消
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

export interface ProductFormData {
  name: string
  description: string
  price: number | null
  stock: number | null
  stockThreshold: number | null
  status: string
}

interface Props {
  initialData?: ProductFormData
  isEdit?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isEdit: false
})

const emit = defineEmits<{
  (e: 'submit', data: ProductFormData): void
  (e: 'cancel'): void
}>()

const formRef = ref<FormInstance>()
const formData = reactive<ProductFormData>({
  name: '',
  description: '',
  price: null,
  stock: null,
  stockThreshold: 5,
  status: 'ACTIVE'
})

watch(() => props.initialData, (newData) => {
  if (newData) {
    Object.assign(formData, newData)
  }
}, { immediate: true })

const rules: FormRules = {
  name: [{ required: true, message: '商品名稱為必填', trigger: 'blur' }],
  price: [
    { validator: (rule, value, callback) => {
        if (value === null || value === undefined) {
          callback(new Error('價格為必填'))
        } else if (value === '' || isNaN(value)) {
          callback(new Error('請輸入有效的價格'))
        } else if (value <= 0) {
          callback(new Error('價格必須大於0'))
        } else {
          callback()
        }
      }, trigger: ['blur', 'change'], required: true }
  ],
  stock: [
    { required: true, message: '庫存數量為必填', trigger: 'blur' },
    { validator: (rule, value, callback) => {
        if (!value && value !== 0) {
          callback(new Error('請輸入庫存數量'))
        } else if (value < 0) {
          callback(new Error('庫存不能為負數'))
        } else {
          callback()
        }
      }, trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid) {
      emit('submit', { ...formData })
    }
  })
}

const handleCancel = () => {
  emit('cancel')
}
</script>

<style scoped>
.form-help {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
