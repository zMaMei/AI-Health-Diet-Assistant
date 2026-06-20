<template>
  <div class="record-page">

    <!-- Quick add buttons -->
    <div class="quick-actions">
      <button class="action-btn" @click="openPhotoModal">
        <span class="action-icon">📷</span>
        <span>拍照识别</span>
      </button>
      <button class="action-btn" @click="openVoiceModal">
        <span class="action-icon">🎤</span>
        <span>语音输入</span>
      </button>
      <button class="action-btn" @click="openManualModal">
        <span class="action-icon">✏️</span>
        <span>手动添加</span>
      </button>
    </div>

    <!-- Alert warnings -->
    <div class="card alert-card" v-if="alerts.length" style="border-left:3px solid #f44336">
      <h3 class="card-title" style="color:#f44336">🔔 饮食预警</h3>
      <div v-for="(alert, i) in alerts" :key="i" style="margin:4px 0">
        <p style="font-size:13px">{{ alert.message }}</p>
        <p style="font-size:12px;color:#999">{{ alert.suggestion }}</p>
      </div>
    </div>

    <!-- Today's nutrition summary -->
    <div class="card summary-card" v-if="todayNutrition">
      <h3 class="card-title">📊 今日摄入总览</h3>
      <div class="summary-grid">
        <div class="summary-item">
          <span class="summary-value">{{ todayNutrition.calorieTotal }}</span>
          <span class="summary-label">热量(kcal)</span>
        </div>
        <div class="summary-item">
          <span class="summary-value">{{ todayNutrition.proteinTotal }}</span>
          <span class="summary-label">蛋白质(g)</span>
        </div>
        <div class="summary-item">
          <span class="summary-value">{{ todayNutrition.fatTotal }}</span>
          <span class="summary-label">脂肪(g)</span>
        </div>
        <div class="summary-item">
          <span class="summary-value">{{ todayNutrition.carbohydrateTotal }}</span>
          <span class="summary-label">碳水(g)</span>
        </div>
        <div class="summary-item">
          <span class="summary-value">{{ todayNutrition.sugarTotal }}</span>
          <span class="summary-label">糖(g)</span>
        </div>
        <div class="summary-item">
          <span class="summary-value">{{ todayNutrition.sodiumTotal }}</span>
          <span class="summary-label">钠(mg)</span>
        </div>
      </div>
    </div>

    <!-- Meal accordion list -->
    <template v-for="meal in mealTypes" :key="meal.key">
      <div class="meal-card card" v-if="groupedRecords[meal.key]?.length" :class="{ expanded: expandedMeals[meal.key] }">
        <!-- Collapsed header: click to toggle -->
        <div class="meal-header" @click="toggleMeal(meal.key)">
          <div class="meal-header-left">
            <span class="meal-icon">{{ meal.icon }}</span>
            <span class="meal-name">{{ meal.label }}</span>
            <span class="meal-count">{{ groupedRecords[meal.key].length }}种食物</span>
          </div>
          <div class="meal-header-right">
            <span class="meal-summary">🔥{{ mealTotals[meal.key]?.calorie || 0 }}</span>
            <span class="meal-summary-sub">🥩{{ mealTotals[meal.key]?.protein || 0 }}g</span>
            <span class="meal-arrow">{{ expandedMeals[meal.key] ? '▴' : '▾' }}</span>
          </div>
        </div>

        <!-- Expanded body -->
        <div class="meal-body" v-show="expandedMeals[meal.key]">
          <!-- Photo carousel -->
          <div class="photo-carousel" v-if="mealPhotos[meal.key]?.length">
            <div class="photo-track" ref="photoTrack">
              <div class="photo-thumb" v-for="(photo, pi) in mealPhotos[meal.key]" :key="pi"
                   @click="openPhotoPreview(photo.imageUrl || photo)">
                <img :src="photoFullUrl(photo.imageUrl || photo)" alt="食物照片" loading="lazy">
              </div>
            </div>
          </div>

          <div class="meal-divider"></div>

          <!-- Voice records for this meal -->
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
                <span class="food-amount">{{ rec.amount }}{{ rec.unit || '' }}</span>
                <span class="food-source">{{ sourceLabels[rec.source] || rec.source }}</span>
              </div>
              <div class="food-nutrition">
                <span>🔥{{ rec.calorie || 0 }}kcal</span>
                <span>🥩{{ rec.protein || 0 }}g</span>
                <span>🧈{{ rec.fat || 0 }}g</span>
                <span>🌾{{ rec.carbohydrate || 0 }}g</span>
                <span>🍬{{ rec.sugar || 0 }}g</span>
                <span>🧂{{ rec.sodium || 0 }}mg</span>
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

    <!-- Hidden audio player for voice playback -->
    <audio ref="voiceAudioRef" style="display:none" @ended="onVoiceEnded" preload="auto"></audio>

    <div v-if="!Object.values(groupedRecords).some(g => g.length)" class="empty-state">
      <div class="empty-icon">🍽️</div>
      <p>今天还没有饮食记录</p>
      <p style="font-size:12px;color:#bbb;margin-top:4px">点击上方按钮开始记录</p>
    </div>

    <!-- ==================== Photo Preview Lightbox ==================== -->
    <div class="lightbox-overlay" v-if="previewPhoto" @click="closePhotoPreview">
      <img :src="previewPhoto" alt="食物照片大图" class="lightbox-img" @click.stop>
    </div>

    <!-- ==================== Photo Recognition Modal ==================== -->
    <div class="modal-overlay" v-if="showPhotoModal" @click.self="!photoSaving && closePhotoModal()">
      <div class="modal-content">
        <h3>📷 拍照识别食物</h3>

        <input type="file" ref="photoInputRef" accept="image/*" capture="environment"
               style="display:none" @change="onPhotoFileSelected">

        <div class="mock-camera" @click="triggerPhotoFileInput" v-if="!photoFile && !photoAnalyzed">
          <span>📸 点击拍照或选择图片</span>
        </div>
        <div class="file-selected" v-if="photoFile && !photoAnalyzed && !photoLoading">
          <span class="file-icon">📸</span>
          <span class="file-name">{{ photoFile.name }}</span>
        </div>

        <div v-if="photoLoading" class="loading" style="padding:20px">🤖 AI 智能分析中，请稍候...</div>

        <div v-if="photoAnalyzed && photoCandidates?.length">
          <p style="font-size:13px;color:#666;margin-bottom:8px">
            识别到 {{ photoCandidates.length }} 种食物，请勾选要保存的食物：
          </p>

          <div class="candidate-check-item" v-for="(c, i) in photoCandidates" :key="i"
               :class="{ checked: c._checked }">
            <label class="check-row" @click.stop>
              <input type="checkbox" v-model="c._checked" class="check-box">
              <span class="candidate-name">{{ c.foodName }}</span>
              <span class="candidate-confidence"
                    :style="{ color: c.confidence >= 0.8 ? '#4CAF50' : c.confidence >= 0.5 ? '#FF9800' : '#f44336' }">
                {{ (c.confidence*100).toFixed(0) }}%
              </span>
            </label>
            <div class="candidate-nutrition" v-if="c.nutritionPreview">
              <span>🔥{{ c.nutritionPreview.calorie }}kcal</span>
              <span>🥩{{ c.nutritionPreview.protein }}g</span>
              <span>🧈{{ c.nutritionPreview.fat }}g</span>
              <span>🌾{{ c.nutritionPreview.carbohydrate }}g</span>
              <span v-if="c.nutritionPreview.sugar != null">🍬{{ c.nutritionPreview.sugar }}g</span>
              <span v-if="c.nutritionPreview.sodium != null">🧂{{ c.nutritionPreview.sodium }}mg</span>
            </div>
            <div class="amount-row" v-if="c._checked">
              <label>份量：</label>
              <input type="number" v-model.number="c._amount" min="0.1" step="0.5" class="amount-input">
              <span class="amount-unit">{{ c.unit || '份' }}</span>
            </div>
          </div>

          <div class="modal-actions" style="margin-top:12px">
            <label style="font-size:13px;color:#666">统一餐次：</label>
            <select v-model="selectedMeal">
              <option value="早餐">早餐</option>
              <option value="午餐">午餐</option>
              <option value="晚餐">晚餐</option>
              <option value="夜宵">夜宵</option>
              <option value="其他">其他</option>
            </select>
            <button class="btn btn-primary save-all-btn"
                    :disabled="photoSaving || checkedCount === 0"
                    @click="saveFromPhoto">
              {{ photoSaving ? '⏳ 保存中...' : `✅ 确认保存 (${checkedCount}种)` }}
            </button>
          </div>
        </div>

        <div v-if="photoError" class="error-msg">
          <p>❌ {{ photoError }}</p>
          <button class="btn btn-sm btn-outline" @click="retryPhoto" style="margin-top:8px">重新拍照</button>
        </div>

        <div class="modal-bottom-bar" v-if="!photoAnalyzed">
          <button class="btn btn-sm analyze-btn"
                  :class="{ ready: photoFile && !photoLoading }"
                  :disabled="!photoFile || photoLoading"
                  @click="startPhotoAnalyze">
            🤖 智能分析
          </button>
          <button class="btn btn-sm btn-outline cancel-btn" @click="closePhotoModal">取消</button>
        </div>
        <div class="modal-bottom-bar" v-if="photoAnalyzed || photoError">
          <button class="btn btn-sm btn-outline cancel-btn" @click="closePhotoModal"
                  :disabled="photoSaving">取消</button>
        </div>
      </div>
    </div>

    <!-- ==================== Voice Input Modal ==================== -->
    <div class="modal-overlay" v-if="showVoiceModal" @click.self="!voiceSaving && closeVoiceModal()">
      <div class="modal-content">
        <h3>🎤 语音输入</h3>

        <!-- Idle: ready to record -->
        <div class="voice-record-area" v-if="!voiceRecording && !voiceLoading && !voiceAnalyzed && !voiceError">
          <div class="voice-record-btn" @click="startVoiceRecord">
            <span class="voice-mic-icon">🎙️</span>
            <span>点击开始录音</span>
          </div>
        </div>

        <!-- Recording -->
        <div class="voice-record-area recording" v-if="voiceRecording">
          <div class="voice-recording-indicator">
            <span class="recording-dot"></span>
            <span>录音中...</span>
          </div>
          <div class="voice-timer">{{ formatVoiceTime(voiceElapsed) }}</div>
          <button class="btn btn-stop" @click="stopVoiceRecord">⏹ 停止录音</button>
        </div>

        <!-- Loading -->
        <div v-if="voiceLoading" class="loading" style="padding:20px">🤖 AI 智能分析中，请稍候...</div>

        <!-- Results (photo-modal style) -->
        <div v-if="voiceAnalyzed && voiceResult && !voiceError">
          <p style="font-size:13px;color:#666;margin-bottom:6px">
            <strong>识别文本：</strong>{{ voiceResult.transcribedText }}
          </p>
          <p style="font-size:13px;color:#666;margin-bottom:8px">
            解析到 {{ voiceResult.foodEntities.length }} 种食物，请勾选要保存的食物：
          </p>

          <div class="candidate-check-item" v-for="(e, i) in voiceResult.foodEntities" :key="i"
               :class="{ checked: e._checked }">
            <label class="check-row" @click.stop>
              <input type="checkbox" v-model="e._checked" class="check-box">
              <span class="candidate-name">{{ e.foodName }}</span>
              <span class="candidate-confidence" style="color:#2196F3">语音</span>
            </label>
            <div class="candidate-nutrition" v-if="e.calorie != null">
              <span>🔥{{ e.calorie }}kcal</span>
              <span>🥩{{ e.protein }}g</span>
              <span>🧈{{ e.fat }}g</span>
              <span>🌾{{ e.carbohydrate }}g</span>
              <span v-if="e.sugar != null">🍬{{ e.sugar }}g</span>
              <span v-if="e.sodium != null">🧂{{ e.sodium }}mg</span>
            </div>
            <div class="amount-row" v-if="e._checked">
              <label>份量：</label>
              <input type="number" v-model.number="e.amount" min="0.1" step="0.5" class="amount-input">
              <span class="amount-unit">{{ e.unit || '份' }}</span>
            </div>
          </div>

          <!-- Unified meal type selector -->
          <div class="modal-actions" style="margin-top:12px">
            <label style="font-size:13px;color:#666">统一餐次：</label>
            <select v-model="voiceMealType">
              <option value="早餐">早餐</option>
              <option value="午餐">午餐</option>
              <option value="晚餐">晚餐</option>
              <option value="夜宵">夜宵</option>
              <option value="其他">其他</option>
            </select>
            <button class="btn btn-primary save-all-btn"
                    :disabled="voiceSaving || voiceCheckedCount === 0"
                    @click="saveFromVoice">
              {{ voiceSaving ? '⏳ 保存中...' : `✅ 确认保存 (${voiceCheckedCount}种)` }}
            </button>
          </div>
        </div>

        <!-- Error -->
        <div v-if="voiceError" class="error-msg">
          <p>❌ {{ voiceError }}</p>
          <button class="btn btn-sm btn-outline" @click="retryVoice" style="margin-top:8px">重新录音</button>
        </div>

        <!-- Bottom bar -->
        <div class="modal-bottom-bar" v-if="!voiceRecording && !voiceLoading">
          <button class="btn btn-sm btn-outline cancel-btn" @click="closeVoiceModal"
                  :disabled="voiceSaving">取消</button>
        </div>
      </div>
    </div>

    <!-- ==================== Manual Add Modal ==================== -->
    <div class="modal-overlay" v-if="showManualModal" @click.self="!manualSaving && closeManualModal()">
      <div class="modal-content">
        <h3>✏️ 手动添加</h3>

        <div class="form-group">
          <label>食物名称</label>
          <input v-model="manualForm.foodName" placeholder="如：米饭">
        </div>
        <div class="form-group">
          <label>餐次</label>
          <select v-model="manualForm.mealType">
            <option value="早餐">早餐</option>
            <option value="午餐">午餐</option>
            <option value="晚餐">晚餐</option>
            <option value="夜宵">夜宵</option>
            <option value="其他">其他</option>
          </select>
        </div>
        <div class="form-group">
          <label>份量</label>
          <input type="number" v-model.number="manualForm.amount" placeholder="1" min="0.1" step="0.5">
        </div>

        <div v-if="manualAnalyzing" class="loading" style="padding:12px">🤖 AI 智能分析中，请稍候...</div>

        <div v-if="manualAnalysisResult" class="analysis-preview">
          <p style="font-size:13px;color:#666;margin-bottom:4px">📊 智能分析结果：</p>
          <div class="preview-nutrition">
            <span>🔥 {{ manualAnalysisResult.calorie }} kcal</span>
            <span>🥩 {{ manualAnalysisResult.protein }}g</span>
            <span>🧈 {{ manualAnalysisResult.fat }}g</span>
            <span>🌾 {{ manualAnalysisResult.carbohydrate }}g</span>
          </div>
          <div class="preview-meta" v-if="hasSugarOrSodium(manualAnalysisResult)">
            <span v-if="manualAnalysisResult.sugar != null">🍬 糖 {{ manualAnalysisResult.sugar }}g</span>
            <span v-if="manualAnalysisResult.sodium != null">🧂 钠 {{ manualAnalysisResult.sodium }}mg</span>
          </div>
        </div>

        <div v-if="manualError" class="error-msg">
          <p>❌ {{ manualError }}</p>
        </div>

        <button class="btn" @click="saveManual"
                v-if="manualForm.foodName"
                :disabled="!manualAnalysisResult || manualSaving"
                :class="manualAnalysisResult && !manualSaving ? 'btn-primary' : 'btn-disabled'"
                :title="!manualAnalysisResult ? '请先点击「智能分析」获取营养数据' : ''"
                style="width:100%;margin-top:8px">
          {{ manualSaving ? '⏳ 保存中...' : '保存' }}
        </button>

        <div class="modal-bottom-bar" v-if="!manualAnalyzing">
          <button class="btn btn-sm analyze-btn"
                  :class="{ ready: manualForm.foodName.trim() }"
                  :disabled="!manualForm.foodName.trim()"
                  @click="startManualAnalyze">
            🤖 智能分析
          </button>
          <button class="btn btn-sm btn-outline cancel-btn" @click="closeManualModal"
                  :disabled="manualSaving">取消</button>
        </div>
      </div>
    </div>

    <!-- ==================== Edit Record Modal ==================== -->
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import api from '../api/index.js'
import auth from '../auth.js'
import toast from '../toast.js'

