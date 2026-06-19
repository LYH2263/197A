<template>
  <div class="share-page">
    <div v-loading="loading" class="share-card">
      <template v-if="order">
        <div class="share-header">
          <h2>订单分享</h2>
          <el-tag effect="light" size="small">{{ order.statusText }}</el-tag>
        </div>

        <el-alert type="warning" show-icon :closable="false" style="margin-bottom:20px">
          <template #title>此为脱敏订单信息，敏感信息已隐藏</template>
        </el-alert>

        <el-descriptions :column="1" border class="info-table">
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ order.statusText }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">¥ {{ order.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ order.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ order.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ order.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">{{ order.receiverAddress }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">
          <span class="section-title">商品明细</span>
        </el-divider>
        <el-table :data="order.items" style="width:100%">
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="price" label="单价" width="120">
            <template #default="{ row }">¥ {{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="totalAmount" label="小计" width="120">
            <template #default="{ row }">¥ {{ row.totalAmount }}</template>
          </el-table-column>
        </el-table>

        <template v-if="order.timeline && order.timeline.length > 0">
          <el-divider content-position="left">
            <span class="section-title">订单时间线</span>
          </el-divider>
          <el-timeline>
            <el-timeline-item
              v-for="(node, idx) in order.timeline"
              :key="idx"
              :timestamp="node.time"
              placement="top"
              :type="idx === order.timeline.length - 1 ? 'primary' : 'info'"
            >
              {{ node.label }}
            </el-timeline-item>
          </el-timeline>
        </template>
      </template>

      <template v-else-if="!loading">
        <el-result :icon="errorIcon" :title="errorMsg" sub-title="请联系分享者获取新的链接" />
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const loading = ref(true)
const order = ref(null)
const errorMsg = ref('')
const errorIcon = ref('error')

async function load() {
  const token = route.params.token
  if (!token) {
    errorMsg.value = '无效的分享链接'
    loading.value = false
    return
  }
  try {
    const res = await axios.get(`/api/orders/share/${token}`)
    if (res.data.code === 200) {
      order.value = res.data.data
    } else {
      errorMsg.value = res.data.message || '获取订单失败'
      errorIcon.value = 'warning'
    }
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '网络错误'
    if (msg.includes('过期')) {
      errorMsg.value = '分享链接已过期'
    } else if (msg.includes('无效')) {
      errorMsg.value = '分享链接无效'
    } else {
      errorMsg.value = '加载失败'
    }
    errorIcon.value = 'warning'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.share-page {
  min-height: 100vh;
  background: var(--color-bg, #f5f7fa);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 40px 16px;
}

.share-card {
  max-width: 680px;
  width: 100%;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.share-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.share-header h2 {
  margin: 0;
  font-size: 1.25rem;
  color: var(--color-text, #303133);
}

.section-title {
  font-weight: 600;
  color: var(--color-text, #303133);
  font-size: 0.9375rem;
}

.info-table :deep(.el-descriptions__label) {
  width: 100px;
  color: var(--color-text-secondary, #909399);
  font-weight: 500;
}

.info-table :deep(.el-descriptions__content) {
  color: var(--color-text, #303133);
}
</style>
