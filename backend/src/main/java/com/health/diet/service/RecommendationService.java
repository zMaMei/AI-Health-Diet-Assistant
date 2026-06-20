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
                .orElseThrow(() -> new IllegalArgumentException("请先在\"我的\"页面完善个人资料"));

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