function showLoginToast() {
  toast.show('请先在"我的"页面登录')
}

const today = new Date().toISOString().split('T')[0]
const records = ref([])
const todayNutrition = ref(null)
const alerts = ref([])

// Accordion state: which meals are expanded
const expandedMeals = reactive({
  '早餐': true,
  '午餐': true,
  '晚餐': false,
  '夜宵': false,
  '其他': false,
})

function toggleMeal(key) {
  expandedMeals[key] = !expandedMeals[key]
}

// Photo preview lightbox
const previewPhoto = ref(null)

function photoFullUrl(relativePath) {
  if (!relativePath) return ''
  return '/api/uploads' + relativePath
}

function openPhotoPreview(url) { previewPhoto.value = photoFullUrl(url) }
function closePhotoPreview() { previewPhoto.value = null }

const mealTypes = [
  { key: '早餐', label: '早餐', icon: '🍳' },
  { key: '午餐', label: '午餐', icon: '🍚' },
  { key: '晚餐', label: '晚餐', icon: '🍜' },
  { key: '夜宵', label: '夜宵', icon: '🌙' },
  { key: '其他', label: '其他', icon: '🍽️' },
]

const sourceLabels = { photo: '拍照', voice: '语音', manual: '手动' }

const groupedRecords = computed(() => {
  const groups = {}
  mealTypes.forEach(m => { groups[m.key] = [] })
  records.value.forEach(r => {
    if (groups[r.mealType] !== undefined) groups[r.mealType].push(r)
    else if (groups['其他']) groups['其他'].push(r)
  })
  return groups
})

