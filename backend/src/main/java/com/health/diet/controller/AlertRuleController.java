package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.AlertRuleCreateCommand;
import com.health.diet.dto.command.AlertRuleUpdateCommand;
import com.health.diet.dto.vo.AlertCheckResultVO;
import com.health.diet.dto.vo.AlertRuleVO;
import com.health.diet.service.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/alert-rules")
public class AlertRuleController {

    private final AlertService alertService;

    public AlertRuleController(AlertService alertService) {
        this.alertService = alertService;
    }

    /* 创建预警规则 */
    @PostMapping
    public ApiResponse<Long> createRule(@Valid @RequestBody AlertRuleCreateCommand command,
                                         HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        command.setUserId((Long) request.getAttribute("userId"));
        /* 调用创建预警规则服务 */
        return ApiResponse.success(alertService.createRule(command));
    }

    /* 查询预警规则 */
    @GetMapping
    public ApiResponse<List<AlertRuleVO>> listRules(HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 调用查询预警规则服务 */
        return ApiResponse.success(alertService.listRules(userId));
    }

    /* 修改预警规则 */
    @PutMapping("/{ruleId}")
    public ApiResponse<Void> updateRule(@PathVariable Long ruleId,
                                         @RequestBody AlertRuleUpdateCommand command,
                                         HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 权限校验  -- 调用修改预警规则服务 */
        alertService.updateRule(ruleId, command, userId);
        return ApiResponse.success();
    }

    /* 检查预警 */
    @GetMapping("/check")
    public ApiResponse<AlertCheckResultVO> check(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 日期参数 */
        /* 调用检查预警服务 */
        return ApiResponse.success(alertService.checkAfterRecordSaved(userId, date));
    }

    /* AI分析生成阈值 */
    @PostMapping("/analyze")
    public ApiResponse<List<AlertRuleVO>> analyze(HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");
        /* 调用AI分析生成阈值服务 */
        return ApiResponse.success(alertService.analyzeAndApply(userId));
    }
}
