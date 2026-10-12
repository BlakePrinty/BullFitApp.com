package com.bullfitapp.bullfit.split;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SplitExerciseOverrideRepository extends JpaRepository<SplitExerciseOverride, Long> {
    List<SplitExerciseOverride> findByUserSplitId(Long userSplitId);
    Optional<SplitExerciseOverride> findByUserSplitIdAndSplitDayExerciseId(Long userSplitId, Long sdeId);
}
