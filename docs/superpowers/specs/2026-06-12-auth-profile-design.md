# 用户认证与个人资料系统 — 设计文档

**日期**: 2026-06-12
**状态**: 已确认
**分支**: feature/MifuneShioriko-0611

---

## 一、概述

为 App 新增用户认证（登录/注册）和头像功能，从"演示环境固定用户 ID=1"升级为真正的多用户系统。

### 范围

| 功能 | 说明 |
|------|------|
| 登录 | 用户名 + 密码，令牌持久化（localStorage），重启 App 保持登录 |
| 注册 | 用户名 + 密码 + 确认密码，注册后自动登录 |
| 退出登录 | 清除令牌，回到未登录态 |
| 头像上传 | 本地磁盘存储，右下角相机图标触发，支持替换更新 |
| 操作拦截 | 未登录用户在记录/分析/推荐页操作时提示"请先在'我的'页面登录" |
| 数据隔离 | 所有饮食记录、照片、语音、档案按 userId 隔离 |

### 不纳入本次范围

- 邮箱/手机号注册
- 密码找回
- 第三方登录
- 修改密码
- 用户注销/删除账号

---

## 二、数据库改动

### 2.1 users 表（变更）

```sql
ALTER TABLE users
  ADD COLUMN username      VARCHAR(32)  NOT NULL UNIQUE COMMENT '登录用户名',
  ADD COLUMN password_hash VARCHAR(128) NOT NULL       COMMENT 'BCrypt 密码哈希';
```

现有演示用户（id=1, nickname='健康达人'）保留，追加 `username='demo', password_hash='<BCrypt("demo123")>'` 使已有数据继续可用。

### 2.2 user_profile 表（变更）

```sql
ALTER TABLE user_profile
  ADD COLUMN avatar_url VARCHAR(255) NULL COMMENT '头像本地路径';
```

路径格式：`/api/uploads/avatars/{userId}/{uuid}.jpg`。文件存储在 `backend/uploads/avatars/`。

---

## 三、后端设计

### 3.1 认证方案 — 自定义拦截器 + UUID Token

- 不引入 Spring Security，手写 `LoginInterceptor` + `WebMvcConfigurer`
- Token 是 UUID 字符串，存于服务端 `ConcurrentHashMap<String, Long>`（token → userId）
- 密码使用 BCrypt 哈希
- 登录/注册成功后返回 token
- 登出时从 Map 中移除 token

### 3.2 新 API

| 方法 | 路径 | 请求 | 响应 | 说明 |
|------|------|------|------|------|
| POST | `/api/auth/register` | `{username, password}` | `{userId, username, nickname, avatarUrl, token}` | 注册成功自动登录 |
| POST | `/api/auth/login` | `{username, password}` | `{userId, username, nickname, avatarUrl, token}` | 登录 |
| POST | `/api/auth/logout` | Header: `Authorization: Bearer <token>` | `{}` | 退出 |
| POST | `/api/auth/avatar` | multipart/form-data (file) + token header | `{avatarUrl}` | 上传/替换头像 |

### 3.3 拦截器逻辑

```
LoginInterceptor.preHandle():
  1. 从 request header 取 "Authorization"
  2. 解析 "Bearer <token>"
  3. token → ConcurrentHashMap 查 userId
  4. 查到 → request.setAttribute("userId", userId) → 放行
  5. 未查到 → 返回 401
```

