package com.bullfitapp.bullfit.workout;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {
    Optional<Workout> findFirstByUserIdAndStatus(Long userId, WorkoutStatus status);
    Optional<Workout> findByIdAndUserId(Long id, Long userId);
    Page<Workout> findByUserIdAndStatusAndStartedAtGreaterThanEqualOrderByStartedAtDesc(
            Long userId, WorkoutStatus status, LocalDateTime from, Pageable pageable);

    long countByUserIdAndStatusAndStartedAtLessThan(
            Long userId, WorkoutStatus status, LocalDateTime before);

    List<Workout> findByUserSplitIdAndStatusOrderByStartedAtAsc(Long runId, WorkoutStatus status);

    @Query("""
        select new com.bullfitapp.bullfit.workout.RunWorkoutCount(w.userSplitId, count(w))
        from Workout w
        where w.userSplitId in :runIds and w.status = :status
        group by w.userSplitId
        """)
    List<RunWorkoutCount> countByRuns(@Param("runIds") Collection<Long> runIds,
                                      @Param("status") WorkoutStatus status);

    @Query("""
        select new com.bullfitapp.bullfit.workout.WorkoutTime(w.userId, w.startedAt)
        from Workout w
        where w.userId in :userIds and w.status = :status and w.startedAt >= :from
        """)
    List<WorkoutTime> findTimes(@Param("userIds") Collection<Long> userIds,
                                @Param("status") WorkoutStatus status,
                                @Param("from") LocalDateTime from);
}
