# AI智能个人健康饮食助手

**小组：** 21小组
**成员：** 屈鹏程, 叶志汉, 许志杰, 金岸

---

## 项目简介

AI智能个人健康饮食助手是一款面向普通用户的移动端 H5 健康饮食管理应用。系统通过**拍照识别、语音输入、手动编辑**三种方式采集饮食数据，结合阿里云百炼（DashScope）通义千问多模态 AI 大模型自动估算食物营养成分，并根据用户个性化的健康目标（均衡/减脂/增肌/控糖）生成每日**健康评分、改进建议、个性化食谱推荐和超标预警**。

### 核心价值

- **降低记录门槛**：拍照/语音代替手动输入，AI 自动识别食物并估算营养，三步完成记录
- **数据驱动洞察**：将饮食数据转化为可视化营养分析、健康评分和趋势图表
- **个性化体验**：根据健康目标、口味偏好、忌口、慢性病定制推荐和建议
- **AI 深度对话**：基于当日饮食数据的 AI 营养分析聊天，提供专业级饮食反馈
- **风险主动预警**：超标时主动提醒，支持 AI 智能分析生成个性化阈值

### 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| 开发语言 | Java | 17 |
| ORM | Spring Data JPA (Hibernate) | — |
| 数据库 | MySQL | 8.x |
| 构建工具 | Maven | 3.9+ |
| 前端框架 | Vue 3 (Composition API) | 3.x |
| 构建工具 | Vite | 8.x |
| 前端路由 | Vue Router | 4.x |
| HTTP 客户端 | Axios | — |
| 图片裁剪 | Cropper.js | — |
| AI 平台 | 阿里云百炼 DashScope | qwen-omni-turbo |

### 项目结构

```
├── backend/                         # 后端 Spring Boot 项目
│   ├── pom.xml
│   ├── scripts/                     # 数据管理脚本（导入/清理/重算）
│   └── src/main/java/com/health/diet/
│       ├── DietApplication.java     # 启动类
│       ├── adapter/                 # AI 适配器（6个）
│       │   ├── ImageRecognitionAdapter.java     # 图片食物识别
│       │   ├── SpeechToTextAdapter.java         # 语音转文字
│       │   ├── FoodEntityParserAdapter.java     # 食物实体+营养解析
│       │   ├── RecommendationAdapter.java       # AI 食谱推荐/创造
│       │   ├── ThresholdAnalysisAdapter.java    # AI 个性化阈值生成
│       │   └── NotificationAdapter.java         # 消息推送（模拟）
│       ├── common/                  # 公共组件
│       │   ├── ApiResponse.java     # 统一响应封装
│       │   └── GlobalExceptionHandler.java
│       ├── config/                  # 配置
│       │   ├── CorsConfig.java      # 跨域配置
│       │   ├── DashScopeConfig.java # AI API 配置
│       │   ├── WebMvcConfig.java    # 静态资源映射+拦截器注册
│       │   └── LoginInterceptor.java # Token 认证拦截器
│       ├── controller/              # 控制器（12个）
│       │   ├── AuthController.java              # 登录/注册/登出/头像
│       │   ├── UserProfileController.java       # 用户档案
│       │   ├── DietRecordController.java        # 饮食记录 CRUD
│       │   ├── MealPhotoController.java         # 餐次照片管理
│       │   ├── VoiceParseController.java        # 语音解析上传
│       │   ├── VoiceRecordController.java       # 语音记录管理
│       │   ├── FoodRecognitionController.java   # 食物图片识别+文本分析
│       │   ├── NutritionController.java         # 每日营养汇总
│       │   ├── HealthScoreController.java       # 健康评分
│       │   ├── AiAnalysisController.java        # AI 饮食对话分析
│       │   ├── AlertRuleController.java         # 预警规则+AI阈值分析
│       │   └── RecommendationController.java    # 食谱推荐+刷新
│       ├── dto/command/             # 请求 DTO
│       ├── dto/vo/                  # 响应 VO
│       ├── entity/                  # JPA 实体（12个）
│       ├── repository/              # 数据访问层
│       ├── service/                 # 业务服务（11个）
│       └── resources/
│           ├── application.yml
│           ├── data.sql             # 演示数据
│           └── init.sql             # 完整建表脚本
│
├── frontend/                        # 前端 Vue 3 项目
│   ├── vite.config.js
│   ├── package.json
│   └── src/
│       ├── main.js                  # 入口（含 auth.init 恢复登录态）
│       ├── App.vue                  # 主框架（顶栏+底部Tab+全局Toast）
│       ├── auth.js                  # 登录态管理（reactive + localStorage）
│       ├── toast.js                 # 全局 Toast 通知
│       ├── api/index.js             # API 封装（含 Token 拦截+401自动退出）
│       ├── router/index.js          # 路由配置
│       └── views/                   # 页面组件
│           ├── RecordView.vue       # 记录页（拍照/语音/手动+手风琴展示）
│           ├── NutritionView.vue    # 分析页（营养分析+评分+AI对话）
│           ├── RecommendView.vue    # 推荐页（AI推荐卡片+详情弹窗+换一批）
│           └── ProfileView.vue      # 我的页（登录/注册/头像裁剪/设置）
│
└── README.md
```

