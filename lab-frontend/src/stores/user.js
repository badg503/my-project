import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, getInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  async function login(form) {
    const data = await apiLogin(form)
    token.value = data.token
    user.value = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      role: data.role,
      avatar: data.avatar
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return data
  }

  async function fetchUser() {
    const data = await getInfo()
    user.value = { ...user.value, ...data }
    localStorage.setItem('user', JSON.stringify(user.value))
    return data
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function updateUser(newUser) {
    user.value = newUser
    localStorage.setItem('user', JSON.stringify(newUser))
  }

  const isAdmin = () => user.value?.role === 'SYS_ADMIN'
  const isDeviceAdmin = () => user.value?.role === 'LAB_ADMIN'
  const isTeacher = () => user.value?.role === 'TEACHER'
  const isStudent = () => user.value?.role === 'STUDENT'

  return { token, user, login, fetchUser, logout, updateUser, isAdmin, isDeviceAdmin, isTeacher, isStudent }
})
