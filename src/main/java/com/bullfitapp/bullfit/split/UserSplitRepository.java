package com.bullfitapp.bullfit.split;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSplitRepository extends JpaRepository<UserSplit, Long> {
    Optional<UserSplit> findFirstByUserIdAndStatus(Long userId, RunStatus status);
    Optional<UserSplit> findByIdAndUserId(Long id, Long userId);
    boolean existsBySplitId(Long splitId);
    List<UserSplit> findByUserIdAndStatusOrderByEndedAtDesc(Long userId, RunStatus status);
}
