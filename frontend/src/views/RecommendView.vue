<template>
  <div class="recommend-page">
    <!-- Header -->
    <div class="page-header">
      <h2 class="page-title">今日推荐</h2>
      <button
        class="btn btn-outline btn-refresh"
        :disabled="refreshing"
        @click="refreshAll"
      >
        <span v-if="refreshing" class="spinner"></span>
        {{ refreshing ? '生成中...' : '🔄 换一批' }}
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loading-icon">🤖</div>
      <p>AI 正在为您生成个性化推荐...</p>
      <p class="loading-sub">正在分析您的营养缺口和口味偏好</p>
    </div>

    <!-- Error -->
    <div v-else-if="errorMsg" class="empty-state">
      <div class="empty-icon">⚠️</div>
      <p>{{ errorMsg }}</p>
      <button class="btn btn-primary" @click="fetchRecommendations" style="margin-top:12px">重试</button>
    </div>

    <!-- Empty -->
    <div v-else-if="!recommendations.length" class="empty-state">
      <div class="empty-icon">🍽️</div>
      <p>暂无推荐菜谱</p>
      <p style="font-size:12px;color:#bbb;margin-top:4px">请先在"我的"页面设置健康目标和偏好</p>
    </div>

    <!-- Cards -->
    <template v-else>
      <div class="card rec-card" v-for="rec in recommendations" :key="rec.id">
        <!-- Title row -->
        <div class="rec-header">
          <h3 class="rec-name">{{ rec.recipeName }}</h3>
          <div class="rec-tags-row">
            <span class="tag tag-green" v-for="tag in parseTags(rec.tags)" :key="tag">{{ tag }}</span>
          </div>
        </div>

        <!-- Nutrition bars -->
        <div class="rec-nutrition">
          <div class="nut-row" v-for="nut in nutBars(rec)" :key="nut.label">
            <span class="nut-label">{{ nut.icon }} {{ nut.label }}</span>
            <div class="nut-bar-track">
              <div
                class="nut-bar-fill"
                :style="{ width: nut.pct + '%', background: nut.color }"
              ></div>
            </div>
            <span class="nut-value">{{ nut.recipeVal }}/{{ nut.threshold }}{{ nut.unit }}</span>
          </div>
        </div>

        <!-- Ingredients preview -->
        <div class="rec-ingredients">
          <strong>🥬 食材：</strong>{{ previewIngredients(rec.ingredients) }}
        </div>

        <!-- AI Reason -->
        <div class="rec-reason">
          <strong>💡 推荐理由：</strong>{{ rec.reason }}
        </div>

        <!-- Actions -->
        <div class="rec-actions">
          <button class="btn btn-sm btn-outline" @click="viewDetail(rec)">
            📋 查看详情
          </button>
        </div>
      </div>
    </template>

    <!-- Recipe Detail Modal -->
    <div class="modal-overlay" v-if="showDetailModal" @click.self="showDetailModal=false">
      <div class="modal-content recipe-detail">
        <h3>🍳 {{ detailRecipe?.recipeName }}</h3>
        <div class="detail-tags" v-if="detailRecipe?.tags">
          <span class="tag tag-green" v-for="tag in parseTags(detailRecipe.tags)" :key="tag">{{ tag }}</span>
        </div>
        <div class="detail-nutrition">
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.calorie) }}</span>
            <span class="nut-label">热量(kcal)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.protein) }}</span>
            <span class="nut-label">蛋白质(g)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.fat) }}</span>
            <span class="nut-label">脂肪(g)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.carbohydrate) }}</span>
            <span class="nut-label">碳水(g)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.sugar) }}</span>
            <span class="nut-label">糖分(g)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.sodium) }}</span>
            <span class="nut-label">钠(mg)</span>
          </div>
        </div>
        <div class="detail-section">
          <strong>🥬 食材</strong>
          <p>{{ detailRecipe?.ingredients }}</p>
        </div>
        <div class="detail-section" v-if="detailRecipe?.steps">
          <strong>📝 做法</strong>
          <ol class="steps-list">
            <li v-for="(step, i) in parseSteps(detailRecipe.steps)" :key="i">{{ step }}</li>
          </ol>
        </div>
        <div class="detail-section" v-if="detailRecipe?.reason">
          <strong>💡 推荐理由</strong>
          <p>{{ detailRecipe?.reason }}</p>
        </div>
        <button class="btn btn-outline" @click="showDetailModal=false" style="margin-top:12px;width:100%">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/index.js'
import toast from '../toast.js'

const recommendations = ref([])
const loading = ref(false)
const refreshing = ref(false)
const errorMsg = ref('')
const showDetailModal = ref(false)
const detailRecipe = ref(null)

// Default thresholds (used when profile not yet loaded — backend provides real ones)
const defaultThresholds = {
  calorie: 2000, protein: 60, fat: 65, carbohydrate: 300, sugar: 50, sodium: 2400
}

