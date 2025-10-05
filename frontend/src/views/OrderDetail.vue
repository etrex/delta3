<template>
  <div class="order-detail-container" v-if="order">
    <h1>訂單詳情</h1>

    <div v-if="errorMessage" class="error-message" data-cy="error-message">
      {{ errorMessage }}
      <div v-if="errorDetails" class="error-details" data-cy="error-details">{{ errorDetails }}</div>
    </div>

    <div v-if="successMessage" class="success-message" data-cy="success-message">
      {{ successMessage }}
      <div v-if="order.id" class="order-number" data-cy="order-number">訂單編號: {{ order.id }}</div>
    </div>

    <!-- 訂單標頭 -->
    <el-card class="order-header-card" data-cy="order-header">
      <div class="header-content">
        <div class="order-id" data-cy="order-id">訂單編號: {{ order.id }}</div>
        <el-tag :type="getStatusType(order.status)" data-cy="order-status" size="large">
          {{ getStatusLabel(order.status) }}
        </el-tag>
      </div>
      <div class="order-meta">
        <div class="order-date" data-cy="order-date">
          下單時間: {{ formatDateTime(order.createdAt) }}
        </div>
        <div class="order-total" data-cy="order-total">
          訂單金額: ${{ order.totalAmount?.toFixed(2) }}
        </div>
      </div>
    </el-card>

    <!-- 訂單詳情 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <!-- 商品明細 -->
        <el-card class="items-card" data-cy="order-details">
          <template #header>
            <h3>商品明細</h3>
          </template>
          <div class="order-items" data-cy="order-items">
            <div
              v-for="item in order.items"
              :key="item.id"
              class="order-item"
              data-cy="order-item"
            >
              <div class="item-image" data-cy="product-image">
                <!-- Placeholder for product image -->
                <div class="image-placeholder"></div>
              </div>
              <div class="item-details">
                <div class="product-name" data-cy="product-name">{{ item.productName }}</div>
                <div class="item-meta">
                  <span class="item-price" data-cy="item-price">${{ item.price?.toFixed(2) }}</span>
                  <span class="item-quantity" data-cy="item-quantity">x {{ item.quantity }}</span>
                  <span class="item-subtotal" data-cy="item-subtotal">
                    ${{ ((item.price || 0) * item.quantity).toFixed(2) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 出貨資訊 -->
        <el-card class="shipping-card" data-cy="shipping-info">
          <template #header>
            <h3>出貨資訊</h3>
          </template>
          <div class="shipping-status" data-cy="shipping-status">
            狀態: {{ getShippingStatus(order.status) }}
          </div>
          <div v-if="order.status === 'SHIPPED'" class="tracking-info" data-cy="tracking-info">
            <div data-cy="shipped-date">出貨日期: {{ formatDateTime(order.updatedAt) }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <!-- 訂單摘要 -->
        <el-card class="summary-card" data-cy="order-summary">
          <template #header>
            <h3>訂單摘要</h3>
          </template>
          <div class="summary-row">
            <span>商品總計</span>
            <span data-cy="items-total">${{ order.totalAmount?.toFixed(2) }}</span>
          </div>
          <div class="summary-row">
            <span>運費</span>
            <span data-cy="shipping-fee">$0.00</span>
          </div>
          <div class="summary-row total">
            <span>訂單總額</span>
            <span data-cy="total-amount">${{ order.totalAmount?.toFixed(2) }}</span>
          </div>
        </el-card>

        <!-- 付款資訊 -->
        <el-card class="payment-card" data-cy="payment-info">
          <template #header>
            <h3>付款資訊</h3>
          </template>

          <div class="payment-status" data-cy="payment-status">
            狀態: {{ getPaymentStatus(order.status) }}
          </div>

          <!-- 已付款顯示付款詳情 -->
          <div v-if="order.payments && order.payments.length > 0" class="payment-history" data-cy="payment-history">
            <h4>付款記錄</h4>
            <div
              v-for="payment in order.payments"
              :key="payment.id"
              class="payment-record"
              data-cy="payment-record"
            >
              <div data-cy="payment-method">付款方式: {{ getPaymentMethodLabel(payment.paymentMethod) }}</div>
              <div data-cy="payment-amount">金額: ${{ payment.amount?.toFixed(2) }}</div>
              <div data-cy="payment-date">時間: {{ formatDateTime(payment.paidAt) }}</div>
              <div v-if="payment.transactionId" data-cy="transaction-id">
                交易號: {{ payment.transactionId }}
              </div>
              <el-tag :type="getPaymentStatusType(payment.status)" data-cy="payment-status">
                {{ getPaymentStatusLabel(payment.status) }}
              </el-tag>
            </div>

            <el-tag v-if="hasSuccessfulPayment" type="success" data-cy="payment-completed-badge">
              付款已完成
            </el-tag>
          </div>

          <!-- 未付款顯示付款按鈕 -->
          <div v-if="order.status === 'CREATED' && !hasSuccessfulPayment">
            <el-button
              type="primary"
              data-cy="pay-now-btn"
              @click="showPaymentModal"
              class="pay-btn"
            >立即付款</el-button>
          </div>

          <!-- 付款失敗顯示重試按鈕 -->
          <div v-if="hasFailedPayment">
            <el-tag type="danger" data-cy="payment-failed-badge">付款失敗</el-tag>
            <el-button
              type="warning"
              data-cy="retry-payment-btn"
              @click="showPaymentModal"
              class="retry-btn"
            >重試付款</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 付款 Modal -->
    <el-dialog
      v-model="paymentModalVisible"
      title="選擇付款方式"
      width="600px"
      data-cy="payment-modal"
    >
      <div class="payment-methods" data-cy="payment-methods">
        <el-radio-group v-model="selectedPaymentMethod" class="payment-method-group">
          <el-radio value="CREDIT_CARD" data-cy="payment-method-credit-card">信用卡</el-radio>
          <el-radio value="BANK_TRANSFER" data-cy="payment-method-bank-transfer">銀行轉帳</el-radio>
          <el-radio value="PAYPAL" data-cy="payment-method-paypal">PayPal</el-radio>
          <el-radio value="CASH" data-cy="payment-method-cash">貨到付款</el-radio>
        </el-radio-group>
      </div>

      <div class="payment-amount-display" data-cy="payment-amount">
        付款金額: ${{ order.totalAmount?.toFixed(2) }}
      </div>

      <!-- 信用卡表單 -->
      <div v-if="selectedPaymentMethod === 'CREDIT_CARD'" class="credit-card-form" data-cy="credit-card-form">
        <el-form :model="creditCardForm" :rules="creditCardRules" ref="creditCardFormRef">
          <el-form-item label="卡號" prop="cardNumber">
            <el-input
              v-model="creditCardForm.cardNumber"
              data-cy="card-number"
              placeholder="1234 5678 9012 3456"
              maxlength="16"
            />
            <div v-if="errorFields.cardNumber" class="field-error" data-cy="card-number-error">
              請輸入卡號
            </div>
          </el-form-item>

          <el-form-item label="有效期限" prop="cardExpiry">
            <el-input
              v-model="creditCardForm.cardExpiry"
              data-cy="card-expiry"
              placeholder="MM/YY"
              maxlength="5"
            />
            <div v-if="errorFields.cardExpiry" class="field-error" data-cy="card-expiry-error">
              請輸入有效期
            </div>
          </el-form-item>

          <el-form-item label="安全碼" prop="cardCvv">
            <el-input
              v-model="creditCardForm.cardCvv"
              data-cy="card-cvv"
              type="password"
              placeholder="123"
              maxlength="3"
            />
            <div v-if="errorFields.cardCvv" class="field-error" data-cy="card-cvv-error">
              請輸入安全碼
            </div>
          </el-form-item>

          <el-form-item label="持卡人姓名" prop="cardName">
            <el-input
              v-model="creditCardForm.cardName"
              data-cy="card-name"
              placeholder="JOHN DOE"
            />
          </el-form-item>
        </el-form>

        <div class="security-notice" data-cy="security-notice">
          <el-icon data-cy="ssl-indicator"><Lock /></el-icon>
          安全付款 - 您的資料受到加密保護
        </div>
      </div>

      <!-- 銀行轉帳資訊 -->
      <div v-if="selectedPaymentMethod === 'BANK_TRANSFER'" class="bank-transfer-info" data-cy="bank-transfer-info">
        <p data-cy="bank-account">銀行帳號: 1234-5678-9012</p>
        <p data-cy="bank-code">銀行代碼: 808</p>
        <p data-cy="transfer-amount">轉帳金額: ${{ order.totalAmount?.toFixed(2) }}</p>
        <p data-cy="reference-number">備註: 訂單編號 {{ order.id }}</p>
      </div>

      <!-- PayPal -->
      <div v-if="selectedPaymentMethod === 'PAYPAL'" class="paypal-info" data-cy="paypal-redirect-info">
        <p>點擊確認後將跳轉至 PayPal 付款頁面</p>
        <el-button type="primary" data-cy="paypal-pay-btn" @click="processPayment">
          前往 PayPal
        </el-button>
        <div v-if="isProcessing" data-cy="paypal-processing">處理中...</div>
      </div>

      <!-- 現金付款 -->
      <div v-if="selectedPaymentMethod === 'CASH'" class="cash-payment-info" data-cy="cash-payment-info">
        <p class="cash-instructions" data-cy="cash-instructions">請於收貨時準備現金給送貨人員</p>
        <el-button type="primary" data-cy="confirm-cash-payment-btn" @click="processPayment">
          確認現金付款
        </el-button>
      </div>

      <template #footer v-if="selectedPaymentMethod !== 'PAYPAL' && selectedPaymentMethod !== 'CASH'">
        <el-button @click="paymentModalVisible = false">取消</el-button>
        <el-button
          type="primary"
          data-cy="confirm-payment-btn"
          :loading="isProcessing"
          @click="processPayment"
        >
          <span v-if="isProcessing" data-cy="payment-processing">處理中...</span>
          <span v-else>確認付款</span>
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import ordersApi from '@/api/orders'
import type { Order, Payment } from '@/types'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const paymentModalVisible = ref(false)
const selectedPaymentMethod = ref('CREDIT_CARD')
const isProcessing = ref(false)
const errorMessage = ref('')
const errorDetails = ref('')
const successMessage = ref('')

const creditCardFormRef = ref<FormInstance>()
const creditCardForm = ref({
  cardNumber: '',
  cardExpiry: '',
  cardCvv: '',
  cardName: ''
})

const errorFields = ref({
  cardNumber: false,
  cardExpiry: false,
  cardCvv: false
})

const creditCardRules = {
  cardNumber: [{ required: true, message: '請輸入卡號', trigger: 'blur' }],
  cardExpiry: [{ required: true, message: '請輸入有效期', trigger: 'blur' }],
  cardCvv: [{ required: true, message: '請輸入安全碼', trigger: 'blur' }]
}

const hasSuccessfulPayment = computed(() => {
  return order.value?.payments?.some(p => p.status === 'SUCCESS') || false
})

const hasFailedPayment = computed(() => {
  return order.value?.payments?.some(p => p.status === 'FAILED') || false
})

onMounted(async () => {
  await loadOrder()
})

async function loadOrder() {
  try {
    const orderId = Number(route.params.id)
    const response = await ordersApi.getOrder(orderId)
    order.value = response.data
  } catch (error) {
    console.error('Failed to load order:', error)
    ElMessage.error('載入訂單失敗')
  }
}

function showPaymentModal() {
  paymentModalVisible.value = true
  selectedPaymentMethod.value = 'CREDIT_CARD'
  resetCreditCardForm()
}

function resetCreditCardForm() {
  creditCardForm.value = {
    cardNumber: '',
    cardExpiry: '',
    cardCvv: '',
    cardName: ''
  }
  errorFields.value = {
    cardNumber: false,
    cardExpiry: false,
    cardCvv: false
  }
}

async function processPayment() {
  if (!order.value) return

  // Validate credit card form if payment method is credit card
  if (selectedPaymentMethod.value === 'CREDIT_CARD') {
    errorFields.value = {
      cardNumber: !creditCardForm.value.cardNumber,
      cardExpiry: !creditCardForm.value.cardExpiry,
      cardCvv: !creditCardForm.value.cardCvv
    }

    if (Object.values(errorFields.value).some(v => v)) {
      return
    }
  }

  isProcessing.value = true
  errorMessage.value = ''

  try {
    const paymentData: Partial<Payment> = {
      paymentMethod: selectedPaymentMethod.value as any,
      amount: order.value.totalAmount
    }

    const response = await ordersApi.payOrder(order.value.id!, paymentData)

    // Reload order to get updated status
    await loadOrder()

    ElMessage.success('付款成功')
    paymentModalVisible.value = false

    if (selectedPaymentMethod.value === 'CASH') {
      ElMessage.success('現金付款訂單已確認')
    }
  } catch (error: any) {
    console.error('Payment failed:', error)

    if (error.response?.data?.message) {
      errorMessage.value = '付款失敗'
      errorDetails.value = error.response.data.message
    } else {
      errorMessage.value = '付款失敗，請稍後再試'
    }
  } finally {
    isProcessing.value = false
  }
}

function formatDateTime(date: string | undefined): string {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-TW')
}

function getStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    'CREATED': '已建立',
    'PAID': '已付款',
    'APPROVED': '已批准',
    'SHIPPED': '已出貨',
    'CANCELLED': '已取消'
  }
  return labels[status] || status
}

function getStatusType(status: string): string {
  const types: Record<string, string> = {
    'CREATED': 'info',
    'PAID': 'success',
    'APPROVED': 'warning',
    'SHIPPED': 'success',
    'CANCELLED': 'danger'
  }
  return types[status] || 'info'
}

function getShippingStatus(status: string): string {
  if (status === 'SHIPPED') return '已出貨'
  if (status === 'APPROVED') return '待出貨'
  return '待出貨'
}

function getPaymentStatus(status: string): string {
  if (status === 'PAID' || status === 'APPROVED' || status === 'SHIPPED') return '已付款'
  return '待付款'
}

function getPaymentMethodLabel(method: string): string {
  const labels: Record<string, string> = {
    'CREDIT_CARD': '信用卡',
    'BANK_TRANSFER': '銀行轉帳',
    'PAYPAL': 'PayPal',
    'CASH': '貨到付款'
  }
  return labels[method] || method
}

function getPaymentStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    'PENDING': '處理中',
    'SUCCESS': '成功',
    'FAILED': '失敗',
    'REFUNDED': '已退款'
  }
  return labels[status] || status
}

