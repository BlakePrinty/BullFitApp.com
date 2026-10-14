package com.bullfitapp.bullfit.split;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BullSplitRepository extends JpaRepository<BullSplit, Long> {
    List<BullSplit> findByOwnerIdAndCopyFalseAndArchivedFalseOrderByCreatedAtDesc(Long ownerId);

    Optional<BullSplit> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerIdAndArchivedFalse(Long ownerId);

    long countByOwnerIdAndPublishedTrueAndArchivedFalse(Long ownerId);

    Optional<BullSplit> findByIdAndPublishedTrueAndArchivedFalseAndLockedFalse(Long id);

    List<BullSplit> findByOwnerIdAndPublishedTrueAndArchivedFalseAndLockedFalseOrderByCreatedAtDesc(Long ownerId);

    List<BullSplit> findByOwnerIdAndCopyTrueAndArchivedFalseOrderByCreatedAtDesc(Long ownerId);

    boolean existsByOwnerIdAndCopiedFromIdAndArchivedFalse(Long ownerId, Long copiedFromId);

    long countByCopiedFromId(Long copiedFromId);

    String BROWSE_FROM = """
            from BullSplit s join User u on u.id = s.ownerId
            where s.published = true and s.archived = false and s.locked = false
              and (:days is null or s.dayCount = :days)
              and (lower(s.name) like lower(concat('%', :q, '%'))
                   or lower(u.username) like lower(concat('%', :q, '%')))
            """;

    String BROWSE_SELECT = """
            select new com.bullfitapp.bullfit.split.BrowseRow(
                s.id, s.name, s.description, s.dayCount, u.username, u.role, u.usernameColor, s.createdAt,
                (select count(c) from BullSplit c where c.copiedFromId = s.id))
            """;

    @Query(value = BROWSE_SELECT + BROWSE_FROM + " order by s.createdAt desc", countQuery = "select count(s) "
            + BROWSE_FROM)
    Page<BrowseRow> browseNewest(@Param("days") Integer days, @Param("q") String q, Pageable pageable);

    @Query(value = BROWSE_SELECT + BROWSE_FROM
            + " order by (select count(c2) from BullSplit c2 where c2.copiedFromId = s.id) desc, s.createdAt desc", countQuery = "select count(s) "
                    + BROWSE_FROM)
    Page<BrowseRow> browsePopular(@Param("days") Integer days, @Param("q") String q, Pageable pageable);
}
