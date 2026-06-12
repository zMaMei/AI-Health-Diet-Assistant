# 餐次照片存储 — 数据库与逻辑重构

## 概述

新增 `meal_photo` 表，将照片从"关联单条食物记录"改为"关联某一餐"（user_id + record_date + meal_type），支持一餐多张照片。同时修复后端"拍照识别后未保存照片"的缺失逻辑。

---

## 一、新增 `meal_photo` 表

```sql
CREATE TABLE meal_photo (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    record_date DATE         NOT NULL,
    meal_type   VARCHAR(16)  NOT NULL,   -- 早餐/午餐/晚餐/夜宵/其他
    image_url   VARCHAR(255) NOT NULL,   -- 相对路径
    created_at  DATETIME     DEFAULT NOW()
);
```

主键 `id`，业务查询走 `(user_id, record_date, meal_type)` 复合索引。

---

## 二、新增/修改文件清单

### 后端新建（6个文件）

| 文件 | 说明 |
|------|------|
| `entity/MealPhoto.java` | JPA 实体 |
| `repository/MealPhotoRepository.java` | 数据访问 |
| `dto/command/MealPhotoCreateCommand.java` | 新增照片请求体 |
| `dto/vo/MealPhotoVO.java` | 照片响应体 |
| `controller/MealPhotoController.java` | CRUD 接口 |
| `service/MealPhotoService.java` | 业务逻辑（写入记录 + 保存文件 + 删除文件） |

### 后端修改（3个文件）

| 文件 | 改动 |
|------|------|
| `controller/FoodRecognitionController.java` | 识别后保存图片到磁盘，返回 imageUrl |
| `service/FoodRecognitionService.java` | 新增 saveImage() 方法 |
| `resources/init.sql` + `data.sql` | 新增 meal_photo 建表 |

### 前端修改（2个文件）

| 文件 | 改动 |
|------|------|
| `api/index.js` | 新增 uploadMealPhoto / getMealPhotos / deleteMealPhoto |
| `views/RecordView.vue` | saveFromPhoto 后上传照片；mealPhotos 改为调 API；fetchData 并行拉照片 |

---

## 三、API 设计

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/meal-photos` | POST | `{userId, recordDate, mealType, imageUrl}` → 新增记录 |
| `/api/meal-photos?userId=&date=` | GET | 查询某日所有照片 |
| `/api/meal-photos?userId=&date=&mealType=` | GET | 查询某日某餐照片 |
| `/api/meal-photos/{id}` | DELETE | 删除记录+磁盘文件 |

---

## 四、完整流程

```
拍照 → POST /api/food/recognize
        后端: ①保存照片到 uploads/diet-images/{yyyy}/{MM}/{dd}/{uuid}.jpg
              ②AI 识别
              ③返回 candidates + imageUrl
              ↓
确认保存 → POST /api/diet-records × N（已有）
         → POST /api/meal-photos（新增）
              ↓
首页加载 → GET /api/meal-photos → 按餐次展示照片滑动区
```

---

## 五、对 `diet_record.image_url` 的处理

字段保留不动（已有数据兼容），新逻辑不再写入该字段。
