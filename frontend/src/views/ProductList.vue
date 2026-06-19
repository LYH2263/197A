<template>
  <div class="product-list">
    <div class="toolbar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索商品"
        clearable
        class="toolbar-input"
        @keyup.enter="applyFilters"
      />
      <el-button type="primary" @click="applyFilters">搜索</el-button>
      <el-button @click="toggleFilterPanel">
        <el-icon><Filter /></el-icon>
        高级筛选
      </el-button>
      <el-select
        v-model="filters.sortBy"
        placeholder="排序方式"
        class="toolbar-select"
        @change="applyFilters"
      >
        <el-option label="默认排序" value="" />
        <el-option label="价格从低到高" value="price_asc" />
        <el-option label="价格从高到低" value="price_desc" />
        <el-option label="销量优先" value="sales" />
        <el-option label="最新上架" value="new_arrivals" />
        <el-option label="评价数优先" value="reviews" />
      </el-select>
    </div>

    <div v-if="activeTags.length > 0" class="active-tags">
      <span class="active-tags-label">已选条件：</span>
      <el-tag
        v-for="tag in activeTags"
        :key="tag.key"
        closable
        type="info"
        class="filter-tag"
        @close="removeTag(tag.key)"
      >
        {{ tag.label }}
      </el-tag>
      <el-button link type="primary" size="small" @click="clearAllFilters">清空全部</el-button>
    </div>

    <el-collapse-transition>
      <div v-show="filterPanelVisible" class="filter-panel">
        <el-row :gutter="24">
          <el-col :span="12" :md="6">
            <div class="filter-group">
              <div class="filter-label">价格区间</div>
              <el-slider
                v-model="priceRange"
                range
                :min="0"
                :max="10000"
                :step="100"
                show-stops
                show-input
                :format-tooltip="(val) => '¥' + val"
              />
              <div class="price-display">
                ¥{{ priceRange[0] }} - ¥{{ priceRange[1] }}
              </div>
            </div>
          </el-col>
          <el-col :span="12" :md="6">
            <div class="filter-group">
              <div class="filter-label">商品分类</div>
              <el-tree
                ref="categoryTreeRef"
                :data="categoryTree"
                :props="{ label: 'name', children: 'children' }"
                node-key="id"
                :expand-on-click-node="false"
                :check-strictly="true"
                show-checkbox
                :default-checked-keys="filters.categoryId ? [filters.categoryId] : []"
                @check="handleCategoryCheck"
                class="category-tree"
              />
            </div>
          </el-col>
          <el-col :span="12" :md="6">
            <div class="filter-group">
              <div class="filter-label">库存状态</div>
              <el-radio-group v-model="filters.stockStatus" @change="applyFilters">
                <el-radio value="">全部</el-radio>
                <el-radio value="in_stock">有货</el-radio>
                <el-radio value="out_of_stock">缺货</el-radio>
                <el-radio value="pre_sale">预售</el-radio>
              </el-radio-group>
            </div>
          </el-col>
          <el-col :span="12" :md="6">
            <div class="filter-group">
              <div class="filter-label">上架时间</div>
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                @change="applyFilters"
              />
            </div>
          </el-col>
        </el-row>
      </div>
    </el-collapse-transition>

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
            class="product-img--main"
            @error="$event.target.src = '/images/default-product.svg'"
          />
          <img
            v-if="getSecondImage(p)"
            :src="getSecondImage(p)"
            alt=""
            class="product-img--hover"
            @error="$event.target.src = '/images/default-product.svg'"
          />
          <div v-if="p.stock <= 0" class="stock-badge" :class="{ presale: p.stock < 0 }">
            {{ p.stock < 0 ? '预售' : '缺货' }}
          </div>
        </div>
        <div class="product-info">
          <div class="product-name">{{ p.name }}</div>
          <div class="product-subtitle">{{ p.subtitle }}</div>
          <div class="product-price">¥ {{ p.price }}</div>
          <div class="product-meta">
            <span>销量 {{ p.salesCount || 0 }}</span>
            <span>评价 {{ p.reviewCount || 0 }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && products.length === 0" class="empty-wrapper">
      <el-empty description="暂无符合条件的商品">
        <div class="empty-suggestions">
          <p>试试以下建议放宽筛选条件：</p>
          <ul>
            <li v-if="filters.keyword">
              <el-button link type="primary" @click="removeTag('keyword')">
                清除关键词「{{ filters.keyword }}」
              </el-button>
            </li>
            <li v-if="filters.categoryId">
              <el-button link type="primary" @click="removeTag('categoryId')">
                清除分类筛选
              </el-button>
            </li>
            <li v-if="filters.minPrice != null || filters.maxPrice != null">
              <el-button link type="primary" @click="removeTag('price')">
                清除价格区间限制
              </el-button>
            </li>
            <li v-if="filters.stockStatus">
              <el-button link type="primary" @click="removeTag('stockStatus')">
                清除库存状态筛选
              </el-button>
            </li>
            <li v-if="filters.startDate || filters.endDate">
              <el-button link type="primary" @click="removeTag('date')">
                清除上架时间限制
              </el-button>
            </li>
            <li>
              <el-button link type="primary" @click="clearAllFilters">
                重置所有筛选条件
              </el-button>
            </li>
          </ul>
        </div>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Filter } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const filterPanelVisible = ref(true)
