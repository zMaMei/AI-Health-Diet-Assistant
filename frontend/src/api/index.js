import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

const USER_ID = 1

export default {
  // Diet records
  getDietRecords(date) {
    return api.get('/diet-records', { params: { userId: USER_ID, date } })
  },
  createDietRecord(data) {
    // 带上 AI 返回的营养值（calorie/protein/fat/carbohydrate/sugar/sodium），后端优先使用
    return api.post('/diet-records', { userId: USER_ID, ...data })
  },
  updateDietRecord(id, data) {
    return api.put(`/diet-records/${id}`, data)
  },
  deleteDietRecord(id) {
    return api.delete(`/diet-records/${id}`)
  },

  // Nutrition
  getNutrition(date) {
    return api.get('/nutrition/daily', { params: { userId: USER_ID, date } })
  },

  // Health score
  getHealthScore(date) {
    return api.get('/health-score/daily', { params: { userId: USER_ID, date } })
  },

  // Recommendations
  getRecommendations() {
    return api.get('/recommendations/today', { params: { userId: USER_ID } })
  },
  submitFeedback(recommendationId, feedback) {
    return api.post('/recommendations/feedback', { recommendationId, feedback })
  },

  // Food recognition (image upload)
  recognizeFood(formData) {
    return api.post('/food/recognize', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 15000,
    })
  },

  // Voice parse (with duration)
  parseVoice(formData, durationSeconds) {
    const params = durationSeconds ? `?durationSeconds=${durationSeconds}` : ''
    return api.post('/voice/parse' + params, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 20000,
    })
  },

  // Voice records
  getVoiceRecords(date) {
    return api.get('/voice-records', { params: { userId: USER_ID, date } })
  },
  deleteVoiceRecord(id) {
    return api.delete(`/voice-records/${id}`)
  },
  updateVoiceRecordMealType(id, mealType) {
    return api.put(`/voice-records/${id}/meal-type`, { mealType })
  },

  // Food text analysis (for manual add smart analysis)
  analyzeFoodText(foodName) {
    return api.post('/food/analyze-text', { foodName }, { timeout: 10000 })
  },

  // Alert rules
  getAlertRules() {
    return api.get('/alert-rules', { params: { userId: USER_ID } })
  },
  createAlertRule(data) {
    return api.post('/alert-rules', { userId: USER_ID, ...data })
  },
  updateAlertRule(ruleId, data) {
    return api.put(`/alert-rules/${ruleId}`, data)
  },
  checkAlerts(date) {
    return api.get('/alert-rules/check', { params: { userId: USER_ID, date } })
  },

  // Meal photos
  getMealPhotos(date, mealType) {
    const params = { userId: USER_ID, date }
    if (mealType) params.mealType = mealType
    return api.get('/meal-photos', { params })
  },
  saveMealPhoto(data) {
    return api.post('/meal-photos', { userId: USER_ID, ...data })
  },
  deleteMealPhoto(id) {
    return api.delete(`/meal-photos/${id}`)
  },

  // User profile
  getProfile() {
    return api.get('/user-profile', { params: { userId: USER_ID } })
  },
  updateProfile(data) {
    return api.put('/user-profile', data, { params: { userId: USER_ID } })
  },
  updateNickname(nickname) {
    return api.put('/user-profile/nickname', { nickname }, { params: { userId: USER_ID } })
  },
}
