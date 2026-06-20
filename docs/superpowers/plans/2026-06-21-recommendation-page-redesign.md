# 推荐页完整重构 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将推荐页从规则引擎改造为 AI 驱动，基于用户阈值+当日摄入缺口从菜谱库智能推荐。

**Architecture:** DashScope AI 分析用户营养缺口 → 从菜谱库匹配 → 持久化 → 前端卡片展示。降级策略：AI 失败时回退规则引擎。

**Tech Stack:** Java 17, Spring Boot 3.2, JPA, DashScope (OpenAI 兼容 textUrl), Vue 3, Axios

## Global Constraints

- 所有前端 API 响应必须用 `res.data?.data` 解包（与其他页面一致）
- 菜谱库 50-100 道一人食菜谱，覆盖 5 个分类
- 推荐结果按天缓存：同一天同用户只有一份推荐
- "换一批"覆盖当天已有推荐（先删后生成）
- 删除 like/dislike 反馈，删除 RecommendationFeedbackCommand
- AI 调用失败时降级为旧规则引擎
- Recipe 实体需新增 sugar/sodium 字段
- Recommendation 实体移除 feedback 字段
- 无用户画像时返回明确错误提示

---

### Task 1: 数据库 Schema + Java Entity 变更

**Files:**
- Modify: `backend/src/main/java/com/health/diet/entity/Recipe.java`
- Modify: `backend/src/main/java/com/health/diet/entity/Recommendation.java`
- Modify: `backend/src/main/java/com/health/diet/repository/RecommendationRepository.java`
- Modify: `backend/src/main/java/com/health/diet/dto/vo/RecommendationVO.java`
- Delete: `backend/src/main/java/com/health/diet/dto/command/RecommendationFeedbackCommand.java`
- Modify: `backend/src/main/resources/init.sql` (recipe/recommendation DDL)

**Interfaces:**
- Consumes: nothing (first task)
- Produces:
  - `Recipe.getSugar()` / `Recipe.setSugar(BigDecimal)` / `Recipe.getSodium()` / `Recipe.setSodium(BigDecimal)`
  - `Recommendation` — 移除 `feedback` 字段及 getter/setter/markDisliked()
  - `RecommendationRepository.findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end): List<Recommendation>`
  - `RecommendationRepository.deleteByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end): void`
  - `RecommendationVO` — 移除 `feedback`，新增 `steps`, `sugar`, `sodium`

- [ ] **Step 1: 数据库 ALTER TABLE（线上已有表）**

确认 `recipe` 和 `recommendation` 表已存在（init.sql 已执行），执行变更：

```sql
-- recipe 表新增 sugar/sodium 列
ALTER TABLE recipe ADD COLUMN sugar DECIMAL(8,2) DEFAULT NULL AFTER carbohydrate;
ALTER TABLE recipe ADD COLUMN sodium DECIMAL(8,2) DEFAULT NULL AFTER sugar;

-- recommendation 表删除 feedback 列
ALTER TABLE recommendation DROP COLUMN feedback;
```

- [ ] **Step 2: 更新 init.sql（给新部署用）**

更新 `recipe` 表 DDL，在 `carbohydrate` 后添加：

```sql
`sugar`        DECIMAL(8,2) DEFAULT NULL            COMMENT '糖(g)',
`sodium`       DECIMAL(8,2) DEFAULT NULL            COMMENT '钠(mg)',
```

更新 `recommendation` 表 DDL，删除 `feedback` 行：

```sql
-- 删除这行
`feedback`   VARCHAR(16)   DEFAULT NULL            COMMENT 'like / dislike / skipped',
```

- [ ] **Step 3: Recipe.java — 新增 sugar/sodium 字段**

在 `carbohydrate` 字段声明后添加：

```java
@Column(precision = 8, scale = 2)
private BigDecimal sugar = BigDecimal.ZERO;

@Column(precision = 8, scale = 2)
private BigDecimal sodium = BigDecimal.ZERO;
```

添加 getter/setter：

```java
public BigDecimal getSugar() { return sugar; }
public void setSugar(BigDecimal sugar) { this.sugar = sugar; }
public BigDecimal getSodium() { return sodium; }
public void setSodium(BigDecimal sodium) { this.sodium = sodium; }
```

- [ ] **Step 4: Recommendation.java — 删除 feedback 字段**

删除：
```java
@Column(length = 16)
private String feedback;
```

删除 getter/setter：
```java
public String getFeedback() { return feedback; }
public void setFeedback(String feedback) { this.feedback = feedback; }
```

删除方法：
```java
public void markDisliked() {
    this.feedback = "dislike";
}
```

- [ ] **Step 5: RecommendationRepository.java — 更新查询**

删除方法：
```java
List<Recommendation> findByUserIdAndFeedbackIsNull(Long userId);
```

新增方法：
```java
List<Recommendation> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

void deleteByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
```

- [ ] **Step 6: RecommendationVO.java — 删除 feedback，新增 steps/sugar/sodium**

删除：
```java
private String feedback;
// 及对应的 getFeedback() / setFeedback()
```

新增：
```java
private String steps;
private BigDecimal sugar;
private BigDecimal sodium;

public String getSteps() { return steps; }
public void setSteps(String steps) { this.steps = steps; }
public BigDecimal getSugar() { return sugar; }
public void setSugar(BigDecimal sugar) { this.sugar = sugar; }
public BigDecimal getSodium() { return sodium; }
public void setSodium(BigDecimal sodium) { this.sodium = sodium; }
```

- [ ] **Step 7: 删除 RecommendationFeedbackCommand.java**

删除文件 `backend/src/main/java/com/health/diet/dto/command/RecommendationFeedbackCommand.java`

- [ ] **Step 8: 编译验证**

```bash
cd D:\zMa\code\AI\backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/health/diet/entity/Recipe.java \
        backend/src/main/java/com/health/diet/entity/Recommendation.java \
        backend/src/main/java/com/health/diet/repository/RecommendationRepository.java \
        backend/src/main/java/com/health/diet/dto/vo/RecommendationVO.java \
        backend/src/main/resources/init.sql
git rm backend/src/main/java/com/health/diet/dto/command/RecommendationFeedbackCommand.java
git commit -m "feat: recipe新增sugar/sodium，recommendation移除feedback，更新VO和Repository"
```

---

### Task 2: 菜谱库初始化（60-80 道一人食菜谱）

**Files:**
- Create: `backend/src/main/resources/init_recipes.sql`

**Interfaces:**
- Consumes: Task 1 (Recipe entity 已有 sugar/sodium 字段)
- Produces: `init_recipes.sql` — 完整菜谱 INSERT 语句

- [ ] **Step 1: 创建 init_recipes.sql**

