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

    private static final Map<String, String> NUTRIENT_NAMES = Map.of(
            "calorie", "热量",
            "sugar", "糖分",
            "sodium", "钠"
    );

    private static final Map<String, String> NUTRIENT_SUGGESTIONS = Map.of(
            "calorie", "建议选择低热量食物，增加运动量",
            "sugar", "建议选择无糖或低糖食品",
            "sodium", "建议减少盐分摄入，多吃新鲜蔬果"
    );

    public AlertService(AlertRuleRepository alertRuleRepository,
                        DietRecordRepository dietRecordRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.dietRecordRepository = dietRecordRepository;
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

    public void updateRule(Long ruleId, AlertRuleUpdateCommand command) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("预警规则不存在"));
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

        for (DietRecord record : records) {
            calorieTotal = calorieTotal.add(nvl(record.getCalorie()));
            sugarTotal = sugarTotal.add(nvl(record.getSugar()));
            sodiumTotal = sodiumTotal.add(nvl(record.getSodium()));
        }

        for (AlertRule rule : rules) {
            if (!rule.getEnabled()) continue;

            BigDecimal current = switch (rule.getNutrientType()) {
                case "calorie" -> calorieTotal;
                case "sugar" -> sugarTotal;
                case "sodium" -> sodiumTotal;
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

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