const categoryTreeRef = ref(null)
const categories = ref([])
const products = ref([])
const priceRange = ref([0, 10000])
const dateRange = ref([])

const filters = reactive({
  keyword: '',
  categoryId: null,
  minPrice: null,
  maxPrice: null,
  stockStatus: '',
  startDate: '',
  endDate: '',
  sortBy: '',
  sortOrder: ''
})

const categoryTree = computed(() => {
  const map = {}
  const roots = []
  categories.value.forEach((c) => {
    map[c.id] = { ...c, children: [] }
  })
  categories.value.forEach((c) => {
    if (c.parentId && map[c.parentId]) {
      map[c.parentId].children.push(map[c.id])
    } else if (!c.parentId || c.parentId === 0) {
      roots.push(map[c.id])
    }
  })
  return roots
})

const activeTags = computed(() => {
  const tags = []
  if (filters.keyword) {
    tags.push({ key: 'keyword', label: `关键词：${filters.keyword}` })
  }
  if (filters.categoryId) {
    const name = findCategoryName(filters.categoryId)
    tags.push({ key: 'categoryId', label: `分类：${name || '已选'}` })
  }
  if (filters.minPrice != null || filters.maxPrice != null) {
    const min = filters.minPrice ?? 0
    const max = filters.maxPrice ?? '不限'
    tags.push({ key: 'price', label: `价格：¥${min} - ¥${max}` })
  }
  if (filters.stockStatus) {
    const map = { in_stock: '有货', out_of_stock: '缺货', pre_sale: '预售' }
    tags.push({ key: 'stockStatus', label: `库存：${map[filters.stockStatus]}` })
  }
  if (filters.startDate || filters.endDate) {
    tags.push({ key: 'date', label: `上架时间：${filters.startDate || '不限'} 至 ${filters.endDate || '不限'}` })
  }
  return tags
})

function findCategoryName(id) {
  const c = categories.value.find((x) => x.id === id)
  return c ? c.name : ''
}

function toggleFilterPanel() {
  filterPanelVisible.value = !filterPanelVisible.value
}

function handleCategoryCheck(data, checkedInfo) {
  const keys = checkedInfo.checkedKeys.filter((k) => typeof k === 'number')
  if (keys.length > 0) {
    filters.categoryId = keys[0]
  } else {
    filters.categoryId = null
  }
  applyFilters()
}

function applyFilters() {
  if (filters.sortBy === 'price_asc') {
    filters.sortBy = 'price'
    filters.sortOrder = 'asc'
  } else if (filters.sortBy === 'price_desc') {
    filters.sortBy = 'price'
    filters.sortOrder = 'desc'
  } else if (filters.sortBy === 'sales' || filters.sortBy === 'new_arrivals' || filters.sortBy === 'reviews') {
    filters.sortOrder = ''
  } else {
    filters.sortBy = ''
    filters.sortOrder = ''
  }

  filters.minPrice = priceRange.value[0] > 0 ? priceRange.value[0] : null
  filters.maxPrice = priceRange.value[1] < 10000 ? priceRange.value[1] : null

  if (dateRange.value && dateRange.value.length === 2) {
    filters.startDate = dateRange.value[0]
    filters.endDate = dateRange.value[1]
  } else {
    filters.startDate = ''
    filters.endDate = ''
  }

  syncToUrl()
  load()
}

function syncToUrl() {
  const query = {}
  if (filters.keyword) query.keyword = filters.keyword
  if (filters.categoryId) query.categoryId = filters.categoryId
  if (filters.minPrice != null) query.minPrice = filters.minPrice
  if (filters.maxPrice != null) query.maxPrice = filters.maxPrice
  if (filters.stockStatus) query.stockStatus = filters.stockStatus
  if (filters.startDate) query.startDate = filters.startDate
  if (filters.endDate) query.endDate = filters.endDate
  if (filters.sortBy) {
    if (filters.sortBy === 'price') {
      query.sortBy = filters.sortOrder === 'asc' ? 'price_asc' : 'price_desc'
    } else {
      query.sortBy = filters.sortBy
    }
  }
  router.replace({ query })
}

