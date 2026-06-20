<template>
  <div class="profile-page">
    <div v-if="loading" class="loading">加载中...</div>

    <template v-else-if="profile || !auth.state.isLoggedIn">
      <!-- User info -->
      <div v-if="!auth.state.isLoggedIn" class="card user-card" @click="showAuthModal = true">
        <div class="user-avatar">👤</div>
        <div class="user-info">
          <h3>点击登录/注册</h3>
          <p>登录后可同步个人数据</p>
        </div>
      </div>

      <!-- 已登录：显示用户信息 -->
      <div v-else class="card user-card">
        <div class="avatar-wrapper" @click="openCropModal">
          <img v-if="auth.state.avatarUrl" :src="auth.state.avatarUrl" class="user-avatar-img" />
          <div v-else class="user-avatar">👤</div>
          <div class="avatar-camera">📷</div>
        </div>

        <div class="user-info">
          <div class="nickname-row">
            <input v-if="editingNickname" ref="nicknameInput"
                   v-model="nicknameDraft" @blur="saveNickname" @keyup.enter="saveNickname"
                   class="nickname-input">
            <h3 v-else @click="startEditNickname">{{ auth.state.nickname || '用户' }} ✎</h3>
          </div>
          <p v-if="auth.state.username">@{{ auth.state.username }}</p>
          <p v-if="profile && (profile.age || profile.heightCm || profile.weightKg)">
            {{ profile.age || '-' }}岁 |
            {{ profile.heightCm || '-' }}cm |
            {{ profile.weightKg || '-' }}kg
          </p>
        </div>
      </div>

      <template v-if="auth.state.isLoggedIn">
        <!-- Health goal -->
        <div class="card">
          <h3 class="card-title">🎯 健康目标</h3>
          <select v-model="form.goal" class="profile-select">
            <option value="均衡">均衡饮食</option>
            <option value="减脂">减脂</option>
            <option value="增肌">增肌</option>
            <option value="控糖">控糖</option>
          </select>
        </div>

        <!-- Personal info -->
        <div class="card">
          <h3 class="card-title">📋 个人资料</h3>
          <div class="form-row">
            <label>年龄</label>
            <input type="number" v-model.number="form.age" placeholder="岁">
          </div>
          <div class="form-row">
            <label>身高(cm)</label>
            <input type="number" v-model.number="form.heightCm" placeholder="cm" step="0.1">
          </div>
          <div class="form-row">
            <label>体重(kg)</label>
            <input type="number" v-model.number="form.weightKg" placeholder="kg" step="0.1">
          </div>
          <div class="form-row">
            <label>性别</label>
            <select v-model="form.gender">
              <option value="">未设置</option>
              <option value="男">男</option>
              <option value="女">女</option>
            </select>
          </div>
        </div>

        <!-- Preferences -->
        <div class="card">
          <h3 class="card-title">😋 口味偏好</h3>
          <div class="tag-selector">
            <!-- 预设标签 -->
            <button v-for="taste in tasteOptions" :key="taste"
                    class="tag-btn preset-tag"
                    :class="{ selected: selectedTastes.includes(taste) }"
                    @click="toggleTaste(taste)">
              {{ taste }}
            </button>
            <!-- 自定义口味标签（始终选中，不参与 toggle，仅通过 × 删除） -->
            <template v-for="(taste, idx) in selectedTastes.filter(t => isCustomTaste(t))" :key="'c-taste-'+idx">
              <span v-if="editingTag.group === 'taste' && editingTag.oldName === taste" class="tag-input-wrapper">
                <input v-model="editingTag.draft" class="tag-input"
                       @blur="confirmEditTag" @keyup.enter="confirmEditTag" />
              </span>
              <span v-else class="tag-btn custom-tag selected">
                {{ taste }}
                <span class="tag-actions">
                  <span class="tag-edit" @click.stop="startEditTaste(taste)">✎</span>
                  <span class="tag-delete" @click.stop="deleteTaste(taste)">×</span>
                </span>
              </span>
            </template>
            <!-- + 自定义 或输入框 -->
            <span v-if="addingTaste" class="tag-input-wrapper">
              <input v-model="tasteDraft" class="tag-input" placeholder="输入自定义标签"
                     @blur="confirmAddTaste" @keyup.enter="confirmAddTaste" />
            </span>
            <button v-else class="tag-btn tag-add-btn" @click="startAddTaste">
              + 自定义
            </button>
          </div>
        </div>

        <!-- Taboo -->
        <div class="card">
          <h3 class="card-title">🚫 忌口</h3>
          <div class="tag-selector">
            <!-- 预设标签 -->
            <button v-for="t in tabooOptions" :key="t"
                    class="tag-btn preset-tag"
                    :class="{ selected: selectedTaboos.includes(t) }"
                    @click="toggleTaboo(t)">
              {{ t }}
            </button>
            <!-- 自定义忌口标签（始终选中，不参与 toggle，仅通过 × 删除） -->
            <template v-for="(t, idx) in selectedTaboos.filter(t => isCustomTaboo(t))" :key="'c-taboo-'+idx">
              <span v-if="editingTag.group === 'taboo' && editingTag.oldName === t" class="tag-input-wrapper">
                <input v-model="editingTag.draft" class="tag-input"
                       @blur="confirmEditTag" @keyup.enter="confirmEditTag" />
              </span>
              <span v-else class="tag-btn custom-tag selected">
                {{ t }}
                <span class="tag-actions">
                  <span class="tag-edit" @click.stop="startEditTaboo(t)">✎</span>
                  <span class="tag-delete" @click.stop="deleteTaboo(t)">×</span>
                </span>
              </span>
            </template>
            <!-- + 自定义 或输入框 -->
            <span v-if="addingTaboo" class="tag-input-wrapper">
              <input v-model="tabooDraft" class="tag-input" placeholder="输入自定义标签"
                     @blur="confirmAddTaboo" @keyup.enter="confirmAddTaboo" />
            </span>
            <button v-else class="tag-btn tag-add-btn" @click="startAddTaboo">
              + 自定义
            </button>
          </div>
        </div>

        <!-- Warning Profile -->
        <div class="card">
          <h3 class="card-title">⚠️ 慢性病/特殊饮食</h3>
          <div class="tag-selector">
            <!-- 预设标签 -->
            <button v-for="w in warningOptions" :key="w"
                    class="tag-btn preset-tag"
                    :class="{ selected: selectedWarnings.includes(w) }"
                    @click="toggleWarning(w)">
              {{ w }}
            </button>
            <!-- 自定义病症标签（始终选中，不参与 toggle，仅通过 × 删除） -->
            <template v-for="(w, idx) in selectedWarnings.filter(w => isCustomWarning(w))" :key="'c-warn-'+idx">
              <span v-if="editingTag.group === 'warning' && editingTag.oldName === w" class="tag-input-wrapper">
                <input v-model="editingTag.draft" class="tag-input"
                       @blur="confirmEditTag" @keyup.enter="confirmEditTag" />
              </span>
              <span v-else class="tag-btn custom-tag selected">
                {{ w }}
                <span class="tag-actions">
                  <span class="tag-edit" @click.stop="startEditWarning(w)">✎</span>
                  <span class="tag-delete" @click.stop="deleteWarning(w)">×</span>
                </span>
              </span>
            </template>
            <!-- + 自定义 或输入框 -->
            <span v-if="addingWarning" class="tag-input-wrapper">
              <input v-model="warningDraft" class="tag-input" placeholder="输入自定义标签"
                     @blur="confirmAddWarning" @keyup.enter="confirmAddWarning" />
            </span>
            <button v-else class="tag-btn tag-add-btn" @click="startAddWarning">
              + 自定义
            </button>
          </div>
        </div>

        <!-- Alert rules -->
        <div class="card">
          <h3 class="card-title">🔔 预警阈值设置</h3>
          <div class="alert-rule" v-for="rule in alertRules" :key="rule.id">
            <div class="rule-header">
              <span class="rule-name">{{ nutrientLabels[rule.nutrientType] || rule.nutrientType }}</span>
              <label class="switch">
                <input type="checkbox" v-model="rule.enabled" @change="toggleRule(rule)">
                <span class="slider"></span>
              </label>
            </div>
            <div class="rule-input" v-if="rule.enabled">
              <input type="number" v-model.number="rule.threshold" @change="updateRule(rule)">
              <span class="rule-unit">{{ nutrientUnits[rule.nutrientType] || '' }}</span>
            </div>
          </div>
          <button class="btn btn-ai-analyze" @click="analyzeThreshold" :disabled="analyzingThreshold">
            {{ analyzingThreshold ? '分析中...' : '🤖 AI 智能分析' }}
          </button>
        </div>

        <!-- Save button -->
        <button class="btn btn-primary save-btn" @click="saveProfile">保存设置</button>

        <!-- 退出登录按钮 -->
        <button v-if="auth.state.isLoggedIn" class="btn logout-btn" @click="handleLogout">
          退出登录
        </button>
      </template>

      <!-- 未登录占位 -->
      <div v-if="!auth.state.isLoggedIn" style="text-align: center; padding: 40px 0; color: #ccc; font-size: 14px; line-height: 2;">
        登录后可设置<br>健康目标 · 个人资料 · 预警阈值
      </div>


      <!-- App info -->
      <div class="app-info">
        <p>AI智能个人健康饮食助手 v1.0</p>
        <p>本系统为课程实验作品，饮食建议仅供参考</p>
      </div>
    </template>

  <!-- 头像裁剪模态框 -->
  <div v-if="showAvatarModal" class="modal-overlay" @click.self="closeAvatarModal">
    <div class="modal-sheet" style="width: 340px;">
      <div class="modal-header">
        <span class="modal-title">编辑头像</span>
        <span class="modal-close" @click="closeAvatarModal">✕</span>
      </div>

      <!-- 裁剪区域 -->
      <div class="crop-container">
        <img ref="cropImage" :src="avatarCropSrc" alt="" class="crop-image" />
      </div>
      <p class="crop-hint">拖动或缩放调整头像区域</p>

      <!-- 缩放滑块 -->
      <div class="zoom-control">
        <span>🔍</span>
        <input type="range" min="50" max="200" value="100" class="zoom-slider"
               @input="onZoomChange" />
        <span>🔎</span>
      </div>

      <!-- 选择图片按钮 -->
      <div class="crop-actions-secondary">
        <button class="btn btn-outline btn-sm" @click="selectCropImage">选择图片</button>
        <input type="file" ref="cropFileInput" accept="image/*" style="display:none"
               @change="onCropFileChange" />
      </div>

      <!-- 底部按钮 -->
      <div class="crop-actions">
        <button class="btn btn-cancel" @click="closeAvatarModal">取消</button>
        <button class="btn btn-primary btn-save" @click="saveAvatar" :disabled="!cropperReady">
          保存头像
        </button>
      </div>
    </div>
  </div>

  <!-- 登录/注册模态框 -->
  <div v-if="showAuthModal" class="modal-overlay" @click.self="showAuthModal = false">
    <div class="modal-sheet">
      <div class="modal-header">
        <div class="modal-tabs">
          <span :class="{ active: authTab === 'login' }" @click="authTab = 'login'">登录</span>
          <span :class="{ active: authTab === 'register' }" @click="authTab = 'register'">注册</span>
        </div>
        <span class="modal-close" @click="showAuthModal = false">✕</span>
      </div>

      <!-- 登录表单 -->
      <div v-if="authTab === 'login'" class="modal-form">
        <input v-model="loginForm.username" placeholder="请输入用户名" class="modal-input" />
        <input v-model="loginForm.password" type="password" placeholder="请输入密码" class="modal-input" />
        <p v-if="authError" class="auth-error">{{ authError }}</p>
        <button class="btn btn-primary modal-btn" @click="handleLogin" :disabled="authLoading">
          {{ authLoading ? '登录中...' : '登录' }}
        </button>
      </div>

      <!-- 注册表单 -->
      <div v-if="authTab === 'register'" class="modal-form">
        <input v-model="registerForm.username" placeholder="请设置用户名（至少2字符）" class="modal-input" />
        <input v-model="registerForm.password" type="password" placeholder="请设置密码（至少6字符）" class="modal-input" />
        <input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" class="modal-input" />
        <p v-if="authError" class="auth-error">{{ authError }}</p>
        <button class="btn btn-primary modal-btn" @click="handleRegister" :disabled="authLoading">
          {{ authLoading ? '注册中...' : '注册' }}
        </button>
      </div>
    </div>
  </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import api from '../api/index.js'
