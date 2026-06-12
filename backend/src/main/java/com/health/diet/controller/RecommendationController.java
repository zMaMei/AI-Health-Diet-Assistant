package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.RecommendationFeedbackCommand;
import com.health.diet.dto.vo.RecommendationVO;
import com.health.diet.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/today")
    public ApiResponse<List<RecommendationVO>> getToday(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.success(recommendationService.recommendToday(userId));
    }

    @PostMapping("/feedback")
    public ApiResponse<RecommendationVO> submitFeedback(
            @Valid @RequestBody RecommendationFeedbackCommand command,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        RecommendationVO vo = recommendationService.saveFeedbackAndRefresh(command, userId);
        return ApiResponse.success(vo);
    }
}
