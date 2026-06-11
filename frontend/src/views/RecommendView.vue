<template>
  <div class="recommend-page">
    <div v-if="loading" class="loading">正在为您生成推荐...</div>

    <template v-else-if="recommendations.length">
      <div class="card" v-for="rec in recommendations" :key="rec.id">
        <div class="rec-header">
          <h3 class="rec-name">{{ rec.recipeName }}</h3>
          <div class="rec-match">
            <span class="match-dot" :style="{ background: matchColor(rec.matchScore) }"></span>
            {{ rec.matchScore }}% 匹配
          </div>
        </div>

        <div class="rec-tags">
          <span class="tag tag-green" v-for="tag in parseTags(rec.tags)" :key="tag">{{ tag }}</span>
        </div>

        <div class="rec-nutrition">
          <span>🔥 {{ Number(rec.calorie).toFixed(0) }} kcal</span>
          <span>🥩 {{ Number(rec.protein).toFixed(1) }}g</span>
          <span>🧈 {{ Number(rec.fat).toFixed(1) }}g</span>
          <span>🌾 {{ Number(rec.carbohydrate).toFixed(1) }}g</span>
        </div>

        <div class="rec-ingredients">
          <strong>食材：</strong>{{ rec.ingredients }}
        </div>

        <div class="rec-reason">
          <strong>推荐理由：</strong>{{ rec.reason }}
        </div>

        <div class="rec-actions">
          <button class="btn btn-sm btn-outline" @click="dislike(rec.id)">
            👎 不喜欢
          </button>
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <div class="empty-icon">🍽️</div>
      <p>暂无推荐菜谱</p>
      <p style="font-size:12px;color:#bbb;margin-top:4px">请先在"我的"页面设置健康目标和偏好</p>
      <button class="btn btn-primary" @click="fetchRecommendations" style="margin-top:12px">刷新推荐</button>
    </div>

    <!-- No more message -->
    <div v-if="noMore && recommendations.length > 0" class="no-more">
      已经到底了，没有更多推荐~
    </div>

    <!-- Alert check -->
    <div class="card alert-card" v-if="alerts.length" style="border-left:3px solid #f44336">
      <h3 class="card-title" style="color:#f44336">🔔 饮食预警</h3>
      <div v-for="(alert, i) in alerts" :key="i" style="margin:4px 0">
        <p style="font-size:13px">{{ alert.message }}</p>
        <p style="font-size:12px;color:#999">{{ alert.suggestion }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/index.js'

const recommendations = ref([])
const alerts = ref([])
const loading = ref(false)
const noMore = ref(false)
const today = new Date().toISOString().split('T')[0]

async function fetchRecommendations() {
  loading.value = true
  try {
    const [recRes, alertRes] = await Promise.all([
      api.getRecommendations(),
      api.checkAlerts(today)
    ])
    recommendations.value = recRes.data.data || []
    const alertData = alertRes.data.data
    if (alertData?.hasAlert) {
      alerts.value = alertData.alerts || []
    } else {
      alerts.value = []
    }
    noMore.value = recommendations.value.length === 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function dislike(recommendationId) {
  try {
    const res = await api.submitFeedback(recommendationId, 'dislike')
    if (res.data.data) {
      // Replace the disliked one with a new recommendation
      const idx = recommendations.value.findIndex(r => r.id === recommendationId)
      if (idx >= 0) {
        recommendations.value[idx] = res.data.data
      }
    } else {
      // Remove if no replacement
      recommendations.value = recommendations.value.filter(r => r.id !== recommendationId)
    }
  } catch (e) {
    alert('操作失败')
  }
}

function parseTags(tags) {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

function matchColor(score) {
  if (!score) return '#999'
  return score >= 70 ? '#4CAF50' : score >= 40 ? '#FF9800' : '#f44336'
}

onMounted(fetchRecommendations)
</script>

<style scoped>
.rec-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.rec-name { font-size: 16px; font-weight: 600; }
.rec-match {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
}
.match-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.rec-tags { margin-bottom: 8px; }

.rec-nutrition {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #666;
  padding: 8px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
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

.no-more {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
}
.alert-card { margin-top: 12px; }
</style>
