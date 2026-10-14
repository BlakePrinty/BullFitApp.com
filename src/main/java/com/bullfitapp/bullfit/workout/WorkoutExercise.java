package com.bullfitapp.bullfit.workout;

import jakarta.persistence.*;

@Entity
@Table(name = "workout_exercise")
public class WorkoutExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workout_id", nullable = false)
    private Long workoutId;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "planned", nullable = false)
    private boolean planned = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkoutExerciseStatus status = WorkoutExerciseStatus.COMPLETED;

    @Column(name = "target_text")
    private String targetText;
    @Column(name = "swapped_from_id")
    private Long swappedFromId;

    @Column(name = "split_day_exercise_id")
    private Long splitDayExerciseId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(Long workoutId) {
        this.workoutId = workoutId;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public boolean isPlanned() {
        return planned;
    }

    public void setPlanned(boolean planned) {
        this.planned = planned;
    }

    public WorkoutExerciseStatus getStatus() {
        return status;
    }

    public void setStatus(WorkoutExerciseStatus status) {
        this.status = status;
    }

    public String getTargetText() {
        return targetText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }

    public Long getSwappedFromId() {
        return swappedFromId;
    }

    public void setSwappedFromId(Long swappedFromId) {
        this.swappedFromId = swappedFromId;
    }

    public Long getSplitDayExerciseId() {
        return splitDayExerciseId;
    }

    public void setSplitDayExerciseId(Long splitDayExerciseId) {
        this.splitDayExerciseId = splitDayExerciseId;
    }
}
