package com.bullfitapp.bullfit.workout;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {
    Optional<Workout> findFirstByUserIdAndStatus(Long userId, WorkoutStatus status);
    Optional<Workout> findByIdAndUserId(Long id, Long userId);
    Page<Workout> findByUserIdAndStatusAndStartedAtGreaterThanEqualOrderByStartedAtDesc(
            Long userId, WorkoutStatus status, LocalDateTime from, Pageable pageable);

    long countByUserIdAndStatusAndStartedAtLessThan(
            Long userId, WorkoutStatus status, LocalDateTime before);
}