// Meal-level nutrition totals
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

// Meal photos loaded from backend API
const mealPhotos = ref({})
// Voice records loaded from backend API
const voiceRecords = ref([])

// Group voice records by meal type
const voiceRecordsByMeal = computed(() => {
  const groups = {}
  voiceRecords.value.forEach(v => {
    if (!groups[v.mealType]) groups[v.mealType] = []
    groups[v.mealType].push(v)
  })
  return groups
})

// ==================== Photo Modal State ====================
const showPhotoModal = ref(false)
const photoInputRef = ref(null)
const photoFile = ref(null)
const photoLoading = ref(false)
const photoAnalyzed = ref(false)
const photoCandidates = ref([])
const photoError = ref(null)
const photoSaving = ref(false)
const selectedMeal = ref('午餐')
const photoAnalyzedResultImageUrl = ref(null)   // 识别返回的 imageUrl

// ==================== Voice Modal State ====================
const showVoiceModal = ref(false)
const voiceRecording = ref(false)
const voiceElapsed = ref(0)
const voiceLoading = ref(false)
const voiceAnalyzed = ref(false)
const voiceResult = ref(null)
const voiceError = ref(null)
const voiceSaving = ref(false)
const voiceMealType = ref('午餐')   // 统一餐次选择
const voiceCheckedCount = computed(() =>
  voiceResult.value?.foodEntities?.filter(e => e._checked).length || 0
)

