package com.health.diet.service;

import com.health.diet.dto.vo.HealthScoreVO;
import com.health.diet.entity.DietRecord;
import com.health.diet.entity.NutritionRecord;
import com.health.diet.entity.UserProfile;
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

    public HealthScoreService(DietRecordRepository dietRecordRepository,
                              NutritionRecordRepository nutritionRecordRepository,
                              UserProfileRepository userProfileRepository) {
        this.dietRecordRepository = dietRecordRepository;
        this.nutritionRecordRepository = nutritionRecordRepository;
        this.userProfileRepository = userProfileRepository;
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

            // Calculate score (0-100)
            BigDecimal score = calculateScore(calorie, protein, fat, carb, sugar, sodium, profile);
            vo.setScore(score.setScale(1, RoundingMode.HALF_UP));

            // Generate strengths, risks, suggestions
            List<String> strengths = new ArrayList<>();
            List<String> risks = new ArrayList<>();
            List<String> suggestions = new ArrayList<>();

            BigDecimal calorieGoal = profile != null && "减脂".equals(profile.getGoal())
                    ? new BigDecimal("1600") : new BigDecimal("2000");

            if (calorie.compareTo(calorieGoal.multiply(new BigDecimal("0.9"))) <= 0) {
                strengths.add("热量控制良好");
            } else if (calorie.compareTo(calorieGoal) > 0) {
                risks.add("热量摄入偏高");
                suggestions.add("建议减少高热量食物摄入，增加运动量");
            }

            BigDecimal proteinGoal = profile != null && "增肌".equals(profile.getGoal())
                    ? new BigDecimal("120") : new BigDecimal("60");
            if (protein.compareTo(proteinGoal) >= 0) {
                strengths.add("蛋白质摄入充足");
            } else {
                risks.add("蛋白质摄入不足");
                suggestions.add("建议增加豆制品、鸡蛋、鱼虾等优质蛋白");
            }

            if (sugar.compareTo(new BigDecimal("50")) > 0) {
                risks.add("糖分摄入偏高");
                suggestions.add("建议减少甜食和含糖饮料");
            } else {
                strengths.add("糖分控制良好");
            }

            if (sodium.compareTo(new BigDecimal("2400")) > 0) {
                risks.add("钠摄入偏高");
                suggestions.add("建议减少盐分摄入，少吃腌制食品");
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

    private BigDecimal calculateScore(BigDecimal calorie, BigDecimal protein,
                                      BigDecimal fat, BigDecimal carb,
                                      BigDecimal sugar, BigDecimal sodium,
                                      UserProfile profile) {
        BigDecimal score = new BigDecimal("80");
        BigDecimal calorieGoal = profile != null && "减脂".equals(profile.getGoal())
                ? new BigDecimal("1600") : new BigDecimal("2000");

        // Calorie: +/- 10
        BigDecimal diff = calorie.subtract(calorieGoal).abs();
        if (calorie.compareTo(calorieGoal) > 0) {
            score = score.subtract(diff.divide(calorieGoal, 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("10")));
        }

        // Sugar: - up to 10
        if (sugar.compareTo(new BigDecimal("50")) > 0) {
            score = score.subtract(new BigDecimal("5"));
        }

        // Sodium: - up to 10
        if (sodium.compareTo(new BigDecimal("2400")) > 0) {
            score = score.subtract(new BigDecimal("5"));
        }

        // Protein bonus: + up to 10
        BigDecimal proteinGoal = profile != null && "增肌".equals(profile.getGoal())
                ? new BigDecimal("120") : new BigDecimal("60");
        if (protein.compareTo(proteinGoal) >= 0) {
            score = score.add(new BigDecimal("5"));
        }

        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
