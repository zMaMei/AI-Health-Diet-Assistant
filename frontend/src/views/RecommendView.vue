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
          <button class="btn btn-sm btn-outline" @click="viewDetail(rec)">
            📋 查看详情
          </button>
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

    <!-- Recipe Detail Modal -->
    <div class="modal-overlay" v-if="showDetailModal" @click.self="showDetailModal=false">
      <div class="modal-content recipe-detail">
        <h3>🍳 {{ detailRecipe?.recipeName }}</h3>
        <div class="detail-tags" v-if="detailRecipe?.tags">
          <span class="tag tag-green" v-for="tag in parseTags(detailRecipe.tags)" :key="tag">{{ tag }}</span>
        </div>
        <div class="detail-nutrition">
          <div class="detail-nut-item">
            <span class="nut-value">{{ Number(detailRecipe?.calorie).toFixed(0) }}</span>
            <span class="nut-label">热量(kcal)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ Number(detailRecipe?.protein).toFixed(1) }}</span>
            <span class="nut-label">蛋白质(g)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ Number(detailRecipe?.fat).toFixed(1) }}</span>
            <span class="nut-label">脂肪(g)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ Number(detailRecipe?.carbohydrate).toFixed(1) }}</span>
            <span class="nut-label">碳水(g)</span>
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

const recommendations = ref([])
const loading = ref(false)
const noMore = ref(false)
const showDetailModal = ref(false)
const detailRecipe = ref(null)

async function fetchRecommendations() {
  loading.value = true
  try {
    const res = await api.getRecommendations()
    recommendations.value = res.data.data || []
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
  gap: 8px;
  margin-top: 8px;
}

.no-more {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
}

/* Recipe detail modal */
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
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 10px;
  margin-bottom: 12px;
}
.detail-nut-item {
  text-align: center;
}
.nut-value { font-size: 18px; font-weight: 700; color: #4CAF50; display: block; }
.nut-label { font-size: 11px; color: #999; }
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