---

## 功能模块

### 模块 1：用户认证

- **注册**：用户名+密码注册，BCrypt 哈希存储，注册后自动登录并创建初始档案和三条默认预警规则
- **登录**：用户名+密码登录，返回 UUID Token（内存 ConcurrentHashMap 管理），localStorage 持久化，重启 App 自动恢复
- **登出**：清除服务端 Token + 本地状态，二次确认
- **头像管理**：点击头像弹出 Cropper.js 裁剪模态框，支持拖拽定位、滚轮缩放、重新选图，Canvas 裁剪 256×256 后上传
- **未登录保护**：未登录时"我的"页设置区隐藏，其他页面操作按钮触发 toast 提示；API 返回 401 自动登出

### 模块 2：饮食数据采集（记录页）

支持三种采集入口：

**① 拍照识别食物**
- 调用浏览器摄像头拍照或选择图片上传至后端（保存到 `uploads/diet-images/`）
- 后端调用通义千问多模态 API 识别食物，返回候选食物列表（含食物名称、置信度、6 项营养估算值）
- 用户勾选要保存的食物、调整份量、选择统一餐次后批量保存
- 照片通过 `meal_photo` 表按餐次关联，支持一餐多张照片横向滑动预览、点击大图查看
- 低置信度或未识别时提示用户改用手动添加

**② 语音输入饮食记录**
- 调用浏览器麦克风实时录音（MediaRecorder API），显示录音计时
- 停止后自动上传 .webm 音频文件至后端（保存到 `uploads/voice/`）
- 后端调用通义千问多模态 API 进行语音转文字 + 食物实体解析（含营养估算）
- 解析结果写入 `voice_record` 表（含音频路径、转写文本、食物实体 JSON、时长）
- 前端展示与拍照一致的交互：勾选食物、查看营养、调整份量、选择统一餐次
- 语音记录关联到对应餐次卡片，支持 ▶️ 播放录音和查看转写文本

**③ 手动添加**
- 输入食物名称，点击「智能分析」调用 AI 获取营养数据后保存按钮才可用
- 适用于 AI 识别失败或用户需要补记的场景

**记录展示（手风琴布局）**
- 按餐次（早餐/午餐/晚餐/夜宵/其他）分组，折叠显示可折叠卡片
- 折叠状态：显示餐次图标+名称、该餐热量/蛋白质合计
- 展开状态依次展示：照片横向滑动区 → 语音记录区 → 食物列表
- 每餐食物列表含：名称、份量、来源标签（拍照/语音/手动）、6 项营养值、编辑/删除按钮
- 顶部今日总览卡片：6 项营养当日总和

### 模块 3：智能营养分析与健康评分（分析页）

本页面合并了营养分析、健康评分和 AI 饮食对话三大功能：

**日期导航**
- 日期选择器支持前后切换和历史日期回顾（不可选未来日期），附「今天」快捷按钮

**当日饮食回顾**
- 同记录页手风琴展示各餐次记录，包括照片轮播、语音播放、食物条目，支持编辑和删除

**健康评分**
- SVG 环形进度图展示 0-100 评分（当日记录不足 2 餐不评分）
- 评分逻辑：基础分 80 + 热量超标扣分 + 糖/钠超标扣分 + 蛋白质达标加分
- ✅ 优点列表（绿色标签）和 ⚠️ 风险项列表（橙色标签）

**营养分析**
- 热量/蛋白质/脂肪/碳水四大指标，展示摄入量、目标值、完成百分比环形进度
- 点击营养素查看当日该营养素的食物来源明细弹窗
- 近一周热量趋势柱状图（当日高亮），便于观察饮食趋势
- 自动生成饮食改进建议

