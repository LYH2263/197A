<template>
  <div v-loading="loading" class="admin-order-detail">
    <div class="detail-header">
      <el-button link @click="$router.back()">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回列表</span>
      </el-button>
      <h2 class="page-title">订单详情</h2>
    </div>

    <template v-if="order">
      <div class="main-card content-card">
        <div class="main-card-header">
          <span>订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" effect="light" size="small" class="status-tag">
            {{ statusText(order.status) }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border class="info-table">
          <el-descriptions-item label="订单金额">¥ {{ order.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ order.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="支付时间" v-if="order.paidAt">{{ order.paidAt }}</el-descriptions-item>
          <el-descriptions-item label="发货时间" v-if="order.shippedAt">{{ order.shippedAt }}</el-descriptions-item>
          <el-descriptions-item label="完成时间" v-if="order.completedAt">{{ order.completedAt }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">
          <span class="divider-title">收货信息</span>
        </el-divider>
        <el-descriptions :column="1" border class="info-table">
          <el-descriptions-item label="收货人">
            {{ order.receiverName }} &nbsp;·&nbsp; {{ order.receiverPhone }}
          </el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ order.receiverAddress }}</el-descriptions-item>
        </el-descriptions>

        <template v-if="order.status >= 2 && order.logisticsCompany">
          <el-divider content-position="left">
            <span class="divider-title">物流信息</span>
          </el-divider>
          <el-descriptions :column="1" border class="info-table logistics-table">
            <el-descriptions-item label="物流公司">{{ order.logisticsCompany }}</el-descriptions-item>
            <el-descriptions-item label="运单号">
              {{ order.trackingNo }}
              <el-button link type="primary" size="small" @click="copyTracking(order.trackingNo)" style="margin-left:8px">复制</el-button>
            </el-descriptions-item>
            <el-descriptions-item label="发货备注">{{ order.shippingRemark || '无' }}</el-descriptions-item>
          </el-descriptions>
        </template>

        <el-divider content-position="left">
          <span class="divider-title">订单时间线</span>
        </el-divider>
        <el-timeline class="order-timeline">
          <el-timeline-item
            v-for="(item, index) in timeline"
            :key="index"
            :timestamp="item.time"
            :type="item.type"
            :hollow="item.hollow"
          >
            <div class="timeline-item-label">{{ item.label }}</div>
            <div class="timeline-item-time">{{ item.time }}</div>
          </el-timeline-item>
        </el-timeline>

        <el-divider content-position="left">
          <span class="divider-title">商品明细</span>
        </el-divider>
        <el-table :data="items" style="width: 100%">
          <el-table-column prop="productName" label="商品" />
          <el-table-column label="图片" width="120">
            <template #default="{ row }">
              <el-image
                v-if="row.productImage"
                :src="row.productImage"
                :preview-src-list="[row.productImage]"
                fit="cover"
                style="width: 80px; height: 80px; border-radius: 8px"
              />
              <span v-else style="color:var(--color-text-muted)">无图</span>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="单价" width="120">
            <template #default="{ row }">¥ {{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="totalAmount" label="小计" width="120">
            <template #default="{ row }">¥ {{ row.totalAmount }}</template>
          </el-table-column>
        </el-table>

        <div class="actions">
          <template v-if="order.status === 0">
            <el-button type="warning" disabled>待付款</el-button>
          </template>

          <template v-if="order.status === 1">
            <el-button type="primary" @click="openShip">去发货</el-button>
          </template>

          <template v-if="order.status === 2">
            <el-button type="success" disabled>已发货</el-button>
          </template>

          <template v-if="order.status === 3">
            <el-button type="success" disabled>已完成</el-button>
          </template>

          <template v-if="order.status === 4">
            <el-button type="info" disabled>已取消</el-button>
          </template>
        </div>
      </div>

      <el-dialog
        v-model="shipVisible"
        title="订单发货"
        width="560px"
        destroy-on-close
        @close="resetShipForm"
      >
        <el-form
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
          <el-button type="primary" :loading="shipSubmitting" @click="submitShip">确认发货</el-button>
        </template>
      </el-dialog>
    </template>
    <el-empty v-else-if="!loading" description="订单不存在" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import api from '../api'

const route = useRoute()
const loading = ref(true)
const order = ref(null)
const items = ref([])
const shipVisible = ref(false)
const shipSubmitting = ref(false)
const shipFormRef = ref(null)

const orderId = computed(() => Number(route.params.id))

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

const timeline = computed(() => {
  if (!order.value) return []
  const list = []
  if (order.value.createdAt) {
    list.push({ label: '下单', time: order.value.createdAt, type: 'primary' })
  }
  if (order.value.paidAt) {
    list.push({ label: '支付', time: order.value.paidAt, type: 'primary' })
  }
  if (order.value.shippedAt) {
    list.push({ label: '发货', time: order.value.shippedAt, type: '' })
  }
  if (order.value.completedAt) {
    list.push({ label: '确认收货', time: order.value.completedAt, type: 'success' })
  }
  if (order.value.status === 4) {
    list.push({ label: '已取消', time: order.value.updatedAt || '-', type: 'info', hollow: true })
  }
  return list
})

function copyTracking(no) {
  if (!no) return
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(no).then(() => ElMessage.success('运单号已复制'))
  } else {
    const ta = document.createElement('textarea')
    ta.value = no
    document.body.appendChild(ta)
    ta.select()
    try { document.execCommand('copy'); ElMessage.success('运单号已复制') } catch {}
    document.body.removeChild(ta)
  }
}

function openShip() {
  shipVisible.value = true
}

function resetShipForm() {
  shipForm.logisticsCompany = ''
  shipForm.trackingNo = ''
  shipForm.shippingRemark = ''
  shipFormRef.value?.clearValidate?.()
}

async function submitShip() {
  if (!shipForm.logisticsCompany || !shipForm.trackingNo) {
    ElMessage.warning('请填写物流公司和运单号')
    return
  }
  shipSubmitting.value = true
  try {
    await api.post('/admin/orders/ship', {
      orderId: orderId.value,
      logisticsCompany: shipForm.logisticsCompany,
      trackingNo: shipForm.trackingNo,
      shippingRemark: shipForm.shippingRemark,
    })
    ElMessage.success('发货成功')
    shipVisible.value = false
    load()
  } finally {
    shipSubmitting.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const [oRes, iRes] = await Promise.all([
      api.get(`/admin/orders/${orderId.value}`),
      api.get(`/admin/orders/${orderId.value}/items`),
    ])
    if (oRes.data.code === 200) order.value = oRes.data.data
    if (iRes.data.code === 200) items.value = iRes.data.data || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-order-detail {
  padding-bottom: 32px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-text);
}

.main-card {
  max-width: 960px;
  padding: 28px;
  margin: 0 auto;
}

.main-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
  font-weight: 600;
  color: var(--color-text);
}

.status-tag {
  border-radius: 6px;
  font-weight: 500;
}

.divider-title {
  font-weight: 600;
  color: var(--color-text);
  font-size: 0.9375rem;
}

.info-table {
  margin-bottom: 4px;
}

.info-table :deep(.el-descriptions__label) {
  width: 120px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.info-table :deep(.el-descriptions__content) {
  color: var(--color-text);
}

.logistics-table {
  margin-bottom: 12px;
}

.order-timeline {
  padding: 0 8px 8px 8px;
}

.timeline-item-label {
  font-weight: 600;
  color: var(--color-text);
  font-size: 0.9375rem;
}

.timeline-item-time {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
</style>
