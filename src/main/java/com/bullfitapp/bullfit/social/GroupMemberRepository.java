package com.bullfitapp.bullfit.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
        Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

        List<GroupMember> findByGroupIdOrderByCreatedAtAsc(Long groupId);

        List<GroupMember> findByUserIdAndStatus(Long userId, GroupMemberStatus status);

        long countByUserIdAndStatus(Long userId, GroupMemberStatus status);

        long countByGroupId(Long groupId); // active members plus pending invites

        void deleteByGroupIdAndUserId(Long groupId, Long userId);

        @Query("""
                        select new com.bullfitapp.bullfit.social.GroupCount(m.groupId, count(m))
                        from GroupMember m
                        where m.groupId in :groupIds and m.status = :status
                        group by m.groupId
                        """)
        List<GroupCount> countByGroups(@Param("groupIds") Collection<Long> groupIds,
                        @Param("status") GroupMemberStatus status);
}