**目标体系**

| 目标 | 热量 | 蛋白质 | 脂肪 | 碳水 |
|------|------|--------|------|------|
| 均衡 | 2000 | 60g | 65g | 300g |
| 减脂 | 1600 | 70g | 50g | 200g |
| 增肌 | 2500 | 120g | 70g | 350g |

**AI 饮食对话**
- 以弹窗形式展示 AI 对话界面，支持与通义千问进行当日饮食分析对话
- 预设「开始分析」按钮发送初始分析提示（自动附带用户档案、近 7 天饮食记录和营养汇总作为上下文）
- 支持自由提问、多轮对话，消息历史持久化到数据库
- 支持简单 Markdown 渲染（粗体、换行），消息区域自动滚动至底部

### 模块 4：个性化食谱推荐（推荐页）

**推荐机制（AI 优先，规则引擎降级）**
1. **AI 推荐**（优先）：调用通义千问多模态 API，根据用户营养缺口、目标、口味偏好、忌口，从菜谱库中选择 5 道最合适的菜谱并生成推荐理由，或直接创造全新菜谱
2. **规则引擎**（降级）：AI 不可用时启用
   - 过滤：排除忌口菜谱和历史"不喜欢"菜谱
   - 评分（0-100）：基础分 50 + 目标匹配（±20）+ 口味偏好匹配（+5/项）+ 标签匹配（+15）
   - 高分段随机选取 5 道，增加多样性

**交互功能**
- 推荐卡片展示：菜名、标签（绿色药丸）、营养对比条（热量/蛋白质/脂肪/碳水 vs 用户阈值）、食材预览、推荐理由
- **换一批**：调用 AI 直接创造全新菜谱（不限菜谱库），带旋转加载动画
- **查看详情弹窗**：完整营养数值（含糖/钠）、完整食材列表、分步骤做法、推荐理由
- 营养对比条使用用户自定义阈值，自动适配个性化目标

### 模块 5：饮食提醒与预警

**预警检测**
- 每次查看推荐页或调用 `/api/alert-rules/check` 时自动检测
- 当日累计摄入 > 用户设置的阈值 → 生成预警消息（记录页顶部红色左边框卡片展示）

**预警规则管理**
- 支持 6 项营养素：热量、糖分、钠、蛋白质、脂肪、碳水
- 每项独立开关，修改数值自动保存
- 注册时自动创建热量/糖分/钠三条默认预警规则

**AI 智能阈值分析**
- 「AI 智能分析」按钮：根据用户年龄、性别、身高、体重、BMI、健康目标、慢性病等信息
- 调用通义千问生成个性化营养摄入上限建议，自动更新预警规则

### 模块 6：健康档案与设置（我的页）

- **个人资料**：头像（可裁剪上传）、昵称（点击编辑）、年龄、身高、体重、性别
- **健康目标**：均衡饮食 / 减脂 / 增肌 / 控糖（下拉选择）
- **口味偏好**：多选标签（清淡、中式、西式、日式、辣味、酸甜、咸鲜）+ 自定义标签
- **忌口设置**：多选标签（海鲜、花生、牛奶、鸡蛋、豆制品、辛辣、芝麻、芒果、酒精等）+ 自定义标签，支持行内编辑和删除
- **慢性病/特殊饮食**：预设标签（糖尿病、高血压、高血脂、痛风）+ 自定义标签
- **预警阈值**：6 项营养素的独立开关和数值设置，支持 AI 智能分析一键生成
- 所有设置实时影响推荐算法和预警检测

---

## 数据库设计

系统共 **12 张核心数据表**：

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| `users` | 用户基础信息（含登录凭据） | id, username (唯一), nickname, password_hash, created_at |
| `user_profile` | 用户健康档案 | user_id, age, height_cm, weight_kg, goal, taboo, taste_preference, warning_profile, gender, avatar_url |
| `food_item` | 食物营养成分库（每100g基线，20种） | name, category, unit, calorie, protein, fat, carbohydrate, sugar, sodium |
| `diet_record` | 饮食记录（含营养快照） | user_id, food_id, food_name, meal_type, amount, source, calorie～sodium (6项) |
| `nutrition_record` | 每日营养汇总 | user_id, record_date, calorie_total～sodium_total, score |
| `meal_photo` | 餐次照片（一餐多张） | user_id, record_date, meal_type, image_url |
| `voice_record` | 语音录音记录 | user_id, record_date, meal_type, audio_url, transcribed_text, food_entities, duration_seconds |
| `recipe` | 食谱库（65道一人食菜谱） | name, ingredients, steps, tags, calorie～sodium |
| `recommendation` | 推荐记录 | user_id, recipe_id, reason, score, created_at |
| `alert_rule` | 预警阈值规则 | user_id, nutrient_type, threshold, enabled |
| `ai_conversation` | AI 对话会话 | user_id, record_date, created_at |
| `ai_message` | AI 对话消息 | conversation_id, role (USER/AI), content, created_at |

