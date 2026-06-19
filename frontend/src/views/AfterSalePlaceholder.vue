<template>
  <div class="after-sale-placeholder-page">
    <div class="placeholder-card content-card">
      <div class="icon-wrap success">
        <el-icon :size="48"><Check /></el-icon>
      </div>
      <h2 class="title">售后申请已提交</h2>
      <p class="subtitle">我们已经收到您的售后申请意向</p>

      <el-descriptions :column="1" border class="info-desc">
        <el-descriptions-item label="订单号">
          <router-link :to="`/orders/${orderId}`" class="order-link">
            {{ orderId }}
          </router-link>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ triggeredAt }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="warning" effect="light">客服将在1-3个工作日内与您联系</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-alert type="info" show-icon class="tip-alert">
        <template #title>
          售后完整功能即将上线，本期仅记录申请意图，实际退款/退换货流程将在后续版本提供。
        </template>
      </el-alert>

      <div class="actions">
        <el-button @click="$router.back()">返回上一页</el-button>
        <el-button type="primary" @click="$router.push('/orders')">查看我的订单</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Check } from '@element-plus/icons-vue'

const route = useRoute()
const orderId = computed(() => route.params.id)
const triggeredAt = ref(new Date().toLocaleString('zh-CN'))

onMounted(() => {})
</script>

<style scoped>
.after-sale-placeholder-page {
  padding: 40px 24px;
  display: flex;
  justify-content: center;
}

.placeholder-card {
  max-width: 560px;
  width: 100%;
  padding: 40px;
  text-align: center;
}

.icon-wrap {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.icon-wrap.success {
  background: #e1f3d8;
  color: #67c23a;
}

.title {
  margin: 0 0 8px 0;
  font-size: 1.5rem;
  color: var(--color-text);
  font-weight: 600;
}

.subtitle {
  margin: 0 0 24px 0;
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
}

.info-desc {
  text-align: left;
  margin-bottom: 16px;
}

.info-desc :deep(.el-descriptions__label) {
  width: 100px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.order-link {
  color: var(--color-primary);
  text-decoration: none;
}

.order-link:hover {
  text-decoration: underline;
}

.tip-alert {
  text-align: left;
  margin-bottom: 24px;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
