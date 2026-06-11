package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.VoiceParseResultVO;
import com.health.diet.service.VoiceRecordService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/voice")
public class VoiceParseController {

    private final VoiceRecordService voiceRecordService;

    public VoiceParseController(VoiceRecordService voiceRecordService) {
        this.voiceRecordService = voiceRecordService;
    }

    @PostMapping("/parse")
    public ApiResponse<VoiceParseResultVO> parse(@RequestParam("audio") MultipartFile file) {
        String audioUrl = "simulated://" + file.getOriginalFilename();
        return ApiResponse.success(voiceRecordService.parseVoice(audioUrl));
    }
}
