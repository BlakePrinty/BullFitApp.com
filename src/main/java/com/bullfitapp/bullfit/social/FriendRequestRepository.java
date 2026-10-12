package com.bullfitapp.bullfit.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    Optional<FriendRequest> findFirstBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId,
                                                                      FriendRequestStatus status);
    Optional<FriendRequest> findByIdAndReceiverId(Long id, Long receiverId);
    Optional<FriendRequest> findByIdAndSenderId(Long id, Long senderId);
    List<FriendRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, FriendRequestStatus status);
    List<FriendRequest> findBySenderIdAndStatusOrderByCreatedAtDesc(Long senderId, FriendRequestStatus status);
    long countByReceiverIdAndStatus(Long receiverId, FriendRequestStatus status);
    long countBySenderIdAndStatus(Long senderId, FriendRequestStatus status);
    boolean existsBySenderIdAndReceiverIdAndStatusAndRespondedAtAfter(Long senderId, Long receiverId,
                                                                      FriendRequestStatus status,
                                                                      LocalDateTime after);
}