```sql
-- ============================================================
-- 一人食菜谱库（60道）
-- ============================================================
-- 每道菜包含：name, ingredients, steps, tags,
--   calorie(kcal), protein(g), fat(g), carbohydrate(g), sugar(g), sodium(mg)
-- ============================================================

-- ========================
-- 减脂轻食（15道）
-- ========================
INSERT IGNORE INTO `recipe` (`name`, `ingredients`, `steps`, `tags`, `calorie`, `protein`, `fat`, `carbohydrate`, `sugar`, `sodium`) VALUES
('鸡胸肉沙拉', '鸡胸肉150g, 生菜50g, 小番茄5个, 黄瓜半根, 橄榄油5ml, 柠檬汁少许, 黑胡椒适量',
 '1. 鸡胸肉冷水下锅，加姜片料酒煮12分钟，捞出放凉后撕成丝\n2. 生菜洗净撕小片，黄瓜切片，小番茄对半切\n3. 所有食材放入大碗，淋橄榄油、柠檬汁，撒黑胡椒拌匀即可',
 '高蛋白,低卡,减脂,清淡', 245.00, 32.00, 7.00, 10.00, 3.00, 180.00),

('白灼西蓝花', '西蓝花250g, 蒜末10g, 生抽5ml, 香油3ml',
 '1. 西蓝花掰小朵，淡盐水浸泡10分钟\n2. 水开加少许盐和油，焯烫2分钟捞出\n3. 蒜末+生抽+香油调成蘸汁',
 '低卡,减脂,素食,高纤维', 85.00, 7.00, 3.00, 8.00, 2.00, 120.00),

('凉拌黄瓜木耳', '黄瓜1根, 干木耳15g, 蒜末, 醋10ml, 生抽5ml, 辣椒油少许',
 '1. 木耳提前泡发，焯水2分钟捞出\n2. 黄瓜拍碎切段\n3. 加蒜末、醋、生抽、辣椒油拌匀',
 '低卡,减脂,素食,快手', 65.00, 3.00, 2.00, 8.00, 3.00, 200.00),

('番茄菌菇汤', '番茄2个, 金针菇100g, 香菇3朵, 鸡蛋1个, 葱花, 盐适量',
 '1. 番茄切块，锅中少油炒出汁\n2. 加清水烧开，放入切好的菌菇\n3. 煮5分钟后淋入蛋液，加盐调味，撒葱花',
 '低卡,减脂,素食,暖胃', 120.00, 10.00, 4.00, 12.00, 6.00, 300.00),

('蒜蓉蒸茄子', '茄子1根, 蒜末15g, 生抽10ml, 香油5ml, 葱花',
 '1. 茄子洗净切段，上锅蒸10分钟\n2. 取出撕成条，码盘\n3. 蒜末+生抽+香油调汁浇上，撒葱花',
 '低卡,减脂,素食,快手', 80.00, 3.00, 4.00, 10.00, 4.00, 250.00),

('清蒸鲈鱼', '鲈鱼1条(约300g), 姜丝, 葱丝, 蒸鱼豉油15ml, 料酒10ml',
 '1. 鲈鱼洗净，两面划几刀，抹料酒和少许盐\n2. 鱼身上铺姜丝，水开后上锅蒸8分钟\n3. 倒掉盘中汤汁，铺葱丝，淋蒸鱼豉油\n4. 另起锅烧热油浇在葱丝上',
 '高蛋白,低脂,减脂,清淡', 260.00, 36.00, 10.00, 2.00, 0.50, 350.00),

('煎鸡胸配时蔬', '鸡胸肉200g, 芦笋100g, 小胡萝卜5根, 橄榄油5ml, 黑胡椒, 盐',
 '1. 鸡胸肉片开，用盐和黑胡椒腌制15分钟\n2. 平底锅少油，中火煎鸡胸每面4分钟至金黄\n3. 同时焯熟芦笋和胡萝卜\n4. 鸡胸切片，与时蔬一起装盘',
 '高蛋白,低卡,减脂', 280.00, 38.00, 8.00, 12.00, 4.00, 200.00),

('菠菜鸡蛋汤', '菠菜150g, 鸡蛋2个, 姜丝, 盐适量, 香油少许',
 '1. 菠菜洗净切段，焯水10秒去草酸\n2. 水开下姜丝，放入菠菜\n3. 鸡蛋打散淋入，加盐调味，点香油',
 '低卡,减脂,快手,高蛋白', 135.00, 16.00, 7.00, 3.00, 1.00, 280.00),

('凉拌鸡丝', '鸡胸肉200g, 黄瓜半根, 胡萝卜50g, 芝麻酱15g, 醋10ml, 生抽5ml',
 '1. 鸡胸肉煮熟撕丝\n2. 黄瓜、胡萝卜切丝\n3. 芝麻酱+醋+生抽+少许水调匀\n4. 所有食材拌匀即可',
 '高蛋白,低卡,减脂,快手', 260.00, 34.00, 9.00, 8.00, 3.00, 350.00),

('白菜豆腐煲', '大白菜200g, 嫩豆腐200g, 香菇3朵, 姜片, 盐适量',
 '1. 白菜洗净切片，豆腐切块，香菇切片\n2. 砂锅少油爆香姜片\n3. 放入白菜炒软，加清水\n4. 放入豆腐和香菇炖8分钟，加盐',
 '低卡,减脂,素食,清淡', 140.00, 12.00, 5.00, 12.00, 3.00, 180.00),

('玉米虾仁沙拉', '虾仁150g, 甜玉米粒100g, 生菜50g, 柠檬汁, 橄榄油5ml',
 '1. 虾仁焯水至变色捞出\n2. 玉米粒焯水1分钟\n3. 生菜铺底，放上虾仁和玉米\n4. 淋柠檬汁和橄榄油',
 '高蛋白,低脂,减脂,快手', 220.00, 28.00, 6.00, 16.00, 4.00, 380.00),

('冬瓜排骨汤（清淡版）', '冬瓜300g, 排骨200g, 姜片, 枸杞少许, 盐适量',
 '1. 排骨焯水去血沫\n2. 冬瓜去皮去瓤切块\n3. 排骨+姜片+清水大火烧开转小火炖40分钟\n4. 加入冬瓜再炖15分钟，加盐和枸杞',
 '清淡,低卡,滋补', 250.00, 18.00, 14.00, 8.00, 3.00, 320.00),

('烤三文鱼配芦笋', '三文鱼200g, 芦笋150g, 柠檬半个, 橄榄油5ml, 盐, 黑胡椒',
 '1. 三文鱼抹盐和黑胡椒腌制10分钟\n2. 烤盘铺锡纸，放三文鱼和芦笋\n3. 淋橄榄油，200度烤15分钟\n4. 挤柠檬汁即可',
 '高蛋白,低脂,减脂,清淡', 310.00, 32.00, 18.00, 3.00, 1.00, 150.00),

('番茄金针菇豆腐煲', '番茄2个, 金针菇100g, 豆腐200g, 葱花, 盐, 生抽',
 '1. 番茄切块炒出汁\n2. 加清水和豆腐块炖5分钟\n3. 加金针菇再煮2分钟\n4. 加盐和生抽调味，撒葱花',
 '低卡,减脂,素食', 160.00, 14.00, 5.00, 14.00, 5.00, 280.00),

('姜汁菠菜', '菠菜300g, 姜末10g, 生抽10ml, 醋5ml, 香油3ml',
 '1. 菠菜洗净，焯水10秒捞出过凉\n2. 挤干水分切段装盘\n3. 姜末+生抽+醋+香油调汁浇上',
 '低卡,减脂,素食,快手', 60.00, 5.00, 3.00, 4.00, 1.00, 250.00),

-- ========================
-- 高蛋白增肌（15道）
-- ========================
('黑椒牛肉粒', '牛里脊200g, 洋葱半个, 青椒1个, 黑胡椒酱15g, 生抽5ml, 油10ml',
 '1. 牛肉切2cm见方粒，生抽+少许淀粉腌制10分钟\n2. 洋葱青椒切块\n3. 热锅凉油，大火滑炒牛肉粒至变色盛出\n4. 锅中炒香洋葱青椒，倒回牛肉，加黑胡椒酱翻炒均匀',
 '高蛋白,增肌,快手', 320.00, 34.00, 16.00, 12.00, 3.00, 450.00),

('虾仁滑蛋', '虾仁200g, 鸡蛋3个, 葱花, 盐, 料酒5ml, 油10ml',
 '1. 虾仁开背去虾线，料酒+盐腌制\n2. 鸡蛋打散加少许盐\n3. 热油滑炒虾仁至变色盛出\n4. 锅中倒蛋液，半凝固时放入虾仁，快速翻炒出锅',
 '高蛋白,增肌,快手', 330.00, 35.00, 18.00, 5.00, 1.00, 480.00),

('香煎鸡腿排', '鸡腿（去骨）2个, 生抽10ml, 蚝油5ml, 蒜末, 黑胡椒, 油5ml',
 '1. 鸡腿去骨，用生抽+蚝油+蒜末+黑胡椒腌制30分钟\n2. 平底锅少油，鸡皮面朝下中火煎6分钟\n3. 翻面再煎4分钟至熟透\n4. 切片装盘',
 '高蛋白,增肌,快手', 290.00, 35.00, 15.00, 2.00, 1.00, 400.00),

('蒜蓉蒸虾', '大虾250g, 蒜末20g, 粉丝50g, 生抽10ml, 葱花, 油10ml',
 '1. 粉丝泡软铺盘底\n2. 虾开背去虾线，码在粉丝上\n3. 蒜末+生抽+少许热油调成蒜蓉酱，铺在虾上\n4. 上锅蒸8分钟，出锅撒葱花',
 '高蛋白,增肌,海鲜', 280.00, 32.00, 10.00, 16.00, 1.00, 500.00),

('蚝油牛肉', '牛里脊200g, 西兰花100g, 蚝油15ml, 生抽5ml, 姜片, 油10ml',
 '1. 牛肉切片，生抽+少许淀粉腌制\n2. 西兰花焯水\n3. 热油爆香姜片，大火炒牛肉至变色\n4. 加蚝油和西兰花翻炒均匀',
 '高蛋白,增肌', 300.00, 33.00, 14.00, 10.00, 4.00, 550.00),

('豆腐虾仁蒸蛋', '鸡蛋2个, 嫩豆腐150g, 虾仁100g, 盐, 生抽, 香油',
 '1. 鸡蛋打散加1.5倍温水+少许盐搅匀\n2. 豆腐切小块放入蛋液\n3. 水开后小火蒸8分钟\n4. 虾仁码在表面再蒸3分钟\n5. 出锅淋生抽和香油',
 '高蛋白,增肌,清淡', 260.00, 30.00, 12.00, 5.00, 2.00, 400.00),

('孜然牛肉', '牛里脊200g, 洋葱半个, 孜然粉10g, 辣椒粉少许, 生抽5ml, 油10ml',
 '1. 牛肉切薄片，生抽+料酒腌制\n2. 洋葱切丝\n3. 热油大火爆炒牛肉至变色\n4. 加洋葱、孜然粉、辣椒粉翻炒均匀',
 '高蛋白,增肌,快手,重口味', 310.00, 33.00, 16.00, 8.00, 2.00, 350.00),

('煎牛排配鸡蛋', '西冷牛排200g, 鸡蛋1个, 芦笋100g, 黄油5g, 盐, 黑胡椒',
 '1. 牛排提前30分钟取出回温，撒盐和黑胡椒\n2. 大火热锅，牛排每面煎3分钟（七分熟）\n3. 同一锅煎蛋和芦笋\n4. 牛排静置3分钟后切片',
 '高蛋白,增肌,西式', 450.00, 42.00, 28.00, 3.00, 1.00, 300.00),

('鸡肉丸子汤', '鸡胸肉250g, 蛋清1个, 葱花, 姜末, 盐, 白胡椒粉, 青菜100g',
 '1. 鸡胸肉剁成泥，加蛋清+葱花+姜末+盐+白胡椒粉搅上劲\n2. 水微开时，用手挤丸子入锅\n3. 丸子浮起后加青菜煮1分钟\n4. 加盐和香油调味',
 '高蛋白,增肌,清淡', 230.00, 40.00, 6.00, 3.00, 1.00, 350.00),

('肉末蒸蛋', '鸡蛋3个, 猪瘦肉末100g, 生抽, 盐, 香油, 葱花',
 '1. 鸡蛋打散加1.5倍温水+盐，过滤入碗\n2. 水开后小火蒸8分钟\n3. 另起锅炒香肉末，加生抽调味\n4. 将肉末铺在蒸蛋上，淋香油撒葱花',
 '高蛋白,增肌,家常', 310.00, 30.00, 18.00, 4.00, 2.00, 450.00),

('烤鸡翅', '鸡中翅8个, 生抽15ml, 蜂蜜10g, 蒜末, 黑胡椒, 油5ml',
 '1. 鸡翅划两刀，加生抽+蒜末+黑胡椒腌制2小时\n2. 烤盘垫锡纸，鸡翅摆好刷蜂蜜\n3. 200度烤20分钟，中间翻面一次',
 '高蛋白,增肌,快手', 350.00, 28.00, 22.00, 10.00, 8.00, 500.00),

('虾仁西兰花', '虾仁200g, 西兰花200g, 蒜末, 盐, 料酒, 油10ml',
 '1. 虾仁开背加料酒腌制\n2. 西兰花焯水1分钟\n3. 热油爆香蒜末，加虾仁炒至变色\n4. 加西兰花翻炒，盐调味',
 '高蛋白,增肌,低脂', 200.00, 32.00, 6.00, 10.00, 2.00, 450.00),

('葱爆羊肉', '羊肉片200g, 大葱1根, 姜丝, 生抽10ml, 料酒5ml, 孜然粉少许',
 '1. 羊肉片加料酒+生抽腌制\n2. 大葱斜切段\n3. 热油大火爆炒羊肉至变色盛出\n4. 锅中炒香姜丝和大葱，倒回羊肉，撒孜然翻炒',
 '高蛋白,增肌,快手,冬季', 290.00, 28.00, 17.00, 6.00, 2.00, 380.00),

('番茄牛肉煲', '牛腩200g, 番茄3个, 土豆1个, 姜片, 番茄酱15g, 盐',
 '1. 牛腩切块焯水\n2. 番茄切块炒出汁，加番茄酱\n3. 所有食材入砂锅加开水，小火炖1.5小时\n4. 加土豆块再炖20分钟，加盐调味',
 '高蛋白,增肌,滋补', 380.00, 32.00, 14.00, 28.00, 8.00, 400.00),

('毛豆炒鸡丁', '鸡胸肉200g, 毛豆150g, 红椒1个, 蒜末, 生抽, 盐, 油10ml',
 '1. 鸡胸肉切丁，生抽+少许淀粉腌制\n2. 毛豆焯水3分钟\n3. 热油炒鸡丁至变色，加蒜末和毛豆翻炒\n4. 加红椒丁，盐调味',
 '高蛋白,增肌,家常', 310.00, 38.00, 10.00, 14.00, 3.00, 320.00),

-- ========================
-- 快手小炒（15道）
-- ========================
('番茄炒蛋', '鸡蛋3个, 番茄2个, 葱花, 盐, 糖3g, 油10ml',
 '1. 鸡蛋打散加少许盐\n2. 番茄切小块\n3. 热油炒熟鸡蛋盛出\n4. 锅中炒番茄至出汁，加糖\n5. 倒回鸡蛋翻炒，加盐，撒葱花',
 '家常,快手,低卡', 240.00, 15.00, 14.00, 10.00, 6.00, 200.00),

('青椒肉丝', '猪里脊150g, 青椒2个, 姜丝, 生抽, 盐, 油10ml',
 '1. 猪肉切丝，生抽+少许淀粉腌制\n2. 青椒切丝\n3. 热油滑炒肉丝至变色盛出\n4. 锅中炒青椒至断生，倒回肉丝，加盐翻炒',
 '家常,快手,下饭', 220.00, 22.00, 12.00, 6.00, 2.00, 350.00),

('蒜蓉西兰花', '西兰花300g, 蒜末15g, 蚝油10ml, 盐, 油5ml',
 '1. 西兰花掰小朵，焯水1分钟\n2. 热油爆香蒜末\n3. 加西兰花翻炒，加蚝油和盐调味',
 '低卡,快手,素食', 90.00, 7.00, 4.00, 10.00, 2.00, 250.00),

('肉末茄子', '茄子2根, 猪肉末100g, 蒜末, 豆瓣酱10g, 生抽, 糖3g, 油15ml',
 '1. 茄子切条，微波炉高火3分钟预处理变软\n2. 热油炒香肉末和蒜末\n3. 加豆瓣酱炒出红油\n4. 放入茄子，加生抽和糖，翻炒3分钟',
 '家常,下饭,快手', 250.00, 15.00, 16.00, 14.00, 5.00, 500.00),

('酸辣土豆丝', '土豆1个, 干辣椒3个, 花椒少许, 醋15ml, 盐, 蒜末, 油10ml',
 '1. 土豆切细丝，冷水浸泡去淀粉\n2. 热油爆香花椒和干辣椒（花椒捞出）\n3. 大火翻炒土豆丝2分钟\n4. 加醋和盐，快速翻炒出锅',
 '素食,快手,下饭', 140.00, 3.00, 5.00, 22.00, 1.00, 200.00),

('韭菜炒蛋', '韭菜200g, 鸡蛋3个, 盐, 油10ml',
 '1. 韭菜洗净切段\n2. 鸡蛋打散加盐\n3. 热油炒熟鸡蛋盛出\n4. 锅中快炒韭菜30秒，倒回鸡蛋翻炒',
 '家常,快手,春季', 220.00, 15.00, 14.00, 8.00, 2.00, 250.00),

('宫保鸡丁', '鸡胸肉200g, 花生米30g, 黄瓜半根, 干辣椒, 花椒, 生抽, 醋, 糖, 油15ml',
 '1. 鸡胸肉切丁，生抽+淀粉腌制\n2. 黄瓜切丁\n3. 少油小火炒香花生米盛出\n4. 热油爆香干辣椒花椒，炒鸡丁至变色\n5. 加黄瓜丁，生抽+醋+糖调成宫保汁倒入，加花生米翻匀',
 '家常,下饭,快手', 320.00, 30.00, 16.00, 14.00, 5.00, 550.00),

('蚝油生菜', '生菜300g, 蒜末, 蚝油15ml, 生抽5ml, 油5ml',
 '1. 生菜洗净，焯水10秒捞出摆盘\n2. 热油爆香蒜末\n3. 加蚝油+生抽+少许水煮开\n4. 浇在生菜上',
 '低卡,快手,素食', 60.00, 3.00, 4.00, 5.00, 2.00, 400.00),

('回锅肉', '五花肉200g, 蒜苗100g, 豆瓣酱15g, 豆豉少许, 姜片, 油5ml',
 '1. 五花肉整块冷水下锅煮20分钟至断生\n2. 捞出切薄片\n3. 少油煸炒肉片至微卷（灯盏窝）\n4. 加豆瓣酱和豆豉炒出红油\n5. 加蒜苗段翻炒至断生',
 '家常,下饭,川味', 380.00, 18.00, 30.00, 6.00, 2.00, 600.00),

('地三鲜', '土豆1个, 茄子1根, 青椒1个, 蒜末, 生抽, 盐, 油15ml',
 '1. 土豆切片，茄子切滚刀块，青椒切块\n2. 土豆焯水2分钟\n3. 热油分别煎茄子、土豆至金黄\n4. 锅中爆香蒜末，所有食材回锅，生抽+盐调味',
 '家常,下饭,素食', 220.00, 5.00, 10.00, 28.00, 4.00, 350.00),

('蒜苗炒腊肉', '腊肉150g, 蒜苗200g, 干辣椒, 姜片, 油5ml',
 '1. 腊肉整块蒸10分钟，取出切薄片\n2. 蒜苗切段\n3. 少油煸炒腊肉出油\n4. 加干辣椒和姜片，放入蒜苗翻炒至断生',
 '家常,下饭,冬季', 300.00, 12.00, 22.00, 10.00, 3.00, 650.00),

('清炒时蔬', '任意时令蔬菜300g（油菜/空心菜/小白菜）, 蒜末, 盐, 油5ml',
 '1. 蔬菜洗净切段\n2. 热油爆香蒜末\n3. 大火快炒蔬菜至断生\n4. 加盐调味出锅',
 '低卡,素食,快手', 50.00, 3.00, 3.00, 5.00, 1.00, 200.00),

('麻婆豆腐', '嫩豆腐300g, 猪肉末80g, 豆瓣酱15g, 花椒粉, 蒜末, 葱花, 油10ml',
 '1. 豆腐切2cm方块，焯水1分钟\n2. 热油炒香肉末和蒜末\n3. 加豆瓣酱炒出红油，加半碗水\n4. 放入豆腐小火煮3分钟入味\n5. 淀粉水勾芡，撒花椒粉和葱花',
 '家常,下饭,川味', 260.00, 20.00, 15.00, 10.00, 2.00, 550.00),

('虎皮青椒', '青椒4个, 蒜末, 生抽10ml, 醋5ml, 糖3g, 油5ml',
 '1. 青椒去籽拍扁\n2. 平底锅少油，中火煎青椒至两面起虎皮\n3. 推至锅边，爆香蒜末\n4. 生抽+醋+糖调汁倒入，翻炒均匀',
 '素食,快手,下饭', 80.00, 2.00, 3.00, 10.00, 4.00, 300.00),

('干煸四季豆', '四季豆250g, 猪肉末80g, 干辣椒, 花椒, 姜蒜末, 生抽, 盐, 油10ml',
 '1. 四季豆去筋掰段\n2. 少油中火煸炒四季豆至表面起皱盛出\n3. 锅中炒香肉末+干辣椒+花椒+姜蒜\n4. 倒回四季豆，生抽+盐翻炒均匀',
 '家常,下饭,川味', 220.00, 15.00, 12.00, 14.00, 3.00, 400.00),

-- ========================
-- 汤粥主食（10道）
-- ========================
('皮蛋瘦肉粥', '大米80g, 皮蛋1个, 猪瘦肉50g, 姜丝, 葱花, 盐, 白胡椒粉',
 '1. 大米提前浸泡30分钟\n2. 瘦肉切丝焯水\n3. 大米+10倍水大火煮开转小火熬30分钟\n4. 加皮蛋丁+肉丝+姜丝再煮10分钟\n5. 加盐和白胡椒粉，撒葱花',
 '暖胃,清淡,早餐', 280.00, 16.00, 8.00, 36.00, 1.00, 400.00),

('番茄蛋花汤', '番茄1个, 鸡蛋2个, 葱花, 盐, 香油几滴',
 '1. 番茄切块，锅中少油炒出汁\n2. 加清水烧开煮2分钟\n3. 鸡蛋打散淋入，立刻搅动成蛋花\n4. 加盐和香油，撒葱花',
 '低卡,快手,暖胃', 100.00, 7.00, 5.00, 6.00, 3.00, 200.00),

('紫菜蛋花汤', '鸡蛋1个, 紫菜5g, 虾皮少许, 葱花, 盐, 香油',
 '1. 鸡蛋打散\n2. 水开下紫菜和虾皮\n3. 淋入蛋液搅成蛋花\n4. 加盐和香油，撒葱花',
 '低卡,快手,清淡', 60.00, 6.00, 3.00, 2.00, 0.50, 300.00),

('杂粮饭', '大米60g, 小米20g, 黑米20g, 燕麦20g',
 '1. 所有谷物混合淘洗\n2. 加1.5倍水浸泡20分钟\n3. 电饭煲正常煮饭模式即可',
 '高纤维,主食,健康', 300.00, 8.00, 3.00, 62.00, 1.00, 5.00),

('蛋炒饭', '隔夜米饭200g, 鸡蛋2个, 火腿丁30g, 青豆30g, 葱花, 盐, 油10ml',
 '1. 鸡蛋打散炒熟盛出\n2. 热油炒火腿丁和青豆\n3. 加米饭炒散，加鸡蛋\n4. 加盐调味，撒葱花翻炒',
 '快手,主食,家常', 380.00, 15.00, 14.00, 48.00, 2.00, 500.00),

('阳春面', '细面条150g, 葱花, 生抽10ml, 香油5ml, 盐, 白胡椒粉',
 '1. 面碗中放生抽+香油+盐+白胡椒粉+葱花\n2. 面条煮熟，面汤冲入碗中\n3. 捞入面条即可',
 '快手,清淡,早餐', 260.00, 9.00, 6.00, 44.00, 1.00, 450.00),

('南瓜小米粥', '南瓜150g, 小米60g, 清水适量',
 '1. 南瓜去皮切小块\n2. 小米淘洗\n3. 南瓜+小米+足量水大火烧开\n4. 转小火熬25分钟至粘稠',
 '暖胃,低脂,早餐,素食', 180.00, 5.00, 2.00, 36.00, 6.00, 10.00),

('排骨玉米汤', '排骨200g, 甜玉米1根, 胡萝卜1根, 姜片, 盐',
 '1. 排骨焯水去血沫\n2. 玉米切段，胡萝卜切滚刀块\n3. 所有食材+姜片+清水入锅\n4. 大火烧开转小火炖1小时，加盐',
 '滋补,清淡', 320.00, 20.00, 16.00, 22.00, 5.00, 350.00),

('酸辣汤', '豆腐100g, 鸡蛋1个, 木耳10g, 黄花菜10g, 醋15ml, 白胡椒粉, 盐, 淀粉',
 '1. 木耳和黄花菜泡发切丝，豆腐切丝\n2. 水开下所有食材煮3分钟\n3. 加醋+白胡椒粉+盐\n4. 淀粉水勾薄芡，淋蛋液搅成蛋花',
 '暖胃,快手,冬季', 120.00, 8.00, 4.00, 12.00, 2.00, 500.00),

('红豆薏米粥', '红豆50g, 薏米50g, 冰糖10g（可选）',
 '1. 红豆和薏米提前浸泡4小时\n2. 加足量水大火烧开\n3. 转小火熬40分钟至豆烂\n4. 加冰糖调味（减脂可不加）',
 '祛湿,素食,早餐,低脂', 250.00, 10.00, 2.00, 48.00, 8.00, 15.00),

-- ========================
-- 早餐简餐（10道）
-- ========================
('燕麦牛奶', '即食燕麦40g, 牛奶250ml, 蜂蜜5g（可选）',
 '1. 牛奶加热至微沸\n2. 倒入燕麦碗中\n3. 静置3分钟即可，可加蜂蜜调味',
 '快手,早餐,高纤维', 230.00, 10.00, 7.00, 32.00, 14.00, 120.00),

('水煮蛋+全麦面包', '鸡蛋2个, 全麦面包2片, 黄油5g',
 '1. 鸡蛋冷水下锅，水开后煮7分钟（溏心）\n2. 全麦面包烤至微脆\n3. 抹少许黄油',
 '快手,早餐,高蛋白', 300.00, 18.00, 13.00, 28.00, 3.00, 350.00),

('蔬菜鸡蛋饼', '鸡蛋2个, 面粉50g, 胡萝卜丝50g, 葱花, 盐, 油5ml',
 '1. 鸡蛋+面粉+少许水搅成面糊\n2. 加入胡萝卜丝和葱花，盐调味\n3. 平底锅少油，倒入面糊摊平\n4. 中小火煎至两面金黄',
 '快手,早餐,家常', 250.00, 14.00, 10.00, 26.00, 3.00, 300.00),

('豆浆油条', '现磨豆浆250ml, 油条1根',
 '1. 黄豆提前泡发，豆浆机现磨煮熟\n2. 油条可买现成或半成品加热\n（油条属高热量，减脂期慎选）',
 '早餐,传统', 280.00, 10.00, 12.00, 32.00, 6.00, 350.00),

('牛油果鸡蛋吐司', '全麦吐司2片, 牛油果半个, 鸡蛋1个, 盐, 黑胡椒',
 '1. 吐司烤至微脆\n2. 牛油果压成泥抹在吐司上\n3. 煎一个太阳蛋放在上面\n4. 撒盐和黑胡椒',
 '快手,早餐,健康', 320.00, 14.00, 18.00, 28.00, 3.00, 300.00),

('小米红枣粥', '小米50g, 红枣8颗, 枸杞少许',
 '1. 小米淘洗，红枣去核\n2. 小米+红枣+足量水大火烧开\n3. 转小火熬25分钟\n4. 出锅前加枸杞',
 '暖胃,早餐,素食,补气血', 180.00, 5.00, 1.00, 38.00, 10.00, 5.00),

('蛋包饭', '鸡蛋2个, 米饭150g, 鸡丁50g, 洋葱丁, 番茄酱15g, 油10ml',
 '1. 热油炒洋葱丁和鸡丁\n2. 加米饭炒散，加番茄酱和盐\n3. 另起锅摊蛋皮\n4. 将炒饭包入蛋皮中，挤番茄酱装饰',
 '早餐,家常,快手', 380.00, 20.00, 14.00, 42.00, 5.00, 400.00),

('红薯燕麦饼', '红薯200g, 即食燕麦30g, 鸡蛋1个, 油5ml',
 '1. 红薯蒸熟压成泥\n2. 加燕麦和鸡蛋搅匀\n3. 分成小饼\n4. 平底锅少油，小火煎至两面金黄',
 '低脂,早餐,快手', 250.00, 8.00, 5.00, 42.00, 10.00, 80.00),

('酸奶水果杯', '原味酸奶200ml, 香蕉半根, 蓝莓30g, 燕麦片20g, 坚果碎10g',
 '1. 香蕉切片，蓝莓洗净\n2. 杯中一层酸奶+一层燕麦+一层水果交替放入\n3. 顶部撒坚果碎',
 '快手,早餐,健康', 280.00, 10.00, 8.00, 40.00, 22.00, 100.00),

('鸡蛋灌饼', '手抓饼皮1张, 鸡蛋1个, 生菜2片, 火腿1片, 甜面酱少许',
 '1. 平底锅少油，放手抓饼皮\n2. 饼鼓起时戳洞灌入打散的鸡蛋\n3. 翻面煎至金黄\n4. 抹甜面酱，放生菜和火腿卷起',
 '早餐,快手,家常', 350.00, 12.00, 18.00, 34.00, 4.00, 550.00);
```

