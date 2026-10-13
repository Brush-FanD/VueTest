import { computed, ref } from 'vue'
import {
  fetchCurrentUser,
  login as loginApi,
  register as registerApi,
  logout as logoutApi
} from '../api/auth'

/**
 * 全局单例登录状态：模块级 ref，所有组件共享同一份，不需要 Pinia。
 * 页面刷新后靠 GET /auth/me 恢复登录态（会话身份在 HttpOnly Cookie 里）。
 */
const user = ref(null)
const loaded = ref(false)
const pending = ref(false)
const isLoggedIn = computed(() => !!user.value)

const loadUser = async () => {
  try {
    user.value = (await fetchCurrentUser()) || null
  } catch {
    user.value = null
  } finally {
    loaded.value = true
  }
}

const login = async (username, password) => {
  pending.value = true
  try {
    user.value = await loginApi({ username, password })
    loaded.value = true
    return user.value
  } finally {
    pending.value = false
  }
}

const register = async (username, password, displayName) => {
  pending.value = true
  try {
    user.value = await registerApi({ username, password, displayName })
    loaded.value = true
    return user.value
  } finally {
    pending.value = false
  }
}

const logout = async () => {
  try {
    await logoutApi()
  } finally {
    user.value = null
  }
}

export function useAuth() {
  return { user, loaded, pending, isLoggedIn, loadUser, login, register, logout }
}