package com.health.diet.service;

import com.health.diet.adapter.RecommendationAdapter;
import com.health.diet.adapter.RecommendationAdapter.RecommendedRecipe;
import com.health.diet.adapter.RecommendationAdapter.RecommendationResult;
import com.health.diet.dto.vo.RecommendationPageVO;
import com.health.diet.dto.vo.RecommendationVO;
import com.health.diet.entity.AlertRule;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
     * 获取今日推荐页面数据（含用户阈值）。
     */
    public RecommendationPageVO getTodayPage(Long userId) {
        RecommendationPageVO page = new RecommendationPageVO();
        page.setRecommendations(recommendToday(userId));
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        page.setThresholds(profile != null ? getUserThresholds(userId, profile) : Map.of());
        return page;
    }

    /**
     * 强制刷新今日推荐页面数据（含用户阈值）。
     */
    public RecommendationPageVO refreshTodayPage(Long userId) {
        RecommendationPageVO page = new RecommendationPageVO();
        page.setRecommendations(refreshToday(userId));
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        page.setThresholds(profile != null ? getUserThresholds(userId, profile) : Map.of());
        return page;
    }

    /**
     * 今日推荐：有缓存返回缓存，无缓存 AI 生成。
     */
    /* 事务保护 */
    @Transactional
    public List<RecommendationVO> recommendToday(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        /* 缓存推荐结果 */
        // 1. Check today's cache
        try {
            List<Recommendation> cached = recommendationRepository
                    .findByUserIdAndCreatedAtBetween(userId, todayStart, todayEnd);
            if (!cached.isEmpty()) {
                // 只保留最近5条（清理旧系统遗留的冗余缓存）
                if (cached.size() > 5) {
                    log.info("今日缓存过多({}条)，清理旧记录", cached.size());
                    cached.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    List<Recommendation> toDelete = cached.subList(5, cached.size());
                    recommendationRepository.deleteAll(toDelete);
                    cached = cached.subList(0, 5);
                }
                log.info("今日推荐命中缓存: userId={}, count={}", userId, cached.size());
                return cached.stream().map(this::toVO).toList();
            }
        } catch (Exception e) {
            log.warn("读取今日推荐缓存失败，将重新生成: {}", e.getMessage());
        }

        return generateAndSave(userId);
    }

    /**
     * 强制刷新：删除当天推荐，AI 重新生成。
     */
    /* 事务保护 */
    @Transactional
    public List<RecommendationVO> refreshToday(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        /* 清除缓存 */
        try {
            recommendationRepository.deleteByUserIdAndCreatedAtBetween(userId, todayStart, todayEnd);
            log.info("已清除今日推荐缓存: userId={}", userId);
        } catch (Exception e) {
            log.warn("清除今日推荐缓存失败，尝试逐条删除: {}", e.getMessage());
            List<Recommendation> cached = recommendationRepository
                    .findByUserIdAndCreatedAtBetween(userId, todayStart, todayEnd);
            if (!cached.isEmpty()) {
                recommendationRepository.deleteAll(cached);
                log.info("逐条删除今日缓存完成: {}条", cached.size());
            }
        }
        // 换一批：AI 直接创造新菜谱（不限菜谱库）
        return generateFresh(userId);
    }

    /* 事务保护 */
    @Transactional
    private List<RecommendationVO> generateAndSave(Long userId) {
        // 1. Load profile (required)
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("请先在\"我的\"页面完善个人资料"));

        // 2. Load thresholds
        Map<String, BigDecimal> thresholds = getUserThresholds(userId, profile);

        // 3. Load today's intake
        Map<String, BigDecimal> intake = getTodayIntake(userId);

        // 4. Load all recipes
        List<Recipe> allRecipes;
        try {
            allRecipes = recipeRepository.findAll();
        } catch (Exception e) {
            log.error("加载菜谱库失败: {}", e.getMessage());
            return List.of();
        }
        if (allRecipes.isEmpty()) {
            log.warn("菜谱库为空，无法生成推荐");
            return List.of();
        }

        /* AI推荐优先 */
        // 5. Try AI generation, fallback to rules
        try {
            return generateByAI(userId, profile, thresholds, intake, allRecipes);
        } catch (Exception e) {
            /* 规则引擎降级 */
            log.warn("AI 推荐失败，降级为规则引擎: {}", e.getMessage());
            try {
                return generateByRules(userId, profile, allRecipes);
            } catch (Exception e2) {
                log.error("规则引擎推荐也失败: {}", e2.getMessage());
                return List.of();
            }
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

        /* 计算营养缺口 */
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
     * AI 直接创造新菜谱（换一批用），不限菜谱库。
     */
    /* 事务保护 */
    @Transactional
    private List<RecommendationVO> generateFresh(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("请先在\"我的\"页面完善个人资料"));
        Map<String, BigDecimal> thresholds = getUserThresholds(userId, profile);
        Map<String, BigDecimal> intake = getTodayIntake(userId);

        // 加载菜谱库作为风格参考（但不限定从其中选）
        List<Recipe> existingRecipes = recipeRepository.findAll();
        StringBuilder refSummary = new StringBuilder();
        for (Recipe r : existingRecipes.stream().limit(20).toList()) {
            refSummary.append(String.format("%s(%dkcal/%sg蛋白), ", r.getName(),
                    nvl(r.getCalorie()).intValue(), nvl(r.getProtein()).intValue()));
        }

        String gaps = buildGapDescription(thresholds, intake);

        String prompt = String.format("""
            你是一位创意营养厨师。请根据用户画像和营养缺口，设计5道全新的一人食菜谱。
            菜谱应填补营养缺口、避开忌口、匹配口味偏好。每道菜给出完整的食材、做法、营养数据。

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

            ## 已有菜谱风格参考（不要重复，创造新的）
            %s

            请严格返回JSON格式：
            {"recipes": [{
              "name": "菜名",
              "ingredients": "食材1用量, 食材2用量, ...",
              "steps": "1. 步骤一\\n2. 步骤二\\n3. 步骤三",
              "tags": "标签1,标签2",
              "calorie": 300,
              "protein": 25,
              "fat": 12,
              "carbohydrate": 30,
              "sugar": 5,
              "sodium": 400,
              "reason": "个性化推荐理由（提及填补了什么缺口、如何匹配用户目标）",
              "score": 85
            }, ...]}
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
            refSummary.length() > 0 ? refSummary.toString() : "无"
        );

        try {
            RecommendationAdapter.FreshResult result = recommendationAdapter.analyzeFresh(prompt);

            List<RecommendationVO> vos = new ArrayList<>();
            for (RecommendationAdapter.FreshRecipeData fr : result.recipes()) {
                // 检查是否与已有菜谱重名，重名则跳过
                boolean duplicate = existingRecipes.stream()
                        .anyMatch(r -> r.getName().equals(fr.name()));
                if (duplicate) {
                    log.info("AI 生成的菜谱\"{}\"与已有菜谱重名，跳过", fr.name());
                    continue;
                }

                /* 创建新菜谱 */
                // 保存新菜谱
                Recipe newRecipe = new Recipe();
                newRecipe.setName(fr.name());
                newRecipe.setIngredients(fr.ingredients() != null ? fr.ingredients() : "");
                newRecipe.setSteps(fr.steps() != null ? fr.steps() : "");
                newRecipe.setTags(fr.tags() != null ? fr.tags() : "");
                newRecipe.setCalorie(fr.calorie() != null ? fr.calorie() : BigDecimal.ZERO);
                newRecipe.setProtein(fr.protein() != null ? fr.protein() : BigDecimal.ZERO);
                newRecipe.setFat(fr.fat() != null ? fr.fat() : BigDecimal.ZERO);
                newRecipe.setCarbohydrate(fr.carbohydrate() != null ? fr.carbohydrate() : BigDecimal.ZERO);
                newRecipe.setSugar(fr.sugar() != null ? fr.sugar() : BigDecimal.ZERO);
                newRecipe.setSodium(fr.sodium() != null ? fr.sodium() : BigDecimal.ZERO);
                recipeRepository.save(newRecipe);

                // 创建推荐记录
                Recommendation rec = new Recommendation();
                rec.setUserId(userId);
                rec.setRecipeId(newRecipe.getId());
                rec.setReason(fr.reason() != null ? fr.reason() : "AI 个性化推荐");
                rec.setScore(fr.score() != null ? fr.score() : BigDecimal.valueOf(50));
                recommendationRepository.save(rec);

                vos.add(toVO(rec, newRecipe));
            }

            log.info("AI 创造菜谱完成: userId={}, count={}", userId, vos.size());
            if (!vos.isEmpty()) return vos;
        } catch (Exception e) {
            log.warn("AI 创造菜谱失败，降级为规则引擎: {}", e.getMessage());
        }

        // 降级：从菜谱库随机选
        try {
            List<Recipe> allRecipes = recipeRepository.findAll();
            if (!allRecipes.isEmpty()) {
                return generateByRules(userId, profile, allRecipes);
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    /**
     * 降级：规则引擎推荐（保留旧逻辑），引入随机性避免换一批无变化。
     */
    private List<RecommendationVO> generateByRules(Long userId, UserProfile profile,
                                                    List<Recipe> allRecipes) {
        List<ScoredRecipe> scored = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
            BigDecimal score = scoreRecipe(recipe, profile);
            String reason = generateReason(recipe, profile);
            scored.add(new ScoredRecipe(recipe, score, reason));
        }

        // 从高分段随机选取，保证每次结果有变化
        scored.sort((a, b) -> b.score.compareTo(a.score));
        // 取前 30% 或至少 15 道作为候选池，随机选 5 道
        int poolSize = Math.max(15, scored.size() * 30 / 100);
        poolSize = Math.min(poolSize, scored.size());
        /* 随机选取增加多样性 */
        List<ScoredRecipe> pool = new ArrayList<>(scored.subList(0, poolSize));
        Collections.shuffle(pool);
        List<ScoredRecipe> top = pool.stream().limit(5).toList();

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

    /* 食谱评分 */
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

        /* 过滤忌口菜谱 */
        if (profile.getTaboo() != null && recipe.getTags() != null) {
            for (String taboo : profile.getTaboo().split(",")) {
                if (recipe.getTags().contains(taboo.trim()))
                    score = score.subtract(new BigDecimal("50"));
            }
        }

        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /* 生成推荐理由 */
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
        try {
            Recipe recipe = recipeRepository.findById(rec.getRecipeId()).orElse(null);
            return toVO(rec, recipe);
        } catch (Exception e) {
            log.warn("加载推荐关联食谱失败 recipeId={}: {}", rec.getRecipeId(), e.getMessage());
            RecommendationVO vo = new RecommendationVO();
            vo.setId(rec.getId());
            vo.setRecipeId(rec.getRecipeId());
            vo.setRecipeName("(食谱数据加载失败)");
            vo.setReason(rec.getReason());
            vo.setMatchScore(rec.getScore());
            return vo;
        }
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