async function fetchRecommendations() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.getRecommendations()
    const data = res.data?.data
    if (Array.isArray(data)) {
      recommendations.value = data
    } else {
      recommendations.value = []
    }
  } catch (e) {
    console.error(e)
    if (e.response?.status >= 500) {
      errorMsg.value = 'AI 推荐服务繁忙，请稍后重试'
    } else if (e.response?.data?.message) {
      errorMsg.value = e.response.data.message
    } else {
      errorMsg.value = '加载推荐失败，请检查网络'
    }
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  refreshing.value = true
  try {
    const res = await api.refreshRecommendations()
    const data = res.data?.data
    if (Array.isArray(data)) {
      recommendations.value = data
      toast.show('已为您换一批推荐')
    } else {
      recommendations.value = []
    }
  } catch (e) {
    console.error(e)
    if (e.response?.status >= 500) {
      toast.show('AI 推荐服务繁忙，请稍后重试')
    } else if (e.response?.data?.message) {
      toast.show(e.response.data.message)
    } else {
      toast.show('刷新失败，请稍后重试')
    }
  } finally {
    refreshing.value = false
  }
}

function nutBars(rec) {
  const nutrients = [
    { key: 'calorie', label: '热量', icon: '🔥', unit: 'kcal', color: '#FF9800' },
    { key: 'protein', label: '蛋白质', icon: '🥩', unit: 'g', color: '#4CAF50' },
    { key: 'fat', label: '脂肪', icon: '🧈', unit: 'g', color: '#FFC107' },
    { key: 'carbohydrate', label: '碳水', icon: '🌾', unit: 'g', color: '#2196F3' },
  ]
  return nutrients.map(n => {
    const recipeVal = Number(rec[n.key]) || 0
    const threshold = defaultThresholds[n.key] || 2000
    const pct = threshold > 0 ? Math.min(100, (recipeVal / threshold) * 100) : 0
    return {
      ...n,
      recipeVal: recipeVal.toFixed(0),
      threshold,
      pct: Math.round(pct),
    }
  })
}

function previewIngredients(ingredients) {
  if (!ingredients) return ''
  const parts = ingredients.split(',')
  return parts.slice(0, 4).join('、') + (parts.length > 4 ? '...' : '')
}

function fmt(val) {
  const n = Number(val)
  return isNaN(n) ? '-' : n.toFixed(1)
}

function viewDetail(rec) {
  detailRecipe.value = rec
  showDetailModal.value = true
}

function parseTags(tags) {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

function parseSteps(steps) {
  if (!steps) return []
  return steps.split('\n').filter(Boolean)
}

onMounted(fetchRecommendations)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
}
.btn-refresh {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #ccc;
  border-top-color: #666;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state {
  text-align: center;
  padding: 48px 20px;
  color: #666;
}
.loading-icon { font-size: 48px; margin-bottom: 12px; }
.loading-sub { font-size: 12px; color: #bbb; margin-top: 4px; }

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}
.empty-icon { font-size: 48px; margin-bottom: 12px; }

/* Card */
.rec-card { margin-bottom: 12px; }
.rec-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}
.rec-name { font-size: 16px; font-weight: 600; margin: 0; }
.rec-tags-row {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  justify-content: flex-end;
  max-width: 60%;
}

/* Nutrition bars */
.rec-nutrition {
  margin-bottom: 8px;
}
.nut-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.nut-label {
  font-size: 12px;
  color: #666;
  min-width: 70px;
}
.nut-bar-track {
  flex: 1;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}
.nut-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}
.nut-value {
  font-size: 11px;
  color: #999;
  min-width: 80px;
  text-align: right;
}

.rec-ingredients, .rec-reason {
  font-size: 13px;
  color: #555;
  margin-bottom: 6px;
  line-height: 1.5;
}
.rec-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

/* Detail modal (same pattern as before, extended) */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-content {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 400px;
  max-height: 80vh;
  overflow-y: auto;
}
.modal-content h3 { margin-bottom: 12px; }
.recipe-detail .detail-tags { margin-bottom: 12px; }
.detail-nutrition {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 10px;
  margin-bottom: 12px;
}
.detail-nut-item {
  text-align: center;
}
.detail-nutrition .nut-value { font-size: 16px; font-weight: 700; color: #4CAF50; display: block; }
.detail-nutrition .nut-label { font-size: 11px; color: #999; }
.detail-section {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.detail-section p { font-size: 14px; color: #555; line-height: 1.6; margin-top: 4px; }
.steps-list {
  padding-left: 20px;
  margin-top: 4px;
}
.steps-list li {
  font-size: 13px;
  color: #555;
  line-height: 1.6;
  margin-bottom: 4px;
}
</style>
