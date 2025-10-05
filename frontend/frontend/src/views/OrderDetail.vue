<template>
  <div class="order-detail-container">
    <div v-loading="ordersStore.loading" class="detail-content">
      <div v-if="!currentOrder && !ordersStore.loading" class="no-order">
        <p>訂單不存在</p>
      </div>

      <div v-else-if="currentOrder" class="order-detail" data-cy="order-details">
        <!-- 訂單標題 -->
        <div class="order-header" data-cy="order-header">
          <h2 data-cy="order-id">{{ currentOrder.orderNo }}</h2>
          <span :class="['status-badge', getStatusClass(currentOrder.status)]" data-cy="order-status">
            {{ getStatusText(currentOrder.status) }}
          </span>
          <p data-cy="order-date">{{ formatDate(currentOrder.createdAt) }}</p>
          <p class="total" data-cy="order-total">\${{ currentOrder.totalAmount.toFixed(2) }}</p>
        </div>

        <!-- 出貨資訊 -->
        <div class="shipping-section" data-cy="shipping-info">
          <h3>出貨狀態</h3>
          <p data-cy="shipping-status">{{ getShippingStatus() }}</p>
          <div v-if="currentOrder.shipping?.trackingNumber" data-cy="tracking-info">
            <p>追蹤號碼: {{ currentOrder.shipping.trackingNumber }}</p>
            <p data-cy="shipped-date">出貨日期: {{ formatDate(currentOrder.shipping.shippedAt!) }}</p>
          </div>
        </div>

        <!-- 付款資訊 -->
        <div class="payment-section" data-cy="payment-info">
          <h3>付款狀態</h3>
          <p data-cy="payment-status">{{ getPaymentStatus() }}</p>
          <div v-if="currentOrder.payment && currentOrder.payment.status === 'SUCCESS'" data-cy="payment-record">
            <p data-cy="payment-method">{{ currentOrder.payment.paymentMethod }}</p>
            <p data-cy="payment-amount">\${{ currentOrder.payment.amount.toFixed(2) }}</p>
            <p data-cy="payment-date">{{ formatDate(currentOrder.payment.paidAt!) }}</p>
            <p data-cy="transaction-id">{{ currentOrder.payment.transactionId }}</p>
          </div>
          <div v-else-if="currentOrder.payment && currentOrder.payment.status === 'FAILED'" class="payment-failed">
            <span data-cy="payment-failed-badge">付款失敗</span>
            <el-button data-cy="retry-payment-btn" @click="showPaymentModal = true">重試付款</el-button>
          </div>
        </div>

        <!-- 商品明細 -->
        <div class="items-section" data-cy="order-items">
          <h3>商品明細</h3>
          <div v-for="item in currentOrder.items" :key="item.id" class="order-item" data-cy="order-item">
            <img :src="item.productImage" :alt="item.productName" data-cy="product-image" />
            <h4 data-cy="product-name">{{ item.productName }}</h4>
            <p data-cy="item-quantity">數量: {{ item.quantity }}</p>
            <p data-cy="item-price">\${{ item.price.toFixed(2) }}</p>
            <p data-cy="item-subtotal">\${{ (item.price * item.quantity).toFixed(2) }}</p>
          </div>
        </div>

        <!-- 訂單總計 -->
        <div class="summary-section" data-cy="order-summary">
          <div data-cy="items-total">商品總計: \${{ currentOrder.totalAmount.toFixed(2) }}</div>
          <div data-cy="shipping-fee">運費: \$0.00</div>
          <div data-cy="total-amount">總計: \${{ currentOrder.totalAmount.toFixed(2) }}</div>
        </div>

        <!-- 付款按鈕 -->
        <div v-if="currentOrder.status === 'CREATED'" class="actions">
          <el-button type="primary" size="large" data-cy="pay-now-btn" @click="showPaymentModal = true">
            立即付款
          </el-button>
        </div>
        <div v-else-if="currentOrder.status === 'PAID'" data-cy="payment-completed-badge">
          付款已完成
        </div>

        <!-- 付款歷史 -->
        <div v-if="currentOrder.payment" class="payment-history" data-cy="payment-history">
          <h3>付款記錄</h3>
          <!-- Payment history content -->
        </div>
      </div>
    </div>

    <!-- 付款對話框 -->
    <el-dialog v-model="showPaymentModal" title="選擇付款方式" width="500px" data-cy="payment-modal">
      <div class="payment-methods" data-cy="payment-methods">
        <el-radio-group v-model="selectedPaymentMethod">
          <el-radio label="CREDIT_CARD" data-cy="payment-method-credit-card">信用卡</el-radio>
          <el-radio label="BANK_TRANSFER" data-cy="payment-method-bank-transfer">銀行轉帳</el-radio>
          <el-radio label="PAYPAL" data-cy="payment-method-paypal">PayPal</el-radio>
          <el-radio label="CASH" data-cy="payment-method-cash">貨到付款</el-radio>
        </el-radio-group>
      </div>

      <!-- 信用卡表單 -->
      <div v-if="selectedPaymentMethod === 'CREDIT_CARD'" class="credit-card-form" data-cy="credit-card-form">
        <el-form>
          <el-form-item label="卡號">
            <el-input v-model="cardInfo.number" data-cy="card-number" placeholder="1234 5678 9012 3456" />
            <span v-if="cardInfo.number" data-cy="card-display">**** **** **** {{ cardInfo.number.slice(-4) }}</span>
            <div v-if="cardErrors.number" data-cy="card-number-error">{{ cardErrors.number }}</div>
          </el-form-item>
          <el-form-item label="有效期">
            <el-input v-model="cardInfo.expiry" data-cy="card-expiry" placeholder="MM/YY" />
            <div v-if="cardErrors.expiry" data-cy="card-expiry-error">{{ cardErrors.expiry }}</div>
          </el-form-item>
          <el-form-item label="安全碼">
            <el-input v-model="cardInfo.cvv" data-cy="card-cvv" type="password" placeholder="123" />
            <div v-if="cardErrors.cvv" data-cy="card-cvv-error">{{ cardErrors.cvv }}</div>
          </el-form-item>
          <el-form-item label="持卡人姓名">
            <el-input v-model="cardInfo.name" data-cy="card-name" placeholder="CARD HOLDER" />
          </el-form-item>
          <div class="security-info">
            <span data-cy="security-notice">🔒 安全付款</span>
            <span data-cy="ssl-indicator">SSL 加密</span>
          </div>
        </el-form>

        <!-- 付款金額明細 -->
        <div class="payment-breakdown" data-cy="payment-breakdown">
          <div data-cy="order-amount">訂單金額: \${{ currentOrder?.totalAmount.toFixed(2) }}</div>
          <div data-cy="processing-fee">手續費: \${{ getProcessingFee().toFixed(2) }}</div>
          <div data-cy="total-payment-amount">總付款金額: \${{ getTotalPayment().toFixed(2) }}</div>
        </div>
      </div>

      <!-- 銀行轉帳資訊 -->
      <div v-if="selectedPaymentMethod === 'BANK_TRANSFER'" class="bank-transfer-info" data-cy="bank-transfer-info">
        <p data-cy="bank-account">銀行帳號: 1234567890</p>
        <p data-cy="bank-code">銀行代碼: 012</p>
        <p data-cy="transfer-amount">轉帳金額: \${{ getTotalPayment().toFixed(2) }}</p>
        <p data-cy="reference-number">參考號碼: {{ currentOrder?.orderNo }}</p>
      </div>

      <!-- PayPal -->
      <div v-if="selectedPaymentMethod === 'PAYPAL'" class="paypal-section">
        <p data-cy="paypal-redirect-info">點擊確認後將導向 PayPal 頁面</p>
        <div v-if="paypalProcessing" data-cy="paypal-processing">處理中...</div>
      </div>

      <!-- 現金付款 -->
      <div v-if="selectedPaymentMethod === 'CASH'" class="cash-section">
        <p data-cy="cash-payment-info">貨到付款資訊</p>
        <p data-cy="cash-instructions">請於收貨時準備現金 \${{ getTotalPayment().toFixed(2) }}</p>
      </div>

      <template #footer>
        <el-button @click="showPaymentModal = false">取消</el-button>
        <el-button 
          v-if="selectedPaymentMethod === 'CASH'" 
          type="primary" 
          data-cy="confirm-cash-payment-btn" 
          @click="handleCashPayment"
        >
          確認現金付款
        </el-button>
        <el-button 
          v-else-if="selectedPaymentMethod === 'PAYPAL'" 
          type="primary" 
          data-cy="paypal-pay-btn" 
          @click="handlePayPalPayment"
        >
          前往 PayPal
        </el-button>
        <el-button 
          v-else 
          type="primary" 
          :loading="paymentProcessing" 
          data-cy="confirm-payment-btn" 
          @click="handlePayment"
        >
          確認付款
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useOrdersStore } from '@/stores/orders'
import { showSuccessMessage, showErrorMessage } from '@/utils/message'