import auth from '../auth.js'
import toast from '../toast.js'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

const loading = ref(false)
const profile = ref(null)
const alertRules = ref([])
const nicknameInput = ref(null)

const editingNickname = ref(false)
const nicknameDraft = ref('')

const tasteOptions = ['清淡', '中式', '西式', '日式', '辣味', '酸甜', '咸鲜']
const tabooOptions = ['海鲜', '花生', '牛奶', '鸡蛋', '豆制品', ' gluten', '辛辣']
const warningOptions = ['糖尿病', '高血压', '高血脂', '痛风']

const nutrientLabels = { calorie: '每日热量上限', sugar: '每日糖分上限', sodium: '每日钠上限', protein: '每日蛋白质目标', fat: '每日脂肪上限', carb: '每日碳水目标' }
const nutrientUnits = { calorie: 'kcal', sugar: 'g', sodium: 'mg', protein: 'g', fat: 'g', carb: 'g' }

const form = ref({
  goal: '均衡',
  age: null,
  heightCm: null,
  weightKg: null,
  tastePreference: '',
  taboo: '',
  warningProfile: '',
  gender: '',
})

const selectedTastes = ref([])
const selectedTaboos = ref([])

// 从 form.warningProfile 字符串派生的 selected 数组
const selectedWarnings = ref([])

