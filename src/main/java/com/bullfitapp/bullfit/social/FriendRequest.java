package com.bullfitapp.bullfit.social;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "friend_request")
public class FriendRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "sender_id", nullable = false) private Long senderId;
    @Column(name = "receiver_id", nullable = false) private Long receiverId;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false)
    private FriendRequestStatus status = FriendRequestStatus.PENDING;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "responded_at") private LocalDateTime respondedAt;
    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public FriendRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FriendRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
