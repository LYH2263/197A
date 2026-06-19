<template>
  <div class="admin-reviews">
    <h2 class="page-title">评价管理</h2>
    <div v-loading="loading" class="content content-card">
      <el-table :data="list" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userName" label="用户" width="100" />
        <el-table-column prop="productName" label="商品" min-width="140" />
        <el-table-column label="评分" width="120">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="评价时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无评价" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const loading = ref(true)
const list = ref([])

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/reviews')
    list.value = res.data.code === 200 ? (res.data.data || []) : []
  } finally {
    loading.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该评价？', '提示')
    await api.delete(`/admin/reviews/${row.id}`)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}

onMounted(load)
</script>

<style scoped>
.admin-reviews {
  padding-bottom: 32px;
}

.content {
  padding: 24px;
}
</style>
