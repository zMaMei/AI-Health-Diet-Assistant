<template>
  <div class="nutrition-page">
    <!-- ===== 区域①: 日期选择器 ===== -->
    <div class="date-selector">
      <button @click="changeDate(-1)" class="btn btn-sm btn-outline">◀</button>
      <input type="date" v-model="currentDate" :max="todayStr"
             class="date-input" @change="onDateChange">
      <button @click="changeDate(1)" class="btn btn-sm btn-outline" :disabled="isToday">▶</button>
      <button v-if="!isToday" @click="goToday" class="btn btn-sm btn-outline today-btn">今天</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <template v-else>
      <!-- ===== 区域②: 当日饮食记录（可编辑） ===== -->
      <h2 class="page-section-title">🍽️ 当日饮食</h2>

      <template v-for="meal in mealTypes" :key="meal.key">
        <div class="meal-card card" v-if="groupedRecords[meal.key]?.length"
             :class="{ expanded: expandedMeals[meal.key] }">
          <div class="meal-header" @click="toggleMeal(meal.key)">
            <div class="meal-header-left">
              <span class="meal-icon">{{ meal.icon }}</span>
              <span class="meal-name">{{ meal.label }}</span>
              <span class="meal-count">{{ groupedRecords[meal.key].length }}种</span>
            </div>
            <div class="meal-header-right">
              <span class="meal-summary">🔥{{ mealTotals[meal.key]?.calorie || 0 }}</span>
              <span class="meal-summary-sub">🥩{{ mealTotals[meal.key]?.protein || 0 }}g</span>
              <span class="meal-arrow">{{ expandedMeals[meal.key] ? '▴' : '▾' }}</span>
            </div>
          </div>
          <div class="meal-body" v-show="expandedMeals[meal.key]">
            <!-- Photo carousel -->
            <div class="photo-carousel" v-if="mealPhotos[meal.key]?.length">
              <div class="photo-track">
                <div class="photo-thumb" v-for="(photo, pi) in mealPhotos[meal.key]" :key="pi"
                     @click="openPhotoPreview(photo.imageUrl || photo)">
                  <img :src="photoFullUrl(photo.imageUrl || photo)" alt="食物照片" loading="lazy">
                </div>
              </div>
            </div>

            <div class="meal-divider" v-if="mealPhotos[meal.key]?.length || voiceRecordsByMeal[meal.key]?.length"></div>

            <!-- Voice records -->
            <div class="voice-records-mini" v-if="voiceRecordsByMeal[meal.key]?.length">
              <div class="voice-mini-card" v-for="vr in voiceRecordsByMeal[meal.key]" :key="vr.id">
                <div class="voice-mini-header">
                  <span class="voice-mini-icon">🎵</span>
                  <span class="voice-mini-duration">{{ vr.durationSeconds || 0 }}秒</span>
                  <button class="voice-play-btn" @click.stop="playVoice(vr)">
                    {{ playingVoiceId === vr.id ? '⏸ 暂停' : '▶️ 播放' }}
                  </button>
                  <button class="voice-delete-btn" @click.stop="deleteVoiceRecord(vr.id)">删除</button>
                </div>
                <div class="voice-mini-text">{{ vr.transcribedText || '(无识别文本)' }}</div>
              </div>
            </div>

            <!-- Food items -->
            <div class="food-item" v-for="rec in groupedRecords[meal.key]" :key="rec.id">
              <div class="food-main">
                <div class="food-name-row">
                  <strong>{{ rec.foodName }}</strong>
                  <span class="food-amount">{{ rec.amount }}{{ rec.unit || '份' }}</span>
                  <span class="food-source">{{ sourceLabels[rec.source] || rec.source }}</span>
                </div>
                <div class="food-nutrition">
                  <span>🔥{{ rec.calorie || 0 }}kcal</span>
                  <span>🥩{{ rec.protein || 0 }}g</span>
                  <span>🧈{{ rec.fat || 0 }}g</span>
                  <span>🌾{{ rec.carbohydrate || 0 }}g</span>
                </div>
              </div>
              <div class="food-actions">
                <button class="btn btn-sm btn-outline" @click.stop="editRecord(rec)">编辑</button>
                <button class="btn btn-sm btn-danger" @click.stop="deleteRecord(rec.id)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <div v-if="!Object.values(groupedRecords).some(g => g.length)" class="empty-state" style="padding:20px">
        <p>当天还没有饮食记录</p>
        <router-link to="/record" class="btn btn-primary" style="display:inline-block;margin-top:8px">去记录</router-link>
      </div>

      <!-- ===== 区域③: 健康评分 ===== -->
      <h2 class="page-section-title">⭐ 健康评分</h2>

      <template v-if="scoreData">
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
          </div>
        </div>

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

      <!-- ===== 区域④: 营养分析 ===== -->
      <h2 class="page-section-title">📊 营养分析</h2>

      <template v-if="nutrition">
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
          <div v-else class="empty-state" style="padding:20px"><p>暂无趋势数据</p></div>
        </div>
        <div class="card" v-if="nutrition.suggestion">
          <h3 class="card-title">💡 饮食建议</h3>
          <p class="suggestion-text">{{ nutrition.suggestion }}</p>
        </div>
      </template>
      <div v-else class="empty-state"><div class="empty-icon">📊</div><p>暂无营养数据</p></div>
    </template>

    <!-- ===== 区域⑤: AI 智能分析（对话式） ===== -->
    <h2 class="page-section-title">🤖 AI 智能分析</h2>
    <div class="card ai-chat-card">
      <div class="chat-messages" ref="chatMsgsRef">
        <div v-if="aiLoading" class="loading" style="padding:12px">AI 思考中...</div>
        <div v-if="!aiMessages.length && !aiLoading" class="chat-empty">
          <p>点击下方按钮，让 AI 帮你分析今日饮食</p>
          <button class="btn btn-primary" @click="startAiAnalysis" style="margin-top:8px">
            🤖 开始分析
          </button>
        </div>
        <div v-for="(msg, i) in aiMessages" :key="i"
             :class="['chat-bubble', msg.role === 'AI' ? 'ai' : 'user']">
          <div class="bubble-avatar">{{ msg.role === 'AI' ? '🤖' : '👤' }}</div>
          <div class="bubble-content">
            <div class="bubble-text" v-html="renderMarkdown(msg.content)"></div>
            <div class="bubble-time">{{ formatMsgTime(msg.createdAt) }}</div>
          </div>
        </div>
      </div>

      <div class="chat-input-area">
        <input v-model="aiInput" type="text" class="chat-input"
               placeholder="输入你的问题，如：昨天的蛋白质够吗？"
               @keyup.enter="sendAiMessage"
               :disabled="aiLoading">
        <button class="btn btn-primary chat-send-btn"
                @click="sendAiMessage" :disabled="aiLoading || !aiInput.trim()">
          发送
        </button>
      </div>
    </div>

    <!-- Photo preview lightbox -->
    <div class="lightbox-overlay" v-if="previewPhoto" @click="closePhotoPreview">
      <img :src="previewPhoto" alt="食物照片大图" class="lightbox-img" @click.stop>
    </div>

    <!-- Hidden audio player -->
    <audio ref="voiceAudioRef" style="display:none" @ended="onVoiceEnded" preload="auto"></audio>

    <!-- Edit record modal -->
    <div class="modal-overlay" v-if="showEditModal" @click.self="showEditModal=false">
      <div class="modal-content">
        <h3>✏️ 编辑饮食记录</h3>
        <div class="form-group">
          <label>食物名称</label>
          <input v-model="editForm.foodName">
        </div>
        <div class="form-group">
          <label>餐次</label>
          <select v-model="editForm.mealType">
            <option value="早餐">早餐</option>
            <option value="午餐">午餐</option>
            <option value="晚餐">晚餐</option>
            <option value="夜宵">夜宵</option>
            <option value="其他">其他</option>
          </select>
        </div>
        <div class="form-group">
          <label>份量</label>
          <input type="number" v-model.number="editForm.amount" min="0.1" step="0.5">
        </div>
        <button class="btn btn-primary" @click="saveEdit">保存修改</button>
        <button class="btn btn-outline" @click="showEditModal=false" style="margin-top:8px">取消</button>
      </div>
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
        <div v-else class="empty-state" style="padding:20px"><p>暂无该营养素的食物来源数据</p></div>
        <button class="btn btn-outline" @click="showSourceModal=false" style="margin-top:12px;width:100%">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, nextTick } from 'vue'