let voiceMediaRecorder = null
let voiceStream = null
let voiceChunks = []
let voiceTimer = null
let voiceStartTime = null
let voiceCancelOnStop = false

// ==================== Manual Modal State ====================
const showManualModal = ref(false)
const manualForm = ref({ foodName: '', mealType: '午餐', amount: 1 })
const manualAnalyzing = ref(false)
const manualAnalysisResult = ref(null)
const manualError = ref(null)
const manualSaving = ref(false)

// ==================== Edit Modal State ====================
const showEditModal = ref(false)
const editForm = ref({ id: null, foodName: '', mealType: '午餐', amount: 1 })

const checkedCount = computed(() =>
  photoCandidates.value.filter(c => c._checked).length
)

// ==================== Data Fetching ====================
async function fetchData() {
  try {
    const [recRes, nutRes, photoRes, voiceRes] = await Promise.all([
      api.getDietRecords(today),
      api.getNutrition(today),
      api.getMealPhotos(today),
      api.getVoiceRecords(today),
    ])
    records.value = (recRes.data?.data) || []
    todayNutrition.value = (nutRes.data?.data) || null

    // Group photos by meal type
    const photos = {}
    const photoList = (photoRes.data?.data) || []
    photoList.forEach(p => {
      if (!photos[p.mealType]) photos[p.mealType] = []
      photos[p.mealType].push(p)
    })
    mealPhotos.value = photos

    // Store voice records for display
    voiceRecords.value = (voiceRes.data?.data) || []

    // Auto-expand meals that have records
    mealTypes.forEach(m => {
      if (groupedRecords.value[m.key]?.length) {
        expandedMeals[m.key] = true
      }
    })
  } catch (e) {
    console.error('获取数据失败', e)
  }
}

