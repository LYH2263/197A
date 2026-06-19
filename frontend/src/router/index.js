import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/', component: () => import('../views/Layout.vue'), meta: { requiresAuth: false },
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'products', name: 'ProductList', component: () => import('../views/ProductList.vue') },
      { path: 'products/:id', name: 'ProductDetail', component: () => import('../views/ProductDetail.vue') },
      { path: 'cart', name: 'Cart', component: () => import('../views/Cart.vue'), meta: { requiresAuth: true } },
      { path: 'orders', name: 'OrderList', component: () => import('../views/OrderList.vue'), meta: { requiresAuth: true } },
      { path: 'orders/:id', name: 'OrderDetail', component: () => import('../views/OrderDetail.vue'), meta: { requiresAuth: true } },
      { path: 'orders/:id/after-sale', name: 'AfterSalePlaceholder', component: () => import('../views/AfterSalePlaceholder.vue'), meta: { requiresAuth: true } },
      { path: 'my-address', name: 'MyAddress', component: () => import('../views/MyAddress.vue'), meta: { requiresAuth: true } },
      { path: 'my-reviews', name: 'MyReviews', component: () => import('../views/MyReviews.vue'), meta: { requiresAuth: true } },
      { path: 'admin/users', name: 'AdminUsers', component: () => import('../views/AdminUsers.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/reviews', name: 'AdminReviews', component: () => import('../views/AdminReviews.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/orders/pending-ship', name: 'AdminPendingShip', component: () => import('../views/AdminPendingShip.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/product-images', name: 'AdminProductImages', component: () => import('../views/AdminProductImages.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    ],
  },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to, _from, next) => {
  const user = useUserStore()
  if (to.meta.requiresAuth && !user.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.requiresAdmin) {
    if (!user.user && user.token) await user.fetchUser()
    if (!user.isAdmin) {
      next({ name: 'Home' })
      return
    }
  }
  next()
})

export default router
