import { reactive } from 'vue'
import axios from 'axios'

const STORAGE_KEY = 'diet_auth'

function authHeaders() {
  return { Authorization: `Bearer ${state.token}` }
}

const state = reactive({
  isLoggedIn: false,
  userId: null,
  username: '',
  nickname: '',
  avatarUrl: '',
  token: '',
})

function saveToStorage() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({
    userId: state.userId,
    username: state.username,
    nickname: state.nickname,
    avatarUrl: state.avatarUrl,
    token: state.token,
  }))
}

function clearStorage() {
  localStorage.removeItem(STORAGE_KEY)
}

function init() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY))
    if (saved && saved.token) {
      state.isLoggedIn = true
      state.userId = saved.userId
      state.username = saved.username
      state.nickname = saved.nickname
      state.avatarUrl = saved.avatarUrl || ''
      state.token = saved.token
    }
  } catch (e) {
    clearStorage()
  }
}

async function login(username, password) {
  const res = await axios.post('/api/auth/login', { username, password })
  const data = res.data.data
  state.isLoggedIn = true
  state.userId = data.userId
  state.username = data.username
  state.nickname = data.nickname
  state.avatarUrl = data.avatarUrl || ''
  state.token = data.token
  saveToStorage()
  return data
}

async function register(username, password) {
  const res = await axios.post('/api/auth/register', { username, password })
  const data = res.data.data
  state.isLoggedIn = true
  state.userId = data.userId
  state.username = data.username
  state.nickname = data.nickname
  state.avatarUrl = data.avatarUrl || ''
  state.token = data.token
  saveToStorage()
  return data
}

async function logout() {
  try {
    await axios.post('/api/auth/logout', {}, { headers: authHeaders() })
  } catch (e) {
    // 即使 API 调用失败也清除本地状态
  }
  state.isLoggedIn = false
  state.userId = null
  state.username = ''
  state.nickname = ''
  state.avatarUrl = ''
  state.token = ''
  clearStorage()
}

async function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await axios.post('/api/auth/avatar', formData, {
    headers: authHeaders(),
  })
  const avatarUrl = res.data.data
  state.avatarUrl = avatarUrl
  saveToStorage()
  return avatarUrl
}

export default {
  state,
  init,
  login,
  register,
  logout,
  uploadAvatar,
}