async function checkWarnings() {
  try {
    const res = await api.checkAlerts(today)
    const alertData = res.data?.data
    if (alertData?.hasAlert) {
      alerts.value = alertData.alerts || []
    } else {
      alerts.value = []
    }
  } catch (e) {
    console.error('检查预警失败', e)
  }
}

// ==================== Photo Modal ====================
function openPhotoModal() {
  if (!auth.state.isLoggedIn) {
    showLoginToast()
    return
  }
  showPhotoModal.value = true
  photoFile.value = null
  photoAnalyzed.value = false
  photoCandidates.value = []
  photoError.value = null
  photoLoading.value = false
  photoSaving.value = false
  photoAnalyzedResultImageUrl.value = null
}

function closePhotoModal() {
  if (photoSaving.value) return
  showPhotoModal.value = false
  photoFile.value = null
  photoAnalyzed.value = false
  photoCandidates.value = []
  photoError.value = null
  photoLoading.value = false
  photoSaving.value = false
  photoAnalyzedResultImageUrl.value = null
}

function triggerPhotoFileInput() { photoInputRef.value?.click() }

function onPhotoFileSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return
  photoFile.value = file
  photoAnalyzed.value = false
  photoCandidates.value = []
  photoError.value = null
}

async function startPhotoAnalyze() {
  if (!photoFile.value) return
  photoLoading.value = true
  photoError.value = null
  photoCandidates.value = []
  try {
    const formData = new FormData()
    formData.append('image', photoFile.value)
    const res = await api.recognizeFood(formData)
    const data = res.data?.data
    if (data?.candidates?.length) {
      photoCandidates.value = data.candidates.map(c => ({
        ...c,
        _checked: true,
        _amount: c.defaultAmount || 1,
      }))
      // 保存后端返回的照片路径
      photoAnalyzedResultImageUrl.value = data.imageUrl || null
      photoAnalyzed.value = true
    } else {
      photoError.value = '未能识别到食物，请重试或改用手动添加'
    }
  } catch (e) {
    console.error('图片识别失败', e)
    photoError.value = '识别服务暂时不可用，请重试或改用手动添加'
  } finally {
    photoLoading.value = false
  }
}

function retryPhoto() {
  photoError.value = null
  photoAnalyzed.value = false
  photoCandidates.value = []
  photoFile.value = null
  if (photoInputRef.value) photoInputRef.value.value = ''
}

async function saveFromPhoto() {
  const checked = photoCandidates.value.filter(c => c._checked)
  if (!checked.length) return

  photoSaving.value = true
  let saved = 0, failed = 0

  for (const c of checked) {
    try {
      await api.createDietRecord({
        foodName: c.foodName,
        mealType: selectedMeal.value,
        amount: c._amount || 1,
        source: 'photo',
        // 传入 AI 返回的营养数据
        calorie: c.nutritionPreview?.calorie,
        protein: c.nutritionPreview?.protein,
        fat: c.nutritionPreview?.fat,
        carbohydrate: c.nutritionPreview?.carbohydrate,
        sugar: c.nutritionPreview?.sugar,
        sodium: c.nutritionPreview?.sodium,
      })
      saved++
    } catch (e) {
      failed++
      console.error(`保存 ${c.foodName} 失败`, e)
    }
  }

  // ② 保存照片记录到 meal_photo 表
  const imageUrl = photoCandidates.value[0]?.imageUrl || photoAnalyzedResultImageUrl.value
  if (imageUrl) {
    try {
      await api.saveMealPhoto({
        recordDate: today,
        mealType: selectedMeal.value,
        imageUrl: imageUrl,
      })
      console.log('照片记录已保存:', imageUrl)
    } catch (e) {
      console.error('保存照片记录失败', e)
    }
  }

  photoSaving.value = false
  if (failed > 0) toast.show(`保存完成：${saved} 种成功，${failed} 种失败`)
  closePhotoModal()
  await fetchData()
  await checkWarnings()
}

// ==================== Voice Modal ====================
function formatVoiceTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0')
}

function openVoiceModal() {
  if (!auth.state.isLoggedIn) {
    showLoginToast()
    return
  }
  showVoiceModal.value = true
  voiceRecording.value = false
  voiceAnalyzed.value = false
  voiceResult.value = null
  voiceError.value = null
  voiceLoading.value = false
  voiceSaving.value = false
  voiceElapsed.value = 0
}

function closeVoiceModal() {
  if (voiceSaving.value) return
  cleanupVoiceRecorder()
  showVoiceModal.value = false
}

async function startVoiceRecord() {
  try {
    cleanupVoiceRecorder()
    voiceCancelOnStop = false
    voiceStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    voiceMediaRecorder = new MediaRecorder(voiceStream, { mimeType: 'audio/webm' })
    voiceChunks = []

    voiceMediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) voiceChunks.push(e.data)
    }

    voiceMediaRecorder.onstop = () => {
      stopVoiceStream()
      if (voiceCancelOnStop) {
        voiceCancelOnStop = false
        voiceChunks = []
        return
      }
      // Auto trigger analysis after recording stops
      voiceAnalyzeBlob()
    }

    voiceMediaRecorder.start()
    voiceRecording.value = true
    voiceStartTime = Date.now()
    voiceTimer = setInterval(() => {
      voiceElapsed.value = Math.floor((Date.now() - voiceStartTime) / 1000)
    }, 200)
  } catch (e) {
    console.error('麦克风访问失败', e)
    voiceError.value = '无法访问麦克风，请检查浏览器权限设置'
  }
}

