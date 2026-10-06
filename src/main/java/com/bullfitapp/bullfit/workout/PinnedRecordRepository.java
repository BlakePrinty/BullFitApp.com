package com.bullfitapp.bullfit.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PinnedRecordRepository extends JpaRepository<PinnedRecord, Long> {
    List<PinnedRecord> findByUserIdOrderBySlot(Long userId);
    boolean existsByUserIdAndExerciseId(Long userId, Long exerciseId);
    void deleteByUserIdAndExerciseId(Long userId, Long exerciseId);
}
