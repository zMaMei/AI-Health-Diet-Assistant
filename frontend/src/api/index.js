import axios from 'axios'
import auth from '../auth.js'
import toast from '../toast.js'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器：自动带 token
api.interceptors.request.use(config => {
  if (auth.state.token) {
    config.headers.Authorization = `Bearer ${auth.state.token}`
  }
  return config
})

// 响应拦截器：捕获 401
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      try {
        auth.logout()
        if (window.location.hash !== '#/profile') {
          toast.show('请先在"我的"页面登录')
        }
      } catch (ignored) {}
    }
    return Promise.reject(error)
  }
)

export default {
  // Diet records — userId 由后端从 token 解析
  getDietRecords(date) {
    return api.get('/diet-records', { params: { date } })
  },
  createDietRecord(data) {
    return api.post('/diet-records', data)
  },
  updateDietRecord(id, data) {
    return api.put(`/diet-records/${id}`, data)
  },
  deleteDietRecord(id) {
    return api.delete(`/diet-records/${id}`)
  },

  // Nutrition
  getNutrition(date) {
    return api.get('/nutrition/daily', { params: { date } })
  },

  // Health score
  getHealthScore(date) {
    return api.get('/health-score/daily', { params: { date } })
  },

  // Recommendations
  getRecommendations() {
    return api.get('/recommendations/today')
  },
  refreshRecommendations() {
    return api.post('/recommendations/refresh', {}, { timeout: 30000 })
  },

  // Food recognition (image upload)
  recognizeFood(formData) {
    return api.post('/food/recognize', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 15000,
    })
  },

  // Voice parse
  parseVoice(formData, durationSeconds) {
    const params = durationSeconds ? `?durationSeconds=${durationSeconds}` : ''
    return api.post('/voice/parse' + params, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 20000,
    })
  },

  // Voice records
  getVoiceRecords(date) {
    return api.get('/voice-records', { params: { date } })
  },
  deleteVoiceRecord(id) {
    return api.delete(`/voice-records/${id}`)
  },
  updateVoiceRecordMealType(id, mealType) {
    return api.put(`/voice-records/${id}/meal-type`, { mealType })
  },

  // Food text analysis
  analyzeFoodText(foodName) {
    return api.post('/food/analyze-text', { foodName }, { timeout: 10000 })
  },

  // Alert rules
  getAlertRules() {
    return api.get('/alert-rules')
  },
  createAlertRule(data) {
    return api.post('/alert-rules', data)
  },
  updateAlertRule(ruleId, data) {
    return api.put(`/alert-rules/${ruleId}`, data)
  },
  analyzeAlertRules() {
    return api.post('/alert-rules/analyze', {}, { timeout: 20000 })
  },
  checkAlerts(date) {
    return api.get('/alert-rules/check', { params: { date } })
  },

  // Meal photos
  getMealPhotos(date, mealType) {
    const params = { date }
    if (mealType) params.mealType = mealType
    return api.get('/meal-photos', { params })
  },
  saveMealPhoto(data) {
    return api.post('/meal-photos', data)
  },
  deleteMealPhoto(id) {
    return api.delete(`/meal-photos/${id}`)
  },

  // User profile
  getProfile() {
    return api.get('/user-profile')
  },
  updateProfile(data) {
    return api.put('/user-profile', data)
  },

  // AI 饮食分析对话
  sendAiMessage(date, message) {
    return api.post('/ai/analyze-diet', { date, message }, { timeout: 30000 })
  },
  getAiConversation(date) {
    return api.get('/ai/conversation', { params: { date } })
  },
}
