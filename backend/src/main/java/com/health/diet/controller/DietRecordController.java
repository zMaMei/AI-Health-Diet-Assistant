package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.DietRecordCreateCommand;
import com.health.diet.dto.command.DietRecordUpdateCommand;
import com.health.diet.dto.vo.DietRecordVO;
import com.health.diet.service.DietRecordService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/diet-records")
public class DietRecordController {

    private static final Logger log = LoggerFactory.getLogger(DietRecordController.class);

    private final DietRecordService dietRecordService;

    public DietRecordController(DietRecordService dietRecordService) {
        this.dietRecordService = dietRecordService;
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody DietRecordCreateCommand command) {
        log.info("POST /api/diet-records — 创建饮食记录: userId={}, foodName={}, mealType={}, amount={}, source={}",
                command.getUserId(), command.getFoodName(), command.getMealType(),
                command.getAmount(), command.getSource());
        Long id = dietRecordService.create(command);
        log.info("POST /api/diet-records — 创建成功: id={}", id);
        return ApiResponse.success(id);
    }

    @GetMapping
    public ApiResponse<List<DietRecordVO>> list(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("GET /api/diet-records — 查询记录: userId={}, date={}", userId, date);
        List<DietRecordVO> result = dietRecordService.list(userId, date);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @RequestBody DietRecordUpdateCommand command) {
        log.info("PUT /api/diet-records/{} — 更新记录", id);
        dietRecordService.update(id, command);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/diet-records/{} — 删除记录", id);
        dietRecordService.delete(id);
        log.info("DELETE /api/diet-records/{} — 删除成功", id);
        return ApiResponse.success();
    }
}
