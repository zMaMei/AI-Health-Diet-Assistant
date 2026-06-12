# 头像裁剪上传 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将头像上传从"直接选文件上传"改为"裁剪模态框"交互，支持拖拽定位、缩放、圆形裁剪后上传。

**Architecture:** 引入 cropperjs 库，在 ProfileView 中新增裁剪模态框。点击头像/相机图标弹出模态框 → 加载当前头像或选择新图片 → cropperjs 处理拖拽/缩放 → Canvas 输出裁剪结果 → Blob → auth.uploadAvatar()。

**Tech Stack:** Vue 3 + cropperjs (dependency)

---

## File Structure

```
frontend/
├── package.json                  (MOD) — 新增 cropperjs 依赖
├── src/views/ProfileView.vue     (MOD) — 唯一业务改动文件
```

---

### Task 1: 安装 cropperjs

**Files:**
- Modify: `frontend/package.json`

- [ ] **Step 1: 安装 cropperjs**

```bash
cd D:\zMa\code\AI\frontend && npm install cropperjs
```

- [ ] **Step 2: 验证安装**

```bash
cd D:\zMa\code\AI\frontend && npm run build
```

Expected: BUILD SUCCESS，无错误。

- [ ] **Step 3: Commit**

```bash
git add frontend/package.json frontend/package-lock.json
git commit -m "feat: 安装 cropperjs 头像裁剪库"
```

---

### Task 2: 改造 ProfileView.vue — Template

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: 修改用户卡片 — 点击触发的动作**

将第 17 行的：
```html
<div class="avatar-wrapper" @click="triggerAvatarUpload">
```
改为（调用 Task 3 中定义的方法）：
```html
<div class="avatar-wrapper" @click="openCropModal">
```

- [ ] **Step 2: 删除隐藏的 file input**

将第 22 行删除：
```html
<input type="file" ref="avatarInput" accept="image/*" style="display:none" @change="onAvatarChange" />
```

- [ ] **Step 3: 在登录/注册模态框之前添加裁剪模态框**

在 `<!-- 登录/注册模态框 -->` 之前（即 `</template>` 之前）添加：

```html
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
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/ProfileView.vue
git commit -m "feat: 头像裁剪模态框 — template"
```

---

### Task 3: 改造 ProfileView.vue — Script

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: 添加 import**

在 `<script setup>` 顶部现有 import 之后添加：

```javascript
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'
```

- [ ] **Step 2: 删除旧的 avatar 相关代码**

删除以下三行：
```javascript
const avatarInput = ref(null)              // 第 235 行
function triggerAvatarUpload() { ... }      // 第 297-299 行
async function onAvatarChange(e) { ... }    // 第 301-311 行
```

- [ ] **Step 3: 添加裁剪相关状态和方法**

在认证相关状态区域（`const registerForm = ...` 之后）添加：

```javascript
// ==================== 头像裁剪 ====================
const showAvatarModal = ref(false)
const avatarCropSrc = ref('')
const cropImage = ref(null)
const cropFileInput = ref(null)
const cropperReady = ref(false)
let cropperInstance = null

function openCropModal() {
  // 设置初始图片：当前头像或引导选择
  if (auth.state.avatarUrl) {
    avatarCropSrc.value = auth.state.avatarUrl
  }
  showAvatarModal.value = true
}

function initCropper() {
  // 等待图片 DOM 渲染后初始化 Cropper
  const img = cropImage.value
  if (!img) return
  // 销毁旧实例（如有）
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
  const ratio = Number(e.target.value) / 100
  const canvasData = cropperInstance.getCanvasData()
  cropperInstance.zoomTo(ratio)
}

function selectCropImage() {
  cropFileInput.value?.click()
}

function onCropFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  const url = URL.createObjectURL(file)
  avatarCropSrc.value = url
  cropperReady.value = false
  // Cropper 需要图片重新加载，用 nextTick + replace
  setTimeout(() => {
    if (cropperInstance) {
      cropperInstance.replace(url)
      cropperReady.value = true
    }
  }, 100)
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
      alert('头像上传失败')
    }
  }, 'image/jpeg', 0.9)
}

function closeAvatarModal() {
  showAvatarModal.value = false
  cropperReady.value = false
  if (cropperInstance) {
    cropperInstance.destroy()
    cropperInstance = null
  }
}
```

- [ ] **Step 4: 用 watchEffect 监听 DOM 渲染**

需要在模态框打开且图片渲染后初始化 Cropper。在 `onMounted` 附近添加：

```javascript
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'

// 在 openCropModal 中改为：
function openCropModal() {
  if (auth.state.avatarUrl) {
    avatarCropSrc.value = auth.state.avatarUrl
  } else {
    // 无头像时自动弹出文件选择
    avatarCropSrc.value = ''
  }
  showAvatarModal.value = true
  nextTick(() => {
    // 如果有图片 src 则初始化 cropper，否则先触发文件选择
    if (avatarCropSrc.value) {
      initCropper()
    } else {
      selectCropImage()
    }
  })
}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/ProfileView.vue
git commit -m "feat: 头像裁剪模态框 — script"
```

---

### Task 4: 改造 ProfileView.vue — Style

**Files:**
- Modify: `frontend/src/views/ProfileView.vue`

- [ ] **Step 1: 添加裁剪模态框 CSS**

在现有 `.modal-btn:disabled` 之后（style 末尾前）添加：

```css
/* 裁剪模态框 */
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
```

- [ ] **Step 2: 删除不再使用的 CSS（可选）**

`.avatar-camera` 保留（装饰性📷图标仍显示）。不需要删除任何样式。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/ProfileView.vue
git commit -m "feat: 头像裁剪模态框 — CSS"
```

---

### Task 5: 最终验证

- [ ] **Step 1: 构建验证**

```bash
cd D:\zMa\code\AI\frontend && npm run build
```

Expected: BUILD SUCCESS。

- [ ] **Step 2: 交互验证清单**

| 场景 | 预期 |
|------|------|
| 已登录，有头像 → 点头像 | 弹出裁剪模态框，显示当前头像，可拖拽/缩放 |
| 已登录，无头像 → 点头像 | 弹出裁剪模态框 → 自动弹出文件选择 |
| 缩放滑块 | 图片跟随缩放 |
| 点"选择图片" | 文件选择器弹出 → 选完后替换裁剪区图片 |
| 点"保存头像" | Canvas 裁剪 → 上传 → 头像刷新 → 模态框关闭 |
| 点"取消" / 点遮罩 | 模态框关闭，头像不变 |
| 未登录 | 裁剪模态框不出现（头像区域不可点击） |

- [ ] **Step 3: Commit（如有 lint fix）**

```bash
git add -A && git commit -m "chore: 头像裁剪验证通过"
```
