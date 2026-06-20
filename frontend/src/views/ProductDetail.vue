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

        <div v-if="reviews.length === 0" class="reviews-empty">
          <el-empty description="暂无评价" />
        </div>

        <div v-else class="reviews-timeline">
          <div v-for="r in reviews" :key="r.id" class="timeline-group">
            <el-timeline>
              <el-timeline-item
                :timestamp="formatDateTime(r.createdAt)"
                placement="top"
                size="large"
              >
                <template #dot>
                  <el-icon class="timeline-dot timeline-dot--initial"><ChatDotRound /></el-icon>
                </template>
                <el-card shadow="hover" class="review-card review-card--initial">
                  <div class="review-header">
                    <div class="review-user">
                      <el-avatar :size="32">{{ r.userName ? r.userName.charAt(0).toUpperCase() : 'U' }}</el-avatar>
                      <div class="review-user-info">
                        <span class="review-username">{{ r.userName || '匿名用户' }}</span>
                        <el-rate :model-value="r.rating" disabled size="small" />
                      </div>
                    </div>
                    <div class="review-actions">
                      <el-button
                        v-if="r.canEdit"
                        type="primary"
                        link
                        size="small"
                        @click="openEditReview(r)"
                      >
                        编辑
                      </el-button>
                      <el-button
                        v-if="r.canFollowup"
                        type="success"
                        link
                        size="small"
                        @click="openFollowupReview(r)"
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
                      @error="$event.target.src = '/images/default-product.svg'"
                    />
                  </div>
                  <div v-if="r.replyContent" class="review-reply">
                    <div class="review-reply-header">
                      <el-icon><OfficeBuilding /></el-icon>
                      <span class="review-reply-label">商家回复</span>
                      <span class="review-reply-time">{{ formatDateTime(r.replyAt) }}</span>
                    </div>
                    <div class="review-reply-content">{{ r.replyContent }}</div>
                  </div>
                </el-card>
              </el-timeline-item>

              <el-timeline-item
                v-for="f in r.followups"
                :key="f.id"
                :timestamp="formatDateTime(f.createdAt)"
                placement="top"
                size="large"
              >
                <template #dot>
                  <el-icon class="timeline-dot timeline-dot--followup"><ChatLineSquare /></el-icon>
                </template>
                <el-card shadow="hover" class="review-card review-card--followup">
                  <div class="review-header">
                    <div class="review-user">
                      <el-avatar :size="28">{{ f.userName ? f.userName.charAt(0).toUpperCase() : 'U' }}</el-avatar>
                      <div class="review-user-info">
                        <span class="review-username">{{ f.userName || '匿名用户' }}</span>
                        <span class="review-followup-tag">追评</span>
                      </div>
                    </div>
                  </div>
                  <div class="review-content">{{ f.content || '（无文字）' }}</div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </section>

      <section v-if="recommendConfig.viewedAlsoViewEnabled && viewedAlsoViewProducts.length > 0" class="recommend-section">
        <h3 class="recommend-title">看了又看</h3>
        <div class="recommend-scroll">
          <div
            v-for="p in viewedAlsoViewProducts"
            :key="p.id"
            class="recommend-card"
            @click="goToProduct(p.id)"
          >
            <div class="recommend-card-img">
              <img
                :src="p.mainImage || '/images/default-product.svg'"
                alt=""
                @error="$event.target.src = '/images/default-product.svg'"
              />
            </div>
            <div class="recommend-card-info">
              <div class="recommend-card-name">{{ p.name }}</div>
              <div class="recommend-card-price">¥ {{ p.price }}</div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="recommendConfig.guessYouLikeEnabled && guessYouLikeProducts.length > 0" class="recommend-section">
        <h3 class="recommend-title">猜你喜欢</h3>
        <div class="recommend-scroll">
          <div
            v-for="p in guessYouLikeProducts"
            :key="p.id"
            class="recommend-card"
            @click="goToProduct(p.id)"
          >
            <div class="recommend-card-img">
              <img
                :src="p.mainImage || '/images/default-product.svg'"
                alt=""
                @error="$event.target.src = '/images/default-product.svg'"
              />
            </div>
            <div class="recommend-card-info">
              <div class="recommend-card-name">{{ p.name }}</div>
              <div class="recommend-card-price">¥ {{ p.price }}</div>
            </div>
          </div>
        </div>
      </section>

      <el-dialog
        v-model="reviewVisible"
        :title="reviewDialogTitle"
        width="520px"
        @close="resetReviewForm"
      >
        <div v-if="loadingReviewable" class="review-dialog-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在加载…</span>
        </div>
        <template v-else-if="reviewMode === 'initial' && reviewableOrders.length === 0">
          <el-empty description="您暂无可评价的订单">
            <template #description>
              <p>需已付款及之后的订单且包含本商品方可评价</p>
              <el-button type="primary" link @click="goOrders">去我的订单</el-button>
            </template>
          </el-empty>
        </template>
        <el-form v-else :model="reviewForm" label-width="80px">
          <el-form-item v-if="reviewMode === 'initial' && reviewableOrders.length > 1" label="选择订单">
            <el-select v-model="reviewForm.orderId" placeholder="请选择订单" style="width: 100%">
              <el-option
                v-for="o in reviewableOrders"
                :key="o.orderId"
                :label="`订单号 ${o.orderNo}`"
                :value="o.orderId"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="reviewMode !== 'followup'" label="评分" required>
            <el-rate v-model="reviewForm.rating" />
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input
              v-model="reviewForm.content"
              type="textarea"
              :rows="4"
              :placeholder="reviewMode === 'followup' ? '请输入追评内容' : '选填'"
            />
          </el-form-item>
          <el-form-item v-if="reviewMode === 'initial'" label="上传图片">
            <el-upload
              v-model:file-list="reviewImageFileList"
              :action="uploadImageUrl"
              :headers="uploadHeaders"
              list-type="picture-card"
              :limit="3"
              :on-preview="handlePictureCardPreview"
              :on-remove="handleRemove"
              :before-upload="beforeImageUpload"
              :on-success="handleImageSuccess"
              :on-error="handleImageError"
              accept=".jpg,.jpeg,.png,.gif,.webp"
            >
              <el-icon><Plus /></el-icon>
              <template #tip>
                <div class="el-upload__tip">支持 jpg/png/gif/webp 格式，单张不超过 5MB，最多 3 张</div>
              </template>
            </el-upload>
            <el-dialog v-model="previewVisible">
              <img w-full :src="previewImageUrl" />
            </el-dialog>
          </el-form-item>
        </el-form>
        <template
          v-if="(reviewMode === 'initial' && reviewableOrders.length > 0) || reviewMode === 'followup' || reviewMode === 'edit'"
          #footer
        >
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交</el-button>
        </template>
      </el-dialog>
    </template>
    <el-empty v-else-if="!loading" description="商品不存在" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, ArrowLeft, ArrowRight, ChatDotRound, ChatLineSquare, OfficeBuilding, Plus } from '@element-plus/icons-vue'
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
const reviewMode = ref('initial')
const editingReviewId = ref(null)
const reviewForm = reactive({
  parentId: null,
  orderId: null,
  productId: null,
  rating: 5,
  content: '',
  images: [],
})

