/* Axios API接口层 */
import axios from 'axios'
import auth from '../auth.js'
import toast from '../toast.js'

/* Axios实例创建 */
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
})

/* 请求拦截器（自动带Token） */
api.interceptors.request.use(config => {
  if (auth.state.token) {
    config.headers.Authorization = `Bearer ${auth.state.token}`
  }
  return config
})

/* 响应拦截器（捕获401未授权） */
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      try {
        /* 未登录自动登出并提示 */
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
  /* 饮食记录API */
  /* 查询饮食记录 */ getDietRecords(date) {
    return api.get('/diet-records', { params: { date } })
  },
  /* 新增饮食记录 */ createDietRecord(data) {
    return api.post('/diet-records', data)
  },
  /* 修改饮食记录 */ updateDietRecord(id, data) {
    return api.put(`/diet-records/${id}`, data)
  },
  /* 删除饮食记录 */ deleteDietRecord(id) {
    return api.delete(`/diet-records/${id}`)
  },

  /* 营养分析API */
  /* 查询每日营养汇总 */ getNutrition(date) {
    return api.get('/nutrition/daily', { params: { date } })
  },

  /* 健康评分API */
  /* 查询每日健康评分 */ getHealthScore(date) {
    return api.get('/health-score/daily', { params: { date } })
  },

  /* 食谱推荐API */
  /* 获取今日推荐 */ getRecommendations() {
    return api.get('/recommendations/today')
  },
  /* 强制刷新推荐 */ refreshRecommendations() {
    return api.post('/recommendations/refresh', {}, { timeout: 30000 })
  },

  /* 食物识别API */
  /* 拍照识别食物 */ recognizeFood(formData) {
    return api.post('/food/recognize', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 15000,
    })
  },

  /* 语音解析API */
  /* 语音解析上传 */ parseVoice(formData, durationSeconds) {
    const params = durationSeconds ? `?durationSeconds=${durationSeconds}` : ''
    return api.post('/voice/parse' + params, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 20000,
    })
  },

  /* 语音记录API */
  /* 查询语音记录 */ getVoiceRecords(date) {
    return api.get('/voice-records', { params: { date } })
  },
  /* 删除语音记录 */ deleteVoiceRecord(id) {
    return api.delete(`/voice-records/${id}`)
  },
  /* 回填语音记录餐次 */ updateVoiceRecordMealType(id, mealType) {
    return api.put(`/voice-records/${id}/meal-type`, { mealType })
  },

  /* 食物文本分析API */
  /* 文本分析食物 */ analyzeFoodText(foodName) {
    return api.post('/food/analyze-text', { foodName }, { timeout: 10000 })
  },

  /* 预警规则API */
  /* 查询预警规则 */ getAlertRules() {
    return api.get('/alert-rules')
  },
  /* 创建预警规则 */ createAlertRule(data) {
    return api.post('/alert-rules', data)
  },
  /* 修改预警规则 */ updateAlertRule(ruleId, data) {
    return api.put(`/alert-rules/${ruleId}`, data)
  },
  /* AI分析生成阈值 */ analyzeAlertRules() {
    return api.post('/alert-rules/analyze', {}, { timeout: 20000 })
  },
  /* 检查预警 */ checkAlerts(date) {
    return api.get('/alert-rules/check', { params: { date } })
  },

  /* 餐次照片API */
  /* 查询餐次照片 */ getMealPhotos(date, mealType) {
    const params = { date }
    if (mealType) params.mealType = mealType
    return api.get('/meal-photos', { params })
  },
  /* 新增餐次照片 */ saveMealPhoto(data) {
    return api.post('/meal-photos', data)
  },
  /* 删除餐次照片 */ deleteMealPhoto(id) {
    return api.delete(`/meal-photos/${id}`)
  },

  /* 用户档案API */
  /* 查询用户档案 */ getProfile() {
    return api.get('/user-profile')
  },
  /* 更新用户档案 */ updateProfile(data) {
    return api.put('/user-profile', data)
  },

  /* AI对话API */
  /* AI饮食对话 */ sendAiMessage(date, message) {
    return api.post('/ai/analyze-diet', { date, message }, { timeout: 30000 })
  },
  /* 查询AI对话历史 */ getAiConversation(date) {
    return api.get('/ai/conversation', { params: { date } })
  },
}
