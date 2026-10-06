package com.bullfitapp.bullfit.workout;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cardio_entry")
public class CardioEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(name = "workout_exercise_id", nullable = false)
    private Long workoutExerciseId;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "distance")
    private BigDecimal distance;

    @Column(name = "speed")
    private BigDecimal speed;

    @Column(name = "incline")
    private BigDecimal incline;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkoutExerciseId() {
        return workoutExerciseId;
    }

    public void setWorkoutExerciseId(Long workoutExerciseId) {
        this.workoutExerciseId = workoutExerciseId;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public void setDistance(BigDecimal distance) {
        this.distance = distance;
    }

    public BigDecimal getSpeed() {
        return speed;
    }

    public void setSpeed(BigDecimal speed) {
        this.speed = speed;
    }

    public BigDecimal getIncline() {
        return incline;
    }

    public void setIncline(BigDecimal incline) {
        this.incline = incline;
    }
}
