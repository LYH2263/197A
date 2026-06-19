<template>
  <div class="admin-products">
    <h2 class="page-title">商品运营中心</h2>

    <div class="filter-card content-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="商品名称">
          <el-input v-model="filterForm.name" placeholder="请输入商品名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filterForm.categoryId" placeholder="全部分类" clearable style="width: 180px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格区间">
          <div class="price-range">
            <el-input v-model.number="filterForm.minPrice" placeholder="最低价" clearable style="width: 100px" />
            <span class="price-sep">-</span>
            <el-input v-model.number="filterForm.maxPrice" placeholder="最高价" clearable style="width: 100px" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="success" :disabled="selectedIds.length === 0" @click="batchOnSale">
          批量上架
        </el-button>
        <el-button type="warning" :disabled="selectedIds.length === 0" @click="batchOffSale">
          批量下架
        </el-button>
        <el-button type="primary" :disabled="selectedIds.length === 0" @click="showCategoryDialog = true">
          批量改分类
        </el-button>
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="showPriceDialog = true">
          批量调价
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button @click="loadRecommendConfig(); showRecommendDialog = true">推荐位配置</el-button>
        <el-upload
          ref="uploadRef"
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="handleImport"
          accept=".csv"
          style="display: inline-block"
        >
          <el-button type="primary">导入商品</el-button>
        </el-upload>
        <el-button @click="handleExport">导出筛选结果</el-button>
        <el-button type="info" @click="downloadTemplate">下载模板</el-button>
      </div>
    </div>

    <div class="content-card" v-loading="loading">
      <el-table
        :data="tableData"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        stripe
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="subtitle" label="副标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="主图" width="100">
          <template #default="{ row }">
            <img
              :src="row.mainImage || '/images/default-product.svg'"
              class="product-thumb"
              @error="$event.target.src = '/images/default-product.svg'"
            />
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ getCategoryName(row.categoryId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="salesCount" label="销量" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-empty v-if="!loading && tableData.length === 0" description="暂无商品" />

    <el-dialog
      v-model="showCategoryDialog"
      title="批量修改分类"
      width="400px"
      @close="targetCategoryId = null"
    >
      <el-form label-width="80px">
        <el-form-item label="目标分类">
          <el-select v-model="targetCategoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCategoryDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmBatchCategory">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showPriceDialog"
      title="批量调价"
      width="400px"
      @close="priceRatio = null"
    >
      <el-form label-width="100px">
        <el-form-item label="调价比例">
          <el-input-number
            v-model="priceRatio"
            :min="0.01"
            :max="10"
            :step="0.1"
            :precision="2"
            placeholder="0-10之间，如0.9表示打9折"
            style="width: 100%"
          />
        </el-form-item>
        <p class="price-tip">
          新价格 = 原价 × 调价比例，保留两位小数，且不低于 0.01
        </p>
      </el-form>
      <template #footer>
        <el-button @click="showPriceDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmBatchPrice">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showImportResult" title="导入结果" width="500px">
      <div class="import-result">
        <p>成功导入：<span class="success-count">{{ importResult.successCount }}</span> 条</p>
        <p>失败：<span class="fail-count">{{ importResult.failCount }}</span> 条</p>
        <div v-if="importResult.errors && importResult.errors.length > 0" class="error-list">
          <h4>错误详情：</h4>
          <div v-for="(err, idx) in importResult.errors" :key="idx" class="error-item">
            <el-tag type="danger" size="small">第{{ err.rowNum }}行</el-tag>
            <span>{{ err.field }}：{{ err.message }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="showImportResult = false; loadData()">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRecommendDialog" title="推荐位配置" width="500px">
      <el-form :model="recommendForm" label-width="140px">
        <el-divider content-position="left">猜你喜欢</el-divider>
        <el-form-item label="启用">
          <el-switch v-model="recommendForm.guessYouLikeEnabled" />
        </el-form-item>
        <el-form-item label="展示数量">
          <el-input-number v-model="recommendForm.guessYouLikeCount" :min="1" :max="20" />
        </el-form-item>
        <el-divider content-position="left">看了又看</el-divider>
        <el-form-item label="启用">
          <el-switch v-model="recommendForm.viewedAlsoViewEnabled" />
        </el-form-item>
        <el-form-item label="展示数量">
          <el-input-number v-model="recommendForm.viewedAlsoViewCount" :min="1" :max="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRecommendDialog = false">取消</el-button>
        <el-button type="primary" :loading="recommendSaving" @click="saveRecommendConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const categories = ref([])
const selectedIds = ref([])
const showCategoryDialog = ref(false)
const showPriceDialog = ref(false)
const showImportResult = ref(false)
const showRecommendDialog = ref(false)
const recommendSaving = ref(false)
const targetCategoryId = ref(null)
const priceRatio = ref(null)
const uploadRef = ref(null)

const importResult = reactive({
  successCount: 0,
  failCount: 0,
  errors: []
})

const filterForm = reactive({
  name: '',
  categoryId: null,
  status: null,
  minPrice: null,
  maxPrice: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const recommendForm = reactive({
  guessYouLikeEnabled: true,
  guessYouLikeCount: 6,
  viewedAlsoViewEnabled: true,
  viewedAlsoViewCount: 6,
})

const categoryMap = computed(() => {
  const map = {}
  categories.value.forEach(c => {
    map[c.id] = c.name
  })
  return map
})

function getCategoryName(categoryId) {
  return categoryMap.value[categoryId] || '-'
}

onMounted(async () => {
  await loadCategories()
  await loadData()
})

async function loadCategories() {
  try {
    const res = await api.get('/admin/categories')
    if (res.data.code === 200) {
      categories.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      ...filterForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === undefined || params[key] === '') {
        delete params[key]
      }
    })
    const res = await api.get('/admin/products', { params })
    if (res.data.code === 200) {
      const data = res.data.data
      tableData.value = data.list || []
      pagination.total = data.total || 0
    } else {
      ElMessage.error(res.data.message || '加载失败')
    }
  } catch (e) {
    console.error('加载商品列表失败', e)
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.pageNum = 1
  loadData()
}

function resetFilter() {
  filterForm.name = ''
  filterForm.categoryId = null
  filterForm.status = null
  filterForm.minPrice = null
  filterForm.maxPrice = null
  pagination.pageNum = 1
  loadData()
}

function handleSizeChange(val) {
  pagination.pageSize = val
  loadData()
}

function handleCurrentChange(val) {
  pagination.pageNum = val
  loadData()
}

function handleSelectionChange(val) {
  selectedIds.value = val.map(item => item.id)
}

async function batchOnSale() {
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${selectedIds.value.length} 个商品上架？`,
      '确认操作',
      { type: 'warning' }
    )
    await doBatchOperation('status', { status: 1 })
  } catch (e) {}
}

async function batchOffSale() {
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${selectedIds.value.length} 个商品下架？`,
      '确认操作',
      { type: 'warning' }
    )
    await doBatchOperation('status', { status: 0 })
  } catch (e) {}
}

async function confirmBatchCategory() {
  if (!targetCategoryId.value) {
    ElMessage.warning('请选择分类')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${selectedIds.value.length} 个商品分类修改为 ${getCategoryName(targetCategoryId.value)}？`,
      '确认操作',
      { type: 'warning' }
    )
    showCategoryDialog.value = false
    await doBatchOperation('category', { categoryId: targetCategoryId.value })
    targetCategoryId.value = null
  } catch (e) {}
}

async function confirmBatchPrice() {
  if (!priceRatio.value) {
    ElMessage.warning('请输入调价比例')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${selectedIds.value.length} 个商品按比例 ${priceRatio.value} 调价？\n新价格 = 原价 × ${priceRatio.value}`,
      '确认操作',
      { type: 'warning' }
    )
    showPriceDialog.value = false
    await doBatchOperation('price', { priceRatio: priceRatio.value })
    priceRatio.value = null
  } catch (e) {}
}

