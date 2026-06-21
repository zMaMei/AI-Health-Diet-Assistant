package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.DietRecordCreateCommand;
import com.health.diet.dto.command.DietRecordUpdateCommand;
import com.health.diet.dto.vo.DietRecordVO;
import com.health.diet.service.DietRecordService;
import jakarta.servlet.http.HttpServletRequest;
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

    /* 新增饮食记录 */
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody DietRecordCreateCommand command,
                                     HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        command.setUserId((Long) request.getAttribute("userId"));
        log.info("POST /api/diet-records — 创建饮食记录: userId={}, foodName={}, mealType={}, amount={}, source={}",
                command.getUserId(), command.getFoodName(), command.getMealType(),
                command.getAmount(), command.getSource());
        /* 调用新增饮食记录服务 */
        Long id = dietRecordService.create(command);
        log.info("POST /api/diet-records — 创建成功: id={}", id);
        return ApiResponse.success(id);
    }

    /* 查询饮食记录 */
    @GetMapping
    public ApiResponse<List<DietRecordVO>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 日期参数 */
        log.debug("GET /api/diet-records — 查询记录: userId={}, date={}", userId, date);
        /* 调用查询饮食记录服务 */
        List<DietRecordVO> result = dietRecordService.list(userId, date);
        return ApiResponse.success(result);
    }

    /* 修改饮食记录 */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @RequestBody DietRecordUpdateCommand command,
                                     HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        log.info("PUT /api/diet-records/{} — 更新记录", id);
        /* 权限校验  -- 调用更新饮食记录服务 */
        dietRecordService.update(id, command, userId);
        return ApiResponse.success();
    }

    /* 删除饮食记录 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                     HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        log.info("DELETE /api/diet-records/{} — 删除记录", id);
        /* 权限校验  -- 调用删除饮食记录服务 */
        dietRecordService.delete(id, userId);
        log.info("DELETE /api/diet-records/{} — 删除成功", id);
        return ApiResponse.success();
    }
}
