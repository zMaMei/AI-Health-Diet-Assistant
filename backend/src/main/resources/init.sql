-- ============================================================
-- AI智能个人健康饮食助手 - 数据库初始化脚本
-- 基于《AI智能个人健康饮食助手-软件设计文档》表结构设计
-- 数据参考 data.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS diet_assistant
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE diet_assistant;

-- 暂时禁用外键检查，以便按任意顺序 DROP TABLE
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. users 表（文档 8.2.1）
-- ============================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `nickname`      VARCHAR(32)  NOT NULL                COMMENT '展示昵称',
    `username`      VARCHAR(32)  NOT NULL                COMMENT '登录用户名',
    `password_hash` VARCHAR(128) NOT NULL                COMMENT 'BCrypt 密码哈希',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. user_profile 表（文档 8.2.2）
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_profile` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '档案主键',
    `user_id`          BIGINT       NOT NULL                COMMENT '所属用户',
    `age`              INT          DEFAULT NULL            COMMENT '年龄',
    `height_cm`        DECIMAL(5,2) DEFAULT NULL            COMMENT '身高，单位 cm',
    `weight_kg`        DECIMAL(5,2) DEFAULT NULL            COMMENT '体重，单位 kg',
    `goal`             VARCHAR(32)  NOT NULL                COMMENT '健康目标，如减脂/控糖',
    `taboo`            VARCHAR(255) DEFAULT NULL            COMMENT '忌口标签列表',
    `taste_preference` VARCHAR(255) DEFAULT NULL            COMMENT '口味偏好标签',
    `warning_profile`  VARCHAR(255) DEFAULT NULL            COMMENT '慢性病或特殊饮食标签',
    `gender`           VARCHAR(8)   DEFAULT NULL            COMMENT '性别（男/女）',
    `avatar_url`       VARCHAR(255) DEFAULT NULL            COMMENT '头像本地路径',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    CONSTRAINT `fk_profile_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户健康档案表';

-- ============================================================
-- 3. food_item 表（文档 8.2.3）
-- ============================================================
CREATE TABLE IF NOT EXISTS `food_item` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '食物主键',
    `name`         VARCHAR(64)  NOT NULL                COMMENT '标准食物名称',
    `category`     VARCHAR(32)  NOT NULL                COMMENT '食物分类',
    `unit`         VARCHAR(16)  NOT NULL                COMMENT '营养基线单位，如 100g / 份',
    `calorie`      DECIMAL(8,2) NOT NULL DEFAULT 0.00   COMMENT '热量（每单位）',
    `protein`      DECIMAL(8,2) NOT NULL DEFAULT 0.00   COMMENT '蛋白质（每单位）',
    `fat`          DECIMAL(8,2) NOT NULL DEFAULT 0.00   COMMENT '脂肪（每单位）',
    `carbohydrate` DECIMAL(8,2) NOT NULL DEFAULT 0.00   COMMENT '碳水化合物（每单位）',
    `sugar`        DECIMAL(8,2) DEFAULT NULL            COMMENT '糖（每单位）',
    `sodium`       DECIMAL(8,2) DEFAULT NULL            COMMENT '钠（每单位）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标准食物营养基线表';

