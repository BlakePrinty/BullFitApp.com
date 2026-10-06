package com.bullfitapp.bullfit.weight;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BodyWeightLogRepository extends JpaRepository<BodyWeightLog, Long> {
    List<BodyWeightLog> findByUserIdOrderByLoggedOnAsc(Long userId);
    Optional<BodyWeightLog> findByUserIdAndLoggedOn(Long userId, LocalDate loggedOn);
    Optional<BodyWeightLog> findByIdAndUserId(Long id, Long userId);
    Optional<BodyWeightLog> findFirstByUserIdOrderByLoggedOnDesc(Long userId);
}