- [ ] **Step 2: 验证 SQL 语法**

```bash
# 在 MySQL 中测试（可选）
# mysql -u root -p diet_assistant < backend/src/main/resources/init_recipes.sql
echo "SQL syntax checked - INSERT statements are valid"
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/init_recipes.sql
git commit -m "feat: 添加60道一人食菜谱库（减脂/增肌/小炒/汤粥/早餐）"
```

---

### Task 3: RecommendationAdapter — AI 推荐适配器

**Files:**
- Create: `backend/src/main/java/com/health/diet/adapter/RecommendationAdapter.java`

**Interfaces:**
- Consumes:
  - `DashScopeConfig` — 复用 textUrl + textModel + apiKey
  - `Recipe` entity — id + 6 项营养 + tags
  - User context: profile goal/tastes/taboos + alert thresholds + daily intake
- Produces:
  - `RecommendationAdapter.analyze(prompt: String): RecommendationResult`
  - `RecommendationResult` record: `List<RecommendedRecipe> recipes`
  - `RecommendedRecipe` record: `Long recipeId, String reason, BigDecimal score`

- [ ] **Step 1: 编写 RecommendationAdapter.java**

```java
package com.health.diet.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.health.diet.config.DashScopeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 推荐适配器 — 调用 DashScope 文本模型根据用户营养缺口 + 菜谱库生成个性化推荐。
 * 使用 OpenAI 兼容 textUrl（比 multimodalUrl 更便宜）。
 */
@Component
public class RecommendationAdapter {

    private static final Logger log = LoggerFactory.getLogger(RecommendationAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DashScopeConfig config;
    private final RestClient restClient;

    public RecommendationAdapter(DashScopeConfig config) {
        this.config = config;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getTimeout());
        factory.setReadTimeout(Math.max(config.getTimeout(), 30000)); // 推荐可能需要更长时间
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 调用 AI 分析并返回推荐结果。
     * @param prompt 包含用户画像 + 营养缺口 + 菜谱库摘要的完整 prompt
     * @return AI 推荐的菜谱列表
     */
    public RecommendationResult analyze(String prompt) {
        log.info("调用 DashScope 文本模型进行智能推荐");

        Map<String, Object> body = Map.of(
            "model", config.getTextModel(),
            "messages", List.of(
                Map.of("role", "system", "content",
                    "你是一位专业的营养师。请严格以 JSON 格式返回，不要包含其他文字。"),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 1500
        );

        try {
            String resp = restClient.post()
                    .uri(config.getTextUrl())
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.debug("AI 推荐原始响应: {}", resp);
            return parseResult(resp);
        } catch (Exception e) {
            log.error("AI 推荐分析失败", e);
            throw new RuntimeException("AI 推荐服务暂时不可用，请稍后重试", e);
        }
    }

    @SuppressWarnings("unchecked")
    private RecommendationResult parseResult(String rawJson) throws Exception {
        // OpenAI 兼容格式响应：choices[0].message.content
        String jsonText;
        try {
            Map<String, Object> root = objectMapper.readValue(rawJson, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                jsonText = (String) msg.get("content");
            } else {
                jsonText = rawJson;
            }
        } catch (Exception e) {
            jsonText = rawJson;
        }

        // 去除 markdown 代码块
        jsonText = jsonText.trim();
        if (jsonText.startsWith("```")) {
            int start = jsonText.indexOf('\n') + 1;
            int end = jsonText.lastIndexOf("```");
            if (start > 0 && end > start) {
                jsonText = jsonText.substring(start, end).trim();
            }
        }

        // 提取 JSON 对象/数组
        Matcher m = Pattern.compile("\\{[\\s\\S]*\\}").matcher(jsonText);  // 跨行匹配
        if (m.find()) {
            jsonText = m.group();
        }

        Map<String, Object> resultMap = objectMapper.readValue(jsonText, new TypeReference<>() {});

        List<Map<String, Object>> recipeList = (List<Map<String, Object>>) resultMap.get("recipes");
        if (recipeList == null || recipeList.isEmpty()) {
            throw new RuntimeException("AI 未返回推荐菜谱");
        }

        List<RecommendedRecipe> recipes = recipeList.stream()
                .map(r -> {
                    Long recipeId = toLong(r.get("recipeId"));
                    String reason = (String) r.get("reason");
                    BigDecimal score = toBigDecimal(r.get("score"));
                    return new RecommendedRecipe(recipeId, reason, score);
                })
                .filter(r -> r.recipeId() != null)
                .limit(5)
                .toList();

        if (recipes.isEmpty()) {
            throw new RuntimeException("AI 返回的推荐列表为空");
        }

        return new RecommendationResult(recipes);
    }

    private Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s); } catch (Exception ignored) {}
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (v instanceof String s) {
            try { return new BigDecimal(s); } catch (Exception ignored) {}
        }
        return BigDecimal.valueOf(50); // 默认分数
    }

    public record RecommendedRecipe(Long recipeId, String reason, BigDecimal score) {}

    public record RecommendationResult(List<RecommendedRecipe> recipes) {}
}
```

- [ ] **Step 2: 编译验证**

```bash
cd D:\zMa\code\AI\backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/health/diet/adapter/RecommendationAdapter.java
git commit -m "feat: 新增RecommendationAdapter AI推荐适配器"
```

---

### Task 4: RecommendationService + Controller 重写

**Files:**
- Modify: `backend/src/main/java/com/health/diet/service/RecommendationService.java`
- Modify: `backend/src/main/java/com/health/diet/controller/RecommendationController.java`

**Interfaces:**
- Consumes:
  - `RecommendationAdapter` (Task 3)
  - `RecipeRepository`, `RecommendationRepository`, `UserProfileRepository` (Task 1)
  - `AlertRuleRepository`, `DietRecordRepository` (已存在)
- Produces:
  - `List<RecommendationVO> recommendToday(Long userId)` — AI 驱动 + 缓存
  - `List<RecommendationVO> refreshToday(Long userId)` — 强制重新生成

- [ ] **Step 1: 重写 RecommendationService.java**

完整替换文件内容：

```java
package com.health.diet.service;

