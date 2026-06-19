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
          <el-tag :type="statusType(order.status)" effect="light" size="small" class="order-tag">
            {{ statusText(order.status) }}
          </el-tag>
        </div>
        <p class="order-amount"><strong>合计：</strong>¥ {{ order.totalAmount }}</p>
        <p class="order-receiver">
          <strong>收货人：</strong>{{ order.receiverName }} · {{ order.receiverPhone }} · {{ order.receiverAddress }}
        </p>
        <template v-if="order.status >= 2 && order.logisticsCompany">
          <div class="order-logistics content-card-sub">
            <p><strong>物流公司：</strong>{{ order.logisticsCompany }}</p>
            <p><strong>运单号：</strong>{{ order.trackingNo }}
              <el-button link type="primary" size="small" @click="copyTracking(order.trackingNo)" style="margin-left:8px">复制</el-button>
            </p>
            <p v-if="order.shippingRemark"><strong>发货备注：</strong>{{ order.shippingRemark }}</p>
            <p v-if="order.shippedAt"><strong>发货时间：</strong>{{ order.shippedAt }}</p>
          </div>
        </template>
        <p class="order-time"><strong>下单时间：</strong>{{ order.createdAt }}</p>
        <p v-if="order.status === 3 && order.completedAt" class="order-time">
          <strong>完成时间：</strong>{{ order.completedAt }}
        </p>
        <div class="order-actions">
          <el-button size="small" @click="$router.push(`/orders/${order.id}`)">查看详情</el-button>

          <template v-if="order.status === 0">
            <el-button type="primary" size="small" @click="pay(order.id)">去支付</el-button>
            <el-button size="small" @click="cancel(order.id)">取消订单</el-button>
          </template>

          <template v-if="order.status === 1">
            <el-button size="small" disabled>等待发货</el-button>
          </template>

          <template v-if="order.status === 2">
            <el-button type="primary" size="small" @click="viewLogistics(order)">查看物流</el-button>
            <el-button type="success" size="small" @click="receive(order.id)">确认收货</el-button>
          </template>

          <template v-if="order.status === 3">
            <el-button
              v-if="canReview(order.status)"
              type="primary"
              size="small"
              @click="$router.push(`/orders/${order.id}`)"
            >
              去评价
            </el-button>
            <el-button
              v-if="showAfterSale(order)"
              type="warning"
              size="small"
              @click="applyAfterSale(order, 'LIST')"
            >
              申请售后
            </el-button>
          </template>
        </div>
      </div>
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </div>

    <el-dialog
      v-model="showCheckout"
      title="确认订单"
      width="680px"
      class="checkout-dialog"
      destroy-on-close
      @close="onCheckoutClose"
    >
      <div class="checkout-body">
        <div class="checkout-section">
          <div class="section-header">
            <h3>收货地址</h3>
            <el-button link type="primary" @click="showAddrPicker = true">
              <el-icon><Location /></el-icon>
              <span>选择地址</span>
            </el-button>
          </div>
          <div
            v-if="selectedAddress"
            class="selected-address content-card"
            @click="showAddrPicker = true"
          >
            <div class="addr-tags">
              <el-tag v-if="selectedAddress.isDefault === 1" type="danger" size="small" effect="dark">默认</el-tag>
              <el-tag v-if="selectedAddress.tag" size="small" effect="plain">{{ selectedAddress.tag }}</el-tag>
            </div>
            <p class="addr-receiver">
              <strong>{{ selectedAddress.receiverName }}</strong>
              <span class="addr-phone">{{ selectedAddress.receiverPhone }}</span>
            </p>
            <p class="addr-detail">
              {{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.detailAddress }}
            </p>
          </div>
          <div v-else class="selected-address empty-addr content-card" @click="showAddrPicker = true">
            <el-icon class="empty-addr-icon"><Location /></el-icon>
            <span>请选择收货地址</span>
          </div>
        </div>

        <el-divider />

        <div class="checkout-section">
          <div class="section-header">
            <h3>或手动填写地址</h3>
            <span class="hint">（未选地址时生效）</span>
          </div>
          <el-form :model="manualForm" label-width="100px" size="default">
            <el-form-item label="收货人">
              <el-input
                v-model="manualForm.receiverName"
                placeholder="收货人姓名"
                maxlength="32"
              />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input
                v-model="manualForm.receiverPhone"
                placeholder="11位手机号"
                maxlength="11"
              />
            </el-form-item>
            <el-form-item label="所在地区">
              <el-cascader
                v-model="manualForm.region"
                :options="regionData"
                :props="{ expandTrigger: 'hover' }"
                placeholder="请选择省/市/区"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="详细地址">
              <el-input
                v-model="manualForm.detailAddress"
                type="textarea"
                :rows="2"
                placeholder="街道、门牌号等"
                maxlength="200"
              />
            </el-form-item>
            <el-form-item label="标签">
              <el-radio-group v-model="manualForm.tag">
                <el-radio-button
                  v-for="t in tagOptions"
                  :key="t.value"
                  :value="t.value"
                >{{ t.label }}</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="保存地址">
              <el-switch
                v-model="manualForm.saveToAddressBook"
                active-text="保存到地址簿"
                inactive-text="仅本单使用"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>
      <template #footer>
        <el-button @click="showCheckout = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">
          提交订单
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showAddrPicker"
      title="选择收货地址"
      width="620px"
      class="addr-picker-dialog"
      destroy-on-close
    >
      <div v-loading="addrLoading" class="addr-picker-body">
        <div v-if="addresses.length" class="addr-list">
          <div
            v-for="addr in addresses"
            :key="addr.id"
            class="addr-item"
            :class="{ active: (pendingPickAddress || selectedAddress)?.id === addr.id }"
            @click="pickAddress(addr)"
          >
            <div class="addr-item-tags">
              <el-tag v-if="addr.isDefault === 1" type="danger" size="small" effect="dark">默认</el-tag>
              <el-tag v-if="addr.tag" size="small" effect="plain">{{ addr.tag }}</el-tag>
            </div>
            <p class="addr-item-receiver">
              <strong>{{ addr.receiverName }}</strong>
              <span class="addr-item-phone">{{ addr.receiverPhone }}</span>
            </p>
            <p class="addr-item-detail">
              {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
            </p>
            <div v-if="(pendingPickAddress || selectedAddress)?.id === addr.id" class="addr-check">
              <el-icon><Check /></el-icon>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无地址" />
        <div class="addr-picker-footer">
          <el-button type="primary" link @click="goToAddressPage">
            <el-icon><Plus /></el-icon>
            <span>管理所有地址</span>
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="showAddrPicker = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedAddress" @click="confirmPick">
          确定选择
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showLogisticsDialog"
      title="物流信息"
      width="500px"
      destroy-on-close
    >
      <div v-if="currentLogistics" class="logistics-info">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="物流公司">{{ currentLogistics.logisticsCompany }}</el-descriptions-item>
          <el-descriptions-item label="运单号">
            {{ currentLogistics.trackingNo }}
            <el-button link type="primary" size="small" @click="copyTracking(currentLogistics.trackingNo)" style="margin-left:8px">复制</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="发货备注">{{ currentLogistics.shippingRemark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ currentLogistics.shippedAt || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-alert type="info" show-icon style="margin-top:16px" class="logistics-alert">
          <template #title>物流查询服务开发中，实际项目可对接快递100/菜鸟等API</template>
        </el-alert>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Location, Plus, Check } from '@element-plus/icons-vue'
import api from '../api'
import { regionData, tagOptions } from '../data/region'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const orders = ref([])
const showCheckout = ref(false)
const submitting = ref(false)
const showAddrPicker = ref(false)
const addrLoading = ref(false)
const addresses = ref([])
const selectedAddress = ref(null)
const pendingPickAddress = ref(null)

const showLogisticsDialog = ref(false)
const currentLogistics = ref(null)

const manualForm = reactive({
  receiverName: '',
  receiverPhone: '',
  region: [],
  detailAddress: '',
  tag: '',
  saveToAddressBook: false,
})

const STATUS_TEXT = { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消' }
const STATUS_TYPE = {
  0: 'warning',
  1: 'primary',
  2: '',
  3: 'success',
  4: 'info',
}

function statusText(s) {
  return STATUS_TEXT[s] ?? '未知'
}
function statusType(s) {
  return STATUS_TYPE[s] ?? 'info'
}

function canReview(status) {
  return status >= 1 && status <= 3
}

function showAfterSale(order) {
  if (order.status !== 3) return false
  if (!order.completedAt) return false
  const completedAt = new Date(order.completedAt.replace(/-/g, '/'))
  const now = new Date()
  const diffDays = (now - completedAt) / (1000 * 60 * 60 * 24)
  return diffDays <= 7
}

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

async function receive(orderId) {
  try {
    await api.post(`/orders/${orderId}/receive`)
    ElMessage.success('已确认收货')
    load()
  } catch (e) {}
}

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

function viewLogistics(order) {
  currentLogistics.value = order
  showLogisticsDialog.value = true
}

async function applyAfterSale(order, source) {
  try {
    await api.post(`/orders/${order.id}/after-sale-intent`, { source })
    router.push({ name: 'AfterSalePlaceholder', params: { id: order.id } })
  } catch (e) {}
}

watch(
  () => route.query.checkout,
  (v) => {
    if (v === '1') {
      showCheckout.value = true
      loadAddresses()
    }
  },
  { immediate: true }
)

async function loadAddresses() {
  addrLoading.value = true
  try {
    const res = await api.get('/addresses')
    addresses.value = res.data.code === 200 ? res.data.data || [] : []
    if (!selectedAddress.value && addresses.value.length) {
      const def = addresses.value.find((a) => a.isDefault === 1) || addresses.value[0]
      selectedAddress.value = def
    }
  } finally {
    addrLoading.value = false
  }
}

function pickAddress(addr) {
  pendingPickAddress.value = addr
}

function confirmPick() {
  if (pendingPickAddress.value) {
    selectedAddress.value = pendingPickAddress.value
  }
  showAddrPicker.value = false
}

function goToAddressPage() {
  showAddrPicker.value = false
  showCheckout.value = false
  router.push('/my-address')
}

function onCheckoutClose() {
  selectedAddress.value = null
  pendingPickAddress.value = null
  Object.assign(manualForm, {
    receiverName: '',
    receiverPhone: '',
    region: [],
    detailAddress: '',
    tag: '',
    saveToAddressBook: false,
  })
}

async function submitOrder() {
  let payload = {}
  if (selectedAddress.value) {
    payload = { shippingAddressId: selectedAddress.value.id }
  } else {
    if (!manualForm.receiverName || !manualForm.receiverPhone
        || !manualForm.region?.length || !manualForm.detailAddress) {
      ElMessage.warning('请选择地址或完整填写收货信息')
      return
    }
    if (!/^1[3-9]\d{9}$/.test(manualForm.receiverPhone)) {
      ElMessage.warning('手机号格式不正确')
      return
    }
    payload = {
      receiverName: manualForm.receiverName,
      receiverPhone: manualForm.receiverPhone,
      province: manualForm.region[0],
      city: manualForm.region[1],
      district: manualForm.region[2],
      detailAddress: manualForm.detailAddress,
      saveToAddressBook: manualForm.saveToAddressBook,
    }
  }
  submitting.value = true
  try {
    const res = await api.post('/orders', payload)
    if (res.data.code === 200) {
      ElMessage.success('订单创建成功')
      showCheckout.value = false
      onCheckoutClose()
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
  font-weight: 500;
}

.order-amount,
.order-receiver,
.order-time {
  margin: 0 0 4px 0;
  line-height: 1.8;
  font-size: 0.9375rem;
}

.order-amount {
  font-size: 1rem;
  color: var(--color-text);
}

.order-amount strong {
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.order-receiver strong,
.order-time strong {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.order-receiver {
  color: var(--color-text-secondary);
}

.order-time {
  color: var(--color-text-muted);
  font-size: 0.875rem;
  margin-bottom: 12px;
}

.order-logistics {
  margin: 8px 0 12px 0;
  padding: 12px 16px;
  background: var(--color-primary-lightest);
  border: 1px solid var(--color-primary-light);
  border-radius: var(--radius);
}

.order-logistics p {
  margin: 4px 0;
  font-size: 0.9375rem;
  color: var(--color-text);
}

.order-logistics strong {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.order-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px dashed var(--color-border);
  flex-wrap: wrap;
}

.checkout-body {
  max-height: 70vh;
  overflow-y: auto;
}

.checkout-section {
  padding: 4px 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
}

.section-header .hint {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.selected-address {
  padding: 16px 20px;
  cursor: pointer;
  transition: all var(--transition);
  position: relative;
}

.selected-address:hover {
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.empty-addr {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px 20px;
  color: var(--color-text-muted);
  border-style: dashed;
  gap: 8px;
}

.empty-addr-icon {
  font-size: 1.75rem;
  color: var(--color-primary);
}

.addr-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.addr-receiver {
  margin: 0 0 4px 0;
  line-height: 1.5;
}

.addr-receiver strong {
  margin-right: 12px;
  color: var(--color-text);
}

.addr-phone {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.addr-detail {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  line-height: 1.6;
}

.addr-picker-body {
  max-height: 55vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.addr-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.addr-item {
  padding: 14px 18px;
  border: 1.5px solid var(--color-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all var(--transition);
  position: relative;
  background: #fff;
}

.addr-item:hover {
  border-color: var(--color-primary-light);
  background: var(--color-primary-lightest);
}

.addr-item.active {
  border-color: var(--color-primary);
  background: var(--color-primary-lightest);
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.15);
}

.addr-item-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.addr-item-receiver {
  margin: 0 0 4px 0;
  line-height: 1.5;
}

.addr-item-receiver strong {
  margin-right: 12px;
  color: var(--color-text);
}

.addr-item-phone {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.addr-item-detail {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  line-height: 1.5;
  padding-right: 28px;
}

.addr-check {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
}

.addr-picker-footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--color-border);
  display: flex;
  justify-content: flex-end;
}

.logistics-info {
  padding: 4px 0;
}

.logistics-alert {
  --el-alert-padding: 10px 12px;
}
</style>
