package com.bullfitapp.bullfit.social;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountabilityGroupRepository extends JpaRepository<AccountabilityGroup, Long> {
    long countByOwnerId(Long ownerId);
}
