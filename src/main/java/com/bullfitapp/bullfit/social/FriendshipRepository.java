package com.bullfitapp.bullfit.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    boolean existsByUserAIdAndUserBId(Long userAId, Long userBId);

    List<Friendship> findByUserAIdOrUserBId(Long userAId, Long userBId);

    void deleteByUserAIdAndUserBId(Long userAId, Long userBId);
}