import api from '../api/index.js'
import toast from '../toast.js'

const todayStr = new Date().toISOString().split('T')[0]
const currentDate = ref(todayStr)
const nutrition = ref(null)
const scoreData = ref(null)
const loading = ref(false)
const scoreLoading = ref(false)
const circumference = 2 * Math.PI * 54

// Diet records state
const records = ref([])
const mealPhotos = ref({})
const voiceRecords = ref([])
const mealTypes = [
  { key: '早餐', label: '早餐', icon: '🍳' },
  { key: '午餐', label: '午餐', icon: '🍚' },
  { key: '晚餐', label: '晚餐', icon: '🍜' },
  { key: '夜宵', label: '夜宵', icon: '🌙' },
  { key: '其他', label: '其他', icon: '🍽️' },
]
const sourceLabels = { photo: '拍照', voice: '语音', manual: '手动' }
const expandedMeals = reactive({ '早餐': true, '午餐': true, '晚餐': false, '夜宵': false, '其他': false })

const groupedRecords = computed(() => {
  const groups = {}
  mealTypes.forEach(m => { groups[m.key] = [] })
  records.value.forEach(r => {
    if (groups[r.mealType] !== undefined) groups[r.mealType].push(r)
    else groups['其他'].push(r)
  })
  return groups
})

