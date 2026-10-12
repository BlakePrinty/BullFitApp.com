package com.bullfitapp.bullfit.workout;

import com.bullfitapp.bullfit.exercise.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {
    List<WorkoutSet> findByWorkoutExerciseIdInOrderBySetNumber(Collection<Long> ids);
    List<WorkoutSet> findByWorkoutExerciseIdOrderBySetNumber(Long workoutExerciseId);
    long countByWorkoutExerciseId(Long workoutExerciseId);

    @Query("""
        select new com.bullfitapp.bullfit.workout.WorkoutSetCount(we.workoutId, count(s))
        from WorkoutSet s join WorkoutExercise we on we.id = s.workoutExerciseId
        where we.workoutId in :ids
        group by we.workoutId
        """)
    List<WorkoutSetCount> countByWorkout(@Param("ids") Collection<Long> ids);

    @Query("""
        select new com.bullfitapp.bullfit.workout.SetRecordRow(we.exerciseId, s.weight, s.reps, w.startedAt)
        from WorkoutSet s
        join WorkoutExercise we on we.id = s.workoutExerciseId
        join Workout w on w.id = we.workoutId
        where w.userId = :userId and w.status = :status
        """)
    List<SetRecordRow> findRows(@Param("userId") Long userId, @Param("status") WorkoutStatus status);

    @Query("""
        select new com.bullfitapp.bullfit.workout.SetRecordRow(we.exerciseId, s.weight, s.reps, w.startedAt)
        from WorkoutSet s
        join WorkoutExercise we on we.id = s.workoutExerciseId
        join Workout w on w.id = we.workoutId
        where w.userId = :userId and w.status = :status
          and we.exerciseId in :exerciseIds and w.id <> :excludeWorkoutId
        """)
    List<SetRecordRow> findRowsForExercises(@Param("userId") Long userId,
                                            @Param("status") WorkoutStatus status,
                                            @Param("exerciseIds") Collection<Long> exerciseIds,
                                            @Param("excludeWorkoutId") Long excludeWorkoutId);

    @Query("""
        select count(s) from WorkoutSet s
        join WorkoutExercise we on we.id = s.workoutExerciseId
        join Workout w on w.id = we.workoutId
        where w.userSplitId = :runId and w.status = :status
        """)
    long countSetsForRun(@Param("runId") Long runId, @Param("status") WorkoutStatus status);

    @Query("""
        select sum(s.weight * s.reps) from WorkoutSet s
        join WorkoutExercise we on we.id = s.workoutExerciseId
        join Workout w on w.id = we.workoutId
        join Exercise e on e.id = we.exerciseId
        where w.userSplitId = :runId and w.status = :status
          and s.weight is not null and e.equipment <> :assisted
        """)
    BigDecimal volumeForRun(@Param("runId") Long runId, @Param("status") WorkoutStatus status,
                            @Param("assisted") Equipment assisted);
}
