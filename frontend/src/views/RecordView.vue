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

    <!-- Today's summary -->
    <div class="card" v-if="todayNutrition">
      <div class="summary-row">
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
      </div>
    </div>

    <!-- Today's records group by meal -->
    <template v-for="meal in mealTypes" :key="meal.key">
      <div v-if="groupedRecords[meal.key]?.length">
        <h3 class="section-title">{{ meal.label }}</h3>
        <div class="card record-card" v-for="rec in groupedRecords[meal.key]" :key="rec.id">
          <div class="record-header">
            <strong>{{ rec.foodName }}</strong>
            <span class="record-source">{{ sourceLabels[rec.source] || rec.source }}</span>
          </div>
          <div class="record-detail">
            <span>份量: {{ rec.amount }}{{ rec.unit || '' }}</span>
            <span v-if="rec.calorie">热量: {{ rec.calorie }} kcal</span>
          </div>
          <div class="record-actions">
            <button class="btn btn-sm btn-outline" @click="editRecord(rec)">编辑</button>
            <button class="btn btn-sm btn-danger" @click="deleteRecord(rec.id)">删除</button>
          </div>
        </div>
      </div>
    </template>

    <div v-if="!Object.values(groupedRecords).some(g => g.length)" class="empty-state">
      <div class="empty-icon">🍽️</div>
      <p>今天还没有饮食记录</p>
      <p style="font-size:12px;color:#bbb;margin-top:4px">点击上方按钮开始记录</p>
    </div>

    <!-- ==================== Photo Recognition Modal ==================== -->
    <div class="modal-overlay" v-if="showPhotoModal" @click.self="!photoSaving && closePhotoModal()">
      <div class="modal-content">
        <h3>📷 拍照识别食物</h3>

        <!-- Hidden file input -->
        <input type="file" ref="photoInputRef" accept="image/*" capture="environment"
               style="display:none" @change="onPhotoFileSelected">

        <!-- File selection area -->
        <div class="mock-camera" @click="triggerPhotoFileInput" v-if="!photoFile && !photoAnalyzed">
          <span>📸 点击拍照或选择图片</span>
        </div>
        <div class="file-selected" v-if="photoFile && !photoAnalyzed && !photoLoading">
          <span class="file-icon">📸</span>
          <span class="file-name">{{ photoFile.name }}</span>
        </div>

        <!-- Loading -->
        <div v-if="photoLoading" class="loading" style="padding:20px">🤖 AI 智能分析中，请稍候...</div>

        <!-- Analysis results — multi-select candidates with individual amounts -->
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
            </div>
            <div class="amount-row" v-if="c._checked">
              <label>份量：</label>
              <input type="number" v-model.number="c._amount" min="0.1" step="0.5" class="amount-input">
              <span class="amount-unit">{{ c.unit || '份' }}</span>
            </div>
          </div>

          <!-- Meal type + batch save -->
          <div class="modal-actions" style="margin-top:12px">
            <label style="font-size:13px;color:#666">统一餐次：</label>
            <select v-model="selectedMeal">
              <option value="早餐">早餐</option>
              <option value="午餐">午餐</option>
              <option value="晚餐">晚餐</option>
              <option value="加餐">加餐</option>
            </select>
            <button class="btn btn-primary save-all-btn"
                    :disabled="photoSaving || checkedCount === 0"
                    @click="saveFromPhoto">
              {{ photoSaving ? '⏳ 保存中...' : `✅ 确认保存 (${checkedCount}种)` }}
            </button>
          </div>
        </div>

        <!-- Error -->
        <div v-if="photoError" class="error-msg">
          <p>❌ {{ photoError }}</p>
          <button class="btn btn-sm btn-outline" @click="retryPhoto" style="margin-top:8px">重新拍照</button>
        </div>

        <!-- Bottom buttons: 智能分析 + 取消 -->
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

        <input type="file" ref="voiceInputRef" accept="audio/*" capture
               style="display:none" @change="onVoiceFileSelected">

        <div class="mock-camera" @click="triggerVoiceFileInput" v-if="!voiceFile && !voiceAnalyzed">
          <span>🎙️ 点击录音或选择音频文件</span>
        </div>
        <div class="file-selected" v-if="voiceFile && !voiceAnalyzed && !voiceLoading">
          <span class="file-icon">🎵</span>
          <span class="file-name">{{ voiceFile.name }}</span>
        </div>

        <div v-if="voiceLoading" class="loading" style="padding:20px">🤖 AI 智能分析中，请稍候...</div>

        <div v-if="voiceAnalyzed && voiceResult && !voiceError" class="voice-result">
          <p><strong>识别文本：</strong>{{ voiceResult.transcribedText }}</p>
          <div v-for="(entity, i) in voiceResult.foodEntities" :key="i" class="entity-row">
            <input v-model="entity.foodName" placeholder="食物名称">
            <input type="number" v-model.number="entity.amount" placeholder="份量" style="width:60px">
            <select v-model="entity.mealType" style="width:80px">
              <option value="早餐">早餐</option>
              <option value="午餐">午餐</option>
              <option value="晚餐">晚餐</option>
              <option value="加餐">加餐</option>
            </select>
          </div>
          <button class="btn btn-primary" @click="saveFromVoice" style="margin-top:12px"
                  :disabled="voiceSaving">
            {{ voiceSaving ? '⏳ 保存中...' : '确认保存' }}
          </button>
        </div>

        <div v-if="voiceError" class="error-msg">
          <p>❌ {{ voiceError }}</p>
          <button class="btn btn-sm btn-outline" @click="retryVoice" style="margin-top:8px">重新录音</button>
        </div>

        <div class="modal-bottom-bar" v-if="!voiceAnalyzed">
          <button class="btn btn-sm analyze-btn"
                  :class="{ ready: voiceFile && !voiceLoading }"
                  :disabled="!voiceFile || voiceLoading"
                  @click="startVoiceAnalyze">
            🤖 智能分析
          </button>
          <button class="btn btn-sm btn-outline cancel-btn" @click="closeVoiceModal">取消</button>
        </div>
        <div class="modal-bottom-bar" v-if="voiceAnalyzed || voiceError">
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
            <option value="加餐">加餐</option>
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

        <button class="btn btn-primary" @click="saveManual"
                v-if="manualAnalysisResult || manualForm.foodName"
                :disabled="manualSaving" style="width:100%;margin-top:8px">
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
            <option value="加餐">加餐</option>
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
import { ref, computed, onMounted } from 'vue'
import api from '../api/index.js'

