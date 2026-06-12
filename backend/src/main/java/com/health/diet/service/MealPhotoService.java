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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MealPhotoService {

    private static final Logger log = LoggerFactory.getLogger(MealPhotoService.class);

    /** 照片存储根目录（相对于 backend 运行目录） */
    private static final Path UPLOAD_ROOT = Paths.get("uploads", "diet-images");

    private final MealPhotoRepository mealPhotoRepository;

    public MealPhotoService(MealPhotoRepository mealPhotoRepository) {
        this.mealPhotoRepository = mealPhotoRepository;
    }

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

    /** 新增照片记录（数据库记录已存在 imageUrl，只写 DB） */
    public Long create(MealPhotoCreateCommand command) {
        MealPhoto photo = new MealPhoto();
        photo.setUserId(command.getUserId());
        photo.setRecordDate(command.getRecordDate());
        photo.setMealType(command.getMealType());
        photo.setImageUrl(command.getImageUrl());

        mealPhotoRepository.save(photo);
        log.info("照片记录已保存: id={}, mealType={}, imageUrl={}",
                photo.getId(), photo.getMealType(), photo.getImageUrl());
        return photo.getId();
    }

    /** 删除照片记录并删除磁盘文件 */
    public void delete(Long id, Long userId) {
        MealPhoto photo = mealPhotoRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("照片记录不存在: id={}", id);
                    return new IllegalArgumentException("照片记录不存在");
                });
        // 验证所有权
        if (!photo.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此照片");
        }

        // 删除磁盘文件
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

    /**
     * 将上传的图片字节保存到磁盘，返回相对路径。
     * 目录结构: uploads/diet-images/{yyyy}/{MM}/{dd}/{uuid}_{foodName}.jpg
     */
    public String saveImageToDisk(byte[] imageBytes, String foodName) throws IOException {
        LocalDate today = LocalDate.now();
        String datePath = String.format("%04d/%02d/%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        Path dir = UPLOAD_ROOT.resolve(datePath);
        Files.createDirectories(dir);

        // 清理食物名称用作文件名
        String safeName = foodName.replaceAll("[\\\\/:*?\"<>|]", "").substring(0, Math.min(10, foodName.length()));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String filename = uuid + "_" + safeName + ".jpg";
        Path filePath = dir.resolve(filename);

        Files.write(filePath, imageBytes);
        String relativePath = "/diet-images/" + datePath + "/" + filename;
        log.info("照片已保存到磁盘: {} ({} bytes)", relativePath, imageBytes.length);
        return relativePath;
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
