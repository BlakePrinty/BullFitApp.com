package com.bullfitapp.bullfit.workout;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "workout_set")
public class WorkoutSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workout_exercise_id", nullable = false)
    private Long workoutExerciseId;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "weight")
    private BigDecimal weight;

    @Column(name = "reps", nullable = false)
    private int reps;

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

    public int getSetNumber() {
        return setNumber;
    }

    public void setSetNumber(int setNumber) {
        this.setNumber = setNumber;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }
}
