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

    @PostMapping
    public ApiResponse<Long> createRule(@Valid @RequestBody AlertRuleCreateCommand command,
                                         HttpServletRequest request) {
        command.setUserId((Long) request.getAttribute("userId"));
        return ApiResponse.success(alertService.createRule(command));
    }

    @GetMapping
    public ApiResponse<List<AlertRuleVO>> listRules(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.success(alertService.listRules(userId));
    }

    @PutMapping("/{ruleId}")
    public ApiResponse<Void> updateRule(@PathVariable Long ruleId,
                                         @RequestBody AlertRuleUpdateCommand command,
                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        alertService.updateRule(ruleId, command, userId);
        return ApiResponse.success();
    }

    @GetMapping("/check")
    public ApiResponse<AlertCheckResultVO> check(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.success(alertService.checkAfterRecordSaved(userId, date));
    }

    @PostMapping("/analyze")
    public ApiResponse<List<AlertRuleVO>> analyze(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResponse.success(alertService.analyzeAndApply(userId));
    }
}
