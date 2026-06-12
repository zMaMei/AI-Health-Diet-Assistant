package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.VoiceRecordVO;
import com.health.diet.service.VoiceRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice-records")
public class VoiceRecordController {

    private static final Logger log = LoggerFactory.getLogger(VoiceRecordController.class);

    private final VoiceRecordService voiceRecordService;

    public VoiceRecordController(VoiceRecordService voiceRecordService) {
        this.voiceRecordService = voiceRecordService;
    }

    /** 查询某日所有语音记录 */
    @GetMapping
    public ApiResponse<List<VoiceRecordVO>> list(
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<VoiceRecordVO> result = voiceRecordService.listByDate(userId, date);
        log.debug("GET /api/voice-records — userId={}, date={}, count={}",
                userId, date, result.size());
        return ApiResponse.success(result);
    }

    /** 更新语音记录的餐次类型（用户确认保存后回填） */
    @PutMapping("/{id}/meal-type")
    public ApiResponse<Void> updateMealType(@PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        String mealType = body.get("mealType");
        log.info("PUT /api/voice-records/{}/meal-type — mealType={}", id, mealType);
        voiceRecordService.updateMealType(id, mealType);
        return ApiResponse.success();
    }

    /** 删除语音记录 + 磁盘文件 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/voice-records/{} — 删除语音记录", id);
        voiceRecordService.delete(id);
        return ApiResponse.success();
    }
}