async function doBatchOperation(type, data) {
  submitting.value = true
  try {
    const url = `/admin/products/batch/${type}`
    const payload = { productIds: selectedIds.value, ...data }
    const res = await api.post(url, payload)
    if (res.data.code === 200) {
      const result = res.data.data
      let message = `操作完成：成功 ${result.successCount} 条`
      if (result.failCount > 0) {
        message += `，失败 ${result.failCount} 条`
        if (result.failReasons && result.failReasons.length > 0) {
          message += `\n失败原因：\n${result.failReasons.slice(0, 5).join('\n')}`
          if (result.failReasons.length > 5) {
            message += `\n...还有 ${result.failReasons.length - 5} 条错误`
          }
        }
      }
      if (result.failCount > 0) {
        ElMessage.warning(message)
      } else {
        ElMessage.success(message)
      }
      selectedIds.value = []
      loadData()
    }
  } catch (e) {
    console.error('批量操作失败', e)
  } finally {
    submitting.value = false
  }
}

function beforeUpload(file) {
  const isCSV = file.name.toLowerCase().endsWith('.csv')
  if (!isCSV) {
    ElMessage.error('仅支持CSV文件')
    return false
  }
  return true
}

async function handleImport(options) {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const res = await api.post('/admin/products/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.code === 200) {
      const data = res.data.data
      importResult.successCount = data.successCount || 0
      importResult.failCount = data.failCount || 0
      importResult.errors = data.errors || []
      showImportResult.value = true
    }
  } catch (e) {
    console.error('导入失败', e)
  }
}

