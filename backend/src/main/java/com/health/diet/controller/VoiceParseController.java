package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.VoiceParseResultVO;
import com.health.diet.service.VoiceRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/voice")
public class VoiceParseController {

    private static final Logger log = LoggerFactory.getLogger(VoiceParseController.class);

    private final VoiceRecordService voiceRecordService;

    public VoiceParseController(VoiceRecordService voiceRecordService) {
        this.voiceRecordService = voiceRecordService;
    }

    /**
     * POST /api/voice/parse
     * 上传录音 → 保存文件到磁盘 → ASR → 食物实体解析 → 写 voice_record 表 → 返回结果
     */
    @PostMapping("/parse")
    public ApiResponse<VoiceParseResultVO> parse(
            @RequestParam("audio") MultipartFile file,
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "0") Integer durationSeconds) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "音频文件不能为空");
        }

        try {
            log.info("收到音频文件: name={}, size={}, type={}, duration={}s",
                    file.getOriginalFilename(), file.getSize(),
                    file.getContentType(), durationSeconds);

            byte[] audioBytes = file.getBytes();
            VoiceParseResultVO result = voiceRecordService.parseVoice(
                    userId, audioBytes, file.getContentType(), durationSeconds);

            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("读取/保存音频文件失败", e);
            return ApiResponse.error(500, "音频处理失败，请重试");
        }
    }
}
