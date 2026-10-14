package com.bullfitapp.bullfit.split;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_split")
public class UserSplit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "split_id", nullable = false)
    private Long splitId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RunStatus status = RunStatus.ACTIVE;
    @Column(name = "target_length_days")
    private Integer targetLengthDays;
    @Column(name = "current_day_number", nullable = false)
    private int currentDayNumber = 1;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSplitId() {
        return splitId;
    }

    public void setSplitId(Long splitId) {
        this.splitId = splitId;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public Integer getTargetLengthDays() {
        return targetLengthDays;
    }

    public void setTargetLengthDays(Integer targetLengthDays) {
        this.targetLengthDays = targetLengthDays;
    }

    public int getCurrentDayNumber() {
        return currentDayNumber;
    }

    public void setCurrentDayNumber(int currentDayNumber) {
        this.currentDayNumber = currentDayNumber;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}
