package com.bullfitapp.bullfit.split;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSplitRepository extends JpaRepository<UserSplit, Long> {
    Optional<UserSplit> findFirstByUserIdAndStatus(Long userId, RunStatus status);
    Optional<UserSplit> findByIdAndUserId(Long id, Long userId);
    boolean existsBySplitId(Long splitId);
}
