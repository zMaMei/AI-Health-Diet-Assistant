package com.health.diet.service;

import com.health.diet.adapter.SpeechToTextAdapter;
import com.health.diet.adapter.FoodEntityParserAdapter;
import com.health.diet.dto.vo.VoiceParseResultVO;
import com.health.diet.dto.vo.VoiceRecordVO;
import com.health.diet.entity.VoiceRecord;
import com.health.diet.repository.VoiceRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class VoiceRecordService {

    private static final Logger log = LoggerFactory.getLogger(VoiceRecordService.class);

    private static final Path UPLOAD_ROOT = Paths.get("uploads/voice");

    private final SpeechToTextAdapter speechToTextAdapter;
    private final FoodEntityParserAdapter foodEntityParserAdapter;
    private final VoiceRecordRepository voiceRecordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VoiceRecordService(SpeechToTextAdapter speechToTextAdapter,
                               FoodEntityParserAdapter foodEntityParserAdapter,
                               VoiceRecordRepository voiceRecordRepository) {
        this.speechToTextAdapter = speechToTextAdapter;
        this.foodEntityParserAdapter = foodEntityParserAdapter;
        this.voiceRecordRepository = voiceRecordRepository;
    }

    /**
     * 完整语音解析流程：
     * ① 保存音频文件到磁盘
     * ② 语音转文字
     * ③ 食物实体提取
     * ④ 写入 voice_record 表
     * ⑤ 返回解析结果 + audioUrl
     */
    public VoiceParseResultVO parseVoice(Long userId, byte[] audioBytes, String contentType,
                                          Integer durationSeconds) throws IOException {
        // ① 保存音频到磁盘
        String audioUrl = saveAudioToDisk(audioBytes);

        // ② 语音→文字
        String transcribed = speechToTextAdapter.transcribe(audioBytes, contentType);
        log.info("语音转写完成: {}", transcribed);

        // ③ 文字→食物实体
        List<FoodEntityParserAdapter.FoodEntity> entities;
        try {
            entities = foodEntityParserAdapter.extractFoodEntities(transcribed);
            log.info("食物实体提取完成: {} 个", entities.size());
        } catch (Exception e) {
            log.error("食物实体提取失败，保存语音记录但无实体: {}", e.getMessage());
            entities = List.of();
        }

        // ④ 写入 DB
        VoiceRecord record = new VoiceRecord();
        record.setUserId(userId);
        record.setRecordDate(LocalDate.now());
        record.setAudioUrl(audioUrl);
        record.setTranscribedText(transcribed);
        record.setDurationSeconds(durationSeconds != null ? durationSeconds : 0);

        List<VoiceParseResultVO.FoodEntity> voEntities = entities.stream()
                .map(e -> {
                    VoiceParseResultVO.FoodEntity vo = new VoiceParseResultVO.FoodEntity(
                            e.foodName(), e.amount(), e.unit(), e.mealType());
                    vo.setCalorie(e.calorie());
                    vo.setProtein(e.protein());
                    vo.setFat(e.fat());
                    vo.setCarbohydrate(e.carbohydrate());
                    vo.setSugar(e.sugar());
                    vo.setSodium(e.sodium());
                    return vo;
                })
                .toList();
        try {
            record.setFoodEntities(objectMapper.writeValueAsString(voEntities));
        } catch (IOException e) {
            log.warn("食物实体 JSON 序列化失败", e);
        }
        voiceRecordRepository.save(record);
        log.info("语音记录已保存: id={}, audioUrl={}", record.getId(), audioUrl);

        // ⑤ 返回结果
        VoiceParseResultVO result = new VoiceParseResultVO();
        result.setVoiceRecordId(record.getId());
        result.setAudioUrl(audioUrl);
        result.setTranscribedText(transcribed);
        result.setFoodEntities(voEntities);
        return result;
    }

    /** 查询某日所有语音记录 */
    public List<VoiceRecordVO> listByDate(Long userId, LocalDate date) {
        return voiceRecordRepository.findByUserIdAndRecordDateOrderByCreatedAtDesc(userId, date)
                .stream().map(this::toVO).toList();
    }

    /** 更新语音记录的餐次类型（用户确认保存后回填） */
    public void updateMealType(Long id, String mealType, Long userId) {
        VoiceRecord record = voiceRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("语音记录不存在"));
        // 验证所有权
        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此语音记录");
        }
        record.setMealType(mealType);
        voiceRecordRepository.save(record);
        log.info("语音记录餐次已更新: id={}, mealType={}", id, mealType);
    }

    /** 删除语音记录 + 磁盘文件 */
    public void delete(Long id, Long userId) {
        VoiceRecord record = voiceRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("语音记录不存在"));
        // 验证所有权
        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此语音记录");
        }

        Path filePath = UPLOAD_ROOT.resolve(
                record.getAudioUrl().replaceFirst("^/voice/", ""));
        try {
            Files.deleteIfExists(filePath);
            log.info("音频文件已删除: {}", filePath);
        } catch (IOException e) {
            log.warn("删除音频文件失败: {}", filePath, e);
        }

        voiceRecordRepository.deleteById(id);
        log.info("语音记录已删除: id={}", id);
    }

    /**
     * 保存音频字节到磁盘，返回相对路径。
     * 目录: uploads/voice/{yyyy}/{MM}/{dd}/{uuid}.webm
     */
    private String saveAudioToDisk(byte[] audioBytes) throws IOException {
        LocalDate today = LocalDate.now();
        String datePath = String.format("%04d/%02d/%02d",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        Path dir = UPLOAD_ROOT.resolve(datePath);
        Files.createDirectories(dir);

        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String filename = uuid + ".webm";
        Path filePath = dir.resolve(filename);

        Files.write(filePath, audioBytes);
        String relativePath = "/voice/" + datePath + "/" + filename;
        log.info("音频已保存到磁盘: {} ({} bytes)", relativePath, audioBytes.length);
        return relativePath;
    }

    private VoiceRecordVO toVO(VoiceRecord record) {
        VoiceRecordVO vo = new VoiceRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setRecordDate(record.getRecordDate());
        vo.setAudioUrl(record.getAudioUrl());
        vo.setTranscribedText(record.getTranscribedText());
        vo.setFoodEntities(record.getFoodEntities());
        vo.setDurationSeconds(record.getDurationSeconds());
        vo.setMealType(record.getMealType());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
