package com.bullfitapp.bullfit.social;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_block")
public class UserBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;
    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBlockerId() {
        return blockerId;
    }

    public void setBlockerId(Long blockerId) {
        this.blockerId = blockerId;
    }

    public Long getBlockedId() {
        return blockedId;
    }

    public void setBlockedId(Long blockedId) {
        this.blockedId = blockedId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