function restoreFromUrl() {
  const q = route.query
  if (q.keyword) filters.keyword = q.keyword
  if (q.categoryId) filters.categoryId = Number(q.categoryId)
  if (q.minPrice != null) filters.minPrice = Number(q.minPrice)
  if (q.maxPrice != null) filters.maxPrice = Number(q.maxPrice)
  if (q.stockStatus) filters.stockStatus = q.stockStatus
  if (q.startDate) filters.startDate = q.startDate
  if (q.endDate) filters.endDate = q.endDate
  if (q.sortBy) {
    filters.sortBy = q.sortBy
    if (q.sortBy === 'price_asc' || q.sortBy === 'price_desc') {
      filters.sortOrder = q.sortBy === 'price_asc' ? 'asc' : 'desc'
    }
  }

  priceRange.value = [filters.minPrice ?? 0, filters.maxPrice ?? 10000]
  if (filters.startDate && filters.endDate) {
    dateRange.value = [filters.startDate, filters.endDate]
  }
}

function removeTag(key) {
  switch (key) {
    case 'keyword':
      filters.keyword = ''
      break
    case 'categoryId':
      filters.categoryId = null
      if (categoryTreeRef.value) {
        categoryTreeRef.value.setCheckedKeys([])
      }
      break
    case 'price':
      filters.minPrice = null
      filters.maxPrice = null
      priceRange.value = [0, 10000]
      break
    case 'stockStatus':
      filters.stockStatus = ''
      break
    case 'date':
      filters.startDate = ''
      filters.endDate = ''
      dateRange.value = []
      break
  }
  applyFilters()
}

function clearAllFilters() {
  filters.keyword = ''
  filters.categoryId = null
  filters.minPrice = null
  filters.maxPrice = null
  filters.stockStatus = ''
  filters.startDate = ''
  filters.endDate = ''
  filters.sortBy = ''
  filters.sortOrder = ''
  priceRange.value = [0, 10000]
  dateRange.value = []
  if (categoryTreeRef.value) {
    categoryTreeRef.value.setCheckedKeys([])
  }
  applyFilters()
}

async function load() {
  loading.value = true
  try {
    const params = {}
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.categoryId) params.categoryId = filters.categoryId
    if (filters.minPrice != null) params.minPrice = filters.minPrice
    if (filters.maxPrice != null) params.maxPrice = filters.maxPrice
    if (filters.stockStatus) params.stockStatus = filters.stockStatus
    if (filters.startDate) params.startDate = filters.startDate + ' 00:00:00'
    if (filters.endDate) params.endDate = filters.endDate + ' 23:59:59'
    if (filters.sortBy) params.sortBy = filters.sortBy
    if (filters.sortOrder) params.sortOrder = filters.sortOrder
    const res = await api.get('/products', { params })
    products.value = res.data.code === 200 ? (res.data.data || []) : []
  } catch (e) {
    ElMessage.error('加载商品失败')
    products.value = []
  } finally {
    loading.value = false
  }
}

function getSecondImage(p) {
  if (p.images && p.images.length > 1) {
    const second = p.images.find((img) => img.isMain !== 1 && img.imageUrl !== p.mainImage)
    return second ? second.imageUrl : p.images[1].imageUrl
  }
  return null
}

onMounted(async () => {
  restoreFromUrl()
  try {
    const res = await api.get('/categories')
    if (res.data.code === 200) {
      categories.value = res.data.data || []
    }
  } catch (e) {
    // ignore
  }
  await nextTick()
  if (filters.categoryId && categoryTreeRef.value) {
    categoryTreeRef.value.setCheckedKeys([filters.categoryId])
  }
  load()
})
</script>

<style scoped>
.product-list {
  padding-bottom: 32px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.toolbar-select {
  width: 180px;
}

.toolbar-input {
  width: 280px;
}

.active-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 12px 16px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.active-tags-label {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.filter-tag {
  margin: 0;
}

.filter-panel {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 20px;
  margin-bottom: 20px;
}

.filter-group {
  margin-bottom: 8px;
}

.filter-label {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 12px;
}

.price-display {
  margin-top: 8px;
  font-size: 0.875rem;
  color: var(--color-primary);
  font-weight: 500;
  text-align: center;
}

.category-tree {
  max-height: 220px;
  overflow-y: auto;
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  padding: 8px;
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
  position: relative;
}

.product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-img--hover {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.product-card:hover .product-img--hover {
  opacity: 1;
}

.product-card:hover .product-img img {
  transform: scale(1.04);
}

.stock-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 0.75rem;
  z-index: 2;
}

.stock-badge.presale {
  background: rgba(234, 88, 12, 0.9);
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

.product-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.empty-wrapper {
  padding: 40px 0;
}

.empty-suggestions {
  margin-top: 16px;
  text-align: left;
  max-width: 360px;
  margin-left: auto;
  margin-right: auto;
}

.empty-suggestions p {
  font-size: 0.9375rem;
  color: var(--color-text);
  margin-bottom: 12px;
}

.empty-suggestions ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.empty-suggestions li {
  padding: 4px 0;
}
</style>
