package com.health.diet.repository;

import com.health.diet.entity.VoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/* 语音记录数据访问层 */
public interface VoiceRecordRepository extends JpaRepository<VoiceRecord, Long> {

    /* findByUserIdAndRecordDateOrderByCreatedAtDesc 按用户和日期查询语音记录列表（倒序） */
    List<VoiceRecord> findByUserIdAndRecordDateOrderByCreatedAtDesc(Long userId, LocalDate recordDate);
}
