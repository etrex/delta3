<!--
 Copyright (c) 2025 Etrex Kuo. All rights reserved.
-->
<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2>Order Management System</h2>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="Role" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio label="CUSTOMER">Customer</el-radio>
            <el-radio label="ADMIN">Admin</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Username" prop="username">
          <el-input v-model="form.username" placeholder="Enter username" />
        </el-form-item>

        <el-form-item label="Password" prop="password">
          <el-input v-model="form.password" type="password" placeholder="Enter password" show-password />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading">Login</el-button>
          <el-button @click="fillTestAccount">Use Test Account</el-button>
        </el-form-item>
      </el-form>

      <div class="test-accounts">
        <el-divider>Test Accounts</el-divider>
        <p><strong>Admin:</strong> admin / password123</p>
        <p><strong>Customer:</strong> customer1 / password123</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  role: 'CUSTOMER',
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: 'Please enter username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please enter password', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true

      const success = await authStore.login(form.username, form.password)

      loading.value = false

      if (success) {
        ElMessage.success('Login successful!')
        router.push('/dashboard')
      } else {
        ElMessage.error('Invalid username or password')
      }
    }
  })
}

const fillTestAccount = () => {
  if (form.role === 'ADMIN') {
    form.username = 'admin'
    form.password = 'password123'
  } else {
    form.username = 'customer1'
    form.password = 'password123'
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 450px;
}

.login-card h2 {
  text-align: center;
  margin: 0;
}

.test-accounts {
  margin-top: 20px;
  font-size: 14px;
  color: #666;
}

.test-accounts p {
  margin: 5px 0;
}
</style>