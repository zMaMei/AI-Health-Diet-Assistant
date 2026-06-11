<template>
  <div class="record-page">
    <!-- Quick add buttons -->
    <div class="quick-actions">
      <button class="action-btn" @click="showPhotoModal = true">
        <span class="action-icon">📷</span>
        <span>拍照识别</span>
      </button>
      <button class="action-btn" @click="showVoiceModal = true">
        <span class="action-icon">🎤</span>
        <span>语音输入</span>
      </button>
      <button class="action-btn" @click="showManualModal = true">
        <span class="action-icon">✏️</span>
        <span>手动添加</span>
      </button>
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

    <!-- Photo Recognition Modal -->
    <div class="modal-overlay" v-if="showPhotoModal" @click.self="showPhotoModal=false">
      <div class="modal-content">
        <h3>📷 拍照识别食物</h3>
        <div class="mock-camera" @click="simulatePhoto">
          <span v-if="!photoResult">点击模拟拍照</span>
          <div v-else class="candidate-list">
            <div class="candidate-item" v-for="(c, i) in photoResult" :key="i">
              <span>{{ c.foodName }} ({{ (c.confidence*100).toFixed(0) }}%)</span>
            </div>
          </div>
        </div>
        <div v-if="photoResult" class="modal-actions">
          <select v-model="selectedFood" class="food-select">
            <option v-for="(c, i) in photoResult" :key="i" :value="c.foodName">{{ c.foodName }}</option>
          </select>
          <select v-model="selectedMeal">
            <option value="早餐">早餐</option>
            <option value="午餐">午餐</option>
            <option value="晚餐">晚餐</option>
            <option value="加餐">加餐</option>
          </select>
          <input type="number" v-model.number="foodAmount" placeholder="份量" min="0.1" step="0.5">
          <button class="btn btn-primary" @click="saveFromPhoto">确认保存</button>
        </div>
        <button class="btn btn-outline" @click="showPhotoModal=false" style="margin-top:8px">取消</button>
      </div>
    </div>

    <!-- Voice Input Modal -->
    <div class="modal-overlay" v-if="showVoiceModal" @click.self="showVoiceModal=false">
      <div class="modal-content">
        <h3>🎤 语音输入</h3>
        <button class="btn btn-primary" @click="simulateVoice" :disabled="voiceLoading">
          {{ voiceLoading ? '识别中...' : '点击模拟语音输入' }}
        </button>
        <div v-if="voiceResult" class="voice-result">
          <p><strong>识别文本：</strong>{{ voiceResult.transcribedText }}</p>
          <div v-for="(entity, i) in voiceResult.foodEntities" :key="i" class="entity-row">
            <input v-model="entity.foodName" placeholder="食物名称">
            <input type="number" v-model.number="entity.amount" placeholder="份量" style="width:60px">
          </div>
          <button class="btn btn-primary" @click="saveFromVoice">确认保存</button>
        </div>
        <button class="btn btn-outline" @click="showVoiceModal=false;voiceResult=null" style="margin-top:8px">取消</button>
      </div>
    </div>

    <!-- Manual Add Modal -->
    <div class="modal-overlay" v-if="showManualModal" @click.self="showManualModal=false">
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
        <div class="form-group">
          <label>来源</label>
          <select v-model="manualForm.source">
            <option value="manual">手动</option>
            <option value="photo">拍照</option>
            <option value="voice">语音</option>
          </select>
        </div>
        <button class="btn btn-primary" @click="saveManual">保存</button>
        <button class="btn btn-outline" @click="showManualModal=false" style="margin-top:8px">取消</button>
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

const showPhotoModal = ref(false)
const showVoiceModal = ref(false)
const showManualModal = ref(false)

const photoResult = ref(null)
const selectedFood = ref('')
const selectedMeal = ref('午餐')
const foodAmount = ref(1)

const voiceLoading = ref(false)
const voiceResult = ref(null)

const manualForm = ref({ foodName: '', mealType: '午餐', amount: 1, source: 'manual' })

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

async function fetchData() {
  try {
    const [recRes, nutRes] = await Promise.all([
      api.getDietRecords(today),
      api.getNutrition(today)
    ])
    records.value = recRes.data.data || []
    todayNutrition.value = nutRes.data.data
  } catch (e) {
    console.error('Failed to fetch data', e)
  }
}

function simulatePhoto() {
  photoResult.value = [
    { foodName: '米饭', confidence: 0.95 },
    { foodName: '红烧肉', confidence: 0.82 },
    { foodName: '炒青菜', confidence: 0.78 },
  ]
  selectedFood.value = '米饭'
}

async function saveFromPhoto() {
  try {
    await api.createDietRecord({
      foodName: selectedFood.value,
      mealType: selectedMeal.value,
      amount: foodAmount.value,
      source: 'photo',
    })
    showPhotoModal.value = false
    photoResult.value = null
    fetchData()
  } catch (e) {
    alert('保存失败')
  }
}

async function simulateVoice() {
  voiceLoading.value = true
  try {
    // Simulate API call
    await new Promise(r => setTimeout(r, 1000))
    voiceResult.value = {
      transcribedText: '我中午吃了一碗米饭和一个鸡腿',
      foodEntities: [
        { foodName: '米饭', amount: 1, unit: '碗', mealType: '午餐' },
        { foodName: '鸡腿', amount: 1, unit: '个', mealType: '午餐' },
      ],
    }
  } finally {
    voiceLoading.value = false
  }
}

async function saveFromVoice() {
  try {
    for (const entity of voiceResult.value.foodEntities) {
      await api.createDietRecord({
        foodName: entity.foodName,
        mealType: entity.mealType,
        amount: entity.amount,
        source: 'voice',
      })
    }
    showVoiceModal.value = false
    voiceResult.value = null
    fetchData()
  } catch (e) {
    alert('保存失败')
  }
}

async function saveManual() {
  try {
    await api.createDietRecord(manualForm.value)
    showManualModal.value = false
    manualForm.value = { foodName: '', mealType: '午餐', amount: 1, source: 'manual' }
    fetchData()
  } catch (e) {
    alert('保存失败')
  }
}

function editRecord(rec) {
  const newName = prompt('食物名称：', rec.foodName)
  if (newName) {
    api.updateDietRecord(rec.id, { foodName: newName }).then(() => fetchData())
  }
}

async function deleteRecord(id) {
  if (confirm('确定删除这条记录？')) {
    await api.deleteDietRecord(id)
    fetchData()
  }
}

onMounted(fetchData)
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

/* Modal */
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

.mock-camera {
  background: #f0f0f0;
  border: 2px dashed #ddd;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
  color: #999;
  cursor: pointer;
  margin-bottom: 12px;
}
.candidate-list { text-align: left; }
.candidate-item { padding: 8px; border-bottom: 1px solid #eee; }

.food-select, .modal-content select, .modal-content input {
  margin-bottom: 8px;
}

.form-group {
  margin-bottom: 12px;
}
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

.voice-result {
  margin-top: 12px;
}
.entity-row {
  display: flex;
  gap: 8px;
  margin: 8px 0;
}
</style>
