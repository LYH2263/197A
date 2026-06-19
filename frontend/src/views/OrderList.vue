<template>
  <div class="order-list-page">
    <h2 class="page-title">我的订单</h2>
    <div v-loading="loading" class="order-content">
      <div
        v-for="order in orders"
        :key="order.id"
        class="order-card content-card"
      >
        <div class="order-card-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" size="small" class="order-tag">
            {{ statusText(order.status) }}
          </el-tag>
        </div>
        <p class="order-amount">合计：¥ {{ order.totalAmount }}</p>
        <p class="order-time">下单时间：{{ order.createdAt }}</p>
        <div class="order-actions">
          <el-button size="small" @click="$router.push(`/orders/${order.id}`)">查看详情</el-button>
          <template v-if="order.status >= 1 && order.status <= 3">
            <el-button type="primary" size="small" @click="$router.push(`/orders/${order.id}`)">
              去评价
            </el-button>
          </template>
          <template v-if="order.status === 0">
            <el-button type="primary" size="small" @click="pay(order.id)">去支付</el-button>
            <el-button size="small" @click="cancel(order.id)">取消订单</el-button>
          </template>
        </div>
      </div>
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </div>
    <el-dialog v-model="showCheckout" title="确认订单" width="500px" class="checkout-dialog" @close="checkoutForm = {}">
      <el-form :model="checkoutForm" label-width="100px">
        <el-form-item label="收货人" required>
          <el-input v-model="checkoutForm.receiverName" placeholder="收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="checkoutForm.receiverPhone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="收货地址" required>
          <el-input v-model="checkoutForm.receiverAddress" type="textarea" placeholder="详细地址" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCheckout = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const route = useRoute()
const loading = ref(true)
const orders = ref([])
const showCheckout = ref(false)
const checkoutForm = reactive({ receiverName: '', receiverPhone: '', receiverAddress: '' })
const submitting = ref(false)

const statusText = (s) => ({ 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消' })[s] || '未知'
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'info', 3: 'success', 4: 'info' })[s] || 'info'

async function load() {
  loading.value = true
  try {
    const res = await api.get('/orders')
    orders.value = res.data.code === 200 ? res.data.data || [] : []
  } finally {
    loading.value = false
  }
}

async function pay(orderId) {
  try {
    await api.post(`/orders/${orderId}/pay`)
    ElMessage.success('支付成功（模拟）')
    load()
  } catch (e) {}
}

async function cancel(orderId) {
  try {
    await api.post(`/orders/${orderId}/cancel`)
    ElMessage.success('已取消')
    load()
  } catch (e) {}
}

watch(
  () => route.query.checkout,
  (v) => {
    if (v === '1') showCheckout.value = true
  },
  { immediate: true }
)

async function submitOrder() {
  if (!checkoutForm.receiverName || !checkoutForm.receiverPhone || !checkoutForm.receiverAddress) {
    ElMessage.warning('请填写完整收货信息')
    return
  }
  submitting.value = true
  try {
    const res = await api.post('/orders', checkoutForm)
    if (res.data.code === 200) {
      ElMessage.success('订单创建成功')
      showCheckout.value = false
      Object.assign(checkoutForm, { receiverName: '', receiverPhone: '', receiverAddress: '' })
      load()
    }
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.order-list-page {
  padding-bottom: 32px;
}

.order-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  padding: 24px;
}

.order-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.order-no {
  font-weight: 600;
  color: var(--color-text);
}

.order-tag {
  border-radius: 6px;
}

.order-amount {
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
  font-size: 1rem;
}

.order-time {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-bottom: 16px;
}

.order-actions {
  display: flex;
  gap: 8px;
  padding-top: 4px;
}
</style>
