package com.bullfitapp.bullfit.split;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SplitDayRepository extends JpaRepository<SplitDay, Long> {
    List<SplitDay> findBySplitIdOrderByDayNumber(Long splitId);
    Optional<SplitDay> findByIdAndSplitId(Long id, Long splitId);
}
