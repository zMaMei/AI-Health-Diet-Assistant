package com.health.diet.controller;

import com.health.diet.common.ApiResponse;
import com.health.diet.dto.vo.VoiceParseResultVO;
import com.health.diet.service.VoiceRecordService;
import jakarta.servlet.http.HttpServletRequest;
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

    /* 语音解析上传 */
    @PostMapping("/parse")
    public ApiResponse<VoiceParseResultVO> parse(
            @RequestParam("audio") MultipartFile file,
            @RequestParam(defaultValue = "0") Integer durationSeconds,
            HttpServletRequest request) {
        /* 从拦截器注入的用户ID */
        Long userId = (Long) request.getAttribute("userId");

        /* 参数校验 */
        if (file.isEmpty()) {
            return ApiResponse.error(400, "音频文件不能为空");
        }

        /* 异常处理 */
        try {
            log.info("收到音频文件: name={}, size={}, type={}, duration={}s",
                    file.getOriginalFilename(), file.getSize(),
                    file.getContentType(), durationSeconds);

            /* 读取音频字节数据 */
            byte[] audioBytes = file.getBytes();
            /* 调用语音解析服务 */
            VoiceParseResultVO result = voiceRecordService.parseVoice(
                    userId, audioBytes, file.getContentType(), durationSeconds);

            return ApiResponse.success(result);

        } catch (IOException e) {
            log.error("读取/保存音频文件失败", e);
            return ApiResponse.error(500, "音频处理失败，请重试");
        }
    }
}