const today = new Date().toISOString().split('T')[0]
const records = ref([])
const todayNutrition = ref(null)
const alerts = ref([])

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

// ==================== Voice Modal State ====================
const showVoiceModal = ref(false)
const voiceInputRef = ref(null)
const voiceFile = ref(null)
const voiceLoading = ref(false)
const voiceAnalyzed = ref(false)
const voiceResult = ref(null)
const voiceError = ref(null)
const voiceSaving = ref(false)

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

const mealTypes = [
  { key: '早餐', label: '🍳 早餐' },
  { key: '午餐', label: '🍚 午餐' },
  { key: '晚餐', label: '🍜 晚餐' },
  { key: '加餐', label: '🍪 加餐' },
]

const sourceLabels = { photo: '拍照', voice: '语音', manual: '手动' }

const groupedRecords = computed(() => {
  const groups = {}
  mealTypes.forEach(m => { groups[m.key] = [] })
  records.value.forEach(r => {
    if (groups[r.mealType]) groups[r.mealType].push(r)
  })
  return groups
})

const checkedCount = computed(() =>
  photoCandidates.value.filter(c => c._checked).length
)

// ==================== Data Fetching ====================
async function fetchData() {
  try {
    const [recRes, nutRes] = await Promise.all([
      api.getDietRecords(today),
      api.getNutrition(today)
    ])
    records.value = recRes.data.data || []
    todayNutrition.value = nutRes.data.data
  } catch (e) {
    console.error('获取数据失败', e)
  }
}

async function checkWarnings() {
  try {
    const res = await api.checkAlerts(today)
    const alertData = res.data.data
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
  showPhotoModal.value = true
  photoFile.value = null
  photoAnalyzed.value = false
  photoCandidates.value = []
  photoError.value = null
  photoLoading.value = false
  photoSaving.value = false
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
}

function triggerPhotoFileInput() {
  photoInputRef.value?.click()
}

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
    const data = res.data.data
    if (data?.candidates?.length) {
      // Convert to reactive candidates with _checked and _amount
      photoCandidates.value = data.candidates.map(c => ({
        ...c,
        _checked: true,       // default: all checked
        _amount: c.defaultAmount || 1,
      }))
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
  let saved = 0
  let failed = 0

  for (const c of checked) {
    try {
      await api.createDietRecord({
        foodName: c.foodName,
        mealType: selectedMeal.value,
        amount: c._amount || 1,
        source: 'photo',
      })
      saved++
    } catch (e) {
      failed++
      console.error(`保存 ${c.foodName} 失败`, e)
    }
  }

  photoSaving.value = false

  if (failed > 0) {
    alert(`保存完成：${saved} 种成功，${failed} 种失败`)
  }

  closePhotoModal()
  await fetchData()
  await checkWarnings()
}

// ==================== Voice Modal ====================
function openVoiceModal() {
  showVoiceModal.value = true
  voiceFile.value = null
  voiceAnalyzed.value = false
  voiceResult.value = null
  voiceError.value = null
  voiceLoading.value = false
  voiceSaving.value = false
}

function closeVoiceModal() {
  if (voiceSaving.value) return
  showVoiceModal.value = false
  voiceFile.value = null
  voiceAnalyzed.value = false
  voiceResult.value = null
  voiceError.value = null
  voiceLoading.value = false
  voiceSaving.value = false
}

function triggerVoiceFileInput() {
  voiceInputRef.value?.click()
}

function onVoiceFileSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return
  voiceFile.value = file
  voiceAnalyzed.value = false
  voiceResult.value = null
  voiceError.value = null
}

