<template>
  <div class="cart-page">
    <h2 class="page-title">购物车</h2>
    <div v-loading="loading" class="cart-content content-card">
      <template v-if="items.length">
        <el-table ref="tableRef" :data="items" :row-key="(r) => r.productId" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="50" :reserve-selection="false" />
          <el-table-column label="商品" min-width="280">
            <template #default="{ row }">
              <div class="cart-product">
                <img
                  :src="row.productImage || '/images/default-product.svg'"
                  alt=""
                  class="cart-product-img"
                  @error="$event.target.src = '/images/default-product.svg'"
                />
                <span class="name">{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">¥ {{ row.price }}</template>
          </el-table-column>
          <el-table-column label="数量" width="160">
            <template #default="{ row }">
              <el-input-number
                :model-value="row.quantity"
                :min="1"
                :max="row.stock"
                size="small"
                @update:model-value="(v) => updateQuantity(row, v)"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120">
            <template #default="{ row }">¥ {{ row.totalAmount }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button type="danger" link @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="cart-footer">
          <span class="cart-footer-text">已选 {{ checkedCount }} 件，合计：<strong>¥ {{ total }}</strong></span>
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import api from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const items = ref([])
const selected = ref([])
const tableRef = ref(null)

const checkedCount = computed(() => selected.value.length)
const total = computed(() => {
  return selected.value.reduce((s, i) => s + Number(i.totalAmount || 0), 0).toFixed(2)
})

async function onSelectionChange(rows) {
  const prevIds = new Set(selected.value.map((r) => r.productId))
  selected.value = rows
  for (const row of items.value) {
    const nowChecked = rows.some((r) => r.productId === row.productId) ? 1 : 0
    if (row.checked !== nowChecked) {
      try {
        await api.put('/cart/checked', { productId: row.productId, checked: nowChecked })
        row.checked = nowChecked
      } catch (e) {}
    }
  }
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/cart')
    items.value = res.data.code === 200 ? res.data.data || [] : []
    userStore.cartCount = items.value.reduce((s, i) => s + (i.quantity || 0), 0)
    nextTick(() => {
      items.value.filter((i) => i.checked === 1).forEach((row) => tableRef.value?.toggleRowSelection(row, true))
    })
  } finally {
    loading.value = false
  }
}

async function updateQuantity(row, val) {
  try {
    await api.put('/cart/quantity', { productId: row.productId, quantity: val })
    row.quantity = val
    row.totalAmount = (Number(row.price) * val).toFixed(2)
    userStore.cartCount = items.value.reduce((s, i) => s + (i.quantity || 0), 0)
  } catch (e) {}
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该商品？', '提示')
    await api.delete(`/cart/${row.productId}`)
    items.value = items.value.filter((i) => i.productId !== row.productId)
    userStore.cartCount = items.value.reduce((s, i) => s + (i.quantity || 0), 0)
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

function goCheckout() {
  if (selected.value.length === 0) return
  router.push({ path: '/orders', query: { checkout: '1' } })
}

onMounted(load)
</script>

<style scoped>
.cart-page {
  padding-bottom: 32px;
}

.cart-content {
  padding: 24px;
}

.cart-product {
  display: flex;
  align-items: center;
  gap: 14px;
}

.cart-product-img {
  width: 56px;
  height: 56px;
  object-fit: contain;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
}

.cart-product .name {
  font-weight: 500;
  color: var(--color-text);
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
  color: var(--color-primary);
  font-size: 1.125rem;
}
</style>
