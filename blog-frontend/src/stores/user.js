import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const nickname = computed(() => userInfo.value?.nickname || '')
  const avatar = computed(() => userInfo.value?.avatar || '')

  // Actions
  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const login = async (credentials) => {
    const data = await loginApi(credentials)
    setToken(data.token)
    userInfo.value = data.user
    return data
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  const fetchUserInfo = async () => {
    try {
      const data = await getUserInfo()
      userInfo.value = data
      return data
    } catch (error) {
      logout()
      throw error
    }
  }

  // 更新头像
  const updateAvatar = (newAvatar) => {
    if (userInfo.value) {
      userInfo.value.avatar = newAvatar
    }
  }

  // 更新用户资料
  const updateProfile = (profile) => {
    if (userInfo.value) {
      Object.assign(userInfo.value, profile)
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    nickname,
    avatar,
    login,
    logout,
    fetchUserInfo,
    updateAvatar,
    updateProfile,
    setToken
  }
})
