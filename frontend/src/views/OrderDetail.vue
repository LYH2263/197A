<template>
  <div v-loading="loading" class="order-detail">
    <template v-if="order">
      <div class="main-card content-card">
        <div class="main-card-header">
          <span>订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)" effect="light" size="small" class="status-tag">
            {{ statusText(order.status) }}
          </el-tag>
        </div>

        <div class="toolbar no-print">
          <el-button size="small" @click="handlePrint">
            <el-icon><Printer /></el-icon>打印
          </el-button>
          <el-button size="small" type="primary" :loading="pdfLoading" @click="exportPdf">
            <el-icon><Download /></el-icon>导出 PDF
          </el-button>
          <el-button size="small" type="success" :loading="shareLoading" @click="generateShareLink">
            <el-icon><Share /></el-icon>分享订单
          </el-button>
        </div>

        <div class="print-area">
          <div class="print-header print-only">
            <h2>订单详情</h2>
          </div>

          <el-descriptions :column="1" border class="info-table">
            <el-descriptions-item label="收货人">
              {{ order.receiverName }} &nbsp;·&nbsp; {{ order.receiverPhone }}
            </el-descriptions-item>
            <el-descriptions-item label="收货地址">{{ order.receiverAddress }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">¥ {{ order.totalAmount }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">{{ statusText(order.status) }}</el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ order.createdAt }}</el-descriptions-item>
            <el-descriptions-item v-if="order.status >= 1" label="支付时间">
              {{ payTime || '-' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="order.status >= 2 && order.shippedAt" label="发货时间">
              {{ order.shippedAt }}
            </el-descriptions-item>
            <el-descriptions-item v-if="order.status === 3 && order.completedAt" label="完成时间">
              {{ order.completedAt }}
              <el-tag v-if="showAfterSale" type="warning" effect="light" size="small" style="margin-left:12px">
                7天售后申请期内
              </el-tag>
            </el-descriptions-item>
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
            <el-alert type="info" show-icon class="logistics-alert no-print">
              <template #title>物流轨迹查询功能开发中，实际项目可对接快递100/菜鸟等第三方API</template>
            </el-alert>
          </template>

          <el-divider content-position="left">
            <span class="divider-title">商品明细</span>
          </el-divider>
          <el-table :data="items" style="width: 100%">
            <el-table-column prop="productName" label="商品" />
            <el-table-column label="图片" width="120" class="no-print">
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
            <el-table-column label="评价" width="100" class="no-print">
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
                <span v-else-if="hasReview(row.productId)" style="color:var(--color-text-muted)">已评价</span>
              </template>
            </el-table-column>
          </el-table>

          <template v-if="timeline.length > 0">
            <el-divider content-position="left">
              <span class="divider-title">订单时间线</span>
            </el-divider>
            <el-timeline>
              <el-timeline-item
                v-for="(node, idx) in timeline"
                :key="idx"
                :timestamp="node.time"
                placement="top"
                :type="idx === timeline.length - 1 ? 'primary' : 'info'"
              >
                {{ node.label }}
              </el-timeline-item>
            </el-timeline>
          </template>
        </div>

        <div class="actions no-print">
          <template v-if="order.status === 0">
            <el-button type="primary" @click="pay">去支付</el-button>
            <el-button @click="cancel">取消订单</el-button>
          </template>

          <template v-if="order.status === 1">
            <el-button disabled>等待商家发货</el-button>
          </template>

          <template v-if="order.status === 2">
            <el-button type="primary" @click="viewLogistics">查看物流</el-button>
            <el-button type="success" @click="receive">确认收货</el-button>
          </template>

          <template v-if="order.status === 3">
            <el-button
              v-if="items.some(r => canReview(order.status) && !hasReview(r.productId))"
              type="primary"
              @click="openFirstReview"
            >
              去评价
            </el-button>
            <el-button
              v-if="showAfterSale"
              type="warning"
              @click="applyAfterSale"
            >
              申请售后
            </el-button>
          </template>

          <template v-if="order.status === 4">
            <el-button type="info" disabled>订单已取消</el-button>
          </template>
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

      <el-dialog v-model="logisticsVisible" title="物流详情" width="500px" destroy-on-close>
        <div v-if="order" class="logistics-info">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="物流公司">{{ order.logisticsCompany }}</el-descriptions-item>
            <el-descriptions-item label="运单号">
              {{ order.trackingNo }}
              <el-button link type="primary" size="small" @click="copyTracking(order.trackingNo)" style="margin-left:8px">复制</el-button>
            </el-descriptions-item>
            <el-descriptions-item label="发货备注">{{ order.shippingRemark || '无' }}</el-descriptions-item>
            <el-descriptions-item label="发货时间">{{ order.shippedAt || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-alert type="info" show-icon style="margin-top:16px">
            <template #title>物流查询服务开发中</template>
          </el-alert>
        </div>
      </el-dialog>

      <el-dialog v-model="shareDialogVisible" title="分享订单" width="460px">
        <el-alert type="info" show-icon :closable="false" style="margin-bottom:16px">
          <template #title>分享链接24小时内有效，查看者无需登录，敏感信息已脱敏</template>
        </el-alert>
        <el-input :model-value="shareLink" readonly>
          <template #append>
            <el-button @click="copyShareLink">复制</el-button>
          </template>
        </el-input>
      </el-dialog>
    </template>
    <el-empty v-else-if="!loading" description="订单不存在" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Printer, Download, Share } from '@element-plus/icons-vue'
import api from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const order = ref(null)
const items = ref([])
const productReviews = ref(new Set())
const reviewVisible = ref(false)
const reviewForm = reactive({ orderId: null, productId: null, rating: 5, content: '' })
const reviewSubmitting = ref(false)
const logisticsVisible = ref(false)
const timeline = ref([])
const payTime = ref(null)
const pdfLoading = ref(false)
const shareLoading = ref(false)
const shareDialogVisible = ref(false)
const shareLink = ref('')

const orderId = computed(() => Number(route.params.id))

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

function hasReview(productId) {
  return productReviews.value.has(productId)
}

const showAfterSale = computed(() => {
  if (!order.value) return false
  if (order.value.status !== 3) return false
  if (!order.value.completedAt) return false
  const completedAt = new Date(order.value.completedAt.replace(/-/g, '/'))
  const now = new Date()
  const diffDays = (now - completedAt) / (1000 * 60 * 60 * 24)
  return diffDays <= 7
})

function openReview(row) {
  reviewForm.orderId = orderId.value
  reviewForm.productId = row.productId
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewVisible.value = true
}

function openFirstReview() {
  const target = items.value.find((r) => canReview(order.value.status) && !hasReview(r.productId))
  if (target) openReview(target)
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

async function receive() {
  try {
    await api.post(`/orders/${orderId.value}/receive`)
    ElMessage.success('已确认收货')
    load()
  } catch (e) {}
}

function copyTracking(no) {
  if (!no) return
  copyText(no, '运单号已复制')
}

function copyText(text, msg) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).then(() => ElMessage.success(msg))
  } else {
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    try { document.execCommand('copy'); ElMessage.success(msg) } catch {}
    document.body.removeChild(ta)
  }
}

function viewLogistics() {
  logisticsVisible.value = true
}

async function applyAfterSale() {
  try {
    await api.post(`/orders/${orderId.value}/after-sale-intent`, { source: 'DETAIL' })
    router.push({ name: 'AfterSalePlaceholder', params: { id: orderId.value } })
  } catch (e) {}
}

function handlePrint() {
  window.print()
}

async function generateShareLink() {
  shareLoading.value = true
  try {
    const res = await api.post(`/orders/${orderId.value}/share-token`)
    if (res.data.code === 200) {
      const token = res.data.data.token
      const base = window.location.origin
      shareLink.value = `${base}/orders/share/${token}`
      shareDialogVisible.value = true
    }
  } catch (e) {
  } finally {
    shareLoading.value = false
  }
}

function copyShareLink() {
  copyText(shareLink.value, '分享链接已复制')
}

async function loadTimeline() {
  try {
    const res = await api.get(`/orders/${orderId.value}/timeline`)
    if (res.data.code === 200) {
      timeline.value = res.data.data || []
      const payNode = timeline.value.find(n => n.label === '支付')
      if (payNode) payTime.value = payNode.time
    }
  } catch (e) {
    timeline.value = []
  }
}

async function exportPdf() {
  pdfLoading.value = true
  try {
    const { jsPDF } = await import('jspdf')
    const QRCode = (await import('qrcode')).default

    const doc = new jsPDF('p', 'mm', 'a4')
    const pageW = 210
    const margin = 20
    const contentW = pageW - margin * 2
    let y = 20

    doc.setFontSize(18)
    doc.text('Order Detail', margin, y)
    y += 10

    doc.setFontSize(10)
    doc.text(`Order No: ${order.value.orderNo}`, margin, y)
    y += 6
    doc.text(`Status: ${statusText(order.value.status)}`, margin, y)
    y += 6
    doc.text(`Amount: CNY ${order.value.totalAmount}`, margin, y)
    y += 6
    doc.text(`Created: ${order.value.createdAt}`, margin, y)
    y += 6
    if (payTime.value) {
      doc.text(`Paid: ${payTime.value}`, margin, y)
      y += 6
    }
    if (order.value.shippedAt) {
      doc.text(`Shipped: ${order.value.shippedAt}`, margin, y)
      y += 6
    }
    if (order.value.completedAt) {
      doc.text(`Completed: ${order.value.completedAt}`, margin, y)
      y += 6
    }

    try {
      const qrDataUrl = await QRCode.toDataURL(order.value.orderNo, { width: 120, margin: 1 })
      doc.addImage(qrDataUrl, 'PNG', pageW - margin - 30, 20, 30, 30)
    } catch (e) {
      console.warn('QR generation failed', e)
    }

    y += 4
    doc.setDrawColor(200)
    doc.line(margin, y, pageW - margin, y)
    y += 8

    doc.setFontSize(12)
    doc.text('Shipping Info', margin, y)
    y += 6
    doc.setFontSize(10)
    doc.text(`Receiver: ${order.value.receiverName}  ${order.value.receiverPhone}`, margin, y)
    y += 6
    doc.text(`Address: ${order.value.receiverAddress}`, margin, y)
    y += 8

    doc.setDrawColor(200)
    doc.line(margin, y, pageW - margin, y)
    y += 8

    doc.setFontSize(12)
    doc.text('Items', margin, y)
    y += 6
    doc.setFontSize(9)
    doc.setTextColor(100)
    doc.text('Product', margin, y)
    doc.text('Price', margin + 70, y)
    doc.text('Qty', margin + 100, y)
    doc.text('Subtotal', margin + 120, y)
    y += 4
    doc.setDrawColor(220)
    doc.line(margin, y, pageW - margin, y)
    y += 4

    doc.setTextColor(0)
    doc.setFontSize(10)
    for (const item of items.value) {
      if (y > 270) {
        doc.addPage()
        y = 20
      }
      const name = item.productName.length > 20 ? item.productName.substring(0, 20) + '...' : item.productName
      doc.text(name, margin, y)
      doc.text(`CNY ${item.price}`, margin + 70, y)
      doc.text(String(item.quantity), margin + 100, y)
      doc.text(`CNY ${item.totalAmount}`, margin + 120, y)
      y += 6
    }

    y += 2
    doc.setDrawColor(200)
    doc.line(margin, y, pageW - margin, y)
    y += 6
    doc.setFontSize(11)
    doc.text(`Total: CNY ${order.value.totalAmount}`, margin + 100, y)
    y += 10

    if (timeline.value.length > 0) {
      doc.setDrawColor(200)
      doc.line(margin, y, pageW - margin, y)
      y += 8
      doc.setFontSize(12)
      doc.text('Timeline', margin, y)
      y += 6
      doc.setFontSize(10)
      for (const node of timeline.value) {
        if (y > 275) {
          doc.addPage()
          y = 20
        }
        doc.text(`${node.label}  -  ${node.time}`, margin, y)
        y += 6
      }
    }

    doc.save(`order-${order.value.orderNo}.pdf`)
    ElMessage.success('PDF 已导出')
  } catch (e) {
    console.error('PDF export failed', e)
    ElMessage.error('导出失败')
  } finally {
    pdfLoading.value = false
  }
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
    await loadTimeline()
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
  max-width: 860px;
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

.logistics-alert {
  --el-alert-padding: 10px 12px;
}

.order-detail .actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.logistics-info {
  padding: 4px 0;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.print-only {
  display: none;
}

@media print {
  .no-print {
    display: none !important;
  }

  .print-only {
    display: block !important;
  }

  .print-header h2 {
    font-size: 22px;
    margin-bottom: 12px;
    text-align: center;
  }

  .order-detail {
    padding: 0;
  }

  .main-card {
    max-width: 100%;
    padding: 16px;
    box-shadow: none;
  }

  .main-card-header {
    border-bottom: 2px solid #333;
  }

  .content-card {
    box-shadow: none !important;
    border: none !important;
  }

  :deep(.el-descriptions) {
    border: 1px solid #ddd;
  }

  :deep(.el-descriptions__label) {
    background: #f5f5f5 !important;
  }

  :deep(.el-table) {
    border: 1px solid #ddd;
  }

  :deep(.el-table th) {
    background: #f5f5f5 !important;
  }

  :deep(.el-timeline) {
    padding-left: 0;
  }

  body {
    background: #fff;
  }

  @page {
    size: A4;
    margin: 15mm;
  }
}
</style>
