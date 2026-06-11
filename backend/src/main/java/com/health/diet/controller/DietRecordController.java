package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.DietRecordCreateCommand;
import com.health.diet.dto.command.DietRecordUpdateCommand;
import com.health.diet.dto.vo.DietRecordVO;
import com.health.diet.service.DietRecordService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/diet-records")
public class DietRecordController {

    private final DietRecordService dietRecordService;

    public DietRecordController(DietRecordService dietRecordService) {
        this.dietRecordService = dietRecordService;
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody DietRecordCreateCommand command) {
        Long id = dietRecordService.create(command);
        return ApiResponse.success(id);
    }

    @GetMapping
    public ApiResponse<List<DietRecordVO>> list(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(dietRecordService.list(userId, date));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @RequestBody DietRecordUpdateCommand command) {
        dietRecordService.update(id, command);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dietRecordService.delete(id);
        return ApiResponse.success();
    }
}
