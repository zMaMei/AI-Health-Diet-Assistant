<template>
  <div class="score-page">
    <div class="date-selector">
      <button @click="changeDate(-1)" class="btn btn-sm btn-outline">前一天</button>
      <span class="current-date">{{ displayDate }}</span>
      <button @click="changeDate(1)" class="btn btn-sm btn-outline" :disabled="isToday">后一天</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <template v-else-if="scoreData">
      <!-- Score ring -->
      <div class="card score-card">
        <div v-if="scoreData.score !== null" class="score-ring">
          <svg viewBox="0 0 120 120" class="score-svg">
            <circle cx="60" cy="60" r="54" fill="none" stroke="#f0f0f0" stroke-width="8"/>
            <circle cx="60" cy="60" r="54" fill="none" stroke="#4CAF50" stroke-width="8"
                    :stroke-dasharray="circumference"
                    :stroke-dashoffset="dashOffset"
                    stroke-linecap="round" transform="rotate(-90 60 60)"/>
          </svg>
          <div class="score-text">
            <span class="score-number">{{ scoreData.score }}</span>
            <span class="score-unit">分</span>
          </div>
        </div>
        <div v-else class="score-empty">
          <p>⚠️ 数据不足</p>
          <p style="font-size:13px;color:#999;margin-top:4px">
            {{ scoreData.suggestions?.[0] || '记录更多饮食后可获得评分' }}
          </p>
        </div>
      </div>

      <!-- Strengths & Risks -->
      <div class="card" v-if="scoreData.strengths?.length">
        <h3 class="card-title">✅ 优点</h3>
        <div class="tag-list">
          <span class="tag tag-green" v-for="s in scoreData.strengths" :key="s">{{ s }}</span>
        </div>
      </div>

      <div class="card" v-if="scoreData.risks?.length">
        <h3 class="card-title">⚠️ 风险项</h3>
        <div class="tag-list">
          <span class="tag tag-orange" v-for="r in scoreData.risks" :key="r">{{ r }}</span>
        </div>
      </div>

      <div class="card" v-if="scoreData.suggestions?.length">
        <h3 class="card-title">💡 改进建议</h3>
        <ul class="suggestion-list">
          <li v-for="(s, i) in scoreData.suggestions" :key="i">{{ s }}</li>
        </ul>
      </div>

      <!-- Score history -->
      <div class="card" v-if="scoreData.history?.length">
        <h3 class="card-title">📈 评分趋势</h3>
        <div class="history-list">
          <div class="history-item" v-for="h in scoreData.history" :key="h.date">
            <span class="history-date">{{ formatDateShort(h.date) }}</span>
            <div class="history-bar-bg">
              <div class="history-bar" :style="{ width: h.score + '%' }"
                   :class="{ good: h.score >= 70, mid: h.score >= 40 && h.score < 70, bad: h.score < 40 }"></div>
            </div>
            <span class="history-score">{{ h.score }}</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="empty-state">
      <div class="empty-icon">⭐</div>
      <p>暂无评分数据</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/index.js'

const currentDate = ref(new Date().toISOString().split('T')[0])
const scoreData = ref(null)
const loading = ref(false)
const circumference = 2 * Math.PI * 54

const displayDate = computed(() => {
  const d = new Date(currentDate.value)
  return `${d.getMonth()+1}月${d.getDate()}日`
})
const isToday = computed(() => currentDate.value === new Date().toISOString().split('T')[0])

const dashOffset = computed(() => {
  if (!scoreData.value?.score) return circumference
  return circumference - (scoreData.value.score / 100) * circumference
})

async function fetchData() {
  loading.value = true
  try {
    const res = await api.getHealthScore(currentDate.value)
    scoreData.value = res
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function changeDate(delta) {
  const d = new Date(currentDate.value)
  d.setDate(d.getDate() + delta)
  currentDate.value = d.toISOString().split('T')[0]
  fetchData()
}

function formatDateShort(dateStr) {
  const d = new Date(dateStr + 'T00:00:00')
  return `${d.getMonth()+1}/${d.getDate()}`
}

onMounted(fetchData)
</script>

<style scoped>
.date-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}
.current-date { font-size: 16px; font-weight: 600; }

.score-card { text-align: center; padding: 24px; }
.score-ring {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto;
}
.score-svg { width: 100%; height: 100%; }
.score-text {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}
.score-number { font-size: 32px; font-weight: 700; color: #4CAF50; display: block; }
.score-unit { font-size: 12px; color: #999; }
.score-empty { color: #999; }

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }

.suggestion-list {
  list-style: none;
}
.suggestion-list li {
  padding: 6px 0;
  font-size: 14px;
  color: #555;
  border-bottom: 1px solid #f0f0f0;
}
.suggestion-list li:last-child { border-bottom: none; }

.history-list { display: flex; flex-direction: column; gap: 8px; }
.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.history-date { font-size: 12px; color: #999; width: 40px; }
.history-bar-bg {
  flex: 1;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}
.history-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}
.history-bar.good { background: #4CAF50; }
.history-bar.mid { background: #FF9800; }
.history-bar.bad { background: #f44336; }
.history-score { font-size: 12px; color: #666; width: 30px; text-align: right; }
</style>
