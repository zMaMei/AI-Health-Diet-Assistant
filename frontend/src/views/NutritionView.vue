<template>
  <div class="nutrition-page">
    <!-- Date selector -->
    <div class="date-selector">
      <button @click="changeDate(-1)" class="btn btn-sm btn-outline">前一天</button>
      <span class="current-date">{{ displayDate }}</span>
      <button @click="changeDate(1)" class="btn btn-sm btn-outline" :disabled="isToday">后一天</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <template v-else-if="nutrition">
      <!-- Nutrient rings -->
      <div class="card">
        <div class="nutrient-grid">
          <div class="nutrient-item" v-for="n in nutrients" :key="n.key">
            <div class="progress-ring" :style="{ borderColor: n.color }">
              <span class="progress-value">{{ getPercent(n.key) }}%</span>
            </div>
            <span class="nutrient-name">{{ n.label }}</span>
            <span class="nutrient-value">{{ getValue(n.key) }} / {{ getGoal(n.key) }}</span>
          </div>
        </div>
      </div>

      <!-- Weekly trend chart -->
      <div class="card">
        <h3 class="card-title">📈 近一周热量趋势</h3>
        <div class="trend-chart" v-if="nutrition.trend?.length">
          <div class="bar-container" v-for="(point, i) in nutrition.trend" :key="i">
            <div class="bar-wrapper">
              <div class="bar" :style="{ height: barHeight(point.calorie) + '%' }"
                   :class="{ today: isTodayDate(point.date) }"></div>
            </div>
            <span class="bar-label">{{ formatDateShort(point.date) }}</span>
          </div>
        </div>
        <div v-else class="empty-state" style="padding:20px">
          <p>暂无趋势数据</p>
        </div>
      </div>

      <!-- Dietary suggestion -->
      <div class="card" v-if="nutrition.suggestion">
        <h3 class="card-title">💡 饮食建议</h3>
        <p class="suggestion-text">{{ nutrition.suggestion }}</p>
      </div>
    </template>

    <div v-else class="empty-state">
      <div class="empty-icon">📊</div>
      <p>暂无营养数据</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/index.js'

const currentDate = ref(new Date().toISOString().split('T')[0])
const nutrition = ref(null)
const loading = ref(false)

const displayDate = computed(() => {
  const d = new Date(currentDate.value)
  return `${d.getMonth()+1}月${d.getDate()}日`
})

const isToday = computed(() => currentDate.value === new Date().toISOString().split('T')[0])

const nutrients = [
  { key: 'calorie', label: '热量', color: '#FF5722' },
  { key: 'protein', label: '蛋白质', color: '#2196F3' },
  { key: 'fat', label: '脂肪', color: '#FF9800' },
  { key: 'carbohydrate', label: '碳水', color: '#4CAF50' },
]

async function fetchData() {
  loading.value = true
  try {
    const res = await api.getNutrition(currentDate.value)
    nutrition.value = res.data.data
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

function getValue(key) {
  if (!nutrition.value) return 0
  const v = nutrition.value[key + 'Total']
  return v ? Number(v).toFixed(0) : 0
}

function getGoal(key) {
  if (!nutrition.value) return 0
  const g = nutrition.value[key + 'Goal']
  return g ? Number(g).toFixed(0) : 0
}

function getPercent(key) {
  const v = parseFloat(getValue(key))
  const g = parseFloat(getGoal(key))
  if (!g) return 0
  return Math.min(100, Math.round(v / g * 100))
}

function barHeight(cal) {
  if (!cal) return 0
  const max = Math.max(...nutrition.trend.map(p => Number(p.calorie)), 2000)
  return Math.min(100, Number(cal) / max * 100)
}

function isTodayDate(dateStr) {
  return dateStr === new Date().toISOString().split('T')[0]
}

function formatDateShort(dateStr) {
  const d = new Date(dateStr + 'T00:00:00')
  const days = ['日','一','二','三','四','五','六']
  return '周' + days[d.getDay()]
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

.nutrient-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.nutrient-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.progress-ring {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 4px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}
.progress-value {
  font-size: 14px;
  font-weight: 700;
}
.nutrient-name {
  font-size: 13px;
  color: #666;
}
.nutrient-value {
  font-size: 11px;
  color: #999;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 120px;
}
.bar-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}
.bar-wrapper {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.bar {
  width: 70%;
  background: linear-gradient(to top, #81C784, #4CAF50);
  border-radius: 4px 4px 0 0;
  min-height: 4px;
  transition: height 0.3s;
}
.bar.today { background: linear-gradient(to top, #FFB74D, #FF9800); }
.bar-label {
  font-size: 10px;
  color: #999;
  margin-top: 4px;
}

.suggestion-text {
  font-size: 14px;
  line-height: 1.6;
  color: #555;
}
</style>
