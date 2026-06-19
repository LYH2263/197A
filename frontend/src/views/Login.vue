<template>
  <div class="auth-page">
    <div class="auth-bg" />
    <el-card class="auth-card" shadow="never">
      <template #header>
        <span class="auth-card-title">登录</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" native-type="submit" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
        <p class="tip">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </p>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  await formRef.value?.validate().catch(() => {})
  loading.value = true
  try {
    const res = await api.post('/auth/login', form)
    if (res.data.code === 200 && res.data.data?.token) {
      userStore.setToken(res.data.data.token)
      await userStore.fetchUser()
      ElMessage.success('登录成功')
      router.push(route.query.redirect ? String(route.query.redirect) : '/')
    } else {
      ElMessage.error(res.data.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 24px;
}

.auth-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #f0fdfa 0%, #ccfbf1 40%, #99f6e4 100%);
  z-index: 0;
}

.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-lg);
}

.auth-card :deep(.el-card__header) {
  padding: 24px 24px 16px;
  border-bottom: 1px solid var(--color-border);
}

.auth-card-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
}

.auth-card :deep(.el-card__body) {
  padding: 24px;
}

.tip {
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.875rem;
  margin-top: 8px;
}

.tip a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
}

.tip a:hover {
  text-decoration: underline;
}
</style>
