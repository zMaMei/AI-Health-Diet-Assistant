package com.health.diet.service;

import com.health.diet.dto.vo.AlertCheckResultVO;
import com.health.diet.dto.vo.AlertCheckResultVO.AlertMessage;
import com.health.diet.dto.command.AlertRuleCreateCommand;
import com.health.diet.dto.command.AlertRuleUpdateCommand;
import com.health.diet.dto.vo.AlertRuleVO;
import com.health.diet.entity.AlertRule;
import com.health.diet.entity.DietRecord;
import com.health.diet.repository.AlertRuleRepository;
import com.health.diet.repository.DietRecordRepository;
import com.health.diet.adapter.ThresholdAnalysisAdapter;
import com.health.diet.entity.UserProfile;
import com.health.diet.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AlertService {

    private final AlertRuleRepository alertRuleRepository;
    private final DietRecordRepository dietRecordRepository;
    private final ThresholdAnalysisAdapter thresholdAnalysisAdapter;
    private final UserProfileRepository userProfileRepository;

    private static final Map<String, String> NUTRIENT_NAMES = Map.of(
            "calorie", "热量",
            "sugar", "糖分",
            "sodium", "钠",
            "protein", "蛋白质",
            "fat", "脂肪",
            "carb", "碳水"
    );

    private static final Map<String, String> NUTRIENT_SUGGESTIONS = Map.of(
            "calorie", "建议选择低热量食物，增加运动量",
            "sugar", "建议选择无糖或低糖食品",
            "sodium", "建议减少盐分摄入，多吃新鲜蔬果",
            "protein", "建议增加优质蛋白摄入（鸡蛋、鱼虾、豆制品）",
            "fat", "建议控制油脂摄入，少吃油炸和肥肉",
            "carb", "建议调整主食结构，增加粗粮比例"
    );

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    public AlertService(AlertRuleRepository alertRuleRepository,
                        DietRecordRepository dietRecordRepository,
                        ThresholdAnalysisAdapter thresholdAnalysisAdapter,
                        UserProfileRepository userProfileRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.dietRecordRepository = dietRecordRepository;
        this.thresholdAnalysisAdapter = thresholdAnalysisAdapter;
        this.userProfileRepository = userProfileRepository;
    }

    public Long createRule(AlertRuleCreateCommand command) {
        AlertRule rule = new AlertRule();
        rule.setUserId(command.getUserId());
        rule.setNutrientType(command.getNutrientType());
        rule.setThreshold(command.getThreshold());
        rule.setEnabled(command.getEnabled() != null ? command.getEnabled() : true);
        alertRuleRepository.save(rule);
        return rule.getId();
    }

    public void updateRule(Long ruleId, AlertRuleUpdateCommand command, Long userId) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("预警规则不存在"));
        // 验证所有权
        if (!rule.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此规则");
        }
        if (command.getThreshold() != null) rule.updateThreshold(command.getThreshold());
        if (command.getEnabled() != null) rule.setEnabled(command.getEnabled());
        alertRuleRepository.save(rule);
    }

    public List<AlertRuleVO> listRules(Long userId) {
        return alertRuleRepository.findByUserId(userId).stream()
                .map(this::toVO).toList();
    }

    public AlertCheckResultVO checkAfterRecordSaved(Long userId, LocalDate recordDate) {
        List<AlertRule> rules = alertRuleRepository.findByUserId(userId);

        AlertCheckResultVO result = new AlertCheckResultVO();
        result.setHasAlert(false);
        result.setAlerts(new ArrayList<>());

        // Calculate current day's totals
        List<DietRecord> records = dietRecordRepository
                .findByUserIdAndRecordTimeBetweenOrderByRecordTimeAsc(
                        userId, recordDate.atStartOfDay(), recordDate.plusDays(1).atStartOfDay());

        BigDecimal calorieTotal = BigDecimal.ZERO;
        BigDecimal sugarTotal = BigDecimal.ZERO;
        BigDecimal sodiumTotal = BigDecimal.ZERO;
        BigDecimal proteinTotal = BigDecimal.ZERO;
        BigDecimal fatTotal = BigDecimal.ZERO;
        BigDecimal carbTotal = BigDecimal.ZERO;

        for (DietRecord record : records) {
            calorieTotal = calorieTotal.add(nvl(record.getCalorie()));
            sugarTotal = sugarTotal.add(nvl(record.getSugar()));
            sodiumTotal = sodiumTotal.add(nvl(record.getSodium()));
            proteinTotal = proteinTotal.add(nvl(record.getProtein()));
            fatTotal = fatTotal.add(nvl(record.getFat()));
            carbTotal = carbTotal.add(nvl(record.getCarbohydrate()));
        }

        for (AlertRule rule : rules) {
            if (!rule.getEnabled()) continue;

            BigDecimal current = switch (rule.getNutrientType()) {
                case "calorie" -> calorieTotal;
                case "sugar" -> sugarTotal;
                case "sodium" -> sodiumTotal;
                case "protein" -> proteinTotal;
                case "fat" -> fatTotal;
                case "carb" -> carbTotal;
                default -> null;
            };

            if (current != null && current.compareTo(rule.getThreshold()) > 0) {
                result.setHasAlert(true);
                String nutrientName = NUTRIENT_NAMES.getOrDefault(rule.getNutrientType(), rule.getNutrientType());
                String suggestion = NUTRIENT_SUGGESTIONS.getOrDefault(rule.getNutrientType(), "请注意控制摄入");

                result.getAlerts().add(new AlertMessage(
                        rule.getNutrientType(),
                        "今日" + nutrientName + "摄入" + current.setScale(1, RoundingMode.HALF_UP)
                                + "，已超过阈值" + rule.getThreshold(),
                        suggestion
                ));
            }
        }

        return result;
    }

    private AlertRuleVO toVO(AlertRule rule) {
        AlertRuleVO vo = new AlertRuleVO();
        vo.setId(rule.getId());
        vo.setUserId(rule.getUserId());
        vo.setNutrientType(rule.getNutrientType());
        vo.setThreshold(rule.getThreshold());
        vo.setEnabled(rule.getEnabled());
        return vo;
    }

    /**
     * AI 分析预警阈值并更新/创建规则。
     * @param userId 用户 ID
     * @return 更新后的规则列表
     */
    public List<AlertRuleVO> analyzeAndApply(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("请先完善个人资料"));

        // 计算 BMI
        String bmiStr = "未知";
        if (profile.getHeightCm() != null && profile.getWeightKg() != null
                && profile.getHeightCm().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal heightM = profile.getHeightCm().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal bmi = profile.getWeightKg().divide(heightM.pow(2), 1, RoundingMode.HALF_UP);
            bmiStr = bmi.toString();
        }

        // 构造 prompt
        String age = profile.getAge() != null ? profile.getAge().toString() : "未知";
        String height = profile.getHeightCm() != null ? profile.getHeightCm().toString() : "未知";
        String weight = profile.getWeightKg() != null ? profile.getWeightKg().toString() : "未知";
        String goal = profile.getGoal() != null ? profile.getGoal() : "均衡";
        String warning = profile.getWarningProfile() != null && !profile.getWarningProfile().isEmpty()
                ? profile.getWarningProfile() : "无特殊疾病";

        String gender = profile.getGender() != null ? profile.getGender() : "未知";

        String prompt = String.format("""
            你是一位专业的营养师。请根据以下用户档案，综合分析给出每日摄入上限/目标建议。
            - 年龄：%s 岁
            - 性别：%s
            - 身高：%s cm
            - 体重：%s kg
            - BMI：%s
            - 健康目标：%s
            - 慢性病/特殊饮食：%s

            请严格以 JSON 格式返回，不要包含其他文字：
            {"calorie": 数字(kcal), "sugar": 数字(g), "sodium": 数字(mg), "protein": 数字(g), "fat": 数字(g), "carb": 数字(g)}
            其中：
            - calorie：每日热量上限
            - sugar：每日糖分上限
            - sodium：每日钠上限
            - protein：每日蛋白质目标(g)
            - fat：每日脂肪上限(g)
            - carb：每日碳水目标(g)
            """, age, gender, height, weight, bmiStr, goal, warning);

        ThresholdAnalysisAdapter.ThresholdResult result = thresholdAnalysisAdapter.analyze(prompt);

        // Upsert 规则
        List<AlertRule> existingRules = alertRuleRepository.findByUserId(userId);
        upsertRule(existingRules, userId, "calorie", result.calorie());
        upsertRule(existingRules, userId, "sugar", result.sugar());
        upsertRule(existingRules, userId, "sodium", result.sodium());
        if (result.protein() != null) upsertRule(existingRules, userId, "protein", result.protein());
        if (result.fat() != null) upsertRule(existingRules, userId, "fat", result.fat());
        if (result.carb() != null) upsertRule(existingRules, userId, "carb", result.carb());

        log.info("AI 预警阈值分析完成: userId={}, calorie={}, sugar={}, sodium={}, protein={}, fat={}, carb={}",
                userId, result.calorie(), result.sugar(), result.sodium(),
                result.protein(), result.fat(), result.carb());

        return listRules(userId);
    }

    private void upsertRule(List<AlertRule> existing, Long userId, String nutrientType, BigDecimal threshold) {
        AlertRule rule = existing.stream()
                .filter(r -> r.getNutrientType().equals(nutrientType))
                .findFirst()
                .orElseGet(() -> {
                    AlertRule newRule = new AlertRule();
                    newRule.setUserId(userId);
                    newRule.setNutrientType(nutrientType);
                    newRule.setEnabled(true);
                    return newRule;
                });
        rule.setThreshold(threshold);
        alertRuleRepository.save(rule);
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
