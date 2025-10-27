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
        <div class="header-actions">
          <el-tag :type="getStatusType(order.status)" data-cy="order-status" size="large">
            {{ getStatusLabel(order.status) }}
          </el-tag>
          <el-button
            v-if="canCancelOrder"
            type="danger"
            size="default"
            data-cy="cancel-order-btn"
            @click="showCancelConfirmation"
          >
            取消訂單
          </el-button>
        </div>
      </div>
      <div class="order-meta">
        <div class="order-date" data-cy="order-date">
          下單時間: {{ formatDateTime(order.createdAt) }}
        </div>
        <div class="order-total" data-cy="order-total">
          訂單金額: ${{ order.totalAmount?.toFixed(0) }}
        </div>
      </div>
    </el-card>

    <!-- 訂單詳情 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <OrderItems :items="order.items" />
        <OrderEvents :events="events" />
      </el-col>

      <el-col :span="8">
        <OrderSummary :total-amount="order.totalAmount" />
        <PaymentInfo
          :payments="order.payments"
          :order-status="order.status"
          :show-payment-button="true"
          @pay="showPaymentModal"
        />
        <ShippingInfo :order-status="order.status" :updated-at="order.updatedAt" />
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
        付款金額: ${{ order.totalAmount?.toFixed(0) }}
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
            <div v-if="creditCardForm.cardNumber" class="card-display" data-cy="card-display">
              {{ maskedCardNumber }}
            </div>
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
        <p data-cy="transfer-amount">轉帳金額: ${{ order.totalAmount?.toFixed(0) }}</p>
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
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import ordersApi from '@/api/orders'
import type { Order, Payment, OrderEvent } from '@/types'
import OrderItems from '@/components/order/OrderItems.vue'
import OrderEvents from '@/components/order/OrderEvents.vue'
import OrderSummary from '@/components/order/OrderSummary.vue'
import PaymentInfo from '@/components/order/PaymentInfo.vue'
import ShippingInfo from '@/components/order/ShippingInfo.vue'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const events = ref<OrderEvent[]>([])
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

onMounted(async () => {
  await loadOrder()
})

async function loadOrder() {
  try {
    const orderId = Number(route.params.id)
    order.value = await ordersApi.getOrder(orderId)
    await loadOrderEvents(orderId)
  } catch (error) {
    console.error('Failed to load order:', error)
    ElMessage.error('載入訂單失敗')
  }
}

async function loadOrderEvents(orderId: number) {
  try {
    events.value = await ordersApi.getOrderEvents(orderId)
  } catch (error) {
    console.error('Failed to load order events:', error)
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
      orderId: order.value.id!,
      paymentMethod: selectedPaymentMethod.value as any,
      amount: order.value.totalAmount,
      // Include credit card info for payment processing
      ...(selectedPaymentMethod.value === 'CREDIT_CARD' && {
        cardExpiry: creditCardForm.value.cardExpiry,
        cardCvv: creditCardForm.value.cardCvv,
        cardName: creditCardForm.value.cardName
      })
    }

    const response = await ordersApi.payOrder(order.value.id!, paymentData)

    // Reload order to get updated status
    await loadOrder()

    // 根據付款狀態顯示訊息
    if (response.status === 'SUCCESS') {
      ElMessage.success('付款成功')
      successMessage.value = '付款成功'
      paymentModalVisible.value = false
    } else if (response.status === 'PENDING') {
      // 銀行轉帳：等待確認
      ElMessage.info('付款資訊已提交，等待確認收款')
      successMessage.value = '付款資訊已提交，等待管理員確認收款'
      paymentModalVisible.value = false
    } else if (response.status === 'FAILED') {
      // 付款失敗時關閉 modal 並顯示錯誤訊息
      paymentModalVisible.value = false
      errorMessage.value = '付款失敗'
      errorDetails.value = response.failureReason || '此訂單付款已失敗，無法重新付款'
    }
  } catch (error: any) {
    console.error('Payment failed:', error)

    // 關閉付款 modal
    paymentModalVisible.value = false

    // 重新載入訂單以更新付款狀態
    await loadOrder()

    // 顯示錯誤訊息
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

function getEventTypeLabel(eventType: string): string {
  const labels: Record<string, string> = {
    'CREATED': '訂單建立',
    'PAID': '付款完成',
    'APPROVED': '訂單批准',
    'SHIPPED': '訂單出貨',
    'DELIVERED': '訂單送達',
    'CANCELLED': '訂單取消',
    'REFUNDED': '訂單退款'
  }
  return labels[eventType] || eventType
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

const maskedCardNumber = computed(() => {
  const cardNumber = creditCardForm.value.cardNumber
  if (!cardNumber) return ''
  if (cardNumber.length < 4) return cardNumber

  // 只顯示最後 4 碼，其餘用 * 遮蔽
  const lastFour = cardNumber.slice(-4)
  const masked = '**** **** **** ' + lastFour
  return masked
})

// 判斷是否可以取消訂單（只有 CREATED 或 PAID 狀態可以取消）
const canCancelOrder = computed(() => {
  if (!order.value) return false
  return order.value.status === 'CREATED' || order.value.status === 'PAID'
})

// 顯示取消訂單確認對話框
async function showCancelConfirmation() {
  try {
    await ElMessageBox.confirm(
      '確定要取消此訂單嗎？取消後庫存將會恢復，若已付款則會自動退款。',
      '取消訂單',
      {
        confirmButtonText: '確認取消',
        cancelButtonText: '返回',
        type: 'warning',
      }
    )
    await cancelOrder()
  } catch {
    // 用戶點擊取消或關閉對話框
  }
}

// 取消訂單
async function cancelOrder() {
  if (!order.value?.id) return

  isProcessing.value = true
  errorMessage.value = ''

  try {
    await ordersApi.cancelOrder(order.value.id)
    ElMessage.success('訂單已取消')

    // 重新載入訂單以更新狀態
    await loadOrder()
  } catch (error: any) {
    console.error('Failed to cancel order:', error)
    if (error.response?.data?.message) {
      errorMessage.value = '取消訂單失敗'
      errorDetails.value = error.response.data.message
    } else {
      errorMessage.value = '取消訂單失敗，請稍後再試'
    }
  } finally {
    isProcessing.value = false
  }
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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

.card-display {
  margin-top: 8px;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 14px;
  color: #606266;
  letter-spacing: 2px;
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

.events-card {
  margin-top: 20px;
}

.event-type {
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 4px;
}

.event-message {
  color: #666;
  font-size: 14px;
}

.event-user {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
  font-style: italic;
}

.no-events {
  text-align: center;
  color: #999;
  padding: 20px;
}
</style>