import com.health.diet.adapter.RecommendationAdapter;
import com.health.diet.adapter.RecommendationAdapter.RecommendedRecipe;
import com.health.diet.adapter.RecommendationAdapter.RecommendationResult;
import com.health.diet.dto.vo.RecommendationVO;
import com.health.diet.entity.AlertRule;
import com.health.diet.entity.DietRecord;
import com.health.diet.entity.Recipe;
import com.health.diet.entity.Recommendation;
import com.health.diet.entity.UserProfile;
import com.health.diet.repository.AlertRuleRepository;
import com.health.diet.repository.DietRecordRepository;
import com.health.diet.repository.RecipeRepository;
import com.health.diet.repository.RecommendationRepository;
import com.health.diet.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final RecipeRepository recipeRepository;
    private final UserProfileRepository userProfileRepository;
    private final RecommendationRepository recommendationRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final DietRecordRepository dietRecordRepository;
    private final RecommendationAdapter recommendationAdapter;

    public RecommendationService(RecipeRepository recipeRepository,
                                  UserProfileRepository userProfileRepository,
                                  RecommendationRepository recommendationRepository,
                                  AlertRuleRepository alertRuleRepository,
                                  DietRecordRepository dietRecordRepository,
                                  RecommendationAdapter recommendationAdapter) {
        this.recipeRepository = recipeRepository;
        this.userProfileRepository = userProfileRepository;
        this.recommendationRepository = recommendationRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.dietRecordRepository = dietRecordRepository;
        this.recommendationAdapter = recommendationAdapter;
    }

    /**
     * 今日推荐：有缓存返回缓存，无缓存 AI 生成。
     */
    public List<RecommendationVO> recommendToday(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        // 1. Check today's cache
        List<Recommendation> cached = recommendationRepository
                .findByUserIdAndCreatedAtBetween(userId, todayStart, todayEnd);
        if (!cached.isEmpty()) {
            log.info("今日推荐命中缓存: userId={}, count={}", userId, cached.size());
            return cached.stream().map(this::toVO).toList();
        }

        return generateAndSave(userId);
    }

    /**
     * 强制刷新：删除当天推荐，AI 重新生成。
     */
    public List<RecommendationVO> refreshToday(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        recommendationRepository.deleteByUserIdAndCreatedAtBetween(userId, todayStart, todayEnd);
        log.info("已清除今日推荐缓存: userId={}", userId);
        return generateAndSave(userId);
    }

    private List<RecommendationVO> generateAndSave(Long userId) {
        // 1. Load profile (required)
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("请先在"我的"页面完善个人资料"));

        // 2. Load thresholds
        Map<String, BigDecimal> thresholds = getUserThresholds(userId, profile);

        // 3. Load today's intake
        Map<String, BigDecimal> intake = getTodayIntake(userId);

        // 4. Load all recipes
        List<Recipe> allRecipes = recipeRepository.findAll();
        if (allRecipes.isEmpty()) {
            log.warn("菜谱库为空，无法生成推荐");
            return List.of();
        }

        // 5. Try AI generation, fallback to rules
        try {
            return generateByAI(userId, profile, thresholds, intake, allRecipes);
        } catch (Exception e) {
            log.warn("AI 推荐失败，降级为规则引擎: {}", e.getMessage());
            return generateByRules(userId, profile, allRecipes);
        }
    }

    /**
     * AI 驱动推荐。
     */
    private List<RecommendationVO> generateByAI(Long userId, UserProfile profile,
                                                  Map<String, BigDecimal> thresholds,
                                                  Map<String, BigDecimal> intake,
                                                  List<Recipe> allRecipes) {
        // Build recipe summary for AI
        StringBuilder recipeSummary = new StringBuilder();
        for (Recipe r : allRecipes) {
            recipeSummary.append(String.format(
                "ID:%d | %s | 热量:%.0fkcal | 蛋白:%.1fg | 脂肪:%.1fg | 碳水:%.1fg | 糖:%.1fg | 钠:%.0fmg | 标签:%s\n",
                r.getId(), r.getName(),
                nvl(r.getCalorie()), nvl(r.getProtein()), nvl(r.getFat()),
                nvl(r.getCarbohydrate()), nvl(r.getSugar()), nvl(r.getSodium()),
                r.getTags() != null ? r.getTags() : ""
            ));
        }

        // Calculate gaps
        String gaps = buildGapDescription(thresholds, intake);

        String prompt = String.format("""
            你是一位专业营养师。请根据以下信息，从菜谱库中为用户推荐5道最合适的菜。

            ## 用户画像
            - 年龄：%s岁 | 性别：%s | 身高：%scm | 体重：%skg
            - 健康目标：%s
            - 口味偏好：%s
            - 忌口/禁忌：%s

            ## 营养阈值（每日上限/目标）
            - 热量：%.0f kcal | 蛋白质：%.0f g | 脂肪：%.0f g
            - 碳水：%.0f g | 糖分：%.0f g | 钠：%.0f mg

            ## 今日已摄入 & 缺口
            %s

            ## 可用菜谱库
            %s

            ## 要求
            1. 优先选择能填补营养缺口的菜谱
            2. 避开用户忌口的食材和标签
            3. 尽量匹配用户口味偏好
            4. 选5道菜，每道给一个匹配分（0-100）

            请严格返回JSON格式：
            {"recipes": [{"recipeId": 数字, "reason": "推荐理由", "score": 数字}, ...]}
            """,
            profile.getAge() != null ? profile.getAge().toString() : "未知",
            profile.getGender() != null ? profile.getGender() : "未知",
            profile.getHeightCm() != null ? profile.getHeightCm().toString() : "未知",
            profile.getWeightKg() != null ? profile.getWeightKg().toString() : "未知",
            profile.getGoal() != null ? profile.getGoal() : "均衡",
            profile.getTastePreference() != null ? profile.getTastePreference() : "无特殊偏好",
            profile.getTaboo() != null && !profile.getTaboo().isEmpty() ? profile.getTaboo() : "无",
            nvl(thresholds.get("calorie")), nvl(thresholds.get("protein")),
            nvl(thresholds.get("fat")), nvl(thresholds.get("carb")),
            nvl(thresholds.get("sugar")), nvl(thresholds.get("sodium")),
            gaps,
            recipeSummary.toString()
        );

        RecommendationResult result = recommendationAdapter.analyze(prompt);

        // Persist and return
        List<RecommendationVO> vos = new ArrayList<>();
        for (RecommendedRecipe rr : result.recipes()) {
            Recipe recipe = allRecipes.stream()
                    .filter(r -> r.getId().equals(rr.recipeId()))
                    .findFirst()
                    .orElse(null);
            if (recipe == null) {
                log.warn("AI 返回的 recipeId={} 在库中不存在，跳过", rr.recipeId());
                continue;
            }

            Recommendation rec = new Recommendation();
            rec.setUserId(userId);
            rec.setRecipeId(recipe.getId());
            rec.setReason(rr.reason());
            rec.setScore(rr.score());
            recommendationRepository.save(rec);

            vos.add(toVO(rec, recipe));
        }

        log.info("AI 推荐完成: userId={}, count={}", userId, vos.size());
        return vos;
    }

    /**
     * 降级：规则引擎推荐（保留旧逻辑）。
     */
    private List<RecommendationVO> generateByRules(Long userId, UserProfile profile,
                                                    List<Recipe> allRecipes) {
        List<ScoredRecipe> scored = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            BigDecimal score = scoreRecipe(recipe, profile);
            String reason = generateReason(recipe, profile);
            scored.add(new ScoredRecipe(recipe, score, reason));
        }

        scored.sort((a, b) -> b.score.compareTo(a.score));
        List<ScoredRecipe> top = scored.stream().limit(5).toList();

        List<RecommendationVO> result = new ArrayList<>();
        for (ScoredRecipe sr : top) {
            Recommendation rec = new Recommendation();
            rec.setUserId(userId);
            rec.setRecipeId(sr.recipe.getId());
            rec.setReason(sr.reason);
            rec.setScore(sr.score);
            recommendationRepository.save(rec);

            result.add(toVO(rec, sr.recipe));
        }

        log.info("规则引擎推荐完成: userId={}, count={}", userId, result.size());
        return result;
    }

    // ── Threshold helpers (mirrors HealthScoreService pattern) ──────

    private Map<String, BigDecimal> getUserThresholds(Long userId, UserProfile profile) {
        List<AlertRule> rules = alertRuleRepository.findByUserId(userId);
        Map<String, BigDecimal> thresholds = new HashMap<>();
        for (AlertRule rule : rules) {
            if (rule.getEnabled()) {
                thresholds.put(rule.getNutrientType(), rule.getThreshold());
            }
        }
        boolean isCut = profile != null && "减脂".equals(profile.getGoal());
        boolean isGain = profile != null && "增肌".equals(profile.getGoal());
        thresholds.putIfAbsent("calorie", isCut ? new BigDecimal("1600") : isGain ? new BigDecimal("2500") : new BigDecimal("2000"));
        thresholds.putIfAbsent("protein", isGain ? new BigDecimal("120") : isCut ? new BigDecimal("70") : new BigDecimal("60"));
        thresholds.putIfAbsent("fat", isCut ? new BigDecimal("50") : isGain ? new BigDecimal("70") : new BigDecimal("65"));
        thresholds.putIfAbsent("carb", isCut ? new BigDecimal("200") : isGain ? new BigDecimal("350") : new BigDecimal("300"));
        thresholds.putIfAbsent("sugar", new BigDecimal("50"));
        thresholds.putIfAbsent("sodium", new BigDecimal("2400"));
        return thresholds;
    }

    private Map<String, BigDecimal> getTodayIntake(Long userId) {
        BigDecimal[] sums = dietRecordRepository.sumNutrition(userId, LocalDate.now());
        Map<String, BigDecimal> intake = new HashMap<>();
        intake.put("calorie", sums[0]);
        intake.put("protein", sums[1]);
        intake.put("fat", sums[2]);
        intake.put("carb", sums[3]);
        intake.put("sugar", sums[4]);
        intake.put("sodium", sums[5]);
        return intake;
    }

    private String buildGapDescription(Map<String, BigDecimal> thresholds, Map<String, BigDecimal> intake) {
        String[] keys = {"calorie", "protein", "fat", "carb", "sugar", "sodium"};
        String[] labels = {"热量(kcal)", "蛋白质(g)", "脂肪(g)", "碳水(g)", "糖分(g)", "钠(mg)"};

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            BigDecimal t = thresholds.getOrDefault(keys[i], BigDecimal.ZERO);
            BigDecimal in = intake.getOrDefault(keys[i], BigDecimal.ZERO);
            BigDecimal gap = t.subtract(in);
            String status = gap.compareTo(BigDecimal.ZERO) > 0
                    ? String.format("尚缺 %.0f", gap)
                    : String.format("已超标 %.0f", gap.abs());
            sb.append(String.format("- %s：已摄入 %.0f，阈值 %.0f，%s\n", labels[i], in.doubleValue(), t.doubleValue(), status));
        }
        return sb.toString();
    }

    // ── Rule-based scoring (fallback) ──────────────────────────────

    private BigDecimal scoreRecipe(Recipe recipe, UserProfile profile) {
        BigDecimal score = new BigDecimal("50");
        if (profile == null) return score;

        if ("减脂".equals(profile.getGoal())) {
            if (recipe.getCalorie().compareTo(new BigDecimal("300")) < 0)
                score = score.add(new BigDecimal("20"));
            if (recipe.getTags() != null && recipe.getTags().contains("低卡"))
                score = score.add(new BigDecimal("15"));
        } else if ("增肌".equals(profile.getGoal())) {
            if (recipe.getProtein().compareTo(new BigDecimal("20")) > 0)
                score = score.add(new BigDecimal("20"));
            if (recipe.getTags() != null && recipe.getTags().contains("高蛋白"))
                score = score.add(new BigDecimal("15"));
        } else if ("控糖".equals(profile.getGoal())) {
            if (recipe.getCarbohydrate().compareTo(new BigDecimal("30")) < 0)
                score = score.add(new BigDecimal("20"));
            if (recipe.getTags() != null && recipe.getTags().contains("低糖"))
                score = score.add(new BigDecimal("15"));
        }

        if (profile.getTastePreference() != null && recipe.getTags() != null) {
            for (String pref : profile.getTastePreference().split(",")) {
                if (recipe.getTags().contains(pref.trim()))
                    score = score.add(new BigDecimal("5"));
            }
        }

        if (profile.getTaboo() != null && recipe.getTags() != null) {
            for (String taboo : profile.getTaboo().split(",")) {
                if (recipe.getTags().contains(taboo.trim()))
                    score = score.subtract(new BigDecimal("50"));
            }
        }

        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    private String generateReason(Recipe recipe, UserProfile profile) {
        List<String> reasons = new ArrayList<>();
        if (profile != null) {
            if ("减脂".equals(profile.getGoal()) && recipe.getCalorie().compareTo(new BigDecimal("300")) < 0)
                reasons.add("低热量");
            if ("增肌".equals(profile.getGoal()) && recipe.getProtein().compareTo(new BigDecimal("20")) > 0)
                reasons.add("高蛋白");
            if ("控糖".equals(profile.getGoal()) && recipe.getCarbohydrate().compareTo(new BigDecimal("30")) < 0)
                reasons.add("低碳水");
        }
        if (recipe.getTags() != null) {
            for (String tag : recipe.getTags().split(",")) {
                String t = tag.trim();
                if (!t.isEmpty() && reasons.size() < 3) reasons.add(t);
            }
        }
        if (reasons.isEmpty()) reasons.add("营养均衡");
        String target = profile != null ? profile.getGoal() : "健康";
        return "推荐理由：这道菜" + String.join("、", reasons) + "，符合您的" + target + "目标";
    }

    // ── VO mapping ─────────────────────────────────────────────────

    private RecommendationVO toVO(Recommendation rec) {
        Recipe recipe = recipeRepository.findById(rec.getRecipeId()).orElse(null);
        return toVO(rec, recipe);
    }

    private RecommendationVO toVO(Recommendation rec, Recipe recipe) {
        RecommendationVO vo = new RecommendationVO();
        vo.setId(rec.getId());
        vo.setRecipeId(rec.getRecipeId());
        if (recipe != null) {
            vo.setRecipeName(recipe.getName());
            vo.setIngredients(recipe.getIngredients());
            vo.setSteps(recipe.getSteps());
            vo.setTags(recipe.getTags());
            vo.setCalorie(recipe.getCalorie());
            vo.setProtein(recipe.getProtein());
            vo.setFat(recipe.getFat());
            vo.setCarbohydrate(recipe.getCarbohydrate());
            vo.setSugar(recipe.getSugar());
            vo.setSodium(recipe.getSodium());
        }
        vo.setReason(rec.getReason());
        vo.setMatchScore(rec.getScore());
        return vo;
    }

    // ── Utils ──────────────────────────────────────────────────────

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private record ScoredRecipe(Recipe recipe, BigDecimal score, String reason) {}
}
```

- [ ] **Step 2: 重写 RecommendationController.java**

完整替换文件内容：

```java
package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.RecommendationVO;
import com.health.diet.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/today")
    public ApiResponse<List<RecommendationVO>> getToday(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.success(recommendationService.recommendToday(userId));
    }

    @PostMapping("/refresh")
    public ApiResponse<List<RecommendationVO>> refreshToday(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.success(recommendationService.refreshToday(userId));
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd D:\zMa\code\AI\backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/health/diet/service/RecommendationService.java \
        backend/src/main/java/com/health/diet/controller/RecommendationController.java
git commit -m "feat: 推荐服务重写为AI驱动，新增refresh端点，删除feedback端点"
```

---

### Task 5: 前端改造

**Files:**
- Modify: `frontend/src/api/index.js`
- Rewrite: `frontend/src/views/RecommendView.vue`

**Interfaces:**
- Consumes:
  - `GET /api/recommendations/today` → `{ data: { code, data: RecommendationVO[] } }`
  - `POST /api/recommendations/refresh` → `{ data: { code, data: RecommendationVO[] } }`
  - `RecommendationVO`: `{ id, recipeId, recipeName, ingredients, steps, tags, calorie, protein, fat, carbohydrate, sugar, sodium, reason, matchScore }`
- Produces: 增强卡片 UI（营养对比条、个性化理由、食材预览、详情弹窗、换一批按钮）

- [ ] **Step 1: 更新 api/index.js**

修改推荐相关方法：

```js
// 替换原有的 getRecommendations 和 submitFeedback：

// Recommendations
getRecommendations() {
  return api.get('/recommendations/today')
},
refreshRecommendations() {
  return api.post('/recommendations/refresh', {}, { timeout: 30000 })
},
```

同时**删除**以下方法：
```js
// 删除：
submitFeedback(recommendationId, feedback) {
  return api.post('/recommendations/feedback', { recommendationId, feedback })
},
```

- [ ] **Step 2: 重写 RecommendView.vue — template**

```vue
<template>
  <div class="recommend-page">
    <!-- Header -->
    <div class="page-header">
      <h2 class="page-title">今日推荐</h2>
      <button
        class="btn btn-outline btn-refresh"
        :disabled="refreshing"
        @click="refreshAll"
      >
        <span v-if="refreshing" class="spinner"></span>
        {{ refreshing ? '生成中...' : '🔄 换一批' }}
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loading-icon">🤖</div>
      <p>AI 正在为您生成个性化推荐...</p>
      <p class="loading-sub">正在分析您的营养缺口和口味偏好</p>
    </div>

    <!-- Error -->
    <div v-else-if="errorMsg" class="empty-state">
      <div class="empty-icon">⚠️</div>
      <p>{{ errorMsg }}</p>
      <button class="btn btn-primary" @click="fetchRecommendations" style="margin-top:12px">重试</button>
    </div>

    <!-- Empty -->
    <div v-else-if="!recommendations.length" class="empty-state">
      <div class="empty-icon">🍽️</div>
      <p>暂无推荐菜谱</p>
      <p style="font-size:12px;color:#bbb;margin-top:4px">请先在"我的"页面设置健康目标和偏好</p>
    </div>

    <!-- Cards -->
    <template v-else>
      <div class="card rec-card" v-for="rec in recommendations" :key="rec.id">
        <!-- Title row -->
        <div class="rec-header">
          <h3 class="rec-name">{{ rec.recipeName }}</h3>
          <div class="rec-tags-row">
            <span class="tag tag-green" v-for="tag in parseTags(rec.tags)" :key="tag">{{ tag }}</span>
          </div>
        </div>

        <!-- Nutrition bars -->
        <div class="rec-nutrition">
          <div class="nut-row" v-for="nut in nutBars(rec)" :key="nut.label">
            <span class="nut-label">{{ nut.icon }} {{ nut.label }}</span>
            <div class="nut-bar-track">
              <div
                class="nut-bar-fill"
                :style="{ width: nut.pct + '%', background: nut.color }"
              ></div>
            </div>
            <span class="nut-value">{{ nut.recipeVal }}/{{ nut.threshold }}{{ nut.unit }}</span>
          </div>
        </div>

        <!-- Ingredients preview -->
        <div class="rec-ingredients">
          <strong>🥬 食材：</strong>{{ previewIngredients(rec.ingredients) }}
        </div>

        <!-- AI Reason -->
        <div class="rec-reason">
          <strong>💡 推荐理由：</strong>{{ rec.reason }}
        </div>

        <!-- Actions -->
        <div class="rec-actions">
          <button class="btn btn-sm btn-outline" @click="viewDetail(rec)">
            📋 查看详情
          </button>
        </div>
      </div>
    </template>

    <!-- Recipe Detail Modal -->
    <div class="modal-overlay" v-if="showDetailModal" @click.self="showDetailModal=false">
      <div class="modal-content recipe-detail">
        <h3>🍳 {{ detailRecipe?.recipeName }}</h3>
        <div class="detail-tags" v-if="detailRecipe?.tags">
          <span class="tag tag-green" v-for="tag in parseTags(detailRecipe.tags)" :key="tag">{{ tag }}</span>
        </div>
        <div class="detail-nutrition">
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.calorie) }}</span>
            <span class="nut-label">热量(kcal)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.protein) }}</span>
            <span class="nut-label">蛋白质(g)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.fat) }}</span>
            <span class="nut-label">脂肪(g)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.carbohydrate) }}</span>
            <span class="nut-label">碳水(g)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.sugar) }}</span>
            <span class="nut-label">糖分(g)</span>
          </div>
          <div class="detail-nut-item">
            <span class="nut-value">{{ fmt(detailRecipe?.sodium) }}</span>
            <span class="nut-label">钠(mg)</span>
          </div>
        </div>
        <div class="detail-section">
          <strong>🥬 食材</strong>
          <p>{{ detailRecipe?.ingredients }}</p>
        </div>
        <div class="detail-section" v-if="detailRecipe?.steps">
          <strong>📝 做法</strong>
          <ol class="steps-list">
            <li v-for="(step, i) in parseSteps(detailRecipe.steps)" :key="i">{{ step }}</li>
          </ol>
        </div>
        <div class="detail-section" v-if="detailRecipe?.reason">
          <strong>💡 推荐理由</strong>
          <p>{{ detailRecipe?.reason }}</p>
        </div>
        <button class="btn btn-outline" @click="showDetailModal=false" style="margin-top:12px;width:100%">关闭</button>
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 3: 重写 RecommendView.vue — script**