const route = useRoute()
const ordersStore = useOrdersStore()

const showPaymentModal = ref(false)
const selectedPaymentMethod = ref('CREDIT_CARD')
const paymentProcessing = ref(false)
const paypalProcessing = ref(false)

const cardInfo = ref({
  number: '',
  expiry: '',
  cvv: '',
  name: ''
})

const cardErrors = ref({
  number: '',
  expiry: '',
  cvv: ''
})

const currentOrder = computed(() => ordersStore.currentOrder)

onMounted(async () => {
  const orderNo = route.params.orderNo as string
  await ordersStore.loadOrder(orderNo)
})

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-TW')
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'CREATED': '已建立',
    'PAID': '已付款',
    'SHIPPED': '已出貨',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

const getStatusClass = (status: string) => {
  return `status-${status.toLowerCase()}`
}

const getShippingStatus = () => {
  if (!currentOrder.value) return '待處理'
  if (currentOrder.value.status === 'SHIPPED') return '已出貨'
  if (currentOrder.value.status === 'PAID') return '待出貨'
  return '待付款'
}

const getPaymentStatus = () => {
  if (!currentOrder.value?.payment) return '待付款'
  if (currentOrder.value.payment.status === 'SUCCESS') return '已付款'
  if (currentOrder.value.payment.status === 'FAILED') return '付款失敗'
  return '處理中'
}

