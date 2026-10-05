package com.bullfitapp.bullfit.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByOwnerIdIsNullOrderByName();
    List<Exercise> findByOwnerIdOrderByName(Long ownerId);

    Optional<Exercise> findByIdAndOwnerIdIsNull(Long id);
    Optional<Exercise> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerIdAndActiveTrue(Long ownerId);

    boolean existsByOwnerIdIsNullAndNameIgnoreCase(String name);
    boolean existsByOwnerIdIsNullAndNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsByOwnerIdIsNullAndNameIgnoreCaseAndActiveTrue(String name);
    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);
    boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(Long ownerId, String name, Long id);

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