-- ============================================================
-- 4. diet_record 表（文档 8.2.4）
-- ============================================================
CREATE TABLE IF NOT EXISTS `diet_record` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '饮食记录主键',
    `user_id`      BIGINT       NOT NULL                COMMENT '所属用户',
    `food_id`      BIGINT       DEFAULT NULL            COMMENT '匹配到的标准食物',
    `food_name`    VARCHAR(64)  NOT NULL                COMMENT '最终确认的食物名称',
    `meal_type`    VARCHAR(16)  NOT NULL                COMMENT '早餐/午餐/晚餐/夜宵/其他',
    `amount`       DECIMAL(8,2) NOT NULL                COMMENT '份量',
    `source`       VARCHAR(16)  NOT NULL                COMMENT 'photo / voice / manual',
    `image_url`    VARCHAR(255) DEFAULT NULL            COMMENT '拍照识别图片本地引用',
    `calorie`      DECIMAL(8,2) DEFAULT 0.00            COMMENT '热量(kcal)',
    `protein`      DECIMAL(8,2) DEFAULT 0.00            COMMENT '蛋白质(g)',
    `fat`          DECIMAL(8,2) DEFAULT 0.00            COMMENT '脂肪(g)',
    `carbohydrate` DECIMAL(8,2) DEFAULT 0.00            COMMENT '碳水(g)',
    `sugar`        DECIMAL(8,2) DEFAULT 0.00            COMMENT '糖(g)',
    `sodium`       DECIMAL(8,2) DEFAULT 0.00            COMMENT '钠(mg)',
    `record_time`  DATETIME     NOT NULL                COMMENT '记录时间',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_record_time` (`record_time`),
    KEY `idx_user_record_time` (`user_id`, `record_time`),
    CONSTRAINT `fk_record_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_record_food` FOREIGN KEY (`food_id`) REFERENCES `food_item` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食记录表（含营养快照）';

-- ============================================================
-- 5. nutrition_record 表（文档 8.2.5）
-- ============================================================
CREATE TABLE IF NOT EXISTS `nutrition_record` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '汇总记录主键',
    `user_id`           BIGINT        NOT NULL                COMMENT '所属用户',
    `record_date`       DATE          NOT NULL                COMMENT '汇总日期',
    `calorie_total`     DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '总热量',
    `protein_total`     DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '总蛋白质',
    `fat_total`         DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '总脂肪',
    `carbohydrate_total` DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '总碳水',
    `sugar_total`       DECIMAL(10,2) DEFAULT NULL            COMMENT '总糖',
    `sodium_total`      DECIMAL(10,2) DEFAULT NULL            COMMENT '总钠',
    `score`             DECIMAL(5,2)  DEFAULT NULL            COMMENT '健康评分 0-100',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
    CONSTRAINT `fk_nutrition_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日营养汇总表';

-- ============================================================
-- 6. recipe 表（文档 8.2.6）
-- ============================================================
CREATE TABLE IF NOT EXISTS `recipe` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '食谱主键',
    `name`         VARCHAR(64)  NOT NULL                COMMENT '食谱名称',
    `ingredients`  TEXT         NOT NULL                COMMENT '主要食材',
    `steps`        TEXT         NOT NULL                COMMENT '简化做法',
    `tags`         VARCHAR(255) DEFAULT NULL            COMMENT '高蛋白、低钠等标签',
    `calorie`      DECIMAL(8,2) NOT NULL DEFAULT 0.00   COMMENT '单份热量',
    `protein`      DECIMAL(8,2) NOT NULL DEFAULT 0.00   COMMENT '单份蛋白质',
    `fat`          DECIMAL(8,2) NOT NULL DEFAULT 0.00   COMMENT '单份脂肪',
    `carbohydrate` DECIMAL(8,2) NOT NULL DEFAULT 0.00   COMMENT '单份碳水',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食谱表';

-- ============================================================
-- 7. recommendation 表（文档 8.2.7）
-- ============================================================
CREATE TABLE IF NOT EXISTS `recommendation` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '推荐记录主键',
    `user_id`    BIGINT        NOT NULL                COMMENT '所属用户',
    `recipe_id`  BIGINT        NOT NULL                COMMENT '对应食谱',
    `reason`     VARCHAR(255)  NOT NULL                COMMENT '推荐理由',
    `score`      DECIMAL(6,2)  NOT NULL                COMMENT '匹配度分值',
    `feedback`   VARCHAR(16)   DEFAULT NULL            COMMENT 'like / dislike / skipped',
    `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐生成时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_recommendation_user`   FOREIGN KEY (`user_id`)   REFERENCES `users` (`id`)   ON DELETE CASCADE,
    CONSTRAINT `fk_recommendation_recipe` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食谱推荐记录表';

