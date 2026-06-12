package com.health.diet.repository;

import com.health.diet.entity.VoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VoiceRecordRepository extends JpaRepository<VoiceRecord, Long> {

    List<VoiceRecord> findByUserIdAndRecordDateOrderByCreatedAtDesc(Long userId, LocalDate recordDate);
}
