<template>
  <div class="product-list">
    <div class="toolbar">
      <el-select
        v-model="categoryId"
        placeholder="全部分类"
        clearable
        class="toolbar-select"
        @change="load"
      >
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="搜索商品"
        clearable
        class="toolbar-input"
        @keyup.enter="load"
      />
      <el-button type="primary" @click="load">搜索</el-button>
    </div>
    <div v-loading="loading" class="grid">
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
          <div class="product-subtitle">{{ p.subtitle }}</div>
          <div class="product-price">¥ {{ p.price }}</div>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && products.length === 0" description="暂无商品" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const loading = ref(true)
const categories = ref([])
const products = ref([])
const categoryId = ref(null)
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const params = {}
    if (categoryId.value) params.categoryId = categoryId.value
    if (keyword.value) params.keyword = keyword.value
    const res = await api.get('/products', { params })
    products.value = res.data.code === 200 ? (res.data.data || []) : []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const q = route.query
  if (q.categoryId) categoryId.value = Number(q.categoryId)
  if (q.keyword) keyword.value = q.keyword
  load()
  api.get('/categories').then((res) => {
    if (res.data.code === 200) categories.value = res.data.data || []
  })
})
</script>

<style scoped>
.product-list {
  padding-bottom: 32px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.toolbar-select {
  width: 180px;
}

.toolbar-input {
  width: 280px;
}

.grid {
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
  height: 240px;
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

.product-subtitle {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin-top: 4px;
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
</style>
