<template>
  <div class="sfl-page">
    <h2 class="page-title">稍后购买</h2>
    <div v-loading="loading" class="sfl-content content-card">
      <template v-if="items.length">
        <div class="sfl-toolbar">
          <div class="sfl-toolbar-left">
            <el-checkbox v-model="selectAll" @change="onSelectAllChange">全选</el-checkbox>
            <el-button size="small" :disabled="selectedIds.length === 0" @click="batchMoveBack">
              移回购物车（{{ selectedIds.length }}）
            </el-button>
            <el-button size="small" type="danger" :disabled="selectedIds.length === 0" @click="batchDelete">
              批量删除（{{ selectedIds.length }}）
            </el-button>
          </div>
          <div class="sfl-toolbar-right">
            <span class="sort-label">排序：</span>
            <el-radio-group v-model="sortBy" size="small" @change="load">
              <el-radio-button value="time">加入时间</el-radio-button>
              <el-radio-button value="price">价格</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="sfl-list">
          <div
            v-for="item in items"
            :key="item.productId"
            class="sfl-item"
            :class="{ 'price-dropped': item.priceDropped }"
          >
            <div class="sfl-item-check">
              <el-checkbox
                :model-value="selectedIds.includes(item.productId)"
                @change="(v) => toggleSelect(item.productId, v)"
              />
            </div>
            <div class="sfl-item-product">
              <img
                :src="item.productImage || '/images/default-product.svg'"
                alt=""
                class="sfl-item-img"
                @error="$event.target.src = '/images/default-product.svg'"
              />
              <div class="sfl-item-info">
                <span class="name">{{ item.productName }}</span>
                <div class="price-row">
                  <span class="current-price">¥ {{ item.currentPrice }}</span>
                  <span v-if="item.priceDropped" class="price-drop-tag">
                    降价 ¥{{ item.priceDrop }}
                  </span>
                </div>
                <div v-if="item.priceDropped" class="price-snapshot-info">
                  移入时 ¥{{ item.priceSnapshot }} → 现在 ¥{{ item.currentPrice }}
                </div>
                <div class="quantity-info">数量：{{ item.quantity }}</div>
              </div>
            </div>
            <div class="sfl-item-alert">
              <template v-if="item.alertId">
                <el-tag size="small" type="warning">目标价 ¥{{ item.alertTargetPrice }}</el-tag>
                <el-button link size="small" @click="cancelAlert(item)">取消提醒</el-button>
              </template>
              <template v-else>
                <el-button link size="small" type="primary" @click="openAlertDialog(item)">
                  订阅降价提醒
                </el-button>
              </template>
            </div>
            <div class="sfl-item-actions">
              <el-button type="primary" link @click="moveBack(item)">移回购物车</el-button>
              <el-button type="danger" link @click="deleteItem(item)">删除</el-button>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无稍后购买的商品">
        <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
      </el-empty>
    </div>

    <el-dialog v-model="alertDialogVisible" title="订阅降价提醒" width="400px">
      <p>商品：{{ alertTargetItem?.productName }}</p>
      <p>当前价格：¥{{ alertTargetItem?.currentPrice }}</p>
      <el-form label-width="80px">
        <el-form-item label="目标价格">
          <el-input-number
            v-model="alertTargetPrice"
            :min="0.01"
            :precision="2"
            :step="1"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alertDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="subscribeAlert">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import api from '../api'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const loading = ref(true)
const items = ref([])
const selectedIds = ref([])
const sortBy = ref('time')
const alertDialogVisible = ref(false)
const alertTargetItem = ref(null)
const alertTargetPrice = ref(0)

const selectAll = computed({
  get: () => items.value.length > 0 && selectedIds.value.length === items.value.length,
  set: () => {}
})

async function load() {
  loading.value = true
  try {
    const res = await api.get('/save-for-later', { params: { sortBy: sortBy.value } })
    if (res.data.code === 200) {
      items.value = res.data.data || []
      userStore.saveForLaterCount = items.value.length
    }
  } finally {
    loading.value = false
  }
}

