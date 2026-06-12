# 头像裁剪上传 — 设计文档

**日期**: 2026-06-13
**状态**: 已确认
**分支**: feature/MifuneShioriko-0613

---

## 一、概述

将头像上传从"直接选文件上传原图"改为"裁剪模态框"交互，用户可拖拽定位、缩放图片，确认后裁剪并上传。

---

## 二、交互流程

```
点击头像区域（头像图片/📷图标）
  → 弹出裁剪模态框（页面居中）
  → 模态框内默认显示当前头像图片
  → 用户可：
      ① 点击"选择图片"换新图
      ② 拖拽图片调整裁剪位置
      ③ 滑动缩放条放大/缩小
      ④ 圆形裁剪框实时预览（aspectRatio=1）
  → 点击"保存头像" → Canvas 裁剪输出(256×256) → Blob → File → auth.uploadAvatar()
    → 更新 auth.state.avatarUrl → 关模态框
  → 点击"取消"或点击遮罩 → 关模态框，丢弃修改
```

---

## 三、技术方案

### 3.1 依赖

- **cropperjs** — 轻量图片裁剪库，支持拖拽/缩放/Canvas 输出

```bash
npm install cropperjs
```

### 3.2 Cropper 配置

```javascript
const cropper = new Cropper(imageElement, {
  aspectRatio: 1,        // 正方形裁剪（头像）
  viewMode: 1,           // 裁剪框限制在图片内
  dragMode: 'move',      // 默认拖拽移动图片
  guides: false,         // 不显示九宫格辅助线
  background: false,     // 不显示网格背景
  rotatable: false,      // 不需要旋转
  scalable: true,        // 允许缩放
  zoomable: true,        // 允许滚轮缩放
  zoomOnWheel: true,     // 滚轮缩放
})
```

### 3.3 裁剪输出

```javascript
const canvas = cropper.getCroppedCanvas({ width: 256, height: 256 })
canvas.toBlob(async (blob) => {
  const file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
  await auth.uploadAvatar(file)
  showAvatarModal.value = false
}, 'image/jpeg', 0.9)
```

---

## 四、前端改动

### 4.1 文件范围

只改 `frontend/src/views/ProfileView.vue`。后端 `AuthController.uploadAvatar` 不变。

### 4.2 Template 改动

| 位置 | 改前 | 改后 |
|------|------|------|
| 用户卡片 avatar-wrapper | `@click="triggerAvatarUpload"` | `@click="showAvatarModal = true"` |
| 隐藏 input[type=file] | 存在 (`ref="avatarInput"`) | **删除** |
| 新增 | — | 裁剪模态框 HTML |

### 4.3 Script 改动

| 改前 | 改后 |
|------|------|
| `const avatarInput = ref(null)` | **删除** |
| `function triggerAvatarUpload()` | **删除** |
| `async function onAvatarChange(e)` | **删除** |
| — | **新增** `showAvatarModal`, `avatarImageSrc`, `cropperInstance` 响应式变量 |
| — | **新增** `openAvatarModal()` — 弹出模态框 + 初始化 Cropper |
| — | **新增** `selectAvatarFile()` — 触发隐藏 file input 选择新图片 |
| — | **新增** `onAvatarFileChange(e)` — 图片选择后更新 Cropper 的 src |
| — | **新增** `saveAvatar()` — Canvas 裁剪 → Blob → uploadAvatar → 关闭 |
| — | **新增** `closeAvatarModal()` — 销毁 Cropper 实例 + 关闭 |
| `import auth from '../auth.js'` | 增加 `import Cropper from 'cropperjs'` + `import 'cropperjs/dist/cropper.css'` |

### 4.4 Style 改动

| 删除 | 新增 |
|------|------|
| `.avatar-camera` 保留（装饰） | 裁剪模态框全套样式（overlay, sheet, crop-area, slider, buttons） |
| `.avatar-wrapper` 保留 | — |

---

## 五、模态框布局

```
┌──────────────────────────────────┐
│  ✕    编辑头像                    │
├──────────────────────────────────┤
│                                  │
│        ┌──────────┐              │
│        │           │              │
│        │  圆形裁剪  │              │  320×260 裁剪区域
│        │  预览区域  │              │
│        │           │              │
│        └──────────┘              │
│                                  │
│  拖动图片调整位置                 │
├──────────────────────────────────┤
│  🔍  ──────●─────────  🔎       │  缩放滑块
├──────────────────────────────────┤
│  [ 选择图片 ]                     │  更换图片
├──────────────────────────────────┤
│  [ 取消 ]       [ 保存头像 ]      │
└──────────────────────────────────┘
```

- 模态框宽度 `calc(100% - 32px)`，最大 400px，居中
- 裁剪区域高度 280px，深色背景
- 底部按钮左右排列

---

## 六、边界情况

| 场景 | 处理 |
|------|------|
| 首次设置（无旧头像） | 模态框打开后自动触发文件选择 |
| 已有头像 | 模态框显示当前头像图片 |
| 未选择图片点保存 | 按钮置灰/提示 |
| 组件卸载时 Cropper 未销毁 | `onUnmounted` 中调用 `cropper.destroy()` |