**设计要点：**
- 营养数据以 **AI 返回值为准**，`food_item` 库仅补全 unit/category 及 AI 失败时兜底
- `diet_record` 存储 6 项营养快照，每日汇总和评分直接 SUM 该表的营养字段
- `meal_photo` 按 `(user_id, record_date, meal_type)` 关联餐次，一餐可有多张照片
- `voice_record` 支持语音→转写→食物解析完整链路，`meal_type` 在用户确认后回填
- 上传文件组织为 `uploads/{userId}/{YYYY}/{MM}/{DD}/{UUID}.ext`，便于管理和清理
- 食谱库覆盖减脂、增肌、小炒、汤粥、早餐五大类 65 道一人食菜谱
- `ai_conversation` 和 `ai_message` 记录 AI 饮食对话历史，按日期组织

---

## API 接口

所有接口统一返回 `ApiResponse<T>` 格式：
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

认证方式：Bearer Token（UUID），未登录接口除外。Token 由 `LoginInterceptor` 从 `Authorization` 请求头提取并注入 `userId`。

### 认证 Auth

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/auth/register` | POST | — | 注册新用户（自动登录，返回 token + 用户信息） |
| `/api/auth/login` | POST | — | 用户名+密码登录，返回 token + 用户信息 |
| `/api/auth/logout` | POST | Bearer | 退出登录，服务端清除 token |
| `/api/auth/avatar` | POST | Bearer | 上传/更新头像（multipart/form-data） |

### 饮食记录 Diet Records

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/diet-records` | POST | Bearer | 新增饮食记录（含营养快照，AI 值优先） |
| `/api/diet-records` | GET | Bearer | 查询指定日期饮食记录列表（按餐次分组） |
| `/api/diet-records/{id}` | PUT | Bearer | 修改饮食记录（校验所有权） |
| `/api/diet-records/{id}` | DELETE | Bearer | 删除饮食记录（校验所有权） |

### 食物识别 Food

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/food/recognize` | POST | Bearer | 上传图片 → AI 多模态识别食物 + 营养估算 → 返回候选列表（含 imageUrl） |
| `/api/food/analyze-text` | POST | Bearer | 对食物名称文本分析 → AI 估算营养值（用于手动添加"智能分析"） |

### 语音解析 Voice

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/voice/parse` | POST | Bearer | 上传录音文件 → 保存 → 语音转文字 → 食物实体解析 → 返回结果 |
| `/api/voice-records` | GET | Bearer | 查询指定日期语音记录列表 |
| `/api/voice-records/{id}/meal-type` | PUT | Bearer | 回填语音记录的餐次类型（用户确认后，校验所有权） |
| `/api/voice-records/{id}` | DELETE | Bearer | 删除语音记录及其音频文件（校验所有权） |

### 餐次照片 Meal Photos

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/meal-photos` | POST | Bearer | 新增餐次照片记录（从临时目录移动到最终目录） |
| `/api/meal-photos` | GET | Bearer | 按日期（及可选 mealType）查询餐次照片列表 |
| `/api/meal-photos/{id}` | DELETE | Bearer | 删除照片记录及磁盘文件（校验所有权） |

### 营养分析 Nutrition

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/nutrition/daily` | GET | Bearer | 获取指定日期营养汇总（摄入量、目标阈值、周趋势柱状图数据、建议） |

### 健康评分 Health Score

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/health-score/daily` | GET | Bearer | 获取指定日期健康评分（0-100，含优缺点、风险项、建议、周趋势） |

### AI 饮食对话 AI Analysis

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/ai/analyze-diet` | POST | Bearer | 发送消息给 AI 进行饮食分析（含用户档案+近7天记录上下文），保存对话历史 |
| `/api/ai/conversation` | GET | Bearer | 获取指定日期的 AI 对话历史（消息列表） |