function onSelectAllChange(val) {
  selectedIds.value = val ? items.value.map((i) => i.productId) : []
}

function toggleSelect(productId, checked) {
  if (checked) {
    if (!selectedIds.value.includes(productId)) {
      selectedIds.value.push(productId)
    }
  } else {
    selectedIds.value = selectedIds.value.filter((id) => id !== productId)
  }
}

async function moveBack(item) {
  try {
    await api.post('/save-for-later/move-back', { productId: item.productId })
    items.value = items.value.filter((i) => i.productId !== item.productId)
    selectedIds.value = selectedIds.value.filter((id) => id !== item.productId)
    userStore.saveForLaterCount = items.value.length
    userStore.cartCount = (userStore.cartCount || 0) + item.quantity
    ElMessage.success('已移回购物车')
  } catch (e) {}
}

async function batchMoveBack() {
  try {
    await api.post('/save-for-later/batch-move-back', { productIds: selectedIds.value })
    const movedQuantity = items.value
      .filter((i) => selectedIds.value.includes(i.productId))
      .reduce((s, i) => s + i.quantity, 0)
    items.value = items.value.filter((i) => !selectedIds.value.includes(i.productId))
    userStore.saveForLaterCount = items.value.length
    userStore.cartCount = (userStore.cartCount || 0) + movedQuantity
    selectedIds.value = []
    ElMessage.success('已批量移回购物车')
  } catch (e) {}
}

async function deleteItem(item) {
  try {
    await ElMessageBox.confirm('确定删除该商品？', '提示')
    await api.delete(`/save-for-later/${item.productId}`)
    items.value = items.value.filter((i) => i.productId !== item.productId)
    selectedIds.value = selectedIds.value.filter((id) => id !== item.productId)
    userStore.saveForLaterCount = items.value.length
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

async function batchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 件商品？`, '提示')
    await api.post('/save-for-later/batch-delete', { productIds: selectedIds.value })
    items.value = items.value.filter((i) => !selectedIds.value.includes(i.productId))
    userStore.saveForLaterCount = items.value.length
    selectedIds.value = []
    ElMessage.success('已批量删除')
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

function openAlertDialog(item) {
  alertTargetItem.value = item
  alertTargetPrice.value = Number(item.currentPrice) - 1
  alertDialogVisible.value = true
}

async function subscribeAlert() {
  if (!alertTargetItem.value) return
  try {
    await api.post('/price-alert/subscribe', {
      productId: alertTargetItem.value.productId,
      targetPrice: alertTargetPrice.value
    })
    alertDialogVisible.value = false
    ElMessage.success('已订阅降价提醒')
    load()
  } catch (e) {}
}

async function cancelAlert(item) {
  try {
    await api.delete(`/price-alert/${item.productId}`)
    ElMessage.success('已取消降价提醒')
    load()
  } catch (e) {}
}

onMounted(load)
</script>

<style scoped>
.sfl-page {
  padding-bottom: 32px;
}

.sfl-content {
  padding: 24px;
}

.sfl-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.sfl-toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sfl-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sort-label {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.sfl-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sfl-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  transition: border-color 0.3s, background 0.3s;
}

.sfl-item.price-dropped {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.sfl-item-check {
  flex-shrink: 0;
}

.sfl-item-product {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.sfl-item-img {
  width: 56px;
  height: 56px;
  object-fit: contain;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  flex-shrink: 0;
}

.sfl-item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sfl-item-info .name {
  font-weight: 500;
  color: var(--color-text);
}

.price-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.current-price {
  color: var(--color-primary);
  font-weight: 600;
}

.price-drop-tag {
  background: #e6a23c;
  color: #fff;
  font-size: 0.75rem;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 500;
}

.price-snapshot-info {
  font-size: 0.75rem;
  color: #e6a23c;
}

.quantity-info {
  font-size: 0.8125rem;
  color: var(--color-text-secondary);
}

.sfl-item-alert {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.sfl-item-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
