# 推荐页完整重构 设计文档

> 日期：2026-06-21 | 状态：已批准

## 目标

将"推荐"页从纯规则引擎改造为 AI 驱动，基于用户口味偏好、alert_rule 阈值、当日饮食摄入缺口，从菜谱库中智能筛选并持久化推荐结果。

## 范围

### 包含
- 新建 50-100 道一人食菜谱库（从网络检索真实数据，包含完整营养数据）
- 后端：AI 推荐服务替换规则引擎，基于 DashScope 分析营养缺口 + 匹配菜谱
- 后端：推荐结果按天持久化，同一天同用户只有一份推荐（"换一批"覆盖）
- 后端：删除 like/dislike 反馈机制
- 前端：推荐卡片增强展示（营养对比条、个性化理由、食材预览）
- 前端：详情弹窗（完整食材、做法、营养全量）
- 前端："换一批"按钮触发重新生成

### 不包含
- like/dislike 反馈
- 推荐历史查看
- 菜谱库的管理后台（CRUD）

## 菜谱库

### 数据来源
从网络检索真实一人食菜谱数据，每道菜包含完整营养数据（热量、蛋白质、脂肪、碳水、糖分、钠、食材清单、做法步骤、标签）。

### 分类规划（50-100 道）

| 分类 | 数量 | 示例 |
|------|------|------|
| 减脂轻食 | 15-20 | 鸡胸肉沙拉、清蒸鲈鱼、凉拌黄瓜木耳、白灼西蓝花 |
| 高蛋白增肌 | 15-20 | 黑椒牛肉粒、虾仁滑蛋、香煎鸡腿排、蒜蓉蒸虾 |
| 快手小炒 | 20-25 | 番茄炒蛋、青椒肉丝、蒜蓉西兰花、肉末茄子 |
| 汤粥主食 | 10-15 | 皮蛋瘦肉粥、番茄蛋花汤、杂粮饭、紫菜蛋花汤 |
| 早餐简餐 | 10-15 | 燕麦牛奶、水煮蛋全麦面包、蔬菜鸡蛋饼、豆浆油条 |

### 数据库表
沿用现有 `recipe` 表，字段已完备：id, name, ingredients(TEXT), steps(TEXT), tags, calorie, protein, fat, carbohydrate, sugar, sodium。

### 初始化方式
编写 SQL 初始化脚本 `init_recipes.sql`，预装所有菜谱数据。

## 后端改造

### AI 推荐流程

```
GET /api/recommendations/today
  │
  ├── 1. 查询今天是否已有推荐？
  │     ├── 有 → 直接返回（跳过生成）
  │     └── 无 ↓
  │
  ├── 2. 收集上下文
  │     ├── UserProfile（目标、口味、禁忌、年龄/性别/身高/体重）
  │     ├── AlertRule（6项阈值：热量/糖/钠/蛋白/脂肪/碳水）
  │     └── 当日 DietRecord 汇总（已摄入的6项营养）
  │
  ├── 3. 计算营养缺口
  │     gap = threshold - intake（负值表示已超标）
  │
  ├── 4. 构造 AI prompt
  │     "用户画像：...，阈值：...，今日已摄入：...，缺口：...
  │      菜谱库：[完整菜谱列表含营养数据]
  │      从菜谱库中选出最合适的5道菜，优先填补营养缺口，
  │      避开用户禁忌，匹配口味偏好。返回JSON。"
  │
  ├── 5. 调用 DashScope（复用 ThresholdAnalysisAdapter 的配置）
  │
  ├── 6. 解析 AI 返回的 recipe IDs + 个性化理由
  │
  ├── 7. 持久化到 recommendation 表（覆盖当天已有记录）
  │
  └── 8. 返回 List<RecommendationVO>
```

### 接口变更

| 端点 | 变更 |
|------|------|
| `GET /api/recommendations/today` | 逻辑重写：AI 驱动、有缓存则直接返回 |
| `POST /api/recommendations/refresh` | **新增**：强制重新生成（"换一批"） |
| `POST /api/recommendations/feedback` | **删除** |

### 删除内容
- `RecommendationFeedbackCommand` DTO
- `saveFeedbackAndRefresh()` 方法
- `scoreRecipe()` 规则评分方法（替换为 AI 分析）
- `generateReason()` 规则理由方法（替换为 AI 生成）
- `ScoredRecipe` 内部记录类

### 新增内容
- `RecommendationService.recommendToday()` — 重写为 AI 驱动
- `RecommendationService.refreshToday()` — 强制重新生成
- `RecommendationAdapter` — AI 调用适配器（推荐专用 prompt 构造 + 响应解析）

### Recommendation 实体字段变更
- **删除** `feedback` 字段（不再需要 like/dislike）
- **保留** `id`, `userId`, `recipeId`, `reason`, `score`, `createdAt`

### RecommendationVO 字段（最终版）
- `id`, `recipeId`, `recipeName`, `ingredients`, `steps`, `tags`
- `calorie`, `protein`, `fat`, `carbohydrate`, `sugar`, `sodium` — 菜谱营养
- `reason` — AI 生成的个性化推荐理由
- `matchScore` — AI 给出的匹配度（保留用于排序展示，非规则引擎分数）

### AI Prompt 构造策略
- 菜谱库摘要传入 AI（每道菜：id, name, calorie, protein, fat, carb, sugar, sodium, tags），非完整食材/步骤
- AI 返回 recipe ID 列表 + 个性化理由，后端再根据 ID 查询完整 Recipe 数据填充 VO
- 预估 prompt token：~100 道菜 × ~120 字符/道 ≈ 12k 字符 ≈ 3-4k tokens，在 DashScope 上下文窗口内