async function startVoiceAnalyze() {
  if (!voiceFile.value) return
  voiceLoading.value = true
  voiceError.value = null
  voiceResult.value = null
  try {
    const formData = new FormData()
    formData.append('audio', voiceFile.value)
    const res = await api.parseVoice(formData)
    const data = res.data.data
    if (data?.foodEntities?.length) {
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
  voiceFile.value = null
  if (voiceInputRef.value) voiceInputRef.value.value = ''
}

async function saveFromVoice() {
  voiceSaving.value = true
  let saved = 0, failed = 0;

  for (const entity of voiceResult.value.foodEntities) {
    try {
      await api.createDietRecord({
        foodName: entity.foodName,
        mealType: entity.mealType || '午餐',
        amount: entity.amount,
        source: 'voice',
      })
      saved++
    } catch (e) {
      failed++
      console.error(`语音-保存 ${entity.foodName} 失败`, e)
    }
  }

  voiceSaving.value = false
  if (failed > 0) alert(`保存完成：${saved} 种成功，${failed} 种失败`)

  closeVoiceModal()
  await fetchData()
  await checkWarnings()
}

// ==================== Manual Modal ====================
function openManualModal() {
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
    const data = res.data.data
    if (data) {
      manualAnalysisResult.value = data
    } else {
      manualError.value = '未找到该食物的营养信息，请检查食物名称'
    }
  } catch (e) {
    console.error('文字分析失败', e)
    manualError.value = '智能分析服务暂时不可用，可直接保存'
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
    await api.createDietRecord({
      foodName: manualForm.value.foodName,
      mealType: manualForm.value.mealType,
      amount: manualForm.value.amount,
      source: 'manual',
    })
    closeManualModal()
    await fetchData()
    await checkWarnings()
  } catch (e) {
    console.error('手动保存失败', e)
    alert('保存失败：' + (e?.response?.data?.message || e.message || '未知错误'))
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
    alert('保存失败：' + (e?.response?.data?.message || e.message || '未知错误'))
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

.summary-row {
  display: flex;
  justify-content: space-around;
  text-align: center;
}
.summary-item { display: flex; flex-direction: column; }
.summary-value { font-size: 20px; font-weight: 700; color: #4CAF50; }
.summary-label { font-size: 11px; color: #999; margin-top: 2px; }

.record-card {
  border-left: 3px solid #4CAF50;
}
.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.record-source {
  font-size: 11px;
  color: #999;
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
}
.record-detail {
  font-size: 13px;
  color: #666;
  display: flex;
  gap: 12px;
}
.record-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  justify-content: flex-end;
}

/* ==================== Modal ==================== */
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
.file-name {
  font-size: 13px;
  font-weight: 500;
  word-break: break-all;
}

/* Candidate check items */
.candidate-check-item {
  padding: 10px 12px;
  border: 2px solid #eee;
  border-radius: 10px;
  margin-bottom: 8px;
  transition: border-color 0.2s;
}
.candidate-check-item.checked {
  border-color: #4CAF50;
  background: #F1F8E9;
}
.check-row {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.check-box {
  width: 18px;
  height: 18px;
  accent-color: #4CAF50;
  cursor: pointer;
  flex-shrink: 0;
}
.candidate-name { font-weight: 600; flex: 1; }
.candidate-confidence { font-size: 12px; }
.candidate-nutrition {
  display: flex;
  gap: 10px;
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
.amount-input {
  width: 70px;
  padding: 4px 6px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 13px;
  text-align: center;
}
.amount-unit { font-size: 12px; color: #999; }
.save-all-btn { width: 100%; }

.analysis-preview {
  background: #F1F8E9;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 8px;
}
.preview-nutrition {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #2E7D32;
  margin-top: 4px;
}
.preview-meta {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: #666;
  margin-top: 4px;
}

.error-msg {
  text-align: center;
  padding: 12px;
  color: #f44336;
  font-size: 14px;
  margin-bottom: 8px;
}

/* ==================== Bottom bar ==================== */
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
.analyze-btn:disabled {
  cursor: not-allowed;
  background: #e0e0e0;
  color: #999;
}
.analyze-btn.ready {
  background: linear-gradient(135deg, #4CAF50, #2E7D32);
  color: #fff;
  cursor: pointer;
}
.analyze-btn.ready:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(76,175,80,0.3);
}

.cancel-btn { padding: 8px 20px; }

.form-group { margin-bottom: 12px; }
.form-group label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.modal-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.modal-content select, .modal-content input {
  margin-bottom: 8px;
}

.voice-result { margin-top: 12px; }
.entity-row {
  display: flex;
  gap: 8px;
  margin: 8px 0;
}
</style>