function stopVoiceRecord() {
  if (voiceMediaRecorder && voiceMediaRecorder.state === 'recording') {
    voiceMediaRecorder.stop()
    voiceRecording.value = false
    if (voiceTimer) { clearInterval(voiceTimer); voiceTimer = null }
  }
}

async function voiceAnalyzeBlob() {
  if (!voiceChunks.length) return
  voiceLoading.value = true
  voiceError.value = null
  voiceResult.value = null

  try {
    const blob = new Blob(voiceChunks, { type: 'audio/webm' })
    const formData = new FormData()
    formData.append('audio', blob, 'recording.webm')
    const duration = voiceElapsed.value
    const res = await api.parseVoice(formData, duration)
    const data = res.data?.data
    if (data?.foodEntities?.length) {
      // Add checkbox state to each entity (match photo modal pattern)
      data.foodEntities.forEach(e => {
        e._checked = true
        e._amount = e.amount || 1
      })
      voiceResult.value = data
      voiceAnalyzed.value = true
    } else {
      voiceError.value = '未能从语音中识别到食物信息，请重录或改用手动输入'
    }
  } catch (e) {
    console.error('语音识别失败', e)
    voiceError.value = '语音识别服务暂时不可用，请重试或改用手动输入'
  } finally {
    voiceLoading.value = false
  }
}

function retryVoice() {
  voiceError.value = null
  voiceAnalyzed.value = false
  voiceResult.value = null
  voiceRecording.value = false
  voiceElapsed.value = 0
  voiceChunks = []
}

// ==================== Voice Playback ====================
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

function onVoiceEnded() {
  playingVoiceId.value = null
}

async function deleteVoiceRecord(id) {
  if (!confirm('确定删除这条语音记录？')) return
  try {
    await api.deleteVoiceRecord(id)
    await fetchData()
  } catch (e) {
    console.error('删除语音记录失败', e)
    toast.show('删除失败，请稍后重试')
  }
}

function cleanupVoiceRecorder() {
  if (voiceTimer) { clearInterval(voiceTimer); voiceTimer = null }
  if (voiceMediaRecorder && voiceMediaRecorder.state === 'recording') {
    voiceCancelOnStop = true
    voiceMediaRecorder.stop()
  } else {
    stopVoiceStream()
  }
  voiceMediaRecorder = null
  voiceChunks = []
}

function stopVoiceStream() {
  if (voiceStream) {
    voiceStream.getTracks().forEach(t => t.stop())
    voiceStream = null
  }
}

async function saveFromVoice() {
  const checked = voiceResult.value.foodEntities.filter(e => e._checked)
  if (!checked.length) return

  voiceSaving.value = true
  let saved = 0, failed = 0

  for (const entity of checked) {
    try {
      // 使用 AI 已经返回的营养数据，如果为空再兜底查库
      await api.createDietRecord({
        foodName: entity.foodName,
        mealType: voiceMealType.value,
        amount: entity._amount || entity.amount || 1,
        source: 'voice',
        calorie: entity.calorie,
        protein: entity.protein,
        fat: entity.fat,
        carbohydrate: entity.carbohydrate,
        sugar: entity.sugar,
        sodium: entity.sodium,
      })
      saved++
    } catch (e) {
      failed++
      console.error(`语音-保存 ${entity.foodName} 失败`, e)
    }
  }

  // ② 回填语音记录的餐次类型
  const voiceRecordId = voiceResult.value?.voiceRecordId
  if (voiceRecordId) {
    try {
      await api.updateVoiceRecordMealType(voiceRecordId, voiceMealType.value)
    } catch (e) {
      console.error('更新语音记录餐次失败', e)
    }
  }

  voiceSaving.value = false
  if (failed > 0) toast.show(`保存完成：${saved} 种成功，${failed} 种失败`)
  closeVoiceModal()
  await fetchData()
  await checkWarnings()
}

// ==================== Manual Modal ====================
function openManualModal() {
  if (!auth.state.isLoggedIn) {
    showLoginToast()
    return
  }
  showManualModal.value = true
  manualForm.value = { foodName: '', mealType: '午餐', amount: 1 }
  manualAnalysisResult.value = null
  manualError.value = null
  manualAnalyzing.value = false
  manualSaving.value = false
}

function closeManualModal() {
  if (manualSaving.value) return
  showManualModal.value = false
  manualForm.value = { foodName: '', mealType: '午餐', amount: 1 }
  manualAnalysisResult.value = null
  manualError.value = null
  manualAnalyzing.value = false
  manualSaving.value = false
}