// 更新 selectedWarnings（在 fetchData 中同步）
function syncWarningFromForm() {
  selectedWarnings.value = form.value.warningProfile
    ? form.value.warningProfile.split(',').map(s => s.trim()).filter(Boolean)
    : []
}

// ==================== 认证相关 ====================
const showAuthModal = ref(false)
const authTab = ref('login')
const authLoading = ref(false)
const authError = ref('')
const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', confirmPassword: '' })

// ==================== 头像裁剪 ====================
const showAvatarModal = ref(false)
const avatarCropSrc = ref('')
const cropImage = ref(null)
const cropFileInput = ref(null)
const cropperReady = ref(false)
let cropperInstance = null

function openCropModal() {
  avatarCropSrc.value = auth.state.avatarUrl || ''
  showAvatarModal.value = true
  nextTick(() => {
    if (avatarCropSrc.value) {
      initCropper()
    } else {
      selectCropImage()
    }
  })
}

function initCropper() {
  const img = cropImage.value
  if (!img) return
  if (cropperInstance) cropperInstance.destroy()
  cropperInstance = new Cropper(img, {
    aspectRatio: 1,
    viewMode: 1,
    dragMode: 'move',
    guides: false,
    background: false,
    rotatable: false,
    scalable: true,
    zoomable: true,
    zoomOnWheel: true,
    ready() {
      cropperReady.value = true
    },
  })
}