const mealTotals = computed(() => {
  const totals = {}
  mealTypes.forEach(m => {
    const items = groupedRecords.value[m.key]
    if (items?.length) {
      totals[m.key] = {
        calorie: items.reduce((s, r) => s + (Number(r.calorie) || 0), 0).toFixed(0),
        protein: items.reduce((s, r) => s + (Number(r.protein) || 0), 0).toFixed(1),
      }
    }
  })
  return totals
})

const voiceRecordsByMeal = computed(() => {
  const groups = {}
  voiceRecords.value.forEach(v => {
    if (!groups[v.mealType]) groups[v.mealType] = []
    groups[v.mealType].push(v)
  })
  return groups
})

// Photo lightbox
const previewPhoto = ref(null)
function photoFullUrl(relativePath) {
  if (!relativePath) return ''
  return '/api/uploads' + relativePath
}
function openPhotoPreview(url) { previewPhoto.value = photoFullUrl(url) }
function closePhotoPreview() { previewPhoto.value = null }

// Voice playback
const voiceAudioRef = ref(null)
const playingVoiceId = ref(null)
function playVoice(vr) {
  if (playingVoiceId.value === vr.id) {
    voiceAudioRef.value?.pause()
    playingVoiceId.value = null
    return
  }
  const audio = voiceAudioRef.value
  if (audio) {
    audio.src = '/api/uploads' + vr.audioUrl
    audio.play().catch(e => console.error('语音播放失败', e))
    playingVoiceId.value = vr.id
  }
}
function onVoiceEnded() { playingVoiceId.value = null }
async function deleteVoiceRecord(id) {
  if (!confirm('确定删除这条语音记录？')) return
  try {
    await api.deleteVoiceRecord(id)
    await fetchAll()
  } catch (e) {
    console.error('删除语音记录失败', e)
    toast.show('删除失败')
  }
}

// Edit modal
const showEditModal = ref(false)
const editForm = ref({ id: null, foodName: '', mealType: '午餐', amount: 1 })

