package com.bullfitapp.bullfit.split;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BullSplitRepository extends JpaRepository<BullSplit, Long> {
    List<BullSplit> findByOwnerIdAndCopyFalseAndArchivedFalseOrderByCreatedAtDesc(Long ownerId);
    Optional<BullSplit> findByIdAndOwnerId(Long id, Long ownerId);
    long countByOwnerIdAndArchivedFalse(Long ownerId);
    long countByOwnerIdAndPublishedTrueAndArchivedFalse(Long ownerId);
}
