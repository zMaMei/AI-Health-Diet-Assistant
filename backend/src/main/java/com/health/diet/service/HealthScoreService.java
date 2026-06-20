package com.health.diet.service;

import com.health.diet.dto.vo.HealthScoreVO;
import com.health.diet.entity.AlertRule;
import com.health.diet.entity.DietRecord;
import com.health.diet.entity.NutritionRecord;
import com.health.diet.entity.UserProfile;
import com.health.diet.repository.AlertRuleRepository;
import com.health.diet.repository.DietRecordRepository;
import com.health.diet.repository.NutritionRecordRepository;
import com.health.diet.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class HealthScoreService {

    private final DietRecordRepository dietRecordRepository;
    private final NutritionRecordRepository nutritionRecordRepository;
    private final UserProfileRepository userProfileRepository;
    private final AlertRuleRepository alertRuleRepository;

    public HealthScoreService(DietRecordRepository dietRecordRepository,
                              NutritionRecordRepository nutritionRecordRepository,
                              UserProfileRepository userProfileRepository,
                              AlertRuleRepository alertRuleRepository) {
        this.dietRecordRepository = dietRecordRepository;
        this.nutritionRecordRepository = nutritionRecordRepository;
        this.userProfileRepository = userProfileRepository;
        this.alertRuleRepository = alertRuleRepository;
    }

    public HealthScoreVO getDailyScore(Long userId, LocalDate date) {
        List<DietRecord> records = dietRecordRepository.findByUserAndDate(userId, date);

        // Check meal count - at least 2 meals required
        Set<String> mealTypes = new HashSet<>();
        for (DietRecord r : records) {
            mealTypes.add(r.getMealType());
        }

        HealthScoreVO vo = new HealthScoreVO();

        if (mealTypes.size() < 2) {
            vo.setScore(null);
            vo.setSuggestions(List.of("记录更多餐次后可获得评分（至少2餐）"));
            vo.setStrengths(List.of());
            vo.setRisks(List.of());
        } else {
            // 直接从 diet_record 营养快照汇总
            BigDecimal calorie = BigDecimal.ZERO;
            BigDecimal protein = BigDecimal.ZERO;
            BigDecimal fat = BigDecimal.ZERO;
            BigDecimal carb = BigDecimal.ZERO;
            BigDecimal sugar = BigDecimal.ZERO;
            BigDecimal sodium = BigDecimal.ZERO;

            for (DietRecord record : records) {
                calorie = calorie.add(nvl(record.getCalorie()));
                protein = protein.add(nvl(record.getProtein()));
                fat = fat.add(nvl(record.getFat()));
                carb = carb.add(nvl(record.getCarbohydrate()));
                sugar = sugar.add(nvl(record.getSugar()));
                sodium = sodium.add(nvl(record.getSodium()));
            }

            UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
            Map<String, BigDecimal> thresholds = getUserThresholds(userId, profile);

            // Calculate score (0-100) using user's alert_rule thresholds
            BigDecimal score = calculateScore(calorie, protein, fat, carb, sugar, sodium, thresholds);
            vo.setScore(score.setScale(1, RoundingMode.HALF_UP));

            // Generate strengths, risks, suggestions — all based on user thresholds
            List<String> strengths = new ArrayList<>();
            List<String> risks = new ArrayList<>();
            List<String> suggestions = new ArrayList<>();

            BigDecimal calorieGoal = thresholds.get("calorie");
            if (calorie.compareTo(calorieGoal.multiply(new BigDecimal("0.9"))) <= 0) {
                strengths.add("热量控制良好");
            } else if (calorie.compareTo(calorieGoal) > 0) {
                risks.add("热量摄入偏高（" + calorie.setScale(0, RoundingMode.HALF_UP) + "/" + calorieGoal.setScale(0, RoundingMode.HALF_UP) + "kcal）");
                suggestions.add("建议减少高热量食物摄入，增加运动量");
            }

            BigDecimal proteinGoal = thresholds.get("protein");
            if (protein.compareTo(proteinGoal) >= 0) {
                strengths.add("蛋白质摄入充足");
            } else {
                risks.add("蛋白质摄入不足（" + protein.setScale(0, RoundingMode.HALF_UP) + "/" + proteinGoal.setScale(0, RoundingMode.HALF_UP) + "g）");
                suggestions.add("建议增加豆制品、鸡蛋、鱼虾等优质蛋白");
            }

            BigDecimal fatGoal = thresholds.get("fat");
            if (fat.compareTo(fatGoal) > 0) {
                risks.add("脂肪摄入偏高（" + fat.setScale(0, RoundingMode.HALF_UP) + "/" + fatGoal.setScale(0, RoundingMode.HALF_UP) + "g）");
                suggestions.add("建议控制油炸食品和肥肉摄入");
            } else if (fat.compareTo(BigDecimal.ZERO) > 0) {
                strengths.add("脂肪摄入适中");
            }

            BigDecimal carbGoal = thresholds.get("carb");
            if (carb.compareTo(carbGoal) > 0) {
                risks.add("碳水摄入偏高");
                suggestions.add("建议适当减少精制主食，增加粗粮比例");
            } else if (carb.compareTo(carbGoal.multiply(new BigDecimal("0.5"))) >= 0) {
                strengths.add("碳水摄入合理");
            }

            BigDecimal sugarGoal = thresholds.get("sugar");
            if (sugar.compareTo(sugarGoal) > 0) {
                risks.add("糖分摄入偏高");
                suggestions.add("建议减少甜食和含糖饮料");
            } else if (sugar.compareTo(BigDecimal.ZERO) > 0) {
                strengths.add("糖分控制良好");
            }

            BigDecimal sodiumGoal = thresholds.get("sodium");
            if (sodium.compareTo(sodiumGoal) > 0) {
                risks.add("钠摄入偏高");
                suggestions.add("建议减少盐分摄入，少吃腌制食品");
            } else if (sodium.compareTo(BigDecimal.ZERO) > 0) {
                strengths.add("钠摄入适中");
            }

            if (suggestions.isEmpty()) {
                suggestions.add("今日饮食非常健康，继续保持！");
            }

            vo.setStrengths(strengths);
            vo.setRisks(risks);
            vo.setSuggestions(suggestions);

            // Update score in nutrition record
            nutritionRecordRepository.findByUserIdAndRecordDate(userId, date).ifPresent(nr -> {
                nr.setScore(score.setScale(1, RoundingMode.HALF_UP));
                nutritionRecordRepository.save(nr);
            });
        }

        // Get history
        LocalDate weekAgo = date.minusDays(6);
        List<NutritionRecord> historyRecords = nutritionRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, weekAgo, date);

        List<HealthScoreVO.ScoreHistoryPoint> history = new ArrayList<>();
        for (NutritionRecord nr : historyRecords) {
            if (nr.getScore() != null) {
                history.add(new HealthScoreVO.ScoreHistoryPoint(
                        nr.getRecordDate().toString(), nr.getScore()));
            }
        }
        vo.setHistory(history);

        return vo;
    }

    /**
     * 从 alert_rule 表读取用户设定的阈值，缺失时使用 profile.goal 默认值。
     */
    private Map<String, BigDecimal> getUserThresholds(Long userId, UserProfile profile) {
        List<AlertRule> rules = alertRuleRepository.findByUserId(userId);
        Map<String, BigDecimal> thresholds = new HashMap<>();

        // 从 alert_rule 读取已设定值
        for (AlertRule rule : rules) {
            if (rule.getEnabled()) {
                thresholds.put(rule.getNutrientType(), rule.getThreshold());
            }
        }

        // 缺失时使用 profile.goal 默认值
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

    private BigDecimal calculateScore(BigDecimal calorie, BigDecimal protein,
                                      BigDecimal fat, BigDecimal carb,
                                      BigDecimal sugar, BigDecimal sodium,
                                      Map<String, BigDecimal> goals) {
        BigDecimal score = new BigDecimal("80");
        BigDecimal calorieGoal = goals.get("calorie");

        // Calorie: 超标扣分
        if (calorie.compareTo(calorieGoal) > 0) {
            BigDecimal diff = calorie.subtract(calorieGoal);
            BigDecimal penalty = diff.divide(calorieGoal, 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("10"));
            score = score.subtract(penalty.min(new BigDecimal("10")));
        }

        // Sugar: 超标 -5
        BigDecimal sugarGoal = goals.get("sugar");
        if (sugar.compareTo(sugarGoal) > 0) {
            score = score.subtract(new BigDecimal("5"));
        }

        // Sodium: 超标 -5
        BigDecimal sodiumGoal = goals.get("sodium");
        if (sodium.compareTo(sodiumGoal) > 0) {
            score = score.subtract(new BigDecimal("5"));
        }

        // Fat: 超标 -3
        BigDecimal fatGoal = goals.get("fat");
        if (fat.compareTo(fatGoal) > 0) {
            score = score.subtract(new BigDecimal("3"));
        }

        // Protein: 达标 +5, 严重不足 -3
        BigDecimal proteinGoal = goals.get("protein");
        if (protein.compareTo(proteinGoal) >= 0) {
            score = score.add(new BigDecimal("5"));
        } else if (protein.compareTo(proteinGoal.multiply(new BigDecimal("0.5"))) < 0) {
            score = score.subtract(new BigDecimal("3"));
        }

        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
