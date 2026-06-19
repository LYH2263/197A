<template>
  <div v-loading="loading" class="product-detail">
    <template v-if="product">
      <el-row :gutter="24">
        <el-col :span="10">
          <div class="gallery">
            <div class="gallery-main" @touchstart="onTouchStart" @touchend="onTouchEnd">
              <img
                :src="currentImage || '/images/default-product.svg'"
                alt=""
                @error="$event.target.src = '/images/default-product.svg'"
              />
              <template v-if="galleryImages.length > 1">
                <button class="gallery-arrow gallery-arrow--left" @click.stop="prevImage">
                  <el-icon><ArrowLeft /></el-icon>
                </button>
                <button class="gallery-arrow gallery-arrow--right" @click.stop="nextImage">
                  <el-icon><ArrowRight /></el-icon>
                </button>
              </template>
              <div v-if="galleryImages.length > 1" class="gallery-counter">
                {{ currentIndex + 1 }} / {{ galleryImages.length }}
              </div>
            </div>
            <div v-if="galleryImages.length > 1" class="gallery-thumbs">
              <div
                v-for="(img, idx) in galleryImages"
                :key="img.id || idx"
                class="gallery-thumb"
                :class="{ 'gallery-thumb--active': idx === currentIndex }"
                @click="currentIndex = idx"
              >
                <img
                  :src="img.imageUrl || img"
                  alt=""
                  @error="$event.target.src = '/images/default-product.svg'"
                />
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="14">
          <h1 class="title">{{ product.name }}</h1>
          <p class="subtitle">{{ product.subtitle }}</p>
          <p class="price">¥ {{ product.price }}</p>
          <p class="stock">库存：{{ product.stock }}</p>
          <div class="actions">
            <el-input-number v-model="quantity" :min="1" :max="product.stock" />
            <el-button type="primary" :disabled="product.stock < 1" @click="addToCart">
              加入购物车
            </el-button>
          </div>
          <div v-if="product.detail" class="detail" v-html="product.detail" />
        </el-col>
      </el-row>
      <el-divider />
      <section class="reviews">
        <h3>商品评价</h3>
        <div v-if="userStore.isLoggedIn" class="add-review">
          <el-button type="primary" :loading="loadingReviewable" @click="openAddReview">
            我要评价
          </el-button>
        </div>
        <div v-else class="add-review-hint">
          <el-text type="info">登录后可发表评价</el-text>
        </div>
        <div v-for="r in reviews" :key="r.id" class="review-item">
          <el-rate :model-value="r.rating" disabled />
          <span class="review-content">{{ r.content || '（无文字）' }}</span>
          <span class="review-time">{{ r.createdAt }}</span>
        </div>
        <el-empty v-if="reviews.length === 0" description="暂无评价" />
      </section>
      <el-dialog v-model="reviewVisible" title="发表评价" width="420px" @close="resetReviewForm">
        <div v-if="loadingReviewable" class="review-dialog-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在加载可评价订单…</span>
        </div>
        <template v-else-if="reviewableOrders.length === 0">
          <el-empty description="您暂无可评价的订单">
            <template #description>
              <p>需已付款及之后的订单且包含本商品方可评价</p>
              <el-button type="primary" link @click="goOrders">去我的订单</el-button>
            </template>
          </el-empty>
        </template>
        <el-form v-else :model="reviewForm" label-width="80px">
          <el-form-item v-if="reviewableOrders.length > 1" label="选择订单">
            <el-select v-model="reviewForm.orderId" placeholder="请选择订单" style="width: 100%">
              <el-option
                v-for="o in reviewableOrders"
                :key="o.orderId"
                :label="`订单号 ${o.orderNo}`"
                :value="o.orderId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="评分" required>
            <el-rate v-model="reviewForm.rating" />
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input v-model="reviewForm.content" type="textarea" :rows="3" placeholder="选填" />
          </el-form-item>
        </el-form>
        <template v-if="reviewableOrders.length > 0" #footer>
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交</el-button>
        </template>
      </el-dialog>
    </template>
    <el-empty v-else-if="!loading" description="商品不存在" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import api from '../api'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const product = ref(null)
const quantity = ref(1)
const reviews = ref([])
const reviewVisible = ref(false)
const reviewableOrders = ref([])
const loadingReviewable = ref(false)
const reviewSubmitting = ref(false)
const reviewForm = reactive({ orderId: null, productId: null, rating: 5, content: '' })

const productId = computed(() => Number(route.params.id))

const galleryImages = computed(() => {
  if (!product.value) return []
  if (product.value.images && product.value.images.length > 0) {
    return product.value.images
  }
  if (product.value.mainImage) {
    return [{ imageUrl: product.value.mainImage, isMain: 1 }]
  }
  return []
})

const currentIndex = ref(0)

const currentImage = computed(() => {
  if (galleryImages.value.length === 0) return ''
  const img = galleryImages.value[currentIndex.value]
  return img.imageUrl || img
})

function prevImage() {
  if (galleryImages.value.length <= 1) return
  currentIndex.value = (currentIndex.value - 1 + galleryImages.value.length) % galleryImages.value.length
}

function nextImage() {
  if (galleryImages.value.length <= 1) return
  currentIndex.value = (currentIndex.value + 1) % galleryImages.value.length
}

function onKeydown(e) {
  if (e.key === 'ArrowLeft') prevImage()
  else if (e.key === 'ArrowRight') nextImage()
}

