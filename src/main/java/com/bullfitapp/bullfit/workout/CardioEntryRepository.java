package com.bullfitapp.bullfit.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CardioEntryRepository extends JpaRepository<CardioEntry, Long> {
    List<CardioEntry> findByWorkoutExerciseIdInOrderById(Collection<Long> ids);
    long countByWorkoutExerciseId(Long workoutExerciseId);
}
