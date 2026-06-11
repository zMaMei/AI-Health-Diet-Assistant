package com.health.diet.service;

import com.health.diet.dto.command.DietRecordCreateCommand;
import com.health.diet.dto.command.DietRecordUpdateCommand;
import com.health.diet.dto.vo.DietRecordVO;
import com.health.diet.entity.DietRecord;
import com.health.diet.entity.FoodItem;
import com.health.diet.repository.DietRecordRepository;
import com.health.diet.repository.FoodItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DietRecordService {

    private final DietRecordRepository dietRecordRepository;
    private final FoodItemRepository foodItemRepository;

    public DietRecordService(DietRecordRepository dietRecordRepository,
                             FoodItemRepository foodItemRepository) {
        this.dietRecordRepository = dietRecordRepository;
        this.foodItemRepository = foodItemRepository;
    }

    public Long create(DietRecordCreateCommand command) {
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
            throw new IllegalArgumentException("份量必须大于0");
        }

        dietRecordRepository.save(record);
        return record.getId();
    }

    public List<DietRecordVO> list(Long userId, LocalDate date) {
        List<DietRecord> records = dietRecordRepository.findByUserAndDate(userId, date);
        return records.stream().map(this::toVO).toList();
    }

    public void update(Long id, DietRecordUpdateCommand command) {
        DietRecord record = dietRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("饮食记录不存在"));

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
    }

    public void delete(Long id) {
        if (!dietRecordRepository.existsById(id)) {
            throw new IllegalArgumentException("饮食记录不存在");
        }
        dietRecordRepository.deleteById(id);
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
                BigDecimal ratio = record.getAmount().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                vo.setCalorie(food.getCalorie().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                vo.setProtein(food.getProtein().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                vo.setFat(food.getFat().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
                vo.setCarbohydrate(food.getCarbohydrate().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
            });
        }
        return vo;
    }
}