let touchStartX = 0
function onTouchStart(e) {
  touchStartX = e.changedTouches[0].clientX
}
function onTouchEnd(e) {
  const diff = touchStartX - e.changedTouches[0].clientX
  if (Math.abs(diff) > 50) {
    if (diff > 0) nextImage()
    else prevImage()
  }
}

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  try {
    const [pRes, rRes] = await Promise.all([
      api.get(`/products/${productId.value}`),
      api.get(`/reviews/product/${productId.value}`),
    ])
    if (pRes.data.code === 200) product.value = pRes.data.data
    if (rRes.data.code === 200) reviews.value = rRes.data.data || []
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})

async function loadReviewableOrders() {
  loadingReviewable.value = true
  reviewableOrders.value = []
  try {
    const [ordersRes, myReviewsRes] = await Promise.all([
      api.get('/orders'),
      api.get('/reviews/me'),
    ])
    const orders = (ordersRes.data.code === 200 ? ordersRes.data.data : []) || []
    const myReviews = (myReviewsRes.data.code === 200 ? myReviewsRes.data.data : []) || []
    const reviewedSet = new Set(myReviews.map((r) => `${r.orderId}-${r.productId}`))
    const reviewable = orders.filter((o) => o.status >= 1 && o.status <= 3)
    const itemPromises = reviewable.map((o) => api.get(`/orders/${o.id}/items`))
    const itemResults = await Promise.all(itemPromises)
    const list = []
    reviewable.forEach((o, i) => {
      const items = (itemResults[i].data.code === 200 ? itemResults[i].data.data : []) || []
      const hasProduct = items.some((it) => Number(it.productId) === productId.value)
      if (hasProduct && !reviewedSet.has(`${o.id}-${productId.value}`)) {
        list.push({ orderId: o.id, orderNo: o.orderNo })
      }
    })
    reviewableOrders.value = list
    if (list.length > 0) {
      reviewForm.orderId = list[0].orderId
      reviewForm.productId = productId.value
      reviewForm.rating = 5
      reviewForm.content = ''
    }
  } finally {
    loadingReviewable.value = false
  }
}

function openAddReview() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  reviewVisible.value = true
  loadReviewableOrders()
}

function resetReviewForm() {
  reviewForm.orderId = null
  reviewForm.productId = null
  reviewForm.rating = 5
  reviewForm.content = ''
}

function goOrders() {
  reviewVisible.value = false
  router.push({ name: 'OrderList' })
}

async function submitReview() {
  if (!reviewForm.orderId || !reviewForm.productId) return
  reviewSubmitting.value = true
  try {
    await api.post('/reviews', {
      orderId: reviewForm.orderId,
      productId: reviewForm.productId,
      rating: reviewForm.rating,
      content: reviewForm.content || undefined,
    })
    ElMessage.success('评价成功')
    reviewVisible.value = false
    const rRes = await api.get(`/reviews/product/${productId.value}`)
    if (rRes.data.code === 200) reviews.value = rRes.data.data || []
  } catch (e) {
  } finally {
    reviewSubmitting.value = false
  }
}

async function addToCart() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await api.post('/cart/add', { productId: productId.value, quantity: quantity.value })
    ElMessage.success('已加入购物车')
    userStore.cartCount = (userStore.cartCount || 0) + quantity.value
  } catch (e) {
  }
}
</script>

<style scoped>
.product-detail {
  padding-bottom: 48px;
}

.gallery {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gallery-main {
  aspect-ratio: 1;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  position: relative;
  user-select: none;
}

.gallery-main img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.2s ease;
}

.gallery-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: var(--color-text);
  font-size: 16px;
  z-index: 2;
}

.gallery-arrow:hover {
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.gallery-arrow--left {
  left: 12px;
}

.gallery-arrow--right {
  right: 12px;
}

.gallery-counter {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 0.75rem;
  padding: 4px 10px;
  border-radius: 12px;
  z-index: 2;
}

.gallery-thumbs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.gallery-thumb {
  width: 64px;
  height: 64px;
  min-width: 64px;
  border-radius: 8px;
  border: 2px solid transparent;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s ease;
  background: var(--color-bg);
}

.gallery-thumb--active {
  border-color: var(--color-primary);
}

.gallery-thumb:hover {
  border-color: var(--color-primary-hover);
}

.gallery-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.title {
  font-size: 1.625rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.subtitle {
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  font-size: 1rem;
}

.price {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 8px;
}

.stock {
  color: var(--color-text-muted);
  margin-bottom: 20px;
  font-size: 0.9375rem;
}

.actions {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 28px;
}

.detail {
  margin-top: 20px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.reviews {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
}

.reviews h3 {
  font-size: 1.125rem;
  font-weight: 700;
  margin-bottom: 16px;
  color: var(--color-text);
}

.add-review {
  margin-bottom: 16px;
}

.add-review-hint {
  margin-bottom: 16px;
}

.review-dialog-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--color-text-muted);
}

.review-item {
  padding: 14px 0;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.review-content {
  flex: 1;
  color: var(--color-text-secondary);
}

.review-time {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .gallery-main {
    border-radius: 0;
    margin: -32px -24px 0 -24px;
  }

  .gallery-thumbs {
    padding: 0 24px;
  }

  .gallery-arrow {
    width: 32px;
    height: 32px;
  }

  .gallery-arrow--left {
    left: 8px;
  }

  .gallery-arrow--right {
    right: 8px;
  }

  .gallery-thumb {
    width: 56px;
    height: 56px;
    min-width: 56px;
  }
}
</style>
