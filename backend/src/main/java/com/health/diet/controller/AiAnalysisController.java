package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.command.AiChatCommand;
import com.health.diet.dto.vo.AiChatVO;
import com.health.diet.service.AiAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisController.class);

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/analyze-diet")
    public ApiResponse<AiChatVO> analyzeDiet(@RequestBody AiChatCommand command,
                                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("POST /api/ai/analyze-diet — userId={}, date={}, message={}",
                userId, command.getDate(), command.getMessage());
        AiChatVO result = aiAnalysisService.chat(userId, command);
        return ApiResponse.success(result);
    }

    @GetMapping("/conversation")
    public ApiResponse<AiChatVO> getConversation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.debug("GET /api/ai/conversation — 获取对话: userId={}, date={}", userId, date);
        AiChatVO result = aiAnalysisService.getConversation(userId, date);
        return ApiResponse.success(result);
    }
}
