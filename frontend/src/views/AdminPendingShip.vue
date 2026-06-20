<template>
  <div class="admin-pending-ship">
    <h2 class="page-title">待发货订单</h2>
    <el-alert type="info" show-icon style="margin-bottom: 20px" class="intro-alert">
      <template #title>
        当前显示所有「已付款」状态订单，点击「发货」填写物流信息后订单流转为「已发货」。
      </template>
    </el-alert>

    <div v-loading="loading" class="order-content">
      <div
        v-for="order in orders"
        :key="order.id"
        class="order-card content-card"
      >
        <div class="order-card-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" effect="light" size="small" class="order-tag">
            {{ statusText(order.status) }}
          </el-tag>
        </div>
        <div class="order-info-grid">
          <p><strong>下单时间：</strong>{{ order.createdAt }}</p>
          <p><strong>订单金额：</strong>¥ {{ order.totalAmount }}</p>
          <p><strong>收货人：</strong>{{ order.receiverName }}</p>
          <p><strong>联系电话：</strong>{{ order.receiverPhone }}</p>
          <p class="full-width"><strong>收货地址：</strong>{{ order.receiverAddress }}</p>
        </div>
        <div class="order-actions">
          <el-button size="small" @click="viewOrder(order)">查看详情</el-button>
          <el-button type="primary" size="small" @click="openShip(order)">发货</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && orders.length === 0" description="暂无待发货订单" />
    </div>

    <el-dialog
      v-model="shipVisible"
      :title="`发货 - 订单号：${currentOrder?.orderNo}`"
      width="560px"
      destroy-on-close
      @close="resetForm"
    >
      <el-form
      v-if="currentOrder"
        :model="shipForm"
        :rules="shipRules"
        ref="shipFormRef"
        label-width="100px"
      >
        <el-form-item label="物流公司" prop="logisticsCompany">
          <el-select
            v-model="shipForm.logisticsCompany"
            placeholder="选择常用物流公司"
            style="width: 100%"
            filterable
            allow-create
          >
            <el-option v-for="c in commonCompanies" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号" prop="trackingNo">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入运单号" maxlength="64" />
        </el-form-item>
        <el-form-item label="发货备注" prop="shippingRemark">
          <el-input
            v-model="shipForm.shippingRemark"
            type="textarea"
            :rows="3"
            maxlength="256"
            placeholder="选填，用户可见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const loading = ref(true)
const orders = ref([])
const shipVisible = ref(false)
const currentOrder = ref(null)
const submitting = ref(false)
const shipFormRef = ref(null)

const commonCompanies = [
  '顺丰速运',
  '京东物流',
  '中通快递',
  '圆通速递',
  '韵达速递',
  '申通快递',
  '百世快递',
  '邮政EMS',
  '德邦快递',
  '极兔速递',
]

const shipForm = reactive({
  logisticsCompany: '',
  trackingNo: '',
  shippingRemark: '',
})

const shipRules = {
  logisticsCompany: [
    { required: true, message: '请选择或填写物流公司', trigger: 'change' },
  ],
  trackingNo: [
    { required: true, message: '请输入运单号', trigger: 'blur' },
  ],
}

const STATUS_TEXT = { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消' }
const STATUS_TYPE = { 0: 'warning', 1: 'primary', 2: '', 3: 'success', 4: 'info' }

function statusText(s) { return STATUS_TEXT[s] ?? '未知' }
function statusType(s) { return STATUS_TYPE[s] ?? 'info' }

function viewOrder(order) {
  router.push(`/admin/orders/${order.id}`)
}

function openShip(order) {
  currentOrder.value = order
  shipVisible.value = true
}

function resetForm() {
  shipForm.logisticsCompany = ''
  shipForm.trackingNo = ''
  shipForm.shippingRemark = ''
  shipFormRef.value?.clearValidate?.()
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/orders/pending-ship')
    orders.value = res.data.code === 200 ? res.data.data || [] : []
  } finally {
    loading.value = false
  }
}

async function submitShip() {
  if (!currentOrder.value) return
  if (!shipForm.logisticsCompany || !shipForm.trackingNo) {
    ElMessage.warning('请填写物流公司和运单号')
    return
  }
  submitting.value = true
  try {
    await api.post('/admin/orders/ship', {
      orderId: currentOrder.value.id,
      logisticsCompany: shipForm.logisticsCompany,
      trackingNo: shipForm.trackingNo,
      shippingRemark: shipForm.shippingRemark,
    })
    ElMessage.success('发货成功')
    shipVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-pending-ship {
  padding-bottom: 32px;
}

.intro-alert {
  max-width: 1200px;
}

.order-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  padding: 20px 24px;
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
  font-weight: 500;
}

.order-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 4px 24px;
  margin-bottom: 12px;
}

.order-info-grid p {
  margin: 4px 0;
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.order-info-grid strong {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.order-info-grid p.full-width {
  grid-column: 1 / -1;
}

.order-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px dashed var(--color-border);
}
</style>
