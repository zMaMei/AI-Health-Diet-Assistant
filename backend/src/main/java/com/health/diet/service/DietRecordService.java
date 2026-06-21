package com.health.diet.service;

import com.health.diet.dto.command.DietRecordCreateCommand;
import com.health.diet.dto.command.DietRecordUpdateCommand;
import com.health.diet.dto.vo.DietRecordVO;
import com.health.diet.entity.DietRecord;
import com.health.diet.entity.FoodItem;
import com.health.diet.repository.DietRecordRepository;
import com.health.diet.repository.FoodItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DietRecordService {

    private static final Logger log = LoggerFactory.getLogger(DietRecordService.class);

    private final DietRecordRepository dietRecordRepository;
    private final FoodItemRepository foodItemRepository;

    public DietRecordService(DietRecordRepository dietRecordRepository,
                             FoodItemRepository foodItemRepository) {
        this.dietRecordRepository = dietRecordRepository;
        this.foodItemRepository = foodItemRepository;
    }

    /* 新增饮食记录 */
    public Long create(DietRecordCreateCommand command) {
        log.info("创建饮食记录: userId={}, foodName={}, mealType={}, amount={}, source={}",
                command.getUserId(), command.getFoodName(), command.getMealType(),
                command.getAmount(), command.getSource());

        DietRecord record = new DietRecord();
        record.setUserId(command.getUserId());
        record.setFoodId(command.getFoodId());
        record.setFoodName(command.getFoodName());
        record.setMealType(command.getMealType());
        record.setAmount(command.getAmount());
        record.setSource(command.getSource());
        record.setImageUrl(command.getImageUrl());
        record.setRecordTime(command.getRecordTime() != null
                ? command.getRecordTime() : LocalDateTime.now());

        if (record.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("份量无效: {}", record.getAmount());
            throw new IllegalArgumentException("份量必须大于0");
        }

        // 写入营养快照：优先使用 command 传入的 AI 分析值，否则从 food_item 库计算兜底
        populateNutrition(record, command);

        dietRecordRepository.save(record);
        log.info("饮食记录已保存: id={}, foodName={}, mealType={}, calorie={}, protein={}",
                record.getId(), record.getFoodName(), record.getMealType(),
                record.getCalorie(), record.getProtein());
        return record.getId();
    }

    /** 写入营养快照：优先 AI 返回值，兜底查 food_item 计算 */
    private void populateNutrition(DietRecord record, DietRecordCreateCommand command) {
        /* AI营养值优先 */
        // 优先使用 command 中传入的营养值（AI 返回）
        if (command.getCalorie() != null) {
            record.setCalorie(command.getCalorie());
            record.setProtein(command.getProtein() != null ? command.getProtein() : BigDecimal.ZERO);
            record.setFat(command.getFat() != null ? command.getFat() : BigDecimal.ZERO);
            record.setCarbohydrate(command.getCarbohydrate() != null ? command.getCarbohydrate() : BigDecimal.ZERO);
            record.setSugar(command.getSugar() != null ? command.getSugar() : BigDecimal.ZERO);
            record.setSodium(command.getSodium() != null ? command.getSodium() : BigDecimal.ZERO);
            return;
        }

        /* food_item兜底计算 */
        // 兜底：从 food_item 库按份量比例计算
        if (record.getFoodId() != null) {
            foodItemRepository.findById(record.getFoodId()).ifPresentOrElse(food -> {
                /* 按份量换算营养 */
                BigDecimal ratio = record.getAmount()
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                record.setCalorie(food.getCalorie().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                record.setProtein(food.getProtein().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                record.setFat(food.getFat().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                record.setCarbohydrate(food.getCarbohydrate().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                record.setSugar((food.getSugar() != null ? food.getSugar() : BigDecimal.ZERO)
                        .multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                record.setSodium((food.getSodium() != null ? food.getSodium() : BigDecimal.ZERO)
                        .multiply(ratio).setScale(2, RoundingMode.HALF_UP));
            }, () -> {
                log.warn("food_id={} 在 food_item 库中未找到，营养值留空", record.getFoodId());
                setZeroNutrition(record);
            });
        } else {
            // 无 food_id 且无 AI 营养值 → 留空
            log.warn("饮食记录无 food_id 且无 AI 营养值: foodName={}", record.getFoodName());
            setZeroNutrition(record);
        }
    }

    private void setZeroNutrition(DietRecord record) {
        record.setCalorie(BigDecimal.ZERO);
        record.setProtein(BigDecimal.ZERO);
        record.setFat(BigDecimal.ZERO);
        record.setCarbohydrate(BigDecimal.ZERO);
        record.setSugar(BigDecimal.ZERO);
        record.setSodium(BigDecimal.ZERO);
    }

    /* 查询饮食记录 */
    public List<DietRecordVO> list(Long userId, LocalDate date) {
        log.debug("查询饮食记录: userId={}, date={}", userId, date);
        List<DietRecord> records = dietRecordRepository.findByUserAndDate(userId, date);
        log.debug("查询到 {} 条记录", records.size());
        return records.stream().map(this::toVO).toList();
    }

    /* 修改饮食记录 */
    public void update(Long id, DietRecordUpdateCommand command, Long userId) {
        log.info("更新饮食记录: id={}", id);
        DietRecord record = dietRecordRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("饮食记录不存在: id={}", id);
                    return new IllegalArgumentException("饮食记录不存在");
                });
        /* 所有权校验 */
        // 验证所有权
        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此记录");
        }

        if (command.getFoodId() != null) record.setFoodId(command.getFoodId());
        if (command.getFoodName() != null) record.setFoodName(command.getFoodName());
        if (command.getMealType() != null) record.setMealType(command.getMealType());
        if (command.getAmount() != null) {
            if (command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("份量必须大于0");
            }
            record.setAmount(command.getAmount());
        }
        if (command.getImageUrl() != null) record.setImageUrl(command.getImageUrl());

        dietRecordRepository.save(record);
        log.info("饮食记录已更新: id={}", id);
    }

    /* 删除饮食记录 */
    public void delete(Long id, Long userId) {
        DietRecord record = dietRecordRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("尝试删除不存在的饮食记录: id={}", id);
                    return new IllegalArgumentException("饮食记录不存在");
                });
        /* 所有权校验 */
        // 验证所有权
        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此记录");
        }
        dietRecordRepository.deleteById(id);
        log.info("饮食记录已删除: id={}", id);
    }

    private DietRecordVO toVO(DietRecord record) {
        DietRecordVO vo = new DietRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setFoodId(record.getFoodId());
        vo.setFoodName(record.getFoodName());
        vo.setMealType(record.getMealType());
        vo.setAmount(record.getAmount());
        vo.setSource(record.getSource());
        vo.setImageUrl(record.getImageUrl());
        vo.setRecordTime(record.getRecordTime());

        // 直接从 entity 读取营养快照（写入时已计算好）
        vo.setCalorie(record.getCalorie());
        vo.setProtein(record.getProtein());
        vo.setFat(record.getFat());
        vo.setCarbohydrate(record.getCarbohydrate());
        vo.setSugar(record.getSugar());
        vo.setSodium(record.getSodium());

        return vo;
    }
}