### 食谱推荐 Recommendations

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/recommendations/today` | GET | Bearer | 获取今日推荐（含缓存结果、用户阈值、营养缺口，AI 优先/规则引擎降级） |
| `/api/recommendations/refresh` | POST | Bearer | 强制刷新今日推荐（清除缓存，AI 创造全新菜谱或重新选择） |

### 预警规则 Alert Rules

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/alert-rules` | POST | Bearer | 创建预警规则（nutrient_type + threshold + enabled） |
| `/api/alert-rules` | GET | Bearer | 查询当前用户所有预警规则列表 |
| `/api/alert-rules/{ruleId}` | PUT | Bearer | 修改预警规则（阈值/enabled，校验所有权） |
| `/api/alert-rules/check` | GET | Bearer | 检查指定日期摄入是否超过规则阈值，返回预警消息列表 |
| `/api/alert-rules/analyze` | POST | Bearer | AI 分析用户档案 → 生成/更新个性化阈值规则 |

### 用户档案 User Profile

| 接口 | 方法 | 认证 | 说明 |
|------|------|------|------|
| `/api/user-profile` | GET | Bearer | 查询当前用户健康档案 |
| `/api/user-profile` | PUT | Bearer | 更新当前用户健康档案 |

---

## AI 能力集成

系统已接入**阿里云百炼（DashScope）**平台，通过 `qwen-omni-turbo` 多模态模型提供真实 AI 能力：

| AI 能力 | 适配器类 | 功能说明 |
|---------|---------|---------|
| 图像识别 | `ImageRecognitionAdapter` | 传入食物图片 → AI 识别食物名称 + 估算 6 项营养成分（复合菜品不拆分） |
| 食物名称分析 | `ImageRecognitionAdapter.analyzeFoodByName()` | 传入食物名 → AI 估算营养（手动添加的"智能分析"） |
| 语音转文字 | `SpeechToTextAdapter` | 传入 .webm 音频 → qwen-omni-turbo 多模态转写为中文文本 |
| 食物实体解析 | `FoodEntityParserAdapter` | 传入文本 → AI 提取结构化食物实体列表（名称、份量、营养估算） |
| 食谱推荐 | `RecommendationAdapter` | 根据营养缺口/目标/偏好 → 从库中选最佳 5 道或直接创造全新菜谱 |
| 阈值分析 | `ThresholdAnalysisAdapter` | 根据用户档案（年龄/BMI/目标/慢性病）→ AI 生成个性化营养摄入上限 |
| 消息推送 | `NotificationAdapter` | 预警消息推送（当前模拟实现） |

**设计原则：** 适配器模式封装第三方差异，替换实现类即可切换 AI 服务提供商，业务层代码无需修改。所有 AI 调用均包含 JSON 格式约束的 Prompt，后端解析结构化数据。AI 不可用时自动降级为规则引擎或本地兜底逻辑。

---

## 快速开始

### 环境要求

- JDK 17+、Maven 3.9+
- Node.js 18+、npm 9+
- MySQL 8.x

### 启动步骤

```bash
# 1. 配置数据库
# 修改 backend/src/main/resources/application.yml 中的数据库连接信息
# 执行 backend/src/main/resources/init.sql 建表

# 2. 启动后端（终端 1）
cd backend
mvn spring-boot:run

# 3. 启动前端（终端 2）
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`，按 F12 切换到移动端视图。演示账号：`demo` / `demo123`。

---

## 设计原则

| 原则 | 体现 |
|------|------|
| **前后端分离** | RESTful API + 统一 ApiResponse 格式，Vite 代理转发 |
| **模块化** | 按数据采集、营养分析、推荐、预警、档案拆分功能模块 |
| **单一职责** | Controller 仅处理请求/响应，Service 处理业务逻辑，Repository 负责持久化 |
| **适配器模式** | AI 适配器封装第三方服务差异，支持服务商替换 |
| **用户可控** | AI 识别结果必须经用户确认才能落库，支持修改和删除 |
| **数据最小化** | 仅采集课程演示必要数据，不涉及实名/支付/医疗等高敏信息 |
| **降级保护** | AI 不可用时自动降级为规则引擎或本地兜底，保证服务可用 |
| **所有权校验** | 所有涉及用户数据的修改/删除操作验证数据归属，防止越权 |
| **异常处理** | 统一异常处理 + 中文友好提示 |
