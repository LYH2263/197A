<template>
  <div class="address-page">
    <div class="page-header">
      <h2 class="page-title">我的收货地址</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        <span>新增地址</span>
      </el-button>
    </div>

    <div v-loading="loading" class="address-list">
      <template v-if="addresses.length">
        <div
          v-for="addr in addresses"
          :key="addr.id"
          class="address-card content-card"
          :class="{ 'is-disabled': addr.isDisabled === 1 }"
        >
          <div class="address-card-header">
            <div class="address-tags">
              <el-tag v-if="addr.isDefault === 1" type="danger" size="small" effect="dark">默认</el-tag>
              <el-tag
                v-if="addr.tag"
                :type="tagType(addr.tag)"
                size="small"
                effect="plain"
              >{{ addr.tag }}</el-tag>
            </div>
            <span v-if="addr.isDisabled === 1" class="disabled-badge">
              <el-icon><Lock /></el-icon>
              系统保护
            </span>
          </div>
          <div class="address-info">
            <p class="receiver">
              <strong>{{ addr.receiverName }}</strong>
              <span class="phone">{{ addr.receiverPhone }}</span>
            </p>
            <p class="detail">
              {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}
            </p>
          </div>
          <div class="address-actions">
            <template v-if="addr.isDefault !== 1 && addr.isDisabled !== 1">
              <el-button link type="primary" @click="setDefault(addr.id)">设为默认</el-button>
            </template>
            <el-button
              link
              type="primary"
              :disabled="addr.isDisabled === 1"
              @click="openEditDialog(addr)"
            >编辑</el-button>
            <el-popconfirm
              :title="addr.isDisabled === 1 ? '该地址受系统保护，无法删除' : '确定删除该地址？'"
              :disabled="addr.isDisabled === 1"
              @confirm="remove(addr.id)"
            >
              <template #reference>
                <el-button link type="danger" :disabled="addr.isDisabled === 1">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无收货地址">
        <el-button type="primary" @click="openAddDialog">去添加</el-button>
      </el-empty>
    </div>

    <el-dialog
      v-model="showDialog"
      :title="editingId ? '编辑地址' : '新增地址'"
      width="600px"
      class="address-dialog"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所在地区" prop="region">
          <el-cascader
            v-model="form.region"
            :options="regionData"
            :props="{ expandTrigger: 'hover' }"
            placeholder="请选择省/市/区"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="form.detailAddress"
            type="textarea"
            :rows="2"
            placeholder="街道、门牌号等"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-radio-group v-model="form.tag">
            <el-radio-button
              v-for="t in tagOptions"
              :key="t.value"
              :value="t.value"
            >{{ t.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Lock } from '@element-plus/icons-vue'
import api from '../api'
import { regionData, tagOptions } from '../data/region'

const loading = ref(true)
const addresses = ref([])
const showDialog = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  region: [],
  detailAddress: '',
  tag: '',
  isDefault: 0,
})

const rules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  region: [{ required: true, message: '请选择省市区', trigger: 'change' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
}

function tagType(tag) {
  const found = tagOptions.find((t) => t.value === tag)
  return found ? found.type : 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/addresses')
    addresses.value = res.data.code === 200 ? res.data.data || [] : []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    receiverName: '',
    receiverPhone: '',
    region: [],
    detailAddress: '',
    tag: '',
    isDefault: 0,
  })
  editingId.value = null
  formRef.value?.clearValidate()
}

function openAddDialog() {
  resetForm()
  showDialog.value = true
}

function openEditDialog(addr) {
  Object.assign(form, {
    receiverName: addr.receiverName,
    receiverPhone: addr.receiverPhone,
    region: [addr.province, addr.city, addr.district],
    detailAddress: addr.detailAddress,
    tag: addr.tag || '',
    isDefault: addr.isDefault || 0,
  })
  editingId.value = addr.id
  showDialog.value = true
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = {
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      province: form.region[0],
      city: form.region[1],
      district: form.region[2],
      detailAddress: form.detailAddress,
      tag: form.tag || undefined,
      isDefault: form.isDefault,
    }
    if (editingId.value) {
      await api.put(`/addresses/${editingId.value}`, payload)
      ElMessage.success('地址修改成功')
    } else {
      await api.post('/addresses', payload)
      ElMessage.success('地址添加成功')
    }
    showDialog.value = false
    resetForm()
    load()
  } finally {
    submitting.value = false
  }
}

async function setDefault(id) {
  try {
    await api.post(`/addresses/${id}/default`)
    ElMessage.success('已设为默认地址')
    load()
  } catch (e) {}
}

async function remove(id) {
  try {
    await api.delete(`/addresses/${id}`)
    ElMessage.success('删除成功')
    load()
  } catch (e) {}
}

onMounted(load)
</script>

<style scoped>
.address-page {
  padding-bottom: 32px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.address-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.address-card {
  padding: 20px;
  position: relative;
  transition: box-shadow var(--transition), border-color var(--transition);
}

.address-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.address-card.is-disabled {
  background: #fafafa;
  border-color: #e4e7ed;
}

.address-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.address-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.disabled-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8125rem;
  color: #909399;
}

.receiver {
  margin: 0 0 6px 0;
  font-size: 1rem;
  line-height: 1.5;
}

.receiver strong {
  color: var(--color-text);
  margin-right: 12px;
}

.phone {
  color: var(--color-text-secondary);
  font-weight: 500;
}

.detail {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  line-height: 1.6;
}

.address-actions {
  display: flex;
  gap: 16px;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px dashed var(--color-border);
}
</style>
