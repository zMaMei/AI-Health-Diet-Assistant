package com.health.diet.service;

import com.health.diet.dto.vo.NutritionDailyVO;
import com.health.diet.entity.AlertRule;
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
public class NutritionService {

    private final DietRecordRepository dietRecordRepository;
    private final NutritionRecordRepository nutritionRecordRepository;
    private final UserProfileRepository userProfileRepository;
    private final AlertRuleRepository alertRuleRepository;

    private static final BigDecimal DEFAULT_CALORIE_GOAL = new BigDecimal("2000");
    private static final BigDecimal DEFAULT_PROTEIN_GOAL = new BigDecimal("60");
    private static final BigDecimal DEFAULT_FAT_GOAL = new BigDecimal("65");
    private static final BigDecimal DEFAULT_CARB_GOAL = new BigDecimal("300");

    public NutritionService(DietRecordRepository dietRecordRepository,
                            NutritionRecordRepository nutritionRecordRepository,
                            UserProfileRepository userProfileRepository,
                            AlertRuleRepository alertRuleRepository) {
        this.dietRecordRepository = dietRecordRepository;
        this.nutritionRecordRepository = nutritionRecordRepository;
        this.userProfileRepository = userProfileRepository;
        this.alertRuleRepository = alertRuleRepository;
    }

    public NutritionDailyVO getDaily(Long userId, LocalDate date) {
        NutritionDailyVO vo = new NutritionDailyVO();

        // 直接从 diet_record 表 SUM 当日营养快照
        BigDecimal[] sums = dietRecordRepository.sumNutrition(userId, date);
        BigDecimal calorie = sums[0], protein = sums[1], fat = sums[2],
                     carb = sums[3], sugar = sums[4], sodium = sums[5];

        vo.setCalorieTotal(calorie.setScale(2, RoundingMode.HALF_UP));
        vo.setProteinTotal(protein.setScale(2, RoundingMode.HALF_UP));
        vo.setFatTotal(fat.setScale(2, RoundingMode.HALF_UP));
        vo.setCarbohydrateTotal(carb.setScale(2, RoundingMode.HALF_UP));
        vo.setSugarTotal(sugar.setScale(2, RoundingMode.HALF_UP));
        vo.setSodiumTotal(sodium.setScale(2, RoundingMode.HALF_UP));

        // Get goals: alert_rule 阈值优先 → profile.goal 默认值 → 系统默认值
        Map<String, BigDecimal> thresholds = getUserThresholds(userId);

        vo.setCalorieGoal(thresholds.getOrDefault("calorie", DEFAULT_CALORIE_GOAL));
        vo.setProteinGoal(thresholds.getOrDefault("protein", DEFAULT_PROTEIN_GOAL));
        vo.setFatGoal(thresholds.getOrDefault("fat", DEFAULT_FAT_GOAL));
        vo.setCarbohydrateGoal(thresholds.getOrDefault("carb", DEFAULT_CARB_GOAL));

        // Get weekly trend
        LocalDate weekAgo = date.minusDays(6);
        List<NutritionRecord> weekRecords = nutritionRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, weekAgo, date);

        List<NutritionDailyVO.NutritionTrendPoint> trend = new ArrayList<>();
        for (LocalDate d = weekAgo; !d.isAfter(date); d = d.plusDays(1)) {
            LocalDate finalD = d;
            NutritionRecord nr = weekRecords.stream()
                    .filter(r -> r.getRecordDate().equals(finalD))
                    .findFirst().orElse(null);
            trend.add(new NutritionDailyVO.NutritionTrendPoint(
                    d.toString(),
                    nr != null ? nr.getCalorieTotal() : BigDecimal.ZERO
            ));
        }
        vo.setTrend(trend);

        // Generate suggestion
        vo.setSuggestion(generateSuggestion(vo));

        // Save nutrition record
        saveNutritionRecord(userId, date, vo);

        return vo;
    }

    /**
     * 从 alert_rule 表读取用户设定的阈值，缺失时使用 profile.goal 默认值。
     */
    private Map<String, BigDecimal> getUserThresholds(Long userId) {
        List<AlertRule> rules = alertRuleRepository.findByUserId(userId);
        Map<String, BigDecimal> thresholds = new HashMap<>();

        for (AlertRule rule : rules) {
            if (rule.getEnabled()) {
                thresholds.put(rule.getNutrientType(), rule.getThreshold());
            }
        }

        // 缺失时使用 profile.goal 默认值
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        boolean isCut = profile != null && "减脂".equals(profile.getGoal());
        boolean isGain = profile != null && "增肌".equals(profile.getGoal());

        thresholds.putIfAbsent("calorie", isCut ? new BigDecimal("1600") : isGain ? new BigDecimal("2500") : DEFAULT_CALORIE_GOAL);
        thresholds.putIfAbsent("protein", isGain ? new BigDecimal("120") : isCut ? new BigDecimal("70") : DEFAULT_PROTEIN_GOAL);
        thresholds.putIfAbsent("fat", isCut ? new BigDecimal("50") : isGain ? new BigDecimal("70") : DEFAULT_FAT_GOAL);
        thresholds.putIfAbsent("carb", isCut ? new BigDecimal("200") : isGain ? new BigDecimal("350") : DEFAULT_CARB_GOAL);

        return thresholds;
    }

    private String generateSuggestion(NutritionDailyVO vo) {
        if (vo.getCalorieTotal().compareTo(BigDecimal.ZERO) == 0) {
            return "今日暂无饮食记录，开始记录您的饮食吧！";
        }
        List<String> tips = new ArrayList<>();
        if (vo.getCalorieGoal() != null && vo.getCalorieTotal().compareTo(vo.getCalorieGoal()) > 0) {
            tips.add("热量摄入已超标，建议适当减少高热量食物");
        }
        if (vo.getProteinGoal() != null && vo.getProteinTotal().compareTo(vo.getProteinGoal()) < 0) {
            tips.add("蛋白质摄入不足，建议增加豆制品、鸡蛋或瘦肉");
        }
        if (vo.getFatGoal() != null && vo.getFatTotal().compareTo(vo.getFatGoal()) > 0) {
            tips.add("脂肪摄入偏高，建议控制油炸食品和肥肉");
        }
        if (tips.isEmpty()) {
            tips.add("今日营养摄入均衡，继续保持！");
        }
        return String.join("；", tips);
    }

    private void saveNutritionRecord(Long userId, LocalDate date, NutritionDailyVO vo) {
        NutritionRecord nr = nutritionRecordRepository
                .findByUserIdAndRecordDate(userId, date)
                .orElseGet(() -> {
                    NutritionRecord r = new NutritionRecord();
                    r.setUserId(userId);
                    r.setRecordDate(date);
                    return r;
                });

        nr.setCalorieTotal(vo.getCalorieTotal());
        nr.setProteinTotal(vo.getProteinTotal());
        nr.setFatTotal(vo.getFatTotal());
        nr.setCarbohydrateTotal(vo.getCarbohydrateTotal());
        nr.setSugarTotal(vo.getSugarTotal());
        nr.setSodiumTotal(vo.getSodiumTotal());
        nutritionRecordRepository.save(nr);
    }
}
