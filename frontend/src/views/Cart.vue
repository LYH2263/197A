<template>
  <div class="cart-page">
    <h2 class="page-title">购物车</h2>
    <div v-loading="loading" class="cart-content content-card">
      <template v-if="items.length">
        <el-table
          ref="tableRef"
          :data="items"
          :row-key="(r) => r.productId"
          :row-class-name="getRowClassName"
          @selection-change="onSelectionChange"
          @select-all="onSelectAll"
          @select="onSelect"
        >
          <el-table-column type="selection" width="50" :reserve-selection="false" :selectable="isRowSelectable" />
          <el-table-column label="商品" min-width="320">
            <template #default="{ row }">
              <div class="cart-product">
                <img
                  :src="row.productImage || '/images/default-product.svg'"
                  alt=""
                  class="cart-product-img"
                  :class="{ 'img-grayscale': row.offline }"
                  @error="$event.target.src = '/images/default-product.svg'"
                />
                <div class="product-info">
                  <div class="name-row">
                    <span class="name" :class="{ 'text-muted': row.offline }">{{ row.productName }}</span>
                    <el-tag v-if="row.offline" type="info" size="small" effect="dark" class="status-tag">已失效</el-tag>
                    <el-tag v-else-if="row.stockInsufficient" type="danger" size="small" effect="light" class="status-tag">库存不足</el-tag>
                    <el-tag
                      v-else-if="!row.offline && row.priceDiff && row.priceDiff !== 0"
                      :type="row.priceRaised ? 'danger' : 'success'"
                      size="small"
                      effect="light"
                      class="status-tag price-tag"
                    >
                      {{ row.priceRaised ? '↑ 涨' : '↓ 跌' }} ¥{{ formatDiff(row.priceDiff) }}
                    </el-tag>
                  </div>
                  <div v-if="!row.offline && row.priceDiff && row.priceDiff !== 0" class="price-change" :class="row.priceRaised ? 'up' : 'down'">
                    比加入时{{ row.priceRaised ? '涨' : '跌' }}
                    <span class="diff">¥ {{ formatDiff(row.priceDiff) }}</span>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="140">
            <template #default="{ row }">
              <div class="price-col">
                <span class="current-price">¥ {{ row.price }}</span>
                <span v-if="!row.offline && row.priceSnapshot && row.priceSnapshot !== row.price" class="snapshot-price">
                  加入时 ¥ {{ row.priceSnapshot }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="180">
            <template #default="{ row }">
              <el-input-number
                :model-value="row.quantity"
                :min="1"
                :max="row.offline ? row.quantity : (row.stock > 0 ? row.stock : 1)"
                :disabled="row.offline"
                size="small"
                @update:model-value="(v) => onQuantityChange(row, v)"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="130">
            <template #default="{ row }">
              <span class="subtotal" :class="{ 'text-muted': row.offline }">¥ {{ row.totalAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <div class="action-col">
                <el-button
                  v-if="row.offline"
                  type="warning"
                  size="small"
                  @click="moveToSaveForLater(row)"
                >移入稍后购买</el-button>
                <el-button
                  v-if="row.offline"
                  type="danger"
                  size="small"
                  @click="remove(row)"
                >删除</el-button>
                <template v-else>
                  <el-button type="warning" link @click="moveToSaveForLater(row)">稍后购买</el-button>
                  <el-button type="danger" link @click="remove(row)">删除</el-button>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="cart-footer">
          <span class="cart-footer-text">
            已选 <strong>{{ checkedCount }}</strong> 件
            <template v-if="invalidCount > 0">（失效 {{ invalidCount }} 件已自动跳过）</template>
            ，合计：<strong class="total">¥ {{ total }}</strong>
          </span>
          <el-button type="primary" :disabled="checkedCount === 0" @click="goCheckout">
            去结算
          </el-button>
        </div>
      </template>
      <el-empty v-else description="购物车是空的，去逛逛吧">
        <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import cartService from '../services/CartService'
import api from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const items = ref([])
const selected = ref([])
const tableRef = ref(null)

const validItems = computed(() => items.value.filter(i => !i.offline))
const invalidCount = computed(() => items.value.filter(i => i.offline).length)
const checkedCount = computed(() => selected.value.filter(s => !s.offline).length)
const total = computed(() => {
  return selected.value
    .filter(s => !s.offline)
    .reduce((s, i) => s + Number(i.totalAmount || 0), 0)
    .toFixed(2)
})

function formatDiff(val) {
  const abs = Math.abs(Number(val)).toFixed(2)
  return abs
}

function getRowClassName({ row }) {
  if (row.offline) return 'cart-row-offline'
  if (row.stockInsufficient) return 'cart-row-stock-insufficient'
  return ''
}

function isRowSelectable(row) {
  return !row.offline
}

function onSelectAll(selection) {
  nextTick(() => {
    const offlineRows = items.value.filter(r => r.offline)
    for (const row of offlineRows) {
      tableRef.value?.toggleRowSelection(row, false)
    }
    const finalSelection = tableRef.value?.getSelectionRows() || []
    selected.value = finalSelection
    persistChecked(finalSelection)
  })
}

function onSelect(selection, row) {
  if (row.offline) {
    nextTick(() => tableRef.value?.toggleRowSelection(row, false))
    return
  }
  selected.value = selection.filter(r => !r.offline)
  persistChecked(selected.value)
}

async function persistChecked(rows) {
  const checkedIds = new Set(rows.map(r => r.productId))
  for (const row of items.value) {
    if (row.offline) continue
    const nowChecked = checkedIds.has(row.productId) ? 1 : 0
    if (row.checked !== nowChecked) {
      try {
        await cartService.setChecked(row.productId, nowChecked)
        row.checked = nowChecked
      } catch (e) {}
    }
  }
}

async function onSelectionChange(rows) {
  const filtered = rows.filter(r => !r.offline)
  if (filtered.length !== rows.length) {
    nextTick(() => {
      const offlineRows = items.value.filter(r => r.offline && rows.some(x => x.productId === r.productId))
      for (const row of offlineRows) {
        tableRef.value?.toggleRowSelection(row, false)
      }
    })
  }
  const prevIds = new Set(selected.value.map(r => r.productId))
  selected.value = filtered
  const checkedIds = new Set(filtered.map(r => r.productId))
  for (const row of items.value) {
    if (row.offline) continue
    const nowChecked = checkedIds.has(row.productId) ? 1 : 0
    if (row.checked !== nowChecked) {
      try {
        await cartService.setChecked(row.productId, nowChecked)
        row.checked = nowChecked
      } catch (e) {}
    }
  }
}

async function load() {
  loading.value = true
  try {
    const res = await cartService.list()
    items.value = res.data.code === 200 ? res.data.data || [] : []
    userStore.cartCount = items.value.reduce((s, i) => s + (i.quantity || 0), 0)
    nextTick(() => {
      for (const row of items.value) {
        if (row.checked === 1 && !row.offline) {
          tableRef.value?.toggleRowSelection(row, true)
        }
      }
      selected.value = tableRef.value?.getSelectionRows() || items.value.filter(i => i.checked === 1 && !i.offline)
    })
  } finally {
    loading.value = false
  }
}

function updateLocalRow(row, newData) {
  const idx = items.value.findIndex(i => i.productId === row.productId)
  if (idx !== -1) {
    items.value[idx] = { ...items.value[idx], ...newData }
    if (items.value[idx].offline) {
      nextTick(() => tableRef.value?.toggleRowSelection(items.value[idx], false))
    }
  }
}

async function onQuantityChange(row, val) {
  row.quantity = val
  row.totalAmount = (Number(row.price) * val).toFixed(2)
  userStore.cartCount = items.value.reduce((s, i) => s + (i.quantity || 0), 0)
  try {
    const res = await cartService.updateQuantity(row.productId, val)
    if (res.data.code === 200 && res.data.data) {
      updateLocalRow(row, res.data.data)
    }
  } catch (e) {}
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(row.offline ? '该商品已失效，确定删除？' : '确定删除该商品？', '提示')
    cartService.flushAll()
    await cartService.remove(row.productId)
    items.value = items.value.filter(i => i.productId !== row.productId)
    selected.value = selected.value.filter(s => s.productId !== row.productId)
    userStore.cartCount = items.value.reduce((s, i) => s + (i.quantity || 0), 0)
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

async function moveToSaveForLater(row) {
  try {
    cartService.flushAll()
    const res = await cartService.moveToSaveForLater(row.productId)
    items.value = items.value.filter(i => i.productId !== row.productId)
    selected.value = selected.value.filter(s => s.productId !== row.productId)
    userStore.cartCount = items.value.reduce((s, i) => s + (i.quantity || 0), 0)
    if (res.data.code === 200 && res.data.data?.message) {
      ElMessage.warning(res.data.data.message)
    }
    try {
      const countRes = await api.get('/save-for-later/count')
      if (countRes.data.code === 200) {
        userStore.saveForLaterCount = countRes.data.data || 0
      }
    } catch {}
  } catch (e) {}
}

function goCheckout() {
  if (checkedCount.value === 0) return
  cartService.flushAll()
  router.push({ path: '/orders', query: { checkout: '1' } })
}

onMounted(load)
onBeforeUnmount(() => {
  cartService.flushAll()
})
</script>

<style scoped>
.cart-page {
  padding-bottom: 32px;
}

.cart-content {
  padding: 24px;
}

:deep(.cart-row-stock-insufficient) {
  --el-table-tr-bg-color: #fef0f0;
  background-color: #fef0f0;
}

:deep(.cart-row-stock-insufficient:hover > td) {
  background-color: #fde2e2 !important;
}

:deep(.cart-row-offline) {
  --el-table-tr-bg-color: #f5f7fa;
  background-color: #f5f7fa;
}

:deep(.cart-row-offline:hover > td) {
  background-color: #ebeef5 !important;
}

:deep(.cart-row-offline .el-checkbox) {
  opacity: 0.5;
}

.cart-product {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.cart-product-img {
  width: 64px;
  height: 64px;
  object-fit: contain;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  flex-shrink: 0;
}

.cart-product-img.img-grayscale {
  filter: grayscale(100%);
  opacity: 0.7;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.name {
  font-weight: 500;
  color: var(--color-text);
  line-height: 1.4;
}

.name.text-muted {
  color: #909399;
  text-decoration: line-through;
}

.status-tag {
  flex-shrink: 0;
}

.price-tag {
  font-weight: 600;
  padding: 2px 8px;
  letter-spacing: 0.5px;
}

.price-change {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
  align-self: flex-start;
}

.price-change.up {
  background-color: #fef0f0;
  color: #f56c6c;
}

.price-change.down {
  background-color: #f0f9eb;
  color: #67c23a;
}

.price-change .diff {
  font-weight: 600;
  margin-left: 2px;
}

.price-col {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.current-price {
  font-weight: 600;
  color: var(--color-primary);
  font-size: 15px;
}

.snapshot-price {
  font-size: 12px;
  color: #909399;
  text-decoration: line-through;
}

.subtotal {
  font-weight: 600;
  color: var(--color-text);
}

.subtotal.text-muted {
  color: #909399;
  text-decoration: line-through;
}

.action-col {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.cart-footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cart-footer-text {
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
}

.cart-footer-text strong {
  color: var(--color-text);
}

.cart-footer-text .total {
  color: var(--color-primary);
  font-size: 1.25rem;
}
</style>