function onZoomChange(e) {
  if (!cropperInstance) return
  cropperInstance.zoomTo(Number(e.target.value) / 100)
}

function selectCropImage() {
  cropFileInput.value?.click()
}

function onCropFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  // 释放上一个 blob URL
  if (avatarCropSrc.value && avatarCropSrc.value.startsWith('blob:')) {
    URL.revokeObjectURL(avatarCropSrc.value)
  }
  const url = URL.createObjectURL(file)
  avatarCropSrc.value = url
  cropperReady.value = false
  if (cropperInstance) {
    cropperInstance.replace(url)
    cropperReady.value = true
  } else {
    // Cropper not initialized yet (first time selecting image)
    nextTick(() => initCropper())
  }
  e.target.value = ''
}

async function saveAvatar() {
  if (!cropperInstance) return
  const canvas = cropperInstance.getCroppedCanvas({ width: 256, height: 256 })
  canvas.toBlob(async (blob) => {
    const file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
    try {
      await auth.uploadAvatar(file)
      closeAvatarModal()
    } catch (e) {
      toast.show('头像上传失败')
    }
  }, 'image/jpeg', 0.9)
}

function closeAvatarModal() {
  showAvatarModal.value = false
  cropperReady.value = false
  // 释放 blob URL
  if (avatarCropSrc.value && avatarCropSrc.value.startsWith('blob:')) {
    URL.revokeObjectURL(avatarCropSrc.value)
  }
  avatarCropSrc.value = ''
  if (cropperInstance) {
    cropperInstance.destroy()
    cropperInstance = null
  }
}

