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
        <p class="order-receiver">
          收货人：{{ order.receiverName }} · {{ order.receiverPhone }} · {{ order.receiverAddress }}
        </p>
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

const manualForm = reactive({
  receiverName: '',
  receiverPhone: '',
  region: [],
  detailAddress: '',
  tag: '',
  saveToAddressBook: false,
})

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
}

.order-amount {
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
  font-size: 1rem;
}

.order-receiver {
  font-size: 0.9375rem;
  color: var(--color-text-secondary);
  margin: 0 0 4px 0;
  line-height: 1.6;
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
</style>