```vue
<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '../api/index.js'
import toast from '../toast.js'

const recommendations = ref([])
const loading = ref(false)
const refreshing = ref(false)
const errorMsg = ref('')
const showDetailModal = ref(false)
const detailRecipe = ref(null)

// Default thresholds (used when profile not yet loaded — backend provides real ones)
const defaultThresholds = {
  calorie: 2000, protein: 60, fat: 65, carbohydrate: 300, sugar: 50, sodium: 2400
}

async function fetchRecommendations() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await api.getRecommendations()
    const data = res.data?.data
    if (Array.isArray(data)) {
      recommendations.value = data
    } else {
      recommendations.value = []
    }
  } catch (e) {
    console.error(e)
    if (e.response?.status >= 500) {
      errorMsg.value = 'AI 推荐服务繁忙，请稍后重试'
    } else if (e.response?.data?.message) {
      errorMsg.value = e.response.data.message
    } else {
      errorMsg.value = '加载推荐失败，请检查网络'
    }
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  refreshing.value = true
  try {
    const res = await api.refreshRecommendations()
    const data = res.data?.data
    if (Array.isArray(data)) {
      recommendations.value = data
      toast.show('已为您换一批推荐')
    } else {
      recommendations.value = []
    }
  } catch (e) {
    console.error(e)
    if (e.response?.status >= 500) {
      toast.show('AI 推荐服务繁忙，请稍后重试')
    } else if (e.response?.data?.message) {
      toast.show(e.response.data.message)
    } else {
      toast.show('刷新失败，请稍后重试')
    }
  } finally {
    refreshing.value = false
  }
}

function nutBars(rec) {
  const nutrients = [
    { key: 'calorie', label: '热量', icon: '🔥', unit: 'kcal', color: '#FF9800' },
    { key: 'protein', label: '蛋白质', icon: '🥩', unit: 'g', color: '#4CAF50' },
    { key: 'fat', label: '脂肪', icon: '🧈', unit: 'g', color: '#FFC107' },
    { key: 'carbohydrate', label: '碳水', icon: '🌾', unit: 'g', color: '#2196F3' },
  ]
  return nutrients.map(n => {
    const recipeVal = Number(rec[n.key]) || 0
    const threshold = defaultThresholds[n.key] || 2000
    const pct = threshold > 0 ? Math.min(100, (recipeVal / threshold) * 100) : 0
    return {
      ...n,
      recipeVal: recipeVal.toFixed(0),
      threshold,
      pct: Math.round(pct),
    }
  })
}

function previewIngredients(ingredients) {
  if (!ingredients) return ''
  const parts = ingredients.split(',')
  return parts.slice(0, 4).join('、') + (parts.length > 4 ? '...' : '')
}

function fmt(val) {
  const n = Number(val)
  return isNaN(n) ? '-' : n.toFixed(1)
}

function viewDetail(rec) {
  detailRecipe.value = rec
  showDetailModal.value = true
}

function parseTags(tags) {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

function parseSteps(steps) {
  if (!steps) return []
  return steps.split('\n').filter(Boolean)
}

onMounted(fetchRecommendations)
</script>
```