// AI chat state
const aiMessages = ref([])
const aiInput = ref('')
const aiLoading = ref(false)
const chatMsgsRef = ref(null)

// Source detail modal
const showSourceModal = ref(false)
const sourceDetailTitle = ref('')
const sourceDetailUnit = ref('')
const sourceDetailItems = ref([])

// Computed
const isToday = computed(() => currentDate.value === todayStr)
const dashOffset = computed(() => {
  if (!scoreData.value?.score) return circumference
  return circumference - (scoreData.value.score / 100) * circumference
})

// Nutrient config
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

// Date handling
function changeDate(delta) {
  const d = new Date(currentDate.value + 'T00:00:00')
  d.setDate(d.getDate() + delta)
  if (d > new Date()) return
  currentDate.value = d.toISOString().split('T')[0]
  onDateChange()
}

function goToday() {
  currentDate.value = todayStr
  onDateChange()
}

function onDateChange() {
  fetchAll()
}

// Data fetching
async function fetchAll() {
  loading.value = true
  scoreLoading.value = true
  try {
    const [recRes, nutRes, scoreRes, photoRes, voiceRes, convRes] = await Promise.all([
      api.getDietRecords(currentDate.value),
      api.getNutrition(currentDate.value),
      api.getHealthScore(currentDate.value),
      api.getMealPhotos(currentDate.value),
      api.getVoiceRecords(currentDate.value),
      api.getAiConversation(currentDate.value),
    ])
    records.value = (recRes.data?.data) || []
    nutrition.value = (nutRes.data?.data) || null
    scoreData.value = (scoreRes.data?.data) || null

    // Group photos by meal type
    const photos = {}
    const photoList = (photoRes.data?.data) || []
    photoList.forEach(p => {
      if (!photos[p.mealType]) photos[p.mealType] = []
      photos[p.mealType].push(p)
    })
    mealPhotos.value = photos

    // Store voice records
    voiceRecords.value = (voiceRes.data?.data) || []

    // Auto-expand meals with records
    mealTypes.forEach(m => {
      if (groupedRecords.value[m.key]?.length) expandedMeals[m.key] = true
    })

    // Load conversation history
    const convData = (convRes.data?.data)
    if (convData?.messages?.length) {
      aiMessages.value = convData.messages
    } else {
      aiMessages.value = []
    }
  } catch (e) {
    console.error('获取数据失败', e)
    toast.show('数据加载失败，请稍后重试')
  } finally {
    loading.value = false
    scoreLoading.value = false
  }
}

// Diet record actions
function toggleMeal(key) { expandedMeals[key] = !expandedMeals[key] }

function editRecord(rec) {
  editForm.value = { id: rec.id, foodName: rec.foodName, mealType: rec.mealType, amount: rec.amount }
  showEditModal.value = true
}

async function saveEdit() {
  try {
    await api.updateDietRecord(editForm.value.id, {
      foodName: editForm.value.foodName,
      mealType: editForm.value.mealType,
      amount: editForm.value.amount,
    })
    showEditModal.value = false
    await fetchAll()
  } catch (e) {
    console.error('编辑失败', e)
    toast.show('保存失败：' + (e?.response?.data?.message || e.message || '未知错误'))
  }
}

async function deleteRecord(id) {
  if (!confirm('确定删除这条记录？')) return
  try {
    await api.deleteDietRecord(id)
    await fetchAll()
  } catch (e) {
    console.error('删除失败', e)
    toast.show('删除失败')
  }
}

// AI chat actions
async function startAiAnalysis() {
  aiLoading.value = true
  try {
    const res = await api.sendAiMessage(currentDate.value, '请帮我分析今天的饮食情况，包括营养摄入是否均衡、哪些方面做得好、哪些需要改进。')
    const data = res.data?.data
    if (data?.messages?.length) {
      aiMessages.value = data.messages
    }
  } catch (e) {
    console.error('AI 分析失败', e)
    toast.show('AI 分析失败，请稍后重试')
  } finally {
    aiLoading.value = false
    scrollChatBottom()
  }
}