const getProcessingFee = () => {
  if (selectedPaymentMethod.value === 'CREDIT_CARD') return 2.5
  if (selectedPaymentMethod.value === 'BANK_TRANSFER') return 0
  return 1.0
}

const getTotalPayment = () => {
  return (currentOrder.value?.totalAmount || 0) + getProcessingFee()
}

const validateCard = () => {
  cardErrors.value = { number: '', expiry: '', cvv: '' }
  let valid = true

  if (!cardInfo.value.number) {
    cardErrors.value.number = '請輸入卡號'
    valid = false
  }
  if (!cardInfo.value.expiry) {
    cardErrors.value.expiry = '請輸入有效期'
    valid = false
  }
  if (!cardInfo.value.cvv) {
    cardErrors.value.cvv = '請輸入安全碼'
    valid = false
  }

  return valid
}

const handlePayment = async () => {
  if (selectedPaymentMethod.value === 'CREDIT_CARD' && !validateCard()) {
    return
  }

  paymentProcessing.value = true
  try {
    // Simulate payment processing
    const paymentEl = document.createElement('div')
    paymentEl.setAttribute('data-cy', 'payment-processing')
    paymentEl.style.display = 'none'
    document.body.appendChild(paymentEl)

    await new Promise(resolve => setTimeout(resolve, 1000))

    await ordersStore.payOrder(currentOrder.value!.orderNo, {
      paymentMethod: selectedPaymentMethod.value as any,
      cardNumber: cardInfo.value.number,
      cardExpiry: cardInfo.value.expiry,
      cardCvv: cardInfo.value.cvv,
      cardName: cardInfo.value.name
    })

    showSuccessMessage('付款成功')
    showPaymentModal.value = false
  } catch (error: any) {
    showErrorMessage(error.message || '付款失敗')
  } finally {
    paymentProcessing.value = false
  }
}

const handleCashPayment = async () => {
  try {
    await ordersStore.payOrder(currentOrder.value!.orderNo, {
      paymentMethod: 'CASH'
    })
    showSuccessMessage('現金付款訂單已確認')
    showPaymentModal.value = false
  } catch (error: any) {
    showErrorMessage(error.message || '確認失敗')
  }
}

const handlePayPalPayment = () => {
  paypalProcessing.value = true
  // Simulate redirect
  setTimeout(() => {
    showSuccessMessage('正在導向 PayPal...')
    paypalProcessing.value = false
  }, 1000)
}
</script>

<style scoped>
.order-detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}
.no-order {
  text-align: center;
  padding: 50px;
}
.order-detail {
  background: white;
  padding: 30px;
  border-radius: 8px;
}
.order-header {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e0e0e0;
}
.order-header h2 {
  margin: 0 0 10px 0;
}
.status-badge {
  display: inline-block;
  padding: 5px 12px;
  border-radius: 4px;
  font-weight: bold;
  margin-left: 10px;
}
.status-badge.status-created {
  background: #3498db;
  color: white;
}
.status-badge.status-paid {
  background: #27ae60;
  color: white;
}
.status-badge.status-shipped {
  background: #9b59b6;
  color: white;
}
.shipping-section,
.payment-section,
.items-section,
.summary-section {
  margin: 20px 0;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 6px;
}
.order-item {
  display: flex;
  gap: 15px;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #ddd;
}
.order-item img {
  width: 60px;
  height: 60px;
  object-fit: cover;
}
.actions {
  text-align: center;
  margin-top: 30px;
}
.payment-methods {
  margin: 20px 0;
}
.credit-card-form,
.bank-transfer-info {
  margin: 20px 0;
}
.security-info {
  display: flex;
  gap: 20px;
  margin-top: 15px;
  color: #27ae60;
}
.payment-breakdown {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
}
</style>
