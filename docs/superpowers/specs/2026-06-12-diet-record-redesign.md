# 饮食记录数据库重构 + 首页交互重设计

## 概述

将 `diet_record` 表改造为独立完整的营养快照记录，不依赖 `food_item` 表的 JOIN 计算。同时重构前端首页的交互，改为按餐次手风琴折叠展示 + 照片滑动浏览。

---

## 一、数据库改动

### 1.1 `diet_record` 表新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `calorie` | DECIMAL(8,2) | 该条记录的热量(kcal) |
| `protein` | DECIMAL(8,2) | 蛋白质(g) |
| `fat` | DECIMAL(8,2) | 脂肪(g) |
| `carbohydrate` | DECIMAL(8,2) | 碳水(g) |
| `sugar` | DECIMAL(8,2) | 糖(g) |
| `sodium` | DECIMAL(8,2) | 钠(mg) |

写入逻辑：
- AI 识别/分析返回营养数据时，优先使用 AI 的值
- AI 未返回时，若 `food_id` 匹配到 `food_item`，按 `amount / 100 × 每100g营养` 计算写入

### 1.2 餐次类型扩展

`早餐/午餐/晚餐/加餐` → `早餐/午餐/晚餐/夜宵/其他`

### 1.3 `nutrition_record` 表不变

结构不变。每日营养汇总改为直接 SUM `diet_record` 表当天记录的营养字段。

---

## 二、后端改动

### 2.1 Entity

- `DietRecord.java` — 新增 6 个营养字段 + getter/setter

### 2.2 DTO

- `DietRecordVO.java` — 新增 `sugar`, `sodium` 字段
- `DietRecordCreateCommand.java` — 新增 6 个可选营养字段

### 2.3 Service

- `DietRecordService.create()` — 写入营养字段（优先 AI 值，兜底 food_item 计算）
- `DietRecordService.toVO()` — 从 entity 直接取营养值
- `NutritionService.getDaily()` — SUM diet_record 表代替 JOIN food_item

### 2.4 Repository

- `DietRecordRepository` — 新增聚合查询 `sumNutritionByUserAndDate()`

### 2.5 Config

- 新增静态资源映射 `/api/uploads/**` → `backend/uploads/`

### 2.6 照片存储

- 路径：`backend/uploads/diet-images/{yyyy}/{MM}/{dd}/{uuid6}_{foodName}.jpg`
- 数据库 `image_url` 存相对路径

---

## 三、前端改动

### 3.1 首页布局（RecordView.vue）

```
┌──────────────────────────────┐
│ 📷拍照  🎤语音  ✏️手动       │  快捷按钮
└──────────────────────────────┘
┌──────────────────────────────┐
│ 📊 今日摄入总览              │  6项汇总
│ 🔥1500 🥩80 🧈45 🌾200      │
│ 🍬30 🧂1800                  │
└──────────────────────────────┘
┌──────────────────────────────┐
│ 🍚 午餐         🔥800kcal ▴  │  手风琴-展开
│ ← [照片1] [照片2] →         │  可滑动照片区
│ ───────────────────────────  │
│ 🍗鸡腿 1个 拍照 🔥181 🥩20  │  食物条目
│ 🍚米饭 1碗 手动 🔥116 🥩2.6 │
│          [编辑] [删除]       │
└──────────────────────────────┘
┌──────────────────────────────┐
│ 🍳 早餐         🔥500kcal ▾  │  手风琴-折叠
└──────────────────────────────┘
```

交互规则：
- 折叠状态显示：餐次名 + 该餐热量/蛋白质合计
- 展开状态显示：照片滑动区 + 每条食物详情（6项营养 + 编辑/删除按钮）
- 点击缩略图弹出全屏大图预览
- 无照片的餐次不显示照片区域
- 无记录的餐次不显示整块卡片

### 3.2 其他文件

- `api/index.js` — create 请求携带营养数据
- `App.vue` — 全局遮罩样式（照片预览）

---

## 四、文件改动清单

| 文件 | 操作 |
|------|------|
| `entity/DietRecord.java` | 新增 6 字段 + getter/setter |
| `dto/command/DietRecordCreateCommand.java` | 新增 6 可选营养字段 |
| `dto/vo/DietRecordVO.java` | 新增 sugar、sodium |
| `service/DietRecordService.java` | create/toVO 改写 |
| `service/NutritionService.java` | SUM 替代 JOIN |
| `repository/DietRecordRepository.java` | 新增 SUM 聚合查询 |
| `config/WebConfig.java`（新建） | 静态资源映射 |
| `resources/data.sql` | 更新演示数据 |
| `views/RecordView.vue` | 手风琴布局重构 |
| `api/index.js` | 请求携带营养数据 |