const reviewImageFileList = ref([])
const previewVisible = ref(false)
const previewImageUrl = ref('')

const productId = computed(() => Number(route.params.id))

const uploadImageUrl = computed(() => {
  const base = import.meta.env.VITE_API_BASE || ''
  return `${base}/upload/image`
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const reviewDialogTitle = computed(() => {
  if (reviewMode.value === 'followup') return '追加评价'
  if (reviewMode.value === 'edit') return '编辑评价'
  return '发表评价'
})

const galleryImages = computed(() => {
  if (!product.value) return []
  if (product.value.images && product.value.images.length > 0) {
    const seen = new Set()
    const deduped = []
    for (const img of product.value.images) {
      const url = img.imageUrl || img
      if (!seen.has(url)) {
        seen.add(url)
        deduped.push(img)
      }
    }
    return deduped
  }
  if (product.value.mainImage) {
    return [{ imageUrl: product.value.mainImage, isMain: 1 }]
  }
  return []
})

const guessYouLikeProducts = ref([])
const viewedAlsoViewProducts = ref([])
const recommendConfig = reactive({
  guessYouLikeEnabled: true,
  guessYouLikeCount: 6,
  viewedAlsoViewEnabled: true,
  viewedAlsoViewCount: 6,
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
  await loadProduct()
})

watch(productId, async () => {
  await loadProduct()
})

async function loadProduct() {
  loading.value = true
  guessYouLikeProducts.value = []
  viewedAlsoViewProducts.value = []
  try {
    const [pRes, rRes] = await Promise.all([
      api.get(`/products/${productId.value}`),
      api.get(`/reviews/product/${productId.value}`),
    ])
    if (pRes.data.code === 200) product.value = pRes.data.data
    if (rRes.data.code === 200) reviews.value = rRes.data.data || []

    if (product.value) {
      const cid = product.value.categoryId
      const pid = product.value.id

      try {
        const cfgRes = await api.get('/recommendations/config')
        if (cfgRes.data.code === 200 && cfgRes.data.data) {
          Object.assign(recommendConfig, cfgRes.data.data)
        }
      } catch {}

      const recPromises = []
      if (recommendConfig.guessYouLikeEnabled) {
        recPromises.push(
          api.get('/recommendations/guess-you-like', { params: { categoryId: cid, excludeProductId: pid } })
            .then(res => { if (res.data.code === 200) guessYouLikeProducts.value = res.data.data || [] })
            .catch(() => {})
        )
      }
      if (recommendConfig.viewedAlsoViewEnabled) {
        recPromises.push(
          api.get('/recommendations/viewed-also-view', { params: { categoryId: cid, excludeProductId: pid } })
            .then(res => { if (res.data.code === 200) viewedAlsoViewProducts.value = res.data.data || [] })
            .catch(() => {})
        )
      }
      await Promise.all(recPromises)
    }
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})

function formatDateTime(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

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
  reviewMode.value = 'initial'
  editingReviewId.value = null
  reviewImageFileList.value = []
  reviewForm.images = []
  reviewVisible.value = true
  loadReviewableOrders()
}

function openEditReview(review) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  reviewMode.value = 'edit'
  editingReviewId.value = review.id
  reviewForm.parentId = null
  reviewForm.orderId = review.orderId
  reviewForm.productId = review.productId
  reviewForm.rating = review.rating
  reviewForm.content = review.content || ''
  reviewForm.images = [...(review.images || [])]
  reviewImageFileList.value = (review.images || []).map((url, idx) => ({
    name: `image-${idx}`,
    url: url,
    response: { data: { url: url } },
  }))
  reviewVisible.value = true
}

function openFollowupReview(review) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  reviewMode.value = 'followup'
  editingReviewId.value = null
  reviewForm.parentId = review.id
  reviewForm.orderId = review.orderId
  reviewForm.productId = review.productId
  reviewForm.rating = review.rating
  reviewForm.content = ''
  reviewForm.images = []
  reviewImageFileList.value = []
  reviewVisible.value = true
}