async function handleLogin() {
  authError.value = ''
  if (!loginForm.value.username.trim() || !loginForm.value.password.trim()) {
    authError.value = '请填写用户名和密码'
    return
  }
  authLoading.value = true
  try {
    await auth.login(loginForm.value.username.trim(), loginForm.value.password)
    showAuthModal.value = false
    loginForm.value = { username: '', password: '' }
    await fetchData()
  } catch (e) {
    const msg = e?.response?.data?.message
    authError.value = msg || '登录失败，请重试'
    console.error('登录失败', e)
  } finally {
    authLoading.value = false
  }
}

async function handleRegister() {
  authError.value = ''
  const { username, password, confirmPassword } = registerForm.value
  if (!username.trim() || !password.trim()) {
    authError.value = '请填写用户名和密码'
    return
  }
  if (username.trim().length < 2) {
    authError.value = '用户名至少 2 个字符'
    return
  }
  if (password.length < 6) {
    authError.value = '密码至少 6 个字符'
    return
  }
  if (password !== confirmPassword) {
    authError.value = '两次密码不一致'
    return
  }
  authLoading.value = true
  try {
    await auth.register(username.trim(), password)
    showAuthModal.value = false
    registerForm.value = { username: '', password: '', confirmPassword: '' }
    await fetchData()
  } catch (e) {
    const msg = e?.response?.data?.message
    authError.value = msg || '注册失败，请重试'
    console.error('注册失败', e)
  } finally {
    authLoading.value = false
  }
}

async function handleLogout() {
  if (!confirm('确定要退出登录吗？')) return
  await auth.logout()
  profile.value = null
}


