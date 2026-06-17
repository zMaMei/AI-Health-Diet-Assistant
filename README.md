# AI智能个人健康饮食助手

**小组：** 21小组
**成员：** 屈鹏程, 叶志汉, 许志杰, 金岸

## 项目简介

AI智能个人健康饮食助手是一款面向普通用户的移动端健康饮食管理应用。系统通过拍照识别、语音输入和手动编辑等方式采集饮食数据，结合食物营养成分库计算每日摄入情况，并根据用户健康目标生成饮食评分、改进建议、个性化食谱推荐和超标预警。

## 技术栈

- **后端：** Java 17 + Spring Boot 3.2 + Spring Data JPA + H2/MySQL
- **前端：** Vue 3 + Vite + Vue Router + Axios
- **数据库：** MySQL 8.x（可选）

## 项目结构

```
├── backend/                    # 后端 Spring Boot 项目
│   ├── pom.xml
│   └── src/main/java/com/health/diet/
│       ├── adapter/            # AI 适配器（模拟实现）
│       ├── common/             # 公共响应类和异常处理
│       ├── config/             # CORS 等配置
│       ├── controller/         # RESTful API 控制器
│       ├── dto/                # 数据传输对象
│       ├── entity/             # JPA 实体类
│       ├── repository/         # 数据访问层
│       └── service/            # 业务逻辑层
├── frontend/                   # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/                # API 调用层
│   │   ├── router/             # 路由配置
│   │   └── views/              # 页面组件
│   └── ...
└── README.md
```

## 功能模块

1. **饮食数据采集**：拍照识别食物、语音输入饮食记录、手动添加
2. **智能营养分析**：热量/蛋白质/脂肪/碳水计算、周趋势、目标完成度
3. **健康评分与建议**：每日评分（0-100分）、风险项说明、改进建议
4. **个性化推荐**：每日推荐3-5道菜、不喜欢替换
5. **提醒与预警**：热量/糖/钠阈值设置、超标提醒
6. **健康档案**：健康目标、口味偏好、忌口、预警阈值设置

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/food/recognize | POST | 上传图片并返回食物识别候选结果 |
| /api/voice/parse | POST | 上传语音并返回转写结果与食物实体 |
| /api/diet-records | POST/GET/PUT/DELETE | 饮食记录增删改查 |
| /api/nutrition/daily | GET | 查询指定日期营养汇总与趋势 |
| /api/health-score/daily | GET | 查询健康评分、风险和建议 |
| /api/recommendations/today | GET | 获取今日食谱推荐 |
| /api/recommendations/feedback | POST | 提交推荐反馈 |
| /api/alert-rules | POST/GET/PUT | 维护预警阈值 |
| /api/user-profile | GET/PUT | 查询和维护健康档案 |
