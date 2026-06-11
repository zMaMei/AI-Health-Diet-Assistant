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

        dietRecordRepository.save(record);
        log.info("饮食记录已保存: id={}, foodName={}, mealType={}",
                record.getId(), record.getFoodName(), record.getMealType());
        return record.getId();
    }

    public List<DietRecordVO> list(Long userId, LocalDate date) {
        log.debug("查询饮食记录: userId={}, date={}", userId, date);
        List<DietRecord> records = dietRecordRepository.findByUserAndDate(userId, date);
        log.debug("查询到 {} 条记录", records.size());
        return records.stream().map(this::toVO).toList();
    }

    public void update(Long id, DietRecordUpdateCommand command) {
        log.info("更新饮食记录: id={}", id);
        DietRecord record = dietRecordRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("饮食记录不存在: id={}", id);
                    return new IllegalArgumentException("饮食记录不存在");
                });

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

    public void delete(Long id) {
        if (!dietRecordRepository.existsById(id)) {
            log.warn("尝试删除不存在的饮食记录: id={}", id);
            throw new IllegalArgumentException("饮食记录不存在");
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

        // Calculate nutrition if food item exists
        if (record.getFoodId() != null) {
            foodItemRepository.findById(record.getFoodId()).ifPresent(food -> {
                BigDecimal ratio = record.getAmount()
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                vo.setCalorie(food.getCalorie().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                vo.setProtein(food.getProtein().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                vo.setFat(food.getFat().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                vo.setCarbohydrate(food.getCarbohydrate().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
            });
        }
        return vo;
    }
}
