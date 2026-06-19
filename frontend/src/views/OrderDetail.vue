<template>
  <div v-loading="loading" class="order-detail">
    <template v-if="order">
      <div class="main-card content-card">
        <div class="main-card-header">
          <span>订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" size="small">{{ statusText(order.status) }}</el-tag>
        </div>
        <p><strong>收货人：</strong>{{ order.receiverName }} {{ order.receiverPhone }}</p>
        <p><strong>收货地址：</strong>{{ order.receiverAddress }}</p>
        <p><strong>订单金额：</strong>¥ {{ order.totalAmount }}</p>
        <p><strong>下单时间：</strong>{{ order.createdAt }}</p>
        <el-divider />
        <h4>商品明细</h4>
        <el-table :data="items" style="width: 100%">
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="price" label="单价" width="120">
            <template #default="{ row }">¥ {{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="totalAmount" label="小计" width="120">
            <template #default="{ row }">¥ {{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="评价" width="100">
            <template #default="{ row }">
              <el-button
                v-if="canReview(order.status) && !hasReview(row.productId)"
                type="primary"
                link
                size="small"
                @click="openReview(row)"
              >
                评价
              </el-button>
              <span v-else-if="hasReview(row.productId)">已评价</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="order.status === 0" class="actions">
          <el-button type="primary" @click="pay">去支付</el-button>
          <el-button @click="cancel">取消订单</el-button>
        </div>
      </div>
      <el-dialog v-model="reviewVisible" title="商品评价" width="400px" @close="reviewForm = {}">
        <el-form :model="reviewForm" label-width="80px">
          <el-form-item label="评分" required>
            <el-rate v-model="reviewForm.rating" />
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input v-model="reviewForm.content" type="textarea" :rows="3" placeholder="选填" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交</el-button>
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
import api from '../api'

const route = useRoute()
const loading = ref(true)
const order = ref(null)
const items = ref([])
const productReviews = ref(new Set())
const reviewVisible = ref(false)
const reviewForm = reactive({ orderId: null, productId: null, rating: 5, content: '' })
const reviewSubmitting = ref(false)

const orderId = computed(() => Number(route.params.id))

function statusText(s) {
  return { 0: '待付款', 1: '已付款', 2: '已发货', 3: '已完成', 4: '已取消' }[s] || '未知'
}
function statusType(s) {
  return { 0: 'warning', 1: 'primary', 2: 'info', 3: 'success', 4: 'info' }[s] || 'info'
}
/** 已付款(1)、已发货(2)、已完成(3) 可评价 */
function canReview(status) {
  return status >= 1 && status <= 3
}

function hasReview(productId) {
  return productReviews.value.has(productId)
}

function openReview(row) {
  reviewForm.orderId = orderId.value
  reviewForm.productId = row.productId
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewVisible.value = true
}

async function submitReview() {
  reviewSubmitting.value = true
  try {
    await api.post('/reviews', {
      orderId: reviewForm.orderId,
      productId: reviewForm.productId,
      rating: reviewForm.rating,
      content: reviewForm.content,
    })
    ElMessage.success('评价成功')
    productReviews.value.add(reviewForm.productId)
    reviewVisible.value = false
  } finally {
    reviewSubmitting.value = false
  }
}

async function pay() {
  try {
    await api.post(`/orders/${orderId.value}/pay`)
    ElMessage.success('支付成功（模拟）')
    load()
  } catch (e) {}
}

async function cancel() {
  try {
    await api.post(`/orders/${orderId.value}/cancel`)
    ElMessage.success('已取消')
    load()
  } catch (e) {}
}

async function load() {
  loading.value = true
  try {
    const [oRes, iRes, rRes] = await Promise.all([
      api.get(`/orders/${orderId.value}`),
      api.get(`/orders/${orderId.value}/items`),
      api.get('/reviews/me').catch(() => ({ data: { code: 401, data: [] } })),
    ])
    if (oRes.data.code === 200) order.value = oRes.data.data
    if (iRes.data.code === 200) items.value = iRes.data.data || []
    const myReviews = (rRes?.data?.code === 200 ? rRes.data.data : []) || []
    productReviews.value = new Set(
      myReviews.filter((r) => Number(r.orderId) === orderId.value).map((r) => r.productId)
    )
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.order-detail {
  padding-bottom: 32px;
}

.main-card {
  max-width: 800px;
  padding: 28px;
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

.order-detail .actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}
</style>