### 推荐持久化
- 同一天同一用户只保留一份推荐（5 条记录）
- "换一批" 时先删除当天已有推荐，再生成新的

## 前端改造

### 页面布局（从上到下）

```
┌──────────────────────────────────┐
│  标题："今日推荐"    [换一批]     │
├──────────────────────────────────┤
│  ┌──────────────────────────┐    │
│  │ 🥩 黑椒牛肉粒    高蛋白   │    │
│  │ 热量 320/2000kcal        │    │
│  │ 蛋白 28g ████████░░ 60g │    │
│  │ 脂肪 12g ████░░░░░░ 65g │    │
│  │ ...                      │    │
│  │ 食材：牛肉、洋葱、青椒... │    │
│  │ 💡 补充今日蛋白质缺口15g  │    │
│  │              [查看详情]   │    │
│  └──────────────────────────┘    │
│  ┌──────────────────────────┐    │
│  │ ...（第2-5道）           │    │
│  └──────────────────────────┘    │
└──────────────────────────────────┘
```

### 卡片内容
- **菜名** + 分类标签
- **关键营养对比条**：热量 + 蛋白质 + 脂肪 + 碳水，每个显示"份量 vs 日阈值"的进度条
- **食材预览**：前 3-4 种食材
- **AI 推荐理由**：个性化文字（如"补充今日蛋白质缺口 15g，低脂符合减脂目标"）
- **"查看详情"**按钮 → 弹窗

### 详情弹窗
- 菜名 + 所有标签
- 完整营养数据（6 项：热量/蛋白质/脂肪/碳水/糖分/钠）
- 完整食材清单
- 做法步骤（按行分隔）
- AI 推荐理由

### "换一批"按钮
- 调用 `POST /api/recommendations/refresh`
- 后端删除当天推荐 → AI 重新生成 → 返回新的 5 道
- 按钮在加载中时禁用（loading 态）

### 删除内容
- 匹配度分数（matchScore）展示
- "踩"按钮（dislike）
- 相关的方法：`dislike()`, `matchColor()`

### 加载态
- 首次加载：骨架屏或"正在为您生成推荐..."文字
- 换一批：按钮 loading，列表保留当前内容不闪烁

## API 响应解包修复

前端 `RecommendView.vue` 的 `fetchRecommendations()` 和其他页面保持一致：
- `const res = await api.getRecommendations()`
- `recommendations.value = res.data?.data || []`（当前是 `res || []`，有 bug）

`api/index.js` 中 `getRecommendations()` 方法保持不变。

## 数据流

```
RecommendView.vue
  │  onMounted → fetchRecommendations()
  │  "换一批"  → refreshRecommendations()
  │
  ▼
api/index.js
  ├── getRecommendations()    → GET  /api/recommendations/today
  └── refreshRecommendations() → POST /api/recommendations/refresh
  │
  ▼
RecommendationController
  ├── getToday()      → recommendationService.recommendToday(userId)
  └── refreshToday()  → recommendationService.refreshToday(userId)
  │
  ▼
RecommendationService
  ├── 检查今日缓存 → 有则返回
  ├── 收集 UserProfile + AlertRule + DietRecord
  ├── 计算营养缺口
  ├── 加载菜谱库
  ├── 构造 prompt → RecommendationAdapter.analyze()
  ├── 解析 AI 结果 → 持久化 → 返回 VO
  │
  ▼
RecommendationAdapter (新增)
  ├── 构造推荐专用 prompt
  ├── 调用 DashScope API（复用 DashScopeConfig）
  └── 解析 AI 返回的 recipe IDs + 理由

```

## 错误处理

| 场景 | 处理 |
|------|------|
| 无用户画像 | 返回错误提示"请先完善个人资料" |
| 无 alert_rule | 使用 profile.goal 默认阈值（与 HealthScoreService 一致） |
| 无当日饮食记录 | 正常推荐，营养缺口 = 阈值全额（首次使用场景） |
| AI 调用失败 | 降级为规则引擎（保留旧 `scoreRecipe` 逻辑作为 fallback） |
| 菜谱库为空 | 返回空列表 + 提示"菜谱库暂未初始化" |

## 文件变更清单

### 后端（修改）
- `RecommendationService.java` — 重写核心逻辑
- `RecommendationController.java` — 新增 refresh 端点、删除 feedback 端点
- `RecommendationVO.java` — 新增 `steps`, `sugar`, `sodium` 字段
- `Recommendation.java` — 删除 `feedback` 字段

### 后端（新增）
- `RecommendationAdapter.java` — AI 推荐适配器

### 后端（删除）
- `RecommendationFeedbackCommand.java` — 不再需要

### 数据库
- `init_recipes.sql` — 菜谱库初始化脚本（50-100 道）
- `recommendation` 表 — 删除 `feedback` 列（ALTER TABLE DROP COLUMN）

### 前端（修改）
- `RecommendView.vue` — 完整重写
- `api/index.js` — 新增 `refreshRecommendations()`、删除 `submitFeedback()`

## 验收条件

- [ ] 打开推荐页，AI 自动根据当日饮食缺口生成 5 道推荐
- [ ] 每道推荐展示营养对比条 + AI 个性化理由
- [ ] 点击"查看详情"弹出完整菜谱信息
- [ ] 点击"换一批"重新生成，旧推荐被替换
- [ ] 同一天多次打开推荐页，不会重复调用 AI（缓存生效）
- [ ] 无用户画像时给出明确错误提示
- [ ] 无当日饮食记录时正常推荐（使用阈值作为缺口）
- [ ] AI 调用失败时降级为规则引擎
