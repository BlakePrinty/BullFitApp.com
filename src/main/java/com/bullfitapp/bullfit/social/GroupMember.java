package com.bullfitapp.bullfit.social;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_member")
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "group_id", nullable = false) private Long groupId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false)
    private GroupMemberStatus status = GroupMemberStatus.INVITED;
    @Column(name = "invited_by", nullable = false) private Long invitedBy;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "joined_at") private LocalDateTime joinedAt;
    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public GroupMemberStatus getStatus() {
        return status;
    }

    public void setStatus(GroupMemberStatus status) {
        this.status = status;
    }

    public Long getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Long invitedBy) {
        this.invitedBy = invitedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