async function sendAiMessage() {
  const msg = aiInput.value.trim()
  if (!msg || aiLoading.value) return
  aiInput.value = ''
  aiLoading.value = true
  aiMessages.value.push({ role: 'USER', content: msg, createdAt: new Date().toISOString() })
  scrollChatBottom()
  try {
    const res = await api.sendAiMessage(currentDate.value, msg)
    const data = res.data?.data
    if (data?.messages?.length) {
      const aiMsg = data.messages.filter(m => m.role === 'AI').pop()
      if (aiMsg) {
        aiMessages.value.push(aiMsg)
      }
    }
  } catch (e) {
    console.error('AI 回复失败', e)
    aiMessages.value.push({ role: 'AI', content: '抱歉，AI 服务暂时不可用，请稍后重试。', createdAt: new Date().toISOString() })
  } finally {
    aiLoading.value = false
    scrollChatBottom()
  }
}

function scrollChatBottom() {
  nextTick(() => {
    const el = chatMsgsRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function renderMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

function formatMsgTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr + (timeStr.endsWith('Z') ? '' : 'Z'))
  if (isNaN(d.getTime())) return ''
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

// Nutrition helpers
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
function isTodayDate(dateStr) { return dateStr === todayStr }
function formatDateShort(dateStr) {
  const d = new Date(dateStr + 'T00:00:00')
  const days = ['日','一','二','三','四','五','六']
  return '周' + days[d.getDay()]
}
function formatScoreDate(dateStr) {
  const d = new Date(dateStr + 'T00:00:00')
  return `${d.getMonth()+1}/${d.getDate()}`
}

async function showSourceDetail(nutrient) {
  const info = nutrientKeyMap[nutrient.key]
  sourceDetailTitle.value = `🔍 ${info.label}食物来源`
  sourceDetailUnit.value = info.unit
  try {
    const res = await api.getDietRecords(currentDate.value)
    const allRecords = (res.data?.data) || []
    const keyTotal = nutrient.key + 'Total'
    sourceDetailItems.value = allRecords
      .filter(r => (r[keyTotal] !== undefined && r[keyTotal] !== null) || (r[nutrient.key] !== undefined && r[nutrient.key] !== null))
      .map(r => ({
        foodName: r.foodName,
        mealType: r.mealType,
        amount: r.amount,
        unit: r.unit,
        value: Number(r[keyTotal] || r[nutrient.key] || 0).toFixed(1),
      }))
    if (!sourceDetailItems.value.length) {
      sourceDetailItems.value = allRecords.map(r => ({
        foodName: r.foodName, mealType: r.mealType, amount: r.amount, unit: r.unit, value: '—',
      }))
    }
  } catch (e) { console.error(e); sourceDetailItems.value = [] }
  showSourceModal.value = true
}

onMounted(fetchAll)
</script>

<style scoped>
/* ---- Date selector ---- */
.date-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 16px;
}
.date-input {
  width: auto;
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  text-align: center;
}
.today-btn { padding: 6px 14px; }

.page-section-title {
  font-size: 17px;
  font-weight: 600;
  margin: 18px 0 10px;
  color: #333;
}

/* ---- Meal accordion ---- */
.meal-card { padding: 0 !important; overflow: hidden; margin-bottom: 10px; border-left: 3px solid #4CAF50; }
.meal-card.expanded { border-left-color: #388E3C; }
.meal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; cursor: pointer; user-select: none;
}
.meal-header:active { background: #F5F5F5; }
.meal-header-left { display: flex; align-items: center; gap: 8px; }
.meal-icon { font-size: 18px; }
.meal-name { font-size: 14px; font-weight: 600; }
.meal-count { font-size: 11px; color: #999; background: #f0f0f0; padding: 2px 8px; border-radius: 10px; }
.meal-header-right { display: flex; align-items: center; gap: 8px; }
.meal-summary { font-size: 15px; font-weight: 700; color: #E65100; }
.meal-summary-sub { font-size: 11px; color: #999; }
.meal-arrow { font-size: 14px; color: #bbb; }
.meal-body { padding: 0 14px 10px 14px; }

/* ---- Photo carousel ---- */
.photo-carousel { margin-bottom: 10px; overflow: hidden; }
.photo-track {
  display: flex; gap: 8px; overflow-x: auto;
  scroll-snap-type: x mandatory; -webkit-overflow-scrolling: touch; padding: 4px 0;
}
.photo-track::-webkit-scrollbar { height: 4px; }
.photo-track::-webkit-scrollbar-thumb { background: #ccc; border-radius: 2px; }
.photo-thumb {
  flex-shrink: 0; width: 120px; height: 100px;
  border-radius: 10px; overflow: hidden; scroll-snap-align: start;
  cursor: pointer; border: 2px solid #eee; transition: border-color 0.2s;
}
.photo-thumb:hover { border-color: #4CAF50; }
.photo-thumb img { width: 100%; height: 100%; object-fit: cover; }

.meal-divider { height: 1px; background: #f0f0f0; margin-bottom: 8px; }

/* ---- Voice mini cards ---- */
.voice-records-mini { display: flex; flex-direction: column; gap: 6px; margin-bottom: 8px; }
.voice-mini-card {
  background: #F3E5F5; border-radius: 10px; padding: 10px 12px; border-left: 3px solid #9C27B0;
}
.voice-mini-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.voice-mini-icon { font-size: 16px; }
.voice-mini-duration { font-size: 12px; color: #7B1FA2; font-weight: 500; }
.voice-play-btn {
  padding: 4px 12px; border-radius: 6px; border: 1px solid #9C27B0;
  background: #fff; color: #9C27B0; font-size: 12px; font-weight: 600; cursor: pointer;
}
.voice-play-btn:hover { background: #F3E5F5; }
.voice-delete-btn {
  margin-left: auto; padding: 2px 8px; border-radius: 4px; border: 1px solid #e0e0e0;
  background: #fff; color: #999; font-size: 11px; cursor: pointer;
}
.voice-mini-text {
  font-size: 12px; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* ---- Food item ---- */
.food-item {
  display: flex; justify-content: space-between; align-items: flex-start;
  padding: 10px 12px; background: #FAFAFA; border-radius: 10px; margin-bottom: 8px;
}
.food-main { flex: 1; min-width: 0; }
.food-name-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-bottom: 4px; }
.food-name-row strong { font-size: 14px; }
.food-amount { font-size: 12px; color: #666; background: #E8F5E9; padding: 1px 6px; border-radius: 4px; }
.food-source { font-size: 11px; color: #999; background: #f0f0f0; padding: 1px 6px; border-radius: 4px; }
.food-nutrition { display: flex; flex-wrap: wrap; gap: 6px; font-size: 11px; color: #888; }
.food-actions { display: flex; gap: 6px; flex-shrink: 0; margin-left: 8px; }

/* ---- Lightbox ---- */
.lightbox-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.85); z-index: 300;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
}
.lightbox-img { max-width: 92%; max-height: 85vh; border-radius: 12px; object-fit: contain; }

/* ---- Score ---- */
.score-card { text-align: center; padding: 24px; }
.score-ring { position: relative; width: 120px; height: 120px; margin: 0 auto; }
.score-svg { width: 100%; height: 100%; }
.score-text { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); text-align: center; }
.score-number { font-size: 32px; font-weight: 700; color: #4CAF50; display: block; }
.score-unit { font-size: 12px; color: #999; }
.score-empty { color: #999; }

.card-title { font-size: 15px; font-weight: 600; margin-bottom: 8px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 6px; }
.suggestion-list { list-style: none; }
.suggestion-list li { padding: 6px 0; font-size: 14px; color: #555; border-bottom: 1px solid #f0f0f0; }
.suggestion-list li:last-child { border-bottom: none; }
.history-list { display: flex; flex-direction: column; gap: 8px; }
.history-item { display: flex; align-items: center; gap: 8px; }
.history-date { font-size: 12px; color: #999; width: 40px; }
.history-bar-bg { flex: 1; height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden; }
.history-bar { height: 100%; border-radius: 4px; transition: width 0.3s; }
.history-bar.good { background: #4CAF50; }
.history-bar.mid { background: #FF9800; }
.history-bar.bad { background: #f44336; }
.history-score { font-size: 12px; color: #666; width: 30px; text-align: right; }

/* ---- Nutrition ---- */
.nutrient-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.nutrient-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  cursor: pointer; padding: 4px; border-radius: 10px; transition: background 0.2s;
}
.nutrient-item:hover { background: #f9f9f9; }
.click-hint { font-size: 10px; color: #bbb; }
.progress-ring {
  width: 64px; height: 64px; border-radius: 50%; border: 4px solid;
  display: flex; align-items: center; justify-content: center; background: #fafafa;
}
.progress-value { font-size: 14px; font-weight: 700; }
.nutrient-name { font-size: 13px; color: #666; }
.nutrient-value { font-size: 11px; color: #999; }
.trend-chart { display: flex; align-items: flex-end; gap: 4px; height: 120px; }
.bar-container { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; }
.bar-wrapper { flex: 1; width: 100%; display: flex; align-items: flex-end; justify-content: center; }
.bar {
  width: 70%; background: linear-gradient(to top, #81C784, #4CAF50);
  border-radius: 4px 4px 0 0; min-height: 4px; transition: height 0.3s;
}
.bar.today { background: linear-gradient(to top, #FFB74D, #FF9800); }
.bar-label { font-size: 10px; color: #999; margin-top: 4px; }
.suggestion-text { font-size: 14px; line-height: 1.6; color: #555; }

/* ---- AI Chat ---- */
.ai-chat-card { padding: 12px !important; }
.chat-messages {
  max-height: 350px; overflow-y: auto; padding: 8px 4px;
  display: flex; flex-direction: column; gap: 10px;
}
.chat-empty { text-align: center; padding: 24px; color: #999; }
.chat-bubble { display: flex; gap: 8px; }
.chat-bubble.ai { align-self: flex-start; }
.chat-bubble.user { align-self: flex-end; flex-direction: row-reverse; }
.bubble-avatar { font-size: 22px; flex-shrink: 0; margin-top: 2px; }
.bubble-content { max-width: 85%; }
.bubble-text {
  padding: 10px 14px; border-radius: 14px; font-size: 14px; line-height: 1.6;
  word-break: break-word;
}
.chat-bubble.ai .bubble-text { background: #F1F8E9; color: #333; }
.chat-bubble.user .bubble-text { background: #4CAF50; color: #fff; }
.bubble-time { font-size: 10px; color: #bbb; margin-top: 2px; padding: 0 4px; }
.chat-bubble.user .bubble-time { text-align: right; }
.chat-input-area { display: flex; gap: 8px; margin-top: 10px; padding-top: 10px; border-top: 1px solid #f0f0f0; }
.chat-input { flex: 1; padding: 10px 12px; border: 1px solid #ddd; border-radius: 20px; font-size: 14px; }
.chat-send-btn { border-radius: 20px; padding: 10px 20px; }

/* ---- Modals ---- */
.modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); z-index: 200;
  display: flex; align-items: center; justify-content: center;
}
.modal-content {
  background: #fff; border-radius: 16px; padding: 24px;
  width: 90%; max-width: 400px; max-height: 80vh; overflow-y: auto;
}
.modal-content h3 { margin-bottom: 16px; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; color: #666; margin-bottom: 4px; }
.source-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f0f0f0; }
.source-info strong { display: block; margin-bottom: 2px; }
.source-value { font-weight: 600; color: #4CAF50; font-size: 14px; }
</style>