function resetReviewForm() {
  reviewMode.value = 'initial'
  editingReviewId.value = null
  reviewForm.parentId = null
  reviewForm.orderId = null
  reviewForm.productId = null
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewForm.images = []
  reviewImageFileList.value = []
}

function goOrders() {
  reviewVisible.value = false
  router.push({ name: 'OrderList' })
}

function beforeImageUpload(file) {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  const isImage = allowedTypes.includes(file.type)
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('仅支持 jpg/png/gif/webp 格式的图片！')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('单张图片大小不能超过 5MB！')
    return false
  }
  return true
}

function handlePictureCardPreview(uploadFile) {
  previewImageUrl.value = uploadFile.response?.data?.url || uploadFile.url
  previewVisible.value = true
}

function handleRemove(uploadFile) {
  const url = uploadFile.response?.data?.url || uploadFile.url
  reviewForm.images = reviewForm.images.filter((u) => u !== url)
}

function handleImageSuccess(response, uploadFile) {
  if (response.code === 200 && response.data?.url) {
    reviewForm.images.push(response.data.url)
  } else {
    ElMessage.error(response.message || '图片上传失败')
  }
}

function handleImageError(err, uploadFile) {
  ElMessage.error('图片上传失败，请重试')
}

async function submitReview() {
  if (!reviewForm.orderId || !reviewForm.productId) return
  reviewSubmitting.value = true
  try {
    const body = {
      orderId: reviewForm.orderId,
      productId: reviewForm.productId,
      rating: reviewForm.rating,
      content: reviewForm.content || undefined,
    }
    if (reviewMode.value === 'followup') {
      body.parentId = reviewForm.parentId
    }
    if (reviewMode.value === 'initial' || reviewMode.value === 'edit') {
      if (reviewForm.images && reviewForm.images.length > 0) {
        body.images = reviewForm.images
      }
    }

    let successMsg = '评价成功'
    if (reviewMode.value === 'edit') {
      await api.put(`/reviews/${editingReviewId.value}`, body)
      successMsg = '编辑成功'
    } else if (reviewMode.value === 'followup') {
      await api.post('/reviews', body)
      successMsg = '追评成功'
    } else {
      await api.post('/reviews', body)
    }
    ElMessage.success(successMsg)
    reviewVisible.value = false
    const rRes = await api.get(`/reviews/product/${productId.value}`)
    if (rRes.data.code === 200) reviews.value = rRes.data.data || []
  } catch (e) {
    if (e?.response?.data?.message) {
      ElMessage.error(e.response.data.message)
    }
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

function goToProduct(id) {
  router.push(`/products/${id}`)
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

.reviews-empty {
  padding: 24px 0;
}

.reviews-timeline {
  margin-top: 8px;
}

.timeline-group {
  margin-bottom: 24px;
}

.timeline-dot {
  font-size: 18px;
}

.timeline-dot--initial {
  color: var(--color-primary);
}

.timeline-dot--followup {
  color: var(--color-success);
}

.review-card {
  margin-bottom: 8px;
}

.review-card--initial {
  border-left: 3px solid var(--color-primary);
}

.review-card--followup {
  border-left: 3px solid var(--color-success);
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.05) 0%, transparent 100%);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  gap: 12px;
  flex-wrap: wrap;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.review-username {
  font-weight: 600;
  color: var(--color-text);
  font-size: 0.9375rem;
}

.review-followup-tag {
  display: inline-block;
  padding: 1px 8px;
  background: var(--color-success-light-9);
  color: var(--color-success);
  border-radius: 10px;
  font-size: 0.75rem;
  margin-top: 2px;
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
  width: 100px;
  height: 100px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--color-border);
  transition: transform 0.2s ease;
}

.review-image:hover {
  transform: scale(1.05);
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
  gap: 6px;
  margin-bottom: 6px;
  color: var(--color-warning);
}

.review-reply-label {
  font-weight: 600;
  font-size: 0.875rem;
}

.review-reply-time {
  color: var(--color-text-muted);
  font-size: 0.75rem;
  margin-left: auto;
}

.review-reply-content {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  line-height: 1.6;
}

.review-dialog-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--color-text-muted);
}