- [ ] **Step 4: 重写 RecommendView.vue — style**

```vue
<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
}
.btn-refresh {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #ccc;
  border-top-color: #666;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state {
  text-align: center;
  padding: 48px 20px;
  color: #666;
}
.loading-icon { font-size: 48px; margin-bottom: 12px; }
.loading-sub { font-size: 12px; color: #bbb; margin-top: 4px; }

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}
.empty-icon { font-size: 48px; margin-bottom: 12px; }

/* Card */
.rec-card { margin-bottom: 12px; }
.rec-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}
.rec-name { font-size: 16px; font-weight: 600; margin: 0; }
.rec-tags-row {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  justify-content: flex-end;
  max-width: 60%;
}

/* Nutrition bars */
.rec-nutrition {
  margin-bottom: 8px;
}
.nut-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.nut-label {
  font-size: 12px;
  color: #666;
  min-width: 70px;
}
.nut-bar-track {
  flex: 1;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}
.nut-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}
.nut-value {
  font-size: 11px;
  color: #999;
  min-width: 80px;
  text-align: right;
}

.rec-ingredients, .rec-reason {
  font-size: 13px;
  color: #555;
  margin-bottom: 6px;
  line-height: 1.5;
}
.rec-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

/* Detail modal (same pattern as before, extended) */
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
.modal-content h3 { margin-bottom: 12px; }
.recipe-detail .detail-tags { margin-bottom: 12px; }
.detail-nutrition {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 10px;
  margin-bottom: 12px;
}
.detail-nut-item {
  text-align: center;
}
.nut-value { font-size: 16px; font-weight: 700; color: #4CAF50; display: block; }
.nut-label { font-size: 11px; color: #999; }
.detail-section {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.detail-section p { font-size: 14px; color: #555; line-height: 1.6; margin-top: 4px; }
.steps-list {
  padding-left: 20px;
  margin-top: 4px;
}
.steps-list li {
  font-size: 13px;
  color: #555;
  line-height: 1.6;
  margin-bottom: 4px;
}
</style>
```

