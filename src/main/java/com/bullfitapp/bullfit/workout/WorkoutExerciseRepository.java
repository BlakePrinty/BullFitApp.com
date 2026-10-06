package com.bullfitapp.bullfit.workout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, Long> {
    List<WorkoutExercise> findByWorkoutIdOrderByOrderIndex(Long workoutId);
    Optional<WorkoutExercise> findByIdAndWorkoutId(Long id, Long workoutId);
    boolean existsByWorkoutIdAndExerciseId(Long workoutId, Long exerciseId);
    long countByWorkoutId(Long workoutId);

    @Query("select coalesce(max(we.orderIndex), 0) from WorkoutExercise we where we.workoutId = :workoutId")
    int maxOrderIndex(@Param("workoutId") Long workoutId);

    @Query("""
        select new com.bullfitapp.bullfit.workout.WorkoutExerciseName(we.workoutId, e.name)
        from WorkoutExercise we join Exercise e on e.id = we.exerciseId
        where we.workoutId in :ids
        order by we.workoutId, we.orderIndex
        """)
    List<WorkoutExerciseName> findNames(@Param("ids") Collection<Long> ids);
}
