package com.health.diet.service;

import com.health.diet.dto.command.MealPhotoCreateCommand;
import com.health.diet.dto.vo.MealPhotoVO;
import com.health.diet.entity.MealPhoto;
import com.health.diet.repository.MealPhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MealPhotoService {

    private static final Logger log = LoggerFactory.getLogger(MealPhotoService.class);

    private static final Path UPLOAD_ROOT = Paths.get("uploads", "diet-images");
    private static final Path TEMP_DIR = UPLOAD_ROOT.resolve("temp");

    private static final Map<String, String> MEAL_TYPE_EN = Map.of(
        "早餐", "breakfast",
        "午餐", "lunch",
        "晚餐", "dinner",
        "夜宵", "night",
        "其他", "other"
    );

    private final MealPhotoRepository mealPhotoRepository;

    public MealPhotoService(MealPhotoRepository mealPhotoRepository) {
        this.mealPhotoRepository = mealPhotoRepository;
    }

    /* 查询餐次照片 */
    /** 查询某日所有照片 */
    public List<MealPhotoVO> listByDate(Long userId, LocalDate date) {
        return mealPhotoRepository.findByUserIdAndRecordDateOrderByCreatedAtAsc(userId, date)
                .stream().map(this::toVO).toList();
    }

    /** 查询某日某餐次照片 */
    public List<MealPhotoVO> listByMeal(Long userId, LocalDate date, String mealType) {
        return mealPhotoRepository
                .findByUserIdAndRecordDateAndMealTypeOrderByCreatedAtAsc(userId, date, mealType)
                .stream().map(this::toVO).toList();
    }

    /** 新增照片记录：从临时路径移到最终路径，然后写 DB */
    public Long create(MealPhotoCreateCommand command) {
        LocalDate date = command.getRecordDate();
        String mealType = command.getMealType();
        Long userId = command.getUserId();
        String tempPath = command.getImageUrl(); // /diet-images/temp/{uuid}.jpg

        // 从临时路径提取 uuid
        String uuid = tempPath.replaceFirst(".*/", "").replaceFirst("\\.jpg$", "");

        // 移动到最终位置
        String finalPath = moveToFinal(uuid, userId, date, mealType);

        MealPhoto photo = new MealPhoto();
        photo.setUserId(userId);
        photo.setRecordDate(date);
        photo.setMealType(mealType);
        photo.setImageUrl(finalPath);

        mealPhotoRepository.save(photo);
        log.info("照片记录已保存: id={}, date={}, mealType={}, imageUrl={}",
                photo.getId(), date, mealType, photo.getImageUrl());
        return photo.getId();
    }

    /* 删除照片及文件 */
    /** 删除照片记录并删除磁盘文件 */
    public void delete(Long id, Long userId) {
        MealPhoto photo = mealPhotoRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("照片记录不存在: id={}", id);
                    return new IllegalArgumentException("照片记录不存在");
                });
        /* 所有权校验 */
        if (!photo.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此照片");
        }

        Path filePath = UPLOAD_ROOT.resolve(photo.getImageUrl().replaceFirst("^/diet-images/", ""));
        try {
            Files.deleteIfExists(filePath);
            log.info("照片文件已删除: {}", filePath);
        } catch (IOException e) {
            log.warn("删除照片文件失败: {}", filePath, e);
        }

        mealPhotoRepository.deleteById(id);
        log.info("照片记录已删除: id={}", id);
    }

    /* 保存照片到临时目录 */
    /**
     * 将上传的图片保存到临时目录，返回临时相对路径。
     * 后续 create() 时再按记录日期+餐次移动到最终位置。
     * 临时路径: /diet-images/temp/{uuid}.jpg
     */
    public String saveImageToDisk(byte[] imageBytes, Long userId) throws IOException {
        Files.createDirectories(TEMP_DIR);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String filename = uuid + ".jpg";
        Path filePath = TEMP_DIR.resolve(filename);
        Files.write(filePath, imageBytes);
        String relativePath = "/diet-images/temp/" + filename;
        log.info("照片已保存到临时目录: {} ({} bytes)", relativePath, imageBytes.length);
        return relativePath;
    }

    /* 照片移动到最终目录 */
    /**
     * 将临时文件移动到最终位置。
     * 最终路径: {userId}/{yyyy}/{MM}/{dd}/{yyyy-MM-dd}-{userId}-{mealTypeEn}.jpg
     */
    private String moveToFinal(String uuid, Long userId, LocalDate recordDate, String mealType) {
        try {
            Path tempFile = TEMP_DIR.resolve(uuid + ".jpg");
            if (!Files.exists(tempFile)) {
                log.warn("临时文件不存在，跳过移动: {}", tempFile);
                return "/diet-images/temp/" + uuid + ".jpg";
            }

            String mealEn = MEAL_TYPE_EN.getOrDefault(mealType, "other");
            /* 文件路径组织 userId/YYYY/MM/DD/ */
            String datePath = String.format("%d/%04d/%02d/%02d",
                    userId, recordDate.getYear(), recordDate.getMonthValue(), recordDate.getDayOfMonth());
            Path finalDir = UPLOAD_ROOT.resolve(datePath);
            Files.createDirectories(finalDir);

            String dateStr = String.format("%04d-%02d-%02d",
                    recordDate.getYear(), recordDate.getMonthValue(), recordDate.getDayOfMonth());
            String filename = dateStr + "-" + userId + "-" + mealEn + "-" + uuid + ".jpg";
            Path finalFile = finalDir.resolve(filename);

            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
            String relativePath = "/diet-images/" + datePath + "/" + filename;
            log.info("照片已移动到最终位置: {} -> {}", tempFile, finalFile);
            return relativePath;
        } catch (IOException e) {
            log.error("移动照片文件失败: uuid={}", uuid, e);
            return "/diet-images/temp/" + uuid + ".jpg";
        }
    }

    private MealPhotoVO toVO(MealPhoto photo) {
        MealPhotoVO vo = new MealPhotoVO();
        vo.setId(photo.getId());
        vo.setUserId(photo.getUserId());
        vo.setRecordDate(photo.getRecordDate());
        vo.setMealType(photo.getMealType());
        vo.setImageUrl(photo.getImageUrl());
        vo.setCreatedAt(photo.getCreatedAt());
        return vo;
    }
}
