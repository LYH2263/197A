<template>
  <div class="admin-images">
    <h2 class="page-title">商品图集管理</h2>
    <div class="toolbar">
      <el-select
        v-model="selectedProductId"
        placeholder="选择商品"
        filterable
        class="toolbar-select"
        @change="loadImages"
      >
        <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
    </div>

    <template v-if="selectedProductId">
      <div class="actions-bar">
        <el-button type="primary" @click="showAddDialog = true">添加图片</el-button>
        <el-button @click="showBulkDialog = true">批量导入 URL</el-button>
        <span class="image-count">当前 {{ images.length }} / 8 张</span>
      </div>

      <div v-loading="loadingImages" class="image-grid">
        <div
          v-for="(img, idx) in images"
          :key="img.id"
          class="image-card"
          :class="{ 'image-card--main': img.isMain === 1, 'image-card--drag': dragIndex === idx }"
          draggable="true"
          @dragstart="onDragStart(idx, $event)"
          @dragover.prevent="onDragOver(idx)"
          @drop="onDrop(idx)"
          @dragend="onDragEnd"
        >
          <div class="image-card-img">
            <img :src="img.imageUrl" alt="" @error="$event.target.src = '/images/default-product.svg'" />
          </div>
          <div class="image-card-actions">
            <el-tag v-if="img.isMain === 1" type="success" size="small">主图</el-tag>
            <el-button v-else type="primary" link size="small" @click="setMain(img.id)">设为主图</el-button>
            <el-button type="danger" link size="small" @click="deleteImage(img.id)">删除</el-button>
          </div>
          <div class="image-card-order">{{ idx + 1 }}</div>
        </div>
        <div v-if="images.length === 0 && !loadingImages" class="image-empty">
          暂无图片，请添加
        </div>
      </div>
    </template>

    <el-dialog v-model="showAddDialog" title="添加图片" width="460px" @close="addUrl = ''">
      <el-form label-width="80px">
        <el-form-item label="图片URL">
          <el-input v-model="addUrl" placeholder="请输入图片URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="addImage">添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBulkDialog" title="批量导入 URL" width="520px" @close="bulkUrls = ''">
      <p style="color: var(--color-text-muted); margin-bottom: 12px; font-size: 0.875rem;">
        每行一个 URL，最多可导入 {{ 8 - images.length }} 张
      </p>
      <el-input
        v-model="bulkUrls"
        type="textarea"
        :rows="6"
        placeholder="https://example.com/image1.jpg&#10;https://example.com/image2.jpg"
      />
      <template #footer>
        <el-button @click="showBulkDialog = false">取消</el-button>
        <el-button type="primary" :loading="bulkImporting" @click="bulkImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const products = ref([])
const selectedProductId = ref(null)
const images = ref([])
const loadingImages = ref(false)
const showAddDialog = ref(false)
const addUrl = ref('')
const adding = ref(false)
const showBulkDialog = ref(false)
const bulkUrls = ref('')
const bulkImporting = ref(false)

let dragIndex = ref(null)

onMounted(async () => {
  try {
    const res = await api.get('/products')
    if (res.data.code === 200) products.value = res.data.data || []
  } catch {}
})

async function loadImages() {
  if (!selectedProductId.value) return
  loadingImages.value = true
  try {
    const res = await api.get(`/admin/products/${selectedProductId.value}/images`)
    if (res.data.code === 200) images.value = res.data.data || []
  } catch {
    images.value = []
  } finally {
    loadingImages.value = false
  }
}

async function addImage() {
  if (!addUrl.value.trim()) {
    ElMessage.warning('请输入图片URL')
    return
  }
  adding.value = true
  try {
    await api.post(`/admin/products/${selectedProductId.value}/images`, {
      imageUrl: addUrl.value.trim(),
    })
    ElMessage.success('添加成功')
    showAddDialog.value = false
    addUrl.value = ''
    await loadImages()
  } catch {} finally {
    adding.value = false
  }
}

async function bulkImport() {
  const urls = bulkUrls.value.split('\n').map((u) => u.trim()).filter((u) => u.length > 0)
  if (urls.length === 0) {
    ElMessage.warning('请输入至少一个URL')
    return
  }
  bulkImporting.value = true
  try {
    await api.post(`/admin/products/${selectedProductId.value}/images/bulk`, { urls })
    ElMessage.success('导入成功')
    showBulkDialog.value = false
    bulkUrls.value = ''
    await loadImages()
  } catch {} finally {
    bulkImporting.value = false
  }
}

async function deleteImage(id) {
  try {
    await ElMessageBox.confirm('确定删除此图片？', '提示', { type: 'warning' })
    await api.delete(`/admin/products/images/${id}`)
    ElMessage.success('删除成功')
    await loadImages()
  } catch {}
}

async function setMain(id) {
  try {
    await api.put(`/admin/products/images/${id}/set-main`)
    ElMessage.success('已设为主图')
    await loadImages()
  } catch {}
}

function onDragStart(idx) {
  dragIndex.value = idx
}

function onDragOver(idx) {
  if (dragIndex.value === null || dragIndex.value === idx) return
  const list = [...images.value]
  const item = list.splice(dragIndex.value, 1)[0]
  list.splice(idx, 0, item)
  images.value = list
  dragIndex.value = idx
}

function onDrop() {
  saveReorder()
}

function onDragEnd() {
  dragIndex.value = null
}

async function saveReorder() {
  const orderedIds = images.value.map((img) => img.id)
  try {
    await api.put(`/admin/products/${selectedProductId.value}/images/reorder`, { orderedIds })
    await loadImages()
  } catch {}
}
</script>

<style scoped>
.admin-images {
  padding-bottom: 48px;
}

.toolbar {
  margin-bottom: 20px;
}

.toolbar-select {
  width: 320px;
}

.actions-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.image-count {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.image-card {
  background: var(--color-surface);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: all 0.2s ease;
  position: relative;
}

.image-card--main {
  border-color: var(--color-primary);
}

.image-card--drag {
  opacity: 0.5;
}

.image-card-img {
  aspect-ratio: 1;
  background: var(--color-bg);
  overflow: hidden;
}

.image-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-card-actions {
  padding: 8px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
}

.image-card-order {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 0.75rem;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-empty {
  grid-column: 1 / -1;
  text-align: center;
  color: var(--color-text-muted);
  padding: 48px 0;
  font-size: 0.9375rem;
}
</style>