**拦截范围**：所有 `/api/**` 路径，但以下路径**豁免**：
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/uploads/**`（静态资源）
- `OPTIONS /**`（CORS 预检）

### 3.4 现有 Controller 改造

所有现有接口原本从 query param 或 request body 接收 `userId`，改为从 `request.getAttribute("userId")` 获取。

改动面涉及：
- `DietRecordController` — 查询/创建/更新/删除
- `NutritionController` — 每日营养查询
- `HealthScoreController` — 健康评分
- `RecommendationController` — 推荐/反馈
- `FoodRecognitionController` — 食物识别
- `VoiceParseController` — 语音解析
- `VoiceRecordController` — 语音记录 CRUD
- `MealPhotoController` — 餐照 CRUD
- `UserProfileController` — 档案查询/更新
- `AlertRuleController` — 预警规则 CRUD

### 3.5 头像上传

`AuthService.uploadAvatar(userId, MultipartFile)`:
1. 校验文件类型（仅允许 image/jpeg, image/png, image/gif, image/webp）
2. 限制大小（≤5MB）
3. 保存到 `uploads/avatars/{userId}/{uuid}.{ext}`
4. 更新 `user_profile.avatar_url`
5. 如果已有旧头像文件，删除旧文件

### 3.6 新文件清单

```
backend/src/main/java/com/health/diet/
├── config/
│   └── LoginInterceptor.java        (NEW)
├── controller/
│   └── AuthController.java          (NEW)
├── service/
│   └── AuthService.java             (NEW)
├── dto/command/
│   ├── LoginCommand.java            (NEW)
│   └── RegisterCommand.java         (NEW)
├── dto/vo/
│   └── LoginResultVO.java           (NEW)
```

### 3.7 需修改的文件清单

```
backend/src/main/java/com/health/diet/
├── config/WebMvcConfig.java            — 注册 LoginInterceptor
├── entity/User.java                    — 新增 username、passwordHash 字段
├── entity/UserProfile.java             — 新增 avatarUrl 字段
├── controller/DietRecordController.java    — userId 从 request attribute 取
├── controller/NutritionController.java     — 同上
├── controller/HealthScoreController.java   — 同上
├── controller/RecommendationController.java — 同上
├── controller/FoodRecognitionController.java — 同上
├── controller/VoiceParseController.java     — 同上
├── controller/VoiceRecordController.java    — 同上
├── controller/MealPhotoController.java      — 同上
├── controller/UserProfileController.java    — 同上
├── controller/AlertRuleController.java      — 同上
├── repository/UserRepository.java           — 新增 findByUsername()
├── resources/init.sql                       — DDL 变更
├── resources/data.sql                       — 演示数据更新
```

---

## 四、前端设计

### 4.1 认证状态管理 — `src/auth.js`

```js
// 响应式全局状态
import { reactive } from 'vue'

const state = reactive({
  isLoggedIn: false,
  userId: null,
  username: '',
  nickname: '',
  avatarUrl: '',
  token: '',
})
```

方法：
- `init()` — App 启动时从 localStorage 恢复 token 和用户信息，直接信任本地 token。若 token 已失效，后续 API 调用返回 401 时再由响应拦截器处理登出
- `login(username, password)` — 调 API → 存 localStorage → 更新 state
- `register(username, password)` — 调 API → 存 localStorage → 更新 state
- `logout()` — 调 API → 清除 localStorage → 重置 state
- `uploadAvatar(file)` — 调 API → 更新 state.avatarUrl

### 4.2 路由守卫 — `router/index.js`

```js
router.beforeEach((to, from, next) => {
  // 不做页面级拦截，四个 tab 始终可访问
  // 仅确保 auth 模块已初始化
  next()
})
```

守卫不做拦截，检查登录态由各个页面组件内部处理。

### 4.3 Axios 拦截器 — `api/index.js`

- **请求拦截器**：如果 `auth.state.token` 存在，自动加 `Authorization: Bearer <token>` Header
- **响应拦截器**：捕获 401 → 调用 `auth.logout()` → 提示"登录已过期，请重新登录"

不再硬编码 `USER_ID = 1`，所有请求的 userId 由后端从 token 解析。

### 4.4 "我的"页面改造 — `ProfileView.vue`

#### 未登录状态

- 用户卡片显示灰色默认头像 👤 + "点击登录/注册"文字
- 点击卡片弹出登录/注册模态框
- 下方设置区域隐藏，显示占位提示："登录后可设置健康目标 · 个人资料 · 预警阈值"

#### 已登录状态

- 用户卡片显示真实头像（有 avatarUrl 用图片，否则默认渐变）+ 昵称 + 用户名 + 基本信息
- 头像右下角有 📷 相机图标，点击触发文件选择 → `auth.uploadAvatar(file)`
- 下方设置区域正常展示（与现有逻辑一致）
- 最底部（App info 上方）一个红色描边"退出登录"按钮，点击后二次确认

### 4.5 登录/注册模态框

底部弹出 sheet（bottom sheet），白色圆角卡片，背景半透明遮罩。

**登录模式**：
- 顶部 tab 切换："登录"（高亮绿色+下划线）/ "注册"（灰色）
- 表单：用户名输入框 + 密码输入框
- 绿色"登录"按钮
- 右上角 ✕ 关闭

**注册模式**：
- 顶部 tab 切换："登录"（灰色）/ "注册"（高亮绿色+下划线）
- 表单：用户名输入框 + 密码输入框 + 确认密码输入框
- 前后端双重校验：用户名≥2字符、密码≥6字符、两次密码一致
- 绿色"注册"按钮

### 4.6 其他页面的"请先登录"提示

RecordView / NutritionView / RecommendView 在用户触发操作（拍照、语音、保存、手动添加、提交反馈等）时，先检查 `auth.state.isLoggedIn`：

- 未登录 → 弹出轻提示 toast："请先在'我的'页面登录"
- 已登录 → 正常执行

提示实现：不引入额外库，用 Vue 响应式变量控制一段固定定位的 toast 文字，2 秒自动消失。

### 4.7 头像上传交互

- 点击相机图标 → 触发 `<input type="file" accept="image/*">`
- 选择图片后即时裁剪/预览？**不做裁剪**，直接上传原图
- 上传成功后立即刷新头像显示
- 上传失败 toast 提示"头像上传失败"

---

## 五、数据流

### 注册流程

```
前端: 填写用户名+密码+确认密码 → 点击注册
  → POST /api/auth/register {username, password}
  → 后端: 校验用户名唯一 → BCrypt 哈希密码 → INSERT users → INSERT user_profile
  → 生成 UUID token → 存入 ConcurrentHashMap
  → 返回 {userId, username, nickname, avatarUrl, token}
  → 前端: 存入 localStorage → 更新 auth.state
  → 关闭模态框 → "我的"页面刷新为已登录状态
```

### 登录流程

```
前端: 填写用户名+密码 → 点击登录
  → POST /api/auth/login {username, password}
  → 后端: 查 users 表 → BCrypt 验证密码
  → 成功 → 生成 UUID token → 存入 ConcurrentHashMap
  → 返回 {userId, username, nickname, avatarUrl, token}
  → 前端: 存入 localStorage → 更新 auth.state
  → 关闭模态框 → "我的"页面刷新为已登录状态
```

### 退出流程

```
前端: 点击"退出登录" → 确认对话框 → 确认
  → POST /api/auth/logout (带 token header)
  → 后端: Map 移除 token
  → 前端: 清除 localStorage → 重置 auth.state
  → "我的"页面刷新为未登录状态
```

### 操作拦截流程

```
用户点击拍照/保存等操作
  → 前端: 检查 auth.state.isLoggedIn
  → false → toast "请先在'我的'页面登录"
  → true → 正常执行业务逻辑（后端从 token 获取 userId）
```

---

## 六、兼容性

- 现有演示用户（id=1, nickname='健康达人'）赋予 `username='demo', password_hash=BCrypt('demo123')`
- 已有饮食记录、照片、语音等数据因 userId=1 属于 demo 用户，登录 demo 账号后继续可见
- 前端 auth.js 模块是新增文件，不影响现有页面结构

---

## 七、安全考虑

| 措施 | 说明 |
|------|------|
| 密码 BCrypt 哈希 | 数据库中不存储明文密码 |
| Token 随机 UUID | 不可猜测，防暴力枚举 |
| 客户端 userId 不可信 | 所有 userId 由后端从 token 解析，不接受客户端传入 |
| 文件类型白名单 | 头像仅允许 jpeg/png/gif/webp |
| 文件大小限制 | 头像 ≤5MB |