function handleExport() {
  const params = { ...filterForm }
  Object.keys(params).forEach(key => {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key]
    }
  })
  const queryString = new URLSearchParams(params).toString()
  const url = `/api/admin/products/export${queryString ? '?' + queryString : ''}`
  window.open(url, '_blank')
}

function downloadTemplate() {
  const content = 'name,subtitle,categoryId,price,stock,mainImage,detail,status\n示例商品,示例副标题,1,99.99,100,/images/default-product.svg,商品详情,1'
  const blob = new Blob(['\ufeff' + content], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '商品导入模板.csv'
  link.click()
  URL.revokeObjectURL(url)
}

async function loadRecommendConfig() {
  try {
    const res = await api.get('/recommendations/config')
    if (res.data.code === 200 && res.data.data) {
      Object.assign(recommendForm, res.data.data)
    }
  } catch (e) {
    console.error('加载推荐配置失败', e)
  }
}

async function saveRecommendConfig() {
  recommendSaving.value = true
  try {
    await api.put('/admin/recommendations/config', recommendForm)
    ElMessage.success('推荐配置已保存')
    showRecommendDialog.value = false
  } catch (e) {
    console.error('保存推荐配置失败', e)
  } finally {
    recommendSaving.value = false
  }
}
</script>

<style scoped>
.admin-products {
  padding-bottom: 48px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-form {
  margin: 0;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-sep {
  color: var(--color-text-muted);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.product-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.price {
  color: var(--color-danger);
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.price-tip {
  color: var(--color-text-muted);
  font-size: 12px;
  margin: 0;
}

.import-result p {
  margin: 8px 0;
  font-size: 14px;
}

.success-count {
  color: var(--color-success);
  font-weight: 600;
  font-size: 18px;
}

.fail-count {
  color: var(--color-danger);
  font-weight: 600;
  font-size: 18px;
}

.error-list {
  margin-top: 16px;
  max-height: 300px;
  overflow-y: auto;
}

.error-list h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: var(--color-text-muted);
}

.error-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: var(--color-bg-light);
  border-radius: 4px;
  margin-bottom: 8px;
  font-size: 13px;
}
</style>
