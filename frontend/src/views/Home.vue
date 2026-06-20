<template>
  <div class="home">
    <section class="hero">
      <div class="hero-bg" />
      <div class="hero-content">
        <h1>精选好物，品质生活</h1>
        <p>发现心仪商品，享受便捷购物体验</p>
        <el-button type="primary" size="large" class="hero-cta" @click="$router.push('/products')">
          去逛逛
        </el-button>
      </div>
    </section>

    <section class="section categories">
      <h2 class="section-title">商品分类</h2>
      <div v-loading="loading" class="category-list">
        <div
          v-for="c in categories"
          :key="c.id"
          class="category-card"
          @click="$router.push({ path: '/products', query: { categoryId: c.id } })"
        >
          <span>{{ c.name }}</span>
        </div>
      </div>
    </section>

    <section class="section products">
      <h2 class="section-title">热门商品</h2>
      <div v-loading="loading" class="product-grid">
        <div
          v-for="p in products"
          :key="p.id"
          class="product-card"
          @click="$router.push(`/products/${p.id}`)"
        >
          <div class="product-img">
            <img
              :src="p.mainImage || '/images/default-product.svg'"
              alt=""
              @error="$event.target.src = '/images/default-product.svg'"
            />
          </div>
          <div class="product-info">
            <div class="product-name">{{ p.name }}</div>
            <div class="product-price">¥ {{ p.price }}</div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="recommendConfig.guessYouLikeEnabled && guessYouLikeProducts.length > 0" class="section recommend-section">
      <h2 class="section-title">猜你喜欢</h2>
      <div class="recommend-scroll">
        <div
          v-for="p in guessYouLikeProducts"
          :key="p.id"
          class="recommend-card"
          @click="$router.push(`/products/${p.id}`)"
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import api from '../api'

const loading = ref(true)
const categories = ref([])
const products = ref([])
const guessYouLikeProducts = ref([])
const recommendConfig = reactive({
  guessYouLikeEnabled: true,
  guessYouLikeCount: 6,
  viewedAlsoViewEnabled: true,
  viewedAlsoViewCount: 6,
})

onMounted(async () => {
  try {
    const [catRes, prodRes, cfgRes] = await Promise.all([
      api.get('/categories'),
      api.get('/products'),
      api.get('/recommendations/config').catch(() => null),
    ])
    if (catRes.data.code === 200) categories.value = (catRes.data.data || []).filter(c => c.parentId === 0)
    if (prodRes.data.code === 200) products.value = (prodRes.data.data || []).slice(0, 8)
    if (cfgRes && cfgRes.data && cfgRes.data.code === 200 && cfgRes.data.data) {
      Object.assign(recommendConfig, cfgRes.data.data)
    }

    if (recommendConfig.guessYouLikeEnabled) {
      try {
        const res = await api.get('/recommendations/home')
        if (res.data.code === 200) guessYouLikeProducts.value = res.data.data || []
      } catch {}
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home {
  padding-bottom: 48px;
}

.hero {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 48px;
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #0f766e 0%, #134e4a 50%, #0d9488 100%);
  opacity: 0.96;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 48px 24px;
}

.hero h1 {
  font-size: 2.25rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  letter-spacing: -0.03em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.hero p {
  font-size: 1.0625rem;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 28px;
}

.hero-cta {
  padding: 12px 28px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  background: #fff !important;
  color: var(--color-primary) !important;
  border: none !important;
}

.hero-cta:hover {
  background: rgba(255, 255, 255, 0.95) !important;
  color: var(--color-primary-hover) !important;
}

.section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 20px;
  letter-spacing: -0.02em;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.category-card {
  cursor: pointer;
  min-width: 120px;
  padding: 14px 20px;
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: all var(--transition);
}

.category-card:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  box-shadow: var(--shadow-md);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.product-card {
  cursor: pointer;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all var(--transition);
}

.product-card:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.product-img {
  height: 220px;
  background: var(--color-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-card:hover .product-img img {
  transform: scale(1.04);
}

.product-info {
  padding: 16px;
}

.product-name {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-primary);
  margin-top: 8px;
}

.recommend-section {
  margin-top: 8px;
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
</style>
