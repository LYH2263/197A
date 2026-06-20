<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">电商购物平台</router-link>
        <nav class="nav">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link to="/products" class="nav-link">商品</router-link>
          <router-link to="/cart" class="nav-link cart-link">
            购物车
            <el-badge v-if="cartCount" :value="cartCount" class="cart-badge" />
          </router-link>
          <router-link to="/save-for-later" class="nav-link cart-link">
            稍后购买
            <el-badge v-if="saveForLaterCount" :value="saveForLaterCount" class="cart-badge" />
          </router-link>
          <el-popover placement="bottom" :width="360" trigger="click" @show="loadNotifications">
            <template #reference>
              <span class="nav-link cart-link" style="cursor:pointer">
                通知
                <el-badge v-if="unreadCount" :value="unreadCount" class="cart-badge" />
              </span>
            </template>
            <div class="notification-panel">
              <div v-if="notifications.length === 0" class="notification-empty">暂无通知</div>
              <div v-for="n in notifications" :key="n.id" class="notification-item" :class="{ unread: n.isRead === 0 }" @click="markRead(n)">
                <div class="notification-content">{{ n.content }}</div>
                <div class="notification-time">{{ n.createdAt }}</div>
              </div>
              <div v-if="notifications.length > 0" class="notification-footer">
                <el-button link size="small" @click="markAllRead">全部已读</el-button>
              </div>
            </div>
          </el-popover>
          <template v-if="userStore.isLoggedIn">
            <router-link to="/orders" class="nav-link">我的订单</router-link>
            <router-link to="/my-address" class="nav-link">我的地址</router-link>
            <router-link to="/my-reviews" class="nav-link">我的评价</router-link>
            <template v-if="userStore.isAdmin">
              <router-link to="/admin/users" class="nav-link">用户管理</router-link>
              <router-link to="/admin/orders/pending-ship" class="nav-link">待发货订单</router-link>
              <router-link to="/admin/reviews" class="nav-link">评价管理</router-link>
              <router-link to="/admin/product-images" class="nav-link">图集管理</router-link>
            </template>
            <el-dropdown @command="handleCommand" trigger="click">
              <span class="user-trigger">
                {{ userStore.user?.nickname || userStore.user?.username }}
                <el-icon class="chevron"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login" class="nav-link">登录</router-link>
            <router-link to="/register" class="nav-link nav-link--primary">注册</router-link>
          </template>
        </nav>
      </div>
    </header>
    <main class="main">
      <router-view v-slot="{ Component }">
        <Suspense>
          <component :is="Component" />
          <template #fallback>
            <div class="page-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载中...</span>
            </div>
          </template>
        </Suspense>
      </router-view>
    </main>
    <footer class="footer">
      <p>© 电商购物平台 · 题目 197</p>
    </footer>
  </div>
</template>

<script setup>
import { onMounted, computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'
import { Loading, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const notifications = ref([])
const unreadCount = ref(0)

const cartCount = computed(() => {
  if (!userStore.isLoggedIn) return 0
  return userStore.cartCount ?? 0
})

const saveForLaterCount = computed(() => {
  if (!userStore.isLoggedIn) return 0
  return userStore.saveForLaterCount ?? 0
})

async function loadNotifications() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await api.get('/notification')
    if (res.data.code === 200) {
      notifications.value = res.data.data || []
    }
  } catch {}
}

async function fetchUnreadCount() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await api.get('/notification/unread-count')
    if (res.data.code === 200) {
      unreadCount.value = res.data.data || 0
    }
  } catch {}
}

async function markRead(n) {
  if (n.isRead === 1) return
  try {
    await api.put(`/notification/${n.id}/read`)
    n.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch {}
}

async function markAllRead() {
  try {
    await api.put('/notification/read-all')
    notifications.value.forEach(n => { n.isRead = 1 })
    unreadCount.value = 0
  } catch {}
}

onMounted(async () => {
  if (userStore.isLoggedIn) {
    await userStore.fetchUser()
    try {
      const res = await api.get('/cart')
      if (res.data.code === 200 && Array.isArray(res.data.data)) {
        userStore.cartCount = res.data.data.reduce((s, i) => s + (i.quantity || 0), 0)
      }
    } catch {
      userStore.cartCount = 0
    }
    try {
      const res = await api.get('/save-for-later/count')
      if (res.data.code === 200) {
        userStore.saveForLaterCount = res.data.data || 0
      }
    } catch {
      userStore.saveForLaterCount = 0
    }
    fetchUnreadCount()
  }
})

watch(() => route.path, async () => {
  if (userStore.isLoggedIn) {
    try {
      const res = await api.get('/save-for-later/count')
      if (res.data.code === 200) {
        userStore.saveForLaterCount = res.data.data || 0
      }
    } catch {}
  }
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/')
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border);
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
  text-decoration: none;
  letter-spacing: -0.02em;
  transition: color var(--transition);
}

.logo:hover {
  color: var(--color-primary);
}

.nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-link {
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: 0.9375rem;
  font-weight: 500;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  transition: color var(--transition), background var(--transition);
}

.nav-link:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.nav-link.router-link-active {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.nav-link--primary.router-link-active,
.nav-link--primary:hover {
  color: #fff;
  background: var(--color-primary);
}

.cart-link {
  position: relative;
}

.cart-badge {
  margin-left: 4px;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition: color var(--transition), background var(--transition);
}

.user-trigger:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.chevron {
  font-size: 0.75rem;
  transition: transform var(--transition);
}

.main {
  flex: 1;
  padding: 32px 24px;
  max-width: 1280px;
  margin: 0 auto;
  width: 100%;
}

.page-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 64px;
  color: var(--color-text-muted);
}

.footer {
  padding: 24px;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.875rem;
  border-top: 1px solid var(--color-border);
}

.notification-panel {
  max-height: 360px;
  overflow-y: auto;
}

.notification-empty {
  padding: 24px;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.notification-item {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: background var(--transition);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: var(--color-primary-light);
}

.notification-item.unread {
  background: #ecf5ff;
}

.notification-content {
  font-size: 0.875rem;
  color: var(--color-text);
  line-height: 1.5;
}

.notification-time {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.notification-footer {
  padding: 8px 12px;
  text-align: center;
  border-top: 1px solid var(--color-border);
}
</style>
