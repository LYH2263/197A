<template>
  <div class="my-reviews">
    <h2 class="page-title">我的评价</h2>
    <div v-loading="loading" class="content content-card">
      <el-table :data="list" style="width: 100%">
        <el-table-column prop="productName" label="商品" min-width="160" />
        <el-table-column label="评分" width="120">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="评价时间" width="180" />
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无评价" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const loading = ref(true)
const list = ref([])

onMounted(async () => {
  loading.value = true
  try {
    const res = await api.get('/reviews/me')
    list.value = res.data.code === 200 ? (res.data.data || []) : []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.my-reviews {
  padding-bottom: 32px;
}

.content {
  padding: 24px;
}
</style>
