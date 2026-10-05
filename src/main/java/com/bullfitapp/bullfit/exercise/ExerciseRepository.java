package com.bullfitapp.bullfit.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    @Query("""
            select e from Exercise e
            where e.active = true
              and (e.ownerId is null or e.ownerId = :userId)
              and lower(e.name) like lower(concat('%', :q, '%'))
              and (:muscle is null or e.muscleGroup = :muscle)
              and (:equipment is null or e.equipment = :equipment)
            order by e.name
            """)
    List<Exercise> search(@Param("userId") Long userId,
                          @Param("q") String q,
                          @Param("muscle") MuscleGroup muscle,
                          @Param("equipment") Equipment equipment);
}
