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
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <span v-if="row.images && row.images.length > 0">{{ row.images.length }}张</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="追评" width="80">
          <template #default="{ row }">
            <span v-if="row.followups && row.followups.length > 0" class="followup-badge">有</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="商家回复" min-width="140">
          <template #default="{ row }">
            <span v-if="row.replyContent" class="reply-badge">已回复</span>
            <span v-else class="reply-badge reply-badge--none">未回复</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="评价时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.reviewType === 0 && !row.replyContent"
              type="primary"
              link
              size="small"
              @click="openReply(row)"
            >
              回复
            </el-button>
            <el-button
              v-if="row.reviewType === 0 && row.replyContent"
              type="info"
              link
              size="small"
              @click="viewReply(row)"
            >
              查看回复
            </el-button>
            <el-button type="danger" link size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无评价" />
    </div>

    <el-dialog v-model="replyVisible" :title="`回复评价 #${replyForm.reviewId}`" width="500px">
      <el-form :model="replyForm" label-width="80px">
        <el-form-item label="用户">
          <span>{{ replyingReview?.userName }}</span>
        </el-form-item>
        <el-form-item label="商品">
          <span>{{ replyingReview?.productName }}</span>
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input :model-value="replyingReview?.content" type="textarea" :rows="2" readonly />
        </el-form-item>
        <el-form-item label="回复内容" required>
          <el-input
            v-model="replyForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入商家官方回复内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replySubmitting" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewReplyVisible" title="查看商家回复" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户">{{ replyingReview?.userName }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ replyingReview?.productName }}</el-descriptions-item>
        <el-descriptions-item label="用户评价">{{ replyingReview?.content || '（无文字）' }}</el-descriptions-item>
        <el-descriptions-item label="商家回复">
          <template v-if="replyingReview?.replyContent">
            <div class="reply-content">{{ replyingReview.replyContent }}</div>
            <div class="reply-time">回复时间：{{ replyingReview.replyAt }}</div>
          </template>
          <span v-else>暂无回复</span>
        </el-descriptions-item>
      </el-descriptions>
      <template v-if="replyingReview?.followups && replyingReview.followups.length > 0" #footer>
        <el-divider />
        <h4>用户追评</h4>
        <div v-for="(f, idx) in replyingReview.followups" :key="f.id" class="followup-item">
          <div class="followup-content">{{ f.content || '（无文字）' }}</div>
          <div class="followup-time">{{ f.createdAt }}</div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const loading = ref(true)
const list = ref([])
const replyVisible = ref(false)
const viewReplyVisible = ref(false)
const replySubmitting = ref(false)
const replyingReview = ref(null)
const replyForm = reactive({
  reviewId: null,
  content: '',
})

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/reviews')
    list.value = res.data.code === 200 ? (res.data.data || []) : []
  } finally {
    loading.value = false
  }
}

function openReply(row) {
  replyingReview.value = row
  replyForm.reviewId = row.id
  replyForm.content = ''
  replyVisible.value = true
}

function viewReply(row) {
  replyingReview.value = row
  viewReplyVisible.value = true
}

async function submitReply() {
  if (!replyForm.content || !replyForm.content.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replySubmitting.value = true
  try {
    await api.post('/admin/reviews/reply', {
      reviewId: replyForm.reviewId,
      content: replyForm.content.trim(),
    })
    ElMessage.success('回复成功')
    replyVisible.value = false
    load()
  } catch (e) {
    if (e?.response?.data?.message) {
      ElMessage.error(e.response.data.message)
    }
  } finally {
    replySubmitting.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该评价？追评也将一并删除。', '提示')
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

.followup-badge {
  display: inline-block;
  padding: 2px 8px;
  background: var(--color-success-light-9);
  color: var(--color-success);
  border-radius: 10px;
  font-size: 0.75rem;
}

.reply-badge {
  display: inline-block;
  padding: 2px 8px;
  background: var(--color-warning-light-9);
  color: var(--color-warning);
  border-radius: 10px;
  font-size: 0.75rem;
}

.reply-badge--none {
  background: var(--color-bg-page);
  color: var(--color-text-muted);
}

.reply-content {
  color: var(--color-text-secondary);
  line-height: 1.6;
  padding: 8px 0;
}

.reply-time {
  color: var(--color-text-muted);
  font-size: 0.8125rem;
}

.followup-item {
  padding: 12px;
  background: var(--color-bg-page);
  border-radius: 8px;
  margin-bottom: 8px;
}

.followup-content {
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.followup-time {
  color: var(--color-text-muted);
  font-size: 0.75rem;
}
</style>
