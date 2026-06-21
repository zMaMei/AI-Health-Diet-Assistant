package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.NutritionDailyVO;
import com.health.diet.service.NutritionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/nutrition")
public class NutritionController {

    private final NutritionService nutritionService;

    public NutritionController(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    /* 查询每日营养汇总 */
    @GetMapping("/daily")
    public ApiResponse<NutritionDailyVO> getDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 日期参数 */
        /* 调用查询每日营养汇总服务 */
        return ApiResponse.success(nutritionService.getDaily(userId, date));
    }
}
