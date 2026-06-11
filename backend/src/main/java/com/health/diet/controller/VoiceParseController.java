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

    @PostMapping("/parse")
    public ApiResponse<VoiceParseResultVO> parse(@RequestParam("audio") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "音频文件不能为空");
        }

        try {
            log.info("收到音频文件: name={}, size={}, type={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            byte[] audioBytes = file.getBytes();
            VoiceParseResultVO result = voiceRecordService.parseVoice(
                    audioBytes, file.getContentType());

            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("读取音频文件失败", e);
            return ApiResponse.error(500, "音频文件读取失败，请重试");
        }
    }
}