function getPaymentStatusType(status: string): string {
  const types: Record<string, string> = {
    'PENDING': 'warning',
    'SUCCESS': 'success',
    'FAILED': 'danger',
    'REFUNDED': 'info'
  }
  return types[status] || 'info'
}
</script>

<style scoped>
.order-detail-container {
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  margin-bottom: 30px;
  color: #333;
}

.error-message {
  background-color: #FEF0F0;
  color: #F56C6C;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #FDE2E2;
}

.error-details {
  margin-top: 8px;
  font-size: 14px;
}

.success-message {
  background-color: #F0F9FF;
  color: #409EFF;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 20px;
  border: 1px solid #C6E2FF;
}

.order-number {
  margin-top: 8px;
  font-weight: bold;
}

.order-header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.order-id {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
}

.order-meta {
  display: flex;
  gap: 30px;
  color: #606266;
}

.order-total {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.items-card,
.shipping-card,
.summary-card,
.payment-card {
  margin-bottom: 20px;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background-color: #fafafa;
  border-radius: 4px;
}

.item-image {
  width: 80px;
  height: 80px;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  background-color: #e4e7ed;
  border-radius: 4px;
}

.item-details {
  flex: 1;
}

.product-name {
  font-weight: bold;
  margin-bottom: 8px;
}

.item-meta {
  display: flex;
  gap: 20px;
  color: #909399;
}

.item-subtotal {
  color: #409EFF;
  font-weight: bold;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.summary-row.total {
  font-size: 20px;
  font-weight: bold;
  color: #409EFF;
  border-bottom: none;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 2px solid #409EFF;
}

.payment-status {
  margin-bottom: 16px;
  font-size: 16px;
}

.payment-history {
  margin-top: 20px;
}

.payment-record {
  padding: 12px;
  background-color: #fafafa;
  border-radius: 4px;
  margin-bottom: 12px;
}

.pay-btn,
.retry-btn {
  width: 100%;
  margin-top: 16px;
}

.payment-methods {
  margin-bottom: 20px;
}

.payment-method-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.payment-amount-display {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
  padding: 12px;
  background-color: #ecf5ff;
  border-radius: 4px;
}

.credit-card-form {
  margin-top: 20px;
}

.field-error {
  color: #F56C6C;
  font-size: 12px;
  margin-top: 4px;
}

.security-notice {
  margin-top: 16px;
  padding: 12px;
  background-color: #f0f9ff;
  border-radius: 4px;
  color: #409EFF;
  display: flex;
  align-items: center;
  gap: 8px;
}

.bank-transfer-info,
.paypal-info,
.cash-payment-info {
  padding: 20px;
  background-color: #fafafa;
  border-radius: 4px;
}

.cash-instructions {
  margin-bottom: 16px;
  font-size: 16px;
}
</style>
