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

  // User profile
  getProfile() {
    return api.get('/user-profile', { params: { userId: USER_ID } })
  },
  updateProfile(data) {
    return api.put('/user-profile', data, { params: { userId: USER_ID } })
  },
}