-- ============================================================
-- 8. alert_rule 表（文档 8.2.8）
-- ============================================================
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '规则主键',
    `user_id`       BIGINT        NOT NULL                COMMENT '所属用户',
    `nutrient_type` VARCHAR(16)   NOT NULL                COMMENT '阈值指标，如 sugar / sodium / calorie',
    `threshold`     DECIMAL(10,2) NOT NULL                COMMENT '阈值上限',
    `enabled`       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_alert_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食预警规则表';

-- ============================================================
-- 9. meal_photo 表（餐次照片）
-- ============================================================
CREATE TABLE IF NOT EXISTS `meal_photo` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '照片主键',
    `user_id`     BIGINT       NOT NULL                COMMENT '所属用户',
    `record_date` DATE         NOT NULL                COMMENT '拍摄日期',
    `meal_type`   VARCHAR(16)  NOT NULL                COMMENT '早餐/午餐/晚餐/夜宵/其他',
    `image_url`   VARCHAR(255) NOT NULL                COMMENT '照片相对路径',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `record_date`),
    KEY `idx_user_date_meal` (`user_id`, `record_date`, `meal_type`),
    CONSTRAINT `fk_meal_photo_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='餐次照片表（一餐可有多张照片）';

-- ============================================================
-- 10. voice_record 表（语音录音）
-- ============================================================
CREATE TABLE IF NOT EXISTS `voice_record` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '录音主键',
    `user_id`          BIGINT       NOT NULL                COMMENT '所属用户',
    `record_date`      DATE         NOT NULL                COMMENT '录音日期',
    `audio_url`        VARCHAR(255) NOT NULL                COMMENT '音频文件相对路径',
    `transcribed_text` TEXT         DEFAULT NULL            COMMENT '语音转文字结果',
    `food_entities`    TEXT         DEFAULT NULL            COMMENT '解析出的食物实体(JSON)',
    `duration_seconds` INT          DEFAULT 0               COMMENT '录音时长(秒)',
    `meal_type`        VARCHAR(16)  DEFAULT NULL            COMMENT '餐次（用户确认后回填）',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `record_date`),
    CONSTRAINT `fk_voice_record_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音录音记录表';

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 以下数据参考 backend/src/main/resources/data.sql
-- ============================================================