async function fetchData() {
  if (!auth.state.isLoggedIn) return
  loading.value = true
  try {
    const [profileRes, rulesRes] = await Promise.all([
      api.getProfile(),
      api.getAlertRules(),
    ])
    const p = profileRes.data?.data
    profile.value = p
    form.value.goal = p.goal || '均衡'
    form.value.age = p.age
    form.value.heightCm = p.heightCm
    form.value.weightKg = p.weightKg
    selectedTastes.value = p.tastePreference ? p.tastePreference.split(',').map(s => s.trim()).filter(Boolean) : []
    selectedTaboos.value = p.taboo ? p.taboo.split(',').map(s => s.trim()).filter(Boolean) : []
    form.value.warningProfile = p.warningProfile || ''
    form.value.gender = p.gender || ''
    syncWarningFromForm()

    alertRules.value = rulesRes.data?.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function startEditNickname() {
  nicknameDraft.value = profile.value?.nickname || ''
  editingNickname.value = true
  nextTick(() => {
    nicknameInput.value?.focus()
  })
}

async function saveNickname() {
  editingNickname.value = false
  const trimmed = nicknameDraft.value.trim()
  if (trimmed && trimmed !== profile.value?.nickname) {
    try {
      await api.updateProfile({ nickname: trimmed })
      auth.state.nickname = trimmed
      if (profile.value) profile.value.nickname = trimmed
    } catch (e) {
      console.error('Failed to update nickname', e)
    }
  }
}

function toggleTaste(taste) {
  const i = selectedTastes.value.indexOf(taste)
  if (i >= 0) selectedTastes.value.splice(i, 1)
  else selectedTastes.value.push(taste)
}

function toggleTaboo(taboo) {
  const i = selectedTaboos.value.indexOf(taboo)
  if (i >= 0) selectedTaboos.value.splice(i, 1)
  else selectedTaboos.value.push(taboo)
}

function toggleWarning(w) {
  const i = selectedWarnings.value.indexOf(w)
  if (i >= 0) selectedWarnings.value.splice(i, 1)
  else selectedWarnings.value.push(w)
  form.value.warningProfile = selectedWarnings.value.join(',')
}

// ==================== 判断自定义标签 ====================
function isCustomTaste(tag) { return !tasteOptions.includes(tag) }
function isCustomTaboo(tag) { return !tabooOptions.includes(tag) }
function isCustomWarning(tag) { return !warningOptions.includes(tag) }

// ==================== 添加自定义标签状态 ====================
const addingTaste = ref(false)
const addingTaboo = ref(false)
const addingWarning = ref(false)
const tasteDraft = ref('')
const tabooDraft = ref('')
const warningDraft = ref('')

// ==================== 编辑自定义标签状态 ====================
const editingTag = ref({ group: '', oldName: '', draft: '' })
const analyzingThreshold = ref(false)

function startAddTaste()      { addingTaste.value = true; tasteDraft.value = '' }
function startAddTaboo()      { addingTaboo.value = true; tabooDraft.value = '' }
function startAddWarning()    { addingWarning.value = true; warningDraft.value = '' }

function confirmAddTaste() {
  const name = tasteDraft.value.trim()
  addingTaste.value = false
  if (!name) return
  if (selectedTastes.value.includes(name)) {
    toast.show('标签已存在')
    return
  }
  selectedTastes.value.push(name)
  tasteDraft.value = ''
}

function confirmAddTaboo() {
  const name = tabooDraft.value.trim()
  addingTaboo.value = false
  if (!name) return
  if (selectedTaboos.value.includes(name)) {
    toast.show('标签已存在')
    return
  }
  selectedTaboos.value.push(name)
  tabooDraft.value = ''
}

function confirmAddWarning() {
  const name = warningDraft.value.trim()
  addingWarning.value = false
  if (!name) return
  if (selectedWarnings.value.includes(name)) {
    toast.show('标签已存在')
    return
  }
  selectedWarnings.value.push(name)
  form.value.warningProfile = selectedWarnings.value.join(',')
  warningDraft.value = ''
}

// ==================== 编辑自定义标签 ====================
function startEditTaste(oldName) {
  editingTag.value = { group: 'taste', oldName, draft: oldName }
}
function startEditTaboo(oldName) {
  editingTag.value = { group: 'taboo', oldName, draft: oldName }
}
function startEditWarning(oldName) {
  editingTag.value = { group: 'warning', oldName, draft: oldName }
}

function confirmEditTag() {
  const { group, oldName, draft } = editingTag.value
  const newName = draft.trim()
  editingTag.value = { group: '', oldName: '', draft: '' }
  if (!newName || newName === oldName) return

  if (group === 'taste') {
    if (selectedTastes.value.includes(newName)) { toast.show('标签已存在'); return }
    const i = selectedTastes.value.indexOf(oldName)
    if (i >= 0) selectedTastes.value[i] = newName
  } else if (group === 'taboo') {
    if (selectedTaboos.value.includes(newName)) { toast.show('标签已存在'); return }
    const i = selectedTaboos.value.indexOf(oldName)
    if (i >= 0) selectedTaboos.value[i] = newName
  } else if (group === 'warning') {
    if (selectedWarnings.value.includes(newName)) { toast.show('标签已存在'); return }
    const i = selectedWarnings.value.indexOf(oldName)
    if (i >= 0) selectedWarnings.value[i] = newName
    form.value.warningProfile = selectedWarnings.value.join(',')
  }
}

function cancelEditTag() {
  editingTag.value = { group: '', oldName: '', draft: '' }
}

// ==================== 删除自定义标签 ====================
async function deleteTaste(tag) {
  if (!confirm(`确定删除"${tag}"吗？`)) return
  const i = selectedTastes.value.indexOf(tag)
  if (i >= 0) selectedTastes.value.splice(i, 1)
  await saveProfile()
}

async function deleteTaboo(tag) {
  if (!confirm(`确定删除"${tag}"吗？`)) return
  const i = selectedTaboos.value.indexOf(tag)
  if (i >= 0) selectedTaboos.value.splice(i, 1)
  await saveProfile()
}

async function deleteWarning(tag) {
  if (!confirm(`确定删除"${tag}"吗？`)) return
  const i = selectedWarnings.value.indexOf(tag)
  if (i >= 0) selectedWarnings.value.splice(i, 1)
  form.value.warningProfile = selectedWarnings.value.join(',')
  await saveProfile()
}

async function analyzeThreshold() {
  analyzingThreshold.value = true
  try {
    await api.updateProfile({
      goal: form.value.goal,
      age: form.value.age,
      heightCm: form.value.heightCm,
      weightKg: form.value.weightKg,
      gender: form.value.gender,
      tastePreference: selectedTastes.value.join(','),
      taboo: selectedTaboos.value.join(','),
      warningProfile: form.value.warningProfile,
    })
    const res = await api.analyzeAlertRules()
    alertRules.value = res.data?.data || []
    toast.show('AI 分析完成，可手动调整后保存')
  } catch (e) {
    toast.show('AI 分析失败，请稍后重试')
  } finally {
    analyzingThreshold.value = false
  }
}

async function saveProfile() {
  try {
    await api.updateProfile({
      goal: form.value.goal,
      age: form.value.age,
      heightCm: form.value.heightCm,
      weightKg: form.value.weightKg,
      tastePreference: selectedTastes.value.join(','),
      taboo: selectedTaboos.value.join(','),
      warningProfile: form.value.warningProfile,
    })
    toast.show('保存成功')
  } catch (e) {
    toast.show('保存失败，请稍后重试')
  }
}

async function toggleRule(rule) {
  await api.updateAlertRule(rule.id, { enabled: rule.enabled })
}

async function updateRule(rule) {
  await api.updateAlertRule(rule.id, { threshold: rule.threshold })
}

onMounted(fetchData)
</script>

<style scoped>
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: linear-gradient(135deg, #E8F5E9, #C8E6C9);
  border: none;
}
.user-avatar {
  font-size: 48px;
  width: 64px;
  height: 64px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-info .nickname-row h3 {
  font-size: 18px;
  margin-bottom: 4px;
  cursor: pointer;
}
.user-info .nickname-row h3:hover { color: #4CAF50; }
.nickname-input {
  font-size: 18px;
  font-weight: 600;
  padding: 4px 8px;
  border: 1px solid #4CAF50;
  border-radius: 6px;
  width: auto;
  max-width: 200px;
}
.user-info p { font-size: 13px; color: #666; }

.card-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }

.form-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.form-row label {
  width: 80px;
  font-size: 13px;
  color: #666;
  flex-shrink: 0;
}

.profile-select {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  width: 100%;
}

.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag-btn {
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 16px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.tag-btn.selected {
  background: #4CAF50;
  color: #fff;
  border-color: #4CAF50;
}
.tag-btn:active { transform: scale(0.95); }

/* 自定义标签 */
.tag-btn.custom-tag {
  border-style: dashed;
  border-color: #aaa;
  position: relative;
}
.tag-btn.custom-tag.selected {
  display: inline-flex;
  align-items: center;
  width: auto;
  border-color: #4CAF50;
  padding-right: 48px; /* 为 ✎× 留空间 */
}

/* 操作图标（✎ 编辑 / × 删除） */
.tag-actions {
  display: inline-flex;
  gap: 4px;
  margin-left: 6px;
  vertical-align: middle;
}
.tag-edit, .tag-delete {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  font-size: 11px;
  line-height: 1;
  cursor: pointer;
  color: #fff;
  transition: opacity 0.15s;
}
.tag-edit { background: #4CAF50; }
.tag-delete { background: #f44336; }
.tag-edit:active, .tag-delete:active { opacity: 0.6; }

/* 内联输入框（新增/编辑自定义标签） */
.tag-input-wrapper {
  display: inline-block;
  vertical-align: middle;
}
.tag-input {
  padding: 6px 14px;
  border: 1px solid #4CAF50;
  border-radius: 16px;
  font-size: 13px;
  outline: none;
  width: 130px;
  background: #fafffe;
  transition: border-color 0.2s;
}
.tag-input:focus {
  border-color: #2E7D32;
}

/* + 自定义 添加按钮 */
.tag-add-btn {
  border-style: dashed;
  color: #4CAF50;
  border-color: #4CAF50;
  background: #f0faf0;
}
.tag-add-btn:active {
  background: #4CAF50;
  color: #fff;
}

.alert-rule {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.rule-name { font-size: 14px; }
.rule-input {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.rule-input input { width: 120px; }
.rule-unit { font-size: 12px; color: #999; }

/* Toggle switch */
.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}
.switch input { opacity: 0; width: 0; height: 0; }
.slider {
  position: absolute;
  cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background: #ccc;
  transition: 0.3s;
  border-radius: 24px;
}
.slider:before {
  content: "";
  position: absolute;
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background: white;
  transition: 0.3s;
  border-radius: 50%;
}
input:checked + .slider { background: #4CAF50; }
input:checked + .slider:before { transform: translateX(20px); }

.btn-ai-analyze {
  width: 100%;
  margin-top: 12px;
  padding: 10px;
  font-size: 14px;
  color: #4CAF50;
  border: 1px dashed #4CAF50;
  border-radius: 8px;
  background: #f0faf0;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-ai-analyze:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-ai-analyze:not(:disabled):active {
  background: #4CAF50;
  color: #fff;
}

.save-btn {
  width: 100%;
  margin-top: 16px;
  padding: 12px;
  font-size: 16px;
}


.app-info {
  text-align: center;
  padding: 24px;
  color: #bbb;
  font-size: 12px;
  line-height: 1.6;
}

/* 头像相关 */
.avatar-wrapper {
  position: relative;
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  cursor: pointer;
}
.avatar-wrapper .user-avatar {
  width: 100%;
  height: 100%;
}
.user-avatar-img {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}
.avatar-camera {
  position: absolute;
  bottom: 0;
  right: -2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #4CAF50;
  border: 2px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}
.logout-btn {
  width: 100%;
  margin-top: 12px;
  padding: 12px;
  font-size: 15px;
  color: #f44336;
  border: 1px solid #f44336;
  background: #fff;
  border-radius: 8px;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
}
.modal-sheet {
  width: calc(100% - 32px);
  max-width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 20px 16px 24px;
  animation: modalFadeIn 0.25s ease-out;
}
@keyframes modalFadeIn {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.modal-tabs {
  display: flex;
  gap: 20px;
}
.modal-tabs span {
  font-size: 15px;
  color: #999;
  cursor: pointer;
  padding-bottom: 4px;
  transition: color 0.2s;
}
.modal-tabs span.active {
  font-weight: 700;
  font-size: 18px;
  color: #4CAF50;
  border-bottom: 2px solid #4CAF50;
}
.modal-close {
  font-size: 20px;
  color: #999;
  cursor: pointer;
}
.modal-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.modal-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}
.modal-input:focus {
  border-color: #4CAF50;
}
.modal-btn {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  font-weight: 600;
  margin-top: 4px;
}
.modal-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.auth-error {
  color: #f44336;
  font-size: 13px;
  margin: 0;
}

/* 头像裁剪模态框 */
.modal-title {
  font-size: 16px;
  font-weight: 600;
}
.crop-container {
  width: 100%;
  height: 280px;
  background: #333;
  border-radius: 8px;
  overflow: hidden;
  margin: 0 0 8px;
}
.crop-container .crop-image {
  max-width: 100%;
  max-height: 100%;
  display: block;
}
.crop-hint {
  text-align: center;
  font-size: 12px;
  color: #999;
  margin: 0 0 12px;
}
.zoom-control {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px 14px;
}
.zoom-slider {
  flex: 1;
  accent-color: #4CAF50;
}
.crop-actions-secondary {
  padding-bottom: 12px;
  text-align: center;
}
.crop-actions {
  display: flex;
  gap: 12px;
}
.btn-cancel {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: #fff;
  color: #666;
  font-size: 14px;
}
.btn-save {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: #4CAF50;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}
.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
