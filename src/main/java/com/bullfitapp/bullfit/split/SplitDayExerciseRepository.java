package com.bullfitapp.bullfit.split;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SplitDayExerciseRepository extends JpaRepository<SplitDayExercise, Long> {
    List<SplitDayExercise> findBySplitDayIdInOrderByOrderIndex(Collection<Long> dayIds);
    List<SplitDayExercise> findBySplitDayIdOrderByOrderIndex(Long dayId);
    Optional<SplitDayExercise> findByIdAndSplitDayId(Long id, Long splitDayId);
    boolean existsBySplitDayIdAndExerciseId(Long dayId, Long exerciseId);
    long countBySplitDayId(Long dayId);

    @Query("select coalesce(max(e.orderIndex), 0) from SplitDayExercise e where e.splitDayId = :dayId")
    int maxOrderIndex(@Param("dayId") Long dayId);

    @Query("""
            select new com.bullfitapp.bullfit.split.SplitExerciseCount(d.splitId, count(e))
            from SplitDayExercise e join SplitDay d on d.id = e.splitDayId
            where d.splitId in :splitIds
            group by d.splitId
            """)
    List<SplitExerciseCount> countBySplit(@Param("splitIds") Collection<Long> splitIds);
}
