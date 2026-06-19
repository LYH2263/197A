<template>
  <div class="my-reviews">
    <h2 class="page-title">我的评价</h2>
    <div v-loading="loading" class="content">
      <div v-if="list.length === 0" class="reviews-empty">
        <el-empty description="暂无评价" />
      </div>

      <div v-else class="reviews-timeline">
        <div v-for="r in list" :key="r.id" class="review-group">
          <el-card shadow="hover" class="review-card review-card--initial">
            <div class="review-header">
              <div class="review-product">
                <span class="product-name">{{ r.productName }}</span>
                <el-rate :model-value="r.rating" disabled size="small" />
              </div>
              <div class="review-actions">
                <el-button
                  v-if="r.canEdit"
                  type="primary"
                  link
                  size="small"
                  @click="goToProduct(r.productId)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="r.canFollowup"
                  type="success"
                  link
                  size="small"
                  @click="goToProduct(r.productId)"
                >
                  追评
                </el-button>
                <el-tooltip
                  v-else-if="r.followupDisabledReason"
                  :content="r.followupDisabledReason"
                  placement="top"
                >
                  <el-button type="success" link size="small" disabled>追评</el-button>
                </el-tooltip>
              </div>
            </div>
            <div class="review-content">{{ r.content || '（无文字）' }}</div>
            <div v-if="r.images && r.images.length > 0" class="review-images">
              <el-image
                v-for="(img, idx) in r.images"
                :key="idx"
                :src="img"
                :preview-src-list="r.images"
                :initial-index="idx"
                fit="cover"
                class="review-image"
              />
            </div>
            <div class="review-time">{{ formatDateTime(r.createdAt) }}</div>
            <div v-if="r.replyContent" class="review-reply">
              <div class="review-reply-header">
                <span class="review-reply-label">商家回复</span>
                <span class="review-reply-time">{{ formatDateTime(r.replyAt) }}</span>
              </div>
              <div class="review-reply-content">{{ r.replyContent }}</div>
            </div>

            <div v-for="f in r.followups" :key="f.id" class="followup-item">
              <div class="followup-header">
                <span class="followup-tag">追评</span>
                <span class="followup-time">{{ formatDateTime(f.createdAt) }}</span>
              </div>
              <div class="followup-content">{{ f.content || '（无文字）' }}</div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()
const loading = ref(true)
const list = ref([])

function formatDateTime(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function goToProduct(productId) {
  router.push({ name: 'ProductDetail', params: { id: productId } })
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await api.get('/reviews/me')
    list.value = res.data.code === 200 ? (res.data.data || []) : []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.my-reviews {
  padding-bottom: 32px;
}

.content {
  padding: 0;
}

.reviews-empty {
  padding: 48px 0;
}

.reviews-timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  margin-bottom: 0;
}

.review-card--initial {
  border-left: 3px solid var(--color-primary);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 12px;
  flex-wrap: wrap;
}

.review-product {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-weight: 600;
  color: var(--color-text);
  font-size: 1rem;
}

.review-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.review-content {
  color: var(--color-text-secondary);
  line-height: 1.6;
  font-size: 0.9375rem;
  margin-bottom: 12px;
}

.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.review-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--color-border);
}

.review-time {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  margin-bottom: 8px;
}

.review-reply {
  background: var(--color-bg-page);
  border-radius: 8px;
  padding: 12px 16px;
  margin-top: 8px;
  border-left: 3px solid var(--color-warning);
}

.review-reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.review-reply-label {
  font-weight: 600;
  color: var(--color-warning);
  font-size: 0.875rem;
}

.review-reply-time {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.review-reply-content {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  line-height: 1.6;
}

.followup-item {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.05) 0%, transparent 100%);
  border-radius: 8px;
  padding: 12px 16px;
  margin-top: 8px;
  border-left: 3px solid var(--color-success);
}

.followup-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.followup-tag {
  display: inline-block;
  padding: 1px 8px;
  background: var(--color-success-light-9);
  color: var(--color-success);
  border-radius: 10px;
  font-size: 0.75rem;
  font-weight: 600;
}

.followup-time {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}

.followup-content {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .content {
    padding: 0;
  }
}
</style>
