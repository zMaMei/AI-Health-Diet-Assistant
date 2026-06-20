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
      <!-- ===== 营养分析区域 ===== -->
      <h2 class="page-section-title">📊 营养分析</h2>

      <!-- Nutrient rings — clickable per design doc section 6 -->
      <div class="card">
        <div class="nutrient-grid">
          <div class="nutrient-item" v-for="n in nutrients" :key="n.key"
               @click="showSourceDetail(n)">
            <div class="progress-ring" :style="{ borderColor: n.color }">
              <span class="progress-value">{{ getPercent(n.key) }}%</span>
            </div>
            <span class="nutrient-name">{{ n.label }}</span>
            <span class="nutrient-value">{{ getValue(n.key) }} / {{ getGoal(n.key) }}</span>
            <span class="click-hint">点击查看来源</span>
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

      <!-- ===== 健康评分区域 (merged from ScoreView) ===== -->
      <h2 class="page-section-title" style="margin-top:24px">⭐ 健康评分</h2>

      <template v-if="scoreData">
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
              {{ scoreData.suggestions?.[0] || '今日饮食记录不足2餐，无法生成评分' }}
            </p>
            <router-link to="/record" class="btn btn-primary" style="display:inline-block;margin-top:12px">
              去记录
            </router-link>
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
              <span class="history-date">{{ formatScoreDate(h.date) }}</span>
              <div class="history-bar-bg">
                <div class="history-bar" :style="{ width: h.score + '%' }"
                     :class="{ good: h.score >= 70, mid: h.score >= 40 && h.score < 70, bad: h.score < 40 }"></div>
              </div>
              <span class="history-score">{{ h.score }}</span>
            </div>
          </div>
        </div>
      </template>

      <div v-else-if="scoreLoading" class="loading">评分计算中...</div>
    </template>

    <div v-else class="empty-state">
      <div class="empty-icon">📊</div>
      <p>暂无营养数据</p>
    </div>

    <!-- Nutrient source detail modal -->
    <div class="modal-overlay" v-if="showSourceModal" @click.self="showSourceModal=false">
      <div class="modal-content">
        <h3>{{ sourceDetailTitle }}</h3>
        <div v-if="sourceDetailItems?.length">
          <div class="source-item" v-for="(item, i) in sourceDetailItems" :key="i">
            <div class="source-info">
              <strong>{{ item.foodName }}</strong>
              <span style="color:#666;font-size:12px">{{ item.mealType }} · {{ item.amount }}{{ item.unit || '份' }}</span>
            </div>
            <span class="source-value">{{ item.value }}{{ sourceDetailUnit }}</span>
          </div>
        </div>
        <div v-else class="empty-state" style="padding:20px">
          <p>暂无该营养素的食物来源数据</p>
        </div>
        <button class="btn btn-outline" @click="showSourceModal=false" style="margin-top:12px;width:100%">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api/index.js'

const currentDate = ref(new Date().toISOString().split('T')[0])
const nutrition = ref(null)
const scoreData = ref(null)
const loading = ref(false)
const scoreLoading = ref(false)
const circumference = 2 * Math.PI * 54

// Source detail modal
const showSourceModal = ref(false)
const sourceDetailTitle = ref('')
const sourceDetailUnit = ref('')
const sourceDetailItems = ref([])

const displayDate = computed(() => {
  const d = new Date(currentDate.value)
  return `${d.getMonth()+1}月${d.getDate()}日`
})

const isToday = computed(() => currentDate.value === new Date().toISOString().split('T')[0])

const nutrientKeyMap = {
  calorie: { label: '热量', unit: 'kcal' },
  protein: { label: '蛋白质', unit: 'g' },
  fat: { label: '脂肪', unit: 'g' },
  carbohydrate: { label: '碳水', unit: 'g' },
}

const nutrients = [
  { key: 'calorie', label: '热量', color: '#FF5722' },
  { key: 'protein', label: '蛋白质', color: '#2196F3' },
  { key: 'fat', label: '脂肪', color: '#FF9800' },
  { key: 'carbohydrate', label: '碳水', color: '#4CAF50' },
]

const dashOffset = computed(() => {
  if (!scoreData.value?.score) return circumference
  return circumference - (scoreData.value.score / 100) * circumference
})

async function fetchData() {
  loading.value = true
  scoreLoading.value = true
  try {
    const [nutRes, scoreRes] = await Promise.all([
      api.getNutrition(currentDate.value),
      api.getHealthScore(currentDate.value),
    ])
    nutrition.value = nutRes.data.data
    scoreData.value = scoreRes.data.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
    scoreLoading.value = false
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
  const max = Math.max(...nutrition.value.trend.map(p => Number(p.calorie)), 2000)
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

function formatScoreDate(dateStr) {
  const d = new Date(dateStr + 'T00:00:00')
  return `${d.getMonth()+1}/${d.getDate()}`
}

// Click nutrient ring to show food source detail
async function showSourceDetail(nutrient) {
  const info = nutrientKeyMap[nutrient.key]
  sourceDetailTitle.value = `🔍 ${info.label}食物来源`
  sourceDetailUnit.value = info.unit
  try {
    const res = await api.getDietRecords(currentDate.value)
    const allRecords = res.data.data || []
    // Extract records that have nutrition data for this nutrient, grouped by food
    const keyTotal = nutrient.key + 'Total'
    sourceDetailItems.value = allRecords
      .filter(r => r[keyTotal] !== undefined && r[keyTotal] !== null)
      .map(r => ({
        foodName: r.foodName || r.food_name,
        mealType: r.mealType || r.meal_type,
        amount: r.amount,
        unit: r.unit,
        value: Number(r[keyTotal] || r[nutrient.key] || 0).toFixed(1),
      }))
    if (!sourceDetailItems.value.length) {
      sourceDetailItems.value = allRecords.map(r => ({
        foodName: r.foodName || r.food_name,
        mealType: r.mealType || r.meal_type,
        amount: r.amount,
        unit: r.unit,
        value: '—',
      }))
    }
  } catch (e) {
    console.error(e)
    sourceDetailItems.value = []
  }
  showSourceModal.value = true
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

.page-section-title {
  font-size: 17px;
  font-weight: 600;
  margin: 8px 0 12px;
  color: #333;
}

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
  cursor: pointer;
  padding: 4px;
  border-radius: 10px;
  transition: background 0.2s;
}
.nutrient-item:hover { background: #f9f9f9; }
.nutrient-item:active { background: #f0f0f0; }
.click-hint {
  font-size: 10px;
  color: #bbb;
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

/* Score section styles */
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

/* Source detail modal */
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
.modal-content h3 { margin-bottom: 16px; }
.source-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.source-info strong { display: block; margin-bottom: 2px; }
.source-value { font-weight: 600; color: #4CAF50; font-size: 14px; }
</style>
