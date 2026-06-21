package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.RecommendationPageVO;
import com.health.diet.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /* 获取今日推荐 */
    @GetMapping("/today")
    public ApiResponse<RecommendationPageVO> getToday(HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 调用获取今日推荐服务 */
        return ApiResponse.success(recommendationService.getTodayPage(userId));
    }

    /* 强制刷新推荐 */
    @PostMapping("/refresh")
    public ApiResponse<RecommendationPageVO> refreshToday(HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 调用强制刷新推荐服务 */
        return ApiResponse.success(recommendationService.refreshTodayPage(userId));
    }
}