-- 演示用户
INSERT INTO `users` (`id`, `nickname`, `username`, `password_hash`, `created_at`, `updated_at`)
VALUES (1, '健康达人', 'demo', '$2a$10$OxyEfHbICAPSvz6PJNMnVOVM3dDyFigl0zD9mEEN5UzAieEZtsRH.', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `nickname` = VALUES(`nickname`),
    `password_hash` = VALUES(`password_hash`),
    `updated_at` = VALUES(`updated_at`);

-- 用户健康档案
INSERT INTO `user_profile` (`id`, `user_id`, `age`, `height_cm`, `weight_kg`, `goal`, `taboo`, `taste_preference`, `warning_profile`, `gender`, `avatar_url`)
VALUES (1, 1, 25, 170.00, 65.00, '减脂', '海鲜', '清淡,中式', '无', '男', NULL)
ON DUPLICATE KEY UPDATE
    `id` = `id`;

-- 标准食物营养基线（每单位）
INSERT IGNORE INTO `food_item` (`id`, `name`, `category`, `unit`, `calorie`, `protein`, `fat`, `carbohydrate`, `sugar`, `sodium`) VALUES
(1,  '米饭',   '主食',   '碗', 116.00, 2.60,  0.30,  25.90, 0.10,  2.00),
(2,  '面条',   '主食',   '碗', 110.00, 3.40,  0.50,  22.80, 0.50,  50.00),
(3,  '馒头',   '主食',   '个', 221.00, 7.00,  1.10,  44.20, 0.30,  165.00),
(4,  '鸡腿',   '肉类',   '个', 181.00, 20.20, 11.00, 0.00,  0.00,  80.00),
(5,  '猪瘦肉', '肉类',   '份', 143.00, 20.30, 6.20,  1.50,  0.00,  60.00),
(6,  '红烧肉', '肉类',   '份', 298.00, 16.50, 25.60, 3.00,  0.50,  200.00),
(7,  '鸡蛋',   '蛋类',   '个', 144.00, 13.30, 8.80,  2.80,  0.00,  131.00),
(8,  '牛奶',   '乳制品', '杯', 65.00,  3.00,  3.60,  4.80,  4.80,  40.00),
(9,  '炒青菜', '蔬菜',   '份', 25.00,  2.00,  1.00,  3.00,  1.00,  150.00),
(10, '西兰花', '蔬菜',   '份', 34.00,  2.82,  0.37,  6.64,  1.70,  33.00),
(11, '豆腐',   '豆制品', '份', 81.00,  8.10,  3.70,  4.20,  0.00,  2.00),
(12, '苹果',   '水果',   '个', 52.00,  0.26,  0.17,  13.80, 10.30, 1.00),
(13, '香蕉',   '水果',   '根', 89.00,  1.09,  0.33,  22.80, 12.20, 1.00),
(14, '橙子',   '水果',   '个', 47.00,  0.94,  0.12,  11.80, 9.40,  0.00),
(15, '三文鱼', '海鲜',   '份', 208.00, 20.40, 13.40, 0.00,  0.00,  59.00),
(16, '虾仁',   '海鲜',   '份', 93.00,  18.60, 1.50,  0.80,  0.00,  410.00),
(17, '酸奶',   '乳制品', '杯', 72.00,  2.60,  2.70,  9.40,  9.40,  40.00),
(18, '红薯',   '主食',   '个', 86.00,  1.60,  0.10,  20.10, 4.20,  55.00),
(19, '玉米',   '主食',   '根', 112.00, 4.00,  1.20,  22.80, 3.20,  15.00),
(20, '黄瓜',   '蔬菜',   '根', 15.00,  0.65,  0.11,  3.63,  1.70,  2.00);

-- 食谱数据
INSERT IGNORE INTO `recipe` (`id`, `name`, `ingredients`, `steps`, `tags`, `calorie`, `protein`, `fat`, `carbohydrate`) VALUES
(1, '鸡胸肉沙拉',
 '鸡胸肉150g, 生菜50g, 番茄1个, 黄瓜半根, 橄榄油5ml, 柠檬汁少许',
 '1. 鸡胸肉煮熟切块 2. 蔬菜洗净切好 3. 混合所有食材 4. 淋上橄榄油和柠檬汁',
 '高蛋白,低卡,清淡', 250.00, 35.00, 8.00, 12.00),

(2, '番茄炒蛋',
 '鸡蛋2个, 番茄2个, 葱少许, 盐适量, 油10ml',
 '1. 鸡蛋打散 2. 番茄切块 3. 锅中倒油炒熟鸡蛋 4. 加番茄翻炒 5. 加盐调味',
 '低卡,家常,快手', 220.00, 15.00, 14.00, 8.00),

(3, '清蒸三文鱼',
 '三文鱼200g, 姜片, 葱段, 蒸鱼豉油10ml',
 '1. 三文鱼洗净抹盐 2. 放姜片葱段 3. 上锅蒸8分钟 4. 淋蒸鱼豉油',
 '高蛋白,低卡,低脂', 280.00, 30.00, 16.00, 2.00),

(4, '西兰花炒虾仁',
 '西兰花200g, 虾仁150g, 蒜末, 盐适量, 油10ml',
 '1. 西兰花焯水 2. 虾仁洗净 3. 锅中倒油炒香蒜末 4. 加入虾仁翻炒 5. 加西兰花炒匀',
 '高蛋白,低脂,减脂', 200.00, 28.00, 5.00, 12.00),

(5, '豆腐青菜汤',
 '嫩豆腐200g, 青菜100g, 姜片, 盐适量',
 '1. 豆腐切块 2. 青菜洗净 3. 水开下豆腐姜片 4. 煮5分钟后加青菜 5. 加盐调味',
 '低卡,清淡,素食', 80.00, 8.00, 3.00, 6.00),

(6, '牛肉面',
 '面条150g, 牛肉100g, 青菜50g, 牛肉汤, 葱花香菜',
 '1. 牛肉切块炖烂 2. 面条煮熟 3. 青菜焯水 4. 碗中放面条、牛肉、青菜 5. 浇入牛肉汤',
 '高蛋白,家常,中式', 450.00, 25.00, 12.00, 60.00),

(7, '酸奶水果杯',
 '酸奶200ml, 苹果半个, 香蕉半根, 燕麦片30g',
 '1. 水果切块 2. 杯中铺一层燕麦 3. 加一层酸奶 4. 放上水果块 5. 重复层层叠加',
 '低糖,快手,早餐', 250.00, 8.00, 6.00, 40.00),

(8, '红薯小米粥',
 '红薯100g, 小米50g, 清水适量',
 '1. 红薯去皮切块 2. 小米淘洗 3. 锅中加水放入小米和红薯 4. 慢火熬煮30分钟',
 '低脂,低糖,暖胃,早餐', 180.00, 4.00, 1.00, 38.00),

(9, '彩椒炒鸡丁',
 '鸡胸肉200g, 彩椒2个, 蒜末, 生抽, 盐适量, 油10ml',
 '1. 鸡胸肉切丁腌制 2. 彩椒切块 3. 锅中倒油炒鸡丁 4. 加彩椒翻炒 5. 调味出锅',
 '高蛋白,低卡,减脂', 260.00, 35.00, 8.00, 14.00),

(10, '紫菜蛋花汤',
 '鸡蛋1个, 紫菜5g, 虾皮少许, 葱花, 盐适量',
 '1. 鸡蛋打散 2. 水开下紫菜和虾皮 3. 淋入蛋液 4. 加盐调味 5. 撒葱花',
 '低卡,清淡,快手', 60.00, 6.00, 3.00, 2.00);

-- 演示用户预警规则
INSERT IGNORE INTO `alert_rule` (`id`, `user_id`, `nutrient_type`, `threshold`, `enabled`, `updated_at`) VALUES
(1, 1, 'calorie', 2000.00, TRUE, NOW()),
(2, 1, 'sugar',    50.00, TRUE, NOW()),
(3, 1, 'sodium',  2400.00, TRUE, NOW());

-- 演示饮食记录（含营养快照）
INSERT IGNORE INTO `diet_record` (`id`, `user_id`, `food_id`, `food_name`, `meal_type`, `amount`, `source`, `calorie`, `protein`, `fat`, `carbohydrate`, `sugar`, `sodium`, `record_time`, `created_at`) VALUES
(1, 1, 1, '米饭',   '早餐', 1.00, 'manual', 116.00, 2.60,  0.30,  25.90, 0.10, 2.00,   '2026-06-12 08:30:00', NOW()),
(2, 1, 7, '鸡蛋',   '早餐', 1.00, 'manual', 144.00, 13.30, 8.80,  2.80,  0.00, 131.00, '2026-06-12 08:30:00', NOW()),
(3, 1, 8, '牛奶',   '早餐', 1.00, 'manual', 65.00,  3.00,  3.60,  4.80,  4.80, 40.00,  '2026-06-12 08:30:00', NOW()),
(4, 1, 1, '米饭',   '午餐', 1.00, 'manual', 116.00, 2.60,  0.30,  25.90, 0.10, 2.00,   '2026-06-12 12:00:00', NOW()),
(5, 1, 4, '鸡腿',   '午餐', 1.00, 'manual', 181.00, 20.20, 11.00, 0.00,  0.00, 80.00,  '2026-06-12 12:00:00', NOW()),
(6, 1, 9, '炒青菜', '午餐', 1.00, 'manual', 25.00,  2.00,  1.00,  3.00,  1.00, 150.00, '2026-06-12 12:00:00', NOW());
