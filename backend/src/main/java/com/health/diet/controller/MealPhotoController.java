package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.MealPhotoCreateCommand;
import com.health.diet.dto.vo.MealPhotoVO;
import com.health.diet.service.MealPhotoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/meal-photos")
public class MealPhotoController {

    private static final Logger log = LoggerFactory.getLogger(MealPhotoController.class);

    private final MealPhotoService mealPhotoService;

    public MealPhotoController(MealPhotoService mealPhotoService) {
        this.mealPhotoService = mealPhotoService;
    }

    /* 新增餐次照片 */
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody MealPhotoCreateCommand command,
                                     HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        command.setUserId((Long) request.getAttribute("userId"));
        log.info("POST /api/meal-photos — 新增照片记录: userId={}, date={}, mealType={}",
                command.getUserId(), command.getRecordDate(), command.getMealType());
        /* 调用新增餐次照片服务 */
        Long id = mealPhotoService.create(command);
        return ApiResponse.success(id);
    }

    /* 查询餐次照片 */
    @GetMapping
    public ApiResponse<List<MealPhotoVO>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String mealType,
            HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 日期参数 */
        List<MealPhotoVO> result;
        if (mealType != null && !mealType.isBlank()) {
            result = mealPhotoService.listByMeal(userId, date, mealType);
        } else {
            result = mealPhotoService.listByDate(userId, date);
        }
        log.debug("GET /api/meal-photos — 查询照片: userId={}, date={}, mealType={}, count={}",
                userId, date, mealType, result.size());
        return ApiResponse.success(result);
    }

    /* 删除餐次照片 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                     HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        log.info("DELETE /api/meal-photos/{} — 删除照片", id);
        /* 权限校验  -- 调用删除餐次照片服务 */
        mealPhotoService.delete(id, userId);
        return ApiResponse.success();
    }
}
