package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.HealthScoreVO;
import com.health.diet.service.HealthScoreService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/health-score")
public class HealthScoreController {

    private final HealthScoreService healthScoreService;

    public HealthScoreController(HealthScoreService healthScoreService) {
        this.healthScoreService = healthScoreService;
    }

    @GetMapping("/daily")
    public ApiResponse<HealthScoreVO> getDailyScore(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.success(healthScoreService.getDailyScore(userId, date));
    }
}