.recommend-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
}

.recommend-title {
  font-size: 1.125rem;
  font-weight: 700;
  margin-bottom: 16px;
  color: var(--color-text);
}

.recommend-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.recommend-scroll::-webkit-scrollbar {
  height: 6px;
}

.recommend-scroll::-webkit-scrollbar-track {
  background: var(--color-bg);
  border-radius: 3px;
}

.recommend-scroll::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 3px;
}

.recommend-card {
  min-width: 180px;
  max-width: 180px;
  cursor: pointer;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  scroll-snap-align: start;
  transition: all var(--transition);
  flex-shrink: 0;
}

.recommend-card:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.recommend-card-img {
  height: 180px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.recommend-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.recommend-card:hover .recommend-card-img img {
  transform: scale(1.04);
}

.recommend-card-info {
  padding: 12px;
}

.recommend-card-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-card-price {
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-primary);
  margin-top: 6px;
}

@media (max-width: 768px) {
  .recommend-card {
    min-width: 150px;
    max-width: 150px;
  }

  .recommend-card-img {
    height: 150px;
  }
}

:deep(.el-timeline-item__timestamp) {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
}

:deep(.el-timeline-item__wrapper) {
  padding-left: 20px;
}

:deep(.el-upload--picture-card) {
  width: 80px;
  height: 80px;
  line-height: 80px;
}

:deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 80px;
  height: 80px;
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
