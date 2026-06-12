package com.health.diet.service;

import com.health.diet.dto.vo.NutritionDailyVO;
import com.health.diet.entity.NutritionRecord;
import com.health.diet.entity.UserProfile;
import com.health.diet.repository.DietRecordRepository;
import com.health.diet.repository.NutritionRecordRepository;
import com.health.diet.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class NutritionService {

    private final DietRecordRepository dietRecordRepository;
    private final NutritionRecordRepository nutritionRecordRepository;
    private final UserProfileRepository userProfileRepository;

    private static final BigDecimal DEFAULT_CALORIE_GOAL = new BigDecimal("2000");
    private static final BigDecimal DEFAULT_PROTEIN_GOAL = new BigDecimal("60");
    private static final BigDecimal DEFAULT_FAT_GOAL = new BigDecimal("65");
    private static final BigDecimal DEFAULT_CARB_GOAL = new BigDecimal("300");

    public NutritionService(DietRecordRepository dietRecordRepository,
                            NutritionRecordRepository nutritionRecordRepository,
                            UserProfileRepository userProfileRepository) {
        this.dietRecordRepository = dietRecordRepository;
        this.nutritionRecordRepository = nutritionRecordRepository;
        this.userProfileRepository = userProfileRepository;
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

        // Get goals from user profile
        userProfileRepository.findByUserId(userId).ifPresentOrElse(profile -> {
            vo.setCalorieGoal(getGoalForProfile(profile, "calorie", DEFAULT_CALORIE_GOAL));
            vo.setProteinGoal(getGoalForProfile(profile, "protein", DEFAULT_PROTEIN_GOAL));
            vo.setFatGoal(getGoalForProfile(profile, "fat", DEFAULT_FAT_GOAL));
            vo.setCarbohydrateGoal(getGoalForProfile(profile, "carb", DEFAULT_CARB_GOAL));
        }, () -> {
            vo.setCalorieGoal(DEFAULT_CALORIE_GOAL);
            vo.setProteinGoal(DEFAULT_PROTEIN_GOAL);
            vo.setFatGoal(DEFAULT_FAT_GOAL);
            vo.setCarbohydrateGoal(DEFAULT_CARB_GOAL);
        });

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

    private BigDecimal getGoalForProfile(UserProfile profile, String type, BigDecimal defaultGoal) {
        if ("减脂".equals(profile.getGoal())) {
            return switch (type) {
                case "calorie" -> new BigDecimal("1600");
                case "protein" -> new BigDecimal("70");
                case "fat" -> new BigDecimal("50");
                case "carb" -> new BigDecimal("200");
                default -> defaultGoal;
            };
        } else if ("增肌".equals(profile.getGoal())) {
            return switch (type) {
                case "calorie" -> new BigDecimal("2500");
                case "protein" -> new BigDecimal("120");
                case "fat" -> new BigDecimal("70");
                case "carb" -> new BigDecimal("350");
                default -> defaultGoal;
            };
        }
        return defaultGoal;
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
