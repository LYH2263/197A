import api from '../api'

const DEBOUNCE_MS = 300

const debounceTimers = new Map()
const pendingPromises = new Map()

function clearDebounce(key) {
  const timer = debounceTimers.get(key)
  if (timer) {
    clearTimeout(timer)
    debounceTimers.delete(key)
  }
}

function debounceUpdateQuantity(productId, quantity) {
  const key = `qty_${productId}`
  clearDebounce(key)

  if (pendingPromises.has(key)) {
    pendingPromises.get(key).canceled = true
  }

  return new Promise((resolve, reject) => {
    const timer = setTimeout(async () => {
      debounceTimers.delete(key)
      try {
        const res = await api.put('/cart/quantity', { productId, quantity })
        if (!pendingPromises.get(key)?.canceled) {
          pendingPromises.delete(key)
          resolve(res)
        }
      } catch (e) {
        pendingPromises.delete(key)
        reject(e)
      }
    }, DEBOUNCE_MS)

    debounceTimers.set(key, timer)
    const promiseEntry = { timer, canceled: false }
    pendingPromises.set(key, promiseEntry)
  })
}

async function updateQuantityImmediate(productId, quantity) {
  const key = `qty_${productId}`
  clearDebounce(key)
  if (pendingPromises.has(key)) {
    pendingPromises.get(key).canceled = true
    pendingPromises.delete(key)
  }
  return api.put('/cart/quantity', { productId, quantity })
}

export default {
  list() {
    return api.get('/cart')
  },

  add(productId, quantity = 1) {
    return api.post('/cart/add', { productId, quantity })
  },

  updateQuantity(productId, quantity, immediate = false) {
    if (immediate) {
      return updateQuantityImmediate(productId, quantity)
    }
    return debounceUpdateQuantity(productId, quantity)
  },

  setChecked(productId, checked) {
    return api.put('/cart/checked', { productId, checked })
  },

  remove(productId) {
    return api.delete(`/cart/${productId}`)
  },

  moveToSaveForLater(productId) {
    return api.post('/save-for-later/move', { productId })
  },

  flushAll() {
    for (const key of debounceTimers.keys()) {
      clearDebounce(key)
    }
    pendingPromises.clear()
  }
}