async function startManualAnalyze() {
  const name = manualForm.value.foodName.trim()
  if (!name) return
  manualAnalyzing.value = true
  manualError.value = null
  manualAnalysisResult.value = null
  try {
    const res = await api.analyzeFoodText(name)
    const data = res.data?.data
    if (data) {
      manualAnalysisResult.value = data
    } else {
      manualError.value = '未找到该食物的营养信息，请检查食物名称'
    }
  } catch (e) {
    console.error('文字分析失败', e)
    manualError.value = '智能分析服务暂时不可用，请稍后重试'
  } finally {
    manualAnalyzing.value = false
  }
}

function hasSugarOrSodium(r) {
  return (r.sugar != null || r.sodium != null)
}

async function saveManual() {
  manualSaving.value = true
  try {
    const payload = {
      foodName: manualForm.value.foodName,
      mealType: manualForm.value.mealType,
      amount: manualForm.value.amount,
      source: 'manual',
    }
    // 传入 AI 分析出的营养数据
    if (manualAnalysisResult.value) {
      payload.calorie = manualAnalysisResult.value.calorie
      payload.protein = manualAnalysisResult.value.protein
      payload.fat = manualAnalysisResult.value.fat
      payload.carbohydrate = manualAnalysisResult.value.carbohydrate
      payload.sugar = manualAnalysisResult.value.sugar
      payload.sodium = manualAnalysisResult.value.sodium
    }
    await api.createDietRecord(payload)
    manualSaving.value = false
    closeManualModal()
    await fetchData()
    await checkWarnings()
  } catch (e) {
    console.error('手动保存失败', e)
    toast.show('保存失败：' + (e?.response?.data?.message || e.message || '未知错误'))
  } finally {
    manualSaving.value = false
  }
}

// ==================== Edit / Delete ====================
function editRecord(rec) {
  editForm.value = {
    id: rec.id,
    foodName: rec.foodName,
    mealType: rec.mealType,
    amount: rec.amount,
  }
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
    await fetchData()
    await checkWarnings()
  } catch (e) {
    console.error('编辑保存失败', e)
    toast.show('保存失败：' + (e?.response?.data?.message || e.message || '未知错误'))
  }
}

async function deleteRecord(id) {
  if (confirm('确定删除这条记录？')) {
    await api.deleteDietRecord(id)
    await fetchData()
    await checkWarnings()
  }
}

onMounted(async () => {
  await fetchData()
  await checkWarnings()
})
</script>

<style scoped>
.quick-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.action-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 8px;
  background: linear-gradient(135deg, #E8F5E9, #C8E6C9);
  border: none;
  border-radius: 12px;
  font-size: 13px;
  cursor: pointer;
  color: #2E7D32;
  transition: transform 0.1s;
}
.action-btn:active { transform: scale(0.97); }
.action-icon { font-size: 28px; }

.alert-card { margin-bottom: 12px; }
.card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}

