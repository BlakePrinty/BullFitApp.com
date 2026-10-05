package com.bullfitapp.bullfit.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExerciseRequestRepository extends JpaRepository<ExerciseRequest, Long> {

    boolean existsByExerciseIdAndStatus(Long exerciseId, RequestStatus status);

    Optional<ExerciseRequest> findByIdAndRequesterId(Long id, Long requesterId);

    long countByStatus(RequestStatus status);

    @Query("select r.exerciseId from ExerciseRequest r "
            + "where r.requesterId = :userId and r.status = :status")
    List<Long> findExerciseIds(@Param("userId") Long userId, @Param("status") RequestStatus status);

    @Query("""
            select new com.bullfitapp.bullfit.exercise.RequestView(
                r.id, r.exerciseId, e.name, e.muscleGroup, e.equipment,
                u.username, r.status, r.note, r.reviewNote, r.createdAt)
            from ExerciseRequest r
            join Exercise e on e.id = r.exerciseId
            join User u on u.id = r.requesterId
            where r.status = :status
            order by r.createdAt
            """)
    List<RequestView> findViewsByStatus(@Param("status") RequestStatus status);

    @Query("""
            select new com.bullfitapp.bullfit.exercise.RequestView(
                r.id, r.exerciseId, e.name, e.muscleGroup, e.equipment,
                u.username, r.status, r.note, r.reviewNote, r.createdAt)
            from ExerciseRequest r
            join Exercise e on e.id = r.exerciseId
            join User u on u.id = r.requesterId
            where r.requesterId = :userId
            order by r.createdAt desc
            """)
    List<RequestView> findViewsByRequester(@Param("userId") Long userId);
}