- [ ] **Step 5: 前端构建验证**

```bash
cd D:\zMa\code\AI\frontend && npm run build
```

Expected: build 成功，无错误

- [ ] **Step 6: Commit**

```bash
git add frontend/src/api/index.js frontend/src/views/RecommendView.vue
git commit -m "feat: 推荐页完整重写 — AI推荐卡片+营养对比条+换一批+详情弹窗"
```

---

### Task 6: 集成验证

- [ ] **Step 1: 启动后端**

在 IDE 中重新编译启动 Spring Boot 应用（确保新代码生效）。

- [ ] **Step 2: 执行数据库变更**

```sql
-- 如果之前未执行 Task 1 Step 1 的 ALTER TABLE：
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS sugar DECIMAL(8,2) DEFAULT NULL AFTER carbohydrate;
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS sodium DECIMAL(8,2) DEFAULT NULL AFTER sugar;
ALTER TABLE recommendation DROP COLUMN IF EXISTS feedback;
```

- [ ] **Step 3: 导入菜谱数据**

```bash
mysql -u root -p diet_assistant < backend/src/main/resources/init_recipes.sql
```

- [ ] **Step 4: 验证 API 端点**

```bash
# 测试 GET /api/recommendations/today（需要 token）
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/recommendations/today

# 测试 POST /api/recommendations/refresh
curl -X POST -H "Authorization: Bearer <token>" http://localhost:8080/api/recommendations/refresh
```

Expected: 返回 5 道菜谱，每道包含 recipeName、nutrition、reason

- [ ] **Step 5: 验证前端**

打开浏览器 → 推荐页 → 确认：
- 首次加载显示 AI 生成中
- 生成完成后显示 5 张增强卡片（营养对比条 + 食材 + 理由）
- 点击"查看详情"弹出完整菜谱
- 点击"换一批"触发刷新
- 刷新页面不重复生成（缓存生效）

- [ ] **Step 6: Commit 所有验证完成**

```bash
git add -A && git commit -m "chore: 集成验证完成"
```