/* ==================== Summary Card ==================== */
.summary-card { margin-bottom: 12px; }
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  text-align: center;
}
.summary-item { display: flex; flex-direction: column; padding: 4px 0; }
.summary-value { font-size: 18px; font-weight: 700; color: #4CAF50; }
.summary-label { font-size: 11px; color: #999; margin-top: 2px; }

/* ==================== Meal Accordion ==================== */
.meal-card {
  padding: 0 !important;
  overflow: hidden;
  margin-bottom: 10px;
  border-left: 3px solid #4CAF50;
  transition: all 0.2s;
}
.meal-card.expanded {
  border-left-color: #388E3C;
}

.meal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}
.meal-header:active { background: #F5F5F5; }

.meal-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.meal-icon { font-size: 20px; }
.meal-name { font-size: 15px; font-weight: 600; }
.meal-count {
  font-size: 12px;
  color: #999;
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 10px;
}

.meal-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.meal-summary { font-size: 16px; font-weight: 700; color: #E65100; }
.meal-summary-sub { font-size: 12px; color: #999; }
.meal-arrow { font-size: 14px; color: #bbb; }

.meal-body {
  padding: 0 16px 12px 16px;
}

/* ==================== Photo Carousel ==================== */
.photo-carousel {
  margin-bottom: 10px;
  overflow: hidden;
}
.photo-track {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
  padding: 4px 0;
}
.photo-track::-webkit-scrollbar { height: 4px; }
.photo-track::-webkit-scrollbar-thumb { background: #ccc; border-radius: 2px; }

.photo-thumb {
  flex-shrink: 0;
  width: 120px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
  scroll-snap-align: start;
  cursor: pointer;
  border: 2px solid #eee;
  transition: border-color 0.2s;
}
.photo-thumb:hover { border-color: #4CAF50; }
.photo-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.meal-divider {
  height: 1px;
  background: #f0f0f0;
  margin-bottom: 8px;
}

/* ==================== Food Item ==================== */
.food-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 10px 12px;
  background: #FAFAFA;
  border-radius: 10px;
  margin-bottom: 8px;
}
.food-main { flex: 1; min-width: 0; }
.food-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.food-name-row strong { font-size: 14px; }
.food-amount {
  font-size: 12px;
  color: #666;
  background: #E8F5E9;
  padding: 1px 6px;
  border-radius: 4px;
}
.food-source {
  font-size: 11px;
  color: #999;
  background: #f0f0f0;
  padding: 1px 6px;
  border-radius: 4px;
}
.food-nutrition {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 11px;
  color: #888;
}
.food-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  margin-left: 8px;
}

/* ==================== Lightbox ==================== */
.lightbox-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.85);
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.lightbox-img {
  max-width: 92%;
  max-height: 85vh;
  border-radius: 12px;
  object-fit: contain;
}

/* ==================== Voice Mini Cards in Meal ==================== */
.voice-records-mini {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 8px;
}
.voice-mini-card {
  background: #F3E5F5;
  border-radius: 10px;
  padding: 10px 12px;
  border-left: 3px solid #9C27B0;
}
.voice-mini-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.voice-mini-icon { font-size: 16px; }
.voice-mini-duration {
  font-size: 12px;
  color: #7B1FA2;
  font-weight: 500;
}
.voice-play-btn {
  padding: 4px 12px;
  border-radius: 6px;
  border: 1px solid #9C27B0;
  background: #fff;
  color: #9C27B0;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.voice-play-btn:hover { background: #F3E5F5; }
.voice-delete-btn {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #999;
  font-size: 11px;
  cursor: pointer;
}
.voice-mini-text {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ==================== Voice Recording Styles ==================== */
.voice-record-area {
  text-align: center;
  padding: 32px 16px;
}
.voice-record-btn {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 48px;
  background: #E8F5E9;
  border: 2px dashed #4CAF50;
  border-radius: 20px;
  cursor: pointer;
  transition: transform 0.15s, background 0.2s;
  color: #2E7D32;
}
.voice-record-btn:hover { background: #C8E6C9; }
.voice-record-btn:active { transform: scale(0.96); }
.voice-mic-icon { font-size: 48px; }

.voice-record-area.recording {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: #FFF3F0;
  border: 2px solid #f44336;
  border-radius: 20px;
}
.voice-recording-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #f44336;
}
.recording-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #f44336;
  animation: recording-blink 0.6s infinite alternate;
}
@keyframes recording-blink {
  from { opacity: 1; transform: scale(1); }
  to { opacity: 0.3; transform: scale(1.3); }
}
.voice-timer {
  font-size: 36px;
  font-weight: 700;
  color: #333;
  font-variant-numeric: tabular-nums;
}
.btn-stop {
  padding: 10px 28px;
  border-radius: 12px;
  border: none;
  background: #f44336;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-stop:active { opacity: 0.8; }

/* ==================== Modal Styles (preserved) ==================== */
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
  max-width: 420px;
  max-height: 82vh;
  overflow-y: auto;
}
.modal-content h3 { margin-bottom: 16px; }

.mock-camera {
  background: #f0f0f0;
  border: 2px dashed #ddd;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  color: #999;
  cursor: pointer;
  margin-bottom: 16px;
  transition: border-color 0.2s, background 0.2s;
}
.mock-camera:hover { border-color: #4CAF50; background: #F1F8E9; }

.file-selected {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #E8F5E9;
  border: 2px solid #4CAF50;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  color: #2E7D32;
}
.file-icon { font-size: 24px; }
.file-name { font-size: 13px; font-weight: 500; word-break: break-all; }

.candidate-check-item {
  padding: 10px 12px;
  border: 2px solid #eee;
  border-radius: 10px;
  margin-bottom: 8px;
  transition: border-color 0.2s;
}
.candidate-check-item.checked { border-color: #4CAF50; background: #F1F8E9; }
.check-row { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.check-box { width: 18px; height: 18px; accent-color: #4CAF50; cursor: pointer; flex-shrink: 0; }
.candidate-name { font-weight: 600; flex: 1; }
.candidate-confidence { font-size: 12px; }
.candidate-nutrition {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
  color: #999;
  margin-top: 6px;
  margin-left: 26px;
}
.amount-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  margin-left: 26px;
  font-size: 13px;
  color: #666;
}
.amount-input { width: 70px; padding: 4px 6px; border: 1px solid #ccc; border-radius: 6px; font-size: 13px; text-align: center; }
.amount-unit { font-size: 12px; color: #999; }
.save-all-btn { width: 100%; }

.analysis-preview { background: #F1F8E9; border-radius: 10px; padding: 12px; margin-bottom: 8px; }
.preview-nutrition { display: flex; flex-wrap: wrap; gap: 8px; font-size: 12px; color: #2E7D32; margin-top: 4px; }
.preview-meta { display: flex; gap: 10px; font-size: 11px; color: #666; margin-top: 4px; }

.error-msg { text-align: center; padding: 12px; color: #f44336; font-size: 14px; margin-bottom: 8px; }

.modal-bottom-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.analyze-btn {
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.25s;
  background: #e0e0e0;
  color: #999;
}
.analyze-btn:disabled { cursor: not-allowed; background: #e0e0e0; color: #999; }
.analyze-btn.ready {
  background: linear-gradient(135deg, #4CAF50, #2E7D32);
  color: #fff;
  cursor: pointer;
}
.analyze-btn.ready:hover { opacity: 0.9; transform: translateY(-1px); box-shadow: 0 2px 8px rgba(76,175,80,0.3); }

.cancel-btn { padding: 8px 20px; }
.btn-disabled {
  background: #e0e0e0;
  color: #999;
  cursor: not-allowed;
  border: none;
  padding: 10px 0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
}
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; color: #666; margin-bottom: 4px; }
.modal-actions { display: flex; flex-direction: column; gap: 8px; }
.modal-content select, .modal-content input { margin-bottom: 8px; }
.voice-result { margin-top: 12px; }
.entity-row { display: flex; gap: 8px; margin: 8px 0; }
</style>
